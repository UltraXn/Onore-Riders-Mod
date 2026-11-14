package com.neroferno.krm_onore.network;

import com.neroferno.krm_onore.KRMRevoMod;
import com.neroferno.krm_onore.client.GrabbedPoseClientHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Servidor -> clientes: un jugador queda tumbado (o se levanta) por el Especial 1. */
public record GrabbedPoseSyncPacket(int entityId, boolean grabbed) implements CustomPacketPayload {
    public static final Type<GrabbedPoseSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "grabbed_pose_sync"));

    public static final StreamCodec<ByteBuf, GrabbedPoseSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GrabbedPoseSyncPacket::entityId,
            ByteBufCodecs.BOOL, GrabbedPoseSyncPacket::grabbed,
            GrabbedPoseSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GrabbedPoseSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLLoader.getDist() == Dist.CLIENT) {
                GrabbedPoseClientHandler.handle(packet);
            }
        });
    }
}
