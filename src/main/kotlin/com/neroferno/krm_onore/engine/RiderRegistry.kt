package com.neroferno.krm_onore.engine

import java.util.concurrent.ConcurrentHashMap

/**
 * Central registry for all Kamen Rider series and forms across eras.
 * Fully interoperable with Java and Kotlin.
 */
object RiderRegistry {
    private val riders = ConcurrentHashMap<String, RiderSeries>()

    @JvmStatic
    fun register(rider: RiderSeries) {
        riders[rider.id] = rider
    }

    @JvmStatic
    fun get(id: String): RiderSeries? = riders[id]

    @JvmStatic
    fun getAll(): Collection<RiderSeries> = riders.values

    @JvmStatic
    fun getForm(riderId: String, formId: Int): RiderForm? = riders[riderId]?.getForm(formId)

    @JvmStatic
    fun hasRider(id: String): Boolean = riders.containsKey(id)
}

/**
 * High-level DSL entrypoint to define and register a Kamen Rider.
 */
fun rider(id: String, block: RiderBuilder.() -> Unit): RiderSeries {
    val builder = RiderBuilder(id)
    builder.block()
    val series = builder.build()
    RiderRegistry.register(series)
    return series
}
