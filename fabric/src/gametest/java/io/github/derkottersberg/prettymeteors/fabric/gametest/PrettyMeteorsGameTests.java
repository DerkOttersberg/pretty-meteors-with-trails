package io.github.derkottersberg.prettymeteors.fabric.gametest;

import com.derko.prettymeteors.MeteorShowerConfig;
import com.derko.prettymeteors.PrettyMeteorsMod;
import com.derko.prettymeteors.network.MeteorShowerPayload;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

@SuppressWarnings("removal")
public final class PrettyMeteorsGameTests {
    @GameTest(maxTicks = 40)
    public void synchronizesActiveShowerAndCodec(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        MeteorShowerConfig config = MeteorShowerConfig.createLarge(
            helper.getLevel().getGameTime(),
            RandomSource.create(0x5EA4_1E55L),
            player.getX(),
            MeteorShowerConfig.skyOriginY(player.getY()),
            player.getZ()
        );

        PrettyMeteorsMod.startShower(helper.getLevel(), config);
        MeteorShowerPayload state = PrettyMeteorsMod.statePayload(helper.getLevel());
        helper.assertTrue(state.active(), "A started meteor shower was not exposed to late joiners");
        helper.assertValueEqual(state.toConfig(), config, "The synchronized shower state changed fields");

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            helper.getLevel().registryAccess()
        );
        try {
            MeteorShowerPayload.CODEC.encode(buffer, state);
            MeteorShowerPayload decoded = MeteorShowerPayload.CODEC.decode(buffer);
            helper.assertValueEqual(decoded, state, "The registered meteor payload codec changed the shower state");
        } finally {
            buffer.release();
        }

        PrettyMeteorsMod.syncPlayer(player);
        PrettyMeteorsMod.stopShower(helper.getLevel());
        helper.assertFalse(
            PrettyMeteorsMod.statePayload(helper.getLevel()).active(),
            "Stopping a shower did not produce an inactive synchronization payload"
        );
        helper.succeed();
    }
}
