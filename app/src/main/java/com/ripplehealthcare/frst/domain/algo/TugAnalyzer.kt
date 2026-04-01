package com.ripplehealthcare.frst.domain.algo

import com.ripplehealthcare.frst.domain.model.TugThresholds
import com.ripplehealthcare.frst.ui.screens.RecordedDataPoint
import kotlin.math.abs

data class TugResult(
    val totalTimeSeconds: Double,
    val stepCount: Int,
    val status: String
)

class TugAnalyzer {

    fun analyzeTug(data: List<RecordedDataPoint>, thresholds: TugThresholds): TugResult {
        if (data.size < 10) return TugResult(0.0, 0, "Insufficient Data")

        val energyList = calculateEnergy(data)

        var startTime: Long = 0
        var endTime: Long = 0
        var foundStart = false
        var foundEnd = false

        for (i in 1 until data.size) {
            val currentRoll = data[i].sensorData.leftRoll.toDouble()
            val energy = energyList[i-1] // Energy leading UP TO this point

            // Dynamic Logic check:
            // If Stand > Sit (e.g. 20 > -90), then Standing is (Roll > Threshold)
            // If Stand < Sit (e.g. -90 < 20), then Standing is (Roll < Threshold)
            val isStanding = if (thresholds.isStandingValueHigher) {
                currentRoll > thresholds.postureThreshold
            } else {
                currentRoll < thresholds.postureThreshold
            }

            if (!foundStart) {
                // START CONDITION: Transition to Standing + High Energy
                if (isStanding && energy > thresholds.movementThreshold) {
                    startTime = data[i].timestamp
                    foundStart = true
                }
            } else {
                // END CONDITION: Transition to Sitting + Low Energy (Silence)
                // We assume they are sitting, but we need to verify they stay still
                if (!isStanding && energy < thresholds.movementThreshold) {

                    // Check for 2 seconds of silence using the Dynamic Movement Threshold
                    if (isStillForDuration(data, energyList, i, 2000, thresholds.movementThreshold)) {
                        endTime = data[i].timestamp
                        foundEnd = true
                        break
                    }
                }
            }
        }

        if (!foundStart || !foundEnd) return TugResult(0.0, 0, "Error: Start/End not found")

        val durationSeconds = (endTime - startTime) / 1000.0

        // Pass the Dynamic Step Threshold
        val steps = countSteps(data, startTime, endTime, thresholds.stepPeakThreshold)

        return TugResult(durationSeconds, steps, "Success")
    }

    // Helper: Calculate variance/change between frames
    private fun calculateEnergy(data: List<RecordedDataPoint>): List<Float> {
        val energy = mutableListOf<Float>()
        for (i in 1 until data.size) {
            val prev = data[i - 1].sensorData
            val curr = data[i].sensorData

            // We focus on LeftRoll + RightRoll as the primary indicators
            val deltaLeft = abs(curr.leftRoll - prev.leftRoll)
            val deltaRight = abs(curr.rightRoll - prev.rightRoll)
            energy.add(deltaLeft + deltaRight)
        }
        return energy
    }

    // Helper: Look ahead to see if user remains still
    private fun isStillForDuration(
        data: List<RecordedDataPoint>,
        energy: List<Float>,
        currentIndex: Int,
        durationMs: Long,
        movementThreshold: Double // Added parameter
    ): Boolean {
        val startTs = data[currentIndex].timestamp

        // We look forward from the current index
        for (j in currentIndex until data.size - 1) {

            // Safety check: Energy list is smaller than Data list
            if (j >= energy.size) break

            val timeDiff = data[j].timestamp - startTs
            if (timeDiff >= durationMs) return true // Successfully stayed still

            // If they move again (Energy > Threshold), it wasn't the end
            if (energy[j] > movementThreshold) return false
        }

        // If we ran out of data but haven't hit the duration,
        // strictly speaking, we might assume they stayed still until the recording stopped.
        return true
    }

    // Helper: Count peaks in the walking phase
    private fun countSteps(
        data: List<RecordedDataPoint>,
        startTs: Long,
        endTs: Long,
        stepThreshold: Double // Added parameter
    ): Int {
        var steps = 0
        var isPeaking = false

        // Filter data to only the active TUG period
        val activeData = data.filter { it.timestamp in startTs..endTs }

        for (point in activeData) {
            val roll = point.sensorData.leftRoll

            // Use the passed stepThreshold instead of a constant
            if (abs(roll) > stepThreshold && !isPeaking) {
                isPeaking = true
                steps++
            } else if (abs(roll) < stepThreshold) {
                isPeaking = false
            }
        }
        // Multiply by 2 (Left sensor peaks = 1 step, but we have 2 legs)
        return steps * 2
    }
}