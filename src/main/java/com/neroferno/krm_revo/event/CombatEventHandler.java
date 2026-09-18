package com.neroferno.krm_revo.event;

import com.neroferno.krm_revo.KRMRevoMod;
import com.neroferno.krm_revo.network.ImpactVFXPacket;
import com.neroferno.krm_revo.network.TransformationHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import com.neroferno.krm_revo.network.SyncRiderStatePacket;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.minecraft.server.level.ServerPlayer;
import com.neroferno.krm_revo.network.SyncVelocityPacket;
import com.neroferno.krm_revo.network.TransformVFXPacket;
import java.util.List;

/**
 * Handles KRM-modified combat behavior.
 *
 * While transformed and attacking with an EMPTY hand:
 *   - Base damage is set to 8.0 (Iron Sword equivalent + bonus)
 *   - Knockback is increased
 *
 *
 * Future phases will add:
 *   - Combo system tracking
 */
@SuppressWarnings({"null"})
@EventBusSubscriber(modid = KRMRevoMod.MODID)
public class CombatEventHandler {


    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        DamageSource source = event.getSource();

        // Only care about player melee attacks
        if (!(source.getEntity() instanceof Player attacker)) return;

        // Player must be transformed
        if (!TransformationHelper.isTransformed(attacker)) return;

        // Attack must be unarmed (empty main hand)
        if (!attacker.getMainHandItem().isEmpty()) return;

        int form = attacker.getPersistentData().getInt("krm_revo:form");
        float damage = form == 1 ? 6.0f : 12.0f; // Growing = 6.0, Mighty = 12.0
        event.setNewDamage(damage);

        LivingEntity victim = event.getEntity();
        if (form == 1) { // Growing Form (Blanco)
            Vec3 look = attacker.getLookAngle().normalize();
            victim.push(look.x * 0.25D, 0.05D, look.z * 0.25D);
            victim.hurtMarked = true;
        } else { // Mighty Form (Rojo)
            victim.setRemainingFireTicks(40); // Set on fire for 2 seconds
            Vec3 look = attacker.getLookAngle().normalize();
            victim.push(look.x * 0.6D, 0.1D, look.z * 0.6D);
            victim.hurtMarked = true;

            // Electric / impact sparks sound
            attacker.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                    net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_IMPACT, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.8f);
        }

        KRMRevoMod.LOGGER.debug(
            "{} delivered a transformed unarmed strike (form {}) for {} damage to {}",
            attacker.getName().getString(),
            form,
            damage,
            victim.getName().getString()
        );

        Vec3 hitPos = victim.position().add(0, victim.getBbHeight() / 2.0, 0);
        Vec3 look = attacker.getViewVector(1.0f);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                attacker,
                new ImpactVFXPacket(hitPos.x, hitPos.y, hitPos.z, look.x, look.y, look.z)
        );
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (player.getPersistentData().getBoolean("RiderKickFallImmunity")) {
                event.setDistance(0.0f);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide) return;

        Player player = event.getEntity();
        int state = player.getPersistentData().getInt("RiderKickState");

        if (state > 0) {
            if (!player.isAlive()) {
                player.getPersistentData().putInt("RiderKickState", 0);
                player.getPersistentData().putBoolean("RiderKickFallImmunity", false);
                syncRiderKickState(player, 0);
                releaseTarget(player);
                return;
            }

            int ticks = player.getPersistentData().getInt("RiderKickTicks");
            ticks++;
            player.getPersistentData().putInt("RiderKickTicks", ticks);

            // Continuously freeze/lock the target entity if present
            int targetId = player.getPersistentData().getInt("RiderKickTargetId");
            if (targetId != 0) {
                net.minecraft.world.entity.Entity targetEntity = player.level().getEntity(targetId);
                if (targetEntity instanceof LivingEntity target && target.isAlive()) {
                    double tx = player.getPersistentData().getDouble("RiderKickTargetX");
                    double ty = player.getPersistentData().getDouble("RiderKickTargetY");
                    double tz = player.getPersistentData().getDouble("RiderKickTargetZ");
                    target.teleportTo(tx, ty, tz);
                    target.setDeltaMovement(Vec3.ZERO);
                    target.hurtMarked = true;
                    if (target instanceof net.minecraft.world.entity.Mob mob) {
                        mob.setNoAi(true);
                    }
                    target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 100, false, false));
                }
            }

            if (state == 1) { // Phase 1: High Leap
                // Apex detection: peak of vertical leap (y velocity <= 0.02) after ascending at least 2 ticks
                if ((ticks >= 2 && player.getDeltaMovement().y <= 0.02D) || ticks >= 25) { // Apex reached
                    // Stop momentum momentarily to show the cinematic charge pose!
                    player.setDeltaMovement(Vec3.ZERO);
                    player.hurtMarked = true;
                    PacketDistributor.sendToPlayer((ServerPlayer) player, new SyncVelocityPacket(0, 0, 0));

                    // Transition to Phase 2 (Dive kick charge)
                    player.getPersistentData().putInt("RiderKickState", 2);
                    player.getPersistentData().putInt("RiderKickTicks", 0);
                    syncRiderKickState(player, 2);

                    // Play apex charging sound
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                             net.minecraft.sounds.SoundEvents.FIRECHARGE_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1.5f, 1.2f);
                }
            } else if (state == 2) { // Phase 2: Dive Kick
                if (ticks == 4) { // Start downward dive after a tiny pause of 4 ticks
                    Vec3 lookVec = player.getLookAngle();
                    double diveY = lookVec.y;
                    if (diveY > -0.4D) diveY = -0.8D; // force diving down
                    Vec3 diveVec = new Vec3(lookVec.x, diveY, lookVec.z).normalize().scale(4.0D); // Upgraded to 4.0D!
                    player.setDeltaMovement(diveVec);
                    player.hurtMarked = true;
                    PacketDistributor.sendToPlayer((ServerPlayer) player, new SyncVelocityPacket(diveVec.x, diveVec.y, diveVec.z));

                    // Play swoosh wind sound
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            net.minecraft.sounds.SoundEvents.WIND_CHARGE_THROW, net.minecraft.sounds.SoundSource.PLAYERS, 1.5f, 0.6f);
                }

                if (ticks >= 4) {
                    // Check collision
                    AABB footBox = player.getBoundingBox().inflate(0.8D);
                    List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, footBox,
                            entity -> entity != player && entity.isAlive());

                    boolean hitGround = player.onGround();
                    boolean hitTarget = !targets.isEmpty();

                    if (hitGround || hitTarget) {
                        // Transition to Phase 3: Impact pause
                        player.getPersistentData().putInt("RiderKickState", 3);
                        player.getPersistentData().putInt("RiderKickTicks", 0);
                        player.setDeltaMovement(Vec3.ZERO);
                        player.hurtMarked = true;
                        PacketDistributor.sendToPlayer((ServerPlayer) player, new SyncVelocityPacket(0, 0, 0));
                        syncRiderKickState(player, 3);

                        // Store impact location
                        player.getPersistentData().putDouble("RiderKickImpactX", player.getX());
                        player.getPersistentData().putDouble("RiderKickImpactY", player.getY());
                        player.getPersistentData().putDouble("RiderKickImpactZ", player.getZ());

                        // Initial heavy blow (keep locked, do not knock back yet)
                        if (hitTarget) {
                            for (LivingEntity target : targets) {
                                target.hurt(player.damageSources().playerAttack(player), 12.0f); // 6 Hearts initial impact
                            }
                        }

                        // Play impact sounds
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                net.minecraft.sounds.SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, net.minecraft.sounds.SoundSource.PLAYERS, 2.0f, 0.5f);
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_IMPACT, net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 1.5f);
                    }
                }

                if (ticks > 100) { // Safety timeout
                    player.getPersistentData().putInt("RiderKickState", 0);
                    player.getPersistentData().putBoolean("RiderKickFallImmunity", false);
                    syncRiderKickState(player, 0);
                    releaseTarget(player);
                }
            } else if (state == 3) { // Phase 3: Delayed cinematic explosion
                // Freeze the player completely in 3D stasis at impact point
                double ix = player.getPersistentData().getDouble("RiderKickImpactX");
                double iy = player.getPersistentData().getDouble("RiderKickImpactY");
                double iz = player.getPersistentData().getDouble("RiderKickImpactZ");
                player.teleportTo(ix, iy, iz);
                player.setDeltaMovement(Vec3.ZERO);
                player.hurtMarked = true;
                PacketDistributor.sendToPlayer((ServerPlayer) player, new SyncVelocityPacket(0, 0, 0));

                if (ticks >= 12) { // The big bang!
                    double x = player.getPersistentData().getDouble("RiderKickImpactX");
                    double y = player.getPersistentData().getDouble("RiderKickImpactY");
                    double z = player.getPersistentData().getDouble("RiderKickImpactZ");

                    // AOE explosive damage
                    AABB blastArea = new AABB(x - 4.5D, y - 2.0D, z - 4.5D, x + 4.5D, y + 3.0D, z + 4.5D);
                    List<LivingEntity> victims = player.level().getEntitiesOfClass(LivingEntity.class, blastArea,
                            entity -> entity != player && entity.isAlive());

                    for (LivingEntity victim : victims) {
                        victim.hurt(player.damageSources().playerAttack(player), 28.0f); // Massive finishing burst damage (14 Hearts!)
                        Vec3 kbVec = victim.position().subtract(new Vec3(x, y, z)).normalize().scale(2.2D);
                        victim.push(kbVec.x, 0.8D, kbVec.z);
                        victim.setRemainingFireTicks(100);
                    }

                    // Trigger client screenshake & electric explosion
                    PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                            new TransformVFXPacket(x, y, z, true));

                    // Delayed explosion sounds
                    player.level().playSound(null, x, y, z, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.PLAYERS, 2.5f, 0.55f);
                    player.level().playSound(null, x, y, z, net.minecraft.sounds.SoundEvents.DRAGON_FIREBALL_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 2.0f, 0.8f);

                    // Ability completed
                    player.getPersistentData().putInt("RiderKickState", 0);
                    player.getPersistentData().putBoolean("RiderKickFallImmunity", false);
                    syncRiderKickState(player, 0);
                    releaseTarget(player);

                    // Epic slide off finish
                    Vec3 slide = player.getLookAngle().scale(1.2D);
                    player.setDeltaMovement(new Vec3(slide.x, 0.1D, slide.z));
                    player.hurtMarked = true;
                    PacketDistributor.sendToPlayer((ServerPlayer) player, new SyncVelocityPacket(slide.x, 0.1D, slide.z));
                }
            }
        }

        if (player.tickCount % 10 == 0) { // Sync twice a second
            boolean isTransformed = TransformationHelper.isTransformed(player);
            boolean hasBelt = TransformationHelper.isBeltEquipped(player);
            int riderKickState = player.getPersistentData().getInt("RiderKickState");
            int form = player.getPersistentData().getInt("krm_revo:form");

            PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                    player,
                    new SyncRiderStatePacket(player.getId(), isTransformed, hasBelt, riderKickState, form)
            );
        }
    }

    public static void releaseTarget(Player player) {
        int targetId = player.getPersistentData().getInt("RiderKickTargetId");
        if (targetId != 0) {
            net.minecraft.world.entity.Entity targetEntity = player.level().getEntity(targetId);
            if (targetEntity instanceof LivingEntity target) {
                target.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
                if (target instanceof net.minecraft.world.entity.Mob mob) {
                    mob.setNoAi(false);
                }
            }
            player.getPersistentData().putInt("RiderKickTargetId", 0);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                    new com.neroferno.krm_revo.network.SyncRiderKickTargetPacket(player.getId(), 0, 0, 0, 0));
        }
    }

    private static void syncRiderKickState(Player player, int state) {
        boolean isTransformed = TransformationHelper.isTransformed(player);
        boolean hasBelt = TransformationHelper.isBeltEquipped(player);
        int form = player.getPersistentData().getInt("krm_revo:form");
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new SyncRiderStatePacket(player.getId(), isTransformed, hasBelt, state, form)
        );
    }
}

