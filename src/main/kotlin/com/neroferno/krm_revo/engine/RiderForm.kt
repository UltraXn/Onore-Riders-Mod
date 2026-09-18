package com.neroferno.krm_revo.engine

import net.minecraft.resources.ResourceLocation

/**
 * Attributes and combat modifiers granted by a Rider Form.
 */
data class FormStats(
    val maxHpBonus: Double = 0.0,
    val attackDamage: Double = 1.0,
    val armor: Double = 0.0,
    val armorToughness: Double = 0.0,
    val movementSpeedMultiplier: Double = 1.0,
    val jumpBoostLevel: Int = 0,
    val resistanceLevel: Int = 0
)

/**
 * Represents a specific form/mode of a Kamen Rider (e.g. Kuuga Mighty, Faiz Axel, Build Hazard).
 */
data class RiderForm(
    val id: Int,
    val nameKey: String,
    val stats: FormStats = FormStats(),
    val colorHex: Int = 0xFFFFFF,
    val modelLocation: ResourceLocation? = null,
    val textureLocation: ResourceLocation? = null,
    val finishers: List<FinisherDefinition> = emptyList()
)
