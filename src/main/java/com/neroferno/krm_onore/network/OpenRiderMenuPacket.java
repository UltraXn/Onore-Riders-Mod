package com.neroferno.krm_onore.network;

import com.neroferno.krm_onore.menu.RiderMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@SuppressWarnings("null")
public record OpenRiderMenuPacket() implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final Type<OpenRiderMenuPacket> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("krm_revo", "open_rider_menu"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf, OpenRiderMenuPacket> STREAM_CODEC = 
            net.minecraft.network.codec.StreamCodec.unit(new OpenRiderMenuPacket());

    @Override
    public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(new SimpleMenuProvider(
                        (containerId, playerInventory, player) -> new RiderMenu(containerId, playerInventory, null),
                        Component.translatable("gui.krm_revo.rider_inventory")
                ));
            }
        });
    }
}
