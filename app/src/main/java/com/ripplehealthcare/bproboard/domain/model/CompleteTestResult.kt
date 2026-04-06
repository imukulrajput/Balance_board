package com.ripplehealthcare.bproboard.domain.model

data class CompleteTestResult(
    val testId: String = "",
    val patientId: String = "", // Needed to link to the patient
    val userId: String = "",    // Needed for security rules (ownership)
    val date: String = "",
    val time: String = "",
    val patient: Patient = Patient(),
    val repetitions5x: List<Long> = emptyList(), // per rep times in ms
    val repetitions30s: List<Long> = emptyList(), // timestamps or reps completed
)