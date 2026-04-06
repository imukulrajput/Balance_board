package com.ripplehealthcare.bproboard.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ripplehealthcare.bproboard.domain.model.AccPoint
import com.ripplehealthcare.bproboard.domain.model.ConnectionState
import com.ripplehealthcare.bproboard.domain.model.FiveRepResult
import com.ripplehealthcare.bproboard.domain.model.TestState
import com.ripplehealthcare.bproboard.domain.model.TestType
import com.ripplehealthcare.bproboard.domain.model.ThirtySecResult
import com.ripplehealthcare.bproboard.ui.components.AccelerationGraph
import com.ripplehealthcare.bproboard.ui.components.DialogState
import com.ripplehealthcare.bproboard.ui.components.DialogType
import com.ripplehealthcare.bproboard.ui.components.SavingDialog
import com.ripplehealthcare.bproboard.ui.components.StatusDialog
import com.ripplehealthcare.bproboard.ui.components.TopBar
import com.ripplehealthcare.bproboard.ui.theme.PrimaryColor
import com.ripplehealthcare.bproboard.ui.theme.WhiteColor
import com.ripplehealthcare.bproboard.ui.viewmodel.BluetoothViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.PatientViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel

private const val ACC_RANGE = 4.0f // Scale for STS graph

@SuppressLint("DefaultLocale")
@Composable
fun SitToStandTestScreen(
    navController: NavController,
    viewModel: BluetoothViewModel,
    testViewModel: TestViewModel,
    patientViewModel: PatientViewModel
) {
    val connectionState by viewModel.connectionState.collectAsState()

    // 1. Observe State
    val testState by testViewModel.testState.collectAsState()
    val currentTestType by testViewModel.currentTestType.collectAsState()
    val timerValue by testViewModel.timer.collectAsState()
    val sensorData by viewModel.sensorData.collectAsState()

    val isSaving by testViewModel.isSaving.collectAsState()
    val isSavedToBackend by testViewModel.isSavedToBackend.collectAsState()
    val patient by patientViewModel.selectedPatient.collectAsState()

    // Determine which rep counter to use based on test type
    val totalReps by if (currentTestType == TestType.FIVE_REPS) {
        testViewModel.fiveRepsTotalReps
    } else {
        testViewModel.thirtySecondsTotalReps
    }.collectAsState()

    // 2. Data Containers (History & Live Graphs)
    val graphLeft = remember { mutableStateListOf<AccPoint>() }
    val graphRight = remember { mutableStateListOf<AccPoint>() }
    val graphCenter = remember { mutableStateListOf<AccPoint>() }

    // History for saving to DB (Not state, just raw lists)
    val accHistoryLeft = remember { mutableListOf<AccPoint>() }
    val accHistoryRight = remember { mutableListOf<AccPoint>() }
    val accHistoryCenter = remember { mutableListOf<AccPoint>() }

    val sittingCalib by testViewModel.sittingCalibration.collectAsState()

    // 3. Calibration
//    val standingCalib by testViewModel.standingCalibration.collectAsState()
//    val baseLAccX = standingCalib?.leftAccX ?: 0f
//    val baseLAccY = standingCalib?.leftAccY ?: 0f
//    val baseLAccZ = standingCalib?.leftAccZ ?: 0f
//    val baseRAccX = standingCalib?.rightAccX ?: 0f
//    val baseRAccY = standingCalib?.rightAccY ?: 0f
//    val baseRAccZ = standingCalib?.rightAccZ ?: 0f
//    val baseCAccX = standingCalib?.centerAccX ?: 0f
//    val baseCAccY = standingCalib?.centerAccY ?: 0f
//    val baseCAccZ = standingCalib?.centerAccZ ?: 0f

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
        val shouldNavigateBack = dialogState.title == "Connection Lost"
        dialogState = dialogState.copy(isVisible = false)
        if (shouldNavigateBack) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(connectionState) {
        when (connectionState) {
            ConnectionState.DISCONNECTED -> {
                if(testState==TestState.RUNNING){
                    testViewModel.cancelTest()
                }
                showDialog("Connection Lost", "The device has disconnected unexpectedly.", DialogType.ERROR)
            }
            else -> Unit
        }
    }

    // 4. Lifecycle: Reset on Entry
    LaunchedEffect(Unit) {
        testViewModel.resetTestData(keepTestType = true)
        graphLeft.clear(); graphRight.clear(); graphCenter.clear()
        accHistoryLeft.clear(); accHistoryRight.clear(); accHistoryCenter.clear()
    }

    // 5. Data Processing Loop
    LaunchedEffect(sensorData, testState) {
        sensorData?.let { data ->
            // A. Update Logic (Rep counting)
            if (testState == TestState.RUNNING) {
                testViewModel.onNewAngleReceived(data.leftPitch, data.rightPitch)
            }

            // B. Prepare Data Points (Zeroed against calibration)
//            val cX = data.centerAccX - baseCAccX; val cY = data.centerAccY - baseCAccY; val cZ = data.centerAccZ - baseCAccZ
//            val lX = data.leftAccX - baseLAccX;   val lY = data.leftAccY - baseLAccY;   val lZ = data.leftAccZ - baseLAccZ
//            val rX = data.rightAccX - baseRAccX;  val rY = data.rightAccY - baseRAccY;  val rZ = data.rightAccZ - baseRAccZ

            val cX = data.centerAccX; val cY = data.centerAccY; val cZ = data.centerAccZ
            val lX = data.leftAccX;   val lY = data.leftAccY;   val lZ = data.leftAccZ
            val rX = data.rightAccX;  val rY = data.rightAccY;  val rZ = data.rightAccZ

            val now = System.currentTimeMillis()

            // C. Accumulate History (ONLY when test is RUNNING)
            if (testState == TestState.RUNNING) {
                // Update Live Graphs
                addPointToGraph(graphCenter, cX, cY, cZ)
                addPointToGraph(graphLeft, lX, lY, lZ)
                addPointToGraph(graphRight, rX, rY, rZ)

                // Add to History
                accHistoryCenter.add(AccPoint(cX, cY, cZ, now))
                accHistoryLeft.add(AccPoint(lX, lY, lZ, now))
                accHistoryRight.add(AccPoint(rX, rY, rZ, now))
            }
        }
    }

    // 6. Auto-Stop Logic
    LaunchedEffect(totalReps, timerValue, testState) {
        if (testState == TestState.RUNNING) {
            val fiveRepsDone = currentTestType == TestType.FIVE_REPS && totalReps >= 5
            val thirtySecondsDone = currentTestType == TestType.THIRTY_SECONDS && timerValue <= 0

            if (fiveRepsDone || thirtySecondsDone) {
                testViewModel.stopTest()
            }
        }
    }

    // 7. Save & Complete Logic (UPDATED FOR SESSION)
    LaunchedEffect(Unit) {
        testViewModel.navigateToResultEvent.collect {
            // Test Stopped -> Generate Results
            val fiveRepTimes = testViewModel.fiveRepsTimes.value
            val thirtySecTimes = testViewModel.thirtySecondsTimes.value

            if (currentTestType == TestType.FIVE_REPS) {
                val repStats = testViewModel.generateRepetitionStats(
                    repDurations = fiveRepTimes,
                    accCenter = accHistoryCenter,
                    accLeft = accHistoryLeft,
                    accRight = accHistoryRight
                )
                val result = FiveRepResult(
                    totalTimeSeconds = timerValue.toFloat(), // Timer counts UP for 5-reps
                    repetitionStats = repStats,
                    repTimes = fiveRepTimes,
                    calAccLeft = sittingCalib?.let { AccPoint(it.leftAccX, it.leftAccY, it.leftAccZ) }
                        ?: AccPoint(0f,0f,0f),
                    calAccRight = sittingCalib?.let { AccPoint(it.rightAccX, it.rightAccY, it.rightAccZ) }
                        ?: AccPoint(0f,0f,0f),
                    accLeft = accHistoryLeft.toList(),
                    accRight = accHistoryRight.toList(),
                    accCenter = accHistoryCenter.toList()
                )

                patient?.let {
                    testViewModel.saveFiveRepResult(it.centerId,
                        it.doctorId,result)
                }

            } else if (currentTestType == TestType.THIRTY_SECONDS) {
                val repStats = testViewModel.generateRepetitionStats(
                    repDurations = thirtySecTimes,
                    accCenter = accHistoryCenter,
                    accLeft = accHistoryLeft,
                    accRight = accHistoryRight
                )
                val result = ThirtySecResult(
                    totalRepetitions = totalReps,
                    repetitionStats = repStats,
                    repTimestamps = thirtySecTimes,
                    accLeft = accHistoryLeft.toList(),
                    accRight = accHistoryRight.toList(),
                    accCenter = accHistoryCenter.toList()
                )

                patient?.let { testViewModel.saveThirtySecResult(it.centerId, it.doctorId,result) }
            }

        }
    }

    LaunchedEffect(isSavedToBackend) {
        if (isSavedToBackend) {
            // 1. Reset data now that we are safe
            testViewModel.resetTestData(keepTestType = true)

            // 2. Navigate back to Dashboard
            navController.popBackStack("sessionDashboard", inclusive = false)
        }
    }

    // --- UI Structure ---
    Scaffold(
        topBar = {
            TopBar(
                title = if (currentTestType == TestType.FIVE_REPS) "5-Rep Sit to Stand" else "30-Sec Sit to Stand",
                onBackClick = { navController.popBackStack() }
            )
        },
        bottomBar = {
            if (testState != TestState.COMPLETED) {
                Surface(shadowElevation = 8.dp, color = Color.White) {
                    Button(
                        onClick = {
                            if (testState == TestState.RUNNING) {
                                testViewModel.cancelTest()
                                graphLeft.clear(); graphRight.clear(); graphCenter.clear()
                                accHistoryLeft.clear(); accHistoryRight.clear(); accHistoryCenter.clear()
                            } else {
                                // Clear & Start
                                graphLeft.clear(); graphRight.clear(); graphCenter.clear()
                                accHistoryLeft.clear(); accHistoryRight.clear(); accHistoryCenter.clear()
                                testViewModel.startTest()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(16.dp, 20.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (testState == TestState.RUNNING) Color(0xFFD32F2F) else PrimaryColor,
                            contentColor = WhiteColor
                        )
                    ) {
                        Text(
                            text = if (testState == TestState.RUNNING) "Cancel Test" else "Start Test",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFFF8F9FA)
    ) { innerPadding ->
        AnimatedContent(
            targetState = testState,
            transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(500)) },
            modifier = Modifier.padding(innerPadding),
            label = "StsStateAnimation"
        ) { state ->
            if (state == TestState.COMPLETED) {
                // Should navigate away quickly, but shows completion momentarily
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryColor)
                }
            } else {
                StsLiveContent(
                    timerValue = timerValue,
                    reps = totalReps,
                    testType = currentTestType,
                    graphLeft = graphLeft,
                    graphRight = graphRight,
                    graphCenter = graphCenter
                )
            }
        }
        StatusDialog(
            state = dialogState,
            onDismissRequest = { dismissDialog() }
        )
        SavingDialog(isSaving = isSaving)
    }
}

@Composable
fun StsLiveContent(
    timerValue: Int,
    reps: Int,
    testType: TestType,
    graphLeft: List<AccPoint>,
    graphRight: List<AccPoint>,
    graphCenter: List<AccPoint>
) {
    val scrollState = rememberScrollState()
    var selectedGraphTab by remember { mutableIntStateOf(0) }
    val graphTabs = listOf("Waist", "Left Leg", "Right Leg")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. Metrics Row (Timer & Reps) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Timer Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Timer, null, tint = PrimaryColor)
                    Spacer(Modifier.height(8.dp))
                    Text("Time", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    val timeText = String.format("%02d:%02d", timerValue / 60, timerValue % 60)
                    Text(timeText, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            // Reps Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.FitnessCenter, null, tint = PrimaryColor)
                    Spacer(Modifier.height(8.dp))
                    Text("Repetitions", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Text("$reps", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- 2. Live Graph Section ---
        Text(
            "Live Acceleration",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(Modifier.height(8.dp))

        TabRow(
            selectedTabIndex = selectedGraphTab,
            containerColor = Color.White,
            contentColor = PrimaryColor,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(Modifier.tabIndicatorOffset(tabPositions[selectedGraphTab]), color = PrimaryColor)
            }
        ) {
            graphTabs.forEachIndexed { index, title ->
                Tab(selected = selectedGraphTab == index, onClick = { selectedGraphTab = index }, text = { Text(title) })
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth().height(250.dp)
        ) {
            Box(Modifier.padding(16.dp)) {
                when (selectedGraphTab) {
                    0 -> AccelerationGraph(Modifier.fillMaxSize(), graphCenter, ACC_RANGE)
                    1 -> AccelerationGraph(Modifier.fillMaxSize(), graphLeft, ACC_RANGE)
                    2 -> AccelerationGraph(Modifier.fillMaxSize(), graphRight, ACC_RANGE)
                }
            }
        }

        // --- 3. Instructions / Status Text ---
        Spacer(modifier = Modifier.height(32.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE3F2FD), RoundedCornerShape(8.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (testType == TestType.FIVE_REPS)
                    "Perform 5 Sit-to-Stands as fast as possible."
                else
                    "Perform as many Sit-to-Stands as possible in 30 seconds.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1565C0),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

// Helper to update graph lists (same as used in TUG)
fun addPointToGraph(list: androidx.compose.runtime.snapshots.SnapshotStateList<AccPoint>, x: Float, y: Float, z: Float) {
    list.add(AccPoint(x, y, z))
    if (list.size > 100) {
        list.removeRange(0, list.size - 100)
    }
}