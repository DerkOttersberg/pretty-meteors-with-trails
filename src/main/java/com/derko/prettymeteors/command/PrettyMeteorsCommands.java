package com.derko.prettymeteors.command;

import com.derko.prettymeteors.MeteorShowerConfig;
import com.derko.prettymeteors.PrettyMeteorsMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

public final class PrettyMeteorsCommands {
    private PrettyMeteorsCommands() {
    }

    private enum ShowerSize { SINGLE, SMALL, MEDIUM, LARGE }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("prettymeteors")
                // /prettymeteors start [large|medium|small|single]
                .then(CommandManager.literal("start")
                        .executes(ctx -> startShower(ctx.getSource(), ShowerSize.LARGE))
                        .then(CommandManager.literal("large")
                                .executes(ctx -> startShower(ctx.getSource(), ShowerSize.LARGE)))
                        .then(CommandManager.literal("medium")
                                .executes(ctx -> startShower(ctx.getSource(), ShowerSize.MEDIUM)))
                        .then(CommandManager.literal("small")
                                .executes(ctx -> startShower(ctx.getSource(), ShowerSize.SMALL)))
                        .then(CommandManager.literal("single")
                                .executes(ctx -> startShower(ctx.getSource(), ShowerSize.SINGLE))))

                // /prettymeteors stop
                .then(CommandManager.literal("stop")
                        .executes(ctx -> stop(ctx.getSource())))

                // /prettymeteors status
                .then(CommandManager.literal("status")
                        .executes(ctx -> status(ctx.getSource())))

                // /prettymeteors night ...
                .then(CommandManager.literal("night")
                        .then(CommandManager.literal("enable")
                                .executes(ctx -> setNightEnabled(ctx.getSource(), true)))
                        .then(CommandManager.literal("disable")
                                .executes(ctx -> setNightEnabled(ctx.getSource(), false)))
                        .then(CommandManager.literal("stars")
                                .then(CommandManager.argument("count", IntegerArgumentType.integer(0, 20))
                                        .executes(ctx -> setNightStars(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "count")))))
                        .then(CommandManager.literal("chances")
                                .then(CommandManager.argument("none", IntegerArgumentType.integer(0, 100))
                                        .then(CommandManager.argument("small", IntegerArgumentType.integer(0, 100))
                                                .then(CommandManager.argument("medium", IntegerArgumentType.integer(0, 100))
                                                        .then(CommandManager.argument("large", IntegerArgumentType.integer(0, 100))
                                                                .executes(ctx -> setNightChances(
                                                                        ctx.getSource(),
                                                                        IntegerArgumentType.getInteger(ctx, "none"),
                                                                        IntegerArgumentType.getInteger(ctx, "small"),
                                                                        IntegerArgumentType.getInteger(ctx, "medium"),
                                                                        IntegerArgumentType.getInteger(ctx, "large"))))))))
                        .then(CommandManager.literal("status")
                                .executes(ctx -> nightStatus(ctx.getSource())))));
    }

    private static int startShower(ServerCommandSource source, ShowerSize size) {
        ServerWorld world = source.getWorld();
        Random random = world.getRandom();
        Vec3d origin = source.getPosition();
        double originY = Math.max(origin.y + 150.0, 292.0);
        long startTick = world.getTime();

        MeteorShowerConfig config = switch (size) {
            case SINGLE -> MeteorShowerConfig.createSingle(startTick, random, origin.x, originY, origin.z);
            case SMALL  -> MeteorShowerConfig.createSmall(startTick, random, origin.x, originY, origin.z);
            case MEDIUM -> MeteorShowerConfig.createMedium(startTick, random, origin.x, originY, origin.z);
            case LARGE  -> MeteorShowerConfig.createLarge(startTick, random, origin.x, originY, origin.z);
        };

        PrettyMeteorsMod.startShower(world, config);
        String sizeName = size.name().toLowerCase();
        int durationSec = config.durationTicks() / 20;
        source.sendFeedback(() -> Text.literal(
                "Started a " + sizeName + " meteor shower (" + durationSec + "s, " + config.meteorsPerSecond() + " meteors/sec)."), true);
        return 1;
    }

    private static int stop(ServerCommandSource source) {
        ServerWorld world = source.getWorld();
        if (PrettyMeteorsMod.getActiveShower(world) == null) {
            source.sendError(Text.literal("No meteor shower is active in this world."));
            return 0;
        }
        PrettyMeteorsMod.stopShower(world);
        source.sendFeedback(() -> Text.literal("Stopped the meteor shower."), true);
        return 1;
    }

    private static int status(ServerCommandSource source) {
        ServerWorld world = source.getWorld();
        MeteorShowerConfig config = PrettyMeteorsMod.getActiveShower(world);
        if (config == null) {
            source.sendFeedback(() -> Text.literal("No meteor shower is currently active."), false);
            return 0;
        }
        int secondsRemaining = Math.max(0, (int) ((config.endTick() - world.getTime()) / 20L));
        source.sendFeedback(() -> Text.literal(
                "Meteor shower active: " + secondsRemaining + "s remaining, "
                        + config.meteorsPerSecond() + " meteors/sec, yaw "
                        + String.format("%.1f", config.yawDegrees()) + " deg, pitch "
                        + String.format("%.1f", config.pitchDegrees()) + " deg."), false);
        return 1;
    }

    // ---- Night event config commands ----

    private static int setNightEnabled(ServerCommandSource source, boolean enabled) {
        PrettyMeteorsMod.nightEventsEnabled = enabled;
        source.sendFeedback(() -> Text.literal(
                "Nightly meteor events " + (enabled ? "enabled" : "disabled") + "."), true);
        return 1;
    }

    private static int setNightStars(ServerCommandSource source, int count) {
        PrettyMeteorsMod.nightStarCount = count;
        source.sendFeedback(() -> Text.literal(
                "Nightly shooting stars set to " + count + " per night."), true);
        return 1;
    }

    private static int setNightChances(ServerCommandSource source, int none, int small, int medium, int large) {
        if (none + small + medium + large != 100) {
            source.sendError(Text.literal(
                    "Percentages must add up to 100 (got " + (none + small + medium + large) + ")."));
            return 0;
        }
        PrettyMeteorsMod.nightNoneChance = none;
        PrettyMeteorsMod.nightSmallChance = small;
        PrettyMeteorsMod.nightMediumChance = medium;
        PrettyMeteorsMod.nightLargeChance = large;
        source.sendFeedback(() -> Text.literal(
                "Night shower chances: none=" + none + "%, small=" + small + "%, medium=" + medium + "%, large=" + large + "%."), true);
        return 1;
    }

    private static int nightStatus(ServerCommandSource source) {
        String msg = "Night events: " + (PrettyMeteorsMod.nightEventsEnabled ? "ENABLED" : "DISABLED")
                + "  |  Stars/night: " + PrettyMeteorsMod.nightStarCount
                + "  |  Shower chances: none=" + PrettyMeteorsMod.nightNoneChance
                + "%, small=" + PrettyMeteorsMod.nightSmallChance
                + "%, medium=" + PrettyMeteorsMod.nightMediumChance
                + "%, large=" + PrettyMeteorsMod.nightLargeChance + "%";
        source.sendFeedback(() -> Text.literal(msg), false);
        return 1;
    }
}

