package com.neroferno.krm_onore.network;

import com.neroferno.krm_onore.KRMRevoMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SelectFormPacket(int formId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SelectFormPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "select_form"));

    public static final StreamCodec<ByteBuf, SelectFormPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SelectFormPacket::formId,
            SelectFormPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
