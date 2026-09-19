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
import com.neroferno.krm_onore.network.PlayerAnimationPacket;
import com.neroferno.krm_onore.attachment.ModAttachments;
import com.neroferno.krm_onore.attachment.RiderEnergyData;
import com.neroferno.krm_onore.network.SyncRiderEnergyPacket;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

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

        // Transformed unarmed melee strike: gain +5.0f Rider Energy
        RiderEnergyData attackerEnergy = attacker.getData(ModAttachments.RIDER_ENERGY);
        attackerEnergy.gain(5.0f);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                attacker,
                new SyncRiderEnergyPacket(attacker.getId(), attackerEnergy.getEnergy(), attackerEnergy.getMaxEnergy())
        );

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

            // Continuously immobilize the target entity in place without lifting it
            int targetId = player.getPersistentData().getInt("RiderKickTargetId");
            if (targetId != 0) {
                net.minecraft.world.entity.Entity targetEntity = player.level().getEntity(targetId);
                if (targetEntity instanceof LivingEntity target && target.isAlive()) {
                    target.setDeltaMovement(0, Math.min(0, target.getDeltaMovement().y), 0);
                    target.hurtMarked = true;
                    if (target instanceof net.minecraft.world.entity.Mob mob) {
                        mob.setNoAi(true);
                    }
                    target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 100, false, false));
                }
            }

            if (state == 1) { // Phase 1: High Leap & Apex Kick Pose
                // Apex detection: peak of vertical leap (y velocity <= 0.08D) or ticks >= 14
                int hoverTicks = player.getPersistentData().getInt("RiderKickHoverTicks");

                if (hoverTicks > 0 || (ticks >= 3 && player.getDeltaMovement().y <= 0.08D) || ticks >= 16) {
                    hoverTicks++;
                    player.getPersistentData().putInt("RiderKickHoverTicks", hoverTicks);

                    // Low gravity apex pause: gentle float at the apex
                    Vec3 currentVel = player.getDeltaMovement();
                    Vec3 hoverVel = new Vec3(currentVel.x * 0.4D, 0.02D, currentVel.z * 0.4D);
                    player.setDeltaMovement(hoverVel);
                    player.hurtMarked = true;
                    PacketDistributor.sendToPlayer((ServerPlayer) player, new SyncVelocityPacket(hoverVel.x, hoverVel.y, hoverVel.z));

                    if (hoverTicks == 1) {
                        // Play apex charging sound on hover start
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                net.minecraft.sounds.SoundEvents.FIRECHARGE_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1.5f, 1.2f);

                        // Trigger the Rider Kick pose animation right at the apex
                        if (!player.getPersistentData().getBoolean("RiderKickAnimPlayed")) {
                            player.getPersistentData().putBoolean("RiderKickAnimPlayed", true);
                            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                                    new PlayerAnimationPacket(player.getId(), "rider_kick"));
                        }
                    }

                    // Hover lasts 3 ticks (sharp ~0.15s dynamic apex freeze frame)
                    if (hoverTicks >= 3) {
                        // Store Apex starting point
                        player.getPersistentData().putDouble("RiderKickApexX", player.getX());
                        player.getPersistentData().putDouble("RiderKickApexY", player.getY());
                        player.getPersistentData().putDouble("RiderKickApexZ", player.getZ());

                        // Transition to Phase 2 (Angled Dive Kick straight towards mob)
                        player.getPersistentData().putInt("RiderKickState", 2);
                        player.getPersistentData().putInt("RiderKickTicks", 0);
                        player.getPersistentData().putInt("RiderKickHoverTicks", 0);
                        syncRiderKickState(player, 2);

                        // Ensure animation is playing
                        if (!player.getPersistentData().getBoolean("RiderKickAnimPlayed")) {
                            player.getPersistentData().putBoolean("RiderKickAnimPlayed", true);
                            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                                    new PlayerAnimationPacket(player.getId(), "rider_kick"));
                        }

                        // Play swoosh launch wind sound
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                net.minecraft.sounds.SoundEvents.WIND_CHARGE_THROW, net.minecraft.sounds.SoundSource.PLAYERS, 2.0f, 0.55f);
                    }
                }
            } else if (state == 2) { // Phase 2: Direct Angled Dive Kick straight at Mob
                // Ensure animation played
                if (!player.getPersistentData().getBoolean("RiderKickAnimPlayed")) {
                    player.getPersistentData().putBoolean("RiderKickAnimPlayed", true);
                    PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                            new PlayerAnimationPacket(player.getId(), "rider_kick"));
                }

                // Target position (updated from target if alive)
                double tx = player.getPersistentData().getDouble("RiderKickTargetX");
                double ty = player.getPersistentData().getDouble("RiderKickTargetY");
                double tz = player.getPersistentData().getDouble("RiderKickTargetZ");
                double chestOffsetY = 0.8D;

                targetId = player.getPersistentData().getInt("RiderKickTargetId");
                LivingEntity targetEntityRef = null;
                if (targetId != 0) {
                    net.minecraft.world.entity.Entity targetEntity = player.level().getEntity(targetId);
                    if (targetEntity instanceof LivingEntity target && target.isAlive()) {
                        targetEntityRef = target;
                        tx = target.getX();
                        ty = target.getY(); // Actual ground position of target
                        tz = target.getZ();
                        chestOffsetY = target.getBbHeight() * 0.55D;
                        player.getPersistentData().putDouble("RiderKickTargetX", tx);
                        player.getPersistentData().putDouble("RiderKickTargetY", ty);
                        player.getPersistentData().putDouble("RiderKickTargetZ", tz);
                    }
                }

                // Target chest center: aim straight at enemy center of mass
                Vec3 targetChest = new Vec3(tx, ty + chestOffsetY, tz);
                Vec3 toTarget = targetChest.subtract(player.position());
                double distToChest = toTarget.length();

                // Direct angled dive vector (sharp diagonal straight to the mob)
                Vec3 diveDir = distToChest > 0.001D ? toTarget.normalize() : player.getLookAngle();
                double diveSpeed = 2.4D; // Fast, direct Tokusatsu dive velocity

                Vec3 desiredVel = diveDir.scale(diveSpeed);
                player.setDeltaMovement(desiredVel);
                player.hurtMarked = true;
                PacketDistributor.sendToPlayer((ServerPlayer) player, new SyncVelocityPacket(desiredVel.x, desiredVel.y, desiredVel.z));

                // Collision & Impact Detection ("PAM!")
                // 1. Proximity to target chest
                boolean closeToTarget = (targetEntityRef != null && distToChest <= 1.8D) || (distToChest <= 1.5D);

                // 2. Entity sweep detection
                AABB sweepBox = player.getBoundingBox().minmax(player.getBoundingBox().move(desiredVel)).inflate(0.6D);
                List<LivingEntity> hitEntities = player.level().getEntitiesOfClass(LivingEntity.class, sweepBox,
                        entity -> entity != player && entity.isAlive());
                boolean hitEntity = !hitEntities.isEmpty();

                // 3. Block clip detection
                Vec3 currentPos = player.position();
                Vec3 nextPos = currentPos.add(desiredVel);
                BlockHitResult blockHit = player.level().clip(new ClipContext(currentPos, nextPos,
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                boolean hitBlock = blockHit.getType() != HitResult.Type.MISS;

                // 4. Ground contact after descent
                boolean hitGround = player.onGround() && ticks >= 2;

                if (closeToTarget || hitEntity || hitBlock || hitGround) {
                    // Transition to Phase 3: Hit-Stop & PAM! Explosion
                    player.getPersistentData().putInt("RiderKickState", 3);
                    player.getPersistentData().putInt("RiderKickTicks", 0);
                    player.setDeltaMovement(Vec3.ZERO);
                    player.hurtMarked = true;
                    PacketDistributor.sendToPlayer((ServerPlayer) player, new SyncVelocityPacket(0, 0, 0));
                    syncRiderKickState(player, 3);

                    Vec3 impactPos;
                    if (targetEntityRef != null) {
                        impactPos = targetEntityRef.position().add(0, targetEntityRef.getBbHeight() * 0.5D, 0);
                    } else if (hitEntity) {
                        impactPos = hitEntities.get(0).position().add(0, hitEntities.get(0).getBbHeight() * 0.5D, 0);
                    } else if (hitBlock) {
                        impactPos = blockHit.getLocation();
                    } else {
                        impactPos = player.position();
                    }

                    player.getPersistentData().putDouble("RiderKickImpactX", impactPos.x);
                    player.getPersistentData().putDouble("RiderKickImpactY", impactPos.y);
                    player.getPersistentData().putDouble("RiderKickImpactZ", impactPos.z);

                    // Initial strike impact damage
                    if (targetEntityRef != null) {
                        targetEntityRef.hurt(player.damageSources().playerAttack(player), 14.0f);
                    }
                    for (LivingEntity victim : hitEntities) {
                        if (victim != targetEntityRef) {
                            victim.hurt(player.damageSources().playerAttack(player), 14.0f);
                        }
                    }

                    // Initial heavy contact impact sounds
                    player.level().playSound(null, impactPos.x, impactPos.y, impactPos.z,
                            net.minecraft.sounds.SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, net.minecraft.sounds.SoundSource.PLAYERS, 2.0f, 0.5f);
                    player.level().playSound(null, impactPos.x, impactPos.y, impactPos.z,
                            net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_IMPACT, net.minecraft.sounds.SoundSource.PLAYERS, 1.5f, 1.6f);
                    player.level().playSound(null, impactPos.x, impactPos.y, impactPos.z,
                            net.minecraft.sounds.SoundEvents.SHIELD_BLOCK, net.minecraft.sounds.SoundSource.PLAYERS, 1.5f, 0.6f);
                }

                if (ticks > 40) { // Safety timeout (2 seconds max dive)
                    player.getPersistentData().putInt("RiderKickState", 0);
                    player.getPersistentData().putBoolean("RiderKickFallImmunity", false);
                    syncRiderKickState(player, 0);
                    releaseTarget(player);
                }
            } else if (state == 3) { // Phase 3: Hit-Stop (4 ticks) & PAM! Finishing Explosion
                double ix = player.getPersistentData().getDouble("RiderKickImpactX");
                double iy = player.getPersistentData().getDouble("RiderKickImpactY");
                double iz = player.getPersistentData().getDouble("RiderKickImpactZ");

                // Freeze in place during Hit-Stop (4 ticks)
                player.teleportTo(ix, iy, iz);
                player.setDeltaMovement(Vec3.ZERO);
                player.hurtMarked = true;
                PacketDistributor.sendToPlayer((ServerPlayer) player, new SyncVelocityPacket(0, 0, 0));

                if (ticks % 2 == 0) {
                    player.level().playSound(null, ix, iy, iz,
                            net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.4f);
                }

                if (ticks >= 4) { // The big "PAM!" finishing explosion
                    // AOE explosive damage (15 hearts!)
                    AABB blastArea = new AABB(ix - 5.0D, iy - 2.0D, iz - 5.0D, ix + 5.0D, iy + 3.0D, iz + 5.0D);
                    List<LivingEntity> victims = player.level().getEntitiesOfClass(LivingEntity.class, blastArea,
                            entity -> entity != player && entity.isAlive());

                    for (LivingEntity victim : victims) {
                        victim.hurt(player.damageSources().playerAttack(player), 30.0f); // 15 Hearts finishing damage!
                        Vec3 kbVec = victim.position().subtract(new Vec3(ix, iy, iz)).normalize().scale(2.4D);
                        victim.push(kbVec.x, 0.8D, kbVec.z);
                        victim.setRemainingFireTicks(100);
                    }

                    // Trigger client screenshake & electric explosion
                    PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                            new TransformVFXPacket(ix, iy, iz, true));

                    // Massive explosion audio
                    player.level().playSound(null, ix, iy, iz, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.PLAYERS, 3.0f, 0.55f);
                    player.level().playSound(null, ix, iy, iz, net.minecraft.sounds.SoundEvents.DRAGON_FIREBALL_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 2.5f, 0.8f);
                    player.level().playSound(null, ix, iy, iz, net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.PLAYERS, 2.0f, 1.2f);

                    // Ability completed
                    player.getPersistentData().putInt("RiderKickState", 0);
                    player.getPersistentData().putInt("RiderKickHoverTicks", 0);
                    player.getPersistentData().putBoolean("RiderKickFallImmunity", false);
                    player.getPersistentData().putBoolean("RiderKickAnimPlayed", false);
                    player.resetFallDistance();
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

        // Rider Energy management (every second / 20 ticks)
        if (player.tickCount % 20 == 0) {
            boolean isTransformed = TransformationHelper.isTransformed(player);
            RiderEnergyData energy = player.getData(ModAttachments.RIDER_ENERGY);

            if (isTransformed) {
                // If transformed and energy <= 0, automatically detransform!
                if (energy.getEnergy() <= 0.0f) {
                    TransformationHelper.setTransformed(player, false);
                    player.displayClientMessage(
                            Component.translatable("krm_revo.transform.exhausted")
                                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                            true
                    );
                    boolean hasBelt = TransformationHelper.isBeltEquipped(player);
                    int form = player.getPersistentData().getInt("krm_revo:form");

                    PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                            new SyncRiderStatePacket(player.getId(), false, hasBelt, 0, form));
                    PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                            new TransformVFXPacket(player.getX(), player.getY(), player.getZ(), false));
                }
            } else {
                // Untransformed passive regeneration (+1.0f per second up to max)
                if (energy.getEnergy() < energy.getMaxEnergy()) {
                    energy.gain(1.0f);
                    PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                            new SyncRiderEnergyPacket(player.getId(), energy.getEnergy(), energy.getMaxEnergy()));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        RiderEnergyData energy = player.getData(ModAttachments.RIDER_ENERGY);
        energy.fill();
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new SyncRiderEnergyPacket(player.getId(), energy.getEnergy(), energy.getMaxEnergy())
        );
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        RiderEnergyData energy = player.getData(ModAttachments.RIDER_ENERGY);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new SyncRiderEnergyPacket(player.getId(), energy.getEnergy(), energy.getMaxEnergy())
        );
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        RiderEnergyData energy = player.getData(ModAttachments.RIDER_ENERGY);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new SyncRiderEnergyPacket(player.getId(), energy.getEnergy(), energy.getMaxEnergy())
        );
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
            player.getPersistentData().putBoolean("RiderKickAnimPlayed", false);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                    new com.neroferno.krm_onore.network.SyncRiderKickTargetPacket(player.getId(), 0, 0, 0, 0));
        } else {
            player.getPersistentData().putBoolean("RiderKickAnimPlayed", false);
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

