package io.github.derkottersberg.prettymeteors.forge;

import com.derko.prettymeteors.PrettyMeteorsMod;
import com.derko.prettymeteors.command.PrettyMeteorsCommands;
import com.derko.prettymeteors.network.MeteorShowerPayload;
import io.github.derkottersberg.prettymeteors.internal.PlatformServices;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;

@Mod(PrettyMeteorsMod.MOD_ID)
public final class PrettyMeteorsForge {
    private static final Channel<CustomPacketPayload> NETWORK = createNetwork();

    public PrettyMeteorsForge(FMLJavaModLoadingContext context) {
        RegisterCommandsEvent.BUS.addListener(event -> PrettyMeteorsCommands.register(event.getDispatcher()));
        TickEvent.LevelTickEvent.Post.BUS.addListener(event -> {
            if (event.side() == LogicalSide.SERVER && event.level() instanceof ServerLevel level) {
                PrettyMeteorsMod.tickWorld(level);
            }
        });
        PrettyMeteorsMod.initialize(new ForgePlatformServices());
        if (FMLEnvironment.dist.isClient()) {
            PrettyMeteorsForgeClient.initialize();
        }
    }

    private static Channel<CustomPacketPayload> createNetwork() {
        return ChannelBuilder.named(PrettyMeteorsMod.id("network"))
                .networkProtocolVersion(1)
                .payloadChannel()
                .play()
                .clientbound()
                .addMain(MeteorShowerPayload.ID, MeteorShowerPayload.CODEC, (payload, context) -> {
                    if (context.isClientSide()) {
                        PrettyMeteorsForgeClient.handlePayload(payload);
                    }
                })
                .build();
    }

    private static final class ForgePlatformServices implements PlatformServices {
        @Override
        public String loaderName() {
            return "Forge";
        }

        @Override
        public void broadcast(ServerLevel level, MeteorShowerPayload payload) {
            level.players().forEach(player -> NETWORK.send(payload, PacketDistributor.PLAYER.with(player)));
        }
    }
}
