package com.neroferno.krm_onore.seasons.heisei1

import com.neroferno.krm_onore.engine.FinisherType
import com.neroferno.krm_onore.engine.RiderEra
import com.neroferno.krm_onore.engine.RiderSeries
import com.neroferno.krm_onore.engine.rider
import com.neroferno.krm_onore.event.ModSounds
import com.neroferno.krm_onore.item.ModItems

/**
 * Definition of Kamen Rider Kuuga (First Heisei Rider - 2000).
 * Handles all canonical forms from Growing to Ultimate.
 */
object Kuuga {
    val SERIES: RiderSeries = rider("kuuga") {
        era = RiderEra.HEISEI_PHASE_1
        driverSupplier = ModItems.DRIVER_BELT
        defaultFormId = 1 // Growing Form

        forms {
            // Form 1: Growing Form (White / Unfinished)
            form(1, "form.krm_revo.kuuga_growing") {
                stats(
                    hp = 20.0,
                    atk = 6.0,
                    def = 10.0,
                    toughness = 2.0,
                    speed = 1.05
                )
                colorHex = 0xE0E0E0
            }

            // Form 2: Mighty Form (Red / Standard Balance)
            form(2, "form.krm_revo.kuuga_mighty") {
                stats(
                    hp = 30.0,
                    atk = 12.0,
                    def = 15.0,
                    toughness = 4.0,
                    speed = 1.20
                )
                colorHex = 0xCC1111

                finisher(
                    id = "mighty_kick",
                    type = FinisherType.RIDER_KICK,
                    animation = "krm_revo:kick",
                    damage = 25.0f,
                    explosion = true,
                    explosionRadius = 3.0f,
                    impact = ModSounds.TRANSFORM.get()
                )
            }

            // Form 3: Dragon Form (Blue / Agile & Speed)
            form(3, "form.krm_revo.kuuga_dragon") {
                stats(
                    hp = 25.0,
                    atk = 8.0,
                    def = 12.0,
                    toughness = 2.0,
                    speed = 1.45,
                    jump = 2
                )
                colorHex = 0x1144CC
            }

            // Form 4: Pegasus Form (Green / Sensory & Ranged)
            form(4, "form.krm_revo.kuuga_pegasus") {
                stats(
                    hp = 20.0,
                    atk = 7.0,
                    def = 10.0,
                    toughness = 1.0,
                    speed = 1.10
                )
                colorHex = 0x11AA44
            }

            // Form 5: Titan Form (Purple / Heavy Armored)
            form(5, "form.krm_revo.kuuga_titan") {
                stats(
                    hp = 45.0,
                    atk = 18.0,
                    def = 28.0,
                    toughness = 8.0,
                    speed = 0.85,
                    resistance = 1
                )
                colorHex = 0x771199
            }

            // Form 6: Amazing Mighty (Black & Gold / Enhanced Mighty)
            form(6, "form.krm_revo.kuuga_amazing_mighty") {
                stats(
                    hp = 50.0,
                    atk = 24.0,
                    def = 22.0,
                    toughness = 6.0,
                    speed = 1.30
                )
                colorHex = 0x222222

                finisher(
                    id = "amazing_rider_kick",
                    type = FinisherType.RIDER_KICK,
                    animation = "krm_revo:kick",
                    damage = 40.0f,
                    explosion = true,
                    explosionRadius = 4.5f
                )
            }

            // Form 7: Ultimate Form (Black with Red/Black Eyes / Cataclysmic)
            form(7, "form.krm_revo.kuuga_ultimate") {
                stats(
                    hp = 80.0,
                    atk = 35.0,
                    def = 35.0,
                    toughness = 12.0,
                    speed = 1.50,
                    jump = 3,
                    resistance = 2
                )
                colorHex = 0x050505

                finisher(
                    id = "ultimate_kick",
                    type = FinisherType.RIDER_KICK,
                    animation = "krm_revo:kick",
                    damage = 65.0f,
                    explosion = true,
                    explosionRadius = 6.0f
                )
            }
        }
    }

    /**
     * Touch to ensure registration when the game loads.
     */
    @JvmStatic
    fun init() {
        // Accessing SERIES initializes the object and registers with RiderRegistry
        SERIES.id
    }
}
