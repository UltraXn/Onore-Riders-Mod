package com.neroferno.krm_onore.network;

import com.neroferno.krm_onore.KRMRevoMod;
import com.neroferno.krm_onore.client.GrowingPunchAnimationPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;

public record GrowingPunchAnimationPacket(int playerId, int attackIndex, boolean heavy) implements CustomPacketPayload {
    public static final Type<GrowingPunchAnimationPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "growing_punch_animation"));

    public static final StreamCodec<FriendlyByteBuf, GrowingPunchAnimationPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.playerId());
                buf.writeVarInt(packet.attackIndex());
                buf.writeBoolean(packet.heavy());
            },
            buf -> new GrowingPunchAnimationPacket(buf.readVarInt(), buf.readVarInt(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GrowingPunchAnimationPacket packet,
                              net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLLoader.getDist() == Dist.CLIENT) {
                GrowingPunchAnimationPacketHandler.handle(packet);
            }
        });
    }
}
