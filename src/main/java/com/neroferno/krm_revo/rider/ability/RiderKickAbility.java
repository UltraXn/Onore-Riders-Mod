package com.neroferno.krm_revo.rider.ability;

import com.neroferno.krm_revo.network.PlayerAnimationPacket;
import com.neroferno.krm_revo.network.TransformVFXPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class RiderKickAbility implements RiderAbility {

    @Override
    public String getId() {
        return "rider_kick";
    }

    @Override
    public int getCooldownTicks() {
        return 200; // 10s
    }

    @Override
    public void execute(ServerPlayer player) {
        // 1. Play the 'kick' animation for all tracking clients
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new PlayerAnimationPacket(player.getId(), "kick"));

        // 2. Dash forward - apply velocity in the direction the player is looking
        Vec3 lookVec = player.getLookAngle();
        Vec3 dashVec = lookVec.scale(2.5D);
        player.setDeltaMovement(dashVec);
        player.hurtMarked = true; // Force sync to client

        // Calculate impact zone 3 blocks in front
        Vec3 impactCenter = player.position().add(lookVec.scale(3.0D));

        // VFX at the impact center
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new TransformVFXPacket(impactCenter.x, impactCenter.y + 1.0, impactCenter.z, true));

        // 3. Deal AOE damage
        AABB impactBox = new AABB(impactCenter.x - 2, impactCenter.y - 1, impactCenter.z - 2,
                impactCenter.x + 2, impactCenter.y + 2, impactCenter.z + 2);

        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, impactBox,
                entity -> entity != player && entity.isAlive());

        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().playerAttack(player), 20.0f);

            Vec3 knockback = target.position().subtract(player.position()).normalize().scale(1.5D);
            target.push(knockback.x, 0.5D, knockback.z);
        }
    }
}
