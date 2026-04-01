package com.ripplehealthcare.frst.ui.screens

import FourStageResult
import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ripplehealthcare.frst.domain.model.AccPoint
import com.ripplehealthcare.frst.domain.model.ConnectionState
import com.ripplehealthcare.frst.ui.components.AccelerationGraph
import com.ripplehealthcare.frst.ui.components.DialogState
import com.ripplehealthcare.frst.ui.components.DialogType
import com.ripplehealthcare.frst.ui.components.SavingDialog
import com.ripplehealthcare.frst.ui.components.SensorCircle
import com.ripplehealthcare.frst.ui.components.StatusDialog
import com.ripplehealthcare.frst.ui.components.TopBar
import com.ripplehealthcare.frst.ui.theme.PrimaryColor
import com.ripplehealthcare.frst.ui.theme.RedColor
import com.ripplehealthcare.frst.ui.theme.WhiteColor
import com.ripplehealthcare.frst.ui.viewmodel.BluetoothViewModel
import com.ripplehealthcare.frst.ui.viewmodel.ManagementViewModel
import com.ripplehealthcare.frst.ui.viewmodel.PatientViewModel
import com.ripplehealthcare.frst.ui.viewmodel.TestViewModel
import kotlinx.coroutines.delay

private const val ACC_RANGE = 6.0f

enum class GameState {
    IDLE, CALIBRATING, READY, RUNNING, STAGE_FINISHED, ALL_FINISHED
}

data class SensorBaseline(
    var yaw: Float = 0f, var pitch: Float = 0f,
    var accX: Float = 0f, var accY: Float = 0f, var accZ: Float = 0f
)

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun FourStageTestScreen(
    navController: NavController,
    viewModel: BluetoothViewModel,
    testViewModel: TestViewModel,
    patientViewModel: PatientViewModel
) {
    val sensorData by viewModel.sensorData.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    // Test ViewModel State
    val currentStage by testViewModel.currentStage.collectAsState()
    val stageResults by testViewModel.stageResults.collectAsState()

    val isSaving by testViewModel.isSaving.collectAsState()
    val isSavedToBackend by testViewModel.isSavedToBackend.collectAsState()
    val patient by patientViewModel.selectedPatient.collectAsState()

    // --- Local UI State ---
    var gameState by remember { mutableStateOf(GameState.IDLE) }
    var timeRemaining by remember { mutableIntStateOf(10) }
    var countdownSeconds by remember { mutableIntStateOf(3) }

    // Tab State
    var selectedGraphTab by remember { mutableIntStateOf(0) }
    val graphTabs = listOf("Waist", "Left Leg", "Right Leg")

    // --- Calibration Data ---
    val centerBase = remember { SensorBaseline() }
    val leftBase = remember { SensorBaseline() }
    val rightBase = remember { SensorBaseline() }

    val calibDataCenter = remember { mutableListOf<SensorBaseline>() }
    val calibDataLeft = remember { mutableListOf<SensorBaseline>() }
    val calibDataRight = remember { mutableListOf<SensorBaseline>() }

    // --- Live Graph Data ---
    val graphCenter = remember { mutableStateListOf<AccPoint>() }
    val graphLeft = remember { mutableStateListOf<AccPoint>() }
    val graphRight = remember { mutableStateListOf<AccPoint>() }

    // --- History for Saving (Current Stage) ---
    val angleHistoryLeft = remember { mutableListOf<Pair<Float, Float>>() }
    val accHistoryLeft = remember { mutableListOf<AccPoint>() }
    val angleHistoryRight = remember { mutableListOf<Pair<Float, Float>>() }
    val accHistoryRight = remember { mutableListOf<AccPoint>() }
    val angleHistoryCenter = remember { mutableListOf<Pair<Float, Float>>() }
    val accHistoryCenter = remember { mutableListOf<AccPoint>() }

    // --- Sensor Readings ---
    val cYaw = sensorData?.centerYaw ?: 0f; val cPitch = sensorData?.centerPitch ?: 0f
    val cAccX = sensorData?.centerAccX ?: 0f; val cAccY = sensorData?.centerAccY ?: 0f; val cAccZ = sensorData?.centerAccZ ?: 0f

    val lYaw = sensorData?.leftYaw ?: 0f; val lPitch = sensorData?.leftPitch ?: 0f
    val lAccX = sensorData?.leftAccX ?: 0f; val lAccY = sensorData?.leftAccY ?: 0f; val lAccZ = sensorData?.leftAccZ ?: 0f

    val rYaw = sensorData?.rightYaw ?: 0f; val rPitch = sensorData?.rightPitch ?: 0f
    val rAccX = sensorData?.rightAccX ?: 0f; val rAccY = sensorData?.rightAccY ?: 0f; val rAccZ = sensorData?.rightAccZ ?: 0f

    var dialogState by remember { mutableStateOf(DialogState()) }

    fun showDialog(title: String, message: String, type: DialogType) {
        dialogState = DialogState(true, title, message, type)
    }

    fun dismissDialog() {
        val shouldNav = dialogState.title == "Connection Lost"
        dialogState = dialogState.copy(isVisible = false)
        if (shouldNav) navController.popBackStack()
    }

    // 2. Connection Check
    LaunchedEffect(connectionState) {
        if(connectionState == ConnectionState.DISCONNECTED) {
            showDialog("Connection Lost", "Device disconnected.", DialogType.ERROR)
        }
    }

    // 3. Game Loop (Calibration & Running)
    LaunchedEffect(gameState) {
        when (gameState) {
            GameState.CALIBRATING -> {
                calibDataCenter.clear(); calibDataLeft.clear(); calibDataRight.clear()
                countdownSeconds = 3
                while (countdownSeconds > 0) {
                    delay(1000L)
                    countdownSeconds--
                }
                // Calculate Averages
                if (calibDataCenter.isNotEmpty()) {
                    centerBase.yaw = calibDataCenter.map { it.yaw }.average().toFloat()
                    centerBase.pitch = calibDataCenter.map { it.pitch }.average().toFloat()
                    centerBase.accX = calibDataCenter.map { it.accX }.average().toFloat()
                    centerBase.accY = calibDataCenter.map { it.accY }.average().toFloat()
                    centerBase.accZ = calibDataCenter.map { it.accZ }.average().toFloat()

                    leftBase.yaw = calibDataLeft.map { it.yaw }.average().toFloat()
                    leftBase.pitch = calibDataLeft.map { it.pitch }.average().toFloat()
                    leftBase.accX = calibDataLeft.map { it.accX }.average().toFloat()
                    leftBase.accY = calibDataLeft.map { it.accY }.average().toFloat()
                    leftBase.accZ = calibDataLeft.map { it.accZ }.average().toFloat()

                    rightBase.yaw = calibDataRight.map { it.yaw }.average().toFloat()
                    rightBase.pitch = calibDataRight.map { it.pitch }.average().toFloat()
                    rightBase.accX = calibDataRight.map { it.accX }.average().toFloat()
                    rightBase.accY = calibDataRight.map { it.accY }.average().toFloat()
                    rightBase.accZ = calibDataRight.map { it.accZ }.average().toFloat()
                }
                gameState = GameState.READY
            }
            GameState.RUNNING -> {
                // Clear LOCAL history for this stage run
                graphCenter.clear(); graphLeft.clear(); graphRight.clear()
                angleHistoryLeft.clear(); accHistoryLeft.clear()
                angleHistoryRight.clear(); accHistoryRight.clear()
                angleHistoryCenter.clear(); accHistoryCenter.clear()

                timeRemaining = 10
                while (timeRemaining > 0) {
                    delay(1000L)
                    timeRemaining--
                }

                // --- SAVE STAGE DATA ---
                testViewModel.saveStageData(
                    pointsLeft = angleHistoryLeft.toList(),
                    accLeft = accHistoryLeft.toList(),
                    pointsRight = angleHistoryRight.toList(),
                    accRight = accHistoryRight.toList(),
                    pointsCenter = angleHistoryCenter.toList(),
                    accCenter = accHistoryCenter.toList()
                )

                gameState = GameState.STAGE_FINISHED
            }
            else -> {}
        }
    }

    // 4. Data Collection Loop
    LaunchedEffect(sensorData) {
        if (gameState == GameState.CALIBRATING) {
            calibDataCenter.add(SensorBaseline(cYaw, cPitch, cAccX, cAccY, cAccZ))
            calibDataLeft.add(SensorBaseline(lYaw, lPitch, lAccX, lAccY, lAccZ))
            calibDataRight.add(SensorBaseline(rYaw, rPitch, rAccX, rAccY, rAccZ))
        } else if (gameState == GameState.RUNNING) {
            // Live Graph (Calibrated)
            addPointToGraph(graphCenter, cAccX, cAccY, cAccZ)
            addPointToGraph(graphLeft, lAccX, lAccY, lAccZ)
            addPointToGraph(graphRight, rAccX, rAccY, rAccZ)

            // History for Saving
            angleHistoryLeft.add(Pair(lYaw - leftBase.yaw, lPitch - leftBase.pitch))
            accHistoryLeft.add(AccPoint(lAccX, lAccY, lAccZ))
            angleHistoryRight.add(Pair(rYaw - rightBase.yaw, rPitch - rightBase.pitch))
            accHistoryRight.add(AccPoint(rAccX, rAccY, rAccZ))
            angleHistoryCenter.add(Pair(cYaw - centerBase.yaw, cPitch - centerBase.pitch))
            accHistoryCenter.add(AccPoint(cAccX, cAccY, cAccZ))
        }
    }

    LaunchedEffect(isSavedToBackend) {
        if (isSavedToBackend) {
            testViewModel.resetTestData(keepTestType = true)
            navController.popBackStack("sessionDashboard", inclusive = false)
        }
    }

    val isCalibrating = gameState == GameState.CALIBRATING
    val blurRadius by animateDpAsState(if (isCalibrating) 16.dp else 0.dp, label = "Blur")

    // Stage Title Helper
    val stageTitle = when(currentStage) {
        1 -> "1. Feet Together"
        2 -> "2. Semi-Tandem"
        3 -> "3. Tandem"
        4 -> "4. One Leg Stand"
        else -> "Test Completed"
    }

    // --- UI Structure ---
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.blur(blurRadius),
            topBar = {
                TopBar(
                    title = stageTitle,
                    onBackClick = { navController.popBackStack() })
            },
            bottomBar = {
                FourStageBottomBar(
                    gameState = gameState,
                    currentStage = currentStage,
                    timeRemaining = timeRemaining,
                    onAction = { action ->
                        when (action) {
                            "START_CALIB" -> gameState = GameState.CALIBRATING
                            "START_RUN" -> gameState = GameState.RUNNING
                            "STOP" -> gameState = GameState.STAGE_FINISHED // Early stop saves partial? Or discard? currently saves
                            "NEXT_STAGE" -> {
                                if (currentStage < 4) {
                                    testViewModel.setStage(currentStage + 1)
                                    gameState = GameState.IDLE
                                }
                            }
                            "FINISH_ALL" -> {
                                // --- FINAL SAVE TO SESSION ---
                                val result = FourStageResult(
                                    stages = stageResults.values.toList()
                                )
                                // Save to Session
                                patient?.let { testViewModel.saveFourStageResult(it.centerId,it.doctorId,result) }
                            }
                        }
                    }
                )
            },
            containerColor = Color(0xFFF8F9FA)
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Section 1: Waist
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Waist Sensor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        SensorCircle(
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            yaw = cYaw, pitch = cPitch,
                            baseYaw = centerBase.yaw, basePitch = centerBase.pitch,
                            gameState = gameState,
                            dotColor = PrimaryColor
                        )
                    }
                }

                // Section 2: Legs
                item {
                    Text("Leg Sensors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Left", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            SensorCircle(
                                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                                yaw = lYaw, pitch = lPitch,
                                baseYaw = leftBase.yaw, basePitch = leftBase.pitch,
                                gameState = gameState,
                                dotColor = Color(0xFF1976D2)
                            )
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Right", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            SensorCircle(
                                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                                yaw = rYaw, pitch = rPitch,
                                baseYaw = rightBase.yaw, basePitch = rightBase.pitch,
                                gameState = gameState,
                                dotColor = Color(0xFFD32F2F)
                            )
                        }
                    }
                }

                // Section 3: Graphs
                item {
                    Column {
                        Text("Acceleration Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        TabRow(
                            selectedTabIndex = selectedGraphTab,
                            containerColor = Color.White,
                            contentColor = PrimaryColor,
                            indicator = { TabRowDefaults.Indicator(Modifier.tabIndicatorOffset(it[selectedGraphTab]), color = PrimaryColor) }
                        ) {
                            graphTabs.forEachIndexed { i, title ->
                                Tab(selected = selectedGraphTab == i, onClick = { selectedGraphTab = i }, text = { Text(title) })
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth().height(250.dp)) {
                            Box(Modifier.padding(16.dp)) {
                                when (selectedGraphTab) {
                                    0 -> AccelerationGraph(Modifier.fillMaxSize(), graphCenter, ACC_RANGE)
                                    1 -> AccelerationGraph(Modifier.fillMaxSize(), graphLeft, ACC_RANGE)
                                    2 -> AccelerationGraph(Modifier.fillMaxSize(), graphRight, ACC_RANGE)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Calibration Overlay
        AnimatedVisibility(visible = isCalibrating, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(32.dp)) {
                    Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryColor, strokeWidth = 6.dp, modifier = Modifier.size(60.dp))
                        Spacer(Modifier.height(24.dp))
                        Text("Calibrating...", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PrimaryColor)
                        Text("Please stand still", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Spacer(Modifier.height(24.dp))
                        Text("$countdownSeconds", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.ExtraBold, color = PrimaryColor)
                    }
                }
            }
        }
        StatusDialog(state = dialogState, onDismissRequest = { dismissDialog() })
        SavingDialog(isSaving)
    }
}

@Composable
fun FourStageBottomBar(
    gameState: GameState,
    currentStage: Int,
    timeRemaining: Int,
    onAction: (String) -> Unit
) {
    Surface(shadowElevation = 12.dp, color = Color.White) {
        Column(
            modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(16.dp, 20.dp)
        ) {
            if (gameState == GameState.RUNNING) {
                Text(
                    text = "Time Remaining: ${timeRemaining}s",
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryColor
                )
            }

            // Logic for Buttons based on flow
            Button(
                onClick = {
                    when (gameState) {
                        GameState.IDLE -> onAction("START_CALIB")
                        GameState.READY -> onAction("START_RUN")
                        GameState.RUNNING -> onAction("STOP")

                        // When a stage is finished:
                        GameState.STAGE_FINISHED -> {
                            if (currentStage < 4) onAction("NEXT_STAGE") else onAction("FINISH_ALL")
                        }

                        else -> {}
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    contentColor = WhiteColor,
                    containerColor = if (gameState == GameState.RUNNING) RedColor else PrimaryColor
                ),
                enabled = gameState != GameState.CALIBRATING
            ) {
                Text(
                    text = when (gameState) {
                        GameState.IDLE -> "Calibrate Sensors"
                        GameState.CALIBRATING -> "Calibrating..."
                        GameState.READY -> "Start Stage $currentStage"
                        GameState.RUNNING -> "Stop Test"
                        GameState.STAGE_FINISHED -> if (currentStage < 4) "Proceed to Stage ${currentStage + 1}" else "Finish & Save"
                        else -> "Finish"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Allow finishing early if user cannot complete all stages
            if (gameState == GameState.STAGE_FINISHED && currentStage < 4) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onAction("FINISH_ALL") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Finish Test Here (Skip remaining)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}