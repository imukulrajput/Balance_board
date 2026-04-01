package com.ripplehealthcare.frst.domain.model

import java.util.Date

data class HolePuzzleResult(
    val testId: String = "",
    val sessionId: String = "",
    val patientId: String = "",
    val timeSurvivedMs: Long = 0L,
    val holesDodged: Int = 0,
    val score: Int = 0,
    val isWin: Boolean = false,
    val timestamp: Date = Date()
)