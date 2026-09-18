package com.neroferno.krm_onore.engine

import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent

/**
 * Categorization for Rider Finishers.
 */
enum class FinisherType {
    RIDER_KICK,
    RIDER_PUNCH,
    WEAPON_SLASH,
    SPECIAL_SHOT
}

/**
 * Defines a finisher sequence for a Rider form.
 */
data class FinisherDefinition(
    val id: String,
    val type: FinisherType = FinisherType.RIDER_KICK,
    val animationId: ResourceLocation,
    val baseDamage: Float = 20.0f,
    val causesExplosion: Boolean = true,
    val explosionRadius: Float = 2.5f,
    val voiceSound: SoundEvent? = null,
    val impactSound: SoundEvent? = null
)
