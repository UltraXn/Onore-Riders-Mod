package com.neroferno.krm_onore.combat;

/**
 * Estado de "poca vida" del Rider. Lo comparten el servidor (drenaje de energia) y el cliente (parpadeo de la barra),
 * asi los dos calculan lo mismo. Se activa con 1 corazon (o menos) de vida y tiene histeresis: no se desactiva
 * hasta recuperar la mitad de la vida maxima, para que no parpadee al rozar el limite.
 */
public final class RiderLowHealth {
    /** Vida (en puntos; 2.0 = 1 corazon) a la que, o por debajo, se activa la poca vida. */
    public static final float ENTER_HEALTH = 2.0F;
    /** Fraccion de vida maxima a la que se recupera la normalidad. */
    public static final float EXIT_FRACTION = 0.5F;
    /** Energia que se drena cada segundo mientras dura la poca vida (se suma a la que gastan los golpes). */
    public static final float ENERGY_DRAIN_PER_SECOND = 1.0F;

    private RiderLowHealth() {}

    public static boolean next(boolean currentlyLow, float health, float maxHealth) {
        if (maxHealth <= 0.0F) {
            return false;
        }
        if (!currentlyLow) {
            return health <= ENTER_HEALTH;
        }
        // Con vidas maximas muy bajas la mitad podria quedar por debajo del corazon de entrada.
        float exitHealth = Math.max(maxHealth * EXIT_FRACTION, ENTER_HEALTH + 1.0F);
        return health < exitHealth;
    }
}
