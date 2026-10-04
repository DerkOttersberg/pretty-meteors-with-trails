package io.github.derkottersberg.prettymeteors.forge.gametest;

import com.derko.prettymeteors.gametest.MeteorGameTestScenario;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("prettymeteors")
@PrefixGameTestTemplate(false)
public final class PrettyMeteorsForgeGameTests {
    private PrettyMeteorsForgeGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void commandsRequireOperator(GameTestHelper helper) {
        MeteorGameTestScenario.commandsRequireOperator(helper);
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void synchronizesActiveShowerAndCodec(GameTestHelper helper) {
        // Forge's distributor requires a Netty pipeline even for a GameTest player.
        // A local embedded channel exercises the real encoder without a socket.
        var channel = new io.netty.channel.embedded.EmbeddedChannel();
        try {
            MeteorGameTestScenario.synchronizesActiveShowerAndCodec(helper, test -> {
                var level = test.getLevel();
                var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
                channel.pipeline().addLast("packet_handler", connection);
                channel.pipeline().fireChannelActive();
                channel.attr(io.netty.util.AttributeKey.<String>valueOf("fml:netversion"))
                        .set(net.minecraftforge.network.NetworkConstants.NETVERSION);
                var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level,
                        new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "Meteor-QA"));
                level.getServer().getPlayerList().placeNewPlayer(connection, player);
                return player;
            });
            channel.runPendingTasks();
            long meteorPackets = channel.outboundMessages().stream()
                    .filter(packet -> packet instanceof net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket custom
                            && custom.getIdentifier().equals(com.derko.prettymeteors.PrettyMeteorsMod.id("network")))
                    .count();
            helper.assertTrue(meteorPackets >= 2,
                    "Forge did not emit the active and inactive meteor packets");
        } catch (RuntimeException exception) {
            com.derko.prettymeteors.PrettyMeteorsMod.LOGGER.error("Forge meteor GameTest failure", exception);
            throw exception;
        } finally {
            channel.finishAndReleaseAll();
        }
    }
}
