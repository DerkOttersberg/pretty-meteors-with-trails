package com.derko.prettymeteors.client;

import net.minecraft.util.math.Vec3d;

/**
 * A single straight-line meteor. startPos is the spawn offset relative to the
 * shower's world-space origin. velocity is in blocks-per-tick. The path is a
 * simple linear function: position(age) = startPos + velocity * age.
 */
public final class SkyMeteor {
    private final long birthTick;
    private final int lifetimeTicks;
    private final Vec3d startPos;
    private final Vec3d velocity;
    private final float trailWidth;
    private final float tipLength;
    private final int segmentCount;
    private final int headColor;
    private final int tailColor;
    private final float trailDurationTicks;

    public SkyMeteor(long birthTick, int lifetimeTicks, Vec3d startPos, Vec3d velocity,
                     float trailWidth, float tipLength, int segmentCount,
                     int headColor, int tailColor, float trailDurationTicks) {
        this.birthTick = birthTick;
        this.lifetimeTicks = lifetimeTicks;
        this.startPos = startPos;
        this.velocity = velocity;
        this.trailWidth = trailWidth;
        this.tipLength = tipLength;
        this.segmentCount = segmentCount;
        this.headColor = headColor;
        this.tailColor = tailColor;
        this.trailDurationTicks = trailDurationTicks;
    }

    public float ageAt(long worldTime, float tickDelta) {
        return (worldTime - birthTick) + tickDelta;
    }

    public boolean isAlive(long worldTime) {
        return worldTime - birthTick <= lifetimeTicks;
    }

    /** Position offset from shower origin (world-space). Perfectly straight. */
    public Vec3d positionAt(float age) {
        return startPos.add(velocity.multiply(age));
    }

    /** Constant travel direction derived from velocity. */
    public Vec3d travelDirection() {
        double len = velocity.length();
        if (len < 1.0E-7) return new Vec3d(1.0, 0.0, 0.0);
        return velocity.multiply(1.0 / len);
    }

    public float trailWidth() { return trailWidth; }
    public float tipLength() { return tipLength; }
    public int segmentCount() { return segmentCount; }
    public int headColor() { return headColor; }
    public int tailColor() { return tailColor; }
    public float trailDurationTicks() { return trailDurationTicks; }
    public int lifetimeTicks() { return lifetimeTicks; }
}