package com.neroferno.krm_onore.mixin.client;

import com.neroferno.krm_onore.attachment.ModAttachments;
import com.neroferno.krm_onore.network.TransformationHelper;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractClientPlayer.class, remap = false)
public abstract class GrowingSuitPunchAnimationMixin {
    @Inject(
            method = "playAttackAnimation(Ljava/lang/String;Lnet/bettercombat/logic/AnimatedHand;FF)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void krm_revo$playGrowingSuitPunch(
            String animationName,
            net.bettercombat.logic.AnimatedHand hand,
            float attackDuration,
            float progress,
            CallbackInfo callback
    ) {
        if (!"bettercombat:one_handed_punch".equals(animationName)) {
            return;
        }

        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        if (!TransformationHelper.isTransformed(player)
                || !player.hasData(ModAttachments.RIDER_STATE)
                || !com.neroferno.krm_onore.combat.RiderFistCombat.usesFistSystem(
                        player.getData(ModAttachments.RIDER_STATE).getForm())) {
            return;
        }

        if (!player.getMainHandItem().isEmpty()
                || !player.getOffhandItem().isEmpty()
                || !TransformationHelper.isBeltEquipped(player)) {
            return;
        }

        callback.cancel();
    }
}
