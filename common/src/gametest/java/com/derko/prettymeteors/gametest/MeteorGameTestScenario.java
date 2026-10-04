package com.derko.prettymeteors.gametest;

import com.derko.prettymeteors.MeteorShowerConfig;
import com.derko.prettymeteors.PrettyMeteorsMod;
import com.derko.prettymeteors.network.MeteorShowerPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

public final class MeteorGameTestScenario {
    private MeteorGameTestScenario() {
    }

    public static void commandsRequireOperator(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var source = server.createCommandSourceStack().withLevel(helper.getLevel());
        var dispatcher = server.getCommands().getDispatcher();
        for (String command : java.util.List.of("prettymeteors start large", "prettymeteors stop",
                "prettymeteors night disable", "prettymeteors night chances 0 0 0 100")) {
            boolean denied = false;
            try { dispatcher.execute(command, source.withPermission(0)); }
            catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) { denied = true; }
            helper.assertTrue(denied, "Non-operator could change server meteor state: " + command);
        }
        try {
            helper.assertTrue(dispatcher.execute("prettymeteors start large", source.withPermission(2)) == 1,
                "Operator could not start a shower");
            helper.assertTrue(PrettyMeteorsMod.statePayload(helper.getLevel()).active(), "Operator shower did not start");
            dispatcher.execute("prettymeteors stop", source.withPermission(2));
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
            throw new IllegalStateException("Operator meteor command rejected", exception);
        }
        PrettyMeteorsMod.LOGGER.info("QA: commandsRequireOperator assertions passed");
        helper.succeed();
    }

    public static void synchronizesActiveShowerAndCodec(GameTestHelper helper) {
        synchronizesActiveShowerAndCodec(helper, GameTestHelper::makeMockServerPlayerInLevel);
    }

    public static void synchronizesActiveShowerAndCodec(GameTestHelper helper,
            java.util.function.Function<GameTestHelper, ServerPlayer> createPlayer) {
        ServerPlayer latePlayer = createPlayer.apply(helper);
        MeteorShowerConfig config = MeteorShowerConfig.createLarge(
                helper.getLevel().getGameTime(),
                RandomSource.create(0x5EA4_1E55L),
                latePlayer.getX(),
                MeteorShowerConfig.skyOriginY(latePlayer.getY()),
                latePlayer.getZ());

        PrettyMeteorsMod.startShower(helper.getLevel(), config);
        MeteorShowerPayload state = PrettyMeteorsMod.statePayload(helper.getLevel());
        helper.assertTrue(state.active(), "A started meteor shower was not exposed to late joiners");
        helper.assertTrue(config.equals(state.toConfig()), "The synchronized shower state changed fields");

        // Exercise the real loader PlatformServices adapter used by login and
        // dimension-change hooks, rather than only inspecting the state supplier.
        PrettyMeteorsMod.syncPlayer(latePlayer);

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            MeteorShowerPayload.CODEC.encode(buffer, state);
            helper.assertTrue(
                    state.equals(MeteorShowerPayload.CODEC.decode(buffer)),
                    "The registered meteor payload codec changed the shower state");
        } finally {
            buffer.release();
        }

        PrettyMeteorsMod.stopShower(helper.getLevel());
        helper.assertFalse(
                PrettyMeteorsMod.statePayload(helper.getLevel()).active(),
                "Stopping a shower did not produce an inactive synchronization payload");
        PrettyMeteorsMod.syncPlayer(latePlayer);
        PrettyMeteorsMod.LOGGER.info("QA: synchronizesActiveShowerAndCodec assertions passed");
        helper.succeed();
    }
}
