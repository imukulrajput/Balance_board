package com.ripplehealthcare.bproboard.domain.model



import java.util.Date

data class ShapeTrainingResult(
    val testId: String = "",
    val sessionId: String = "",
    val patientId: String = "",
    val score: Int = 0,
    val timeTakenMs: Long = 0,
    val gameMode: String = "",
    val fallCount: Int = 0,
    val level: Int = 1,
    val angularErrors: List<Float> = emptyList(),
    val frontalData: List<Float> = emptyList(),
    val sagittalData: List<Float> = emptyList(),
    val targetSequence: List<Int> = emptyList(),
    val targetAngles: List<Float> = emptyList(),

    val timestamp: Date = Date()
)