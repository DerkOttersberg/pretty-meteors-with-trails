package io.github.derkottersberg.prettymeteors.internal;

import com.derko.prettymeteors.PrettyMeteorsMod;
import com.derko.prettymeteors.client.MeteorRenderTypes;
import java.util.Objects;

public final class PrettyMeteorsClientBootstrap {
    private static ClientPlatformServices platform;

    private PrettyMeteorsClientBootstrap() {
    }

    public static void initialize(ClientPlatformServices services) {
        if (platform != null) {
            throw new IllegalStateException("Pretty Meteors client has already been initialized");
        }
        platform = Objects.requireNonNull(services, "services");
        MeteorRenderTypes.initialize();
        PrettyMeteorsMod.LOGGER.info("Pretty Meteors client initialized on {}", platform.loaderName());
    }
}
