package com.derko.prettymeteors.network;

import com.derko.prettymeteors.MeteorShowerConfig;
import com.derko.prettymeteors.PrettyMeteorsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record MeteorShowerPayload(
        boolean active,
        long startTick,
        int durationTicks,
        float meteorsPerSecond,
        float yawDegrees,
        float pitchDegrees,
        float angularSpreadDegrees,
        float shellRadius,
        float laneSpread,
        float heightOffset,
        float baseSpeed,
        float speedVariance,
        int minLifetimeTicks,
        int maxLifetimeTicks,
        double originX,
        double originY,
        double originZ,
        int seed) implements CustomPayload {

    public static final CustomPayload.Id<MeteorShowerPayload> ID = new CustomPayload.Id<>(PrettyMeteorsMod.id("shower_state"));

    public static final PacketCodec<RegistryByteBuf, MeteorShowerPayload> CODEC = new PacketCodec<>() {
        @Override
        public MeteorShowerPayload decode(RegistryByteBuf buf) {
            return new MeteorShowerPayload(
                    buf.readBoolean(),
                    buf.readLong(),
                    buf.readVarInt(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readVarInt());
        }

        @Override
        public void encode(RegistryByteBuf buf, MeteorShowerPayload value) {
            buf.writeBoolean(value.active);
            buf.writeLong(value.startTick);
            buf.writeVarInt(value.durationTicks);
            buf.writeFloat(value.meteorsPerSecond);
            buf.writeFloat(value.yawDegrees);
            buf.writeFloat(value.pitchDegrees);
            buf.writeFloat(value.angularSpreadDegrees);
            buf.writeFloat(value.shellRadius);
            buf.writeFloat(value.laneSpread);
            buf.writeFloat(value.heightOffset);
            buf.writeFloat(value.baseSpeed);
            buf.writeFloat(value.speedVariance);
            buf.writeVarInt(value.minLifetimeTicks);
            buf.writeVarInt(value.maxLifetimeTicks);
            buf.writeDouble(value.originX);
            buf.writeDouble(value.originY);
            buf.writeDouble(value.originZ);
            buf.writeVarInt(value.seed);
        }
    };

    public static MeteorShowerPayload fromConfig(MeteorShowerConfig config) {
        return new MeteorShowerPayload(
                true,
                config.startTick(),
                config.durationTicks(),
                config.meteorsPerSecond(),
                config.yawDegrees(),
                config.pitchDegrees(),
                config.angularSpreadDegrees(),
                config.shellRadius(),
                config.laneSpread(),
                config.heightOffset(),
                config.baseSpeed(),
                config.speedVariance(),
                config.minLifetimeTicks(),
                config.maxLifetimeTicks(),
                config.originX(),
                config.originY(),
                config.originZ(),
                config.seed());
    }

    public static MeteorShowerPayload inactive() {
        return new MeteorShowerPayload(false, 0L, 0, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0, 0, 0.0, 0.0, 0.0, 0);
    }

    public MeteorShowerConfig toConfig() {
        return new MeteorShowerConfig(
                startTick,
                durationTicks,
                meteorsPerSecond,
                yawDegrees,
                pitchDegrees,
                angularSpreadDegrees,
                shellRadius,
                laneSpread,
                heightOffset,
                baseSpeed,
                speedVariance,
                minLifetimeTicks,
                maxLifetimeTicks,
                originX,
                originY,
                originZ,
                seed);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}