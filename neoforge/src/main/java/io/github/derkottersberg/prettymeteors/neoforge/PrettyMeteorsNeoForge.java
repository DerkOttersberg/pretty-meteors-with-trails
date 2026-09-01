package io.github.derkottersberg.prettymeteors.neoforge;

import com.derko.prettymeteors.PrettyMeteorsMod;
import com.derko.prettymeteors.command.PrettyMeteorsCommands;
import com.derko.prettymeteors.network.MeteorShowerPayload;
import io.github.derkottersberg.prettymeteors.internal.PlatformServices;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;

import java.lang.reflect.InvocationTargetException;

@Mod(PrettyMeteorsMod.MOD_ID)
public final class PrettyMeteorsNeoForge {
    public PrettyMeteorsNeoForge(IEventBus modEventBus) {
        registerDevelopmentGameTests(modEventBus);
        modEventBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        NeoForge.EVENT_BUS.addListener(this::onLevelTick);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerChangedDimension);
        PrettyMeteorsMod.initialize(new NeoForgePlatformServices());
        if (FMLEnvironment.getDist().isClient()) {
            PrettyMeteorsNeoForgeClient.initialize();
        }
    }

    /**
     * The GameTest source set is intentionally absent from release jars. Its bootstrap is
     * discovered reflectively only in development/GameTest runs so production artifacts do
     * not ship test registrations.
     */
    private static void registerDevelopmentGameTests(IEventBus modEventBus) {
        try {
            Class<?> bootstrap = Class.forName(
                    "io.github.derkottersberg.prettymeteors.neoforge.gametest.PrettyMeteorsNeoForgeGameTests");
            bootstrap.getMethod("register", IEventBus.class).invoke(null, modEventBus);
        } catch (ClassNotFoundException ignored) {
            // Expected for normal development launches and production jars.
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Could not register Pretty Meteors NeoForge GameTests", exception);
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("2")
                .executesOn(HandlerThread.MAIN)
                .playToClient(MeteorShowerPayload.ID, MeteorShowerPayload.CODEC,
                        (payload, context) -> PrettyMeteorsNeoForgeClient.handlePayload(payload));
    }

    private void registerCommands(RegisterCommandsEvent event) {
        PrettyMeteorsCommands.register(event.getDispatcher());
    }

    private void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            PrettyMeteorsMod.tickWorld(level);
        }
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        syncPlayer(event.getEntity());
    }

    private void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        syncPlayer(event.getEntity());
    }

    private static void syncPlayer(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PrettyMeteorsMod.syncPlayer(serverPlayer);
        }
    }

    private static final class NeoForgePlatformServices implements PlatformServices {
        @Override
        public String loaderName() {
            return "NeoForge";
        }

        @Override
        public void broadcast(ServerLevel level, MeteorShowerPayload payload) {
            level.players().forEach(player -> sendToPlayer(player, payload));
        }

        @Override
        public void sendToPlayer(ServerPlayer player, MeteorShowerPayload payload) {
            // NeoForge rejects a custom payload when the remote connection did
            // not negotiate its channel. Real modded clients advertise it;
            // vanilla/test mock connections are safely skipped.
            if (player.connection.hasChannel(payload.type())) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }
}
