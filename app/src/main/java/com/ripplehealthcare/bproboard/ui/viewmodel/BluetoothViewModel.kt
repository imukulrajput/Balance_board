// presentation/viewmodel/BluetoothViewModel.kt
package com.ripplehealthcare.bproboard.ui.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ripplehealthcare.bproboard.core.di.AppContainer
import com.ripplehealthcare.bproboard.data.model.SensorData
import com.ripplehealthcare.bproboard.domain.model.ConnectionState
import com.ripplehealthcare.bproboard.domain.repository.DeviceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class BluetoothViewModel(private val context: Context) : ViewModel() {
    private val appContainer = AppContainer()
    private var deviceRepository: DeviceRepository? = null
    private var connectionStateJob: Job? = null
    private var sensorDataJob: Job? = null
    private var standingJob: Job? = null
    private var sittingJob: Job? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _sensorData = MutableStateFlow<SensorData?>(null)
    val sensorData = _sensorData.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices = _pairedDevices.asStateFlow()

    var centerPitch: Float = -89.42f
    var centerYaw: Float = 0.76f

    init {
        getPairedDevices()
    }

    fun connectToDevice(device: BluetoothDevice) {
        disconnect() // Clean up previous connection
        appContainer.initialize(context, device) // Initialize with the specific device
        deviceRepository = appContainer.deviceRepository

        connectionStateJob = viewModelScope.launch {
            deviceRepository?.connectionState?.collectLatest { state ->
                _connectionState.value = state
            } ?: run {
                _connectionState.value = ConnectionState.DISCONNECTED
                Log.e("BluetoothViewModel", "DeviceRepository not initialized")
            }
        }

        sensorDataJob = viewModelScope.launch {
            deviceRepository?.getSensorDataFlow()?.collectLatest { data ->
                _sensorData.value = data
            } ?: Log.w("BluetoothViewModel", "Sensor data flow not available")
        }

        viewModelScope.launch {
            val success = deviceRepository?.connect() ?: false
            if (!success) {
                _connectionState.value = ConnectionState.DISCONNECTED
                Log.e("BluetoothViewModel", "Failed to connect to device")
            }
        }
    }

    fun getPairedDevices() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.w("BluetoothViewModel", "BLUETOOTH_CONNECT permission not granted")
            return // Exit early; permission must be granted before calling this
        }
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        _pairedDevices.value = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        if (_pairedDevices.value.isEmpty()) {
            Log.w("BluetoothViewModel", "No paired devices found")
        }
    }

    fun disconnect() {
        connectionStateJob?.cancel()
        sensorDataJob?.cancel()
        standingJob?.cancel()
        sittingJob?.cancel()
        deviceRepository?.disconnect()
        _connectionState.value = ConnectionState.DISCONNECTED
        _sensorData.value = null
        deviceRepository = null // Reset to null after disconnection
        Log.d("BluetoothViewModel", "Disconnected.")
    }

    // Delegate to repository methods with better error handling
    fun startTest() = deviceRepository?.startTest() ?: Log.w("BluetoothViewModel", "No repository")
    fun stopTest() = deviceRepository?.stopTest() ?: Log.w("BluetoothViewModel", "No repository")
    fun resetSensorData() = deviceRepository?.resetSensorData() ?: Log.w("BluetoothViewModel", "No repository")
    fun sendSaveCalibration() = deviceRepository?.sendSaveCalibrationCommand() ?: Log.w("BluetoothViewModel", "No repository")

    override fun onCleared() {
        super.onCleared()
        connectionStateJob?.cancel()
        sensorDataJob?.cancel()
        standingJob?.cancel()
        sittingJob?.cancel()
        deviceRepository?.stop()
        Log.d("BluetoothViewModel", "ViewModel cleared and resources released.")
    }

}