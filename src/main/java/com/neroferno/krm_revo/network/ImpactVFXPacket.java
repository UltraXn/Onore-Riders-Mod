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
 * Server -> Client packet for short impact spark effects.
 */
@SuppressWarnings("null")
public record ImpactVFXPacket(double posX, double posY, double posZ, double dirX, double dirY, double dirZ) implements CustomPacketPayload {

    public static final Type<ImpactVFXPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "impact_vfx"));

    public static final StreamCodec<FriendlyByteBuf, ImpactVFXPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, pkt) -> {
                        buf.writeDouble(pkt.posX);
                        buf.writeDouble(pkt.posY);
                        buf.writeDouble(pkt.posZ);
                        buf.writeDouble(pkt.dirX);
                        buf.writeDouble(pkt.dirY);
                        buf.writeDouble(pkt.dirZ);
                    },
                    buf -> new ImpactVFXPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ImpactVFXPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLLoader.getDist() != Dist.CLIENT) return;
            TransformVFXHelper.playImpactSparks(new Vec3(packet.posX, packet.posY, packet.posZ), new Vec3(packet.dirX, packet.dirY, packet.dirZ));
        });
    }
}

