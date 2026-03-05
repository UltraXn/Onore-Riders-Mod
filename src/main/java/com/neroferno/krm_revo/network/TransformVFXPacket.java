package com.neroferno.krm_revo.network;

import com.neroferno.krm_revo.KRMRevoMod;
import com.neroferno.krm_revo.client.vfx.TransformVFXHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;

/**
 * Packet sent from the server to all clients in range when a player
 * transforms or de-transforms. Triggers the VFX burst at the player's position.
 *
 * @param posX  World X position of the effect
 * @param posY  World Y position of the effect
 * @param posZ  World Z position of the effect
 * @param transforming  true = transforming, false = detransforming
 */
@SuppressWarnings("null")
public record TransformVFXPacket(double posX, double posY, double posZ,
                                  boolean transforming) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TransformVFXPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "transform_vfx"));

    public static final StreamCodec<FriendlyByteBuf, TransformVFXPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, pkt) -> {
                        buf.writeDouble(pkt.posX);
                        buf.writeDouble(pkt.posY);
                        buf.writeDouble(pkt.posZ);
                        buf.writeBoolean(pkt.transforming);
                    },
                    buf -> new TransformVFXPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readBoolean())
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Client-side handler â€” called by ModNetwork when this packet arrives. */
    public static void handle(TransformVFXPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLLoader.getDist() != Dist.CLIENT) return;
            Vec3 pos = new Vec3(packet.posX, packet.posY, packet.posZ);
            if (packet.transforming) {
                TransformVFXHelper.playTransformBurst(pos);
            } else {
                TransformVFXHelper.playDetransformBurst(pos);
            }
        });
    }
}

