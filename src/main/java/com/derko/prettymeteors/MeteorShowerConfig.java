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

    public static MeteorShowerConfig createDefault(long startTick, Random random, double originX, double originY, double originZ) {
        return new MeteorShowerConfig(
                startTick,
                90 * 20,
                2.5f,
                -45.0f + (random.nextFloat() - 0.5f) * 10.0f, // yaw: diagonal NW→SE
                5.0f + (random.nextFloat() - 0.5f) * 3.0f,    // pitch: very shallow fall
                1.4f,       // angularSpreadDegrees (×15 in spawn = ±10.5° direction spread)
                230.0f,     // shellRadius (unused in straight-line model, kept for compat)
                1100.0f,    // laneSpread: spawn ±1100 blocks in X/Z around origin
                160.0f,     // heightOffset: spawn altitude above shower origin
                15.0f,      // baseSpeed: fast streaks
                3.0f,       // speedVariance
                70,
                220,
                originX,
                originY,
                originZ,
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