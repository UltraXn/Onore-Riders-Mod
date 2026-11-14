package com.neroferno.krm_onore.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.neroferno.krm_onore.attachment.ModAttachments;
import com.neroferno.krm_onore.client.renderer.layer.SuitRenderLayer;
import com.neroferno.krm_onore.client.renderer.SuitVisualRenderer;
import com.neroferno.krm_onore.network.TransformationHelper;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin {
    /**
     * Mientras dura un ataque Growing, el cuerpo se renderiza orientado hacia donde mira la cabeza (la camara),
     * en vez de quedarse con el giro retrasado de vanilla. Asi la animacion del traje/cinturon sale hacia
     * donde apuntas y no hacia donde el cuerpo se habia quedado.
     */
    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void krm_revo$faceLookWhileAttacking(AbstractClientPlayer player, float entityYaw, float partialTick,
                                                 PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                                 CallbackInfo ci) {
        if (TransformationHelper.isTransformed(player) && SuitRenderLayer.isFacingLook(player)) {
            player.yBodyRot = player.yHeadRot;
            player.yBodyRotO = player.yHeadRotO;
        }
    }

    @Inject(method = "setModelProperties", at = @At("RETURN"))
    private void krm_revo$setModelProperties(AbstractClientPlayer player, CallbackInfo ci) {
        if (TransformationHelper.isTransformed(player)) {
            var riderState = player.getData(ModAttachments.RIDER_STATE);
            if (SuitVisualRenderer.usesRevealAnimation(riderState.getForm())
                    && SuitVisualRenderer.getGrowingRevealElapsedTicks(player, 0.0F)
                    < SuitVisualRenderer.GROWING_REVEAL_TOTAL_TICKS) {
                return;
            }

            PlayerRenderer renderer = (PlayerRenderer) (Object) this;
            PlayerModel<AbstractClientPlayer> model = renderer.getModel();
            model.setAllVisible(false);
            model.leftSleeve.visible = false;
            model.rightSleeve.visible = false;
            model.leftPants.visible = false;
            model.rightPants.visible = false;
            model.jacket.visible = false;
        }
    }
}
