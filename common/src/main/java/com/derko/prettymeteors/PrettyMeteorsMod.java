package com.derko.prettymeteors;

import io.github.derkottersberg.prettymeteors.internal.PlatformServices;
import com.derko.prettymeteors.network.MeteorShowerPayload;
import com.derko.prettymeteors.schedule.NightEventPlanner;
import com.derko.prettymeteors.schedule.NightEventPlanner.ShowerType;
import com.derko.seamlessapi.api.meteor.MeteorShowerAPI;
import com.derko.seamlessapi.api.meteor.MeteorShowerRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PrettyMeteorsMod {
    public static final String MOD_ID = "prettymeteors";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // ---- Active showers ----
    private static final Map<ResourceKey<Level>, MeteorShowerConfig> ACTIVE_SHOWERS = new HashMap<>();

    // ---- Nightly event scheduling ----
    private record ScheduledEvent(long fireTick, ShowerType type) {}

    /** Per-world queue of events to fire, sorted ascending by fireTick. */
    private static final Map<ResourceKey<Level>, List<ScheduledEvent>> EVENT_QUEUE = new HashMap<>();

    /**
     * Day number (worldTime / 24000) for which nightly events have already been
     * scheduled, per world.  Prevents double-scheduling within the same Minecraft night.
     */
    private static final Map<ResourceKey<Level>, Long> LAST_NIGHT_DAY = new HashMap<>();
    private static PlatformServices platform;

    // ---- Mod lifecycle ----

    public static void initialize(PlatformServices services) {
        if (platform != null) {
            throw new IllegalStateException("Pretty Meteors has already been initialized");
        }
        platform = Objects.requireNonNull(services, "services");
        PrettyMeteorsConfig.load(platform.configDirectory());
        MeteorShowerAPI.registerImplementation(
                PrettyMeteorsMod::startShowerFromRegistration,
                PrettyMeteorsMod::stopShower
        );
        LOGGER.info("Pretty Meteors initialized on {}", platform.loaderName());
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    // ---- Public shower API ----

    public static void startShower(ServerLevel world, MeteorShowerConfig config) {
        ACTIVE_SHOWERS.put(world.dimension(), config);
        broadcastState(world, MeteorShowerPayload.fromConfig(config));
    }

    public static void stopShower(ServerLevel world) {
        ACTIVE_SHOWERS.remove(world.dimension());
        broadcastState(world, MeteorShowerPayload.inactive());
    }

    /** Returns the exact state a player entering this dimension must receive. */
    public static MeteorShowerPayload statePayload(ServerLevel world) {
        MeteorShowerConfig config = getActiveShower(world);
        return config != null ? MeteorShowerPayload.fromConfig(config) : MeteorShowerPayload.inactive();
    }

    /** Immediately synchronizes a joining player or a player that changed dimensions. */
    public static void syncPlayer(ServerPlayer player) {
        requirePlatform().sendToPlayer(player, statePayload(player.serverLevel()));
    }

    /**
     * Returns the active shower config for the world, or null if none / expired.
     * Automatically removes expired entries.
     */
    public static MeteorShowerConfig getActiveShower(ServerLevel world) {
        ResourceKey<Level> worldKey = world.dimension();
        MeteorShowerConfig config = ACTIVE_SHOWERS.get(worldKey);
        if (config == null) return null;
        if (world.getGameTime() >= config.endTick()) {
            ACTIVE_SHOWERS.remove(worldKey);
            return null;
        }
        return config;
    }

    // ---- Server tick ----

    public static void tickWorld(ServerLevel world) {
        // 1. Process the nightly event queue (fires queued events when the slot is free).
        processEventQueue(world);

        // 2. Broadcast the active shower state periodically so late-joining players see it.
        MeteorShowerConfig config = getActiveShower(world);
        if (config != null && (world.getGameTime() == config.startTick() || world.getGameTime() % 40L == 0L)) {
            broadcastState(world, MeteorShowerPayload.fromConfig(config));
        }

        // 3. Schedule nightly events, but only in the overworld.
        PrettyMeteorsConfig.Settings settings = PrettyMeteorsConfig.snapshot();
        if (settings.nightEventsEnabled() && world.dimension().equals(Level.OVERWORLD)) {
            checkAndScheduleNightEvents(world, settings);
        }
    }

    // ---- Nightly event scheduling ----

    /**
     * Called every tick in the overworld.  Schedules this night's events the moment
     * the Minecraft time-of-day crosses 13 000 (nightfall), once per in-game day.
     */
    private static void checkAndScheduleNightEvents(
            ServerLevel world,
            PrettyMeteorsConfig.Settings settings) {
        long worldTime  = world.getGameTime();
        long timeOfDay  = worldTime % 24000L;
        long dayNumber  = worldTime / 24000L;

        // Only act during the first 100 ticks after nightfall (13 000 … 13 099).
        // This window is wide enough to survive a single-tick skip but not so wide
        // that it fires again on the same night after a /time command.
        if (timeOfDay < 13000L || timeOfDay >= 13100L) return;

        Long lastDay = LAST_NIGHT_DAY.get(world.dimension());
        if (lastDay != null && lastDay >= dayNumber) return;   // already scheduled tonight

        List<ServerPlayer> players = world.players();
        if (players.isEmpty()) return;   // nobody online — wait until there is

        LAST_NIGHT_DAY.put(world.dimension(), dayNumber);

        List<ScheduledEvent> queue =
                EVENT_QUEUE.computeIfAbsent(world.dimension(), k -> new ArrayList<>());
        // Drop stale events from a previous night that never fired.
        queue.removeIf(e -> e.fireTick() < worldTime - 200L);

        // Night window: timeOfDay 13 000 → 23 000 = 10 000 ticks remaining this night.
        long windowSize = 23000L - timeOfDay;   // ticks until end-of-night

        java.util.Random rng = new java.util.Random(worldTime);

        // Schedule the configured number of individual shooting stars at random times tonight.
        for (int i = 0; i < settings.nightStarCount(); i++) {
            long delay = NightEventPlanner.starDelay(rng, windowSize);
            queue.add(new ScheduledEvent(worldTime + delay, ShowerType.SINGLE));
        }

        // Roll to decide if any shower happens tonight.
        int roll = rng.nextInt(100);
        NightEventPlanner.Chances chances = new NightEventPlanner.Chances(
                settings.nightNoneChance(),
                settings.nightSmallChance(),
                settings.nightMediumChance(),
                settings.nightLargeChance());
        java.util.Optional<ShowerType> plannedShower = NightEventPlanner.chooseShower(roll, chances);
        if (plannedShower.isEmpty()) {
            // No shower tonight — just the shooting stars.
            queue.sort(Comparator.comparingLong(ScheduledEvent::fireTick));
            LOGGER.info("[PrettyMeteors] Night {}: scheduled {} shooting star(s), no shower tonight.",
                    dayNumber, settings.nightStarCount());
        } else {
            long showerDelay = NightEventPlanner.showerDelay(rng);
            ShowerType showerType = plannedShower.orElseThrow();
            queue.add(new ScheduledEvent(worldTime + showerDelay, showerType));
            queue.sort(Comparator.comparingLong(ScheduledEvent::fireTick));
            LOGGER.info("[PrettyMeteors] Night {}: scheduled {} shooting star(s) + 1 {} shower.",
                    dayNumber, settings.nightStarCount(), showerType.name().toLowerCase());
        }
    }

    /**
     * Checks the event queue and fires the next due event, but only when
     * no shower is currently active (so manual showers are never interrupted).
     * Stale events (fired more than 1 minute late) are silently dropped.
     */
    private static void processEventQueue(ServerLevel world) {
        List<ScheduledEvent> queue = EVENT_QUEUE.get(world.dimension());
        if (queue == null || queue.isEmpty()) return;

        long worldTime = world.getGameTime();

        // Drop events that are more than 1 minute (1 200 ticks) overdue.
        queue.removeIf(e -> e.fireTick() < worldTime - 1200L);

        // Only fire when no shower is running.
        MeteorShowerConfig current = ACTIVE_SHOWERS.get(world.dimension());
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
    private static void fireScheduledEvent(ServerLevel world, ShowerType type) {
        List<ServerPlayer> players = world.players();
        if (players.isEmpty()) return;

        // Re-target a random player at fire time so the position is current.
        ServerPlayer target = players.get(world.getRandom().nextInt(players.size()));
        double posX = target.getX();
        double posY = target.getY();
        double posZ = target.getZ();
        double originY = MeteorShowerConfig.skyOriginY(posY);
        long worldTime = world.getGameTime();

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
    private static void startShowerFromRegistration(ServerLevel world, MeteorShowerRegistration reg) {
        java.util.List<ServerPlayer> players = world.players();
        if (players.isEmpty()) return;

        ServerPlayer target = players.get(world.getRandom().nextInt(players.size()));
        double posX = target.getX();
        double posY = target.getY();
        double posZ = target.getZ();
        double originY = MeteorShowerConfig.skyOriginY(posY);
        long worldTime = world.getGameTime();

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
                                                   ServerLevel world,
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

    private static void broadcastState(ServerLevel world, MeteorShowerPayload payload) {
        requirePlatform().broadcast(world, payload);
    }

    private static PlatformServices requirePlatform() {
        if (platform == null) {
            throw new IllegalStateException("Pretty Meteors is not initialized");
        }
        return platform;
    }
}
