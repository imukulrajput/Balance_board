package com.ripplehealthcare.bproboard.domain.model

import java.util.Date
import java.util.UUID

data class TugResult(
    override val testId: String = UUID.randomUUID().toString(),
    override val sessionId: String = "",
    override val patientId: String = "",
    override val centerId: String = "", // Added
    override val doctorId: String = "",  // Replaces userId
    override val timestamp: Date = Date(),
    override val testType: TestType = TestType.TUG,

    // TUG Specific Data
    val totalTimeSeconds: Int = 0,
    val turnSpeed: Float = 0f, // degrees per second

    // Raw Acceleration History (for graphing later)
    val accLeft: List<AccPoint> = emptyList(),
    val accRight: List<AccPoint> = emptyList(),
    val accCenter: List<AccPoint> = emptyList()
): BaseTestResult()