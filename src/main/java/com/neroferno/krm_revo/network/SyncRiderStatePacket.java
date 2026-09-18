package com.neroferno.krm_revo.network;

import com.neroferno.krm_revo.KRMRevoMod;
import com.neroferno.krm_revo.attachment.ModAttachments;
import com.neroferno.krm_revo.rider.RiderDefinition;
import com.neroferno.krm_revo.rider.RiderRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * @param riderId id of the Rider whose Driver is equipped (e.g. "kuuga"),
 *                or "" when hasBelt is false. FIX: previously this packet
 *                only carried a hasBelt boolean, so any transformed remote
 *                player was rendered/tracked as Kuuga regardless of which
 *                Rider they actually had equipped.
 */
@SuppressWarnings({"null", "resource"})
public record SyncRiderStatePacket(int entityId, boolean isTransformed, boolean hasBelt, String riderId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncRiderStatePacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "sync_rider_state"));

    public static final StreamCodec<ByteBuf, SyncRiderStatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SyncRiderStatePacket::entityId,
            ByteBufCodecs.BOOL, SyncRiderStatePacket::isTransformed,
            ByteBufCodecs.BOOL, SyncRiderStatePacket::hasBelt,
            ByteBufCodecs.STRING_UTF8, SyncRiderStatePacket::riderId,
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

                // Update specific Rider Inventory for belt
                ItemStackHandler inv = player.getData(ModAttachments.RIDER_INVENTORY);
                if (packet.hasBelt()) {
                    // FIX: resolve the correct Driver item from riderId instead
                    // of always inserting ModItems.DRIVER_BELT.
                    Item driverItem = resolveDriverItem(packet.riderId());
                    if (inv.getStackInSlot(0).isEmpty()) {
                        inv.setStackInSlot(0, new ItemStack(driverItem));
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

    /**
     * Looks up the Driver item for the synced riderId. Falls back to the
     * original Driver Belt if the id is blank or unrecognized, so a stale
     * sender (or bad data) degrades gracefully instead of throwing.
     */
    private static Item resolveDriverItem(String riderId) {
        if (riderId != null && !riderId.isEmpty()) {
            RiderDefinition rider = RiderRegistry.get(riderId);
            if (rider != null) {
                return rider.getDriver();
            }
        }
        return com.neroferno.krm_revo.item.ModItems.DRIVER_BELT.get();
    }
}
