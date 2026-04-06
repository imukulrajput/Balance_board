package com.ripplehealthcare.bproboard.domain.model

import java.util.Date
import java.util.UUID

data class ThirtySecResult(
    override val testId: String = UUID.randomUUID().toString(),
    override val sessionId: String = "",
    override val patientId: String = "",
    override val centerId: String = "", // Added
    override val doctorId: String = "",  // Replaces userId
    override val timestamp: Date = Date(),
    override val testType: TestType = TestType.THIRTY_SECONDS,

    // Test Specific Data
    val totalRepetitions: Int = 0,
    val repTimestamps: List<Long> = emptyList(), // When each rep finished

    // NEW: Detailed Statistics per Repetition
    val repetitionStats: List<RepetitionStats> = emptyList(),

    // Raw Sensor Data (Full history)
    val accLeft: List<AccPoint> = emptyList(),
    val accRight: List<AccPoint> = emptyList(),
    val accCenter: List<AccPoint> = emptyList()
): BaseTestResult()