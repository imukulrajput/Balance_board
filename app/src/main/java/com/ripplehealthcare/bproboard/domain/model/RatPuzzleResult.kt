package com.ripplehealthcare.bproboard.domain.model

import java.util.Date

data class RatPuzzleResult(
    val testId: String = "",
    val sessionId: String = "",
    val patientId: String = "",
    val isWin: Boolean = false,
    val timeTakenMs: Long = 0L,
    val livesRemaining: Int = 0,
    val timestamp: Date = Date()
)