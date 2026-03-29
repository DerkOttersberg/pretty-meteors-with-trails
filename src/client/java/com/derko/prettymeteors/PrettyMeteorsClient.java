package com.derko.prettymeteors;

import com.derko.prettymeteors.client.MeteorShowerClientState;
import com.derko.prettymeteors.network.MeteorShowerPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

public final class PrettyMeteorsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(MeteorShowerPayload.ID, (payload, context) ->
                context.client().execute(() -> MeteorShowerClientState.INSTANCE.applyPayload(context.client(), payload)));

        ClientTickEvents.END_CLIENT_TICK.register(MeteorShowerClientState.INSTANCE::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> MeteorShowerClientState.INSTANCE.clearAll());
        WorldRenderEvents.END_MAIN.register(MeteorShowerClientState.INSTANCE::renderWorldPass);
    }
}