package com.neroferno.krm_revo.mixin.client;

import com.neroferno.krm_revo.network.TransformationHelper;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin {
    @Inject(method = "setModelProperties", at = @At("RETURN"))
    private void krm_revo$setModelProperties(AbstractClientPlayer player, CallbackInfo ci) {
        if (TransformationHelper.isTransformed(player)) {
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
