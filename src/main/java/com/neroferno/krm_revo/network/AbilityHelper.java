package com.neroferno.krm_revo.network;

import com.neroferno.krm_revo.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;

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

        if ("rider_kick".equals(abilityId)) {
            // Check if player has a valid target within range before executing
            if (!hasValidTarget(player)) {
                MutableComponent msg = Component.translatable("chat.krm_revo.no_target_lock");
                msg.withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                player.displayClientMessage(msg, true);
                return;
            }

            // Check if player's custom cooldown is active
            long now = player.level().getGameTime();
            long cooldownEnd = player.getPersistentData().getLong("RiderKickCooldownEnd");
            if (now < cooldownEnd) {
                long remainingTicks = cooldownEnd - now;
                double remainingSeconds = (double) remainingTicks / 20.0D;
                
                MutableComponent msg = Component.translatable(
                        "chat.krm_revo.cooldown_actionbar", String.format("%.1f", remainingSeconds));
                msg.withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                
                player.displayClientMessage(msg, true);
                return;
            }

            executeRiderKick(player);
            // Apply a 10-second (200 ticks) cooldown to the Belt item visually
            player.getCooldowns().addCooldown(ModItems.DRIVER_BELT.get(), 200);
            player.getPersistentData().putLong("RiderKickCooldownEnd", now + 200);
        }
    }

    private static void executeRiderKick(ServerPlayer player) {
        // Check if already in the middle of a Rider Kick
        if (player.getPersistentData().getInt("RiderKickState") > 0) {
            return;
        }

        // Initialize state machine
        player.getPersistentData().putInt("RiderKickState", 1); // 1 = Leap Phase
        player.getPersistentData().putInt("RiderKickTicks", 0);
        player.getPersistentData().putBoolean("RiderKickFallImmunity", true);

        // 1. Play the 'kick' animation for all tracking clients
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new PlayerAnimationPacket(player.getId(), "kick"));

        // 2. Play wind/electric launch sounds
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.WIND_CHARGE_THROW, net.minecraft.sounds.SoundSource.PLAYERS, 2.0f, 0.6f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.FIRECHARGE_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1.5f, 0.8f);

        // 3. Propel the player upwards and slightly forward in the direction they are looking
        Vec3 lookVec = player.getLookAngle();
        double horizontalScale = 0.45D;

        // Find target first to determine leap dynamics
        double dist = 10.0D; // Default fallback distance
        LivingEntity target = findTarget(player);
        if (target != null) {
            dist = player.distanceTo(target);
        }

        // Scale vertical velocity between 0.6D (close range) and 1.5D (far range)
        double verticalVelocity = 0.6D + (dist / 20.0D) * 0.9D;
        if (verticalVelocity > 1.6D) verticalVelocity = 1.6D;

        Vec3 jumpVec = new Vec3(lookVec.x * horizontalScale, verticalVelocity, lookVec.z * horizontalScale);
        player.setDeltaMovement(jumpVec);
        player.hurtMarked = true; // Sync velocity to client

        // Sync velocity to the client of this player so the leap is extremely smooth and client-authoritative
        PacketDistributor.sendToPlayer(player, new SyncVelocityPacket(jumpVec.x, jumpVec.y, jumpVec.z));

        // Immediately sync Rider State to tracking clients so client-side particles start
        int form = player.getPersistentData().getInt("krm_revo:form");
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new SyncRiderStatePacket(player.getId(), true, true, 1, form));

        // 4. Raycast to find targeted entity to freeze/lock
        findAndLockTarget(player);
    }

    public static LivingEntity findTarget(Player player) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle().normalize();
        double range = 20.0D;
        AABB searchBox = player.getBoundingBox().inflate(range);
        List<LivingEntity> list = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                entity -> entity != player && entity.isAlive() && player.hasLineOfSight(entity));

        LivingEntity closestTarget = null;
        double closestDist = Double.MAX_VALUE;

        for (LivingEntity target : list) {
            Vec3 toTarget = target.position().add(0, target.getEyeHeight() / 2.0F, 0).subtract(eyePos);
            double dist = toTarget.length();
            if (dist > range) continue;

            Vec3 toTargetNorm = toTarget.normalize();
            double dot = lookVec.dot(toTargetNorm);

            // 0.85 dot product corresponds to about ~30 degrees cone of vision. This is perfect for auto-lock!
            if (dot > 0.85D) {
                if (dist < closestDist) {
                    closestDist = dist;
                    closestTarget = target;
                }
            }
        }
        return closestTarget;
    }

    private static void findAndLockTarget(ServerPlayer player) {
        LivingEntity closestTarget = findTarget(player);
        if (closestTarget != null) {
            player.getPersistentData().putInt("RiderKickTargetId", closestTarget.getId());
            // Store stasis coordinates to absolutely prevent any client/server desync drift or gravity falling
            player.getPersistentData().putDouble("RiderKickTargetX", closestTarget.getX());
            player.getPersistentData().putDouble("RiderKickTargetY", closestTarget.getY());
            player.getPersistentData().putDouble("RiderKickTargetZ", closestTarget.getZ());

            // Freeze target's velocity immediately on the server
            closestTarget.setDeltaMovement(Vec3.ZERO);
            closestTarget.hurtMarked = true;
            if (closestTarget instanceof net.minecraft.world.entity.Mob mob) {
                mob.setNoAi(true);
            }
            // Apply heavy slowness effect so they don't slide/drift
            closestTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 100, false, false));

            // Sync target coordinates and ID to clients so they completely freeze on client screens
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                    new SyncRiderKickTargetPacket(player.getId(), closestTarget.getId(), closestTarget.getX(), closestTarget.getY(), closestTarget.getZ()));
        } else {
            player.getPersistentData().putInt("RiderKickTargetId", 0);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                    new SyncRiderKickTargetPacket(player.getId(), 0, 0, 0, 0));
        }
    }

    private static boolean hasValidTarget(ServerPlayer player) {
        return findTarget(player) != null;
    }
}
