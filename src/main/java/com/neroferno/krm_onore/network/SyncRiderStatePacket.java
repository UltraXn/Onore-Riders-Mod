package com.neroferno.krm_onore.network;

import com.neroferno.krm_onore.KRMRevoMod;
import com.neroferno.krm_onore.attachment.ModAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@SuppressWarnings({"null", "resource"})
public record SyncRiderStatePacket(int entityId, boolean isTransformed, boolean hasBelt, int riderKickState, int form) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncRiderStatePacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "sync_rider_state"));

    public static final StreamCodec<ByteBuf, SyncRiderStatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SyncRiderStatePacket::entityId,
            ByteBufCodecs.BOOL, SyncRiderStatePacket::isTransformed,
            ByteBufCodecs.BOOL, SyncRiderStatePacket::hasBelt,
            ByteBufCodecs.INT, SyncRiderStatePacket::riderKickState,
            ByteBufCodecs.INT, SyncRiderStatePacket::form,
            SyncRiderStatePacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncRiderStatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            Entity entity = mc.level.getEntity(packet.entityId());
            if (entity instanceof Player player) {
                // Update persistent data for transformation
                player.getPersistentData().putBoolean("krm_revo:transformed", packet.isTransformed());
                player.getPersistentData().putInt("RiderKickState", packet.riderKickState());
                player.getPersistentData().putInt("krm_revo:form", packet.form());

                // Update specific Rider Inventory for belt
                ItemStackHandler inv = player.getData(ModAttachments.RIDER_INVENTORY);
                if (packet.hasBelt()) {
                    if (inv.getStackInSlot(0).isEmpty()) {
                        inv.setStackInSlot(0, new ItemStack(com.neroferno.krm_onore.item.ModItems.DRIVER_BELT.get()));
                    }
                } else {
                    // Clear the belt slot
                    if (!inv.getStackInSlot(0).isEmpty()) {
                        inv.setStackInSlot(0, ItemStack.EMPTY);
                    }
                }
            }
        });
    }
}
