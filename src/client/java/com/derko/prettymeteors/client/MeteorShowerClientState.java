package com.derko.prettymeteors.client;

import com.derko.prettymeteors.MeteorShowerConfig;
import com.derko.prettymeteors.network.MeteorShowerPayload;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public final class MeteorShowerClientState {
    public static final MeteorShowerClientState INSTANCE = new MeteorShowerClientState();

    private static final double MIN_CLEARANCE_ABOVE_ORIGIN = 88.0;

    private final List<SkyMeteor> meteors = new ArrayList<>();

    private MeteorShowerConfig activeConfig;
    private RegistryKey<World> activeWorldKey;
    private RegistryKey<World> lastSeenWorldKey;
    private Vec3d showerOrigin;
    private Random random = Random.create();
    private double spawnAccumulator;
    private long lastWorldTime = Long.MIN_VALUE;

    private MeteorShowerClientState() {
    }

    public void applyPayload(MinecraftClient client, MeteorShowerPayload payload) {
        if (client.world == null) {
            clearAll();
            return;
        }

        lastSeenWorldKey = client.world.getRegistryKey();

        if (!payload.active()) {
            activeConfig = null;
            activeWorldKey = client.world.getRegistryKey();
            spawnAccumulator = 0.0;
            return;
        }

        MeteorShowerConfig incomingConfig = payload.toConfig();
        RegistryKey<World> incomingWorldKey = client.world.getRegistryKey();

        if (incomingConfig.equals(activeConfig) && incomingWorldKey.equals(activeWorldKey)) {
            return;
        }

        activeConfig = incomingConfig;
        activeWorldKey = incomingWorldKey;
        showerOrigin = new Vec3d(incomingConfig.originX(), incomingConfig.originY(), incomingConfig.originZ());
        random = Random.create(incomingConfig.seed());
        spawnAccumulator = 0.0;
        lastWorldTime = client.world.getTime();
        meteors.clear();
    }

    public void tick(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            clearAll();
            return;
        }

        RegistryKey<World> worldKey = client.world.getRegistryKey();

        if (lastSeenWorldKey != null && !lastSeenWorldKey.equals(worldKey)) {
            clearAll();
        }

        lastSeenWorldKey = worldKey;

        long worldTime = client.world.getTime();
        meteors.removeIf(meteor -> !meteor.isAlive(worldTime));

        if (activeConfig == null) {
            lastWorldTime = worldTime;
            return;
        }

        if (!worldKey.equals(activeWorldKey)) {
            clearAll();
            return;
        }

        if (worldTime >= activeConfig.endTick()) {
            activeConfig = null;
            spawnAccumulator = 0.0;
            lastWorldTime = worldTime;
            return;
        }

        int elapsedTicks = lastWorldTime == Long.MIN_VALUE ? 1 : (int) Math.max(1L, worldTime - lastWorldTime);
        lastWorldTime = worldTime;

        if (!activeConfig.isActiveAt(worldTime)) {
            return;
        }

        spawnAccumulator += activeConfig.meteorsPerSecond() * elapsedTicks / 20.0;
        while (spawnAccumulator >= 1.0) {
            if (meteors.size() < 320) {
                meteors.add(createMeteor(worldTime, activeConfig));
            }
            spawnAccumulator -= 1.0;
        }
    }

    public void renderWorldPass(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || meteors.isEmpty()) {
            return;
        }

        float tickDelta = client.getRenderTickCounter().getTickProgress(true);
        if (showerOrigin == null) {
            return;
        }

        SkyMeteorRenderer.render(context.matrices().peek().getPositionMatrix(), meteors, showerOrigin, client.world.getTime(), tickDelta);
    }

    public void clearAll() {
        meteors.clear();
        activeConfig = null;
        activeWorldKey = null;
        lastSeenWorldKey = null;
        showerOrigin = null;
        spawnAccumulator = 0.0;
        lastWorldTime = Long.MIN_VALUE;
        random = Random.create();
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
        float pitchDeg = MathHelper.clamp(
                config.pitchDegrees() + (random.nextFloat() - 0.5f) * 3.0f,
                2.0f, 18.0f);
        double yawRad = Math.toRadians(yawDeg);
        double pitchRad = Math.toRadians(pitchDeg);
        Vec3d direction = new Vec3d(
                -Math.sin(yawRad) * Math.cos(pitchRad),
                -Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)
        ).normalize();
        Vec3d velocity = direction.multiply(speed);

        // --- spawn position: spread out in X/Z, fixed altitude above shower origin ---
        // Back-offset by half the lifetime travel so the MIDPOINT of each meteor's path
        // is centered on the shower origin instead of starting there and flying to one side.
        double halfSpread = config.laneSpread();
        double midOffsetX = -velocity.x * lifetimeTicks * 0.5;
        double midOffsetZ = -velocity.z * lifetimeTicks * 0.5;
        double startX = midOffsetX + (random.nextDouble() - 0.5) * halfSpread * 2.0;
        double startZ = midOffsetZ + (random.nextDouble() - 0.5) * halfSpread * 2.0;
        double startY = Math.max(MIN_CLEARANCE_ABOVE_ORIGIN,
                config.heightOffset() + (random.nextFloat() - 0.5f) * 20.0f);

        // Ensure the END of the path is also above minimum clearance.
        double endY = startY + velocity.y * lifetimeTicks;
        if (endY < MIN_CLEARANCE_ABOVE_ORIGIN) {
            startY += (MIN_CLEARANCE_ABOVE_ORIGIN - endY);
        }

        Vec3d startPos = new Vec3d(startX, startY, startZ);

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

    private int varyColor(int color, int alphaVariance, int redVariance, int blueVariance) {
        int alpha = clampChannel((color >>> 24) + random.nextBetween(-alphaVariance, alphaVariance));
        int red = clampChannel(((color >>> 16) & 255) + random.nextBetween(-redVariance, redVariance));
        int green = clampChannel(((color >>> 8) & 255) + random.nextBetween(-redVariance, redVariance));
        int blue = clampChannel((color & 255) + random.nextBetween(-blueVariance, blueVariance));
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private int clampChannel(int value) {
        return Math.max(0, Math.min(255, value));
    }
}