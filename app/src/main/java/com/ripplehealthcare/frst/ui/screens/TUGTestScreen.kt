package com.ripplehealthcare.frst.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ripplehealthcare.frst.domain.model.AccPoint
import com.ripplehealthcare.frst.domain.model.ConnectionState
import com.ripplehealthcare.frst.domain.model.TUGPhase
import com.ripplehealthcare.frst.domain.model.TestState
import com.ripplehealthcare.frst.domain.model.TugResult
import com.ripplehealthcare.frst.domain.model.TugState
import com.ripplehealthcare.frst.ui.components.AccelerationGraph
import com.ripplehealthcare.frst.ui.components.DialogState
import com.ripplehealthcare.frst.ui.components.DialogType
import com.ripplehealthcare.frst.ui.components.SavingDialog
import com.ripplehealthcare.frst.ui.components.StatusDialog
import com.ripplehealthcare.frst.ui.components.TopBar
import com.ripplehealthcare.frst.ui.theme.PrimaryColor
import com.ripplehealthcare.frst.ui.theme.WhiteColor
import com.ripplehealthcare.frst.ui.viewmodel.BluetoothViewModel
import com.ripplehealthcare.frst.ui.viewmodel.PatientViewModel
import com.ripplehealthcare.frst.ui.viewmodel.TestViewModel

private const val ACC_RANGE = 8.0f // Adjust scale as needed

@SuppressLint("DefaultLocale")
@Composable
fun TUGTestScreen(
    navController: NavController,
    viewModel: BluetoothViewModel,
    testViewModel: TestViewModel,
    patientViewModel: PatientViewModel
) {
    val connectionState by viewModel.connectionState.collectAsState()

    // 1. Observe ViewModels
    val testState by testViewModel.testState.collectAsState()
    val timerValue by testViewModel.timer.collectAsState()
    val sensorData by viewModel.sensorData.collectAsState()
    val tugState by testViewModel.tugState.collectAsState()
    val patient by patientViewModel.selectedPatient.collectAsState()

    val isSaving by testViewModel.isSaving.collectAsState()
    val isSavedToBackend by testViewModel.isSavedToBackend.collectAsState()

    // 2. Graph State
    val graphLeft = remember { mutableStateListOf<AccPoint>() }
    val graphRight = remember { mutableStateListOf<AccPoint>() }
    val graphCenter = remember { mutableStateListOf<AccPoint>() }

    val accHistoryLeft = remember { mutableListOf<AccPoint>() }
    val accHistoryRight = remember { mutableListOf<AccPoint>() }
    val accHistoryCenter = remember { mutableListOf<AccPoint>() }

    val standingCalib by testViewModel.standingCalibration.collectAsState()

    val baseLAccX = standingCalib?.leftAccX ?: 0f
    val baseLAccY = standingCalib?.leftAccY ?: 0f
    val baseLAccZ = standingCalib?.leftAccZ ?: 0f

    val baseRAccX = standingCalib?.rightAccX ?: 0f
    val baseRAccY = standingCalib?.rightAccY ?: 0f
    val baseRAccZ = standingCalib?.rightAccZ ?: 0f

    val baseCAccX = standingCalib?.centerAccX ?: 0f
    val baseCAccY = standingCalib?.centerAccY ?: 0f
    val baseCAccZ = standingCalib?.centerAccZ ?: 0f

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

    // 4. Data Processing Effect
    LaunchedEffect(sensorData, testState) {
        if (testState == TestState.RUNNING) {
            sensorData?.let { data ->
                // A. Run Algorithm
                testViewModel.analyzeTugData(
                    accelLeftX = data.leftAccX,
                    accelLeftY = data.leftAccY,
                    accelLeftZ = data.leftAccZ,
                    accelRightX = data.rightAccX,
                    accelRightY = data.rightAccY,
                    accelRightZ = data.rightAccZ,
                    leftPitch = data.leftPitch,
                    rightPitch = data.rightPitch,
                    centerRoll = data.centerRoll
                )

                val cX = data.centerAccX; val cY = data.centerAccY; val cZ = data.centerAccZ
                val lX = data.leftAccX;   val lY = data.leftAccY;   val lZ = data.leftAccZ
                val rX = data.rightAccX;  val rY = data.rightAccY;  val rZ = data.rightAccZ

                // C. Update Live Graphs
                addPointToGraph(graphCenter, cX, cY, cZ)
                addPointToGraph(graphLeft, lX, lY, lZ)
                addPointToGraph(graphRight, rX, rY, rZ)

                // D. Accumulate for Backend Saving
                accHistoryCenter.add(AccPoint(cX, cY, cZ))
                accHistoryLeft.add(AccPoint(lX, lY, lZ))
                accHistoryRight.add(AccPoint(rX, rY, rZ))
            }
        }
    }

    LaunchedEffect(Unit) {
        testViewModel.errorEvent.collect { errorMessage ->
            showDialog("Test Failed", errorMessage, DialogType.ERROR)
        }
    }

    // 5. Auto-Stop Logic (UPDATED FOR SESSION)
    LaunchedEffect(testState) {
        if (testState == TestState.COMPLETED) {
            val result = TugResult(
                totalTimeSeconds = timerValue,
                accLeft = accHistoryLeft,
                accRight = accHistoryRight,
                accCenter = accHistoryCenter
            )

            // 1. Save to the Active Session via TestViewModel
            patient?.let { testViewModel.saveTugResult(it.centerId, it.doctorId, result) }
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

    Scaffold(
        topBar = { TopBar("TUG Test", onBackClick = { navController.popBackStack() }) },
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
                                graphLeft.clear(); graphRight.clear(); graphCenter.clear()
                                accHistoryLeft.clear(); accHistoryRight.clear(); accHistoryCenter.clear()
                                testViewModel.startTugTest()
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
            transitionSpec = { fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500)) },
            modifier = Modifier.padding(innerPadding),
            label = "TestStateAnimation"
        ) { state ->
            if (state == TestState.COMPLETED) {
                // We likely won't see this for long as we navigate away,
                // but we keep a simple loading indicator just in case
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryColor)
                }
            } else {
                TugLiveContent(
                    timerValue = timerValue,
                    tugState = tugState,
                    testState = state,
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
        SavingDialog(isSaving)
    }
}

// --- SUB-COMPONENTS (Unchanged) ---

@SuppressLint("DefaultLocale")
@Composable
fun TugLiveContent(
    timerValue: Int,
    tugState: TugState,
    testState: TestState,
    graphLeft: List<AccPoint>,
    graphRight: List<AccPoint>,
    graphCenter: List<AccPoint>
) {
    // Scroll state for the column
    val scrollState = rememberScrollState()

    // Graph Tab State
    var selectedGraphTab by remember { mutableIntStateOf(0) }
    val graphTabs = listOf("Waist", "Left Leg", "Right Leg")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState) // Enable scrolling
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. Timer ---
        Text("Total Time", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
        val timeText = String.format("%02d:%02d", timerValue / 60, timerValue % 60)
        Text(
            text = timeText,
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryColor,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // --- 2. Live State Card ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Current State", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Text(
                        text = tugState.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (testState == TestState.RUNNING) Color(0xFF4CAF50) else Color.Gray)
                )
            }
        }

        // --- 3. Phase List ---
        Text(
            text = "Test Stages",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 16.dp)
        )

        val phases = TUGPhase.entries.toTypedArray()
        phases.forEach { phase ->
            val isActive = when (phase) {
                TUGPhase.SIT_TO_STAND -> tugState == TugState.SITTING
                TUGPhase.WALKING -> tugState == TugState.WALKING || tugState == TugState.STANDING
                TUGPhase.STAND_TO_SIT -> tugState == TugState.SITTING
            }

            TugPhaseRow(
                phase = phase,
                isActive = isActive && testState == TestState.RUNNING,
                isCompleted = false
            )
            if (phase != phases.last()) {
                Box(
                    modifier = Modifier
                        .padding(start = 23.dp)
                        .width(2.dp)
                        .height(24.dp)
                        .background(Color.LightGray)
                        .align(Alignment.Start)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- 4. Acceleration Graph ---
        Text(
            "Live Acceleration",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(Modifier.height(8.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedGraphTab,
            containerColor = Color.White,
            contentColor = PrimaryColor,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedGraphTab]),
                    color = PrimaryColor
                )
            }
        ) {
            graphTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedGraphTab == index,
                    onClick = { selectedGraphTab = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Graph Content
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth().height(250.dp)
        ) {
            Box(Modifier.padding(16.dp)) {
                when (selectedGraphTab) {
                    0 -> AccelerationGraph( // Center
                        modifier = Modifier.fillMaxSize(),
                        dataPoints = graphCenter,
                        range = ACC_RANGE
                    )
                    1 -> AccelerationGraph( // Left Leg
                        modifier = Modifier.fillMaxSize(),
                        dataPoints = graphLeft,
                        range = ACC_RANGE
                    )
                    2 -> AccelerationGraph( // Right Leg
                        modifier = Modifier.fillMaxSize(),
                        dataPoints = graphRight,
                        range = ACC_RANGE
                    )
                }
            }
        }

        // Padding for bottom bar
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// Reusing your row component
@Composable
fun TugPhaseRow(phase: TUGPhase, isActive: Boolean, isCompleted: Boolean) {
    val color by animateColorAsState(targetValue = if (isActive) PrimaryColor else Color.LightGray)

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isActive) color.copy(alpha = 0.1f) else Color(0xFFEEEEEE))
                .border(2.dp, if (isActive) color else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Default.Check else phase.icon,
                contentDescription = null,
                tint = if (isActive) color else Color.Gray
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = phase.label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) Color.Black else Color.Gray
        )
    }
}