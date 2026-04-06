package com.ripplehealthcare.bproboard.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ripplehealthcare.bproboard.domain.algo.TugAnalyzer
import com.ripplehealthcare.bproboard.domain.algo.TugCalibrator
import com.ripplehealthcare.bproboard.domain.algo.TugResult
import com.ripplehealthcare.bproboard.domain.model.TugThresholds
import com.ripplehealthcare.bproboard.ui.viewmodel.BluetoothViewModel
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Enum to define which TUG phase we are capturing
enum class TugPhase {
    IDLE,
    STANDING,   // Calibration Phase 1
    WALKING,    // Calibration Phase 2
    SITTING,    // Calibration Phase 3
    TEST_RUN    // Actual TUG Test
}

// Data wrapper for CSV rows
data class RecordedDataPoint(
    val timestamp: Long,
    val phase: String,
    val sensorData: com.ripplehealthcare.bproboard.data.model.SensorData
)

@Composable
fun WalkCalibrationScreen(navController: NavController, viewModel: BluetoothViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Observe the data stream
    val sensorData by viewModel.sensorData.collectAsState()

    // UI State
    var currentPhase by remember { mutableStateOf(TugPhase.IDLE) }
    var isRecording by remember { mutableStateOf(false) }

    // Result State
    var tugThresholds by remember { mutableStateOf<TugThresholds?>(null) }
    var lastTugResult by remember { mutableStateOf<TugResult?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }

    // Buffers for Calibration Data
    val sittingData = remember { mutableStateListOf<RecordedDataPoint>() }
    val standingData = remember { mutableStateListOf<RecordedDataPoint>() }
    val walkingData = remember { mutableStateListOf<RecordedDataPoint>() }

    // Buffer for actual Test Run
    val testRunData = remember { mutableStateListOf<RecordedDataPoint>() }

    // 1. Data Collection Effect
    LaunchedEffect(sensorData) {
        sensorData?.let { data ->
            if (isRecording && currentPhase != TugPhase.IDLE) {
                val point = RecordedDataPoint(
                    timestamp = System.currentTimeMillis(),
                    phase = currentPhase.name,
                    sensorData = data
                )

                // Route data to correct buffer based on phase
                when (currentPhase) {
                    TugPhase.SITTING -> sittingData.add(point)
                    TugPhase.STANDING -> standingData.add(point)
                    TugPhase.WALKING -> walkingData.add(point)
                    TugPhase.TEST_RUN -> testRunData.add(point)
                    else -> {}
                }
            }
        }
    }

    // 2. Cleanup on Exit
    DisposableEffect(Unit) {
        onDispose {
            if (isRecording) viewModel.stopTest()
        }
    }

    // 3. Result Dialog
    if (showResultDialog && lastTugResult != null) {
        AlertDialog(
            onDismissRequest = { showResultDialog = false },
            title = { Text("TUG Test Result") },
            text = {
                Column {
                    Text("Time: ${String.format("%.2f", lastTugResult!!.totalTimeSeconds)} s", style = MaterialTheme.typography.titleLarge)
                    Text("Steps: ${lastTugResult!!.stepCount}")
                    Text("Status: ${lastTugResult!!.status}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Thresholds Used:", fontWeight = FontWeight.Bold)
                    tugThresholds?.let {
                        Text("Posture: ${String.format("%.1f", it.postureThreshold)}")
                        Text("Move Energy: ${String.format("%.1f", it.movementThreshold)}")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showResultDialog = false }) { Text("Close") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("TUG Calibration & Test", style = MaterialTheme.typography.headlineMedium)

        // --- STATUS CARD ---
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isRecording) Color(0xFFFFEBEE) else Color(0xFFE3F2FD)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Status: ${if (isRecording) "REC: $currentPhase" else "IDLE"}")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Collected Data Points:", fontWeight = FontWeight.Bold)
                Text("Sit: ${sittingData.size} | Stand: ${standingData.size} | Walk: ${walkingData.size}")
                Text("Test Run: ${testRunData.size}")

                if (tugThresholds != null) {
                    Text("✅ Calibration Ready", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                } else {
                    Text("⚠️ Calibration Needed", color = Color(0xFFEF6C00))
                }
            }
        }

        Divider()

        // --- STEP 1: CALIBRATION ---
        Text("Step 1: Record Calibration Data", style = MaterialTheme.typography.titleMedium)

        PhaseToggleButton(
            label = "1. SITTING (3s)",
            isActive = currentPhase == TugPhase.SITTING && isRecording,
            enabled = !isRecording || currentPhase == TugPhase.SITTING,
            onClick = {
                toggleRecording(
                    targetPhase = TugPhase.SITTING,
                    currentPhase = currentPhase,
                    isRecording = isRecording,
                    onStart = { viewModel.startTest(); sittingData.clear() },
                    onStop = { viewModel.stopTest() },
                    onStateChange = { p, r -> currentPhase = p; isRecording = r }
                )
            }
        )

        PhaseToggleButton(
            label = "2. STANDING (3s)",
            isActive = currentPhase == TugPhase.STANDING && isRecording,
            enabled = !isRecording || currentPhase == TugPhase.STANDING,
            onClick = {
                toggleRecording(
                    targetPhase = TugPhase.STANDING,
                    currentPhase = currentPhase,
                    isRecording = isRecording,
                    onStart = { viewModel.startTest(); standingData.clear() },
                    onStop = { viewModel.stopTest() },
                    onStateChange = { p, r -> currentPhase = p; isRecording = r }
                )
            }
        )

        PhaseToggleButton(
            label = "3. WALKING (5s)",
            isActive = currentPhase == TugPhase.WALKING && isRecording,
            enabled = !isRecording || currentPhase == TugPhase.WALKING,
            onClick = {
                toggleRecording(
                    targetPhase = TugPhase.WALKING,
                    currentPhase = currentPhase,
                    isRecording = isRecording,
                    onStart = { viewModel.startTest(); walkingData.clear() },
                    onStop = { viewModel.stopTest() },
                    onStateChange = { p, r -> currentPhase = p; isRecording = r }
                )
            }
        )

        // --- STEP 2: CALCULATE THRESHOLDS ---
        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
            enabled = sittingData.isNotEmpty() && standingData.isNotEmpty() && walkingData.isNotEmpty() && !isRecording,
            onClick = {
                try {
                    tugThresholds = TugCalibrator.calculateThresholds(sittingData, standingData, walkingData)
                    Toast.makeText(context, "Thresholds Calculated!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            Text("Step 2: Calculate Thresholds")
        }

        Divider()

        // --- STEP 3: RUN TEST ---
        Text("Step 3: Perform TUG Test", style = MaterialTheme.typography.titleMedium)

        PhaseToggleButton(
            label = "START TUG TEST",
            isActive = currentPhase == TugPhase.TEST_RUN && isRecording,
            enabled = (!isRecording || currentPhase == TugPhase.TEST_RUN) && tugThresholds != null,
            onClick = {
                toggleRecording(
                    targetPhase = TugPhase.TEST_RUN,
                    currentPhase = currentPhase,
                    isRecording = isRecording,
                    onStart = {
                        testRunData.clear()
                        viewModel.startTest()
                    },
                    onStop = {
                        viewModel.stopTest()
                        // Auto-Analyze when stopped
                        if (testRunData.isNotEmpty() && tugThresholds != null) {
                            val analyzer = TugAnalyzer()
                            lastTugResult = analyzer.analyzeTug(testRunData, tugThresholds!!)
                            showResultDialog = true
                        }
                    },
                    onStateChange = { p, r -> currentPhase = p; isRecording = r }
                )
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        // --- UTILS ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(
                onClick = {
                    sittingData.clear(); standingData.clear(); walkingData.clear(); testRunData.clear()
                    tugThresholds = null
                    lastTugResult = null
                },
                enabled = !isRecording
            ) {
                Text("Reset All")
            }

            OutlinedButton(
                onClick = {
                    val allData = sittingData + standingData + walkingData + testRunData
                    if (allData.isNotEmpty()) saveToCsv(context, allData)
                },
                enabled = !isRecording
            ) {
                Text("Export All CSV")
            }
        }
    }
}

// --- Components & Logic ---

@Composable
fun PhaseToggleButton(
    label: String,
    isActive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) Color.Red else MaterialTheme.colorScheme.primary,
            disabledContainerColor = Color.LightGray
        )
    ) {
        Text(if (isActive) "STOP & ANALYZE" else label)
    }
}

fun toggleRecording(
    targetPhase: TugPhase,
    currentPhase: TugPhase,
    isRecording: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onStateChange: (TugPhase, Boolean) -> Unit
) {
    if (isRecording && currentPhase == targetPhase) {
        onStop()
        onStateChange(TugPhase.IDLE, false)
    } else {
        onStart()
        onStateChange(targetPhase, true)
    }
}

fun saveToCsv(context: Context, data: List<RecordedDataPoint>) {
    try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Full_TUG_Session_$timeStamp.csv"
        val file = File(context.getExternalFilesDir(null), fileName)
        val writer = FileWriter(file)

        writer.append("Timestamp,Phase,LeftPitch,LeftRoll,RightPitch,RightRoll,LeftAccX,LeftAccY,LeftAccZ,RightAccX,RightAccY,RightAccZ\n")

        data.forEach { point ->
            val sd = point.sensorData
            writer.append("${point.timestamp},${point.phase},")
            writer.append("${sd.leftPitch},${sd.leftRoll},${sd.rightPitch},${sd.rightRoll},${sd.leftAccX},${sd.leftAccY},${sd.leftAccZ},${sd.rightAccX},${sd.rightAccY},${sd.rightAccZ}\n")
        }

        writer.flush()
        writer.close()
        Toast.makeText(context, "Saved: ${file.name}", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}