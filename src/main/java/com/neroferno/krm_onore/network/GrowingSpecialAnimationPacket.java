package com.neroferno.krm_onore.network;

import com.neroferno.krm_onore.KRMRevoMod;
import com.neroferno.krm_onore.client.GrowingSpecialAnimationPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;

/** Servidor -> clientes: reproduce una animacion especial del traje y del cinturon (por ahora "especial3"). */
public record GrowingSpecialAnimationPacket(int playerId, String animation) implements CustomPacketPayload {
    public static final Type<GrowingSpecialAnimationPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "growing_special_animation"));

    public static final StreamCodec<FriendlyByteBuf, GrowingSpecialAnimationPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.playerId());
                buf.writeUtf(packet.animation());
            },
            buf -> new GrowingSpecialAnimationPacket(buf.readVarInt(), buf.readUtf())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GrowingSpecialAnimationPacket packet,
                              net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLLoader.getDist() == Dist.CLIENT) {
                GrowingSpecialAnimationPacketHandler.handle(packet);
            }
        });
    }
}
