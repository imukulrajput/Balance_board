package com.ripplehealthcare.frst.domain.repository

import com.ripplehealthcare.frst.domain.model.ConnectionState
import com.ripplehealthcare.frst.data.model.SensorData
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