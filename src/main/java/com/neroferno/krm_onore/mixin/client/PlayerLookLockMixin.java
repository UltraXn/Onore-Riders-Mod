package com.neroferno.krm_onore.mixin.client;

import com.neroferno.krm_onore.client.GrowingFistInputHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Especial 1: mientras dura la llave el jugador no puede girar la camara (el enemigo queda siempre delante). */
@Mixin(Entity.class)
public abstract class PlayerLookLockMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void krm_lockLookDuringGrab(double yRot, double xRot, CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer player && GrowingFistInputHandler.isRooted(player)) {
            ci.cancel();
        }
    }
}
