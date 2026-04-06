package com.ripplehealthcare.bproboard.domain.model

data class SensorAxisStats(
    val avg: Float = 0f,
    val min: Float = 0f,
    val max: Float = 0f
)

/**
 * Holds the stats for ALL sensors for a SINGLE repetition.
 */
data class RepetitionStats(
    val repIndex: Int = 0,         // e.g., 1, 2, 3...
    val duration: Long = 0L,       // How long this rep took in ms

    // Nested stats for each sensor
    val centerX: SensorAxisStats = SensorAxisStats(),
    val centerY: SensorAxisStats = SensorAxisStats(),
    val centerZ: SensorAxisStats = SensorAxisStats(),

    val leftX: SensorAxisStats = SensorAxisStats(),
    val leftY: SensorAxisStats = SensorAxisStats(),
    val leftZ: SensorAxisStats = SensorAxisStats(),

    val rightX: SensorAxisStats = SensorAxisStats(),
    val rightY: SensorAxisStats = SensorAxisStats(),
    val rightZ: SensorAxisStats = SensorAxisStats()
)