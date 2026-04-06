package com.ripplehealthcare.bproboard.utils

import com.ripplehealthcare.bproboard.domain.model.TestType

object ReportInterpretor {

    fun getClinicalInterpretation(report: AllTestReport): String {
        val scores = report.individualScores
        val sTug = scores[TestType.TUG] ?: 100.0
        val sSts = scores[TestType.FIVE_REPS] ?: 100.0 // Using 5-Rep as primary STS
        val sSway = scores[TestType.FOUR_STAGE_BALANCE] ?: 100.0

        val threshold = 50.0 // Define 'Low' score threshold

        return when {
            sTug < threshold && sSts < threshold && sSway < threshold ->
                "Note: General decline in mobility and stability observed; may indicate frailty."
            sSts < threshold && sTug >= threshold ->
                "Note: Lower scores in STS compared to TUG suggest a potential strength deficit."
            sTug < threshold && sSts >= threshold ->
                "Note: Slower TUG performance with normal STS may indicate potential gait instability."
            sSway < threshold && sSts >= threshold ->
                "Note: High sway with normal STS suggests a potential balance deficit."
            else -> "Note: Mobility and balance appear to be within expected ranges for this assessment."
        }
    }
}