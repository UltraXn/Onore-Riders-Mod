package com.neroferno.krm_onore.combat;

import com.neroferno.krm_onore.KRMRevoMod;
import com.neroferno.krm_onore.attachment.ModAttachments;
import com.neroferno.krm_onore.network.AbilityHelper;
import com.neroferno.krm_onore.network.GrowingSpecialAnimationPacket;
import com.neroferno.krm_onore.network.SyncVelocityPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Especial 1 (doble Shift con un enemigo al frente, solo sin arma equipada): llave de suelo.
 *
 * <p>1) especial1_1: el jugador se lanza hacia el enemigo (findTarget) manteniendo la pose de 0.25 s (las piernas no
 * tienen keys, asi que caminan normal). A 1 bloque lo agarra con la mano izquierda (locator de armorLeftForearm),
 * sigue corriendo 1.5 bloques con el objetivo, salta y lo coloca delante al aterrizar.
 * <p>2) especial1_2 (0 -> 1.17 s): de 0.21 a 0.50 s el enemigo queda tumbado en el suelo (pose de dormir) y de 0.50 a
 * 1.17 s llega el primer golpe. Luego se repite en bucle 4 veces el tramo 1.17 -> 1.67 s (especial1_loop) y cierra
 * con especial1_end (1.67 -> 2.0 s). Cada golpe es el punto donde el puno llega al maximo (1.17 s y 1.67 s).
 * <p>Se interrumpe con un salto manual o si en total te quitan 2 corazones (4 de dano) mientras dura.
 *
 * <p>Las animaciones especial1_* del traje/cinturon son tramos de la especial1_2 original (el bucle necesita
 * poder repetirse), ver kuuga_suit.animation.json.
 */
@EventBusSubscriber(modid = KRMRevoMod.MODID)
public final class Especial1Combat {
    // --- Busqueda y acercamiento ---
    private static final double APPROACH_RANGE = 14.0D;       // alcance de findTarget al activar
    private static final double GRAB_DISTANCE = 1.0D;         // a esta distancia (hasta la caja del enemigo) lo agarra
    private static final double APPROACH_SPEED = 0.5D;        // bloques por tick: se ve correr (un sprint normal es ~0.28)
    /** Minimo de ticks en la pose de 0.25 s corriendo antes de agarrar, aunque el enemigo ya este pegado. */
    private static final int APPROACH_MIN_TICKS = 6;
    /** Da tiempo a recorrer la distancia completa desde el alcance maximo de 14 bloques. */
    private static final int APPROACH_TIMEOUT_TICKS = 35;

    // --- Agarre: posicion aproximada de la mano izquierda (locator de armorLeftForearm). Calibrable. ---
    private static final double HAND_FORWARD = 0.7D;
    private static final double HAND_SIDE = 0.3D;
    private static final double HAND_HEIGHT = 1.2D;           // altura de la mano; el enemigo queda centrado ahi
    private static final double CARRY_RUN_DISTANCE = 1.5D;    // sigue corriendo esta distancia despues de agarrarlo
    private static final int CARRY_RUN_TIMEOUT_TICKS = 8;     // limite de seguridad para la carrera agarrando
    private static final double CARRY_DISTANCE = 1.5D;        // donde lo coloca de pie al aterrizar, delante del jugador
    /** Salto al agarrar: ~1.5 bloques de alto. */
    private static final double JUMP_VELOCITY = 0.49D;
    /** Avance horizontal del jugador durante el salto (bloques por tick): arrastra al enemigo un poco. */
    private static final double DRIFT_SPEED = 0.05D;
    private static final int CARRY_MIN_TICKS = 5;              // no puede aterrizar antes de esto
    private static final int CARRY_MAX_TICKS = 14;             // tope por si el aterrizaje no se detecta
    // --- Tiempos de especial1_2 (segundos -> ticks) ---
    private static final int LAY_TICK = ticks(0.21D);          // 4: el enemigo empieza a tumbarse
    private static final int LAY_END_TICK = ticks(0.5D);       // 10: ya esta contra el suelo y empieza el golpe
    private static final int FIRST_HIT_TICK = ticks(1.1667D);  // 23: primer golpe (fin de especial1_2)
    private static final int LOOP_TICKS = ticks(0.5D);         // 10: duracion de un ciclo del bucle (1.17 -> 1.67 s)
    private static final int LOOP_REPEATS = 4;
    private static final int END_TICKS = ticks(0.3333D);       // 7: especial1_end

    // --- Dano ---
    private static final float HIT_DAMAGE = 3.0F;              // por golpe (5 golpes en total)
    /** Dano total recibido (corazones * 2) que corta la llave: 2 corazones. */
    private static final float INTERRUPT_DAMAGE = 4.0F;

    // --- Coste y cooldown (el cooldown cuenta desde que termina o se corta) ---
    private static final float ENERGY_COST = 10.0F;
    private static final int COOLDOWN_TICKS = 160;             // 8 s

    private static final Map<UUID, State> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWN_END = new HashMap<>();

    private Especial1Combat() {}

    private static int ticks(double seconds) {
        return (int) Math.round(seconds * 20.0D);
    }

    /** Ticks que el cliente bloquea golpes y movimiento tras recibir cada fase (cada fase renueva el plazo). */
    public static int clientLockTicks(String animation) {
        return switch (animation) {
            case "especial1_1" -> APPROACH_TIMEOUT_TICKS + CARRY_RUN_TIMEOUT_TICKS + CARRY_MAX_TICKS + 4;
            case "especial1_2" -> FIRST_HIT_TICK + LOOP_TICKS * LOOP_REPEATS + END_TICKS + 4;
            case "especial1_loop" -> LOOP_TICKS * LOOP_REPEATS + END_TICKS + 4;
            case "especial1_end" -> END_TICKS + 2;
            default -> 0;
        };
    }

    public static boolean isActive(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    public static void cancel(ServerPlayer player) {
        State state = ACTIVE.get(player.getUUID());
        if (state != null) {
            state.cancelled = true;
        }
    }

    public static void start(ServerPlayer player) {
        if (!player.isAlive() || !Especial3Combat.isEligible(player) || ACTIVE.containsKey(player.getUUID())
                || Especial2Combat.isActive(player) || Especial3Combat.isActive(player)
                || player.getData(ModAttachments.RIDER_STATE).getRiderKickState() > 0) {
            return;
        }

        // Doble Shift es un gesto comun (agacharse en bordes): sin enemigo al frente no hace nada ni avisa.
        LivingEntity target = AbilityHelper.findTarget(player, APPROACH_RANGE);
        if (target == null || !isGrabbable(target)) {
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
                    Component.translatable("chat.krm_revo.especial1_no_energy")
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

        Vec3 releaseAnchor = findSupportedPosition(player, player.position(), 0.75D);
        if (releaseAnchor == null) {
            releaseAnchor = findSupportedPosition(player, target.position(), 0.75D);
        }
        State state = new State(now, target.getId(), releaseAnchor != null ? releaseAnchor : player.position());
        ACTIVE.put(player.getUUID(), state);
        disableAi(target, state);
        RiderFistCombat.clearBufferedAttack(player);
        sendAnimation(player, "especial1_1");
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WIND_CHARGE_THROW, SoundSource.PLAYERS, 1.2F, 0.8F);
    }

    /**
     * Se puede agarrar a jugadores (en supervivencia/aventura) y a casi cualquier mob. No: Ender Dragon, Wither,
     * slimes (y magma cubes) ni animales pacificos (vacas, cerdos, peces, murcielagos...). Los animales neutrales
     * (lobos, abejas, osos polares) si cuentan: se defienden.
     */
    private static boolean isGrabbable(LivingEntity target) {
        if (target instanceof EnderDragon || target instanceof WitherBoss || target instanceof Slime) {
            return false;
        }
        if (target instanceof Player) {
            return target instanceof ServerPlayer other && !other.isCreative() && !other.isSpectator();
        }
        if (target instanceof NeutralMob) {
            return true;
        }
        return !(target instanceof Animal || target instanceof WaterAnimal || target instanceof AmbientCreature);
    }

    /** Suma el dano real recibido (ya con la absorcion de energia); a 2 corazones se corta la llave. */
    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            State state = ACTIVE.get(player.getUUID());
            if (state != null) {
                state.damageTaken += event.getNewDamage();
                if (state.damageTaken >= INTERRUPT_DAMAGE) {
                    state.cancelled = true;
                }
            }
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

            LivingEntity target = currentTarget(player, state);
            if (state.cancelled || !player.isAlive() || !Especial3Combat.isEligible(player)
                    || (target == null && state.phase == Phase.APPROACH)) {
                release(player, state, target);
                if (player.isAlive()) {
                    sendAnimation(player, "especial1_cancel");
                }
                COOLDOWN_END.put(player.getUUID(), now + COOLDOWN_TICKS);
                iterator.remove();
                continue;
            }

            if (target != null) {
                suppressAi(target);
            }
            boolean done = switch (state.phase) {
                case APPROACH -> tickApproach(player, state, target, now);
                case CARRY_RUN -> tickCarryRun(player, state, target, now);
                case CARRY_JUMP -> tickCarryJump(player, state, target, now);
                case GROUND -> tickGround(player, state, target, now);
            };
            if (done) {
                release(player, state, target);
                COOLDOWN_END.put(player.getUUID(), now + COOLDOWN_TICKS);
                iterator.remove();
            }
        }
    }

    /** @return true si hay que terminar (sin animacion de cancelar). */
    private static boolean tickApproach(ServerPlayer player, State state, LivingEntity target, long now) {
        long t = now - state.startTime;
        double distance = horizontalDistance(player, target);
        boolean closeEnough = distance <= GRAB_DISTANCE;

        if (closeEnough && t >= APPROACH_MIN_TICKS) {
            beginCarry(player, state, target, now);
            return false;
        }
        if (t > APPROACH_TIMEOUT_TICKS) {
            state.cancelled = true;   // no llego: se corta en el siguiente tick con animacion de cancelar
            return false;
        }

        // Carrera hacia el enemigo. Si ya esta pegado, espera en la pose hasta completar sus 0.25 s.
        Vec3 flat = new Vec3(target.getX() - player.getX(), 0.0D, target.getZ() - player.getZ());
        Vec3 direction = Vec3.ZERO;
        if (!closeEnough && flat.lengthSqr() > 1.0E-6D) {
            double speed = Math.min(APPROACH_SPEED, Math.max(0.1D, distance - GRAB_DISTANCE + 0.1D));
            direction = flat.normalize().scale(speed);
        }
        // Carrera: velocidad horizontal hacia el enemigo; la vertical se deja como la conoce el servidor.
        pushVelocity(player, direction.x, player.getDeltaMovement().y, direction.z);
        return false;
    }

    private static void beginCarry(ServerPlayer player, State state, LivingEntity target, long now) {
        state.phase = Phase.CARRY_RUN;
        state.phaseStart = now;
        state.yaw = player.getYRot();
        state.carryRunStart = player.position();
        Vec3 forward = forward(state.yaw);
        state.handLift = Math.max(0.0D, HAND_HEIGHT - target.getBbHeight() * 0.5D);

        // Al agarrarlo se cancela el impulso de la colision y la carrera continua con el enemigo en la mano.
        pushVelocity(player, forward.x * APPROACH_SPEED, 0.0D, forward.z * APPROACH_SPEED);
        lock(target, handPosition(player, state), state.yaw);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    private static boolean tickCarryRun(ServerPlayer player, State state, LivingEntity target, long now) {
        if (target == null) {
            state.cancelled = true;
            return false;
        }

        Vec3 forward = forward(state.yaw);
        double traveled = player.position().subtract(state.carryRunStart).dot(forward);
        if (traveled >= CARRY_RUN_DISTANCE || now - state.phaseStart >= CARRY_RUN_TIMEOUT_TICKS) {
            beginCarryJump(player, state, now);
            lock(target, handPosition(player, state), state.yaw);
            return false;
        }

        pushVelocity(player, forward.x * APPROACH_SPEED, player.getDeltaMovement().y, forward.z * APPROACH_SPEED);
        lock(target, handPosition(player, state), state.yaw);
        return false;
    }

    private static void beginCarryJump(ServerPlayer player, State state, long now) {
        state.phase = Phase.CARRY_JUMP;
        state.phaseStart = now;
        Vec3 forward = forward(state.yaw);
        state.simVy = JUMP_VELOCITY;
        pushVelocity(player, forward.x * DRIFT_SPEED, state.simVy, forward.z * DRIFT_SPEED);
    }

    /** Salto con el enemigo en la mano izquierda: va pegado a la mano todo el salto, subiendo y bajando con el jugador. */
    private static boolean tickCarryJump(ServerPlayer player, State state, LivingEntity target, long now) {
        long c = now - state.phaseStart;
        if (target == null) {
            state.cancelled = true;
            return false;
        }

        Vec3 forward = forward(state.yaw);
        boolean landed = c >= CARRY_MIN_TICKS && player.onGround();
        if (landed || c >= CARRY_MAX_TICKS) {
            // Aterriza, coloca al objetivo directamente delante y empieza la fase de suelo sin arrastrarlo.
            pushVelocity(player, 0.0D, 0.0D, 0.0D);
            planPositions(player, state, target);
            lock(target, state.landPos, state.yaw);
            state.phase = Phase.GROUND;
            state.phaseStart = now;
            sendAnimation(player, "especial1_2");
            return false;
        }

        // Vertical simulada (gravedad vanilla): el servidor no conoce la velocidad vertical del cliente.
        state.simVy = (state.simVy - 0.08D) * 0.98D;
        pushVelocity(player, forward.x * DRIFT_SPEED, state.simVy, forward.z * DRIFT_SPEED);
        lock(target, handPosition(player, state), state.yaw);
        return false;
    }

    private static Vec3 handPosition(ServerPlayer player, State state) {
        Vec3 forward = forward(state.yaw);
        Vec3 left = new Vec3(forward.z, 0.0D, -forward.x);
        // El objetivo se fija antes de que el siguiente movimiento del jugador se aplique en el tick.
        Vec3 playerPos = player.position().add(player.getDeltaMovement());
        return new Vec3(
                playerPos.x + forward.x * HAND_FORWARD + left.x * HAND_SIDE,
                playerPos.y + state.handLift,
                playerPos.z + forward.z * HAND_FORWARD + left.z * HAND_SIDE);
    }

    /**
     * Posiciones del enemigo a partir de donde aterrizo el jugador: de pie delante (landPos) y tumbado debajo del
     * centro del jugador (lyingPos), orientado hacia el jugador.
     */
    private static void planPositions(ServerPlayer player, State state, LivingEntity target) {
        Vec3 forward = forward(state.yaw);
        Vec3 ref = player.position();
        state.landPos = new Vec3(ref.x + forward.x * CARRY_DISTANCE, ref.y, ref.z + forward.z * CARRY_DISTANCE);

        state.lyingPos = new Vec3(
                ref.x,
                ref.y + target.getBbWidth() * 0.5D,    // el cuerpo gira sobre sus pies: sube medio ancho para no hundirse
                ref.z);
        state.lyingYaw = -90.0F - state.yaw;
    }

    private static boolean tickGround(ServerPlayer player, State state, LivingEntity target, long now) {
        long g = now - state.phaseStart;

        if (state.endAt >= 0L) {
            if (target != null) {
                lock(target, state.lyingPos, state.lyingYaw);
            }
            return now >= state.endAt;
        }

        if (target == null) {
            // El enemigo murio: cierra con la animacion final.
            startEnd(player, state, now);
            return false;
        }

        if (g >= LAY_TICK) {
            // 0.21 -> 0.50 s: se tumba y se desliza hasta quedar contra el suelo delante del jugador.
            double progress = Math.min(1.0D, (g - LAY_TICK + 1) / (double) (LAY_END_TICK - LAY_TICK));
            setLying(target, true);
            lock(target, state.landPos.lerp(state.lyingPos, progress), state.lyingYaw);
        } else {
            lock(target, state.landPos, state.yaw);
        }

        long sinceFirstHit = g - FIRST_HIT_TICK;
        if (sinceFirstHit >= 0L && sinceFirstHit % LOOP_TICKS == 0L) {
            int index = (int) (sinceFirstHit / LOOP_TICKS);   // 0 = golpe de especial1_2, 1..4 = fin de cada vuelta
            if (index <= LOOP_REPEATS) {
                hit(player, target);
                lock(target, state.lyingPos, state.lyingYaw);
            }
            if (index == 0) {
                sendAnimation(player, "especial1_loop");
            }
            if (index == LOOP_REPEATS) {
                startEnd(player, state, now);
            }
        }
        return false;
    }

    private static void startEnd(ServerPlayer player, State state, long now) {
        state.endAt = now + END_TICKS;
        sendAnimation(player, "especial1_end");
    }

    /**
     * Tumba o levanta al enemigo. Los jugadores recalculan su pose cada tick, asi que se marcan en GrabbedPlayers
     * (cliente y servidor) y PlayerPoseMixin deja de recalcularla mientras esten agarrados.
     */
    private static void setLying(LivingEntity target, boolean lying) {
        Pose wanted = lying ? Pose.SLEEPING : Pose.STANDING;
        if (target instanceof ServerPlayer victim) {
            boolean changed = lying ? GrabbedPlayers.add(victim.getUUID()) : GrabbedPlayers.remove(victim.getUUID());
            if (changed) {
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                        victim, new com.neroferno.krm_onore.network.GrabbedPoseSyncPacket(victim.getId(), lying));
            }
        }
        if (target.getPose() != wanted && (lying || target.getPose() == Pose.SLEEPING)) {
            target.setPose(wanted);
            target.refreshDimensions();
        }
    }

    private static void hit(ServerPlayer player, LivingEntity target) {
        target.invulnerableTime = 0;
        // Sin empuje: se anula el retroceso del golpe (resistencia total solo durante el golpe) y se deja el
        // movimiento en cero para que el servidor no mande ningun impulso al cliente.
        var resistance = target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
        double previous = resistance != null ? resistance.getBaseValue() : 0.0D;
        if (resistance != null) {
            resistance.setBaseValue(1.0D);
        }
        try {
            RiderFistCombat.dealSpecialHit(player, target, HIT_DAMAGE, 0.0F, player.getViewVector(1.0F));
        } finally {
            if (resistance != null) {
                resistance.setBaseValue(previous);
            }
        }
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = false;
        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.9F);
    }

    /** Mantiene al enemigo clavado en la posicion y orientacion indicadas. */
    private static void lock(LivingEntity target, Vec3 pos, float yaw) {
        if (target instanceof ServerPlayer victim && victim.connection != null
                && victim.position().distanceToSqr(pos) > 0.0025D) {
            // A un jugador no se le mueve con moveTo: hay que teletransportarlo por su conexion.
            victim.connection.teleport(pos.x, pos.y, pos.z, yaw, 0.0F);
        }
        target.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        target.setYBodyRot(yaw);
        target.setYHeadRot(yaw);
        target.setDeltaMovement(Vec3.ZERO);
        target.fallDistance = 0.0F;
        target.hurtMarked = true;
        // Sin colision de empuje: el enemigo tumbado solapa al jugador y lo desplazaba al golpear.
        target.noPhysics = true;
    }

    /** Devuelve al enemigo a la normalidad: de pie, con su IA (si la tenia). */
    private static void release(ServerPlayer player, State state, LivingEntity target) {
        if (target == null) {
            return;
        }
        Vec3 preferredPos = state.phase == Phase.GROUND ? state.landPos : target.position();
        target.noPhysics = false;
        setLying(target, false);
        Vec3 releasePos = findSupportedPosition(player, preferredPos, 0.75D);
        if (releasePos == null) {
            releasePos = state.releaseAnchor;
        }
        moveTo(target, releasePos, state.yaw);
        if (state.aiDisabled && target instanceof Mob mob) {
            mob.setNoAi(false);
        }
        target.setDeltaMovement(Vec3.ZERO);
    }

    private static Vec3 findSupportedPosition(ServerPlayer player, Vec3 candidate, double maxDrop) {
        Vec3 start = candidate.add(0.0D, 0.25D, 0.0D);
        Vec3 end = candidate.add(0.0D, -maxDrop, 0.0D);
        BlockHitResult hit = player.level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
        ));
        if (hit.getType() != HitResult.Type.BLOCK || candidate.y - hit.getLocation().y > maxDrop) {
            return null;
        }
        return new Vec3(candidate.x, hit.getLocation().y, candidate.z);
    }

    private static void moveTo(LivingEntity target, Vec3 pos, float yaw) {
        if (target instanceof ServerPlayer victim && victim.connection != null
                && victim.position().distanceToSqr(pos) > 0.0025D) {
            victim.connection.teleport(pos.x, pos.y, pos.z, yaw, 0.0F);
        }
        target.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        target.setYBodyRot(yaw);
        target.setYHeadRot(yaw);
        target.fallDistance = 0.0F;
    }

    private static LivingEntity currentTarget(ServerPlayer player, State state) {
        Entity entity = player.level().getEntity(state.targetId);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private static double horizontalDistance(ServerPlayer player, LivingEntity target) {
        AABB box = target.getBoundingBox();
        double dx = Math.max(Math.max(box.minX - player.getX(), 0.0D), player.getX() - box.maxX);
        double dz = Math.max(Math.max(box.minZ - player.getZ(), 0.0D), player.getZ() - box.maxZ);
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static Vec3 forward(float yawDegrees) {
        double rad = Math.toRadians(yawDegrees);
        return new Vec3(-Math.sin(rad), 0.0D, Math.cos(rad));
    }

    private static void pushVelocity(ServerPlayer player, double x, double y, double z) {
        player.setDeltaMovement(x, y, z);
        player.hurtMarked = true;
        PacketDistributor.sendToPlayer(player, new SyncVelocityPacket(x, y, z));
    }

    /** El enemigo agarrado (o en camino de serlo) no puede actuar: sin IA, nada de ataques ni explosiones. */
    private static void disableAi(LivingEntity target, State state) {
        if (target instanceof Mob mob && !mob.isNoAi()) {
            mob.setNoAi(true);
            mob.getNavigation().stop();
            mob.setTarget(null);
            state.aiDisabled = true;
        }
        suppressAi(target);
    }

    /** El creeper hincha y explota en su tick, no en su IA: hay que forzar que se desinfle cada tick. */
    private static void suppressAi(LivingEntity target) {
        if (target instanceof Creeper creeper) {
            creeper.setSwellDir(-1);
        }
    }

    private static void sendAnimation(ServerPlayer player, String animation) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player, new GrowingSpecialAnimationPacket(player.getId(), animation));
    }

    private static long gameTime(ServerPlayer player) {
        return player.serverLevel().getServer().overworld().getGameTime();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            COOLDOWN_END.remove(player.getUUID());
            GrabbedPlayers.remove(player.getUUID());
            State state = ACTIVE.remove(player.getUUID());
            if (state != null) {
                release(player, state, currentTarget(player, state));
            }
        }
    }

    /** NoAi y la pose se guardan en el NBT del mob: si el servidor cierra a mitad de la llave no deben quedar. */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MinecraftServer server = event.getServer();
        for (Map.Entry<UUID, State> entry : ACTIVE.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                release(player, entry.getValue(), currentTarget(player, entry.getValue()));
            }
        }
        ACTIVE.clear();
        COOLDOWN_END.clear();
        GrabbedPlayers.clear();
    }

    private enum Phase { APPROACH, CARRY_RUN, CARRY_JUMP, GROUND }

    private static final class State {
        private final long startTime;
        private final int targetId;
        private final Vec3 releaseAnchor;
        private Phase phase = Phase.APPROACH;
        private long phaseStart;
        private long endAt = -1L;
        private float yaw;
        private double handLift;
        private Vec3 carryRunStart = Vec3.ZERO;
        private Vec3 landPos = Vec3.ZERO;
        private boolean aiDisabled;
        private double simVy;
        private Vec3 lyingPos = Vec3.ZERO;
        private float lyingYaw;
        private float damageTaken;
        private boolean cancelled;

        private State(long startTime, int targetId, Vec3 releaseAnchor) {
            this.startTime = startTime;
            this.targetId = targetId;
            this.releaseAnchor = releaseAnchor;
        }
    }
}
