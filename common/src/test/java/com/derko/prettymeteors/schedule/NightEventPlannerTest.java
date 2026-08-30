package com.derko.prettymeteors.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.derko.prettymeteors.schedule.NightEventPlanner.Chances;
import com.derko.prettymeteors.schedule.NightEventPlanner.ShowerType;
import java.util.Random;
import org.junit.jupiter.api.Test;

class NightEventPlannerTest {
    private static final Chances DEFAULTS = new Chances(70, 21, 6, 3);

    @Test
    void exactProbabilityBoundariesSelectExpectedShower() {
        assertTrue(NightEventPlanner.chooseShower(69, DEFAULTS).isEmpty());
        assertEquals(ShowerType.SMALL, NightEventPlanner.chooseShower(70, DEFAULTS).orElseThrow());
        assertEquals(ShowerType.SMALL, NightEventPlanner.chooseShower(90, DEFAULTS).orElseThrow());
        assertEquals(ShowerType.MEDIUM, NightEventPlanner.chooseShower(91, DEFAULTS).orElseThrow());
        assertEquals(ShowerType.MEDIUM, NightEventPlanner.chooseShower(96, DEFAULTS).orElseThrow());
        assertEquals(ShowerType.LARGE, NightEventPlanner.chooseShower(97, DEFAULTS).orElseThrow());
        assertEquals(ShowerType.LARGE, NightEventPlanner.chooseShower(99, DEFAULTS).orElseThrow());
    }

    @Test
    void invalidConfigurationAndRollsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Chances(70, 20, 6, 3));
        assertThrows(IllegalArgumentException.class, () -> NightEventPlanner.chooseShower(-1, DEFAULTS));
        assertThrows(IllegalArgumentException.class, () -> NightEventPlanner.chooseShower(100, DEFAULTS));
    }

    @Test
    void generatedDelaysStayInsideNightWindows() {
        Random random = new Random(123L);
        for (int i = 0; i < 1000; i++) {
            long star = NightEventPlanner.starDelay(random, 10_000L);
            long shower = NightEventPlanner.showerDelay(random);
            assertTrue(star >= 0L && star < 9_900L);
            assertTrue(shower >= 1_500L && shower < 7_500L);
        }
    }
}
