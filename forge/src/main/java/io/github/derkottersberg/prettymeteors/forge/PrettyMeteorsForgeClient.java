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
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

final class PrettyMeteorsForgeClient {
    private PrettyMeteorsForgeClient() {
    }

    static void initialize(FMLJavaModLoadingContext context) {
        PrettyMeteorsClientBootstrap.initialize(new ForgeClientPlatformServices());
        TickEvent.ClientTickEvent.Post.BUS.addListener(event ->
                MeteorShowerClientState.INSTANCE.tick(Minecraft.getInstance()));
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event ->
                MeteorShowerClientState.INSTANCE.clearAll());
        context.getContainer().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(PrettyMeteorsConfigScreen::new));
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
