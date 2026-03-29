package com.derko.prettymeteors.command;

import com.derko.prettymeteors.MeteorShowerConfig;
import com.derko.prettymeteors.PrettyMeteorsMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

public final class PrettyMeteorsCommands {
    private PrettyMeteorsCommands() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        var spreadArgument = CommandManager.argument("spreadDegrees", FloatArgumentType.floatArg(0.1f, 12.0f))
                .executes(context -> startCustom(
                        context.getSource(),
                        IntegerArgumentType.getInteger(context, "durationSeconds"),
                        FloatArgumentType.getFloat(context, "meteorsPerSecond"),
                        FloatArgumentType.getFloat(context, "baseSpeed"),
                        FloatArgumentType.getFloat(context, "yawDegrees"),
                        FloatArgumentType.getFloat(context, "pitchDegrees"),
                        FloatArgumentType.getFloat(context, "spreadDegrees")));

        var pitchArgument = CommandManager.argument("pitchDegrees", FloatArgumentType.floatArg(-20.0f, 50.0f))
                .then(spreadArgument);

        var yawArgument = CommandManager.argument("yawDegrees", FloatArgumentType.floatArg(-180.0f, 180.0f))
                .then(pitchArgument);

        var speedArgument = CommandManager.argument("baseSpeed", FloatArgumentType.floatArg(1.0f, 12.0f))
                .then(yawArgument);

        var rateArgument = CommandManager.argument("meteorsPerSecond", FloatArgumentType.floatArg(1.0f, 80.0f))
                .then(speedArgument);

        var durationArgument = CommandManager.argument("durationSeconds", IntegerArgumentType.integer(5, 600))
                .then(rateArgument);

        var startCommand = CommandManager.literal("start")
                .executes(context -> startDefault(context.getSource()))
                .then(durationArgument);

        dispatcher.register(CommandManager.literal("prettymeteors")
                .then(startCommand)
                .then(CommandManager.literal("stop")
                        .executes(context -> stop(context.getSource())))
                .then(CommandManager.literal("status")
                        .executes(context -> status(context.getSource()))));
    }

    private static int startDefault(ServerCommandSource source) {
        ServerWorld world = source.getWorld();
        Random random = world.getRandom();
                Vec3d origin = source.getPosition();
                MeteorShowerConfig config = MeteorShowerConfig.createDefault(world.getTime(), random, origin.x, Math.max(origin.y + 150.0, 292.0), origin.z);
        PrettyMeteorsMod.startShower(world, config);
        source.sendFeedback(() -> Text.literal("Started a pretty meteor shower for " + (config.durationTicks() / 20) + "s at " + config.meteorsPerSecond() + " meteors/sec."), true);
        return 1;
    }

    private static int startCustom(ServerCommandSource source, int durationSeconds, float meteorsPerSecond, float baseSpeed, float yawDegrees, float pitchDegrees, float spreadDegrees) {
        ServerWorld world = source.getWorld();
                Vec3d origin = source.getPosition();
        int maxLifetime = MathHelper.clamp(Math.round(180.0f - baseSpeed * 11.0f), 72, 220);
        int minLifetime = MathHelper.clamp(Math.round(maxLifetime * 0.24f), 22, maxLifetime - 18);
        float speedVariance = Math.max(0.55f, baseSpeed * 0.42f);

        MeteorShowerConfig config = new MeteorShowerConfig(
                world.getTime(),
                durationSeconds * 20,
                meteorsPerSecond,
                yawDegrees,
                pitchDegrees,
                spreadDegrees,
                170.0f,
                980.0f,
                118.0f,
                baseSpeed,
                speedVariance,
                minLifetime,
                maxLifetime,
                origin.x,
                Math.max(origin.y + 150.0, 292.0),
                origin.z,
                world.getRandom().nextInt());

        PrettyMeteorsMod.startShower(world, config);
        source.sendFeedback(() -> Text.literal("Started a custom meteor shower with base speed " + baseSpeed + " and spread " + spreadDegrees + " degrees."), true);
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
            source.sendFeedback(() -> Text.literal("No meteor shower is active in this world."), false);
            return 0;
        }

        int secondsRemaining = Math.max(0, (int) ((config.endTick() - world.getTime()) / 20L));
        source.sendFeedback(() -> Text.literal(
                "Meteor shower active: "
                        + secondsRemaining
                        + "s remaining, "
                        + config.meteorsPerSecond()
                        + " meteors/sec, yaw "
                        + config.yawDegrees()
                        + ", pitch "
                        + config.pitchDegrees()
                        + ", spread "
                        + config.angularSpreadDegrees()
                        + "."), false);
        return 1;
    }
}