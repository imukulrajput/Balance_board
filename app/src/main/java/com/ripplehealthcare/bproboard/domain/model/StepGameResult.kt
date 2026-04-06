package com.ripplehealthcare.bproboard.domain.model

import java.util.Date

data class StepGameResult(
    val testId: String = "",
    val sessionId: String = "",
    val patientId: String = "",
    val score: Int = 0,
    val correctHits: Int = 0,
    val incorrectHits: Int = 0,
    val timestamp: Date = Date()
)