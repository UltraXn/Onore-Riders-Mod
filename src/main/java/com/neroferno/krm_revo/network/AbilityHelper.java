package com.neroferno.krm_revo.network;

import com.neroferno.krm_revo.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Handles the server-side execution of combat abilities.
 */
@SuppressWarnings("null")
public class AbilityHelper {

    public static void executeAbility(ServerPlayer player, String abilityId) {
        // First, check if the player is transformed
        if (!TransformationHelper.isTransformed(player)) {
            return;
        }

        // Check if the belt active ability is on cooldown
        if (player.getCooldowns().isOnCooldown(ModItems.DRIVER_BELT.get())) {
            return;
        }

        if ("rider_kick".equals(abilityId)) {
            executeRiderKick(player);
            // Apply a 10-second (200 ticks) cooldown to the Belt item
            player.getCooldowns().addCooldown(ModItems.DRIVER_BELT.get(), 200);
        }
    }

    private static void executeRiderKick(ServerPlayer player) {
        // 1. Play the 'kick' animation for all tracking clients
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new PlayerAnimationPacket(player.getId(), "kick"));

        // 2. Dash forward - apply velocity in the direction the player is looking
        Vec3 lookVec = player.getLookAngle();
        Vec3 dashVec = lookVec.scale(2.5D); // Propel forward
        player.setDeltaMovement(dashVec);
        player.hurtMarked = true; // Force sync to client
        // We must sync the new velocity to the client or use a custom DashPacket 
        // to tell the client to move. For simplicity, we apply entity DeltaMovement and let vanilla sync
        // However, player movement is client-authoritative usually.
        // A better approach is to send a packet to the client to dash, OR just calculate the impact here.
        // For a true "dash", we should tell the client to dart forward.
        // Since we are server-side, setting DeltaMovement works well enough for a sudden burst if Hurt() cancels client sync,
        // but let's notify the client via a custom Dash velocity later if needed.
        // For now, let's just do the AOE damage in front of the player.

        // Calculate impact zone 3 blocks in front
        Vec3 impactCenter = player.position().add(lookVec.scale(3.0D));

        // Create an explosive VFX at the impact center
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new TransformVFXPacket(impactCenter.x, impactCenter.y + 1.0, impactCenter.z, true)); // Re-using lightning VFX

        // 3. Deal AOE damage
        AABB impactBox = new AABB(impactCenter.x - 2, impactCenter.y - 1, impactCenter.z - 2,
                impactCenter.x + 2, impactCenter.y + 2, impactCenter.z + 2);
        
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, impactBox,
                entity -> entity != player && entity.isAlive());

        for (LivingEntity target : targets) {
            // Apply high burst damage
            target.hurt(player.damageSources().playerAttack(player), 20.0f); // 10 Hearts of damage
            
            // Knock them back
            Vec3 knockback = target.position().subtract(player.position()).normalize().scale(1.5D);
            target.push(knockback.x, 0.5D, knockback.z);
        }
    }
}

