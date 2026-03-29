package com.derko.prettymeteors;

import com.derko.prettymeteors.command.PrettyMeteorsCommands;
import com.derko.prettymeteors.network.MeteorShowerPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public final class PrettyMeteorsMod implements ModInitializer {
    public static final String MOD_ID = "prettymeteors";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Map<RegistryKey<World>, MeteorShowerConfig> ACTIVE_SHOWERS = new HashMap<>();

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(MeteorShowerPayload.ID, MeteorShowerPayload.CODEC);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> PrettyMeteorsCommands.register(dispatcher));
        ServerTickEvents.END_WORLD_TICK.register(PrettyMeteorsMod::tickWorld);
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    public static void startShower(ServerWorld world, MeteorShowerConfig config) {
        ACTIVE_SHOWERS.put(world.getRegistryKey(), config);
        broadcastState(world, MeteorShowerPayload.fromConfig(config));
    }

    public static void stopShower(ServerWorld world) {
        ACTIVE_SHOWERS.remove(world.getRegistryKey());
        broadcastState(world, MeteorShowerPayload.inactive());
    }

    public static MeteorShowerConfig getActiveShower(ServerWorld world) {
        RegistryKey<World> worldKey = world.getRegistryKey();
        MeteorShowerConfig config = ACTIVE_SHOWERS.get(worldKey);

        if (config == null) {
            return null;
        }

        if (world.getTime() >= config.endTick()) {
            ACTIVE_SHOWERS.remove(worldKey);
            return null;
        }

        return config;
    }

    private static void tickWorld(ServerWorld world) {
        MeteorShowerConfig config = getActiveShower(world);

        if (config == null) {
            return;
        }

        if (world.getTime() == config.startTick() || world.getTime() % 40L == 0L) {
            broadcastState(world, MeteorShowerPayload.fromConfig(config));
        }
    }

    private static void broadcastState(ServerWorld world, MeteorShowerPayload payload) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}