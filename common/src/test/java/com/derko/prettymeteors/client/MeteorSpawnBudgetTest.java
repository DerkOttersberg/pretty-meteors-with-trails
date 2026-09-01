package com.derko.prettymeteors.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MeteorSpawnBudgetTest {
    @Test
    void normalTicksPreserveFractionalRate() {
        MeteorSpawnBudget budget = new MeteorSpawnBudget();
        int births = 0;
        for (long tick = 1; tick <= 20; tick++) {
            births += budget.advance(tick, 2.5F, true, 320);
        }

        assertEquals(2, births);
        assertEquals(0.5D, budget.fractionalCarry(), 1.0E-9D);
    }

    @Test
    void longPauseContributesOnlyCurrentTick() {
        MeteorSpawnBudget budget = new MeteorSpawnBudget();
        for (long tick = 1; tick <= 7; tick++) {
            assertEquals(0, budget.advance(tick, 2.5F, true, 320));
        }

        assertEquals(1, budget.advance(1_207L, 2.5F, true, 320));
        assertEquals(0.0D, budget.fractionalCarry(), 1.0E-9D);
    }

    @Test
    void duplicateAndRewoundTimeDoNotAccrue() {
        MeteorSpawnBudget budget = new MeteorSpawnBudget();
        assertEquals(0, budget.advance(10L, 10.0F, true, 320));
        assertEquals(0, budget.advance(10L, 10.0F, true, 320));
        assertEquals(0, budget.advance(4L, 10.0F, true, 320));
        assertEquals(1, budget.advance(5L, 10.0F, true, 320));
    }

    @Test
    void inactiveTicksRebaseWithoutBacklog() {
        MeteorSpawnBudget budget = new MeteorSpawnBudget();
        assertEquals(0, budget.advance(1L, 20.0F, false, 320));
        assertEquals(0, budget.advance(1_201L, 20.0F, false, 320));
        assertEquals(1, budget.advance(1_202L, 20.0F, true, 320));
    }

    @Test
    void capacityCapDiscardsWholeOverflowButKeepsFraction() {
        MeteorSpawnBudget budget = new MeteorSpawnBudget();
        assertEquals(2, budget.advance(1L, 65.0F, true, 2));
        assertEquals(0.25D, budget.fractionalCarry(), 1.0E-9D);
        assertEquals(0, budget.advance(2L, 0.0F, true, 320));
        assertEquals(0.25D, budget.fractionalCarry(), 1.0E-9D);
    }

    @Test
    void fullCapacityStillAdvancesFractionWithoutDeferringWholeBirths() {
        MeteorSpawnBudget budget = new MeteorSpawnBudget();
        assertEquals(0, budget.advance(1L, 25.0F, true, 0));
        assertEquals(0.25D, budget.fractionalCarry(), 1.0E-9D);
        assertEquals(1, budget.advance(2L, 15.0F, true, 320));
        assertEquals(0.0D, budget.fractionalCarry(), 1.0E-9D);
    }

    @Test
    void maximumFiniteRateDiscardsAllIntegralOverflow() {
        MeteorSpawnBudget budget = new MeteorSpawnBudget();

        assertEquals(2, budget.advance(1L, Float.MAX_VALUE, true, 2));
        assertEquals(0.0D, budget.fractionalCarry(), 0.0D);
        assertEquals(0, budget.advance(2L, 0.0F, true, 320));
    }

    @Test
    void clearRemovesClockAndCarry() {
        MeteorSpawnBudget budget = new MeteorSpawnBudget();
        budget.advance(50L, 19.0F, true, 320);
        budget.clear();

        assertEquals(Long.MIN_VALUE, budget.lastWorldTime());
        assertEquals(0.0D, budget.fractionalCarry(), 0.0D);
    }
}
