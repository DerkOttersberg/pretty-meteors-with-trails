package io.github.derkottersberg.prettymeteors.forge;

import com.derko.prettymeteors.client.MeteorShowerClientState;
import com.derko.prettymeteors.client.PrettyMeteorsConfigScreen;
import com.derko.prettymeteors.network.MeteorShowerPayload;
import io.github.derkottersberg.prettymeteors.internal.ClientPlatformServices;
import io.github.derkottersberg.prettymeteors.internal.PrettyMeteorsClientBootstrap;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;

final class PrettyMeteorsForgeClient {
    private PrettyMeteorsForgeClient() {
    }

    static void initialize() {
        PrettyMeteorsClientBootstrap.initialize(new ForgeClientPlatformServices());
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) MeteorShowerClientState.INSTANCE.tick(Minecraft.getInstance());
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) ->
                MeteorShowerClientState.INSTANCE.clearAll());
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> new PrettyMeteorsConfigScreen(parent)));
    }

    static void handlePayload(MeteorShowerPayload payload) {
        MeteorShowerClientState.INSTANCE.applyPayload(Minecraft.getInstance(), payload);
    }

    private static final class ForgeClientPlatformServices implements ClientPlatformServices {
        @Override
        public String loaderName() {
            return "Forge";
        }
    }
}
