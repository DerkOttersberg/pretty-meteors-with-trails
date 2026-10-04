package io.github.derkottersberg.prettymeteors.fabric;

import com.derko.prettymeteors.PrettyMeteorsMod;
import com.derko.prettymeteors.command.PrettyMeteorsCommands;
import com.derko.prettymeteors.network.MeteorShowerPayload;
import io.github.derkottersberg.prettymeteors.internal.PlatformServices;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.nio.file.Path;

public final class PrettyMeteorsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(MeteorShowerPayload.ID, MeteorShowerPayload.CODEC);
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> PrettyMeteorsCommands.register(dispatcher));
        ServerTickEvents.END_LEVEL_TICK.register(PrettyMeteorsMod::tickWorld);
        PrettyMeteorsMod.initialize(new FabricPlatformServices());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                PrettyMeteorsMod.syncPlayer(handler.player));
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof ServerPlayer player) {
                PrettyMeteorsMod.syncPlayer(player);
            }
        });
    }

    private static final class FabricPlatformServices implements PlatformServices {
        @Override
        public String loaderName() {
            return "Fabric";
        }

        @Override
        public Path configDirectory() {
            return FabricLoader.getInstance().getConfigDir();
        }

        @Override
        public void broadcast(ServerLevel level, MeteorShowerPayload payload) {
            level.players().forEach(player -> ServerPlayNetworking.send(player, payload));
        }

        @Override
        public void sendToPlayer(ServerPlayer player, MeteorShowerPayload payload) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
