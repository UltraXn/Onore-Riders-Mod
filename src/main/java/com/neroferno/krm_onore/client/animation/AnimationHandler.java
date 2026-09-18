package com.neroferno.krm_onore.client.animation;

import com.neroferno.krm_onore.KRMRevoMod;
import dev.kosmx.playerAnim.api.IPlayable;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Client-side handler for player animations.
 * Provides the bridge between networking packets and the PlayerAnimator library.
 */
@SuppressWarnings("null")
public class AnimationHandler {

    /**
     * Plays a specific animation on a player.
     * @param playerId The entity ID of the player.
     * @param animName The name of the animation (e.g. "transform").
     */
    public static void playAnimation(int playerId, String animName) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity entity = mc.level.getEntity(playerId);
        if (!(entity instanceof AbstractClientPlayer player)) return;

        // Get the player's animation stack
        final AnimationStack stack;
        try {
            stack = PlayerAnimationAccess.getPlayerAnimLayer(player);
        } catch (IllegalArgumentException ex) {
            KRMRevoMod.LOGGER.warn("Failed to get animation stack for player {}", player.getName().getString(), ex);
            return;
        }

        // Find the animation in the registry
        ResourceLocation animLoc = ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, animName);
        IPlayable playable = PlayerAnimationRegistry.getAnimation(animLoc);
        
        if (playable == null) {
            // Fallback for animations named using Bedrock standard "animation.mod_id.anim_name"
            animLoc = ResourceLocation.fromNamespaceAndPath(KRMRevoMod.MODID, "animation." + KRMRevoMod.MODID + "." + animName);
            playable = PlayerAnimationRegistry.getAnimation(animLoc);
        }

        if (playable != null) {
            IAnimation animation = playable.playAnimation();

            ModifierLayer<IAnimation> layer = com.neroferno.krm_onore.KRMRevoModClient.getAnimationLayer(player);
            if (layer != null) {
                layer.setAnimation(animation);
                KRMRevoMod.LOGGER.debug("Playing cached animation {} on priority 1500 layer for player {}", animName, player.getName().getString());
            } else {
                ModifierLayer<IAnimation> newLayer = new ModifierLayer<>();
                newLayer.setAnimation(animation);
                stack.addAnimLayer(1500, newLayer);
                KRMRevoMod.LOGGER.warn("Animation layer not cached for player {}, created fallback layer.", player.getName().getString());
            }
        } else {
            KRMRevoMod.LOGGER.warn("Animation {} not found in registry (also tried fully qualified keys)!", animName);
        }
    }
}

