package com.ripplehealthcare.frst.utils

import com.ripplehealthcare.frst.domain.model.Gender

// Data model
data class NormValue(
    val minAge: Int,
    val maxAge: Int,
    val meanValue: Double, // Target for 100%
    val failureValue: Double // Threshold for 0%
)

// Normative values for 5xSTS
val fiveTimesSTSNorms = mapOf(
    Gender.MALE to listOf(
        NormValue(0, 59, 10.0, 15.0),
        NormValue(60, 64, 11.4, 20.0),
        NormValue(65, 69, 12.0, 21.0),
        NormValue(70, 74, 12.6, 22.0),
        NormValue(75, 79, 13.5, 24.0),
        NormValue(80, 84, 14.8, 26.0),
        NormValue(85, 89, 16.0, 28.0),
        NormValue(90, 120, 18.0, 32.0)
    ),
    Gender.FEMALE to listOf(
        NormValue(0, 59, 11.0, 16.0),
        NormValue(60, 64, 12.7, 21.0),
        NormValue(65, 69, 13.4, 22.0),
        NormValue(70, 74, 13.9, 23.0),
        NormValue(75, 79, 14.5, 25.0),
        NormValue(80, 84, 15.0, 27.0),
        NormValue(85, 89, 17.5, 30.0),
        NormValue(90, 120, 20.0, 35.0)
    ),
    Gender.OTHER to listOf(
        NormValue(0, 59, 10.5, 15.5),
        NormValue(60, 64, 12.0, 20.5),
        NormValue(65, 69, 12.7, 21.5),
        NormValue(70, 74, 13.2, 22.5),
        NormValue(75, 79, 14.0, 24.5),
        NormValue(80, 84, 14.9, 26.5),
        NormValue(85, 89, 16.7, 29.0),
        NormValue(90, 120, 19.0, 33.5)
    )
)

// Normative values for 30sSTS
val thirtySecSTSNorms = mapOf(
    Gender.MALE to listOf(
        NormValue(0, 59, 16.0, 10.0),
        NormValue(60, 64, 14.0, 8.0),
        NormValue(65, 69, 12.0, 7.0),
        NormValue(70, 74, 12.0, 6.0),
        NormValue(75, 79, 11.0, 5.0),
        NormValue(80, 84, 10.0, 4.0),
        NormValue(85, 89, 8.0, 3.0),
        NormValue(90, 120, 7.0, 2.0)
    ),
    Gender.FEMALE to listOf(
        NormValue(0, 59, 14.0, 9.0),
        NormValue(60, 64, 12.0, 7.0),
        NormValue(65, 69, 11.0, 6.0),
        NormValue(70, 74, 10.0, 5.0),
        NormValue(75, 79, 10.0, 4.0),
        NormValue(80, 84, 9.0, 3.0),
        NormValue(85, 89, 7.0, 2.0),
        NormValue(90, 120, 6.0, 1.0)
    ),
    Gender.OTHER to listOf(
        NormValue(0, 59, 15.0, 9.5),
        NormValue(60, 64, 13.0, 7.5),
        NormValue(65, 69, 11.5, 6.5),
        NormValue(70, 74, 11.0, 5.5),
        NormValue(75, 79, 10.5, 4.5),
        NormValue(80, 84, 9.5, 3.5),
        NormValue(85, 89, 7.5, 2.5),
        NormValue(90, 120, 6.5, 1.5)
    )
)

val tugNorms = mapOf(
    Gender.MALE to listOf(
        NormValue(0, 64, 8.0, 18.0),
        NormValue(65, 69, 9.0, 20.0),
        NormValue(70, 74, 10.0, 22.0),
        NormValue(75, 79, 11.0, 24.0),
        NormValue(80, 89, 13.0, 28.0),
        NormValue(90, 120, 16.0, 35.0)
    ),
    Gender.FEMALE to listOf(
        NormValue(0, 64, 8.5, 19.0),
        NormValue(65, 69, 9.5, 21.0),
        NormValue(70, 74, 10.5, 23.0),
        NormValue(75, 79, 11.5, 25.0),
        NormValue(80, 89, 14.0, 30.0),
        NormValue(90, 120, 18.0, 40.0)
    ),
    Gender.OTHER to listOf(
        NormValue(0, 64, 8.2, 18.5),
        NormValue(65, 69, 9.2, 20.5),
        NormValue(70, 74, 10.2, 22.5),
        NormValue(75, 79, 11.2, 24.5),
        NormValue(80, 89, 13.5, 29.0),
        NormValue(90, 120, 17.0, 37.5)
    )
)
