package com.derko.prettymeteors.schedule;

import java.util.Optional;
import java.util.Random;

/** Pure scheduling math shared by all loader adapters and covered by unit tests. */
public final class NightEventPlanner {
    public enum ShowerType { SINGLE, SMALL, MEDIUM, LARGE }

    public record Chances(int none, int small, int medium, int large) {
        public Chances {
            if (none < 0 || small < 0 || medium < 0 || large < 0) {
                throw new IllegalArgumentException("Meteor chances cannot be negative");
            }
            if (none + small + medium + large != 100) {
                throw new IllegalArgumentException("Meteor chances must add up to 100");
            }
        }
    }

    private NightEventPlanner() {
    }

    public static Optional<ShowerType> chooseShower(int roll, Chances chances) {
        if (roll < 0 || roll >= 100) {
            throw new IllegalArgumentException("roll must be between 0 and 99");
        }
        if (roll < chances.none()) {
            return Optional.empty();
        }
        int showerRoll = roll - chances.none();
        if (showerRoll < chances.small()) {
            return Optional.of(ShowerType.SMALL);
        }
        if (showerRoll < chances.small() + chances.medium()) {
            return Optional.of(ShowerType.MEDIUM);
        }
        return Optional.of(ShowerType.LARGE);
    }

    public static long starDelay(Random random, long remainingNightTicks) {
        if (remainingNightTicks <= 100L) {
            throw new IllegalArgumentException("At least 101 night ticks must remain");
        }
        return (long) (random.nextDouble() * (remainingNightTicks - 100L));
    }

    public static long showerDelay(Random random) {
        return 1500L + (long) (random.nextDouble() * 6000L);
    }
}
