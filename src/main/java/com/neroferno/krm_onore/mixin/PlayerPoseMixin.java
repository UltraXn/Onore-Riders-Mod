package com.neroferno.krm_onore.mixin;

import com.neroferno.krm_onore.combat.GrabbedPlayers;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Especial 1: un jugador agarrado se queda tumbado; vanilla recalcula su pose cada tick y lo levantaria. */
@Mixin(Player.class)
public abstract class PlayerPoseMixin {
    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void krm_keepGrabbedPose(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (GrabbedPlayers.isGrabbed(player.getUUID())) {
            player.setPose(Pose.SLEEPING);
            ci.cancel();
        }
    }
}
