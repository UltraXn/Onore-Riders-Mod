package com.neroferno.krm_onore.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.neroferno.krm_onore.network.TransformationHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ElytraLayer.class)
public class ElytraLayerMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void krm_onRenderElytra(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, LivingEntity entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof Player player && TransformationHelper.isTransformed(player)) {
            ci.cancel();
        }
    }
}
