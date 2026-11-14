package com.neroferno.krm_onore.combat;

import com.neroferno.krm_onore.KRMRevoMod;
import com.neroferno.krm_onore.attachment.ModAttachments;
import com.neroferno.krm_onore.network.GrowingBlockAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Bloqueo rapido al retroceder: mientras el jugador mantiene la S y recibe un golpe de frente, hace la pose de
 * "block" (se queda en 0.25 s de la animacion) y recibe menos dano. Si no recibe dano, solo retrocede normal.
 * El cliente avisa con BackwardInputPacket cuando empieza/deja de retroceder.
 */
@EventBusSubscriber(modid = KRMRevoMod.MODID)
public final class BackwardBlockCombat {
    /** Fraccion del dano que se recibe al bloquear (0.4 = 60% menos de dano). */
    private static final float DAMAGE_MULTIPLIER = 0.4F;
    /** Cuanto dura la pose de bloqueo tras un golpe (ticks). Cada golpe recibido en ese lapso la renueva. */
    private static final int HOLD_TICKS = 10;

    /** Cuanta velocidad pierdes mientras bloqueas (0.35 = 35% mas lento). */
    private static final double SLOW_AMOUNT = 0.35D;
    /**
     * Coseno minimo entre tu mirada y la direccion del golpe para que bloquee. 0.0 = frente y diagonales de
     * delante (hasta 90 grados a cada lado); un golpe por la espalda nunca se bloquea.
     */
    private static final double MIN_FRONT_COS = 0.0D;
    private static final ResourceLocation SLOW_ID =
            ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "backward_block_slow");

    private static final Set<UUID> BACKING = new HashSet<>();
    private static final Map<UUID, Long> BLOCK_UNTIL = new HashMap<>();

    private BackwardBlockCombat() {}

    /** true mientras la pose de bloqueo esta activa (un golpe recibido de frente la mantiene 10 ticks). */
    public static boolean isBlocking(ServerPlayer player) {
        Long until = BLOCK_UNTIL.get(player.getUUID());
        return until != null && player.serverLevel().getServer().overworld().getGameTime() < until;
    }

    public static void setBacking(ServerPlayer player, boolean backing) {
        if (backing) {
            BACKING.add(player.getUUID());
            return;
        }
        BACKING.remove(player.getUUID());
        // Dejo de retroceder: se suelta la pose al instante.
        if (BLOCK_UNTIL.remove(player.getUUID()) != null) {
            setSlow(player, false);
            send(player, 0);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !BACKING.contains(player.getUUID())
                || !Especial3Combat.isEligible(player)
                || Especial3Combat.isActive(player)
                || Especial2Combat.isActive(player)
                || Especial1Combat.isActive(player)
                // Bloquear y pegar no se puede a la vez: mientras haces un golpe no bloqueas.
                || RiderFistCombat.isAttacking(player)
                || player.getData(ModAttachments.RIDER_STATE).getRiderKickState() > 0) {
            return;
        }

        DamageSource source = event.getSource();
        // Solo golpes con un causante (mobs, jugadores, proyectiles, explosiones), no caida/fuego/vacio.
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || (source.getEntity() == null && source.getDirectEntity() == null)
                || !isInFront(player, source)) {
            return;
        }

        long now = player.serverLevel().getServer().overworld().getGameTime();
        event.setAmount(event.getAmount() * DAMAGE_MULTIPLIER);
        BLOCK_UNTIL.put(player.getUUID(), now + HOLD_TICKS);
        setSlow(player, true);
        send(player, HOLD_TICKS);
    }

    /** Solo bloquea lo que viene de frente o en diagonal por delante; si te pegan por la espalda no sirve. */
    private static boolean isInFront(ServerPlayer player, DamageSource source) {
        Vec3 from = source.getSourcePosition();
        if (from == null) {
            return false; // sin origen conocido no se puede saber si viene de frente
        }
        Vec3 look = player.getLookAngle();
        double lookLen = Math.sqrt(look.x * look.x + look.z * look.z);
        double dx = from.x - player.getX();
        double dz = from.z - player.getZ();
        double dirLen = Math.sqrt(dx * dx + dz * dz);
        if (lookLen < 1.0E-4D || dirLen < 1.0E-4D) {
            return true; // mirando hacia arriba/abajo o golpe desde encima: se considera de frente
        }
        return (look.x * dx + look.z * dz) / (lookLen * dirLen) >= MIN_FRONT_COS;
    }

    /** Lentitud mientras dura la pose de bloqueo (modificador transitorio de velocidad de movimiento). */
    private static void setSlow(ServerPlayer player, boolean slowed) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        if (slowed) {
            if (speed.getModifier(SLOW_ID) == null) {
                speed.addTransientModifier(new AttributeModifier(
                        SLOW_ID, -SLOW_AMOUNT, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        } else {
            speed.removeModifier(SLOW_ID);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (BLOCK_UNTIL.isEmpty()) {
            return;
        }
        long now = event.getServer().overworld().getGameTime();
        var iterator = BLOCK_UNTIL.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (now < entry.getValue()) {
                continue;
            }
            iterator.remove();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                setSlow(player, false);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        BACKING.remove(event.getEntity().getUUID());
        if (BLOCK_UNTIL.remove(event.getEntity().getUUID()) != null && event.getEntity() instanceof ServerPlayer player) {
            setSlow(player, false);
        }
    }

    private static void send(ServerPlayer player, int holdTicks) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player, new GrowingBlockAnimationPacket(player.getId(), holdTicks));
    }
}
