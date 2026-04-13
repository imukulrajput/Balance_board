//package com.ripplehealthcare.bproboard.ui.screens
//
//import androidx.compose.foundation.BorderStroke
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.PathEffect
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.compose.ui.window.Dialog
//import androidx.compose.ui.window.DialogProperties
//import androidx.navigation.NavController
//import com.ripplehealthcare.bproboard.domain.model.HolePuzzleResult
//import com.ripplehealthcare.bproboard.ui.viewmodel.BluetoothViewModel
//import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel
//import com.ripplehealthcare.bproboard.ui.components.TopBar
//import com.ripplehealthcare.bproboard.ui.theme.Green
//import com.ripplehealthcare.bproboard.ui.theme.PrimaryColor
//import com.ripplehealthcare.bproboard.ui.theme.WhiteColor
//import kotlinx.coroutines.delay
//import java.util.UUID
//import kotlin.math.abs
//import kotlin.math.sqrt
//import androidx.activity.compose.BackHandler
//
//private sealed class HoleGameState {
//    object ReadyToStart : HoleGameState()
//    object Playing : HoleGameState()
//    // Added score tracking to the GameOver state
//    data class GameOver(val hasWon: Boolean, val score: Int, val timeMs: Long, val holesDodged: Int) : HoleGameState()
//}
//
//data class PuzzleHole(val id: Int, val x: Float, val y: Float, val radius: Float, val passed: Boolean = false)
//
//val RedColor = Color(0xFFE53935) // Fallback in case it isn't in your theme
//
//@Composable
//fun HolePuzzleGame(
//    navController: NavController,
//    bluetoothViewModel: BluetoothViewModel,
//    testViewModel: TestViewModel
//) {
//
//    BackHandler {
//        navController.popBackStack()
//    }
//
//
//    val sensorData by bluetoothViewModel.sensorData.collectAsState()
//
//    val deltaYaw = (sensorData?.centerYaw ?: bluetoothViewModel.centerYaw) - bluetoothViewModel.centerYaw
//    val currentRoll = -deltaYaw
//
//    var gameState by remember { mutableStateOf<HoleGameState>(HoleGameState.ReadyToStart) }
//
//    Scaffold(
//        topBar = { TopBar("Hole Navigator", onBackClick = { navController.popBackStack() }) },
//        containerColor = Color(0xFFF1F5F9)
//    ) { padding ->
//        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
//            when (val state = gameState) {
//                is HoleGameState.ReadyToStart -> {
//                    // Assuming ReadyToStartScreen is defined elsewhere
//                    // ReadyToStartScreen { gameState = HoleGameState.Playing }
//
//                    // Temporary placeholder so the game can start
//                    Button(onClick = { gameState = HoleGameState.Playing }) {
//                        Text("Start Game")
//                    }
//                }
//                is HoleGameState.Playing, is HoleGameState.GameOver -> {
//                    Box(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp))) {
//                        HoleCanvas(
//                            currentRoll = currentRoll,
//                            isPlaying = state is HoleGameState.Playing,
//                            onGameOver = { hasWon, timeMs, holesDodged ->
//                                // Calculate Score: 5 points per second + 10 points per dodged hole
//                                val finalScore = ((timeMs / 1000).toInt() * 5) + (holesDodged * 10)
//                                val result = HolePuzzleResult(
//                                    sessionId = UUID.randomUUID().toString(),
//                                    patientId = testViewModel.patient.value.patientId,
//                                    timeSurvivedMs = timeMs,
//                                    holesDodged = holesDodged,
//                                    score = finalScore,
//                                    isWin = hasWon
//                                )
//                                testViewModel.saveHolePuzzleResult(result)
//                                gameState = HoleGameState.GameOver(hasWon, finalScore, timeMs, holesDodged)
//                            }
//                        )
//
//                        if (state is HoleGameState.GameOver) {
//                            HoleGameOverOverlay(
//                                hasWon = state.hasWon,
//                                score = state.score,
//                                timeMs = state.timeMs,
//                                holesDodged = state.holesDodged,
//                                onRestart = { gameState = HoleGameState.ReadyToStart },
//                                onFinish = { navController.popBackStack() }
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun HoleCanvas(currentRoll: Float, isPlaying: Boolean, onGameOver: (Boolean, Long, Int) -> Unit) {
//    var holes by remember { mutableStateOf(emptyList<PuzzleHole>()) }
//    var gridOffset by remember { mutableFloatStateOf(0f) }
//    var isInitialized by remember { mutableStateOf(false) }
//
//    var gameStartTime by remember { mutableLongStateOf(0L) }
//    var lastSpawnTime by remember { mutableLongStateOf(0L) }
//    var lastHoleX by remember { mutableFloatStateOf(0.5f) }
//    var holeIdCounter by remember { mutableIntStateOf(0) }
//
//    // STRICT 1-MINUTE TIMER LOGIC
//    val maxGameTimeMs = 60_000L
//    var timeLeftMs by remember { mutableLongStateOf(maxGameTimeMs) }
//
//    var holesDodged by remember { mutableIntStateOf(0) }
//    var dataSaved by remember { mutableStateOf(false) }
//
//    val ballRadius = 14.dp
//    val baseHoleRadius = 24.dp
//    val ballYOffset = 140.dp
//
//    val density = LocalDensity.current
//    val ballRadiusPx = with(density) { ballRadius.toPx() }
//    val baseHoleRadiusPx = with(density) { baseHoleRadius.toPx() }
//    val ballYOffsetPx = with(density) { ballYOffset.toPx() }
//
//    val leftMax = -25f
//    val rightMax = 25f
//    val range = rightMax - leftMax
//    val normalizedPosition = if (abs(range) > 0.1f) { (2 * (currentRoll - leftMax) / range - 1).coerceIn(-1f, 1f) } else 0f
//    val currentPosition by rememberUpdatedState(normalizedPosition)
//
//    LaunchedEffect(isInitialized) {
//        if (isInitialized && gameStartTime == 0L) {
//            gameStartTime = System.currentTimeMillis()
//            lastSpawnTime = gameStartTime
//        }
//    }
//
//    LaunchedEffect(isPlaying) {
//        var timeStationary = 0f
//        var lastPosition = 0f
//
//        while (isPlaying) {
//            if (isInitialized && gameStartTime > 0L) {
//                val now = System.currentTimeMillis()
//                val elapsedMs = now - gameStartTime
//
//                timeLeftMs = (maxGameTimeMs - elapsedMs).coerceAtLeast(0L)
//                val survivedMs = maxGameTimeMs - timeLeftMs
//
//                // CHECK WIN CONDITION (Survived full 60 seconds)
//                if (timeLeftMs <= 0) {
//                    if (!dataSaved) {
//                        dataSaved = true
//                        onGameOver(true, survivedMs, holesDodged)
//                    }
//                    break // End the loop
//                }
//
//                val elapsedSec = elapsedMs / 1000f
//
//                // PROGRESSIVE DIFFICULTY CURVE (Gets harder every 15 seconds)
//                val currentSpeed = when {
//                    elapsedSec < 15f -> 7.0f
//                    elapsedSec < 30f -> 10.0f
//                    elapsedSec < 45f -> 13.0f
//                    else -> 16.0f
//                }
//
//                val spawnIntervalMs = when {
//                    elapsedSec < 15f -> 1100L
//                    elapsedSec < 30f -> 800L
//                    elapsedSec < 45f -> 600L
//                    else -> 400L // Intense finale
//                }
//
//                // Move holes and track dodges
//                var newHolesDodged = holesDodged
//                holes = holes.map { hole ->
//                    val newY = hole.y + currentSpeed
//                    var newlyPassed = hole.passed
//
//                    if (!newlyPassed && newY > 2000f) {
//                        newlyPassed = true
//                        newHolesDodged++
//                    }
//                    hole.copy(y = newY, passed = newlyPassed)
//                }
//                holesDodged = newHolesDodged
//
//                gridOffset = (gridOffset + currentSpeed) % 150f
//
//                if (now - lastSpawnTime >= spawnIntervalMs) {
//                    var nextX = (0.15f + Math.random() * 0.7f).toFloat()
//                    while (abs(nextX - lastHoleX) < 0.28f) nextX = (0.15f + Math.random() * 0.7f).toFloat()
//                    lastHoleX = nextX
//
//                    holes = holes + PuzzleHole(id = holeIdCounter++, x = nextX, y = -150f, radius = baseHoleRadiusPx + (Math.random() * 3f).toFloat())
//                    lastSpawnTime = now
//                }
//
//                // Anti-camping logic
//                if (abs(currentPosition - lastPosition) < 0.05f) {
//                    timeStationary += 16f
//                    if (timeStationary >= 3500f) { // Dropped camping tolerance slightly
//                        val targetHoleX = (currentPosition + 1f) / 2f
//                        holes = holes + PuzzleHole(id = holeIdCounter++, x = targetHoleX, y = -150f, radius = baseHoleRadiusPx + 4f)
//                        timeStationary = 0f
//                    }
//                } else {
//                    timeStationary = 0f
//                    lastPosition = currentPosition
//                }
//            }
//            delay(16)
//        }
//    }
//
//    val backgroundBrush = Brush.linearGradient(colors = listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)), start = Offset(0f, 0f), end = Offset(0f, Float.POSITIVE_INFINITY))
//
//    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
//        Canvas(modifier = Modifier.fillMaxSize()) {
//            val w = size.width
//            val h = size.height
//            val ballX = (w / 2) + (normalizedPosition * (w / 2 - ballRadiusPx))
//            val ballY = h - ballYOffsetPx
//
//            if (!isInitialized) {
//                val initialHoles = mutableListOf<PuzzleHole>()
//                var currentY = -250f
//                var lastXInit = 0.5f
//                repeat(4) {
//                    var nextX = (0.15f + Math.random() * 0.7f).toFloat()
//                    while (abs(nextX - lastXInit) < 0.32f) nextX = (0.15f + Math.random() * 0.7f).toFloat()
//                    lastXInit = nextX
//                    initialHoles.add(PuzzleHole(id = holeIdCounter++, x = nextX, y = currentY, radius = baseHoleRadiusPx + (Math.random() * 3f).toFloat()))
//                    currentY -= 800f + (Math.random() * 100f).toFloat()
//                }
//                holes = initialHoles
//                isInitialized = true
//            }
//
//            val gridColor = Color.White.copy(alpha = 0.4f)
//            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
//            for (i in 0..h.toInt() step 150) {
//                val yLine = i + gridOffset
//                if (yLine < h) drawLine(gridColor, Offset(0f, yLine), Offset(w, yLine), strokeWidth = 3f, pathEffect = pathEffect)
//            }
//
//            holes.forEach { hole ->
//                if (hole.y > -150f && hole.y < h + 150f) {
//                    val actualHoleX = hole.x * w
//                    drawCircle(color = Color.White.copy(alpha = 0.6f), radius = hole.radius + 4f, center = Offset(actualHoleX, hole.y + 4f))
//                    drawCircle(color = Color.Black.copy(alpha = 0.15f), radius = hole.radius + 4f, center = Offset(actualHoleX, hole.y - 4f))
//                    drawCircle(color = Color(0xFF1A1C20), radius = hole.radius, center = Offset(actualHoleX, hole.y))
//                    drawCircle(brush = Brush.radialGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f)), center = Offset(actualHoleX, hole.y), radius = hole.radius), radius = hole.radius, center = Offset(actualHoleX, hole.y))
//
//                    // Using multiplication instead of pow()
//                    val distSquared = (ballX - actualHoleX) * (ballX - actualHoleX) + (ballY - hole.y) * (ballY - hole.y)
//
//                    // CHECK DEATH CONDITION (Fell in hole)
//                    if (isInitialized && isPlaying && sqrt(distSquared) < (ballRadiusPx + hole.radius - 14f)) {
//                        if (!dataSaved) {
//                            dataSaved = true
//                            val survivedMs = maxGameTimeMs - timeLeftMs
//                            onGameOver(false, survivedMs, holesDodged)
//                        }
//                    }
//                }
//            }
//
//            drawCircle(color = Color.Black.copy(alpha = 0.25f), radius = ballRadiusPx, center = Offset(ballX + 6f, ballY + 10f))
//            drawCircle(brush = Brush.radialGradient(colors = listOf(Color(0xFF64B5F6), PrimaryColor, Color(0xFF0D47A1)), center = Offset(ballX - ballRadiusPx / 3, ballY - ballRadiusPx / 3), radius = ballRadiusPx * 1.5f), radius = ballRadiusPx, center = Offset(ballX, ballY))
//            drawCircle(color = Color.White.copy(alpha = 0.5f), radius = ballRadiusPx * 0.35f, center = Offset(ballX - ballRadiusPx * 0.35f, ballY - ballRadiusPx * 0.35f))
//        }
//
//        // Live UI Overlay (Timer and Score)
//        val seconds = timeLeftMs / 1000
//        val timeFormatted = String.format("00:%02d", seconds)
//        val currentScore = (((maxGameTimeMs - timeLeftMs) / 1000).toInt() * 5) + (holesDodged * 10)
//
//        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
//            Text("Time Left: $timeFormatted", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = if (seconds <= 10) Color.Red else PrimaryColor)
//            Text("Score: $currentScore", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PrimaryColor)
//        }
//    }
//}
//
//@Composable
//fun HoleGameOverOverlay(
//    hasWon: Boolean,
//    score: Int,
//    timeMs: Long,
//    holesDodged: Int,
//    onRestart: () -> Unit,
//    onFinish: () -> Unit
//) {
//    val seconds = timeMs / 1000
//    val timeFormatted = String.format("%02d:%02d", seconds / 60, seconds % 60)
//
//    Dialog(
//        onDismissRequest = { onFinish() }, // 1. Trigger onFinish when dismissed
//        properties = DialogProperties(
//            usePlatformDefaultWidth = false,
//            dismissOnBackPress = true,     // 2. Change this to TRUE
//            dismissOnClickOutside = false
//        )
//    ) {
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(
//                    if (hasWon)
//                        Brush.verticalGradient(listOf(Color(0xFFE8F5E9), Color(0xFFFFFFFF)))
//                    else
//                        Brush.verticalGradient(listOf(Color(0xFFFFEBEE), Color(0xFFFFFFFF)))
//                )
//                .padding(24.dp),
//            contentAlignment = Alignment.Center
//        ) {
//            Column(
//                horizontalAlignment = Alignment.CenterHorizontally,
//                verticalArrangement = Arrangement.Center
//            ) {
//                Text(
//                    text = if (hasWon) "🎉 SURVIVED! 🎉" else "💔 FELL IN! 💔",
//                    color = if (hasWon) Green else RedColor,
//                    style = MaterialTheme.typography.displayMedium,
//                    fontWeight = FontWeight.Black
//                )
//
//                Spacer(Modifier.height(16.dp))
//
//                Text(
//                    text = "FINAL SCORE",
//                    color = Color.DarkGray,
//                    fontSize = 18.sp,
//                    fontWeight = FontWeight.SemiBold
//                )
//
//                Text(
//                    text = "$score",
//                    color = PrimaryColor,
//                    fontSize = 86.sp,
//                    fontWeight = FontWeight.Black
//                )
//
//                Spacer(Modifier.height(24.dp))
//
//                Text(
//                    text = "Time Survived: $timeFormatted",
//                    color = Color(0xFF555555),
//                    fontSize = 22.sp
//                )
//
//                Spacer(Modifier.height(8.dp))
//
//                Text(
//                    text = "Holes Dodged: $holesDodged",
//                    color = Color(0xFF555555),
//                    fontSize = 22.sp
//                )
//
//                Spacer(Modifier.height(48.dp))
//
//                Button(
//                    onClick = onRestart,
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = if (hasWon) Green else PrimaryColor
//                    ),
//                    shape = RoundedCornerShape(16.dp),
//                    modifier = Modifier
//                        .height(64.dp)
//                        .fillMaxWidth(0.85f)
//                ) {
//                    Text(
//                        text = if (hasWon) "PLAY AGAIN →" else "TRY AGAIN",
//                        color = WhiteColor,
//                        fontSize = 20.sp,
//                        fontWeight = FontWeight.Bold
//                    )
//                }
//
//                Spacer(Modifier.height(20.dp))
//
//                OutlinedButton(
//                    onClick = onFinish,
//                    border = BorderStroke(2.5.dp, PrimaryColor),
//                    shape = RoundedCornerShape(16.dp),
//                    modifier = Modifier
//                        .height(64.dp)
//                        .fillMaxWidth(0.85f)
//                ) {
//                    Text(
//                        text = "FINISH SESSION",
//                        color = PrimaryColor,
//                        fontSize = 20.sp,
//                        fontWeight = FontWeight.Bold
//                    )
//                }
//            }
//        }
//    }
//}

package com.ripplehealthcare.bproboard.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.ripplehealthcare.bproboard.domain.model.HolePuzzleResult
import com.ripplehealthcare.bproboard.ui.viewmodel.BluetoothViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel
import com.ripplehealthcare.bproboard.ui.components.TopBar
import com.ripplehealthcare.bproboard.ui.theme.Green
import com.ripplehealthcare.bproboard.ui.theme.PrimaryColor
import com.ripplehealthcare.bproboard.ui.theme.WhiteColor
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.math.abs
import kotlin.math.sqrt

private sealed class HoleGameState {
    object ReadyToStart : HoleGameState()
    object Playing : HoleGameState()
    data class GameOver(val hasWon: Boolean, val score: Int, val timeMs: Long, val holesDodged: Int) : HoleGameState()
}

data class PuzzleHole(val id: Int, val x: Float, val y: Float, val radius: Float, val passed: Boolean = false)

val RedColor = Color(0xFFE53935)

@Composable
fun HoleNavigatorLaunchScreen(onStart: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E293B))
            .padding(24.dp)
    ) {
        Text(
            text = "HOLE NAVIGATOR",
            color = Color(0xFF38BDF8),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(32.dp))

        Text("• SURVIVE for 60 Seconds", color = Color(0xFFFACC15), fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start)
        Spacer(Modifier.height(16.dp))
        Text("• AVOID the black holes falling towards you", color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Start)
        Spacer(Modifier.height(12.dp))
        Text("• LEAN in any direction to move the blue orb", color = Color(0xFF38BDF8), fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start)
        Spacer(Modifier.height(16.dp))
        Text("Stay in the center to avoid traps!", color = Color.LightGray, fontSize = 16.sp, textAlign = TextAlign.Center)

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
        ) {
            Text("START ENGINE", color = WhiteColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HolePuzzleGame(
    navController: NavController,
    bluetoothViewModel: BluetoothViewModel,
    testViewModel: TestViewModel
) {

    BackHandler {
        navController.popBackStack()
    }

    val sensorData by bluetoothViewModel.sensorData.collectAsState()

    val deltaYaw = (sensorData?.centerYaw ?: bluetoothViewModel.centerYaw) - bluetoothViewModel.centerYaw
    val currentRoll = -deltaYaw

    var gameState by remember { mutableStateOf<HoleGameState>(HoleGameState.ReadyToStart) }

    Scaffold(
        topBar = { TopBar("Hole Navigator", onBackClick = { navController.popBackStack() }) },
        containerColor = Color(0xFFF1F5F9)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when (val state = gameState) {
                is HoleGameState.ReadyToStart -> {
                    HoleNavigatorLaunchScreen { gameState = HoleGameState.Playing }
                }
                is HoleGameState.Playing, is HoleGameState.GameOver -> {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp))) {
                        HoleCanvas(
                            currentRoll = currentRoll,
                            isPlaying = state is HoleGameState.Playing,
                            onGameOver = { hasWon, timeMs, holesDodged ->
                                val finalScore = ((timeMs / 1000).toInt() * 5) + (holesDodged * 10)
                                val result = HolePuzzleResult(
                                    sessionId = testViewModel.activeSessionId ?: java.util.UUID.randomUUID().toString(),
                                    patientId = testViewModel.patient.value.patientId,
                                    timeSurvivedMs = timeMs,
                                    holesDodged = holesDodged,
                                    score = finalScore,
                                    isWin = hasWon
                                )
                                testViewModel.saveHolePuzzleResult(result)
                                gameState = HoleGameState.GameOver(hasWon, finalScore, timeMs, holesDodged)
                            }
                        )

                        if (state is HoleGameState.GameOver) {
                            HoleGameOverOverlay(
                                hasWon = state.hasWon,
                                score = state.score,
                                timeMs = state.timeMs,
                                holesDodged = state.holesDodged,
                                onRestart = { gameState = HoleGameState.ReadyToStart },
                                onFinish = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HoleCanvas(currentRoll: Float, isPlaying: Boolean, onGameOver: (Boolean, Long, Int) -> Unit) {
    var holes by remember { mutableStateOf(emptyList<PuzzleHole>()) }
    var gridOffset by remember { mutableFloatStateOf(0f) }
    var isInitialized by remember { mutableStateOf(false) }

    var gameStartTime by remember { mutableLongStateOf(0L) }
    var lastSpawnTime by remember { mutableLongStateOf(0L) }
    var lastHoleX by remember { mutableFloatStateOf(0.5f) }
    var holeIdCounter by remember { mutableIntStateOf(0) }

    val maxGameTimeMs = 60_000L
    var timeLeftMs by remember { mutableLongStateOf(maxGameTimeMs) }

    var holesDodged by remember { mutableIntStateOf(0) }
    var dataSaved by remember { mutableStateOf(false) }

    val ballRadius = 14.dp
    val baseHoleRadius = 24.dp
    val ballYOffset = 140.dp

    val density = LocalDensity.current
    val ballRadiusPx = with(density) { ballRadius.toPx() }
    val baseHoleRadiusPx = with(density) { baseHoleRadius.toPx() }
    val ballYOffsetPx = with(density) { ballYOffset.toPx() }

    val leftMax = -25f
    val rightMax = 25f
    val range = rightMax - leftMax
    val normalizedPosition = if (abs(range) > 0.1f) { (2 * (currentRoll - leftMax) / range - 1).coerceIn(-1f, 1f) } else 0f
    val currentPosition by rememberUpdatedState(normalizedPosition)

    LaunchedEffect(isInitialized) {
        if (isInitialized && gameStartTime == 0L) {
            gameStartTime = System.currentTimeMillis()
            lastSpawnTime = gameStartTime
        }
    }

    LaunchedEffect(isPlaying) {
        var timeStationary = 0f
        var lastPosition = 0f

        while (isPlaying) {
            if (isInitialized && gameStartTime > 0L) {
                val now = System.currentTimeMillis()
                val elapsedMs = now - gameStartTime

                timeLeftMs = (maxGameTimeMs - elapsedMs).coerceAtLeast(0L)
                val survivedMs = maxGameTimeMs - timeLeftMs

                if (timeLeftMs <= 0) {
                    if (!dataSaved) {
                        dataSaved = true
                        onGameOver(true, survivedMs, holesDodged)
                    }
                    break
                }

                val elapsedSec = elapsedMs / 1000f

                val currentSpeed = when {
                    elapsedSec < 15f -> 7.0f
                    elapsedSec < 30f -> 10.0f
                    elapsedSec < 45f -> 13.0f
                    else -> 16.0f
                }

                val spawnIntervalMs = when {
                    elapsedSec < 15f -> 1100L
                    elapsedSec < 30f -> 800L
                    elapsedSec < 45f -> 600L
                    else -> 400L
                }

                var newHolesDodged = holesDodged
                holes = holes.map { hole ->
                    val newY = hole.y + currentSpeed
                    var newlyPassed = hole.passed

                    if (!newlyPassed && newY > 2000f) {
                        newlyPassed = true
                        newHolesDodged++
                    }
                    hole.copy(y = newY, passed = newlyPassed)
                }
                holesDodged = newHolesDodged

                gridOffset = (gridOffset + currentSpeed) % 150f

                if (now - lastSpawnTime >= spawnIntervalMs) {
                    var nextX = (0.15f + Math.random() * 0.7f).toFloat()
                    while (abs(nextX - lastHoleX) < 0.28f) nextX = (0.15f + Math.random() * 0.7f).toFloat()
                    lastHoleX = nextX

                    holes = holes + PuzzleHole(id = holeIdCounter++, x = nextX, y = -150f, radius = baseHoleRadiusPx + (Math.random() * 3f).toFloat())
                    lastSpawnTime = now
                }

                if (abs(currentPosition - lastPosition) < 0.05f) {
                    timeStationary += 16f
                    if (timeStationary >= 3500f) {
                        val targetHoleX = (currentPosition + 1f) / 2f
                        holes = holes + PuzzleHole(id = holeIdCounter++, x = targetHoleX, y = -150f, radius = baseHoleRadiusPx + 4f)
                        timeStationary = 0f
                    }
                } else {
                    timeStationary = 0f
                    lastPosition = currentPosition
                }
            }
            delay(16)
        }
    }

    val backgroundBrush = Brush.linearGradient(colors = listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)), start = Offset(0f, 0f), end = Offset(0f, Float.POSITIVE_INFINITY))

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val ballX = (w / 2) + (normalizedPosition * (w / 2 - ballRadiusPx))
            val ballY = h - ballYOffsetPx

            if (!isInitialized) {
                val initialHoles = mutableListOf<PuzzleHole>()
                var currentY = -250f
                var lastXInit = 0.5f
                repeat(4) {
                    var nextX = (0.15f + Math.random() * 0.7f).toFloat()
                    while (abs(nextX - lastXInit) < 0.32f) nextX = (0.15f + Math.random() * 0.7f).toFloat()
                    lastXInit = nextX
                    initialHoles.add(PuzzleHole(id = holeIdCounter++, x = nextX, y = currentY, radius = baseHoleRadiusPx + (Math.random() * 3f).toFloat()))
                    currentY -= 800f + (Math.random() * 100f).toFloat()
                }
                holes = initialHoles
                isInitialized = true
            }

            val gridColor = Color.White.copy(alpha = 0.4f)
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
            for (i in 0..h.toInt() step 150) {
                val yLine = i + gridOffset
                if (yLine < h) drawLine(gridColor, Offset(0f, yLine), Offset(w, yLine), strokeWidth = 3f, pathEffect = pathEffect)
            }

            holes.forEach { hole ->
                if (hole.y > -150f && hole.y < h + 150f) {
                    val actualHoleX = hole.x * w
                    drawCircle(color = Color.White.copy(alpha = 0.6f), radius = hole.radius + 4f, center = Offset(actualHoleX, hole.y + 4f))
                    drawCircle(color = Color.Black.copy(alpha = 0.15f), radius = hole.radius + 4f, center = Offset(actualHoleX, hole.y - 4f))
                    drawCircle(color = Color(0xFF1A1C20), radius = hole.radius, center = Offset(actualHoleX, hole.y))
                    drawCircle(brush = Brush.radialGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f)), center = Offset(actualHoleX, hole.y), radius = hole.radius), radius = hole.radius, center = Offset(actualHoleX, hole.y))

                    val distSquared = (ballX - actualHoleX) * (ballX - actualHoleX) + (ballY - hole.y) * (ballY - hole.y)

                    if (isInitialized && isPlaying && sqrt(distSquared) < (ballRadiusPx + hole.radius - 14f)) {
                        if (!dataSaved) {
                            dataSaved = true
                            val survivedMs = maxGameTimeMs - timeLeftMs
                            onGameOver(false, survivedMs, holesDodged)
                        }
                    }
                }
            }

            drawCircle(color = Color.Black.copy(alpha = 0.25f), radius = ballRadiusPx, center = Offset(ballX + 6f, ballY + 10f))
            drawCircle(brush = Brush.radialGradient(colors = listOf(Color(0xFF64B5F6), PrimaryColor, Color(0xFF0D47A1)), center = Offset(ballX - ballRadiusPx / 3, ballY - ballRadiusPx / 3), radius = ballRadiusPx * 1.5f), radius = ballRadiusPx, center = Offset(ballX, ballY))
            drawCircle(color = Color.White.copy(alpha = 0.5f), radius = ballRadiusPx * 0.35f, center = Offset(ballX - ballRadiusPx * 0.35f, ballY - ballRadiusPx * 0.35f))
        }

        val seconds = timeLeftMs / 1000
        val timeFormatted = String.format("00:%02d", seconds)
        val currentScore = (((maxGameTimeMs - timeLeftMs) / 1000).toInt() * 5) + (holesDodged * 10)

        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Time Left: $timeFormatted", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = if (seconds <= 10) Color.Red else PrimaryColor)
            Text("Score: $currentScore", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PrimaryColor)
        }
    }
}

@Composable
fun HoleGameOverOverlay(
    hasWon: Boolean,
    score: Int,
    timeMs: Long,
    holesDodged: Int,
    onRestart: () -> Unit,
    onFinish: () -> Unit
) {
    val seconds = timeMs / 1000
    val timeFormatted = String.format("%02d:%02d", seconds / 60, seconds % 60)

    Dialog(
        onDismissRequest = { onFinish() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (hasWon)
                        Brush.verticalGradient(listOf(Color(0xFFE8F5E9), Color(0xFFFFFFFF)))
                    else
                        Brush.verticalGradient(listOf(Color(0xFFFFEBEE), Color(0xFFFFFFFF)))
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (hasWon) "🎉 SURVIVED! 🎉" else "💔 FELL IN! 💔",
                    color = if (hasWon) Green else RedColor,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "FINAL SCORE",
                    color = Color.DarkGray,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "$score",
                    color = PrimaryColor,
                    fontSize = 86.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Time Survived: $timeFormatted",
                    color = Color(0xFF555555),
                    fontSize = 22.sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Holes Dodged: $holesDodged",
                    color = Color(0xFF555555),
                    fontSize = 22.sp
                )

                Spacer(Modifier.height(48.dp))

                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasWon) Green else PrimaryColor
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(64.dp)
                        .fillMaxWidth(0.85f)
                ) {
                    Text(
                        text = if (hasWon) "PLAY AGAIN →" else "TRY AGAIN",
                        color = WhiteColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(20.dp))

                OutlinedButton(
                    onClick = onFinish,
                    border = BorderStroke(2.5.dp, PrimaryColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(64.dp)
                        .fillMaxWidth(0.85f)
                ) {
                    Text(
                        text = "FINISH SESSION",
                        color = PrimaryColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}



