package com.neroferno.krm_revo.network;

import com.neroferno.krm_revo.KRMRevoMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@SuppressWarnings({"null", "resource"})
public record SyncRiderKickTargetPacket(int riderPlayerId, int targetEntityId, double targetX, double targetY, double targetZ) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncRiderKickTargetPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "sync_rider_kick_target"));

    public static final StreamCodec<ByteBuf, SyncRiderKickTargetPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SyncRiderKickTargetPacket::riderPlayerId,
            ByteBufCodecs.INT, SyncRiderKickTargetPacket::targetEntityId,
            ByteBufCodecs.DOUBLE, SyncRiderKickTargetPacket::targetX,
            ByteBufCodecs.DOUBLE, SyncRiderKickTargetPacket::targetY,
            ByteBufCodecs.DOUBLE, SyncRiderKickTargetPacket::targetZ,
            SyncRiderKickTargetPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncRiderKickTargetPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            Entity rider = mc.level.getEntity(packet.riderPlayerId());
            if (rider instanceof Player player) {
                player.getPersistentData().putInt("RiderKickTargetId", packet.targetEntityId());
                player.getPersistentData().putDouble("RiderKickTargetX", packet.targetX());
                player.getPersistentData().putDouble("RiderKickTargetY", packet.targetY());
                player.getPersistentData().putDouble("RiderKickTargetZ", packet.targetZ());
            }
        });
    }
}
