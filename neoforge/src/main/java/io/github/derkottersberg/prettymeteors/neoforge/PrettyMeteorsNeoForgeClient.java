package io.github.derkottersberg.prettymeteors.neoforge;

import com.derko.prettymeteors.client.MeteorShowerClientState;
import com.derko.prettymeteors.client.PrettyMeteorsConfigScreen;
import com.derko.prettymeteors.network.MeteorShowerPayload;
import io.github.derkottersberg.prettymeteors.internal.ClientPlatformServices;
import io.github.derkottersberg.prettymeteors.internal.PrettyMeteorsClientBootstrap;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

final class PrettyMeteorsNeoForgeClient {
    private PrettyMeteorsNeoForgeClient() {
    }

    static void initialize(ModContainer container) {
        PrettyMeteorsClientBootstrap.initialize(new NeoForgeClientPlatformServices());
        NeoForge.EVENT_BUS.addListener(PrettyMeteorsNeoForgeClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(PrettyMeteorsNeoForgeClient::onLogout);
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (modContainer, parent) -> new PrettyMeteorsConfigScreen(parent));
    }

    static void handlePayload(MeteorShowerPayload payload) {
        MeteorShowerClientState.INSTANCE.applyPayload(Minecraft.getInstance(), payload);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        MeteorShowerClientState.INSTANCE.tick(Minecraft.getInstance());
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        MeteorShowerClientState.INSTANCE.clearAll();
    }

    private static final class NeoForgeClientPlatformServices implements ClientPlatformServices {
        @Override
        public String loaderName() {
            return "NeoForge";
        }
    }
}
