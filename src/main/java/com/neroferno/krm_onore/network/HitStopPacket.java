package com.neroferno.krm_onore.network;

import com.neroferno.krm_onore.KRMRevoMod;
import com.neroferno.krm_onore.client.HitStopHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;

/**
 * Server -> Client: un golpe del puño conecto. El cliente congela la animacion del atacante
 * {@code hitStopTicks} ticks y, si el atacante es el jugador local, hace temblar la camara.
 */
public record HitStopPacket(int attackerId, int hitStopTicks, float shake) implements CustomPacketPayload {
    public static final Type<HitStopPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "hit_stop"));

    public static final StreamCodec<FriendlyByteBuf, HitStopPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.attackerId());
                buf.writeVarInt(packet.hitStopTicks());
                buf.writeFloat(packet.shake());
            },
            buf -> new HitStopPacket(buf.readVarInt(), buf.readVarInt(), buf.readFloat())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HitStopPacket packet,
                              net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLLoader.getDist() == Dist.CLIENT) {
                HitStopHandler.handle(packet);
            }
        });
    }
}
