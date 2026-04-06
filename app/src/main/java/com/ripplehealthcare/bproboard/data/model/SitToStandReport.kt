package com.ripplehealthcare.bproboard.data.model

data class SitToStandReport(
    val date: String,
    val time: String,
    val referenceTime: Float,
    val repetitions: List<Long>
)
