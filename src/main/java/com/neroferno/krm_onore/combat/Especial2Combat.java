package com.neroferno.krm_onore.combat;

import com.neroferno.krm_onore.KRMRevoMod;
import com.neroferno.krm_onore.attachment.ModAttachments;
import com.neroferno.krm_onore.network.GrowingSpecialAnimationPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Especial 2 (tecla F, solo sin arma equipada): contraataque.
 *
 * <p>Fase 1 - animation.especial2_1 (1.6667 s): se agacha 0 -> 0.33 s, MANTIENE la pose 0.33 -> 1.33 s (1 segundo,
 * ventana de contraataque) y se relaja 1.33 -> 1.67 s. Si en la ventana te golpea un enemigo cuerpo a cuerpo, el
 * golpe se anula (sin dano) y pasa a la fase 2. Si no te golpean, vuelves a la normalidad (cooldown 5 s).
 * Los proyectiles NO cuentan como golpe: solo cancelan la animacion (el dano se aplica normal, cooldown 5 s).
 *
 * <p>Fase 2 - animation.especial2_2 (1 s): en 0.29 s las particulas de fuego van al locator2 (mano derecha) y en
 * 0.42 s se lanza el golpe: dano de fuego a los enemigos de enfrente, hasta 1 bloque de distancia del jugador.
 * Cooldown 3 s, contado desde que termina la animacion.
 *
 * <p>Coste: 20 de energia al usarla (se cobra al arrancar, aunque despues no te golpeen).
 */
@EventBusSubscriber(modid = KRMRevoMod.MODID)
public final class Especial2Combat {
    // --- Fase 1: tiempos (segundos de especial2_1 -> ticks) ---
    private static final int WINDUP_TICKS = ticks(0.3333D);                    // 7: agacharse hasta la pose
    private static final int COUNTER_WINDOW_TICKS = 20;                        // 1 s manteniendo la pose
    private static final int COUNTER_END_TICK = WINDUP_TICKS + COUNTER_WINDOW_TICKS; // 27
    private static final int STANCE_END_TICK = ticks(1.6667D);                 // 33: animacion completa

    // --- Fase 2: tiempos (segundos de especial2_2 -> ticks) ---
    private static final int FIRE_GATHER_TICK = ticks(0.2917D);                // 6: particulas hacia locator2
    private static final int STRIKE_TICK = ticks(0.4167D);                     // 8: sale el golpe
    private static final int STRIKE_END_TICK = ticks(1.0D);                    // 20

    // --- Golpe ---
    /** Alcance medido desde la posicion del jugador hasta la caja del enemigo (1 bloque). */
    private static final double STRIKE_REACH = 1.0D;
    /** Coseno minimo entre la mirada y el enemigo para considerarlo "enfrente" (0.3 = ~72 grados a cada lado). */
    private static final double FRONT_MIN_DOT = 0.3D;
    private static final float STRIKE_DAMAGE = 12.0F;
    private static final float STRIKE_KNOCKBACK = 0.8F;
    /** Segundos que el enemigo golpeado sigue ardiendo (dano de fuego). */
    private static final int BURN_TICKS = 80;

    // --- Posicion aproximada de locator2 (mano derecha) respecto al jugador, para las particulas. Calibrable. ---
    private static final double HAND_FORWARD = 0.55D;
    private static final double HAND_SIDE = 0.35D;
    private static final double HAND_HEIGHT = 1.0D;

    // --- Coste y cooldown (contados desde que TERMINA el especial) ---
    private static final float ENERGY_COST = 20.0F;
    private static final int COOLDOWN_MISS_TICKS = 100;   // 5 s: nadie te golpeo (o un proyectil lo cancelo)
    private static final int COOLDOWN_HIT_TICKS = 60;     // 3 s: el contraataque conecto

    private static final Map<UUID, State> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWN_END = new HashMap<>();

    private Especial2Combat() {}

    private static int ticks(double seconds) {
        return (int) Math.round(seconds * 20.0D);
    }

    /** Duracion de la fase 1 en ticks (bloqueo de golpes/animaciones en cliente). */
    public static int stanceTicks() {
        return STANCE_END_TICK + 1;
    }

    /** Duracion de la fase 2 en ticks. */
    public static int strikeTicks() {
        return STRIKE_END_TICK + 1;
    }

    /** Seguro para el combo normal y el bloqueo: mientras corre el Especial 2 solo debe verse su animacion. */
    public static boolean isActive(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    public static void start(ServerPlayer player) {
        if (!player.isAlive() || !Especial3Combat.isEligible(player) || ACTIVE.containsKey(player.getUUID())
                || Especial3Combat.isActive(player) || Especial1Combat.isActive(player)
                || player.getData(ModAttachments.RIDER_STATE).getRiderKickState() > 0) {
            return;
        }

        long now = gameTime(player);
        long cooldownEnd = COOLDOWN_END.getOrDefault(player.getUUID(), 0L);
        if (now < cooldownEnd) {
            player.displayClientMessage(
                    Component.translatable("chat.krm_revo.cooldown_actionbar",
                                    String.format("%.1f", (cooldownEnd - now) / 20.0D))
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    true
            );
            return;
        }

        com.neroferno.krm_onore.attachment.RiderEnergyData energy = player.getData(ModAttachments.RIDER_ENERGY);
        if (energy.getEnergy() < ENERGY_COST) {
            player.displayClientMessage(
                    Component.translatable("chat.krm_revo.especial2_no_energy")
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    true
            );
            return;
        }

        energy.consume(ENERGY_COST);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new com.neroferno.krm_onore.network.SyncRiderEnergyPacket(
                        player.getId(), energy.getEnergy(), energy.getMaxEnergy())
        );

        ACTIVE.put(player.getUUID(), new State(now));
        RiderFistCombat.clearBufferedAttack(player);
        sendAnimation(player, "especial2_1");
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 0.9F);
    }

    /**
     * Decide que hace un golpe recibido. Solo muta el State (nunca el mapa ACTIVE): este evento puede dispararse
     * mientras onServerTick recorre ACTIVE (p. ej. por espinas al golpear), y tocar el mapa aqui lo romperia.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        State state = ACTIVE.get(player.getUUID());
        if (state == null || state.cancelled) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }

        long now = gameTime(player);

        // Durante toda la animacion del contraataque se ignora el dano normal recibido.
        if (state.phase == Phase.STRIKE) {
            event.setCanceled(true);
            return;
        }

        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            // Un proyectil no cuenta como golpe: no se anula el dano, solo se cancela la animacion.
            state.cancelled = true;
            return;
        }

        if (!isMeleeHit(source)) {
            // Fuego, caida, explosiones, etc.: ni cuentan ni interrumpen.
            return;
        }

        long t = now - state.startTime;
        if (t >= WINDUP_TICKS && t < COUNTER_END_TICK) {
            event.setCanceled(true);
            state.phase = Phase.STRIKE;
            state.strikeStart = now;
            sendAnimation(player, "especial2_2");
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2F, 1.2F);
        } else {
            // Te golpean fuera de la ventana (agachandote o relajandote): el dano entra y se cancela la animacion.
            state.cancelled = true;
        }
    }

    private static boolean isMeleeHit(DamageSource source) {
        return source.getDirectEntity() instanceof LivingEntity attacker
                && source.getEntity() == attacker
                && !source.is(DamageTypeTags.IS_PROJECTILE);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }

        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();
        Iterator<Map.Entry<UUID, State>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, State> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            State state = entry.getValue();
            if (player == null) {
                iterator.remove();
                continue;
            }

            if (state.cancelled || !player.isAlive() || !Especial3Combat.isEligible(player)) {
                // Cancelado por un proyectil, un golpe fuera de ventana, sacar un arma o morir.
                if (player.isAlive()) {
                    sendAnimation(player, "especial2_cancel");
                }
                COOLDOWN_END.put(player.getUUID(), now + COOLDOWN_MISS_TICKS);
                iterator.remove();
                continue;
            }

            if (state.phase == Phase.STANCE) {
                if (now - state.startTime >= STANCE_END_TICK) {
                    // Nadie te golpeo: vuelves a la normalidad y el cooldown es el largo.
                    COOLDOWN_END.put(player.getUUID(), now + COOLDOWN_MISS_TICKS);
                    iterator.remove();
                }
                continue;
            }

            long t = now - state.strikeStart;
            if (t == FIRE_GATHER_TICK) {
                gatherFire(player);
            }
            if (t == STRIKE_TICK && !state.struck) {
                state.struck = true;
                strike(player);
            }
            if (t >= STRIKE_END_TICK) {
                COOLDOWN_END.put(player.getUUID(), now + COOLDOWN_HIT_TICKS);
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            COOLDOWN_END.remove(player.getUUID());
            ACTIVE.remove(player.getUUID());
        }
    }

    /** Posicion aproximada de locator2 (mano derecha) en el mundo. */
    private static Vec3 handPosition(ServerPlayer player) {
        Vec3 forward = flatLook(player);
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        return player.position()
                .add(forward.scale(HAND_FORWARD))
                .add(right.scale(HAND_SIDE))
                .add(0.0D, HAND_HEIGHT, 0.0D);
    }

    private static Vec3 flatLook(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        return flat.lengthSqr() > 1.0E-6D ? flat.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
    }

    /** 0.29 s: particulas de fuego que convergen en locator2 (la mano del golpe). */
    private static void gatherFire(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 hand = handPosition(player);
        java.util.concurrent.ThreadLocalRandom rand = java.util.concurrent.ThreadLocalRandom.current();
        for (int i = 0; i < 14; i++) {
            Vec3 offset = new Vec3(rand.nextDouble(-1.0D, 1.0D), rand.nextDouble(-1.0D, 1.0D), rand.nextDouble(-1.0D, 1.0D));
            if (offset.lengthSqr() < 1.0E-4D) {
                continue;
            }
            offset = offset.normalize().scale(0.9D);
            Vec3 from = hand.add(offset);
            // count = 0: dx/dy/dz se usan como velocidad (hacia la mano), multiplicada por speed.
            level.sendParticles(ParticleTypes.FLAME, from.x, from.y, from.z, 0,
                    -offset.x * 0.12D, -offset.y * 0.12D, -offset.z * 0.12D, 1.0D);
        }
        level.sendParticles(ParticleTypes.SMALL_FLAME, hand.x, hand.y, hand.z, 6, 0.08D, 0.08D, 0.08D, 0.01D);
    }

    /** 0.42 s: golpe de fuego a los enemigos de enfrente, hasta 1 bloque de distancia del jugador. */
    private static void strike(ServerPlayer player) {
        Vec3 forward = flatLook(player);
        double px = player.getX();
        double pz = player.getZ();
        AABB area = player.getBoundingBox().inflate(STRIKE_REACH + 2.0D, 1.0D, STRIKE_REACH + 2.0D);
        double minY = player.getY() - 0.5D;
        double maxY = player.getY() + player.getBbHeight() + 0.5D;

        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive() && !e.isSpectator())) {
            AABB box = target.getBoundingBox();
            if (box.maxY < minY || box.minY > maxY) {
                continue;
            }
            // Distancia horizontal del jugador al punto mas cercano de la caja del enemigo.
            double dx = Math.max(Math.max(box.minX - px, 0.0D), px - box.maxX);
            double dz = Math.max(Math.max(box.minZ - pz, 0.0D), pz - box.maxZ);
            if (dx * dx + dz * dz > STRIKE_REACH * STRIKE_REACH) {
                continue;
            }
            // "Enfrente": dentro del cono de la mirada (si esta pegado al centro del jugador, cuenta siempre).
            Vec3 to = new Vec3(target.getX() - px, 0.0D, target.getZ() - pz);
            if (to.lengthSqr() > 1.0E-4D && forward.dot(to.normalize()) < FRONT_MIN_DOT) {
                continue;
            }
            if (RiderFistCombat.dealSpecialHit(player, target, STRIKE_DAMAGE, STRIKE_KNOCKBACK, player.getViewVector(1.0F))) {
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), BURN_TICKS));
            }
        }

        if (player.level() instanceof ServerLevel level) {
            Vec3 front = player.position().add(forward.scale(0.9D)).add(0.0D, 1.0D, 0.0D);
            level.sendParticles(ParticleTypes.FLAME, front.x, front.y, front.z, 30, 0.3D, 0.25D, 0.3D, 0.06D);
            level.sendParticles(ParticleTypes.SMALL_FLAME, front.x, front.y, front.z, 12, 0.25D, 0.2D, 0.25D, 0.04D);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.9F);
    }

    private static void sendAnimation(ServerPlayer player, String animation) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player, new GrowingSpecialAnimationPacket(player.getId(), animation));
    }

    private static long gameTime(ServerPlayer player) {
        return player.serverLevel().getServer().overworld().getGameTime();
    }

    private enum Phase { STANCE, STRIKE }

    private static final class State {
        private final long startTime;
        private Phase phase = Phase.STANCE;
        private long strikeStart;
        private boolean struck;
        private boolean cancelled;

        private State(long startTime) {
            this.startTime = startTime;
        }
    }
}
