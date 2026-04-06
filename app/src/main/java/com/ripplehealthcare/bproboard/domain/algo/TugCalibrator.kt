package com.ripplehealthcare.bproboard.domain.algo

import com.ripplehealthcare.bproboard.domain.model.TugThresholds
import com.ripplehealthcare.bproboard.ui.screens.RecordedDataPoint
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

object TugCalibrator {

    fun calculateThresholds(
        sittingData: List<RecordedDataPoint>,
        standingData: List<RecordedDataPoint>,
        walkingData: List<RecordedDataPoint>
    ): TugThresholds {

        // 1. Calculate Average Angles (Using LeftRoll as primary)
        // You could average Left+Right for robustness
        val avgSitRoll = sittingData.map { it.sensorData.leftRoll }.average()
        val avgStandRoll = standingData.map { it.sensorData.leftRoll }.average()

        // 2. Posture Threshold (The Midpoint)
        val postureThreshold = (avgSitRoll + avgStandRoll) / 2.0
        val isStandingValueHigher = avgStandRoll > avgSitRoll

        // 3. Movement Threshold (Noise Floor)
        // We look at the Sitting Data (which should be still) and find the Max Energy
        val maxSittingNoise = calculateMaxEnergy(sittingData)
        // We enforce a minimum of 10.0 to prevent hypersensitivity on very stable sensors
        val movementThreshold = (maxSittingNoise * 2.5).coerceAtLeast(10.0)

        // 4. Step Threshold (Walking Intensity)
        // We calculate standard deviation of the roll during walking
        val walkingRolls = walkingData.map { it.sensorData.leftRoll }
        val stdDev = calculateStdDev(walkingRolls)
        val stepThreshold = (stdDev * 0.6).coerceAtLeast(15.0)

        return TugThresholds(
            postureThreshold = postureThreshold,
            movementThreshold = movementThreshold,
            stepPeakThreshold = stepThreshold,
            isStandingValueHigher = isStandingValueHigher
        )
    }

    // Helper: Calculate Max Energy (Chaos) in a dataset
    private fun calculateMaxEnergy(data: List<RecordedDataPoint>): Double {
        var maxEnergy = 0.0
        for (i in 1 until data.size) {
            val prev = data[i - 1].sensorData
            val curr = data[i].sensorData
            val energy = abs(curr.leftRoll - prev.leftRoll) + abs(curr.rightRoll - prev.rightRoll)
            if (energy > maxEnergy) maxEnergy = energy.toDouble()
        }
        return maxEnergy
    }

    // Helper: Standard Deviation
    private fun calculateStdDev(values: List<Float>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        val sumSq = values.sumOf { (it - mean).pow(2) }
        return sqrt(sumSq / values.size)
    }
}