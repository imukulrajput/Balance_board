package com.ripplehealthcare.bproboard.domain.model

import java.util.Date

data class PatternDrawingResult(
    val testId: String = "",
    val sessionId: String = "",
    val patientId: String = "",
    val gameMode: String = "",
    val level: Int = 0,
    val levelName: String = "",
    val timeTakenMs: Long = 0,
    val targetsHit: Int = 0,
    val totalTargets: Int = 0,
    val fallCount: Int = 0,
    val angularErrors: List<Float> = emptyList(),
    val frontalData: List<Float> = emptyList(), // Pitch (Y-axis)
    val sagittalData: List<Float> = emptyList(), // Yaw (X-axis)
    val timestamp: Date = Date()
)