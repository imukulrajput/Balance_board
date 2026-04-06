package com.ripplehealthcare.bproboard.domain.model

data class TestResult(
    val testId: String = "",
    val testType: TestType,
    val totalReps: Int,
    val times: List<Long>,
    val duration: Int // In seconds
)