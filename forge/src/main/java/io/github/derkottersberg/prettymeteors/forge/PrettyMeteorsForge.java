package io.github.derkottersberg.prettymeteors.forge;

import com.derko.prettymeteors.PrettyMeteorsMod;
import com.derko.prettymeteors.command.PrettyMeteorsCommands;
import com.derko.prettymeteors.network.MeteorShowerPayload;
import io.github.derkottersberg.prettymeteors.internal.PlatformServices;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import java.nio.file.Path;

@Mod(PrettyMeteorsMod.MOD_ID)
public final class PrettyMeteorsForge {
    private static final Channel<CustomPacketPayload> NETWORK = createNetwork();

    public PrettyMeteorsForge(FMLJavaModLoadingContext context) {
        registerDevelopmentGameTests(context);
        RegisterCommandsEvent.BUS.addListener(event -> PrettyMeteorsCommands.register(event.getDispatcher()));
        TickEvent.LevelTickEvent.Post.BUS.addListener(event -> {
            if (event.side() == LogicalSide.SERVER && event.level() instanceof ServerLevel level) {
                PrettyMeteorsMod.tickWorld(level);
            }
        });
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(event -> syncPlayer(event.getEntity()));
        PlayerEvent.PlayerChangedDimensionEvent.BUS.addListener(event -> syncPlayer(event.getEntity()));
        PrettyMeteorsMod.initialize(new ForgePlatformServices());
        if (FMLEnvironment.dist.isClient()) {
            PrettyMeteorsForgeClient.initialize(context);
        }
    }

    private static void registerDevelopmentGameTests(FMLJavaModLoadingContext context) {
        try {
            Class<?> tests = Class.forName("io.github.derkottersberg.prettymeteors.forge.gametest.PrettyMeteorsForgeGameTests");
            tests.getMethod("register", net.minecraftforge.eventbus.api.bus.BusGroup.class)
                    .invoke(null, context.getModBusGroup());
        } catch (ClassNotFoundException ignored) {
            // Source-set-only QA code is deliberately absent from release jars.
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not register Pretty Meteors Forge GameTests", exception);
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

    private static void syncPlayer(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PrettyMeteorsMod.syncPlayer(serverPlayer);
        }
    }

    private static final class ForgePlatformServices implements PlatformServices {
        @Override
        public String loaderName() {
            return "Forge";
        }

        @Override
        public Path configDirectory() {
            return FMLPaths.CONFIGDIR.get();
        }

        @Override
        public void broadcast(ServerLevel level, MeteorShowerPayload payload) {
            level.players().forEach(player -> NETWORK.send(payload, PacketDistributor.PLAYER.with(player)));
        }

        @Override
        public void sendToPlayer(ServerPlayer player, MeteorShowerPayload payload) {
            NETWORK.send(payload, PacketDistributor.PLAYER.with(player));
        }
    }
}
