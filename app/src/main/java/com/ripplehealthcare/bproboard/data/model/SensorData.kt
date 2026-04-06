package com.ripplehealthcare.bproboard.data.model

data class SensorData(
    val leftPitch: Float,
    val leftRoll: Float,
    val leftYaw: Float,
    val rightPitch: Float,
    val rightRoll: Float,
    val rightYaw: Float,
    val centerPitch: Float,
    val centerRoll: Float,
    val centerYaw: Float,

    val leftAccX: Float,
    val leftAccY: Float,
    val leftAccZ: Float,
    val rightAccX: Float,
    val rightAccY: Float,
    val rightAccZ: Float,
    val centerAccX: Float,
    val centerAccY: Float,
    val centerAccZ: Float,

    val leftGyX: Float,
    val leftGyY: Float,
    val leftGyZ: Float,
    val rightGyX: Float,
    val rightGyY: Float,
    val rightGyZ: Float,
    val centerGyX: Float,
    val centerGyY: Float,
    val centerGyZ: Float
)