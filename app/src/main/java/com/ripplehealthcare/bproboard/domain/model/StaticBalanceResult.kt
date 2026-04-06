package com.ripplehealthcare.bproboard.domain.model

import java.util.Date

data class StaticBalanceResult(
    val testId: String = "",
    val sessionId: String = "",
    val patientId: String = "",
    val gameMode: String = "", // "SITTING" or "STANDING"
    val level: Int = 1,
    val totalTimeMs: Long = 0L,
    val balanceTimeMs: Long = 0L,
    val imbalanceTimeMs: Long = 0L,
    val efficiencyPercentage: Int = 0,
    val fallCount: Int = 0,
    val fallErrors: List<Float> = emptyList(),
    val frontalData: List<Float> = emptyList(), // Pitch values
    val sagittalData: List<Float> = emptyList(), // Yaw values
    val timestamp: Date = Date()
)