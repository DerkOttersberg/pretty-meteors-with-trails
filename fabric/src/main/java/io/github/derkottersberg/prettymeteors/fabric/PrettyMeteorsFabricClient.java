package io.github.derkottersberg.prettymeteors.fabric;

import com.derko.prettymeteors.client.MeteorShowerClientState;
import com.derko.prettymeteors.network.MeteorShowerPayload;
import io.github.derkottersberg.prettymeteors.internal.ClientPlatformServices;
import io.github.derkottersberg.prettymeteors.internal.PrettyMeteorsClientBootstrap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class PrettyMeteorsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PrettyMeteorsClientBootstrap.initialize(new FabricClientPlatformServices());
        ClientPlayNetworking.registerGlobalReceiver(MeteorShowerPayload.ID, (client, handler, buffer, responseSender) -> {
            MeteorShowerPayload payload = MeteorShowerPayload.CODEC.decode(buffer);
            client.execute(() -> MeteorShowerClientState.INSTANCE.applyPayload(client, payload));
        });
        ClientTickEvents.END_CLIENT_TICK.register(MeteorShowerClientState.INSTANCE::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> MeteorShowerClientState.INSTANCE.clearAll());
    }

    private static final class FabricClientPlatformServices implements ClientPlatformServices {
        @Override
        public String loaderName() {
            return "Fabric";
        }
    }
}
