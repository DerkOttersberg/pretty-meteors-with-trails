package com.derko.prettymeteors.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class SkyMeteorRenderer {
    private static final Vec3 WORLD_UP = new Vec3(0.0, 1.0, 0.0);
    private static final Vec3 WORLD_FORWARD = new Vec3(0.0, 0.0, 1.0);
    private static final Vec3 WORLD_RIGHT = new Vec3(1.0, 0.0, 0.0);

    private SkyMeteorRenderer() {
    }

    public static void render(Matrix4f positionMatrix, VertexConsumer consumer, List<SkyMeteor> meteors, Vec3 showerOrigin, long worldTime, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client.gameRenderer == null) {
            return;
        }

        Vec3 cameraPos = client.gameRenderer.mainCamera().position();

        for (SkyMeteor meteor : meteors) {
            renderMeteor(consumer, positionMatrix, meteor, showerOrigin, cameraPos, worldTime, tickDelta);
        }
    }

    private static void renderMeteor(VertexConsumer consumer, Matrix4f matrix, SkyMeteor meteor, Vec3 showerOrigin, Vec3 cameraPos, long worldTime, float tickDelta) {
        float age = meteor.ageAt(worldTime, tickDelta);
        if (age <= 0.0f || age >= meteor.lifetimeTicks()) {
            return;
        }

        float lifeFade = 1.0f - Mth.clamp(age / meteor.lifetimeTicks(), 0.0f, 1.0f);
        float trailAge = Math.min(age, meteor.trailDurationTicks());
        if (trailAge <= 0.1f) {
            return;
        }

        Vec3 head = showerOrigin.add(meteor.positionAt(age)).subtract(cameraPos);
        Vec3 direction = meteor.travelDirection();

        // Scale width with distance so apparent screen size stays constant.
        float distanceScale = Mth.clamp((float) head.length() / 100.0f, 1.0f, 10.0f);
        float scaledWidth = meteor.trailWidth() * distanceScale;

        Vec3 primaryAxis = normalizeOrNull(direction.cross(WORLD_UP));
        if (primaryAxis == null) primaryAxis = normalizeOrNull(direction.cross(WORLD_FORWARD));
        if (primaryAxis == null) primaryAxis = WORLD_RIGHT;

        // Teardrop trail: thin at leading tip → swells to max width → tapers to nothing.
        renderTrailLayer(consumer, matrix, meteor, showerOrigin, cameraPos, age, trailAge, lifeFade, primaryAxis, scaledWidth, 1.0f, meteor.headColor(), meteor.tailColor(), false);

        // Needle cap: a sharp tapered point extending forward from the head — the droplet's leading tip.
        drawNeedle(consumer, matrix, head, direction, primaryAxis, scaledWidth * 0.14f, scaledWidth * 1.0f, scaleAlpha(meteor.headColor(), lifeFade));
    }

    private static void renderTrailLayer(VertexConsumer consumer, Matrix4f matrix, SkyMeteor meteor, Vec3 showerOrigin, Vec3 cameraPos, float age, float trailAge, float lifeFade, Vec3 axis, float baseWidth, float alphaScale, int headColor, int tailColor, boolean narrowCrossRibbon) {
        int segmentCount = meteor.segmentCount();

        for (int index = 0; index < segmentCount; index++) {
            float progress0 = index / (float) segmentCount;
            float progress1 = (index + 1) / (float) segmentCount;
            float sampleAge0 = Math.max(0.0f, age - trailAge * progress0);
            float sampleAge1 = Math.max(0.0f, age - trailAge * progress1);

            Vec3 point0 = showerOrigin.add(meteor.positionAt(sampleAge0)).subtract(cameraPos);
            Vec3 point1 = showerOrigin.add(meteor.positionAt(sampleAge1)).subtract(cameraPos);
            Vec3 segmentDirection = point1.subtract(point0);
            if (segmentDirection.lengthSqr() < 1.0E-6) {
                continue;
            }

            float widthScale = narrowCrossRibbon ? 0.92f : 1.0f;
            Vec3 side0 = axis.scale(computeTrailWidth(baseWidth, progress0) * widthScale);
            Vec3 side1 = axis.scale(computeTrailWidth(baseWidth, progress1) * widthScale);

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

    private static void drawNeedle(VertexConsumer consumer, Matrix4f matrix, Vec3 base, Vec3 direction, Vec3 axis, float baseWidth, float length, int color) {
        // Tapered cap extending forward from the meteor head.
        // Wide and bright at the base (connects to the trail swell), fades to a transparent point.
        Vec3 tip = base.add(direction.scale(length));
        addQuad(consumer, matrix,
                base.subtract(axis.scale(baseWidth)),
                base.add(axis.scale(baseWidth)),
                tip.add(axis.scale(0.001f)),
                tip.subtract(axis.scale(0.001f)),
                color, color,
                scaleAlpha(color, 0.0f), scaleAlpha(color, 0.0f));
    }

    private static Vec3 orthogonalVector(Vec3 direction, Vec3 primaryFallback, Vec3 secondaryFallback) {
        Vec3 primary = normalizeOrNull(direction.cross(primaryFallback));
        if (primary != null) {
            return primary;
        }

        return normalizeOrNull(direction.cross(secondaryFallback));
    }

    private static Vec3 normalizeOrNull(Vec3 vector) {
        return vector.lengthSqr() < 1.0E-7 ? null : vector.normalize();
    }

    private static Vec3 safeNormalize(Vec3 vector) {
        if (vector.lengthSqr() < 1.0E-7) {
            return WORLD_FORWARD;
        }

        return vector.normalize();
    }

    private static float computeTrailWidth(float baseWidth, float progress) {
        float clamped = Mth.clamp(progress, 0.0f, 1.0f);
        // Teardrop/droplet silhouette:
        // progress=0 (head tip) → width≈0, swell to max at ~12%, taper to 0 at tail.
        float swell = smoothStep(0.0f, 0.12f, clamped);
        float taper = 1.0f - smoothStep(0.12f, 0.98f, clamped);
        return baseWidth * Math.max(swell * taper, 0.001f);
    }

    private static float opacityForProgress(float progress) {
        return 1.0f - 0.88f * smoothStep(0.34f, 1.0f, Mth.clamp(progress, 0.0f, 1.0f));
    }

    private static float smoothStep(float edge0, float edge1, float value) {
        float normalized = Mth.clamp((value - edge0) / Math.max(0.0001f, edge1 - edge0), 0.0f, 1.0f);
        return normalized * normalized * (3.0f - 2.0f * normalized);
    }

    private static int blendColor(int startColor, int endColor, float progress) {
        float clamped = Mth.clamp(progress, 0.0f, 1.0f);
        int startAlpha = startColor >>> 24;
        int startRed = (startColor >>> 16) & 255;
        int startGreen = (startColor >>> 8) & 255;
        int startBlue = startColor & 255;
        int endAlpha = endColor >>> 24;
        int endRed = (endColor >>> 16) & 255;
        int endGreen = (endColor >>> 8) & 255;
        int endBlue = endColor & 255;

        int alpha = Math.round(Mth.lerpInt(clamped, startAlpha, endAlpha));
        int red = Math.round(Mth.lerpInt(clamped, startRed, endRed));
        int green = Math.round(Mth.lerpInt(clamped, startGreen, endGreen));
        int blue = Math.round(Mth.lerpInt(clamped, startBlue, endBlue));
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static int scaleAlpha(int color, float alphaScale) {
        int alpha = color >>> 24;
        int scaledAlpha = Mth.clamp(Math.round(alpha * alphaScale), 0, 255);
        return scaledAlpha << 24 | (color & 0x00FFFFFF);
    }

    private static void drawRibbonQuad(VertexConsumer consumer, Matrix4f matrix, Vec3 start, Vec3 end, Vec3 startSide, Vec3 endSide, int colorA, int colorB, int colorC, int colorD) {
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

    private static void addQuad(VertexConsumer consumer, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int colorA, int colorB, int colorC, int colorD) {
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

    private static void putVertex(VertexConsumer consumer, Matrix4f matrix, Vec3 position, int color) {
        int alpha = color >>> 24;
        int red = (color >>> 16) & 255;
        int green = (color >>> 8) & 255;
        int blue = color & 255;

        consumer.addVertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .setColor(red, green, blue, alpha);
    }
}
