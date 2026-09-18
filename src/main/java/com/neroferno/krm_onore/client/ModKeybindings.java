package com.neroferno.krm_onore.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * All keybindings for Onore Rider.
 * Registered in KRMRevoModClient via the RegisterKeyMappingsEvent.
 */
public class ModKeybindings {

    public static final String CATEGORY = "key.categories.krm_revo";

    /**
     * The main transformation toggle key â€” default K.
     * Only active while in-game (KeyConflictContext.IN_GAME).
     */
    public static final KeyMapping TRANSFORM_KEY = new KeyMapping(
            "key.krm_revo.transform",          // translation key
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,                   // default: K
            CATEGORY
    );

    /**
     * Secondary ability key â€” default V.
     * Used for attacks like Rider Kick.
     */
    public static final KeyMapping ABILITY_KEY = new KeyMapping(
            "key.krm_revo.ability",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,                   // default: V
            CATEGORY
    );

    /**
     * Open the Skill Tree / Belt GUI â€” default B.
     */
    public static final KeyMapping SKILL_TREE_KEY = new KeyMapping(
            "key.krm_revo.skill_tree",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,                   // default: B
            CATEGORY
    );
}
