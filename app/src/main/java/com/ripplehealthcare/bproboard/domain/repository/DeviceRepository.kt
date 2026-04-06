package com.ripplehealthcare.bproboard.domain.repository

import com.ripplehealthcare.bproboard.domain.model.ConnectionState
import com.ripplehealthcare.bproboard.data.model.SensorData
import kotlinx.coroutines.flow.StateFlow

interface DeviceRepository {
    val connectionState: StateFlow<ConnectionState>
    suspend fun connect(): Boolean
    fun disconnect()
    fun startTest()
    fun stopTest()
    fun resetSensorData()
    fun sendSaveCalibrationCommand()
    fun resetCalibrationData()
    fun stop()
    fun getSensorDataFlow(): StateFlow<SensorData?>
}