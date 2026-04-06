package com.ripplehealthcare.bproboard.domain.model

import java.util.Date

data class ColorSorterResult(
    val testId: String = "",
    val sessionId: String = "",
    val patientId: String = "",
    val score: Int = 0,
    val missedCount: Int = 0,
    val redCollected: Int = 0,
    val greenCollected: Int = 0,
    val timestamp: Date = Date()
)