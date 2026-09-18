package com.neroferno.krm_onore.engine

import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.item.Item
import java.util.function.Supplier

/**
 * Encapsulates an entire Kamen Rider series / character (e.g. Kuuga, Agito, Decade).
 */
data class RiderSeries(
    val id: String,
    val era: RiderEra,
    val driverSupplier: Supplier<out Item>?,
    val forms: Map<Int, RiderForm>,
    val defaultFormId: Int
) {
    fun getForm(id: Int): RiderForm? = forms[id]
}

/**
 * DSL Builder for registering a Kamen Rider with all forms and finishers.
 */
class RiderBuilder(val id: String) {
    var era: RiderEra = RiderEra.HEISEI_PHASE_1
    var driverSupplier: Supplier<out Item>? = null
    var defaultFormId: Int = 1
    private val forms = mutableMapOf<Int, RiderForm>()

    fun forms(block: FormsScope.() -> Unit) {
        val scope = FormsScope()
        scope.block()
        forms.putAll(scope.forms)
    }

    fun build(): RiderSeries {
        return RiderSeries(id, era, driverSupplier, forms, defaultFormId)
    }
}

class FormsScope {
    val forms = mutableMapOf<Int, RiderForm>()

    fun form(id: Int, nameKey: String, block: FormBuilder.() -> Unit = {}) {
        val builder = FormBuilder(id, nameKey)
        builder.block()
        forms[id] = builder.build()
    }
}

class FormBuilder(val id: Int, val nameKey: String) {
    var stats = FormStats()
    var colorHex = 0xFFFFFF
    var modelLocation: ResourceLocation? = null
    var textureLocation: ResourceLocation? = null
    private val finishers = mutableListOf<FinisherDefinition>()

    fun stats(
        hp: Double = 0.0,
        atk: Double = 1.0,
        def: Double = 0.0,
        toughness: Double = 0.0,
        speed: Double = 1.0,
        jump: Int = 0,
        resistance: Int = 0
    ) {
        this.stats = FormStats(hp, atk, def, toughness, speed, jump, resistance)
    }

    fun finisher(
        id: String,
        type: FinisherType = FinisherType.RIDER_KICK,
        animation: String,
        damage: Float = 20.0f,
        explosion: Boolean = true,
        explosionRadius: Float = 2.5f,
        voice: SoundEvent? = null,
        impact: SoundEvent? = null
    ) {
        finishers.add(
            FinisherDefinition(
                id = id,
                type = type,
                animationId = ResourceLocation.parse(animation),
                baseDamage = damage,
                causesExplosion = explosion,
                explosionRadius = explosionRadius,
                voiceSound = voice,
                impactSound = impact
            )
        )
    }

    fun build(): RiderForm {
        return RiderForm(id, nameKey, stats, colorHex, modelLocation, textureLocation, finishers)
    }
}
