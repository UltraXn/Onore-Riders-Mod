package com.neroferno.krm_revo.network;

import com.neroferno.krm_revo.KRMRevoMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-to-server packet to trigger an active ability.
 */
@SuppressWarnings("null")
public record AbilityPacket(String abilityId) implements CustomPacketPayload {

    public static final Type<AbilityPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "ability"));

    public static final StreamCodec<FriendlyByteBuf, AbilityPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeUtf(packet.abilityId),
            buf -> new AbilityPacket(buf.readUtf())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

