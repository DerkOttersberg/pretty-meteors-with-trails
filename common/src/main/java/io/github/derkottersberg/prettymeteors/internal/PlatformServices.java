package io.github.derkottersberg.prettymeteors.internal;

import com.derko.prettymeteors.network.MeteorShowerPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Loader operations required by the common server implementation. */
public interface PlatformServices {
    String loaderName();

    void broadcast(ServerLevel level, MeteorShowerPayload payload);

    void sendToPlayer(ServerPlayer player, MeteorShowerPayload payload);
}
