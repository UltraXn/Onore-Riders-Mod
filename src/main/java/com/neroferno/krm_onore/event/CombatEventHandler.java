package com.neroferno.krm_onore.event;

import com.neroferno.krm_onore.KRMRevoMod;
import com.neroferno.krm_onore.network.ImpactVFXPacket;
import com.neroferno.krm_onore.network.TransformationHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import com.neroferno.krm_onore.network.SyncRiderStatePacket;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.minecraft.server.level.ServerPlayer;
import com.neroferno.krm_onore.network.SyncVelocityPacket;
import com.neroferno.krm_onore.network.TransformVFXPacket;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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

            if (state == 1) { // Phase 1: High Leap & Tokusatsu Apex Hover
                // Apex detection: peak of vertical leap (y velocity <= 0.08D) or ticks >= 18
                int hoverTicks = player.getPersistentData().getInt("RiderKickHoverTicks");

                if (hoverTicks > 0 || (ticks >= 2 && player.getDeltaMovement().y <= 0.08D) || ticks >= 20) {
                    hoverTicks++;
                    player.getPersistentData().putInt("RiderKickHoverTicks", hoverTicks);

                    // Low gravity hover: gentle float at the apex while charging the kick pose
                    Vec3 currentVel = player.getDeltaMovement();
                    Vec3 hoverVel = new Vec3(currentVel.x * 0.6D, 0.02D, currentVel.z * 0.6D);
                    player.setDeltaMovement(hoverVel);
                    player.hurtMarked = true;
                    PacketDistributor.sendToPlayer((ServerPlayer) player, new SyncVelocityPacket(hoverVel.x, hoverVel.y, hoverVel.z));

                    if (hoverTicks == 1) {
                        // Play apex charging sound on hover start
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                net.minecraft.sounds.SoundEvents.FIRECHARGE_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1.5f, 1.2f);
                    }

                    // Hover lasts 7 ticks (approx ~0.35s slow-mo peak)
                    if (hoverTicks >= 7) {
                        // Store Apex starting point for the Bézier curve
                        player.getPersistentData().putDouble("RiderKickApexX", player.getX());
                        player.getPersistentData().putDouble("RiderKickApexY", player.getY());
                        player.getPersistentData().putDouble("RiderKickApexZ", player.getZ());

                        // Transition to Phase 2 (Curved Bézier Dive Kick)
                        player.getPersistentData().putInt("RiderKickState", 2);
                        player.getPersistentData().putInt("RiderKickTicks", 0);
                        player.getPersistentData().putInt("RiderKickHoverTicks", 0);
                        syncRiderKickState(player, 2);

                        // Play swoosh launch wind sound
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                net.minecraft.sounds.SoundEvents.WIND_CHARGE_THROW, net.minecraft.sounds.SoundSource.PLAYERS, 2.0f, 0.55f);
                    }
                }
            } else if (state == 2) { // Phase 2: Curved Bézier Dive Kick
                // P0: Apex launch position
                Vec3 p0 = new Vec3(
                        player.getPersistentData().getDouble("RiderKickApexX"),
                        player.getPersistentData().getDouble("RiderKickApexY"),
                        player.getPersistentData().getDouble("RiderKickApexZ")
                );

                // Target position P2 (updated from target if alive)
                double tx = player.getPersistentData().getDouble("RiderKickTargetX");
                double ty = player.getPersistentData().getDouble("RiderKickTargetY");
                double tz = player.getPersistentData().getDouble("RiderKickTargetZ");

                targetId = player.getPersistentData().getInt("RiderKickTargetId");
                if (targetId != 0) {
                    net.minecraft.world.entity.Entity targetEntity = player.level().getEntity(targetId);
                    if (targetEntity instanceof LivingEntity target && target.isAlive()) {
                        tx = target.getX();
                        ty = target.getY() + target.getBbHeight() * 0.55D; // Target center of mass / chest
                        tz = target.getZ();
                        player.getPersistentData().putDouble("RiderKickTargetX", tx);
                        player.getPersistentData().putDouble("RiderKickTargetY", ty);
                        player.getPersistentData().putDouble("RiderKickTargetZ", tz);
                    }
                }
                Vec3 p2 = new Vec3(tx, ty, tz);

                // Control Point P1: Elevated midpoint generating the downward Tokusatsu swooping comba
                Vec3 mid = p0.add(p2).scale(0.5D);
                Vec3 p1 = new Vec3(mid.x, Math.max(p0.y, p2.y) + 1.2D, mid.z);

                double totalDist = p0.distanceTo(p2);
                int totalDiveTicks = (int) Math.max(6, Math.min(14, totalDist * 0.7D));

                double u = Math.min(1.0D, (double) ticks / totalDiveTicks);
                double uNext = Math.min(1.0D, (double) (ticks + 1) / totalDiveTicks);

                // Ease-In acceleration (starts smooth, accelerates to explosive speed)
                double tNext = uNext * uNext;

                // Quadratic Bézier target position for next step: B(t) = (1-t)^2 P0 + 2(1-t)t P1 + t^2 P2
                double oneMinusTNext = 1.0D - tNext;
                Vec3 nextTargetPos = p0.scale(oneMinusTNext * oneMinusTNext)
                        .add(p1.scale(2.0D * oneMinusTNext * tNext))
                        .add(p2.scale(tNext * tNext));

                Vec3 desiredVel = nextTargetPos.subtract(player.position());
                if (desiredVel.length() > 3.8D) {
                    desiredVel = desiredVel.normalize().scale(3.8D);
                }

                player.setDeltaMovement(desiredVel);
                player.hurtMarked = true;
                PacketDistributor.sendToPlayer((ServerPlayer) player, new SyncVelocityPacket(desiredVel.x, desiredVel.y, desiredVel.z));

                // Continuous Collision Detection (Raycast between current position and next position)
                Vec3 currentPos = player.position();
                Vec3 nextPos = currentPos.add(desiredVel);

                // 1. Entity sweep detection
                AABB sweepBox = player.getBoundingBox().minmax(player.getBoundingBox().move(desiredVel)).inflate(0.5D);
                List<LivingEntity> hitEntities = player.level().getEntitiesOfClass(LivingEntity.class, sweepBox,
                        entity -> entity != player && entity.isAlive());

                // 2. Block clip detection
                BlockHitResult blockHit = player.level().clip(new ClipContext(currentPos, nextPos,
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

                boolean hitTarget = !hitEntities.isEmpty();
                boolean hitBlock = blockHit.getType() != HitResult.Type.MISS;
                boolean reachedEnd = u >= 0.95D || player.onGround();

                if (hitTarget || hitBlock || reachedEnd) {
                    // Transition to Phase 3: Hit-Stop
                    player.getPersistentData().putInt("RiderKickState", 3);
                    player.getPersistentData().putInt("RiderKickTicks", 0);
                    player.setDeltaMovement(Vec3.ZERO);
                    player.hurtMarked = true;
                    PacketDistributor.sendToPlayer((ServerPlayer) player, new SyncVelocityPacket(0, 0, 0));
                    syncRiderKickState(player, 3);

                    Vec3 impactPos = hitTarget ? hitEntities.get(0).position().add(0, hitEntities.get(0).getBbHeight() * 0.5D, 0) :
                                     hitBlock ? blockHit.getLocation() : player.position();

                    player.getPersistentData().putDouble("RiderKickImpactX", impactPos.x);
                    player.getPersistentData().putDouble("RiderKickImpactY", impactPos.y);
                    player.getPersistentData().putDouble("RiderKickImpactZ", impactPos.z);

                    // Initial strike impact damage (locks victim in place during hit-stop)
                    if (hitTarget) {
                        for (LivingEntity victim : hitEntities) {
                            victim.hurt(player.damageSources().playerAttack(player), 14.0f); // 7 Hearts initial impact
                        }
                    }

                    // Initial heavy contact impact sounds
                    player.level().playSound(null, impactPos.x, impactPos.y, impactPos.z,
                            net.minecraft.sounds.SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, net.minecraft.sounds.SoundSource.PLAYERS, 2.0f, 0.5f);
                    player.level().playSound(null, impactPos.x, impactPos.y, impactPos.z,
                            net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_IMPACT, net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 1.6f);
                }

                if (ticks > 80) { // Safety timeout
                    player.getPersistentData().putInt("RiderKickState", 0);
                    player.getPersistentData().putBoolean("RiderKickFallImmunity", false);
                    syncRiderKickState(player, 0);
                    releaseTarget(player);
                }
            } else if (state == 3) { // Phase 3: Hit-Stop (5 ticks) & Delayed Explosion Slide-Off
                double ix = player.getPersistentData().getDouble("RiderKickImpactX");
                double iy = player.getPersistentData().getDouble("RiderKickImpactY");
                double iz = player.getPersistentData().getDouble("RiderKickImpactZ");

                // Freeze in place during Hit-Stop (5 ticks)
                player.teleportTo(ix, iy, iz);
                player.setDeltaMovement(Vec3.ZERO);
                player.hurtMarked = true;
                PacketDistributor.sendToPlayer((ServerPlayer) player, new SyncVelocityPacket(0, 0, 0));

                if (ticks % 2 == 0) {
                    player.level().playSound(null, ix, iy, iz,
                            net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.4f);
                }

                if (ticks >= 5) { // The big bang after 5 ticks of Hit-Stop!
                    // AOE explosive damage
                    AABB blastArea = new AABB(ix - 4.5D, iy - 2.0D, iz - 4.5D, ix + 4.5D, iy + 3.0D, iz + 4.5D);
                    List<LivingEntity> victims = player.level().getEntitiesOfClass(LivingEntity.class, blastArea,
                            entity -> entity != player && entity.isAlive());

                    for (LivingEntity victim : victims) {
                        victim.hurt(player.damageSources().playerAttack(player), 28.0f); // Massive finishing burst damage (14 Hearts!)
                        Vec3 kbVec = victim.position().subtract(new Vec3(ix, iy, iz)).normalize().scale(2.2D);
                        victim.push(kbVec.x, 0.8D, kbVec.z);
                        victim.setRemainingFireTicks(100);
                    }

                    // Trigger client screenshake & electric explosion
                    PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                            new TransformVFXPacket(ix, iy, iz, true));

                    // Delayed explosion sounds
                    player.level().playSound(null, ix, iy, iz, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.PLAYERS, 2.5f, 0.55f);
                    player.level().playSound(null, ix, iy, iz, net.minecraft.sounds.SoundEvents.DRAGON_FIREBALL_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 2.0f, 0.8f);

                    // Ability completed
                    player.getPersistentData().putInt("RiderKickState", 0);
                    player.getPersistentData().putInt("RiderKickHoverTicks", 0);
                    player.getPersistentData().putBoolean("RiderKickFallImmunity", false);
                    syncRiderKickState(player, 0);
                    releaseTarget(player);

                    // Epic slide-off finish!
                    Vec3 look = player.getLookAngle();
                    Vec3 slide = new Vec3(look.x, 0.05D, look.z).normalize().scale(1.25D);
                    player.setDeltaMovement(slide);
                    player.hurtMarked = true;
                    PacketDistributor.sendToPlayer((ServerPlayer) player, new SyncVelocityPacket(slide.x, slide.y, slide.z));
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
            player.getPersistentData().putInt("RiderKickHoverTicks", 0);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                    new com.neroferno.krm_onore.network.SyncRiderKickTargetPacket(player.getId(), 0, 0, 0, 0));
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

