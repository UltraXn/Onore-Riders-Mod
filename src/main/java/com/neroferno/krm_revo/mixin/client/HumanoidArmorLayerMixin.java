package com.neroferno.krm_revo.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.neroferno.krm_revo.network.TransformationHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public class HumanoidArmorLayerMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void krm_onRenderArmor(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, LivingEntity entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof Player player && TransformationHelper.isTransformed(player)) {
            ci.cancel();
        }
    }
}
