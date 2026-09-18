package com.neroferno.krm_revo.rider.ability;

import net.minecraft.server.level.ServerPlayer;

/**
 * Contract for a Rider's combat ability (e.g. Rider Kick).
 * Each Rider in RiderRegistry lists the abilities it has access to.
 */
public interface RiderAbility {

    /** Matches the id used when the client requests an ability (network packet, keybind, etc). */
    String getId();

    /** Cooldown applied to the Driver Belt item after this ability runs, in ticks. */
    int getCooldownTicks();

    /** Server-side execution of the ability. Called only after transform + cooldown checks pass. */
    void execute(ServerPlayer player);
}