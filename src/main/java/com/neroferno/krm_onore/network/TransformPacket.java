package com.neroferno.krm_onore.network;

import com.neroferno.krm_onore.KRMRevoMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet sent from the client to the server when the player presses the K key.
 * The server then validates the player has the belt equipped and toggles the
 * transformation state.
 */
@SuppressWarnings("null")
public record TransformPacket(boolean moving) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TransformPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "transform_toggle"));

    /** Codec carries the toggle intent and whether a movement input is active. */
    public static final StreamCodec<FriendlyByteBuf, TransformPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, TransformPacket::moving,
                    TransformPacket::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
