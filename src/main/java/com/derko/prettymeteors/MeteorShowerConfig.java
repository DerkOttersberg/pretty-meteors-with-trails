package com.derko.prettymeteors;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

public record MeteorShowerConfig(
        long startTick,
        int durationTicks,
        float meteorsPerSecond,
        float yawDegrees,
        float pitchDegrees,
        float angularSpreadDegrees,
        float shellRadius,
        float laneSpread,
        float heightOffset,
        float baseSpeed,
        float speedVariance,
        int minLifetimeTicks,
        int maxLifetimeTicks,
        double originX,
        double originY,
        double originZ,
        int seed) {

    /** Alias for {@link #createLarge} — kept for compatibility. */
    public static MeteorShowerConfig createDefault(long startTick, Random random, double originX, double originY, double originZ) {
        return createLarge(startTick, random, originX, originY, originZ);
    }

    /** Large shower: 90 s, 2.5 meteors/sec, wide spread. */
    public static MeteorShowerConfig createLarge(long startTick, Random random, double originX, double originY, double originZ) {
        return new MeteorShowerConfig(
                startTick,
                90 * 20,
                2.5f,
                -45.0f + (random.nextFloat() - 0.5f) * 10.0f,
                5.0f + (random.nextFloat() - 0.5f) * 3.0f,
                1.4f,
                230.0f,
                1100.0f,
                160.0f,
                15.0f,
                3.0f,
                70,
                220,
                originX, originY, originZ,
                random.nextInt());
    }

    /** Medium shower: 60 s, 1.5 meteors/sec, moderate spread. */
    public static MeteorShowerConfig createMedium(long startTick, Random random, double originX, double originY, double originZ) {
        return new MeteorShowerConfig(
                startTick,
                60 * 20,
                1.5f,
                -45.0f + (random.nextFloat() - 0.5f) * 10.0f,
                5.0f + (random.nextFloat() - 0.5f) * 3.0f,
                1.2f,
                180.0f,
                750.0f,
                145.0f,
                14.0f,
                2.5f,
                65,
                200,
                originX, originY, originZ,
                random.nextInt());
    }

    /** Small shower: 30 s, 0.8 meteors/sec, narrow spread. */
    public static MeteorShowerConfig createSmall(long startTick, Random random, double originX, double originY, double originZ) {
        return new MeteorShowerConfig(
                startTick,
                30 * 20,
                0.8f,
                -45.0f + (random.nextFloat() - 0.5f) * 10.0f,
                5.0f + (random.nextFloat() - 0.5f) * 3.0f,
                0.9f,
                120.0f,
                400.0f,
                130.0f,
                13.0f,
                2.0f,
                55,
                180,
                originX, originY, originZ,
                random.nextInt());
    }

    /**
     * Single shooting star: lasts 45 ticks, spawns exactly 1 meteor
     * (0.8 meteors/sec × 45 ticks = 1.8 accumulated; fires at tick 25, then only
     * 0.8 more accumulates before the 45-tick window closes).
     */
    public static MeteorShowerConfig createSingle(long startTick, Random random, double originX, double originY, double originZ) {
        return new MeteorShowerConfig(
                startTick,
                45,
                0.8f,
                (random.nextFloat() - 0.5f) * 360.0f,          // any direction
                5.0f + (random.nextFloat() - 0.5f) * 4.0f,
                0.3f,
                80.0f,
                30.0f,
                130.0f,
                10.0f + random.nextFloat() * 4.0f,
                1.5f,
                60,
                180,
                originX, originY, originZ,
                random.nextInt());
    }

    public long endTick() {
        return startTick + durationTicks;
    }

    public boolean isActiveAt(long worldTime) {
        return worldTime >= startTick && worldTime < endTick();
    }

    public int lifetimeForSpeed(float speed) {
        float minSpeed = Math.max(0.05f, baseSpeed - speedVariance);
        float maxSpeed = baseSpeed + speedVariance;
        float normalized = MathHelper.clamp((speed - minSpeed) / Math.max(0.001f, maxSpeed - minSpeed), 0.0f, 1.0f);
        float curved = normalized * normalized;
        return Math.round(MathHelper.lerp(curved, maxLifetimeTicks, minLifetimeTicks));
    }
}