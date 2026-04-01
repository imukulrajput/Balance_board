package com.ripplehealthcare.frst.data.model

import com.ripplehealthcare.frst.domain.model.Patient

data class CompleteReport(
    val date: String,
    val time: String,
    val patient: Patient,
    val repetitions5x: List<Long>, // per rep times in ms
    val repetitions30s: List<Long>, // timestamps or reps completed
)
