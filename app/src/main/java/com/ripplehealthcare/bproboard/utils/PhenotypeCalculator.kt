package com.ripplehealthcare.bproboard.utils

import com.ripplehealthcare.bproboard.domain.model.AccPoint
import com.ripplehealthcare.bproboard.domain.model.TestType
import kotlin.math.abs
import kotlin.math.sqrt

object PhenotypeCalculator {
    fun calculateFatigueFromDurations(durations: List<Double>): Double {
        if (durations.size < 2) return 0.0

        val t1 = durations.first()
        val tLast = durations.last()

        if (t1 <= 0.0) return 0.0

        return ((tLast - t1) * 100) / t1
    }

    fun calculateRepetitionVariability(repTimestamps: List<Long>): Double {
        if (repTimestamps.size < 2) return 0.0


        val mean = repTimestamps.average()
        if (mean == 0.0) return 0.0

        val variance = repTimestamps.map { (it - mean) * (it - mean) }.average()
        val sd = sqrt(variance)

        return (sd * 100) / mean
    }

    fun calculateLegAsymmetry(leftData: List<AccPoint>, rightData: List<AccPoint>): Double {
        if (leftData.size != rightData.size || leftData.isEmpty()) return 0.0

        // 1. Calculate Resultant Magnitudes (SVM) for each leg
        val leftMagnitudes = leftData.map { sqrt(it.x * it.x + it.y * it.y + it.z * it.z) }
        val rightMagnitudes = rightData.map { sqrt(it.x * it.x + it.y * it.y + it.z * it.z) }

        // 2. Calculate Total Effort (Area under the signal)
        val totalLeftEffort = leftMagnitudes.sum()
        val totalRightEffort = rightMagnitudes.sum()

        // 3. Apply the Asymmetry Index formula
        val difference = abs(totalRightEffort - totalLeftEffort)
        val totalEffort = totalRightEffort + totalLeftEffort

        return (difference / totalEffort) * 100.0
    }

    fun calculateBiomechanicalPower(
        weight: Double,
        accData: List<AccPoint>,
        sittingAccPoint: AccPoint
    ): Pair<Double, Double> {
        val massPerLeg = weight / 2.0
        val g = 9.80665
        val sittingMag = sqrt(sittingAccPoint.x * sittingAccPoint.x +
                sittingAccPoint.y * sittingAccPoint.y +
                sittingAccPoint.z * sittingAccPoint.z)

        var velocity = 0.0
        var peakPower = 0.0
        var totalPower = 0.0
        var movementSamples = 0

        for (i in 1 until accData.size) {
            val dt = (accData[i].timestamp - accData[i-1].timestamp) / 1000.0
            if (dt <= 0 || dt > 0.5) continue

            val mag = sqrt(accData[i].x * accData[i].x + accData[i].y * accData[i].y + accData[i].z * accData[i].z)
            val linearAcc = mag - sittingMag

            // First Kinetic equation of motion [ v = u + at ]
            velocity += linearAcc * dt
            if (velocity < 0) velocity = 0.0

            // Newton's second law [ F = m(a + g) ]
            val force = massPerLeg * (linearAcc + g)

            // Power [ P = Fv ]
            val power = force * velocity

            if (velocity > 0.1) {
                totalPower += power
                movementSamples++
            }

            if (power > peakPower) peakPower = power
        }

        val meanPower = if (movementSamples > 0) totalPower / movementSamples else 0.0
        return meanPower to peakPower
    }

    fun calculatePowerPerRep(
        weight: Double,
        accData: List<AccPoint>,
        repDurations: List<Long>,
        sittingBaseline: AccPoint
    ): List<Pair<Double, Double>> {

        val results = mutableListOf<Pair<Double, Double>>()
        val startTime = accData.firstOrNull()?.timestamp ?: return emptyList()

        var currentWindowStart = startTime

        repDurations.forEach { duration ->
            val currentWindowEnd = currentWindowStart + duration

            val repData = accData.filter { it.timestamp in currentWindowStart until currentWindowEnd }

            if (repData.size > 2) {
                val power = calculateBiomechanicalPower(weight, repData, sittingBaseline)
                results.add(power)
            }

            currentWindowStart = currentWindowEnd
        }

        return results
    }

    fun calculateDomainScores(report: AllTestReport): List<Pair<String, Double>> {
        val scores = report.individualScores
        val tug = scores[TestType.TUG] ?: 0.0
        val sway = scores[TestType.FOUR_STAGE_BALANCE] ?: 0.0
        val sts5 = scores[TestType.FIVE_REPS] ?: 0.0
        val sts30 = scores[TestType.THIRTY_SECONDS] ?: 0.0

        val resultTug = report.tugResult
        val turningScore = resultTug?.let {
            // Normalize: 200 deg/s or higher = 100 points
            val speed = it.turnSpeed // assuming this is a Double in degrees/sec
            ((speed / 200.0) * 100.0).coerceIn(0.0, 100.0)
        } ?: 0.0

        // Using TUG score as proxy for Gait Variability (30%) for now
        val dynamicBalance = (tug * 0.4) + (turningScore * 0.3) + (tug * 0.3)

        // Calculate Strength Domain (50% STS Time, 30% Power, 20% Symmetry)
        // We normalize Power and Symmetry to a 0-100 scale for weighting
        val result5 = report.fiveRepResult
        val weight = report.patient.weight.toDoubleOrNull() ?: 60.0
        val symmetryScore = result5?.let {
            val sym = calculateLegAsymmetry(it.accLeft, it.accRight)
            (100.0 - (sym * 5)).coerceIn(0.0, 100.0)
        } ?: 0.0

        val powerScore = result5?.let {
            val left = calculateBiomechanicalPower(weight, it.accLeft, it.calAccLeft).first
            val right = calculateBiomechanicalPower(weight, it.accRight, it.calAccRight).first
            val totalMeanPower = left + right

            val powerPerKg = totalMeanPower / weight
            ((powerPerKg / 4.0) * 100.0).coerceIn(0.0, 100.0)
        } ?: 0.0

        val strengthDomain = (sts5 * 0.5) + (powerScore * 0.3) + (symmetryScore * 0.2)

        // Calculate Endurance Domain (70% 30s Reps, 30% Fatigue Decline)
        val result30 = report.thirtySecResult
        val fatigueScore = result30?.let { result ->
            val durations = result.repTimestamps.map { it / 1000.0 }
            val fatigue = calculateFatigueFromDurations(durations)
            (100.0 - fatigue).coerceIn(0.0, 100.0) // Lower fatigue = higher score
        } ?: 0.0

        val enduranceDomain = (sts30 * 0.7) + (fatigueScore * 0.3)

        return listOf(
            "Static Balance" to sway,
            "Dynamic Balance" to dynamicBalance,
            "Strength" to strengthDomain,
            "Endurance" to enduranceDomain,
            "Gait Stability" to (tug * 0.6 + sway * 0.4)
        )
    }
}