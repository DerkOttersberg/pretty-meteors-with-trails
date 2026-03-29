package com.derko.prettymeteors.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;

public final class SkyMeteorRenderer {
    private static final Vec3d WORLD_UP = new Vec3d(0.0, 1.0, 0.0);
    private static final Vec3d WORLD_FORWARD = new Vec3d(0.0, 0.0, 1.0);
    private static final Vec3d WORLD_RIGHT = new Vec3d(1.0, 0.0, 0.0);

    /**
     * Custom fog-free render pipeline using position_color shaders (no fog.glsl import).
     * Uses TRANSFORMS_AND_PROJECTION_SNIPPET so the DynamicTransforms UBO (ModelViewMat,
     * ColorModulator) and Projection UBO (ProjMat) are properly bound.
     * Accessed via AccessWidener — works correctly in both dev and production regardless of
     * what Iris or other mods do at runtime (no reflection, no string-based field lookup).
     */
    private static final RenderPipeline METEOR_PIPELINE = createMeteorPipeline();
    private static final RenderLayer METEOR_LAYER = createMeteorLayer();

    private static RenderPipeline createMeteorPipeline() {
        RenderPipeline pipeline = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                .withLocation(Identifier.of("pretty_meteors", "pipeline/meteor"))
                .withVertexShader("core/position_color")
                .withFragmentShader("core/position_color")
                .withBlend(BlendFunction.LIGHTNING)
                .withDepthWrite(false)
                .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                .build();
        RenderPipelines.register(pipeline);
        return pipeline;
    }

    private static RenderLayer createMeteorLayer() {
        RenderSetup setup = RenderSetup.builder(METEOR_PIPELINE).build();
        return RenderLayer.of("pretty_meteors:meteor", setup);
    }

    /**
     * Returns true if Iris is loaded and a shaderpack is currently active.
     * Uses reflection so this works even when Iris is not installed.
     * When shaders are active, Iris owns the GPU pipeline and our custom layer is dropped;
     * we fall back to the vanilla lightning layer, whose fog is then controlled by the
     * shader (not vanilla fog) so distance-clamping is not an issue.
     */
    private static boolean isIrisShadersActive() {
        try {
            Class<?> irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = irisApi.getMethod("getInstance").invoke(null);
            return (Boolean) irisApi.getMethod("isShaderPackInUse").invoke(instance);
        } catch (Throwable t) {
            return false;
        }
    }

    private static RenderLayer getRenderLayer() {
        return isIrisShadersActive() ? RenderLayers.lightning() : METEOR_LAYER;
    }

    private SkyMeteorRenderer() {
    }

    public static void render(Matrix4f positionMatrix, List<SkyMeteor> meteors, Vec3d showerOrigin, long worldTime, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.gameRenderer == null) {
            return;
        }

        Vec3d cameraPos = client.gameRenderer.getCamera().getCameraPos();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (SkyMeteor meteor : meteors) {
            renderMeteor(buffer, positionMatrix, meteor, showerOrigin, cameraPos, worldTime, tickDelta);
        }

        BuiltBuffer builtBuffer = buffer.endNullable();
        if (builtBuffer == null) {
            return;
        }

        try (builtBuffer) {
            getRenderLayer().draw(builtBuffer);
        }
    }

    private static void renderMeteor(VertexConsumer consumer, Matrix4f matrix, SkyMeteor meteor, Vec3d showerOrigin, Vec3d cameraPos, long worldTime, float tickDelta) {
        float age = meteor.ageAt(worldTime, tickDelta);
        if (age <= 0.0f || age >= meteor.lifetimeTicks()) {
            return;
        }

        float lifeFade = 1.0f - MathHelper.clamp(age / meteor.lifetimeTicks(), 0.0f, 1.0f);
        float trailAge = Math.min(age, meteor.trailDurationTicks());
        if (trailAge <= 0.1f) {
            return;
        }

        Vec3d head = showerOrigin.add(meteor.positionAt(age)).subtract(cameraPos);
        Vec3d direction = meteor.travelDirection();

        // Scale width with distance so apparent screen size stays constant.
        float distanceScale = MathHelper.clamp((float) head.length() / 100.0f, 1.0f, 10.0f);
        float scaledWidth = meteor.trailWidth() * distanceScale;

        Vec3d primaryAxis = normalizeOrNull(direction.crossProduct(WORLD_UP));
        if (primaryAxis == null) primaryAxis = normalizeOrNull(direction.crossProduct(WORLD_FORWARD));
        if (primaryAxis == null) primaryAxis = WORLD_RIGHT;

        // Teardrop trail: thin at leading tip → swells to max width → tapers to nothing.
        renderTrailLayer(consumer, matrix, meteor, showerOrigin, cameraPos, age, trailAge, lifeFade, primaryAxis, scaledWidth, 1.0f, meteor.headColor(), meteor.tailColor(), false);

        // Needle cap: a sharp tapered point extending forward from the head — the droplet's leading tip.
        drawNeedle(consumer, matrix, head, direction, primaryAxis, scaledWidth * 0.14f, scaledWidth * 1.0f, scaleAlpha(meteor.headColor(), lifeFade));
    }

    private static void renderTrailLayer(VertexConsumer consumer, Matrix4f matrix, SkyMeteor meteor, Vec3d showerOrigin, Vec3d cameraPos, float age, float trailAge, float lifeFade, Vec3d axis, float baseWidth, float alphaScale, int headColor, int tailColor, boolean narrowCrossRibbon) {
        int segmentCount = meteor.segmentCount();

        for (int index = 0; index < segmentCount; index++) {
            float progress0 = index / (float) segmentCount;
            float progress1 = (index + 1) / (float) segmentCount;
            float sampleAge0 = Math.max(0.0f, age - trailAge * progress0);
            float sampleAge1 = Math.max(0.0f, age - trailAge * progress1);

            Vec3d point0 = showerOrigin.add(meteor.positionAt(sampleAge0)).subtract(cameraPos);
            Vec3d point1 = showerOrigin.add(meteor.positionAt(sampleAge1)).subtract(cameraPos);
            Vec3d segmentDirection = point1.subtract(point0);
            if (segmentDirection.lengthSquared() < 1.0E-6) {
                continue;
            }

            float widthScale = narrowCrossRibbon ? 0.92f : 1.0f;
            Vec3d side0 = axis.multiply(computeTrailWidth(baseWidth, progress0) * widthScale);
            Vec3d side1 = axis.multiply(computeTrailWidth(baseWidth, progress1) * widthScale);

            int color0 = scaleAlpha(blendColor(headColor, tailColor, progress0), alphaScale * lifeFade * opacityForProgress(progress0));
            int color1 = scaleAlpha(blendColor(headColor, tailColor, progress1), alphaScale * lifeFade * opacityForProgress(progress1));

            drawRibbonQuad(
                    consumer,
                    matrix,
                    point0,
                    point1,
                    side0,
                    side1,
                    color0,
                    color0,
                    color1,
                    color1);
        }
    }

    private static void drawNeedle(VertexConsumer consumer, Matrix4f matrix, Vec3d base, Vec3d direction, Vec3d axis, float baseWidth, float length, int color) {
        // Tapered cap extending forward from the meteor head.
        // Wide and bright at the base (connects to the trail swell), fades to a transparent point.
        Vec3d tip = base.add(direction.multiply(length));
        addQuad(consumer, matrix,
                base.subtract(axis.multiply(baseWidth)),
                base.add(axis.multiply(baseWidth)),
                tip.add(axis.multiply(0.001f)),
                tip.subtract(axis.multiply(0.001f)),
                color, color,
                scaleAlpha(color, 0.0f), scaleAlpha(color, 0.0f));
    }

    private static Vec3d orthogonalVector(Vec3d direction, Vec3d primaryFallback, Vec3d secondaryFallback) {
        Vec3d primary = normalizeOrNull(direction.crossProduct(primaryFallback));
        if (primary != null) {
            return primary;
        }

        return normalizeOrNull(direction.crossProduct(secondaryFallback));
    }

    private static Vec3d normalizeOrNull(Vec3d vector) {
        return vector.lengthSquared() < 1.0E-7 ? null : vector.normalize();
    }

    private static Vec3d safeNormalize(Vec3d vector) {
        if (vector.lengthSquared() < 1.0E-7) {
            return WORLD_FORWARD;
        }

        return vector.normalize();
    }

    private static float computeTrailWidth(float baseWidth, float progress) {
        float clamped = MathHelper.clamp(progress, 0.0f, 1.0f);
        // Teardrop/droplet silhouette:
        // progress=0 (head tip) → width≈0, swell to max at ~12%, taper to 0 at tail.
        float swell = smoothStep(0.0f, 0.12f, clamped);
        float taper = 1.0f - smoothStep(0.12f, 0.98f, clamped);
        return baseWidth * Math.max(swell * taper, 0.001f);
    }

    private static float opacityForProgress(float progress) {
        return 1.0f - 0.88f * smoothStep(0.34f, 1.0f, MathHelper.clamp(progress, 0.0f, 1.0f));
    }

    private static float smoothStep(float edge0, float edge1, float value) {
        float normalized = MathHelper.clamp((value - edge0) / Math.max(0.0001f, edge1 - edge0), 0.0f, 1.0f);
        return normalized * normalized * (3.0f - 2.0f * normalized);
    }

    private static int blendColor(int startColor, int endColor, float progress) {
        float clamped = MathHelper.clamp(progress, 0.0f, 1.0f);
        int startAlpha = startColor >>> 24;
        int startRed = (startColor >>> 16) & 255;
        int startGreen = (startColor >>> 8) & 255;
        int startBlue = startColor & 255;
        int endAlpha = endColor >>> 24;
        int endRed = (endColor >>> 16) & 255;
        int endGreen = (endColor >>> 8) & 255;
        int endBlue = endColor & 255;

        int alpha = Math.round(MathHelper.lerp(clamped, startAlpha, endAlpha));
        int red = Math.round(MathHelper.lerp(clamped, startRed, endRed));
        int green = Math.round(MathHelper.lerp(clamped, startGreen, endGreen));
        int blue = Math.round(MathHelper.lerp(clamped, startBlue, endBlue));
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static int scaleAlpha(int color, float alphaScale) {
        int alpha = color >>> 24;
        int scaledAlpha = MathHelper.clamp(Math.round(alpha * alphaScale), 0, 255);
        return scaledAlpha << 24 | (color & 0x00FFFFFF);
    }

    private static void drawRibbonQuad(VertexConsumer consumer, Matrix4f matrix, Vec3d start, Vec3d end, Vec3d startSide, Vec3d endSide, int colorA, int colorB, int colorC, int colorD) {
        addQuad(
                consumer,
                matrix,
                start.add(startSide),
                start.subtract(startSide),
                end.subtract(endSide),
                end.add(endSide),
                colorA,
                colorB,
                colorC,
                colorD);
    }

    private static void addQuad(VertexConsumer consumer, Matrix4f matrix, Vec3d a, Vec3d b, Vec3d c, Vec3d d, int colorA, int colorB, int colorC, int colorD) {
        // Front face
        putVertex(consumer, matrix, a, colorA);
        putVertex(consumer, matrix, b, colorB);
        putVertex(consumer, matrix, c, colorC);
        putVertex(consumer, matrix, d, colorD);
        // Back face (reversed winding) so the trail is visible from both sides
        putVertex(consumer, matrix, d, colorD);
        putVertex(consumer, matrix, c, colorC);
        putVertex(consumer, matrix, b, colorB);
        putVertex(consumer, matrix, a, colorA);
    }

    private static void putVertex(VertexConsumer consumer, Matrix4f matrix, Vec3d position, int color) {
        int alpha = color >>> 24;
        int red = (color >>> 16) & 255;
        int green = (color >>> 8) & 255;
        int blue = color & 255;

        consumer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .color(red, green, blue, alpha);
    }
}