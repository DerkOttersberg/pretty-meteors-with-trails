package com.derko.prettymeteors.command;

import com.derko.prettymeteors.MeteorShowerConfig;
import com.derko.prettymeteors.PrettyMeteorsMod;
import com.derko.prettymeteors.PrettyMeteorsConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class PrettyMeteorsCommands {
    private PrettyMeteorsCommands() {
    }

    private enum ShowerSize { SINGLE, SMALL, MEDIUM, LARGE }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("prettymeteors")
                .requires(source -> source.hasPermission(2))
                // /prettymeteors start [large|medium|small|single]
                .then(Commands.literal("start")
                        .executes(ctx -> startShower(ctx.getSource(), ShowerSize.LARGE))
                        .then(Commands.literal("large")
                                .executes(ctx -> startShower(ctx.getSource(), ShowerSize.LARGE)))
                        .then(Commands.literal("medium")
                                .executes(ctx -> startShower(ctx.getSource(), ShowerSize.MEDIUM)))
                        .then(Commands.literal("small")
                                .executes(ctx -> startShower(ctx.getSource(), ShowerSize.SMALL)))
                        .then(Commands.literal("single")
                                .executes(ctx -> startShower(ctx.getSource(), ShowerSize.SINGLE))))

                // /prettymeteors stop
                .then(Commands.literal("stop")
                        .executes(ctx -> stop(ctx.getSource())))

                // /prettymeteors status
                .then(Commands.literal("status")
                        .executes(ctx -> status(ctx.getSource())))

                // /prettymeteors night ...
                .then(Commands.literal("night")
                        .then(Commands.literal("enable")
                                .executes(ctx -> setNightEnabled(ctx.getSource(), true)))
                        .then(Commands.literal("disable")
                                .executes(ctx -> setNightEnabled(ctx.getSource(), false)))
                        .then(Commands.literal("stars")
                                .then(Commands.argument("count", IntegerArgumentType.integer(0, 20))
                                        .executes(ctx -> setNightStars(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "count")))))
                        .then(Commands.literal("chances")
                                .then(Commands.argument("none", IntegerArgumentType.integer(0, 100))
                                        .then(Commands.argument("small", IntegerArgumentType.integer(0, 100))
                                                .then(Commands.argument("medium", IntegerArgumentType.integer(0, 100))
                                                        .then(Commands.argument("large", IntegerArgumentType.integer(0, 100))
                                                                .executes(ctx -> setNightChances(
                                                                        ctx.getSource(),
                                                                        IntegerArgumentType.getInteger(ctx, "none"),
                                                                        IntegerArgumentType.getInteger(ctx, "small"),
                                                                        IntegerArgumentType.getInteger(ctx, "medium"),
                                                                        IntegerArgumentType.getInteger(ctx, "large"))))))))
                        .then(Commands.literal("status")
                                .executes(ctx -> nightStatus(ctx.getSource())))));
    }

    private static int startShower(CommandSourceStack source, ShowerSize size) {
        ServerLevel world = source.getLevel();
        RandomSource random = world.getRandom();
        Vec3 origin = source.getPosition();
        double originY = MeteorShowerConfig.skyOriginY(origin.y);
        long startTick = world.getGameTime();

        MeteorShowerConfig config = switch (size) {
            case SINGLE -> MeteorShowerConfig.createSingle(startTick, random, origin.x, originY, origin.z);
            case SMALL  -> MeteorShowerConfig.createSmall(startTick, random, origin.x, originY, origin.z);
            case MEDIUM -> MeteorShowerConfig.createMedium(startTick, random, origin.x, originY, origin.z);
            case LARGE  -> MeteorShowerConfig.createLarge(startTick, random, origin.x, originY, origin.z);
        };

        PrettyMeteorsMod.startShower(world, config);
        String sizeName = size.name().toLowerCase();
        int durationSec = config.durationTicks() / 20;
        source.sendSuccess(() -> Component.literal(
                "Started a " + sizeName + " meteor shower (" + durationSec + "s, " + config.meteorsPerSecond() + " meteors/sec)."), true);
        return 1;
    }

    private static int stop(CommandSourceStack source) {
        ServerLevel world = source.getLevel();
        if (PrettyMeteorsMod.getActiveShower(world) == null) {
            source.sendFailure(Component.literal("No meteor shower is active in this world."));
            return 0;
        }
        PrettyMeteorsMod.stopShower(world);
        source.sendSuccess(() -> Component.literal("Stopped the meteor shower."), true);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        ServerLevel world = source.getLevel();
        MeteorShowerConfig config = PrettyMeteorsMod.getActiveShower(world);
        if (config == null) {
            source.sendSuccess(() -> Component.literal("No meteor shower is currently active."), false);
            return 0;
        }
        int secondsRemaining = Math.max(0, (int) ((config.endTick() - world.getGameTime()) / 20L));
        source.sendSuccess(() -> Component.literal(
                "Meteor shower active: " + secondsRemaining + "s remaining, "
                        + config.meteorsPerSecond() + " meteors/sec, yaw "
                        + String.format("%.1f", config.yawDegrees()) + " deg, pitch "
                        + String.format("%.1f", config.pitchDegrees()) + " deg."), false);
        return 1;
    }

    // ---- Night event config commands ----

    private static int setNightEnabled(CommandSourceStack source, boolean enabled) {
        PrettyMeteorsConfig.Settings current = PrettyMeteorsConfig.snapshot();
        PrettyMeteorsConfig.update(new PrettyMeteorsConfig.Settings(
                enabled,
                current.nightStarCount(),
                current.nightNoneChance(),
                current.nightSmallChance(),
                current.nightMediumChance(),
                current.nightLargeChance()));
        source.sendSuccess(() -> Component.literal(
                "Nightly meteor events " + (enabled ? "enabled" : "disabled") + "."), true);
        return 1;
    }

    private static int setNightStars(CommandSourceStack source, int count) {
        PrettyMeteorsConfig.Settings current = PrettyMeteorsConfig.snapshot();
        PrettyMeteorsConfig.update(new PrettyMeteorsConfig.Settings(
                current.nightEventsEnabled(),
                count,
                current.nightNoneChance(),
                current.nightSmallChance(),
                current.nightMediumChance(),
                current.nightLargeChance()));
        source.sendSuccess(() -> Component.literal(
                "Nightly shooting stars set to " + count + " per night."), true);
        return 1;
    }

    private static int setNightChances(CommandSourceStack source, int none, int small, int medium, int large) {
        if (none + small + medium + large != 100) {
            source.sendFailure(Component.literal(
                    "Percentages must add up to 100 (got " + (none + small + medium + large) + ")."));
            return 0;
        }
        PrettyMeteorsConfig.Settings current = PrettyMeteorsConfig.snapshot();
        PrettyMeteorsConfig.update(new PrettyMeteorsConfig.Settings(
                current.nightEventsEnabled(),
                current.nightStarCount(),
                none,
                small,
                medium,
                large));
        source.sendSuccess(() -> Component.literal(
                "Night shower chances: none=" + none + "%, small=" + small + "%, medium=" + medium + "%, large=" + large + "%."), true);
        return 1;
    }

    private static int nightStatus(CommandSourceStack source) {
        PrettyMeteorsConfig.Settings settings = PrettyMeteorsConfig.snapshot();
        String msg = "Night events: " + (settings.nightEventsEnabled() ? "ENABLED" : "DISABLED")
                + "  |  Stars/night: " + settings.nightStarCount()
                + "  |  Shower chances: none=" + settings.nightNoneChance()
                + "%, small=" + settings.nightSmallChance()
                + "%, medium=" + settings.nightMediumChance()
                + "%, large=" + settings.nightLargeChance() + "%";
        source.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }
}

