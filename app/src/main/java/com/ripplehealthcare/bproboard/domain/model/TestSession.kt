package com.ripplehealthcare.bproboard.domain.model

import FourStageResult
import com.google.firebase.firestore.PropertyName
import java.util.Date
import java.util.UUID

data class TestSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val centerId: String = "",
    val doctorId: String = "",
    val patientId: String = "",
    val timestamp: Date = Date(),
    val doctorNotes: String = "",
    val tugResult: TugResult? = null,
    val fiveRepResult: FiveRepResult? = null,
    val thirtySecResult: ThirtySecResult? = null,
    val fourStageResult: FourStageResult? = null,

    val testTypesPerformed: List<String> = emptyList(),
    val sessionDurationSec: Int = 0,

    @get:PropertyName("isCompleted")
    @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false
)