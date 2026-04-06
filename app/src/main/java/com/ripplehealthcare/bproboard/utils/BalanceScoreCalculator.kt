package com.ripplehealthcare.bproboard.utils

import com.ripplehealthcare.bproboard.domain.model.StageResultData
import com.ripplehealthcare.bproboard.domain.model.SwayPoint
import kotlin.math.pow
import kotlin.math.sqrt

data class NormativeRange(
    val target: Double, // The value for a 100% score
    val failure: Double // The value for a 0% score
)

object BalanceScoreCalculator {

    fun calculateTimedScore(actualTime: Double, norms: NormativeRange): Double {
        return when {
            actualTime <= 0.0 -> 0.0
            actualTime <= norms.target -> 100.0 // Perfection
            actualTime >= norms.failure -> 0.0   // High Fall Risk
            else -> {
                // Linear interpolation: 100 * (Failure - Actual) / (Failure - Target)
                val score = 100.0 * (norms.failure - actualTime) / (norms.failure - norms.target)
                score.coerceIn(0.0, 100.0)
            }
        }
    }

    fun calculateRepetitionScore(actualReps: Int, norms: NormativeRange): Double {
        return when {
            actualReps <= 0 -> 0.0
            actualReps >= norms.target -> 100.0
            else -> {
                // Percentage of target reps
                val score = (actualReps.toDouble() / norms.target) * 100.0
                score.coerceIn(0.0, 100.0)
            }
        }
    }

    fun calculateFourStageScore(stages: Map<Int, StageResultData>): Double {
        var totalPoints = 0.0

        val stabilityWeights = mapOf(1 to 5.0, 2 to 10.0, 3 to 10.0, 4 to 15.0)
        val timeWeights = mapOf(1 to 0.5, 2 to 1.0, 3 to 2.0, 4 to 2.5)

        stages.forEach { (stageNum, data) ->
            val seconds = data.durationSeconds ?: 10

            totalPoints += seconds * (timeWeights[stageNum] ?: 0.0)

            val mrd = calculateMRD(data.pointsCenter)
            val quality = calculateStabilityQuality(mrd)

            totalPoints += quality * (stabilityWeights[stageNum] ?: 0.0)
        }

        return totalPoints.coerceIn(0.0, 100.0)
    }

    private fun calculateStabilityQuality(mrd: Double): Double {
        return when {
            mrd <= 2.0 -> 1.0          // 100% Quality
            mrd <= 4.0 -> 0.75        // 75% Quality
            mrd <= 6.0 -> 0.50        // 50% Quality
            mrd <= 8.0 -> 0.25        // 25% Quality
            else -> 0.0                // 0% Quality (Critical)
        }
    }

    private fun calculateMRD(points: List<SwayPoint>): Double {
        if (points.isEmpty()) return 0.0
        val sumDistance = points.sumOf { p ->
            sqrt(p.x.toDouble().pow(2) + p.y.toDouble().pow(2))
        }
        return sumDistance / points.size
    }
}