package com.derko.prettymeteors.gametest;

import com.derko.prettymeteors.MeteorShowerConfig;
import com.derko.prettymeteors.PrettyMeteorsMod;
import com.derko.prettymeteors.network.MeteorShowerPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.RandomSource;

public final class MeteorGameTestScenario {
    private MeteorGameTestScenario() {
    }

    public static void synchronizesActiveShowerAndCodec(GameTestHelper helper) {
        MeteorShowerConfig config = MeteorShowerConfig.createLarge(
                helper.getLevel().getGameTime(),
                RandomSource.create(0x5EA4_1E55L),
                0.0,
                MeteorShowerConfig.skyOriginY(0.0),
                0.0);

        PrettyMeteorsMod.startShower(helper.getLevel(), config);
        MeteorShowerPayload state = PrettyMeteorsMod.statePayload(helper.getLevel());
        helper.assertTrue(state.active(), "A started meteor shower was not exposed to late joiners");
        helper.assertValueEqual(state.toConfig(), config, "The synchronized shower state changed fields");

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            MeteorShowerPayload.CODEC.encode(buffer, state);
            helper.assertValueEqual(
                    MeteorShowerPayload.CODEC.decode(buffer),
                    state,
                    "The registered meteor payload codec changed the shower state");
        } finally {
            buffer.release();
        }

        PrettyMeteorsMod.stopShower(helper.getLevel());
        helper.assertFalse(
                PrettyMeteorsMod.statePayload(helper.getLevel()).active(),
                "Stopping a shower did not produce an inactive synchronization payload");
        helper.succeed();
    }
}
