package com.neroferno.krm_revo.network;

import com.neroferno.krm_revo.KRMRevoMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import net.minecraft.world.item.ItemStack;

/**
 * Registers all network packets and their server-side handlers.
 * Subscribed to RegisterPayloadHandlersEvent from KRMRevoMod.
 */
@SuppressWarnings("null")
public class ModNetwork {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(KRMRevoMod.MODID).versioned("1.0");

        // Client -> Server: Player pressed K to toggle transformation
        registrar.playToServer(
                TransformPacket.TYPE,
                TransformPacket.STREAM_CODEC,
                ModNetwork::handleTransformPacket
        );

        // Server -> Client: Play VFX at player position after transformation toggle
        registrar.playToClient(
                TransformVFXPacket.TYPE,
                TransformVFXPacket.STREAM_CODEC,
                TransformVFXPacket::handle
        );

        // Server -> Client: Trigger custom player animation
        registrar.playToClient(
                PlayerAnimationPacket.TYPE,
                PlayerAnimationPacket.STREAM_CODEC,
                PlayerAnimationPacket::handleData
        );

        // Server -> Client: Short electric impact sparks
        registrar.playToClient(
                ImpactVFXPacket.TYPE,
                ImpactVFXPacket.STREAM_CODEC,
                ImpactVFXPacket::handle
        );

        // Client -> Server: Trigger an active combat ability
        registrar.playToServer(
                AbilityPacket.TYPE,
                AbilityPacket.STREAM_CODEC,
                ModNetwork::handleAbilityPacket
        );

        // Client -> Server: Open custom Rider Inventory Menu
        registrar.playToServer(
                OpenRiderMenuPacket.TYPE,
                OpenRiderMenuPacket.STREAM_CODEC,
                ModNetwork::handleOpenMenuPacket
        );

        // Server -> Client: Sync Rider State (Transformed, Belt Equipped)
        registrar.playToClient(
                SyncRiderStatePacket.TYPE,
                SyncRiderStatePacket.STREAM_CODEC,
                SyncRiderStatePacket::handle
        );

        // Client -> Server: Unequip the Driver Belt from Rider Inventory
        registrar.playToServer(
                UnequipBeltPacket.TYPE,
                UnequipBeltPacket.STREAM_CODEC,
                ModNetwork::handleUnequipPacket
        );

        // Server -> Client: Sync player velocity (anti-rubberbanding dash)
        registrar.playToClient(
                SyncVelocityPacket.TYPE,
                SyncVelocityPacket.STREAM_CODEC,
                SyncVelocityPacket::handle
        );

        // Server -> Client: Sync target entity and coordinates for stasis freeze
        registrar.playToClient(
                SyncRiderKickTargetPacket.TYPE,
                SyncRiderKickTargetPacket.STREAM_CODEC,
                SyncRiderKickTargetPacket::handle
        );

        // Client -> Server: Select transformation form
        registrar.playToServer(
                SelectFormPacket.TYPE,
                SelectFormPacket.STREAM_CODEC,
                ModNetwork::handleSelectFormPacket
        );
    }

    /**
     * Server-side handler for the K-key transformation request.
     * Uses the custom NBT belt-equipped flag.
     */
    private static void handleTransformPacket(TransformPacket packet,
                                               net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!player.isAlive()) return;

            // Check if the belt is equipped via our custom system
            if (TransformationHelper.isBeltEquipped(player)) {
                
                // Enforce a 3-second (60 ticks) cooldown
                long currentTime = player.level().getGameTime();
                long lastTime = TransformationHelper.getLastTransformTime(player);
                
                if (currentTime - lastTime < 60) {
                    player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("krm_revo.transform.cooldown"), 
                        true
                    );
                    return; // Abort transformation
                }
                
                // Update the last transformation timestamp
                TransformationHelper.setLastTransformTime(player, currentTime);

                TransformationHelper.toggleTransformation(player);

                boolean isTransformed = TransformationHelper.isTransformed(player);
                boolean hasBelt = TransformationHelper.isBeltEquipped(player);
                int riderKickState = player.getPersistentData().getInt("RiderKickState");
                int form = player.getPersistentData().getInt("krm_revo:form");

                // Sync the state immediately to all clients to avoid 10-tick lag
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                        new SyncRiderStatePacket(player.getId(), isTransformed, hasBelt, riderKickState, form));

                // Broadcast VFX packet to all nearby players
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                        new TransformVFXPacket(
                                player.getX(), player.getY(), player.getZ(),
                                isTransformed
                        ));

                // Broadcast animation packet
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                        new PlayerAnimationPacket(player.getId(), "transform"));
            } else {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("krm_revo.transform.no_belt"),
                        true
                );
                KRMRevoMod.LOGGER.debug("{} tried to transform without a Driver Belt.", player.getName().getString());
            }
        });
    }

    /**
     * Server-side handler for the V-key ability request.
     */
    private static void handleAbilityPacket(AbilityPacket packet,
                                              net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!player.isAlive()) return;

            AbilityHelper.executeAbility(player, packet.abilityId());
        });
    }

    /**
     * Server-side handler for opening the Rider Menu
     */
    private static void handleOpenMenuPacket(OpenRiderMenuPacket packet,
                                               net.neoforged.neoforge.network.handling.IPayloadContext context) {
        packet.handle(context);
    }

    /**
     * Server-side handler for unequipping the belt from the Rider Inventory.
     */
    private static void handleUnequipPacket(UnequipBeltPacket packet,
                                             net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!player.isAlive()) return;

            if (player.hasData(com.neroferno.krm_revo.attachment.ModAttachments.RIDER_INVENTORY)) {
                net.neoforged.neoforge.items.ItemStackHandler inv = player.getData(com.neroferno.krm_revo.attachment.ModAttachments.RIDER_INVENTORY);
                ItemStack beltStack = inv.getStackInSlot(0);

                if (!beltStack.isEmpty()) {
                    // Detransform if transformed
                    if (TransformationHelper.isTransformed(player)) {
                        TransformationHelper.setTransformed(player, false);
                        // Broadcast VFX packet to detransform
                        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                                new TransformVFXPacket(player.getX(), player.getY(), player.getZ(), false));
                    }

                    ItemStack toAdd = beltStack.copy();
                    inv.setStackInSlot(0, ItemStack.EMPTY);

                    // Try to put it back in player's inventory, drop it on the ground if full
                    if (!player.getInventory().add(toAdd)) {
                        player.drop(toAdd, false);
                    }

                    // Sync the new empty state to the client and all tracking players
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                            new SyncRiderStatePacket(player.getId(), false, false, 0, 0));
                    
                    // Close the current screen (container)
                    player.closeContainer();
                }
            }
        });
    }

    /**
     * Server-side handler for form selection.
     */
    private static void handleSelectFormPacket(SelectFormPacket packet,
                                               net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!player.isAlive()) return;

            // Save the selected form ID
            player.getPersistentData().putInt("krm_revo:form", packet.formId());

            // Sync the updated state to tracking clients and self
            boolean isTransformed = TransformationHelper.isTransformed(player);
            boolean hasBelt = TransformationHelper.isBeltEquipped(player);
            int riderKickState = player.getPersistentData().getInt("RiderKickState");

            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                    new SyncRiderStatePacket(player.getId(), isTransformed, hasBelt, riderKickState, packet.formId()));
        });
    }
}
