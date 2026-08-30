package com.derko.prettymeteors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

class MeteorShowerConfigTest {
    @Test
    void factorySizesRetainExpectedDurationsAndRates() {
        RandomSource random = RandomSource.create(42L);
        MeteorShowerConfig small = MeteorShowerConfig.createSmall(100L, random, 1, 2, 3);
        MeteorShowerConfig medium = MeteorShowerConfig.createMedium(100L, random, 1, 2, 3);
        MeteorShowerConfig large = MeteorShowerConfig.createLarge(100L, random, 1, 2, 3);

        assertEquals(600, small.durationTicks());
        assertEquals(1200, medium.durationTicks());
        assertEquals(1800, large.durationTicks());
        assertEquals(0.8f, small.meteorsPerSecond());
        assertEquals(1.5f, medium.meteorsPerSecond());
        assertEquals(2.5f, large.meteorsPerSecond());
        assertEquals(1900L, large.endTick());
        assertFalse(large.isActiveAt(99L));
        assertTrue(large.isActiveAt(100L));
        assertFalse(large.isActiveAt(1900L));
    }

    @Test
    void fasterMeteorsReceiveShorterBoundedLifetimes() {
        MeteorShowerConfig config = MeteorShowerConfig.createLarge(0, RandomSource.create(7L), 0, 0, 0);
        int slow = config.lifetimeForSpeed(config.baseSpeed() - config.speedVariance());
        int fast = config.lifetimeForSpeed(config.baseSpeed() + config.speedVariance());

        assertEquals(config.maxLifetimeTicks(), slow);
        assertEquals(config.minLifetimeTicks(), fast);
        assertTrue(fast < slow);
    }

    @Test
    void legacySkyAnchorStaysHighAboveNormalTerrain() {
        assertEquals(292.0, MeteorShowerConfig.skyOriginY(64.0));
        assertEquals(292.0, MeteorShowerConfig.skyOriginY(142.0));
        assertEquals(350.0, MeteorShowerConfig.skyOriginY(200.0));
    }

    @Test
    void largeShowerRetainsOriginalScaleAndSpread() {
        MeteorShowerConfig config = MeteorShowerConfig.createLarge(0, RandomSource.create(99L), 0, 0, 0);

        assertEquals(230.0f, config.shellRadius());
        assertEquals(1100.0f, config.laneSpread());
        assertEquals(160.0f, config.heightOffset());
    }

    @Test
    void everyShowerSizeRetainsItsReferenceSpatialProfile() {
        RandomSource random = RandomSource.create(99L);
        MeteorShowerConfig single = MeteorShowerConfig.createSingle(0, random, 0, 0, 0);
        MeteorShowerConfig small = MeteorShowerConfig.createSmall(0, random, 0, 0, 0);
        MeteorShowerConfig medium = MeteorShowerConfig.createMedium(0, random, 0, 0, 0);
        MeteorShowerConfig large = MeteorShowerConfig.createLarge(0, random, 0, 0, 0);

        assertSpatialProfile(single, 80.0f, 30.0f, 130.0f);
        assertSpatialProfile(small, 120.0f, 400.0f, 130.0f);
        assertSpatialProfile(medium, 180.0f, 750.0f, 145.0f);
        assertSpatialProfile(large, 230.0f, 1100.0f, 160.0f);
    }

    private static void assertSpatialProfile(
            MeteorShowerConfig config,
            float shellRadius,
            float laneSpread,
            float heightOffset) {
        assertEquals(shellRadius, config.shellRadius());
        assertEquals(laneSpread, config.laneSpread());
        assertEquals(heightOffset, config.heightOffset());
    }
}
