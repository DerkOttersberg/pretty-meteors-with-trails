package io.github.derkottersberg.prettymeteors.forge;

import com.derko.prettymeteors.PrettyMeteorsMod;
import com.derko.prettymeteors.command.PrettyMeteorsCommands;
import com.derko.prettymeteors.network.MeteorShowerPayload;
import io.github.derkottersberg.prettymeteors.internal.PlatformServices;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod(PrettyMeteorsMod.MOD_ID)
public final class PrettyMeteorsForge {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel NETWORK = createNetwork();

    public PrettyMeteorsForge() {
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                PrettyMeteorsCommands.register(event.getDispatcher()));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.LevelTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END && event.side == LogicalSide.SERVER
                    && event.level instanceof ServerLevel level) PrettyMeteorsMod.tickWorld(level);
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> syncPlayer(event.getEntity()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> syncPlayer(event.getEntity()));
        PrettyMeteorsMod.initialize(new ForgePlatformServices());
        if (FMLEnvironment.dist.isClient()) PrettyMeteorsForgeClient.initialize();
    }

    private static SimpleChannel createNetwork() {
        SimpleChannel channel = NetworkRegistry.newSimpleChannel(PrettyMeteorsMod.id("network"),
                () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
        channel.registerMessage(0, MeteorShowerPayload.class,
                (payload, buffer) -> MeteorShowerPayload.CODEC.encode(buffer, payload),
                MeteorShowerPayload.CODEC::decode, (payload, contextSupplier) -> {
                    var context = contextSupplier.get();
                    context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> PrettyMeteorsForgeClient.handlePayload(payload)));
                    context.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        return channel;
    }

    private static void syncPlayer(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer serverPlayer) PrettyMeteorsMod.syncPlayer(serverPlayer);
    }

    private static final class ForgePlatformServices implements PlatformServices {
        @Override public String loaderName() { return "Forge"; }
        @Override public Path configDirectory() { return FMLPaths.CONFIGDIR.get(); }
        @Override public void broadcast(ServerLevel level, MeteorShowerPayload payload) {
            level.players().forEach(player -> sendToPlayer(player, payload));
        }
        @Override public void sendToPlayer(ServerPlayer player, MeteorShowerPayload payload) {
            NETWORK.send(PacketDistributor.PLAYER.with(() -> player), payload);
        }
    }
}
