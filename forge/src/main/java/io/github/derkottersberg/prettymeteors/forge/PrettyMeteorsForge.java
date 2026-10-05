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
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.Channel;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.network.PacketDistributor;

@Mod(PrettyMeteorsMod.MOD_ID)
public final class PrettyMeteorsForge {
    private static final Channel<CustomPacketPayload> NETWORK = createNetwork();

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

    private static Channel<CustomPacketPayload> createNetwork() {
        return ChannelBuilder.named(PrettyMeteorsMod.id("network"))
                .networkProtocolVersion(1).payloadChannel().play().clientbound()
                .addMain(MeteorShowerPayload.ID, MeteorShowerPayload.CODEC.cast(), (payload, context) -> {
                    if (context.isClientSide()) PrettyMeteorsForgeClient.handlePayload(payload);
                }).build();
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
            NETWORK.send(payload, PacketDistributor.PLAYER.with(player));
        }
    }
}
