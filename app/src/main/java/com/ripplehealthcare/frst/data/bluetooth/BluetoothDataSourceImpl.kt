package com.ripplehealthcare.frst.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.ripplehealthcare.frst.domain.model.ConnectionState
import com.ripplehealthcare.frst.data.model.SensorData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.IOException
import java.util.*

class BluetoothDataSourceImpl(private val device: BluetoothDevice) : BluetoothDataSource {
    private var socket: BluetoothSocket? = null
    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _sensorDataFlow = MutableStateFlow<SensorData?>(null)
    private val _standingFlow = MutableStateFlow<SensorData?>(null)
    private val _sittingFlow = MutableStateFlow<SensorData?>(null)

    override fun getSensorDataFlow(): StateFlow<SensorData?> = _sensorDataFlow

    init {
        // Start the monitor as soon as the manager is created
        startConnectionMonitor()
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        if (socket?.isConnected == true) {
            Log.d("BTManager", "Already connected.")
            _connectionState.value = ConnectionState.CONNECTED
            return@withContext true
        }

        _connectionState.value = ConnectionState.CONNECTING
        try {
            socket?.close()
            socket = device.createRfcommSocketToServiceRecord(uuid)
            socket?.connect()
            _connectionState.value = ConnectionState.CONNECTED
            Log.d("BTManager","Connected Successfully.")
            true
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.FAILED
            Log.e("BTManager", "Connection failed: ${e.message}")
            false
        }
    }

    override fun disconnect() {
        if (_connectionState.value != ConnectionState.DISCONNECTED) {
            Log.d("BTManager","Disconnected by user.")
            _connectionState.value = ConnectionState.DISCONNECTED
            try {
                socket?.close()
            } catch (e: IOException) {
                Log.e("BTManager", "Error closing socket: ${e.message}")
            } finally {
                socket = null
            }
        }
    }

    private fun startConnectionMonitor() {
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                if (_connectionState.value == ConnectionState.CONNECTED) {
                    try {
                        val outputStream = socket?.outputStream
                        if (outputStream == null) {
                            _connectionState.value = ConnectionState.DISCONNECTED
                            continue
                        }
                        outputStream.write(byteArrayOf())
                    } catch (e: IOException) {
                        Log.e("BTManager", "Connection lost (detected by monitor): ${e.message}")
                        _connectionState.value = ConnectionState.DISCONNECTED
                    }
                }
                // Check the connection status every 2 seconds
                delay(2000L)
            }
        }
    }

    override fun sendResetCommand() {
        scope.launch {
            try {
                val outputStream = socket?.outputStream ?: return@launch
                // Send bytes one by one like input stream receives them
                val commandBytes = listOf(0xAA, 0xAA, 0x55, 0x00, 0x00, 0x00, 0x00)
                for (byte in commandBytes) {
                    outputStream.write(byte)
                }
                outputStream.flush()
                Log.d("BTManager", "Reset command sent")
            } catch (e: Exception) {
                Log.e("BTManager", "Failed to send reset command: ${e.message}")
            }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    override fun startTest() {
        scope.launch(Dispatchers.IO) {
            val inputStream = socket?.inputStream ?: return@launch
            try {
                val outputStream = socket?.outputStream ?: return@launch

                // Step 1: Send START Test command
                val startCommand = byteArrayOf(
                    0xAA.toByte(),
                    0x0A.toByte(),
                    0x55,
                    0x00,
                    0x00,
                    0x00,
                    0x00
                )

                // 2. Writing safely
                if (socket?.isConnected == true) {
                    outputStream.write(startCommand)
                    outputStream.flush()
                }
            } catch (e: IOException) {
                // 3. Catch "Broken pipe" or other IO errors
                Log.w("BTManager", "Could not send START command (Socket might be closed): ${e.message}")
            } catch (e: Exception) {
                Log.e("BTManager", "Unexpected error in startTest", e)
            }

            val buffer = mutableListOf<Byte>()
            val tempBuffer = ByteArray(2048)
            val dataInputStream = DataInputStream(BufferedInputStream(inputStream))

            // --- Helper Functions ---

            // 1. 2-Byte to Float Converter (Little Endian, Signed Short / 100.0f)
            fun bytesToFloat(b0: Byte, b1: Byte): Float {
                val asShort = ((b1.toInt() and 0xFF) shl 8) or (b0.toInt() and 0xFF)
                val signedShort = asShort.toShort()
                return signedShort.toFloat() / 100.0f
            }

            // 2. CRC16 Logic
            fun computeCRC16(data: List<Byte>): Int {
                var crc = 0xFFFF
                for (byte in data) {
                    crc = crc xor (byte.toInt() and 0xFF)
                    for (j in 0 until 8) {
                        crc = if ((crc and 0x0001) != 0) {
                            (crc ushr 1) xor 0xA001
                        } else {
                            crc ushr 1
                        }
                    }
                }
                return crc
            }

            fun isValidFloat(value: Float): Boolean {
                return !value.isNaN() && !value.isInfinite()
            }

            fun normalizeAndInvert(value: Float, targetZero: Float = 90f): Float {
                var shifted = value - targetZero
                while (shifted <= -180f) shifted += 360f
                while (shifted > 180f) shifted -= 360f
                return -shifted
            }

            fun normalizeLeftRightPitch(value: Float): Float {
                return normalizeAndInvert(value, -90f)
            }

            // --- Main Loop ---
            while (isActive) {
                try {
                    if (dataInputStream.available() > 0) {
                        val bytesRead = dataInputStream.read(tempBuffer)
                        if (bytesRead > 0) {
                            buffer.addAll(tempBuffer.take(bytesRead))

                            // Loop while we have at least one full packet size (58 bytes)
                            while (buffer.size >= 58) {

                                // Check Header at index 0 (0xAA)
                                if (buffer[0] == 0xAA.toByte()) {
                                    // Check Footer at index 57 (0xBB)
                                    if (buffer[57] == 0xBB.toByte()) {

                                        // 1. Extract Data Section (Indices 1 to 54) -> 54 bytes
                                        val dataPacket = buffer.subList(1, 55)

                                        // 2. Extract Received CRC (Indices 55, 56)
                                        val crcLow = buffer[55].toInt() and 0xFF
                                        val crcHigh = buffer[56].toInt() and 0xFF
                                        val receivedCRC = (crcHigh shl 8) or crcLow

                                        // 3. Verify CRC
                                        val calculatedCRC = computeCRC16(dataPacket)

                                        if (calculatedCRC == receivedCRC) {
                                            // --- PARSING DATA ---

                                            // Helper to slice 2 bytes safely
                                            fun getFloatAt(index: Int): Float {
                                                return bytesToFloat(dataPacket[index], dataPacket[index + 1])
                                            }

                                            // Right Sensor (0-17) - Each value is 2 bytes
                                            val rightPitch = normalizeLeftRightPitch(getFloatAt(0))
                                            val rightYaw = getFloatAt(2)
                                            val rightRoll = getFloatAt(4)
                                            val rightAccX = getFloatAt(6) * 2
                                            val rightAccY = getFloatAt(8) * 2
                                            val rightAccZ = getFloatAt(10) * 2
                                            val rightGyX = getFloatAt(12)
                                            val rightGyY = getFloatAt(14)
                                            val rightGyZ = getFloatAt(16)

                                            // Left Sensor (18-35)
                                            val leftPitch = normalizeLeftRightPitch(getFloatAt(18))
                                            val leftYaw = getFloatAt(20)
                                            val leftRoll = getFloatAt(22)
                                            val leftAccX = getFloatAt(24) * 2
                                            val leftAccY = getFloatAt(26) * 2
                                            val leftAccZ = getFloatAt(28) * 2
                                            val leftGyX = getFloatAt(30)
                                            val leftGyY = getFloatAt(32)
                                            val leftGyZ = getFloatAt(34)

                                            // Center Sensor (36-53)
                                            val centerPitch = normalizeAndInvert(getFloatAt(36))
                                            val centerYaw = getFloatAt(38)
                                            val centerRoll = getFloatAt(40)
                                            val centerAccX = getFloatAt(42) * 2
                                            val centerAccY = getFloatAt(44) * 2
                                            val centerAccZ = getFloatAt(46) * 2
                                            val centerGyX = getFloatAt(48)
                                            val centerGyY = getFloatAt(50)
                                            val centerGyZ = getFloatAt(52)

                                            // Update Flow
                                            if (isValidFloat(leftPitch) && isValidFloat(leftRoll) && isValidFloat(leftYaw) &&
                                                isValidFloat(rightPitch) && isValidFloat(rightRoll) && isValidFloat(rightYaw) &&
                                                isValidFloat(centerPitch) && isValidFloat(centerRoll) && isValidFloat(centerYaw)) {

                                                _sensorDataFlow.value = SensorData(
                                                    leftPitch, leftRoll, -leftYaw,
                                                    rightPitch, rightRoll, -rightYaw,
                                                    centerPitch, centerRoll, -centerYaw,
                                                    leftAccX, leftAccY, leftAccZ,
                                                    rightAccX, rightAccY, rightAccZ,
                                                    centerAccX, centerAccY, centerAccZ,
                                                    leftGyX, leftGyY, leftGyZ,
                                                    rightGyX, rightGyY, rightGyZ,
                                                    centerGyX, centerGyY, centerGyZ
                                                )
                                            }
                                        } else {
                                            Log.e("BTManager", "CRC Mismatch! Calc: ${Integer.toHexString(calculatedCRC)} != Recv: ${Integer.toHexString(receivedCRC)}")
                                        }

                                        // Clear processed packet
                                        buffer.subList(0, 58).clear()

                                    } else {
                                        // Header found but Footer missing at expected index.
                                        buffer.removeAt(0)
                                    }
                                } else {
                                    // First byte is not header. Remove it.
                                    buffer.removeAt(0)
                                }
                            }
                        }
                    } else {
                        delay(10)
                    }
                } catch (e: Exception) {
                    Log.e("BTManager", "Error: ${e.message}")
                    if (!isActive) break
                }
            }
        }
    }
//    override fun startTest() {
//        scope.launch(Dispatchers.IO) {
//            val inputStream = socket?.inputStream ?: return@launch
//            try {
//                val outputStream = socket?.outputStream ?: return@launch
//
//                // Step 1: Send START Test command
//                val startCommand = byteArrayOf(
//                    0xAA.toByte(),
//                    0x0A.toByte(),
//                    0x55,
//                    0x00,
//                    0x00,
//                    0x00,
//                    0x00
//                )
//
//                // 2. Writing safely
//                if (socket?.isConnected == true) {
//                    outputStream.write(startCommand)
//                    outputStream.flush()
//                }
//            } catch (e: IOException) {
//                // 3. Catch "Broken pipe" or other IO errors here so the app doesn't crash
//                Log.w("BTManager", "Could not send START command (Socket might be closed): ${e.message}")
//            } catch (e: Exception) {
//                Log.e("BTManager", "Unexpected error in startTest", e)
//            }
//
//            val buffer = mutableListOf<Byte>()
//            val tempBuffer = ByteArray(2048) // Increased buffer size slightly
//            val dataInputStream = DataInputStream(BufferedInputStream(inputStream))
//
//            // 1. Little Endian Float Converter
//            fun bytesToFloat(b0: Byte, b1: Byte): Float {
//                // 1. Combine two bytes into a 16-bit Signed Integer (Short)
//                // Using (b1 << 8 | b0) for Little Endian
//                val asShort = ((b1.toInt() and 0xFF) shl 8) or (b0.toInt() and 0xFF)
//
//                // 2. Convert to signed short (to handle negative values)
//                val signedShort = asShort.toShort()
//
//                // 3. Scale the value. Usually, IMU data is fixed-point (e.g., 1234 becomes 12.34)
//                return signedShort.toFloat() / 100.0f
//            }
//
//            // 2. CRC16 Logic (Ported from your C code)
//            fun computeCRC16(data: List<Byte>): Int {
//                var crc = 0xFFFF
//                for (byte in data) {
//                    crc = crc xor (byte.toInt() and 0xFF)
//                    for (j in 0 until 8) {
//                        crc = if ((crc and 0x0001) != 0) {
//                            (crc ushr 1) xor 0xA001
//                        } else {
//                            crc ushr 1
//                        }
//                    }
//                }
//                return crc
//            }
//
//            fun isValidFloat(value: Float): Boolean {
//                return !value.isNaN() && !value.isInfinite()
//            }
//
//            fun normalizeAndInvert(value: Float, targetZero: Float = 90f): Float {
//                // 1. Shift the angle
//                var shifted = value - targetZero
//
//                // 2. Wrap the value to keep it within -180 to 180
//                while (shifted <= -180f) {
//                    shifted += 360f
//                }
//                while (shifted > 180f) {
//                    shifted -= 360f
//                }
//
//                // 3. Invert the sign
//                return -shifted
//            }
//
//            fun normalizeLeftRightPitch(value: Float): Float {
//                // We pass -90f as the target zero
//                return normalizeAndInvert(value, -90f)
//            }
//
//            while (isActive) {
//                try {
//                    if (dataInputStream.available() > 0) {
//                        val bytesRead = dataInputStream.read(tempBuffer)
//                        if (bytesRead > 0) {
//                            buffer.addAll(tempBuffer.take(bytesRead))
//
//                            // Loop while we have at least one full packet size (58 bytes)
//                            while (buffer.size >= 58) {
//
//                                // Check Header at index 0 (0xAA)
//                                if (buffer[0] == 0xAA.toByte()) {
//                                    // Check Footer at index 75 (0xBB)
//                                    if (buffer[57] == 0xBB.toByte()) {
//
//                                        val hexString = buffer.take(58).joinToString(" ") {
//                                            String.format("%02X", it)
//                                        }
//                                        Log.d("FullData", "Raw Bytes: $hexString")
//
//                                        // 1. Extract Data Section (Indices 1 to 54)
//                                        val dataPacket = buffer.subList(1, 55) // 54 bytes
//
//                                        // 2. Extract Received CRC (Indices 55, 56)
//                                        val crcLow = buffer[55].toInt() and 0xFF
//                                        val crcHigh = buffer[56].toInt() and 0xFF
//                                        val receivedCRC = (crcHigh shl 8) or crcLow
//
//                                        // 3. Verify CRC
//                                        val calculatedCRC = computeCRC16(dataPacket)
//
//                                        if (calculatedCRC == receivedCRC) {
//                                            // --- PARSING DATA ---
//
//                                            // Helper to slice 4 bytes safely
//                                            fun getFloatAt(index: Int): Float {
//                                                // index is relative to dataPacket list (starts at 0)
//                                                return bytesToFloat(
//                                                    dataPacket[index],
//                                                    dataPacket[index+1],
//                                                )
//                                            }
//
//                                            // Right Sensor (0-17)
//                                            val rightPitch = normalizeLeftRightPitch(getFloatAt(0))
//                                            val rightYaw  = getFloatAt(2)
//                                            val rightRoll   = getFloatAt(4)
//                                            val rightAccX  = getFloatAt(6)*2
//                                            val rightAccY  = getFloatAt(8)*2
//                                            val rightAccZ  = getFloatAt(10)*2
//                                            val rightGyX = getFloatAt(12)
//                                            val rightGyY = getFloatAt(14)
//                                            val rightGyZ = getFloatAt(16)
//
//                                            // Left Sensor (18-35)
//                                            val leftPitch = normalizeLeftRightPitch(getFloatAt(18))
//                                            val leftYaw  = getFloatAt(20)
//                                            val leftRoll   = getFloatAt(22)
//                                            val leftAccX  = getFloatAt(24)*2
//                                            val leftAccY  = getFloatAt(26)*2
//                                            val leftAccZ  = getFloatAt(28)*2
//                                            val leftGyX = getFloatAt(30)
//                                            val leftGyY = getFloatAt(32)
//                                            val leftGyZ = getFloatAt(34)
//
//                                            // Center Sensor (36,53)
//                                            val centerPitch = normalizeAndInvert(getFloatAt(36))
//                                            val centerYaw  = getFloatAt(38)
//                                            val centerRoll   = getFloatAt(40)
//                                            val centerAccX  = getFloatAt(42)*2
//                                            val centerAccY  = getFloatAt(44)*2
//                                            val centerAccZ  = getFloatAt(46)*2
//                                            val centerGyX = getFloatAt(48)
//                                            val centerGyY = getFloatAt(50)
//                                            val centerGyZ = getFloatAt(52)
//
//                                            Log.d("RightData","accX:$rightAccX,accY:$rightAccY,accZ:$rightAccZ, gyX: $rightGyX, gyY: $rightGyY, gyZ: $rightGyZ")
//                                            Log.d("CenterData","accX:$centerAccX,accY:$centerAccY,accZ:$centerAccZ, gyX: $centerGyX, gyY: $centerGyY, gyZ: $centerGyZ")
//                                            Log.d("LeftData","accX:$leftAccX,accY:$leftAccY,accZ:$leftAccZ, gyX: $leftGyX, gyY: $leftGyY, gyZ: $leftGyZ")
//
//                                            // Update Flow
//                                            if (isValidFloat(leftPitch) && isValidFloat(leftRoll) && isValidFloat(leftYaw) && isValidFloat(leftAccX) && isValidFloat(leftAccY) && isValidFloat(leftAccZ) && isValidFloat(leftGyX) && isValidFloat(leftGyY) && isValidFloat(leftGyZ) &&
//                                                isValidFloat(rightPitch) && isValidFloat(rightRoll) && isValidFloat(rightYaw) && isValidFloat(rightAccX) && isValidFloat(rightAccY) && isValidFloat(rightAccZ) && isValidFloat(rightGyX) && isValidFloat(rightGyY) && isValidFloat(rightGyZ) &&
//                                                isValidFloat(centerPitch) && isValidFloat(centerRoll) && isValidFloat(centerYaw) && isValidFloat(centerAccX) && isValidFloat(centerAccY) && isValidFloat(centerAccZ) && isValidFloat(centerGyX) && isValidFloat(centerGyY) && isValidFloat(centerGyZ)) {
//
//                                                _sensorDataFlow.value = SensorData(
//                                                    leftPitch, leftRoll, -leftYaw,
//                                                    rightPitch, rightRoll, -rightYaw,
//                                                    centerPitch, centerRoll, -centerYaw,
//                                                    leftAccX, leftAccY, leftAccZ,
//                                                    rightAccX, rightAccY, rightAccZ,
//                                                    centerAccX, centerAccY, centerAccZ,
//                                                    leftGyX, leftGyY, leftGyZ,
//                                                    rightGyX, rightGyY, rightGyZ,
//                                                    centerGyX, centerGyY, centerGyZ
//                                                )
//
//                                                // Log Acc if needed, or add to SensorData if you updated the class
//                                                // Log.d("BT_ACC", "L:$leftAccX R:$rightAccX C:$centerAccX")
//                                            }
//
//                                        } else {
//                                            Log.e("BTManager", "CRC Mismatch! Calc: ${Integer.toHexString(calculatedCRC)} != Recv: ${Integer.toHexString(receivedCRC)}")
//                                        }
//
//                                        // Clear processed packet
//                                        buffer.subList(0, 58).clear()
//
//                                    } else {
//                                        // Header found but Footer missing at expected index.
//                                        // This indicates misalignment. Drop first byte and retry.
//                                        buffer.removeAt(0)
//                                    }
//                                } else {
//                                    // First byte is not header. Remove it.
//                                    buffer.removeAt(0)
//                                }
//                            }
//                        }
//                    } else {
//                        delay(10)
//                    }
//                } catch (e: Exception) {
//                    Log.e("BTManager", "Error: ${e.message}")
//                    if (!isActive) break
//                }
//            }
//        }
//    }

    override fun stopTest() {
        scope.launch(Dispatchers.IO) { // 1. Ensure this runs on the IO thread
            try {
                val outputStream = socket?.outputStream ?: return@launch

                // Step 1: Send STOP Test command
                val stopCommand = byteArrayOf(
                    0xAA.toByte(),
                    0xA0.toByte(),
                    0x55,
                    0x00,
                    0x00,
                    0x00,
                    0x00
                )

                // 2. Writing safely
                if (socket?.isConnected == true) {
                    outputStream.write(stopCommand)
                    outputStream.flush()
                }
            } catch (e: IOException) {
                // 3. Catch "Broken pipe" or other IO errors here so the app doesn't crash
                Log.w("BTManager", "Could not send STOP command (Socket might be closed): ${e.message}")
            } catch (e: Exception) {
                Log.e("BTManager", "Unexpected error in stopTest", e)
            }
        }
    }

    override fun resetSensorData(){
        _sensorDataFlow.value=null
    }

    override fun sendSaveCalibrationCommand() {
        scope.launch {
            try {
                val outputStream = socket?.outputStream ?: return@launch
                val inputStream = socket?.inputStream ?: return@launch

                val saveCommand = listOf(0xAA, 0xFF, 0x55, 0x00, 0x00, 0x00, 0x00)
                for (byte in saveCommand) outputStream.write(byte)
                outputStream.flush()
                Log.d("BTManager", "Save calibration command sent")

                delay(300)

                val responseByte = inputStream.read()
                if (responseByte == 0xFF) {
                    Log.d("BTManager", "Calibration saved on device successfully")
                } else {
                    Log.w("BTManager", "Unexpected response to save command: 0x${responseByte.toString(16)}")
                }

            } catch (e: Exception) {
                Log.e("BTManager", "Failed to send save calibration command: ${e.message}")
            }
        }
    }

    override fun resetCalibrationData() {
        _standingFlow.value = null
        _sittingFlow.value = null
        Log.d("BTManager", "Calibration data reset.")
    }

    override fun stop() {
        scope.cancel()
        disconnect()
    }
}