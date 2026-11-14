package com.neroferno.krm_onore.mixin;

import com.neroferno.krm_onore.attachment.ModAttachments;
import com.neroferno.krm_onore.network.TransformationHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityJumpMixin {
    @Inject(method = "jumpFromGround", at = @At("RETURN"))
    private void krm_adjustTransformedJump(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof Player player) || !TransformationHelper.isTransformed(player)) {
            return;
        }

        int form = player.getData(ModAttachments.RIDER_STATE).getForm();
        float targetHeight = form == 1 ? 1.4F : 1.8F;
        float targetJumpVelocity = 0.42F * (float) Math.sqrt(targetHeight / 1.25F);
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, motion.y + targetJumpVelocity - 0.42F, motion.z);
    }
}
