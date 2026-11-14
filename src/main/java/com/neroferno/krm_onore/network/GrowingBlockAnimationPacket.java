package com.neroferno.krm_onore.network;

import com.neroferno.krm_onore.KRMRevoMod;
import com.neroferno.krm_onore.client.GrowingBlockAnimationPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;

/**
 * Servidor -> clientes: el jugador bloquea un golpe al retroceder. holdTicks > 0 mantiene la pose de block (0.25 s)
 * ese tiempo (se renueva si llega otro golpe); holdTicks == 0 la suelta de inmediato.
 */
public record GrowingBlockAnimationPacket(int playerId, int holdTicks) implements CustomPacketPayload {
    public static final Type<GrowingBlockAnimationPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "growing_block_animation"));

    public static final StreamCodec<FriendlyByteBuf, GrowingBlockAnimationPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.playerId());
                buf.writeVarInt(packet.holdTicks());
            },
            buf -> new GrowingBlockAnimationPacket(buf.readVarInt(), buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GrowingBlockAnimationPacket packet,
                              net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLLoader.getDist() == Dist.CLIENT) {
                GrowingBlockAnimationPacketHandler.handle(packet);
            }
        });
    }
}
