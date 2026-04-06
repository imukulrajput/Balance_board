package com.ripplehealthcare.bproboard.data.bluetooth


import com.ripplehealthcare.bproboard.domain.model.ConnectionState
import com.ripplehealthcare.bproboard.data.model.SensorData
import kotlinx.coroutines.flow.StateFlow

interface BluetoothDataSource {
    val connectionState: StateFlow<ConnectionState>
    suspend fun connect(): Boolean
    fun disconnect()
    fun sendResetCommand()
    fun startTest()
    fun stopTest()
    fun getSensorDataFlow(): StateFlow<SensorData?>
    fun resetSensorData()
    fun sendSaveCalibrationCommand()
    fun resetCalibrationData()
    fun stop()
}

