package com.ripplehealthcare.frst.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ripplehealthcare.frst.R
import com.ripplehealthcare.frst.data.model.SensorData
import com.ripplehealthcare.frst.domain.model.ConnectionState
import com.ripplehealthcare.frst.ui.components.CalibrationBottomBar
import com.ripplehealthcare.frst.ui.components.DialogState
import com.ripplehealthcare.frst.ui.components.DialogType
import com.ripplehealthcare.frst.ui.components.StatusDialog
import com.ripplehealthcare.frst.ui.components.TopBar
import com.ripplehealthcare.frst.ui.theme.PrimaryColor
import com.ripplehealthcare.frst.ui.viewmodel.BluetoothViewModel
import com.ripplehealthcare.frst.ui.viewmodel.TestViewModel
import kotlinx.coroutines.delay

@SuppressLint("DefaultLocale")
@Composable
fun StandingCalibrationScreen(navController: NavController, viewModel: BluetoothViewModel,testViewModel: TestViewModel) {
    // Data Observations
    val connectionState by viewModel.connectionState.collectAsState()
    val sensorData by viewModel.sensorData.collectAsState()

    // UI State
    var isCalibrating by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    // This holds the calculated median result locally before saving
    var calculatedCalibration by remember { mutableStateOf<SensorData?>(null) }

    // Buffer to record data during the 5s window
    val dataBuffer = remember { mutableStateListOf<SensorData>() }

    // Constants
    val calibrationDuration = 3000L // 3 seconds

    var dialogState by remember { mutableStateOf(DialogState()) }

    // Helper to show Dialog
    fun showDialog(title: String, message: String, type: DialogType) {
        dialogState = DialogState(
            isVisible = true,
            title = title,
            message = message,
            type = type
        )
    }

    // Dismiss Logic
    fun dismissDialog() {
        // capture flags before resetting state
        val shouldNavigateBack = dialogState.title == "Calibration Saved!" ||
                dialogState.title == "Connection Lost"

        dialogState = dialogState.copy(isVisible = false)

        if (shouldNavigateBack) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(connectionState) {
        when (connectionState) {
            ConnectionState.DISCONNECTED -> {
                showDialog("Connection Lost", "The device has disconnected unexpectedly.", DialogType.ERROR)
//                navController.popBackStack()
            }
            else -> Unit // No action for other states
        }
    }

    // 1. Data Collection Effect
    LaunchedEffect(sensorData) {
        if (isCalibrating && sensorData != null) {
            dataBuffer.add(sensorData!!)
        }
    }

    // Ensure we stop the test if the user navigates away mid-calibration
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopTest()
        }
    }

    // 2. Timer & Logic Effect
    LaunchedEffect(isCalibrating) {
        if (isCalibrating) {
            // Start receiving data
            viewModel.startTest()

            // Reset
            dataBuffer.clear()
            progress = 0f

            val startTime = System.currentTimeMillis()

            // Loop for duration
            while (System.currentTimeMillis() - startTime < calibrationDuration) {
                val elapsed = System.currentTimeMillis() - startTime
                progress = elapsed / calibrationDuration.toFloat()
                delay(16) // ~60 FPS update
            }

            // Stop receiving data
            viewModel.stopTest()

            // Finished
            progress = 1f
            isCalibrating = false

            if (dataBuffer.isNotEmpty()) {
                if(dataBuffer.size<40) {
                    showDialog(
                        title = "Calibration Failed",
                        message = "We only received ${dataBuffer.size} samples. Ensure the device is on and nearby.",
                        type = DialogType.ERROR
                    )
                }else{
                    val medianData = calculateMedianSensorData(dataBuffer)
                    if (medianData.leftPitch < -15 || medianData.rightPitch < -15) {
                        calculatedCalibration = null
                        showDialog(
                            title = "Calibration Failed",
                            message = "Incorrect posture detected. Please stand correctly.",
                            type = DialogType.ERROR
                        )
                    } else {
                        // Success case
                        calculatedCalibration = medianData
                        testViewModel.saveStandingCalibration(calculatedCalibration!!)
                    }
                }

            } else {
                showDialog("No Data", "No sensor data was received during the 3 seconds.", DialogType.ERROR)
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar("Standing Calibration", onBackClick = {
                if (!isCalibrating) {
                    navController.popBackStack()
                }
            })
        },
        bottomBar = {
            CalibrationBottomBar(
                // We consider it "calibrated" if we have a calculated result
                isCalibrated = calculatedCalibration != null,
                onStartClick = {
                    if (connectionState == ConnectionState.CONNECTED) {
                        if (!isCalibrating) {
                            isCalibrating = true
                            calculatedCalibration = null // Reset previous result
                        }
                    } else {
                        showDialog("Device Not Found", "Please connect your Bluetooth device before starting.", DialogType.INFO)
                    }
                },
                onSaveClick = {
                    if (calculatedCalibration != null && connectionState == ConnectionState.CONNECTED) {
                        viewModel.sendSaveCalibration() // Persist it
                        showDialog(
                            title = "Calibration Saved!",
                            message = "Your standing calibration has been updated successfully.",
                            type = DialogType.SUCCESS
                        )
                    } else {
                        showDialog("Cannot Save", "Calibration is incomplete or device is disconnected.", DialogType.ERROR)
                    }
                }
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.standing_pose),
                contentDescription = "Standing Pose",
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(1f),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Dynamic Instructions
            Text(
                text = when {
                    isCalibrating -> "Hold still! Recording data..."
                    calculatedCalibration != null -> "Calibration Captured!\nPress Save to confirm."
                    else -> "Please stand normally and ensure the phone is held firmly against your chest."
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isCalibrating) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = PrimaryColor,
                    trackColor = Color.LightGray,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            } else if (calculatedCalibration != null) {
                // Show collected stats
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Result (Median):", style = MaterialTheme.typography.titleSmall)
                        Text("L Pitch: ${String.format("%.2f", calculatedCalibration?.leftPitch)}")
                        Text("L Roll: ${String.format("%.2f", calculatedCalibration?.leftRoll)}")
                        Text("R Pitch: ${String.format("%.2f", calculatedCalibration?.rightPitch)}")
                        Text("R Roll: ${String.format("%.2f", calculatedCalibration?.rightRoll)}")
                    }
                }
            }
        }
        StatusDialog(
            state = dialogState,
            onDismissRequest = { dismissDialog() }
        )
    }
}

fun calculateMedianSensorData(dataList: List<SensorData>): SensorData {
    // Return empty/zero object if no data
    if (dataList.isEmpty()) return SensorData(
        0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, // Orientation
        0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f,  // Acceleration
        0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f  // Gyro
    )

    fun getMedian(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[middle]
        } else {
            (sorted[middle - 1] + sorted[middle]) / 2f
        }
    }

    // 1. Extract Orientation Lists
    val leftPitches = dataList.map { it.leftPitch }
    val leftRolls = dataList.map { it.leftRoll }
    val leftYaws = dataList.map { it.leftYaw }

    val rightPitches = dataList.map { it.rightPitch }
    val rightRolls = dataList.map { it.rightRoll }
    val rightYaws = dataList.map { it.rightYaw }

    val centerPitches = dataList.map { it.centerPitch }
    val centerRolls = dataList.map { it.centerRoll }
    val centerYaws = dataList.map { it.centerYaw }

    // 2. Extract Acceleration Lists
    // (Assuming field names match your SensorData model)
    val leftAccXs = dataList.map { it.leftAccX }
    val leftAccYs = dataList.map { it.leftAccY }
    val leftAccZs = dataList.map { it.leftAccZ }

    val rightAccXs = dataList.map { it.rightAccX }
    val rightAccYs = dataList.map { it.rightAccY }
    val rightAccZs = dataList.map { it.rightAccZ }

    val centerAccXs = dataList.map { it.centerAccX }
    val centerAccYs = dataList.map { it.centerAccY }
    val centerAccZs = dataList.map { it.centerAccZ }

    val leftGyXs = dataList.map { it.leftGyX }
    val leftGyYs = dataList.map { it.leftGyY }
    val leftGyZs = dataList.map { it.leftGyZ }

    val rightGyXs = dataList.map { it.rightGyX }
    val rightGyYs = dataList.map { it.rightGyY }
    val rightGyZs = dataList.map { it.rightGyZ }

    val centerGyXs = dataList.map { it.centerGyX }
    val centerGyYs = dataList.map { it.centerGyY }
    val centerGyZs = dataList.map { it.centerGyZ }

    // 3. Create SensorData with ALL Medians
    return SensorData(
        // Orientation
        leftPitch = getMedian(leftPitches),
        leftRoll = getMedian(leftRolls),
        leftYaw = getMedian(leftYaws),
        rightPitch = getMedian(rightPitches),
        rightRoll = getMedian(rightRolls),
        rightYaw = getMedian(rightYaws),
        centerPitch = getMedian(centerPitches),
        centerRoll = getMedian(centerRolls),
        centerYaw = getMedian(centerYaws),

        // Acceleration (Left)
        leftAccX = getMedian(leftAccXs),
        leftAccY = getMedian(leftAccYs),
        leftAccZ = getMedian(leftAccZs),

        // Acceleration (Right)
        rightAccX = getMedian(rightAccXs),
        rightAccY = getMedian(rightAccYs),
        rightAccZ = getMedian(rightAccZs),

        // Acceleration (Center)
        centerAccX = getMedian(centerAccXs),
        centerAccY = getMedian(centerAccYs),
        centerAccZ = getMedian(centerAccZs),

        // Gyro (Left)
        leftGyX = getMedian(leftGyXs),
        leftGyY = getMedian(leftGyYs),
        leftGyZ = getMedian(leftGyZs),

        // Gyro (Right)
        rightGyX = getMedian(rightGyXs),
        rightGyY = getMedian(rightGyYs),
        rightGyZ = getMedian(rightGyZs),

        // Gyro (Center)
        centerGyX = getMedian(centerGyXs),
        centerGyY = getMedian(centerGyYs),
        centerGyZ = getMedian(centerGyZs)
    )
}