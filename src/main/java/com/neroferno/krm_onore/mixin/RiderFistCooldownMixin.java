package com.neroferno.krm_onore.mixin;

import com.neroferno.krm_onore.attachment.ModAttachments;
import com.neroferno.krm_onore.combat.RiderFistTiming;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.bettercombat.logic.PlayerAttackHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerAttackHelper.class, remap = false)
public abstract class RiderFistCooldownMixin {
    @Inject(method = "getAttackCooldownTicksCapped", at = @At("RETURN"), cancellable = true)
    private static void krm_revo$setRiderFistCooldown(Player player, CallbackInfoReturnable<Float> callback) {
        if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()
                || !player.hasData(ModAttachments.RIDER_INVENTORY)) {
            return;
        }

        ItemStackHandler inventory = player.getData(ModAttachments.RIDER_INVENTORY);
        if (inventory.getStackInSlot(0).isEmpty()) {
            return;
        }

        callback.setReturnValue(Math.max(callback.getReturnValue(), RiderFistTiming.maxAnimationTicks(false)));
    }
}
