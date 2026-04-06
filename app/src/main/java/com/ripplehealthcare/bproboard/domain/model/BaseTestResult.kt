package com.ripplehealthcare.bproboard.domain.model

import java.util.Date

abstract class BaseTestResult {
    abstract val testId: String
    abstract val sessionId: String
    abstract val patientId: String
    abstract val centerId: String
    abstract val doctorId: String
    abstract val timestamp: Date
    abstract val testType: TestType
}