package com.neroferno.krm_onore.combat;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Jugadores que el Especial 1 tiene tumbados. Existe en cliente y en servidor (el servidor lo llena y avisa con
 * GrabbedPoseSyncPacket): PlayerPoseMixin lo usa para que vanilla no los levante mientras estan agarrados.
 */
public final class GrabbedPlayers {
    private static final Set<UUID> IDS = ConcurrentHashMap.newKeySet();

    private GrabbedPlayers() {}

    /** @return true si cambio (no estaba marcado). */
    public static boolean add(UUID id) {
        return IDS.add(id);
    }

    /** @return true si cambio (estaba marcado). */
    public static boolean remove(UUID id) {
        return IDS.remove(id);
    }

    public static boolean isGrabbed(UUID id) {
        return !IDS.isEmpty() && IDS.contains(id);
    }

    public static void clear() {
        IDS.clear();
    }
}
