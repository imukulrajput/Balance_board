package com.ripplehealthcare.bproboard.domain.model

data class TugThresholds(
    val postureThreshold: Double,   // e.g. -39.5
    val movementThreshold: Double,  // e.g. 15.0
    val stepPeakThreshold: Double,  // e.g. 30.0
    val isStandingValueHigher: Boolean // True if Stand > Sit (e.g. -20 > -90)
)