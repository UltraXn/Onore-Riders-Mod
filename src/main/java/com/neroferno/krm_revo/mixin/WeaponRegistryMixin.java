package com.neroferno.krm_revo.mixin;

import com.neroferno.krm_revo.KRMRevoMod;
import com.neroferno.krm_revo.attachment.ModAttachments;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.AttributesContainer;
import net.bettercombat.api.ComboState;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.PlayerAttackHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Mixin that intercepts Better Combat's attack resolution.
 * When the player is holding nothing (empty hand) AND has the Driver Belt
 * equipped in the Rider Inventory, we provide our custom "rider_fist"
 * 3-hit combo instead of returning null (no attack).
 */
@SuppressWarnings("null")
@Mixin(value = PlayerAttackHelper.class, remap = false)
public class WeaponRegistryMixin {

    private static final ResourceLocation RIDER_FIST_ID =
            ResourceLocation.fromNamespaceAndPath("bettercombat", "rider_fist");

    /**
     * Inject at RETURN of getCurrentAttack.
     * If the original method returns null (no weapon attributes found for empty hand)
     * AND the player has the belt equipped, we build an AttackHand with rider_fist attributes.
     */
    @Inject(method = "getCurrentAttack", at = @At("RETURN"), cancellable = true)
    private static void krm_onGetCurrentAttack(Player player, int comboCount,
                                                CallbackInfoReturnable<AttackHand> cir) {
        // Only intercept if the original returned null (no combo available)
        if (cir.getReturnValue() != null) return;

        // Player must be holding nothing in main hand
        if (!player.getMainHandItem().isEmpty()) return;

        // Check if the player has a Driver Belt in the Rider Inventory
        boolean hasBelt = false;
        try {
            ItemStackHandler inv = player.getData(ModAttachments.RIDER_INVENTORY);
            hasBelt = !inv.getStackInSlot(0).isEmpty();
        } catch (Exception e) {
            return;
        }

        if (!hasBelt) return;

        // Belt is equipped → look up the rider_fist preset from CONTAINERS
        // (not registrations, because registrations only contains real items)
        Map<ResourceLocation, AttributesContainer> containers = WeaponRegistryAccessor.getContainers();
        WeaponAttributes riderFist = null;

        if (containers != null) {
            AttributesContainer container = containers.get(RIDER_FIST_ID);
            if (container != null) {
                riderFist = container.attributes();
            }
        }

        if (riderFist == null || riderFist.attacks() == null || riderFist.attacks().length == 0) {
            KRMRevoMod.LOGGER.warn("[KRM] rider_fist preset not found in containers!");
            return;
        }

        // Calculate which attack in the combo we are at
        int attackCount = riderFist.attacks().length;
        int attackIndex = comboCount % attackCount;
        WeaponAttributes.Attack attack = riderFist.attacks()[attackIndex];

        ComboState comboState = new ComboState(attackIndex, attackCount);

        // Build the AttackHand with our rider_fist attributes
        AttackHand hand = new AttackHand(
                attack,
                comboState,
                false, // not offhand
                riderFist,
                player.getMainHandItem() // empty hand ItemStack
        );

        KRMRevoMod.LOGGER.debug("[KRM] Rider fist combo attack {} of {} applied!",
                attackIndex + 1, attackCount);

        cir.setReturnValue(hand);
    }
}
