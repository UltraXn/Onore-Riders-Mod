package com.neroferno.krm_onore.combat;

import com.neroferno.krm_onore.attachment.ModAttachments;
import com.neroferno.krm_onore.KRMRevoMod;
import com.neroferno.krm_onore.network.GrowingPunchAnimationPacket;
import com.neroferno.krm_onore.network.TransformationHelper;
import com.neroferno.krm_onore.network.ImpactVFXPacket;
import com.neroferno.krm_onore.network.HitStopPacket;
import com.neroferno.krm_onore.network.RiderFistConfigSyncPacket;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.PlayerAttackHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = KRMRevoMod.MODID)
public final class RiderFistCombat {
    private static final double ATTACK_RANGE = 3.5D;
    /** Cono horizontal usado para orientar el avance del golpe sin mover la camara del jugador. */
    private static final float AIM_ASSIST_HALF_ANGLE = 55.0F;
    /** Maximo que el avance del golpe puede desviarse de la mirada hacia el enemigo (grados). */
    private static final float LUNGE_STEER_MAX = 45.0F;
    private static final long STUN_PUSH_DELAY_TICKS = 3L;
    private static final Map<ServerPlayer, AttackState> ATTACK_STATES = new WeakHashMap<>();
    private static final Map<ServerPlayer, Long> NEXT_ATTACK_TIME = new WeakHashMap<>();
    private static final List<PendingAttack> PENDING_ATTACKS = new ArrayList<>();
    private static final List<PendingLunge> PENDING_LUNGES = new ArrayList<>();
    private static final Set<UUID> RESOLVING_CUSTOM_IMPACTS = new HashSet<>();
    private static final Map<UUID, Float> RESOLVING_EXTRA_KNOCKBACK = new HashMap<>();
    private static final Map<UUID, Double> RESOLVING_DAMAGE = new HashMap<>();
    private static final Map<net.minecraft.world.entity.Mob, Long> STUNNED_MOBS = new WeakHashMap<>();
    private static final Map<net.minecraft.world.entity.Mob, PendingStun> PENDING_STUNS = new WeakHashMap<>();
    private static final ResourceLocation FIST_PUNCH_SOUND =
            ResourceLocation.fromNamespaceAndPath("bettercombat", "fist_punch");
    private static final WeaponAttributes.Sound DEFAULT_IMPACT_SOUND =
            new WeaponAttributes.Sound("minecraft:entity.generic.hurt");
    private static final Map<ServerPlayer, BufferedAttack> BUFFERED_ATTACKS = new WeakHashMap<>();

    private RiderFistCombat() {}

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        RiderFistTiming.registerReloadListener(event);
    }

    public static void onDatapackSync(OnDatapackSyncEvent event) {
        RiderFistConfigSyncPacket packet = new RiderFistConfigSyncPacket(RiderFistTiming.serializeForSync());
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, packet));
    }

    public static void handleRightClick(ServerPlayer player) {
        requestAttack(player, false);
    }

    public static void handleLeftClickStart(ServerPlayer player) {
        requestAttack(player, true);
    }

    /**
     * Si el cooldown ya termino, ataca. Si termina en pocos ticks, guarda el clic (buffer) y ataca en cuanto
     * se pueda: asi mantener el clic encadena golpes sin huecos, y al soltar deja de encadenar y el traje
     * termina la animacion hasta la pose base.
     */
    private static void requestAttack(ServerPlayer player, boolean leftClick) {
        if (!player.isAlive() || !isEligibleGrowingPlayer(player)) {
            return;
        }

        if (Especial3Combat.isActive(player) || Especial2Combat.isActive(player) || Especial1Combat.isActive(player)) {
            // Seguro: mientras el Especial 3 esta corriendo, el clic (izquierdo o derecho) no debe generar
            // combo ni quedar guardado en buffer para dispararse apenas termine la habilidad.
            BUFFERED_ATTACKS.remove(player);
            return;
        }

        if (BackwardBlockCombat.isBlocking(player)) {
            // Bloquear y pegar no se puede a la vez: mientras dura el bloqueo el clic no genera golpe ni queda
            // guardado en buffer para dispararse al soltar el bloqueo.
            BUFFERED_ATTACKS.remove(player);
            return;
        }

        long gameTime = serverGameTime(player);
        long readyAt = NEXT_ATTACK_TIME.getOrDefault(player, 0L);
        if (gameTime >= readyAt) {
            BUFFERED_ATTACKS.remove(player);
            startAttack(player, leftClick, gameTime);
        } else {
            // Se guarda el ultimo clic (izquierdo o derecho) durante TODO el cooldown y se ejecuta en cuanto termine,
            // asi se puede alternar L/H sin que el clic se pierda por llegar "demasiado pronto".
            BUFFERED_ATTACKS.put(player, new BufferedAttack(leftClick, readyAt + 1));
        }
    }

    private static void startAttack(ServerPlayer player, boolean leftClick, long gameTime) {
        AttackState state = ATTACK_STATES.computeIfAbsent(player, ignored -> new AttackState());
        // Si paso el tiempo configurado desde que termino el cooldown, el combo vuelve al primer golpe.
        if (gameTime - NEXT_ATTACK_TIME.getOrDefault(player, 0L) > RiderFistTiming.comboResetTicks()) {
            state.nextAttackIndex = 0;
        }
        int attackIndex = state.nextAttackIndex;
        advanceAttack(state);
        NEXT_ATTACK_TIME.put(player, gameTime + RiderFistTiming.animationTicks(!leftClick, attackIndex));

        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new GrowingPunchAnimationPacket(player.getId(), attackIndex, !leftClick)
        );

        queueLunge(player, attackIndex, gameTime, leftClick);
        PENDING_ATTACKS.add(new PendingAttack(
                player.getUUID(),
                player.level().dimension(),
                gameTime,
                attackIndex,
                !leftClick,
                player.getLookAngle()
        ));
    }

    /**
     * El sonido propio se reproduce en el cliente al recibir la animacion del golpe.
     * Se cancelan los sonidos automaticos de vanilla y Better Combat para evitar duplicados.
     */
    @SubscribeEvent
    public static void onAtPositionSound(PlayLevelSoundEvent.AtPosition event) {
        if (event.getSound() == null || event.getSource() != SoundSource.PLAYERS
                || !isStrayPunchSound(event.getSound().value().getLocation())) {
            return;
        }
        Vec3 pos = event.getPosition();
        if (event.getLevel().getNearestPlayer(pos.x, pos.y, pos.z, 3.0D,
                entity -> entity instanceof Player p && isGrowingFistPlayer(p)) != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAtEntitySound(PlayLevelSoundEvent.AtEntity event) {
        if (event.getSound() != null
                && event.getEntity() instanceof Player player
                && isGrowingFistPlayer(player)
                && isStrayPunchSound(event.getSound().value().getLocation())) {
            event.setCanceled(true);
        }
    }

    private static boolean isStrayPunchSound(ResourceLocation id) {
        if ("minecraft".equals(id.getNamespace()) && id.getPath().startsWith("entity.player.attack.")) {
            return true;
        }
        return id.equals(FIST_PUNCH_SOUND);
    }

    /** Las lineas blancas solo aparecen cuando el golpe conecta. */
    private static void playImpactFeedback(ServerPlayer player, int attackIndex, Vec3 pos, Vec3 look) {
        playImpactSound(player, impactSoundFor(player, attackIndex), pos);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new ImpactVFXPacket(pos.x, pos.y, pos.z, look.x, look.y, look.z)
        );
    }

    private static void playImpactSound(ServerPlayer player, WeaponAttributes.Sound impactSound, Vec3 pos) {
        ResourceLocation soundId = ResourceLocation.tryParse(impactSound.id());
        if (soundId == null) {
            KRMRevoMod.LOGGER.error("Invalid Growing impact sound ID: {}", impactSound.id());
            return;
        }

        BuiltInRegistries.SOUND_EVENT.getOptional(soundId).ifPresentOrElse(
                sound -> player.level().playSound(
                        null,
                        pos.x,
                        pos.y,
                        pos.z,
                        sound,
                        SoundSource.PLAYERS,
                        impactSound.volume(),
                        impactSound.pitch()
                ),
                () -> KRMRevoMod.LOGGER.error("Growing impact sound {} is not registered.", soundId)
        );
    }

    private static WeaponAttributes.Sound impactSoundFor(ServerPlayer player, int attackIndex) {
        AttackHand attackHand = PlayerAttackHelper.getCurrentAttack(player, attackIndex * 2);
        if (attackHand == null) {
            KRMRevoMod.LOGGER.warn("Could not resolve Better Combat attack {} for Growing impact sound; using fallback.",
                    attackIndex);
            return DEFAULT_IMPACT_SOUND;
        }

        WeaponAttributes.Sound impactSound = attackHand.attack().impactSound();
        if (impactSound == null) {
            KRMRevoMod.LOGGER.warn("Better Combat attack {} has no impact_sound; using fallback.",
                    attackIndex);
            return DEFAULT_IMPACT_SOUND;
        }

        return impactSound;
    }

    private static void advanceAttack(AttackState state) {
        state.nextAttackIndex = (state.nextAttackIndex + 1) % 3;
    }

    private static void queueLunge(ServerPlayer player, int attackIndex, long gameTime, boolean leftClick) {
        if (hasPendingLunge(player, gameTime)) {
            return;
        }

        Vec3 look = player.getLookAngle();
        Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);
        if (horizontalLook.lengthSqr() > 1.0E-6D) {
            RiderFistTiming.AttackDefinition definition = RiderFistTiming.attack(!leftClick, attackIndex);
            PENDING_LUNGES.add(new PendingLunge(
                    player.getUUID(),
                    player.level().dimension(),
                    horizontalLook.normalize(),
                    gameTime,
                    definition.lungeStartTick() + RiderFistTiming.animationBlendTicks(),
                    definition.lungeEndTick() + RiderFistTiming.animationBlendTicks(),
                    definition.lungeStrength()
            ));
        }
    }

    private static List<LivingEntity> findHitboxTargets(ServerPlayer player, Vec3 center, Vec3 direction,
                                                         RiderFistTiming.HitboxPhase phase) {
        Vec3 horizontalDirection = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontalDirection.lengthSqr() <= 1.0E-6D) {
            return List.of();
        }
        horizontalDirection = horizontalDirection.normalize();
        Vec3 right = new Vec3(horizontalDirection.z, 0.0D, -horizontalDirection.x);
        double searchRadius = Math.max(phase.startOffset(), phase.endOffset())
                + Math.max(phase.width(), phase.depth()) + 2.0D;
        AABB searchBounds = player.getBoundingBox().inflate(searchRadius, phase.height() + 2.0D, searchRadius);
        Vec3 attackDirection = horizontalDirection;
        return player.level().getEntitiesOfClass(
                LivingEntity.class,
                searchBounds,
                target -> isAssistCandidate(player, target)
                        && intersectsHitbox(target, center, attackDirection, right, phase)
        );
    }

    private static boolean intersectsHitbox(LivingEntity target, Vec3 center, Vec3 forward, Vec3 right,
                                            RiderFistTiming.HitboxPhase phase) {
        AABB bounds = target.getBoundingBox();
        if (bounds.maxY < center.y - phase.height() * 0.5D
                || bounds.minY > center.y + phase.height() * 0.5D) {
            return false;
        }

        Vec3 targetCenter = bounds.getCenter();
        double dx = targetCenter.x - center.x;
        double dz = targetCenter.z - center.z;
        double forwardDistance = dx * forward.x + dz * forward.z;
        double sideDistance = dx * right.x + dz * right.z;
        double halfX = (bounds.maxX - bounds.minX) * 0.5D;
        double halfZ = (bounds.maxZ - bounds.minZ) * 0.5D;
        double forwardRadius = Math.abs(forward.x) * halfX + Math.abs(forward.z) * halfZ;
        double sideRadius = Math.abs(right.x) * halfX + Math.abs(right.z) * halfZ;
        return Math.abs(forwardDistance) <= phase.depth() * 0.5D + forwardRadius
                && Math.abs(sideDistance) <= phase.width() * 0.5D + sideRadius;
    }

    /** true mientras dura la animacion de un golpe (o la pausa de impacto): en ese lapso no se puede bloquear. */
    public static boolean isAttacking(ServerPlayer player) {
        return serverGameTime(player) < NEXT_ATTACK_TIME.getOrDefault(player, 0L);
    }

    public static boolean hasActiveLunge(ServerPlayer player) {
        long gameTime = serverGameTime(player);
        return hasPendingLunge(player, gameTime);
    }

    private static long serverGameTime(ServerPlayer player) {
        return player.serverLevel().getServer().overworld().getGameTime();
    }

    private static boolean hasPendingLunge(ServerPlayer player, long gameTime) {
        return PENDING_LUNGES.stream().anyMatch(lunge -> {
            long elapsedTicks = gameTime - lunge.attackStartTime();
            return lunge.playerId().equals(player.getUUID())
                    && lunge.dimension().equals(player.level().dimension())
                    && elapsedTicks < lunge.endTick();
        });
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();

        Iterator<Map.Entry<ServerPlayer, BufferedAttack>> bufferedIterator = BUFFERED_ATTACKS.entrySet().iterator();
        while (bufferedIterator.hasNext()) {
            Map.Entry<ServerPlayer, BufferedAttack> entry = bufferedIterator.next();
            ServerPlayer buffered = entry.getKey();
            BufferedAttack request = entry.getValue();
            long now = server.overworld().getGameTime();
            if (buffered == null || !buffered.isAlive() || now > request.expiresAt()
                    || !isEligibleGrowingPlayer(buffered) || Especial3Combat.isActive(buffered) || Especial2Combat.isActive(buffered) || Especial1Combat.isActive(buffered)
                    || BackwardBlockCombat.isBlocking(buffered)) {
                bufferedIterator.remove();
                continue;
            }
            if (now >= NEXT_ATTACK_TIME.getOrDefault(buffered, 0L)) {
                bufferedIterator.remove();
                startAttack(buffered, request.leftClick(), now);
            }
        }

        Iterator<PendingLunge> lungeIterator = PENDING_LUNGES.iterator();
        while (lungeIterator.hasNext()) {
            PendingLunge lunge = lungeIterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(lunge.playerId());
            if (player == null || !player.isAlive()
                    || !player.level().dimension().equals(lunge.dimension())) {
                lungeIterator.remove();
                continue;
            }

            long gameTime = server.overworld().getGameTime();
            double elapsedTicks = gameTime - lunge.attackStartTime();
            if (!isEligibleGrowingPlayer(player)
                    || Especial3Combat.isActive(player) || Especial2Combat.isActive(player) || Especial1Combat.isActive(player)) {
                lungeIterator.remove();
                continue;
            }

            double tickIntervalStart = elapsedTicks - 1.0D;
            double overlapTicks = Math.min(elapsedTicks, lunge.endTick())
                    - Math.max(tickIntervalStart, lunge.startTick());
            if (overlapTicks > 0.0D) {
                double lungeProgress = overlapTicks
                        / (lunge.endTick() - lunge.startTick());
                player.setDeltaMovement(player.getDeltaMovement()
                        .add(currentLungeDirection(player, lunge.direction())
                                .scale(lunge.strength() * lungeProgress)));
                player.hurtMarked = true;
            }
            if (elapsedTicks >= lunge.endTick()) {
                lungeIterator.remove();
            }
        }

        Iterator<PendingAttack> iterator = PENDING_ATTACKS.iterator();
        while (iterator.hasNext()) {
            PendingAttack attack = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(attack.playerId());
            if (player == null || !player.isAlive()
                    || !player.level().dimension().equals(attack.dimension())) {
                iterator.remove();
                continue;
            }

            if (!isEligibleGrowingPlayer(player)) {
                iterator.remove();
                continue;
            }

            long gameTime = server.overworld().getGameTime();
            int elapsedTicks = (int) (gameTime - attack.attackStartTime());
            RiderFistTiming.AttackDefinition definition =
                    RiderFistTiming.attack(attack.heavy(), attack.attackIndex());
            int lastActiveTick = definition.phases().stream()
                    .mapToInt(phase -> phase.endTick() + RiderFistTiming.animationBlendTicks())
                    .max()
                    .orElseThrow();
            if (elapsedTicks > lastActiveTick) {
                iterator.remove();
                continue;
            }

            for (RiderFistTiming.HitboxPhase phase : definition.phases()) {
                int phaseStart = phase.startTick() + RiderFistTiming.animationBlendTicks();
                int phaseEnd = phase.endTick() + RiderFistTiming.animationBlendTicks();
                if (elapsedTicks < phaseStart || elapsedTicks > phaseEnd) {
                    continue;
                }
                double phaseProgress = phaseEnd == phaseStart
                        ? 1.0D
                        : (elapsedTicks - phaseStart) / (double) (phaseEnd - phaseStart);
                double forwardOffset = phase.startOffset()
                        + (phase.endOffset() - phase.startOffset()) * phaseProgress;
                Vec3 direction = currentLungeDirection(player, attack.direction());
                Vec3 hitboxCenter = player.position().add(0.0D, player.getBbHeight() * 0.55D, 0.0D)
                        .add(direction.scale(forwardOffset));
                for (LivingEntity target : findHitboxTargets(player, hitboxCenter, direction, phase)) {
                    if (!attack.hitEntityIds().add(target.getUUID())) {
                        continue;
                    }
                    RESOLVING_CUSTOM_IMPACTS.add(player.getUUID());
                    RESOLVING_EXTRA_KNOCKBACK.put(player.getUUID(), definition.extraKnockback());
                    RESOLVING_DAMAGE.put(player.getUUID(), definition.damage());
                    boolean damageApplied;
                    try {
                        damageApplied = target.hurt(
                                player.damageSources().playerAttack(player),
                                (float) definition.damage()
                        );
                    } finally {
                        RESOLVING_CUSTOM_IMPACTS.remove(player.getUUID());
                        RESOLVING_EXTRA_KNOCKBACK.remove(player.getUUID());
                        RESOLVING_DAMAGE.remove(player.getUUID());
                    }
                    if (!damageApplied) {
                        continue;
                    }

                    Vec3 look = player.getViewVector(1.0F);
                    Vec3 hitPos = target.position().add(0.0D, target.getBbHeight() / 2.0D, 0.0D);
                    playImpactFeedback(player, attack.attackIndex(), hitPos, look);
                    if (!attack.hitStopApplied()) {
                        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                                player,
                                new HitStopPacket(player.getId(), definition.hitStopTicks(), definition.shakeAmount())
                        );
                        attack.setHitStopApplied(true);
                    }
                    if (definition.hitStopTicks() > 0 && !attack.attackerHitStopApplied()) {
                        applyHitStop(player, definition.hitStopTicks());
                        attack.setAttackerHitStopApplied(true);
                    }
                    applyVictimReaction(target, direction, definition, gameTime);
                }
            }
        }

        expireStunnedMobs(server);
    }

    private static void applyVictimReaction(LivingEntity target, Vec3 attackDirection,
                                            RiderFistTiming.AttackDefinition definition, long gameTime) {
        if (definition.reaction() == RiderFistTiming.Reaction.NONE) {
            return;
        }

        if (target instanceof net.minecraft.world.entity.Mob mob
                && (STUNNED_MOBS.containsKey(mob) || PENDING_STUNS.containsKey(mob) || !mob.isNoAi())) {
            PENDING_STUNS.put(
                    mob,
                    new PendingStun(gameTime + STUN_PUSH_DELAY_TICKS, definition.reactionTicks())
            );
        }

        Vec3 velocity = target.getDeltaMovement();
        switch (definition.reaction()) {
            case STUN -> { }
            case KNOCKDOWN -> {
                Vec3 horizontal = new Vec3(attackDirection.x, 0.0D, attackDirection.z);
                if (horizontal.lengthSqr() > 1.0E-6D) {
                    horizontal = horizontal.normalize().scale(0.25D);
                }
                target.setDeltaMovement(horizontal.x, target.onGround() ? 0.0D : -0.15D, horizontal.z);
                target.hurtMarked = true;
            }
            case LAUNCHER -> {
                target.setDeltaMovement(velocity.x, Math.max(velocity.y, definition.launchVelocity()), velocity.z);
                target.hurtMarked = true;
            }
            case NONE -> { }
        }
    }

    /**
     * Suelta el aturdimiento (hit-stop sin IA) de un mob para que un empujon externo, como el impacto del
     * Especial 3, si lo mueva: con la IA apagada y el stun pendiente el knockback se perderia.
     */
    public static void releaseStunForPush(LivingEntity entity) {
        if (entity instanceof net.minecraft.world.entity.Mob mob) {
            PENDING_STUNS.remove(mob);
            if (STUNNED_MOBS.remove(mob) != null) {
                mob.setNoAi(false);
            }
        }
    }

    private static void expireStunnedMobs(MinecraftServer server) {
        long gameTime = server.overworld().getGameTime();
        Iterator<Map.Entry<net.minecraft.world.entity.Mob, PendingStun>> pendingIterator =
                PENDING_STUNS.entrySet().iterator();
        while (pendingIterator.hasNext()) {
            Map.Entry<net.minecraft.world.entity.Mob, PendingStun> entry = pendingIterator.next();
            net.minecraft.world.entity.Mob mob = entry.getKey();
            PendingStun pending = entry.getValue();
            if (mob.isRemoved() || !mob.isAlive()) {
                pendingIterator.remove();
            } else if (gameTime >= pending.applyAt()) {
                mob.setNoAi(true);
                mob.setTarget(null);
                STUNNED_MOBS.merge(mob, gameTime + pending.durationTicks(), Math::max);
                pendingIterator.remove();
            }
        }

        Iterator<Map.Entry<net.minecraft.world.entity.Mob, Long>> stunnedIterator = STUNNED_MOBS.entrySet().iterator();
        while (stunnedIterator.hasNext()) {
            Map.Entry<net.minecraft.world.entity.Mob, Long> entry = stunnedIterator.next();
            net.minecraft.world.entity.Mob mob = entry.getKey();
            if (mob.isRemoved() || !mob.isAlive()) {
                stunnedIterator.remove();
            } else if (gameTime >= entry.getValue()) {
                mob.setNoAi(false);
                stunnedIterator.remove();
            }
        }
    }

    /**
     * Hit-stop del servidor: el enemigo se queda quieto unos ticks (guardando su empuje para soltarlo despues)
     * y el siguiente golpe del atacante se retrasa lo mismo que dura la pausa de la animacion.
     */
    private static void applyHitStop(ServerPlayer player, int ticks) {
        long newReadyAt = NEXT_ATTACK_TIME.getOrDefault(player, 0L) + ticks;
        NEXT_ATTACK_TIME.put(player, newReadyAt);
        BufferedAttack buffered = BUFFERED_ATTACKS.get(player);
        if (buffered != null) {
            BUFFERED_ATTACKS.put(player, new BufferedAttack(buffered.leftClick(), newReadyAt + 1));
        }
    }

    /** Knockback extra configurado para el impacto que se está resolviendo. */
    /** Bloquea el combo normal hasta ese tick (lo usan las habilidades como el Especial 3). */
    public static void blockAttacksUntil(ServerPlayer player, long gameTime) {
        NEXT_ATTACK_TIME.merge(player, gameTime, Math::max);
    }

    /** Descarta un clic de combo que haya quedado en buffer (lo usan las habilidades como el Especial 3). */
    public static void clearBufferedAttack(ServerPlayer player) {
        BUFFERED_ATTACKS.remove(player);
    }

    /**
     * Golpe puntual fuera del combo (habilidades). Pasa por el mismo camino de dano que los golpes del combo,
     * asi CombatEventHandler no lo cancela como golpe inmediato.
     */
    public static boolean dealSpecialHit(ServerPlayer player, LivingEntity target, float damage,
                                         float extraKnockback, Vec3 look) {
        RESOLVING_CUSTOM_IMPACTS.add(player.getUUID());
        RESOLVING_EXTRA_KNOCKBACK.put(player.getUUID(), extraKnockback);
        RESOLVING_DAMAGE.put(player.getUUID(), (double) damage);
        boolean applied;
        try {
            applied = target.hurt(player.damageSources().playerAttack(player), damage);
        } finally {
            RESOLVING_CUSTOM_IMPACTS.remove(player.getUUID());
            RESOLVING_EXTRA_KNOCKBACK.remove(player.getUUID());
            RESOLVING_DAMAGE.remove(player.getUUID());
        }
        if (applied) {
            Vec3 hitPos = target.position().add(0.0D, target.getBbHeight() / 2.0D, 0.0D);
            playImpactFeedback(player, 2, hitPos, look);
        }
        return applied;
    }

    public static float extraKnockback(UUID attackerId) {
        return RESOLVING_EXTRA_KNOCKBACK.getOrDefault(attackerId, 0.4F);
    }

    public static double resolvingDamage(UUID attackerId) {
        return RESOLVING_DAMAGE.getOrDefault(attackerId, 5.0D);
    }

    public static boolean isResolvingCustomImpact(UUID attackerId) {
        return RESOLVING_CUSTOM_IMPACTS.contains(attackerId);
    }

    public static boolean shouldCancelImmediateGrowingHit(ServerPlayer player) {
        return isEligibleGrowingPlayer(player)
                && !RESOLVING_CUSTOM_IMPACTS.contains(player.getUUID());
    }

    private static boolean isEligibleGrowingPlayer(ServerPlayer player) {
        return isGrowingFistPlayer(player);
    }

    /** Formas que pelean con los golpes ligeros y pesados: Growing (1) y Mighty (2, el traje kuuga). */
    public static boolean usesFistSystem(int form) {
        return form == 1 || form == 2;
    }

    /** Growing o Mighty transformado, con cinturon y ambas manos vacias (sirve en cliente y servidor). */
    public static boolean isGrowingFistPlayer(Player player) {
        return TransformationHelper.isTransformed(player)
                && player.hasData(ModAttachments.RIDER_STATE)
                && usesFistSystem(player.getData(ModAttachments.RIDER_STATE).getForm())
                && player.getMainHandItem().isEmpty()
                && player.getOffhandItem().isEmpty()
                && TransformationHelper.isBeltEquipped(player);
    }

    /** Diferencia de yaw (grados, -180..180) entre la mirada del jugador y la direccion horizontal al objetivo. */
    private static float yawDifference(ServerPlayer player, LivingEntity target) {
        Vec3 eye = player.getEyePosition();
        Vec3 center = target.getBoundingBox().getCenter();
        double dx = center.x - eye.x;
        double dz = center.z - eye.z;
        if (dx * dx + dz * dz < 0.09D) {
            // Casi encima: no hay una direccion fiable, no se gira.
            return 0.0F;
        }
        float targetYaw = (float) (Mth.atan2(-dx, dz) * (180.0D / Math.PI));
        return Mth.wrapDegrees(targetYaw - player.getYRot());
    }

    /** Enemigo valido mas cercano dentro del cono corto (alcance ATTACK_RANGE, medio angulo AIM_ASSIST_HALF_ANGLE). */
    private static LivingEntity findAssistTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        AABB searchBounds = new AABB(eye, eye).inflate(ATTACK_RANGE);
        double maxDistSqr = ATTACK_RANGE * ATTACK_RANGE;

        LivingEntity best = null;
        double bestDistSqr = Double.MAX_VALUE;
        float bestAngle = Float.MAX_VALUE;
        for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class, searchBounds,
                entity -> isAssistCandidate(player, entity))) {
            // Distancia al punto mas cercano de su caja, igual que lo que alcanzaria el rayo.
            double distSqr = candidate.getBoundingBox().distanceToSqr(eye);
            if (distSqr > maxDistSqr) {
                continue;
            }
            float angle = Math.abs(yawDifference(player, candidate));
            if (angle > AIM_ASSIST_HALF_ANGLE || !player.hasLineOfSight(candidate)) {
                continue;
            }
            if (distSqr < bestDistSqr - 1.0E-4D || (distSqr < bestDistSqr + 1.0E-4D && angle < bestAngle)) {
                best = candidate;
                bestDistSqr = distSqr;
                bestAngle = angle;
            }
        }
        return best;
    }

    private static boolean isAssistCandidate(ServerPlayer player, LivingEntity entity) {
        return entity != player
                && entity.isAlive()
                && !entity.isSpectator()
                && entity.isPickable()
                && !(entity instanceof ArmorStand)
                && !player.isAlliedTo(entity)
                && !(entity instanceof TamableAnimal pet && pet.isOwnedBy(player));
    }

    /**
     * Direccion del avance: la mirada actual, desviada hasta LUNGE_STEER_MAX grados hacia el enemigo mas cercano
     * si no estas apuntando a nadie. Asi el cuerpo va hacia el enemigo sin que la camara tenga que girar.
     */
    private static Vec3 currentLungeDirection(ServerPlayer player, Vec3 fallback) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() <= 1.0E-6D) {
            return fallback;
        }
        Vec3 direction = horizontal.normalize();
        if (findRayTarget(player) == null) {
            LivingEntity target = findAssistTarget(player);
            if (target != null) {
                float steer = Mth.clamp(yawDifference(player, target), -LUNGE_STEER_MAX, LUNGE_STEER_MAX);
                double rad = Math.toRadians(player.getYRot() + steer);
                direction = new Vec3(-Math.sin(rad), 0.0D, Math.cos(rad));
            }
        }
        return direction;
    }

    private static LivingEntity findRayTarget(ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(ATTACK_RANGE));
        BlockHitResult blockHit = player.level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
        ));
        double maxDistance = blockHit.getType() == HitResult.Type.MISS
                ? ATTACK_RANGE * ATTACK_RANGE
                : start.distanceToSqr(blockHit.getLocation());
        AABB searchBounds = player.getBoundingBox()
                .expandTowards(look.scale(ATTACK_RANGE))
                .inflate(1.0D);

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player,
                start,
                end,
                searchBounds,
                entity -> entity instanceof LivingEntity
                        && entity.getId() != player.getId()
                        && entity.isAlive()
                        && entity.isPickable(),
                maxDistance
        );
        return entityHit != null && entityHit.getEntity() instanceof LivingEntity livingEntity
                ? livingEntity
                : null;
    }

    private static final class PendingAttack {
        private final UUID playerId;
        private final ResourceKey<Level> dimension;
        private final long attackStartTime;
        private final int attackIndex;
        private final boolean heavy;
        private final Vec3 direction;
        private final Set<UUID> hitEntityIds = new HashSet<>();
        private boolean hitStopApplied;
        private boolean attackerHitStopApplied;

        private PendingAttack(UUID playerId, ResourceKey<Level> dimension, long attackStartTime,
                              int attackIndex, boolean heavy, Vec3 direction) {
            this.playerId = playerId;
            this.dimension = dimension;
            this.attackStartTime = attackStartTime;
            this.attackIndex = attackIndex;
            this.heavy = heavy;
            this.direction = direction;
        }

        private UUID playerId() { return playerId; }
        private ResourceKey<Level> dimension() { return dimension; }
        private long attackStartTime() { return attackStartTime; }
        private int attackIndex() { return attackIndex; }
        private boolean heavy() { return heavy; }
        private Vec3 direction() { return direction; }
        private Set<UUID> hitEntityIds() { return hitEntityIds; }
        private boolean hitStopApplied() { return hitStopApplied; }
        private void setHitStopApplied(boolean applied) { hitStopApplied = applied; }
        private boolean attackerHitStopApplied() { return attackerHitStopApplied; }
        private void setAttackerHitStopApplied(boolean applied) { attackerHitStopApplied = applied; }
    }

    private record BufferedAttack(boolean leftClick, long expiresAt) {}

    private record PendingLunge(UUID playerId, ResourceKey<Level> dimension, Vec3 direction, long attackStartTime,
                                double startTick, double endTick, double strength) {}

    private record PendingStun(long applyAt, int durationTicks) {}

    private static final class AttackState {
        private int nextAttackIndex;
    }
}
