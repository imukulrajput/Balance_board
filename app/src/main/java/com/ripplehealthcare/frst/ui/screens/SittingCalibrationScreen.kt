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
import com.ripplehealthcare.frst.ui.components.* // Import your new component
import com.ripplehealthcare.frst.ui.theme.PrimaryColor
import com.ripplehealthcare.frst.ui.viewmodel.BluetoothViewModel
import com.ripplehealthcare.frst.ui.viewmodel.TestViewModel
import kotlinx.coroutines.delay

@SuppressLint("DefaultLocale")
@Composable
fun SittingCalibrationScreen(navController: NavController, viewModel: BluetoothViewModel,testViewModel: TestViewModel) {
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
    // --- DIALOG STATE ---
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

    // 1. Data Collection Effect
    LaunchedEffect(sensorData) {
        if (isCalibrating && sensorData != null) {
            dataBuffer.add(sensorData!!)
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
//                    navController.popBackStack()
                }else{
                    val medianData = calculateMedianSensorData(dataBuffer)
                    if (medianData.leftPitch > -60 || medianData.rightPitch > -60) {
                        calculatedCalibration = null // Reset so we don't show invalid results
                        showDialog(
                            title = "Calibration Failed",
                            message = "Incorrect posture detected. Please sit correctly.",
                            type = DialogType.ERROR
                        )
                    } else {
                        // Success case
                        calculatedCalibration = medianData
                        testViewModel.saveSittingCalibration(calculatedCalibration!!)
                    }
                }
            } else {
                showDialog("No Data", "No sensor data was received during the 3 seconds.", DialogType.ERROR)
            }
        }
    }
        Scaffold(
            topBar = {
                TopBar("Sitting Calibration", onBackClick = {
                    if (!isCalibrating) {
                        navController.popBackStack()
                    }
                })
            },
            bottomBar = {
                CalibrationBottomBar(
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
                                message = "Your sitting calibration has been updated successfully.",
                                type = DialogType.SUCCESS
                            )
//                            navController.popBackStack()
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
                    painter = painterResource(id = R.drawable.sitting_pose),
                    contentDescription = "Sitting Pose",
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
                        else -> "Please sit normally on a sturdy chair with the phone held against your chest."
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
                            Text(
                                "L Pitch: ${
                                    String.format(
                                        "%.2f",
                                        calculatedCalibration?.leftPitch
                                    )
                                }"
                            )
                            Text(
                                "L Roll: ${
                                    String.format(
                                        "%.2f",
                                        calculatedCalibration?.leftRoll
                                    )
                                }"
                            )
                            Text(
                                "R Pitch: ${
                                    String.format(
                                        "%.2f",
                                        calculatedCalibration?.rightPitch
                                    )
                                }"
                            )
                            Text(
                                "R Roll: ${
                                    String.format(
                                        "%.2f",
                                        calculatedCalibration?.rightRoll
                                    )
                                }"
                            )
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