package com.ripplehealthcare.bproboard.domain.model

data class StageResultData(
    val stageNumber: Int = 0,

    // Use SwayPoint instead of Pair, and default to emptyList()
    val pointsLeft: List<SwayPoint> = emptyList(),
    val accelerationLeft: List<AccPoint> = emptyList(),

    val pointsRight: List<SwayPoint> = emptyList(),
    val accelerationRight: List<AccPoint> = emptyList(),

    val pointsCenter: List<SwayPoint> = emptyList(),
    val accelerationCenter: List<AccPoint> = emptyList(),

    val durationSeconds: Int = 10
)