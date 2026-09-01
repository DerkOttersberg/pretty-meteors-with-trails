package com.derko.prettymeteors.client;

/**
 * Converts a per-second meteor rate into births without ever accumulating
 * missed world ticks. A discontinuity contributes at most the current live
 * tick, so pauses and lag spikes cannot create a catch-up burst.
 */
public final class MeteorSpawnBudget {
    private double fractionalCarry;
    private long lastWorldTime = Long.MIN_VALUE;

    public void rebase(long worldTime) {
        lastWorldTime = worldTime;
    }

    public void clear() {
        fractionalCarry = 0.0D;
        lastWorldTime = Long.MIN_VALUE;
    }

    public int advance(long worldTime, float meteorsPerSecond, boolean active, int availableSlots) {
        long elapsed = lastWorldTime == Long.MIN_VALUE ? 1L : worldTime - lastWorldTime;
        lastWorldTime = worldTime;

        if (!active || elapsed <= 0L) {
            return 0;
        }

        double rate = Float.isFinite(meteorsPerSecond) ? Math.max(0.0D, meteorsPerSecond) : 0.0D;
        fractionalCarry += rate / 20.0D;

        long wholeBirths = (long) Math.floor(fractionalCarry);
        fractionalCarry -= wholeBirths;
        if (wholeBirths <= 0L) {
            return 0;
        }

        int capacity = Math.max(0, availableSlots);
        return (int) Math.min(Math.min(wholeBirths, capacity), Integer.MAX_VALUE);
    }

    double fractionalCarry() {
        return fractionalCarry;
    }

    long lastWorldTime() {
        return lastWorldTime;
    }
}
