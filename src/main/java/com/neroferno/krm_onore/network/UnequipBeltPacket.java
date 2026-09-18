package com.neroferno.krm_onore.network;

import com.neroferno.krm_onore.KRMRevoMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet sent from the client to the server when the player clicks the
 * "Quitarse el belt" button in the RiderScreen.
 */
@SuppressWarnings("null")
public record UnequipBeltPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<UnequipBeltPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "unequip_belt"));

    public static final StreamCodec<FriendlyByteBuf, UnequipBeltPacket> STREAM_CODEC =
            StreamCodec.unit(new UnequipBeltPacket());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
