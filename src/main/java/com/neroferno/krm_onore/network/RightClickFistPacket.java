package com.neroferno.krm_onore.network;

import com.neroferno.krm_onore.KRMRevoMod;
import com.neroferno.krm_onore.combat.RiderFistCombat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record RightClickFistPacket() implements CustomPacketPayload {
    public static final Type<RightClickFistPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "right_click_fist"));

    public static final StreamCodec<FriendlyByteBuf, RightClickFistPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {},
            buf -> new RightClickFistPacket()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RightClickFistPacket packet,
                              net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                RiderFistCombat.handleRightClick(player);
            }
        });
    }
}
