package com.derko.prettymeteors.client;

import com.derko.prettymeteors.MeteorShowerConfig;
import com.derko.prettymeteors.PrettyMeteorsMod;
import com.derko.prettymeteors.network.MeteorShowerPayload;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;

public final class MeteorShowerClientState {
    public static final MeteorShowerClientState INSTANCE = new MeteorShowerClientState();

    private static final double MIN_CLEARANCE_ABOVE_ORIGIN = 88.0;
    private static final int MAX_ACTIVE_METEORS = 320;

    private final List<SkyMeteor> meteors = new ArrayList<>();

    private MeteorShowerConfig activeConfig;
    private ResourceKey<Level> activeWorldKey;
    private ResourceKey<Level> lastSeenWorldKey;
    private Vec3 showerOrigin;
    private RandomSource random = RandomSource.create();
    private final MeteorSpawnBudget spawnBudget = new MeteorSpawnBudget();
    private boolean loggedFirstMeteor;
    private boolean loggedFirstRender;

    private MeteorShowerClientState() {
    }

    public void applyPayload(Minecraft client, MeteorShowerPayload payload) {
        if (client.level == null) {
            clearAll();
            return;
        }

        lastSeenWorldKey = client.level.dimension();

        if (!payload.active()) {
            activeConfig = null;
            activeWorldKey = client.level.dimension();
            spawnBudget.clear();
            return;
        }

        MeteorShowerConfig incomingConfig = payload.toConfig();
        ResourceKey<Level> incomingWorldKey = client.level.dimension();

        if (incomingConfig.equals(activeConfig) && incomingWorldKey.equals(activeWorldKey)) {
            return;
        }

        activeConfig = incomingConfig;
        activeWorldKey = incomingWorldKey;
        showerOrigin = new Vec3(incomingConfig.originX(), incomingConfig.originY(), incomingConfig.originZ());
        random = RandomSource.create(incomingConfig.seed());
        spawnBudget.clear();
        spawnBudget.rebase(client.level.getGameTime());
        meteors.clear();
        loggedFirstMeteor = false;
        loggedFirstRender = false;
        PrettyMeteorsMod.LOGGER.debug(
                "Received meteor shower: {}s at ({}, {}, {})",
                incomingConfig.durationTicks() / 20,
                Math.round(incomingConfig.originX()),
                Math.round(incomingConfig.originY()),
                Math.round(incomingConfig.originZ()));
    }

    public void tick(Minecraft client) {
        if (client.level == null || client.player == null) {
            clearAll();
            return;
        }

        ResourceKey<Level> worldKey = client.level.dimension();

        if (lastSeenWorldKey != null && !lastSeenWorldKey.equals(worldKey)) {
            clearAll();
        }

        lastSeenWorldKey = worldKey;

        long worldTime = client.level.getGameTime();
        meteors.removeIf(meteor -> !meteor.isAlive(worldTime));

        if (activeConfig == null) {
            spawnBudget.rebase(worldTime);
            return;
        }

        if (!worldKey.equals(activeWorldKey)) {
            clearAll();
            return;
        }

        if (worldTime >= activeConfig.endTick()) {
            activeConfig = null;
            spawnBudget.clear();
            spawnBudget.rebase(worldTime);
            return;
        }

        int births = spawnBudget.advance(
                worldTime,
                activeConfig.meteorsPerSecond(),
                activeConfig.isActiveAt(worldTime),
                Math.max(0, MAX_ACTIVE_METEORS - meteors.size()));
        for (int index = 0; index < births; index++) {
            SkyMeteor meteor = createMeteor(worldTime, activeConfig);
            meteors.add(meteor);
            if (!loggedFirstMeteor) {
                loggedFirstMeteor = true;
                PrettyMeteorsMod.LOGGER.debug("Meteor shower client spawned its first trail");
            }
        }
    }

    public void renderWorldPass(Matrix4f positionMatrix, VertexConsumer consumer, float depthFar) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || meteors.isEmpty()) {
            return;
        }

        float tickDelta = client.getTimer().getGameTimeDeltaPartialTick(false);
        if (showerOrigin == null) {
            return;
        }

        if (!loggedFirstRender) {
            loggedFirstRender = true;
            PrettyMeteorsMod.LOGGER.debug(
                    "Meteor geometry reached the world render pass ({} active trails)",
                    meteors.size());
        }

        SkyMeteorRenderer.render(
                positionMatrix,
                consumer,
                meteors,
                showerOrigin,
                client.level.getGameTime(),
                tickDelta,
                depthFar);
    }

    public void clearAll() {
        meteors.clear();
        activeConfig = null;
        activeWorldKey = null;
        lastSeenWorldKey = null;
        showerOrigin = null;
        spawnBudget.clear();
        random = RandomSource.create();
        loggedFirstMeteor = false;
        loggedFirstRender = false;
    }

    private SkyMeteor createMeteor(long worldTime, MeteorShowerConfig config) {
        // --- style bucket: 70% fast/small, 30% slow/big ---
        float styleRoll = random.nextFloat();
        float speedFactor, lifetimeFactor, trailFactor, widthFactor, tipFactor;
        if (styleRoll < 0.70f) {
            // fast, short-lived, thin
            speedFactor = 1.5f + random.nextFloat() * 0.7f;
            lifetimeFactor = 0.55f + random.nextFloat() * 0.25f;
            trailFactor = 0.26f + random.nextFloat() * 0.12f;
            widthFactor = 0.52f + random.nextFloat() * 0.14f;
            tipFactor = 5.4f + random.nextFloat() * 2.0f;
        } else {
            // moderately slower, longer trail, only barely wider
            speedFactor = 0.75f + random.nextFloat() * 0.20f;
            lifetimeFactor = 1.8f + random.nextFloat() * 0.5f;
            trailFactor = 0.36f + random.nextFloat() * 0.12f;
            widthFactor = 0.62f + random.nextFloat() * 0.14f;
            tipFactor = 4.8f + random.nextFloat() * 1.6f;
        }

        float sampledBaseSpeed = config.baseSpeed() + (random.nextFloat() - 0.5f) * config.speedVariance() * 1.35f;
        float speed = Math.max(0.55f, sampledBaseSpeed * speedFactor);
        int lifetimeTicks = Math.max(24, Math.round(config.lifetimeForSpeed(speed) * lifetimeFactor));

        // --- early burnout: at each quarter-lifetime checkpoint, 25% chance to cut short ---
        if (random.nextFloat() < 0.25f) {
            lifetimeTicks = Math.max(18, (int)(lifetimeTicks * 0.25f));
        } else if (random.nextFloat() < 0.25f) {
            lifetimeTicks = Math.max(18, (int)(lifetimeTicks * 0.50f));
        } else if (random.nextFloat() < 0.25f) {
            lifetimeTicks = Math.max(18, (int)(lifetimeTicks * 0.75f));
        }

        float trailDuration = Math.min(3.5f + lifetimeTicks * trailFactor * 0.055f, 9.0f);

        // --- travel direction: consistent yaw across the sky, shallow downward pitch ---
        float yawDeg = config.yawDegrees() + (random.nextFloat() - 0.5f) * config.angularSpreadDegrees() * 15.0f;
        float pitchDeg = Mth.clamp(
                config.pitchDegrees() + (random.nextFloat() - 0.5f) * 3.0f,
                2.0f, 18.0f);
        double yawRad = Math.toRadians(yawDeg);
        double pitchRad = Math.toRadians(pitchDeg);
        Vec3 direction = new Vec3(
                -Math.sin(yawRad) * Math.cos(pitchRad),
                -Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)
        ).normalize();
        Vec3 velocity = direction.scale(speed);

        Vec3 startPos = calculateStartPosition(
                config,
                velocity,
                lifetimeTicks,
                random.nextDouble(),
                random.nextDouble(),
                random.nextFloat());

        // --- visual dimensions ---
        float trailWidth = Math.max(0.28f, (0.52f + random.nextFloat() * 0.25f) * widthFactor);
        float tipLength = trailWidth * (3.1f + random.nextFloat() * 1.4f) + speed * (tipFactor * 0.28f);
        int segmentCount = Math.max(10, Math.min(26, Math.round(trailDuration * (0.82f + random.nextFloat() * 0.12f))));
        // Warm yellow-orange head (like a burning meteor entering the atmosphere) fading to
        // transparent deep orange at the tail.  varyColor() adds slight per-meteor variation
        // so no two streaks look identical.
        int headColor = varyColor(0xFFFFDD66, 0, 20, 30);
        int tailColor = 0x00FF6600;

        return new SkyMeteor(
                worldTime,
                lifetimeTicks,
                startPos,
                velocity,
                trailWidth,
                tipLength,
                segmentCount,
                headColor,
                tailColor,
                trailDuration);
    }

    /**
     * Reproduces the original shower field exactly: X and Z are sampled independently
     * across the full lane spread, while the path midpoint is centered on that sample.
     */
    static Vec3 calculateStartPosition(
            MeteorShowerConfig config,
            Vec3 velocity,
            int lifetimeTicks,
            double spreadSampleX,
            double spreadSampleZ,
            float heightSample) {
        double halfSpread = config.laneSpread();
        double midOffsetX = -velocity.x * lifetimeTicks * 0.5;
        double midOffsetZ = -velocity.z * lifetimeTicks * 0.5;
        double startX = midOffsetX + (spreadSampleX - 0.5) * halfSpread * 2.0;
        double startZ = midOffsetZ + (spreadSampleZ - 0.5) * halfSpread * 2.0;
        double startY = Math.max(
                MIN_CLEARANCE_ABOVE_ORIGIN,
                config.heightOffset() + (heightSample - 0.5f) * 20.0f);

        double endY = startY + velocity.y * lifetimeTicks;
        if (endY < MIN_CLEARANCE_ABOVE_ORIGIN) {
            startY += MIN_CLEARANCE_ABOVE_ORIGIN - endY;
        }

        return new Vec3(startX, startY, startZ);
    }

    private int varyColor(int color, int alphaVariance, int redVariance, int blueVariance) {
        int alpha = clampChannel((color >>> 24) + random.nextIntBetweenInclusive(-alphaVariance, alphaVariance));
        int red = clampChannel(((color >>> 16) & 255) + random.nextIntBetweenInclusive(-redVariance, redVariance));
        int green = clampChannel(((color >>> 8) & 255) + random.nextIntBetweenInclusive(-redVariance, redVariance));
        int blue = clampChannel((color & 255) + random.nextIntBetweenInclusive(-blueVariance, blueVariance));
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private int clampChannel(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
