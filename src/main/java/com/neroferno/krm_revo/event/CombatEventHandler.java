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
@SuppressWarnings({"null", "deprecation"})
@EventBusSubscriber(modid = KRMRevoMod.MODID)
public class CombatEventHandler {

    /** Unarmed damage while transformed â€” Iron Sword = 6, we add rider bonus. */
    private static final float TRANSFORMED_UNARMED_DAMAGE = 8.0f;

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        DamageSource source = event.getSource();

        // Only care about player melee attacks
        if (!(source.getEntity() instanceof Player attacker)) return;

        // Player must be transformed
        if (!TransformationHelper.isTransformed(attacker)) return;

        // Attack must be unarmed (empty main hand)
        if (!attacker.getMainHandItem().isEmpty()) return;

        // Override the damage to our boosted value
        event.setNewDamage(TRANSFORMED_UNARMED_DAMAGE);

        KRMRevoMod.LOGGER.debug(
            "{} delivered a transformed unarmed strike for {} damage to {}",
            attacker.getName().getString(),
            TRANSFORMED_UNARMED_DAMAGE,
            event.getEntity().getName().getString()
        );

        Vec3 hitPos = event.getEntity().position().add(0, event.getEntity().getBbHeight() / 2.0, 0);
        Vec3 look = attacker.getViewVector(1.0f);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                attacker,
                new ImpactVFXPacket(hitPos.x, hitPos.y, hitPos.z, look.x, look.y, look.z)
        );
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide) return;

        Player player = event.getEntity();
        if (player.tickCount % 10 == 0) { // Sync twice a second
            boolean isTransformed = TransformationHelper.isTransformed(player);
            boolean hasBelt = TransformationHelper.isBeltEquipped(player);

            PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                    player,
                    new SyncRiderStatePacket(player.getId(), isTransformed, hasBelt)
            );
        }
    }
}

