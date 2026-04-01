package com.ripplehealthcare.frst.domain.model

import java.util.Date

data class StarshipResult(
    val testId: String = "",
    val sessionId: String = "",
    val patientId: String = "",
    val score: Int = 0,
    val aliensDestroyed: Int = 0,
    val timeSurvivedMs: Long = 0L,
    val isWin: Boolean = false, // True if they survived the full 60 seconds
    val timestamp: Date = Date()
)