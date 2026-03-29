package com.derko.prettymeteors;

import com.derko.prettymeteors.command.PrettyMeteorsCommands;
import com.derko.prettymeteors.network.MeteorShowerPayload;
import com.derko.seamlessapi.api.meteor.MeteorShowerAPI;
import com.derko.seamlessapi.api.meteor.MeteorShowerRegistration;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PrettyMeteorsMod implements ModInitializer {
    public static final String MOD_ID = "prettymeteors";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // ---- Active showers ----
    private static final Map<RegistryKey<World>, MeteorShowerConfig> ACTIVE_SHOWERS = new HashMap<>();

    // ---- Nightly event configuration (changed via commands) ----
    public static boolean nightEventsEnabled = true;
    public static int nightStarCount = 5;
    public static int nightNoneChance   = 70;   // out of 100 — probability of NO shower
    public static int nightSmallChance   = 21;
    public static int nightMediumChance  = 6;
    public static int nightLargeChance   = 3;

    // ---- Nightly event scheduling ----
    public enum ShowerType { SINGLE, SMALL, MEDIUM, LARGE }

    private record ScheduledEvent(long fireTick, ShowerType type) {}

    /** Per-world queue of events to fire, sorted ascending by fireTick. */
    private static final Map<RegistryKey<World>, List<ScheduledEvent>> EVENT_QUEUE = new HashMap<>();

    /**
     * Day number (worldTime / 24000) for which nightly events have already been
     * scheduled, per world.  Prevents double-scheduling within the same Minecraft night.
     */
    private static final Map<RegistryKey<World>, Long> LAST_NIGHT_DAY = new HashMap<>();

    // ---- Mod lifecycle ----

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(MeteorShowerPayload.ID, MeteorShowerPayload.CODEC);
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> PrettyMeteorsCommands.register(dispatcher));
        ServerTickEvents.END_WORLD_TICK.register(PrettyMeteorsMod::tickWorld);

        // Register this mod as the MeteorShowerAPI implementation so other mods can trigger showers.
        MeteorShowerAPI.registerImplementation(
                PrettyMeteorsMod::startShowerFromRegistration,
                PrettyMeteorsMod::stopShower
        );
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    // ---- Public shower API ----

    public static void startShower(ServerWorld world, MeteorShowerConfig config) {
        ACTIVE_SHOWERS.put(world.getRegistryKey(), config);
        broadcastState(world, MeteorShowerPayload.fromConfig(config));
    }

    public static void stopShower(ServerWorld world) {
        ACTIVE_SHOWERS.remove(world.getRegistryKey());
        broadcastState(world, MeteorShowerPayload.inactive());
    }

    /**
     * Returns the active shower config for the world, or null if none / expired.
     * Automatically removes expired entries.
     */
    public static MeteorShowerConfig getActiveShower(ServerWorld world) {
        RegistryKey<World> worldKey = world.getRegistryKey();
        MeteorShowerConfig config = ACTIVE_SHOWERS.get(worldKey);
        if (config == null) return null;
        if (world.getTime() >= config.endTick()) {
            ACTIVE_SHOWERS.remove(worldKey);
            return null;
        }
        return config;
    }

    // ---- Server tick ----

    private static void tickWorld(ServerWorld world) {
        // 1. Process the nightly event queue (fires queued events when the slot is free).
        processEventQueue(world);

        // 2. Broadcast the active shower state periodically so late-joining players see it.
        MeteorShowerConfig config = getActiveShower(world);
        if (config != null && (world.getTime() == config.startTick() || world.getTime() % 40L == 0L)) {
            broadcastState(world, MeteorShowerPayload.fromConfig(config));
        }

        // 3. Schedule nightly events, but only in the overworld.
        if (nightEventsEnabled && world.getRegistryKey().equals(World.OVERWORLD)) {
            checkAndScheduleNightEvents(world);
        }
    }

    // ---- Nightly event scheduling ----

    /**
     * Called every tick in the overworld.  Schedules this night's events the moment
     * the Minecraft time-of-day crosses 13 000 (nightfall), once per in-game day.
     */
    private static void checkAndScheduleNightEvents(ServerWorld world) {
        long worldTime  = world.getTime();
        long timeOfDay  = worldTime % 24000L;
        long dayNumber  = worldTime / 24000L;

        // Only act during the first 100 ticks after nightfall (13 000 … 13 099).
        // This window is wide enough to survive a single-tick skip but not so wide
        // that it fires again on the same night after a /time command.
        if (timeOfDay < 13000L || timeOfDay >= 13100L) return;

        Long lastDay = LAST_NIGHT_DAY.get(world.getRegistryKey());
        if (lastDay != null && lastDay >= dayNumber) return;   // already scheduled tonight

        List<ServerPlayerEntity> players = world.getPlayers();
        if (players.isEmpty()) return;   // nobody online — wait until there is

        LAST_NIGHT_DAY.put(world.getRegistryKey(), dayNumber);

        List<ScheduledEvent> queue =
                EVENT_QUEUE.computeIfAbsent(world.getRegistryKey(), k -> new ArrayList<>());
        // Drop stale events from a previous night that never fired.
        queue.removeIf(e -> e.fireTick() < worldTime - 200L);

        // Night window: timeOfDay 13 000 → 23 000 = 10 000 ticks remaining this night.
        long windowSize = 23000L - timeOfDay;   // ticks until end-of-night

        java.util.Random rng = new java.util.Random(worldTime);

        // Schedule nightStarCount individual shooting stars at random times tonight.
        for (int i = 0; i < nightStarCount; i++) {
            long delay = (long) (rng.nextDouble() * (windowSize - 100L));
            queue.add(new ScheduledEvent(worldTime + delay, ShowerType.SINGLE));
        }

        // Roll to decide if any shower happens tonight.
        int roll = rng.nextInt(100);
        if (roll < nightNoneChance) {
            // No shower tonight — just the shooting stars.
            queue.sort(Comparator.comparingLong(ScheduledEvent::fireTick));
            LOGGER.info("[PrettyMeteors] Night {}: scheduled {} shooting star(s), no shower tonight.",
                    dayNumber, nightStarCount);
        } else {
            long showerDelay = 1500L + (long) (rng.nextDouble() * 6000L);
            ShowerType showerType;
            int showerRoll = roll - nightNoneChance; // 0..29 in default config
            if (showerRoll < nightSmallChance) {
                showerType = ShowerType.SMALL;
            } else if (showerRoll < nightSmallChance + nightMediumChance) {
                showerType = ShowerType.MEDIUM;
            } else {
                showerType = ShowerType.LARGE;
            }
            queue.add(new ScheduledEvent(worldTime + showerDelay, showerType));
            queue.sort(Comparator.comparingLong(ScheduledEvent::fireTick));
            LOGGER.info("[PrettyMeteors] Night {}: scheduled {} shooting star(s) + 1 {} shower.",
                    dayNumber, nightStarCount, showerType.name().toLowerCase());
        }
    }

    /**
     * Checks the event queue and fires the next due event, but only when
     * no shower is currently active (so manual showers are never interrupted).
     * Stale events (fired more than 1 minute late) are silently dropped.
     */
    private static void processEventQueue(ServerWorld world) {
        List<ScheduledEvent> queue = EVENT_QUEUE.get(world.getRegistryKey());
        if (queue == null || queue.isEmpty()) return;

        long worldTime = world.getTime();

        // Drop events that are more than 1 minute (1 200 ticks) overdue.
        queue.removeIf(e -> e.fireTick() < worldTime - 1200L);

        // Only fire when no shower is running.
        MeteorShowerConfig current = ACTIVE_SHOWERS.get(world.getRegistryKey());
        if (current != null && worldTime < current.endTick()) return;

        if (!queue.isEmpty()) {
            ScheduledEvent next = queue.get(0);
            if (next.fireTick() <= worldTime) {
                queue.remove(0);
                fireScheduledEvent(world, next.type());
            }
        }
    }

    /** Fires a scheduled event by creating the appropriate config and starting the shower. */
    private static void fireScheduledEvent(ServerWorld world, ShowerType type) {
        List<ServerPlayerEntity> players = world.getPlayers();
        if (players.isEmpty()) return;

        // Re-target a random player at fire time so the position is current.
        ServerPlayerEntity target = players.get(world.getRandom().nextInt(players.size()));
        double posX = target.getX();
        double posY = target.getY();
        double posZ = target.getZ();
        double originY = Math.max(posY + 150.0, 292.0);
        long worldTime = world.getTime();

        MeteorShowerConfig config = switch (type) {
            case SINGLE -> MeteorShowerConfig.createSingle(worldTime, world.getRandom(), posX, originY, posZ);
            case SMALL  -> MeteorShowerConfig.createSmall(worldTime, world.getRandom(), posX, originY, posZ);
            case MEDIUM -> MeteorShowerConfig.createMedium(worldTime, world.getRandom(), posX, originY, posZ);
            case LARGE  -> MeteorShowerConfig.createLarge(worldTime, world.getRandom(), posX, originY, posZ);
        };
        startShower(world, config);
    }

    // ---- Helpers ----

    /**
     * Bridge from MeteorShowerAPI delegate calls to internal shower logic.
     * Picks a random online player as the origin, then dispatches to {@link #startShower}.
     */
    private static void startShowerFromRegistration(ServerWorld world, MeteorShowerRegistration reg) {
        java.util.List<ServerPlayerEntity> players = world.getPlayers();
        if (players.isEmpty()) return;

        ServerPlayerEntity target = players.get(world.getRandom().nextInt(players.size()));
        double posX = target.getX();
        double posY = target.getY();
        double posZ = target.getZ();
        double originY = Math.max(posY + 150.0, 292.0);
        long worldTime = world.getTime();

        MeteorShowerConfig config = switch (reg.size()) {
            case LARGE  -> buildConfig(MeteorShowerRegistration.ShowerSize.LARGE,  reg, worldTime, world, posX, originY, posZ);
            case MEDIUM -> buildConfig(MeteorShowerRegistration.ShowerSize.MEDIUM, reg, worldTime, world, posX, originY, posZ);
            case SMALL  -> buildConfig(MeteorShowerRegistration.ShowerSize.SMALL,  reg, worldTime, world, posX, originY, posZ);
            case SINGLE -> MeteorShowerConfig.createSingle(worldTime, world.getRandom(), posX, originY, posZ);
        };
        startShower(world, config);
    }

    /** Apply any per-field overrides from a {@link MeteorShowerRegistration} on top of the size default. */
    private static MeteorShowerConfig buildConfig(MeteorShowerRegistration.ShowerSize size,
                                                   MeteorShowerRegistration reg,
                                                   long worldTime,
                                                   ServerWorld world,
                                                   double posX, double originY, double posZ) {
        MeteorShowerConfig base = switch (size) {
            case LARGE  -> MeteorShowerConfig.createLarge (worldTime, world.getRandom(), posX, originY, posZ);
            case MEDIUM -> MeteorShowerConfig.createMedium(worldTime, world.getRandom(), posX, originY, posZ);
            case SMALL  -> MeteorShowerConfig.createSmall (worldTime, world.getRandom(), posX, originY, posZ);
            case SINGLE -> MeteorShowerConfig.createSingle(worldTime, world.getRandom(), posX, originY, posZ);
        };

        int durationTicks = reg.durationSeconds() > 0 ? reg.durationSeconds() * 20 : base.durationTicks();
        float rate = reg.meteorsPerSecond() > 0 ? reg.meteorsPerSecond() : base.meteorsPerSecond();
        float spread = reg.angularSpreadDegrees() > 0 ? reg.angularSpreadDegrees() : base.angularSpreadDegrees();

        return new MeteorShowerConfig(
                base.startTick(), durationTicks, rate,
                base.yawDegrees(), base.pitchDegrees(), spread,
                base.shellRadius(), base.laneSpread(), base.heightOffset(),
                base.baseSpeed(), base.speedVariance(),
                base.minLifetimeTicks(), base.maxLifetimeTicks(),
                base.originX(), base.originY(), base.originZ(),
                base.seed());
    }

    private static void broadcastState(ServerWorld world, MeteorShowerPayload payload) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
