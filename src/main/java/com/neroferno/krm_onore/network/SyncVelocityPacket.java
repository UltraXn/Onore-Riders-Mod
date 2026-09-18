package com.neroferno.krm_onore.network;

import com.neroferno.krm_onore.KRMRevoMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet sent from server to client to synchronize/apply velocity directly
 * to the client-side local player, preventing network rubberbanding during dashes.
 */
@SuppressWarnings({"null", "resource"})
public record SyncVelocityPacket(double xd, double yd, double zd) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncVelocityPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "sync_velocity"));

    public static final StreamCodec<ByteBuf, SyncVelocityPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, SyncVelocityPacket::xd,
            ByteBufCodecs.DOUBLE, SyncVelocityPacket::yd,
            ByteBufCodecs.DOUBLE, SyncVelocityPacket::zd,
            SyncVelocityPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Executes client-side to set the player's movement delta.
     */
    public static void handle(SyncVelocityPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.setDeltaMovement(packet.xd(), packet.yd(), packet.zd());
            }
        });
    }
}
