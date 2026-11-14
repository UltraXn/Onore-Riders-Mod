package com.neroferno.krm_onore.combat;

import net.minecraft.world.entity.player.Player;

/**
 * Regla de hambre del Rider. La comparten el servidor (destransformacion, sin regeneracion) y el cliente
 * (parpadeo de la barra de energia). "Sin muslones" = barra de comida en 0.
 */
public final class RiderStarvation {
    /** Ticks seguidos sin muslones de comida tras los cuales el Rider se destransforma (3 s). */
    public static final int STARVE_TICKS = 60;

    private RiderStarvation() {}

    public static boolean isStarving(Player player) {
        return player.getFoodData().getFoodLevel() <= 0;
    }
}
