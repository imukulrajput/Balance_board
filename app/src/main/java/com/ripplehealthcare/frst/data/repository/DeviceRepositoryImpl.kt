package com.ripplehealthcare.frst.data.repository

import com.ripplehealthcare.frst.domain.model.ConnectionState
import com.ripplehealthcare.frst.data.bluetooth.BluetoothDataSource
import com.ripplehealthcare.frst.data.model.SensorData
import com.ripplehealthcare.frst.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.StateFlow

class DeviceRepositoryImpl(private val dataSource: BluetoothDataSource) : DeviceRepository {
    override val connectionState: StateFlow<ConnectionState> = dataSource.connectionState
    override suspend fun connect(): Boolean = dataSource.connect()
    override fun disconnect() = dataSource.disconnect()
    override fun startTest() = dataSource.startTest()
    override fun stopTest() = dataSource.stopTest()
    override fun resetSensorData() = dataSource.resetSensorData()
    override fun sendSaveCalibrationCommand() = dataSource.sendSaveCalibrationCommand()
    override fun resetCalibrationData() = dataSource.resetCalibrationData()
    override fun stop() = dataSource.stop()
    override fun getSensorDataFlow(): StateFlow<SensorData?> = dataSource.getSensorDataFlow()
}
