package com.neroferno.krm_revo.network;

import com.neroferno.krm_revo.KRMRevoMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

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
    }

    /**
     * Server-side handler for the K-key transformation request.
     * Uses the custom NBT belt-equipped flag instead of Curios.
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

                // Broadcast VFX packet to all nearby players
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                        new TransformVFXPacket(
                                player.getX(), player.getY(), player.getZ(),
                                TransformationHelper.isTransformed(player)
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
}
