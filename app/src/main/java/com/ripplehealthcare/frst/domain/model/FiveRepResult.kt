package com.ripplehealthcare.frst.domain.model

import java.util.Date
import java.util.UUID

data class FiveRepResult(
    override val testId: String = UUID.randomUUID().toString(),
    override val sessionId: String = "",
    override val patientId: String = "",
    override val centerId: String = "",
    override val doctorId: String = "",
    override val timestamp: Date = Date(),
    override val testType: TestType = TestType.FIVE_REPS,

    // Test Specific Data
    val totalTimeSeconds: Float = 0f,
    val repTimes: List<Long> = emptyList(), // Duration of each rep

    val repetitionStats: List<RepetitionStats> = emptyList(),

    val calAccLeft: AccPoint = AccPoint(0f, 0f, 0f),
    val calAccRight: AccPoint = AccPoint(0f, 0f, 0f),

    // Raw Sensor Data (Full history)
    val accLeft: List<AccPoint> = emptyList(),
    val accRight: List<AccPoint> = emptyList(),
    val accCenter: List<AccPoint> = emptyList()
): BaseTestResult()