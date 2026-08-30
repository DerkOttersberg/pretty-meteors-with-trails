package com.derko.prettymeteors.client;

import com.derko.prettymeteors.MeteorShowerConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

class SkyMeteorTest {
    @Test
    void positionAndLifetimeAreStableAcrossRenderInterpolation() {
        SkyMeteor meteor = new SkyMeteor(
                100L, 20, new Vec3(1, 2, 3), new Vec3(2, -1, 4),
                0.5f, 1.0f, 12, 0xFFFFFFFF, 0, 5.0f);

        assertEquals(new Vec3(11, -3, 23), meteor.positionAt(5.0f));
        assertEquals(5.5f, meteor.ageAt(105L, 0.5f));
        assertTrue(meteor.isAlive(120L));
        assertFalse(meteor.isAlive(121L));
        assertEquals(1.0, meteor.travelDirection().length(), 1.0E-9);
    }

    @Test
    void spawnPlacementUsesFullLaneSpreadInsteadOfShellRadius() {
        MeteorShowerConfig config = MeteorShowerConfig.createLarge(
                0L, RandomSource.create(1L), 0.0, 0.0, 0.0);

        Vec3 start = MeteorShowerClientState.calculateStartPosition(
                config,
                new Vec3(2.0, -1.0, 4.0),
                10,
                1.0,
                0.0,
                0.5f);

        assertEquals(1090.0, start.x, 1.0E-9);
        assertEquals(160.0, start.y, 1.0E-9);
        assertEquals(-1120.0, start.z, 1.0E-9);
    }

    @Test
    void spawnPlacementKeepsPathEndAboveMinimumClearance() {
        MeteorShowerConfig config = MeteorShowerConfig.createLarge(
                0L, RandomSource.create(1L), 0.0, 0.0, 0.0);

        Vec3 start = MeteorShowerClientState.calculateStartPosition(
                config,
                new Vec3(0.0, -10.0, 0.0),
                20,
                0.5,
                0.5,
                0.5f);

        assertEquals(288.0, start.y, 1.0E-9);
        assertEquals(88.0, start.y - 200.0, 1.0E-9);
    }

    @Test
    void distantMeteorProjectionPreservesAnglesInsideTheFarPlane() {
        float scale = SkyMeteorRenderer.skyDepthScale(2_000.0, 1_000.0f);

        assertEquals(0.41f, scale, 1.0E-6f);
        assertEquals(820.0, 2_000.0 * scale, 1.0E-3);
        assertEquals(1.0f, SkyMeteorRenderer.skyDepthScale(500.0, 1_000.0f));
    }

    @Test
    void meteorProjectionFallsBackSafelyBeforeCameraExtraction() {
        assertEquals(1.0f, SkyMeteorRenderer.skyDepthScale(500.0, 0.0f));
        assertEquals(1.0f, SkyMeteorRenderer.skyDepthScale(Double.NaN, 1_000.0f));
    }
}
