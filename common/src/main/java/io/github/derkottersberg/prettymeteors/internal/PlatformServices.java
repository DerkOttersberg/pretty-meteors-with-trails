package io.github.derkottersberg.prettymeteors.internal;

import com.derko.prettymeteors.network.MeteorShowerPayload;
import java.nio.file.Path;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Loader operations required by the common server implementation. */
public interface PlatformServices {
    String loaderName();

    Path configDirectory();

    void broadcast(ServerLevel level, MeteorShowerPayload payload);

    void sendToPlayer(ServerPlayer player, MeteorShowerPayload payload);
}
