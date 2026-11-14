package com.neroferno.krm_onore.combat;

import com.neroferno.krm_onore.KRMRevoMod;
import com.neroferno.krm_onore.attachment.ModAttachments;
import com.neroferno.krm_onore.network.AbilityHelper;
import com.neroferno.krm_onore.network.GrowingSpecialAnimationPacket;
import com.neroferno.krm_onore.network.SyncVelocityPacket;
import com.neroferno.krm_onore.network.TransformationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Especial 3 (tecla C): salto de ~2.4 bloques, dash hacia el enemigo (hasta 14 bloques), golpe, breve hold en el
 * aire y vuelta al suelo.
 * Los tiempos salen de animation.especial3 (kuuga_suit), ya retimada: agacharse 0 -> 0.33 s, subir 0.33 -> 0.65 s
 * (anticipacion larga), quedarse arriba 0.65 -> 0.75 s (rapido), dash 0.75 -> 0.90 s (golpe en 0.90 s, 0.15 s de
 * dash), hold en el aire 0.90 -> 1.00 s, bajar 1.00 -> 1.35 s.
 */
@EventBusSubscriber(modid = KRMRevoMod.MODID)
public final class Especial3Combat {
    // --- Tiempos (segundos de la animacion -> ticks) ---
    private static final int RISE_START_TICK = ticks(0.3333D);  // 7
    private static final int RISE_END_TICK = ticks(0.65D);      // 13
    private static final int DASH_START_TICK = ticks(0.75D);    // 15
    private static final int IMPACT_TICK = ticks(0.90D);        // 18
    private static final int HOLD_END_TICK = ticks(1.00D);      // 20
    private static final int END_TICK = ticks(1.35D);           // 27

    // --- Movimiento ---
    private static final double RISE_HEIGHT = 2.4D;
    private static final double DASH_RANGE = 14.0D;
    private static final double MAX_DASH_SPEED = 4.4D;   // bloques por tick (dash rapido)
    private static final double STOP_DISTANCE = 1.4D;    // se queda a esta distancia (horizontal) del enemigo
    private static final double DIVE_HEIGHT = 0.5D;      // altura sobre los pies del enemigo al golpear
    private static final double HIT_REACH = 3.0D;

    // --- Sonidos (mismo criterio de peso/volumen que Rider Kick en CombatEventHandler) ---
    private static final net.minecraft.sounds.SoundEvent ANTICIPATION_SOUND =
            net.minecraft.sounds.SoundEvents.FIRECHARGE_USE;
    private static final float ANTICIPATION_VOLUME = 1.2F;
    private static final float ANTICIPATION_PITCH = 0.75F;

    private static final net.minecraft.sounds.SoundEvent LAUNCH_SOUND =
            net.minecraft.sounds.SoundEvents.WIND_CHARGE_THROW;
    private static final float LAUNCH_VOLUME = 1.8F;
    private static final float LAUNCH_PITCH = 0.7F;

    private static final net.minecraft.sounds.SoundEvent IMPACT_HOLD_SOUND =
            net.minecraft.sounds.SoundEvents.SHIELD_BLOCK;
    private static final float IMPACT_HOLD_VOLUME = 1.6F;
    private static final float IMPACT_HOLD_PITCH = 0.55F;

    /**
     * Ajuste vertical al volver al suelo, en bloques. Es la altura que deben tener los pies del jugador sobre el
     * suelo detectado cuando termina la bajada. Se calibra con locator4 del modelo (0 = pies pegados al suelo).
     */
    private static final double LOCATOR4_GROUND_OFFSET = 0.0D;

    // --- Onda del impacto: aleja a los demas enemigos sin danarlos (valores provisionales) ---
    private static final double IMPACT_PUSH_RADIUS = 4.0D;
    private static final double IMPACT_PUSH_STRENGTH = 1.0D;
    private static final double IMPACT_PUSH_LIFT = 0.25D;

    // --- Dano al enemigo golpeado ---
    private static final float HIT_DAMAGE = 15.0F;
    private static final float HIT_KNOCKBACK = 1.2F;

    // --- Coste y cooldown ---
    private static final float ENERGY_COST = 15.0F;
    /** Cooldown de 4 s, contado desde que TERMINA el especial (no desde que se pulsa C). */
    private static final int COOLDOWN_TICKS = 80;

    private static final Map<UUID, State> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWN_END = new HashMap<>();

    private Especial3Combat() {}

    /** Duracion total del Especial 3 en ticks (incluye el tick extra con el que el servidor bloquea el combo). */
    public static int durationTicks() {
        return END_TICK + 1;
    }

    private static int ticks(double seconds) {
        return (int) Math.round(seconds * 20.0D);
    }

    /** Seguro para el combo normal: mientras el Especial 3 esta corriendo, solo debe verse su propia animacion. */
    public static boolean isActive(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    public static void start(ServerPlayer player) {
        if (!player.isAlive() || !isEligible(player) || ACTIVE.containsKey(player.getUUID())
                || Especial1Combat.isActive(player) || Especial2Combat.isActive(player)
                || !player.onGround()
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

        com.neroferno.krm_onore.attachment.RiderEnergyData energy =
                player.getData(ModAttachments.RIDER_ENERGY);
        if (energy.getEnergy() < ENERGY_COST) {
            player.displayClientMessage(
                    Component.translatable("chat.krm_revo.especial3_no_energy")
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    true
            );
            return;
        }

        LivingEntity target = AbilityHelper.findTarget(player, DASH_RANGE);
        if (target == null) {
            player.displayClientMessage(
                    Component.translatable("chat.krm_revo.especial3_no_target")
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    true
            );
            return;
        }

        // Solo se cobra energia si el especial realmente arranca (hay objetivo).
        energy.consume(ENERGY_COST);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new com.neroferno.krm_onore.network.SyncRiderEnergyPacket(
                        player.getId(), energy.getEnergy(), energy.getMaxEnergy())
        );

        ACTIVE.put(player.getUUID(), new State(now, target.getId(), player.getY()));
        RiderFistCombat.blockAttacksUntil(player, now + END_TICK + 1L);
        // Descarta cualquier clic de combo que haya quedado en buffer justo antes de arrancar, para que no
        // dispare la animacion de combo ligero/pesado apenas termine el Especial 3.
        RiderFistCombat.clearBufferedAttack(player);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new GrowingSpecialAnimationPacket(player.getId(), "especial3")
        );
        // Sonido de carga/anticipacion: arranca justo cuando empieza a agacharse, para que se sienta desde ya.
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ANTICIPATION_SOUND, net.minecraft.sounds.SoundSource.PLAYERS, ANTICIPATION_VOLUME, ANTICIPATION_PITCH);
    }

    /**
     * Invulnerable durante todo el Especial 3: se cancela el dano ANTES de que llegue a la vida y a la absorcion
     * de la barra de energia (esa absorcion vive en LivingDamageEvent.Pre, que ya no se dispara). Se dejan pasar
     * solo los danos que ignoran la invulnerabilidad (vacio, /kill) para no dejar al jugador atrapado.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && ACTIVE.containsKey(player.getUUID())
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
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

            long t = now - state.startTime;
            if (!player.isAlive() || !isEligible(player) || t >= END_TICK) {
                finish(player);
                COOLDOWN_END.put(player.getUUID(), now + COOLDOWN_TICKS);
                iterator.remove();
                continue;
            }

            player.fallDistance = 0.0F;
            applyVelocity(player, velocityFor(player, state, t));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            COOLDOWN_END.remove(event.getEntity().getUUID());
        }
        if (event.getEntity() instanceof ServerPlayer player && ACTIVE.remove(player.getUUID()) != null) {
            // NoGravity se guarda en el NBT del jugador: sin esto se quedaria flotando al volver a entrar.
            player.setNoGravity(false);
        }
    }

    private static Vec3 velocityFor(ServerPlayer player, State state, long t) {
        double y = player.getY();

        if (t < RISE_START_TICK) {
            // Agacharse: quieto.
            return new Vec3(0.0D, player.getDeltaMovement().y, 0.0D);
        }

        if (t < DASH_START_TICK) {
            player.setNoGravity(true);
            double progress = t < RISE_END_TICK
                    ? (t - RISE_START_TICK + 1) / (double) (RISE_END_TICK - RISE_START_TICK)
                    : 1.0D;
            double wantedY = state.baseY + RISE_HEIGHT * progress;
            return new Vec3(0.0D, wantedY - y, 0.0D);
        }

        if (t < IMPACT_TICK) {
            player.setNoGravity(true);
            if (t == DASH_START_TICK) {
                // Mismo findTarget que el Rider Kick, pero a 14 bloques o menos.
                LivingEntity retarget = AbilityHelper.findTarget(player, DASH_RANGE);
                if (retarget != null) {
                    state.targetId = retarget.getId();
                }
                // Sonido de lanzamiento: marca el arranque del dash con mas impacto hacia el enemigo.
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        LAUNCH_SOUND, net.minecraft.sounds.SoundSource.PLAYERS, LAUNCH_VOLUME, LAUNCH_PITCH);
            }
            LivingEntity target = currentTarget(player, state);
            if (target == null) {
                return new Vec3(0.0D, state.baseY + RISE_HEIGHT - y, 0.0D);
            }

            Vec3 targetPos = target.position();
            Vec3 flat = new Vec3(targetPos.x - player.getX(), 0.0D, targetPos.z - player.getZ());
            Vec3 direction = flat.lengthSqr() > 1.0E-6D
                    ? flat.normalize()
                    : new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z).normalize();
            Vec3 aim = new Vec3(
                    targetPos.x - direction.x * STOP_DISTANCE,
                    targetPos.y + DIVE_HEIGHT,
                    targetPos.z - direction.z * STOP_DISTANCE
            );
            int remaining = (int) Math.max(1L, IMPACT_TICK - t);
            Vec3 velocity = aim.subtract(player.position()).scale(1.0D / remaining);
            return velocity.length() > MAX_DASH_SPEED ? velocity.normalize().scale(MAX_DASH_SPEED) : velocity;
        }

        // Golpe: se permite una ventana corta durante la pausa para absorber variaciones de movimiento.
        player.setNoGravity(true);
        if (t <= HOLD_END_TICK && !state.hitDone) {
            LivingEntity target = currentTarget(player, state);
            if (target != null && player.distanceToSqr(target) <= HIT_REACH * HIT_REACH) {
                // A recent hit can leave vanilla hurt immunity active and make hurt() reject this special.
                target.invulnerableTime = 0;
                state.hitDone = RiderFistCombat.dealSpecialHit(
                        player, target, HIT_DAMAGE, HIT_KNOCKBACK, player.getViewVector(1.0F));
                if (state.hitDone) {
                    pushAwayOthers(player, target);
                }
            }
        }
        if (t == IMPACT_TICK) {
            // Sonido de impacto que se sostiene durante el hold, para que se sienta el remate antes de bajar.
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    IMPACT_HOLD_SOUND, net.minecraft.sounds.SoundSource.PLAYERS, IMPACT_HOLD_VOLUME, IMPACT_HOLD_PITCH);
        }

        if (t < HOLD_END_TICK) {
            // Se mantiene quieto en el aire sobre la pose de impacto (hold) antes de empezar a bajar.
            return Vec3.ZERO;
        }

        double placeY = groundY(player) + LOCATOR4_GROUND_OFFSET;
        int remaining = (int) Math.max(1L, END_TICK - t);
        double vy = Math.max(-1.5D, Math.min(0.0D, (placeY - y) / remaining));
        return new Vec3(0.0D, vy, 0.0D);
    }

    /** Solo el objetivo recibe dano; el resto de criaturas cercanas al impacto salen empujadas, sin dano. */
    private static void pushAwayOthers(ServerPlayer player, LivingEntity hitTarget) {
        Vec3 center = hitTarget.position();
        AABB area = new AABB(center, center).inflate(IMPACT_PUSH_RADIUS);
        for (LivingEntity other : player.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity != hitTarget && entity.isAlive() && !entity.isSpectator())) {
            double dx = center.x - other.getX();
            double dz = center.z - other.getZ();
            if (dx * dx + dz * dz < 1.0E-4D) {
                Vec3 look = player.getLookAngle();
                dx = -look.x;
                dz = -look.z;
            }
            // knockback() empuja en sentido contrario al vector (x, z) que recibe, como en un golpe normal.
            RiderFistCombat.releaseStunForPush(other);
            other.knockback(IMPACT_PUSH_STRENGTH, dx, dz);
            other.setDeltaMovement(other.getDeltaMovement().add(0.0D, IMPACT_PUSH_LIFT, 0.0D));
            other.hurtMarked = true;
        }
    }

    private static LivingEntity currentTarget(ServerPlayer player, State state) {
        Entity entity = player.level().getEntity(state.targetId);
        if (entity instanceof LivingEntity living && living.isAlive()
                && player.distanceTo(living) <= DASH_RANGE + 2.0D) {
            return living;
        }
        return null;
    }

    private static double groundY(ServerPlayer player) {
        Vec3 start = player.position().add(0.0D, 0.5D, 0.0D);
        Vec3 end = start.add(0.0D, -16.0D, 0.0D);
        BlockHitResult hit = player.level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
        ));
        return hit.getType() == HitResult.Type.MISS ? player.getY() : hit.getLocation().y;
    }

    private static void applyVelocity(ServerPlayer player, Vec3 velocity) {
        player.setDeltaMovement(velocity);
        player.hurtMarked = true;
        PacketDistributor.sendToPlayer(player, new SyncVelocityPacket(velocity.x, velocity.y, velocity.z));
    }

    private static void finish(ServerPlayer player) {
        player.setNoGravity(false);
        player.fallDistance = 0.0F;
        applyVelocity(player, new Vec3(0.0D, Math.min(0.0D, player.getDeltaMovement().y), 0.0D));
    }

    private static long gameTime(ServerPlayer player) {
        return player.serverLevel().getServer().overworld().getGameTime();
    }

    /** Jugador transformado (Growing), con las manos vacias y el cinturon puesto. */
    public static boolean isEligible(ServerPlayer player) {
        return TransformationHelper.isTransformed(player)
                && player.hasData(ModAttachments.RIDER_STATE)
                && player.getData(ModAttachments.RIDER_STATE).getForm() == 1
                && player.getMainHandItem().isEmpty()
                && player.getOffhandItem().isEmpty()
                && TransformationHelper.isBeltEquipped(player);
    }

    private static final class State {
        private final long startTime;
        private final double baseY;
        private int targetId;
        private boolean hitDone;

        private State(long startTime, int targetId, double baseY) {
            this.startTime = startTime;
            this.targetId = targetId;
            this.baseY = baseY;
        }
    }
}
