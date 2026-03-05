package com.neroferno.krm_revo.client.animation;

import com.neroferno.krm_revo.KRMRevoMod;
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

        if (playable != null) {
            IAnimation animation = playable.playAnimation();

            // We use a ModifierLayer to easily add/replace animations
            // In a production mod, you'd likely cache this layer per player
            ModifierLayer<IAnimation> layer = new ModifierLayer<>();
            layer.setAnimation(animation);

            // Add to stack with a specific priority (priority goes first in some versions)
            // Priority 5 allows it to override default movements but be overridden by higher-level effects
            stack.addAnimLayer(5, layer);

            KRMRevoMod.LOGGER.debug("Playing animation {} for player {}", animName, player.getName().getString());
        } else {
            KRMRevoMod.LOGGER.warn("Animation {} not found in registry!", animLoc);
        }
    }
}

