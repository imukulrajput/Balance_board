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
//import androidx.compose.ui.geometry.CornerRadius
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.geometry.Size
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.StrokeCap
//import androidx.compose.ui.graphics.drawscope.Stroke
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.text.TextStyle
//import androidx.compose.ui.text.drawText
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.rememberTextMeasurer
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.compose.ui.window.Dialog
//import androidx.compose.ui.window.DialogProperties
//import androidx.navigation.NavController
//import com.ripplehealthcare.bproboard.domain.model.ColorSorterResult
//import com.ripplehealthcare.bproboard.ui.viewmodel.BluetoothViewModel
//import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel
//import com.ripplehealthcare.bproboard.ui.components.TopBar
//import com.ripplehealthcare.bproboard.ui.theme.PrimaryColor
//import com.ripplehealthcare.bproboard.ui.theme.RedColor
//import com.ripplehealthcare.bproboard.ui.theme.Green
//import com.ripplehealthcare.bproboard.ui.theme.WhiteColor
//import java.util.UUID
//import kotlin.math.abs
//import kotlin.math.ceil
//import kotlin.random.Random
//
//private sealed class SorterGameState {
//    object ReadyToStart : SorterGameState()
//    object Playing : SorterGameState()
//}
//
//private enum class BallPhase { IN_PIPE, FALLING }
//
//private data class ActiveBall(
//    val id: Int, val color: Color, val isRed: Boolean, val y: Float = 0f, val exitX: Float = 0f, val phase: BallPhase = BallPhase.IN_PIPE
//)
//
//private data class Particle(
//    val x: Float, val y: Float, val vx: Float, val vy: Float, val color: Color, val lifeTimeMs: Float = 0f, val maxLifeTimeMs: Float = 600f
//)
//
//@Composable
//fun ColorSorterGame(
//    navController: NavController,
//    bluetoothViewModel: BluetoothViewModel,
//    testViewModel: TestViewModel
//) {
//    val sensorData by bluetoothViewModel.sensorData.collectAsState()
//
//    val deltaYaw = (sensorData?.centerYaw ?: bluetoothViewModel.centerYaw) - bluetoothViewModel.centerYaw
//    val currentRoll = -deltaYaw
//
//    val leftMaxRoll = -20f
//    val rightMaxRoll = 20f
//
//    var gameState by remember { mutableStateOf<SorterGameState>(SorterGameState.ReadyToStart) }
//
//    Scaffold(
//        topBar = { TopBar("Color Sorter", onBackClick = { navController.popBackStack() }) },
//        containerColor = Color(0xFFF8F9FA)
//    ) { padding ->
//        Column(
//            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            when (gameState) {
//                is SorterGameState.ReadyToStart -> {
//                    // Assuming ReadyToStartScreen is defined elsewhere in your project
//                    // ReadyToStartScreen { gameState = SorterGameState.Playing }
//
//                    // Temporary placeholder to allow the game to start if you haven't provided the ReadyToStartScreen code
//                    Button(onClick = { gameState = SorterGameState.Playing }) {
//                        Text("Start Game")
//                    }
//                }
//                is SorterGameState.Playing -> {
//                    Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0xFF2B2D42), MaterialTheme.shapes.medium)) {
//                        SorterCanvas(
//                            currentRoll = currentRoll,
//                            leftMax = leftMaxRoll,
//                            rightMax = rightMaxRoll,
//                            onSaveData = { score, missed, red, green ->
//                                val result = ColorSorterResult(
//                                    sessionId = UUID.randomUUID().toString(),
//                                    patientId = testViewModel.patient.value.patientId,
//                                    score = score,
//                                    missedCount = missed,
//                                    redCollected = red,
//                                    greenCollected = green
//                                )
//                                testViewModel.saveColorSorterResult(result)
//                            },
//                            onFinish = { navController.popBackStack() }
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun SorterCanvas(
//    currentRoll: Float,
//    leftMax: Float,
//    rightMax: Float,
//    onSaveData: (Int, Int, Int, Int) -> Unit,
//    onFinish: () -> Unit
//) {
//    var activeBalls by remember { mutableStateOf<List<ActiveBall>>(emptyList()) }
//    var activeParticles by remember { mutableStateOf<List<Particle>>(emptyList()) }
//
//    var score by remember { mutableIntStateOf(0) }
//    var missedCount by remember { mutableIntStateOf(0) }
//    var redCollected by remember { mutableIntStateOf(0) }
//    var greenCollected by remember { mutableIntStateOf(0) }
//
//    var isGameOver by remember { mutableStateOf(false) }
//    var dataSaved by remember { mutableStateOf(false) }
//    var canvasSize by remember { mutableStateOf(Size.Zero) }
//    var gameTimeLeft by remember { mutableFloatStateOf(45f) }
//
//    val currentRollState by rememberUpdatedState(currentRoll)
//    val density = LocalDensity.current
//    val textMeasurer = rememberTextMeasurer()
//
//    val ballRadiusPx = with(density) { 18.dp.toPx() }
//    val particleRadiusPx = with(density) { 4.dp.toPx() }
//    val pipeWidthPx = with(density) { 50.dp.toPx() }
//    val boxHeightPx = with(density) { 130.dp.toPx() }
//    val strokeWidthPx = with(density) { 6.dp.toPx() }
//
//    LaunchedEffect(isGameOver) {
//        if (isGameOver) return@LaunchedEffect
//
//        var lastFrameTime = withFrameNanos { it }
//        var timeSinceLastSpawnMs = 0f
//        var ballIdCounter = 0
//
//        while (true) {
//            withFrameNanos { frameTime ->
//                val deltaMs = (frameTime - lastFrameTime) / 1_000_000f
//                lastFrameTime = frameTime
//                val timeMultiplier = deltaMs / 16f
//
//                gameTimeLeft -= (deltaMs / 1000f)
//
//                if (gameTimeLeft <= 0f) {
//                    gameTimeLeft = 0f
//                    if (!dataSaved) {
//                        dataSaved = true
//                        isGameOver = true
//                        onSaveData(score, missedCount, redCollected, greenCollected)
//                    }
//                }
//
//                if (canvasSize != Size.Zero && !isGameOver) {
//                    val w = canvasSize.width
//                    val h = canvasSize.height
//
//                    val pipeTopY = h * 0.35f
//                    val pipeBottomY = h - boxHeightPx - 20f
//
//                    val normalizedPosition = calculateNormalizedPosition(currentRollState, leftMax, rightMax)
//                    val pipeBottomX = (w / 2) + (normalizedPosition * (w / 2 - pipeWidthPx))
//
//                    val elapsed = 45f - gameTimeLeft
//                    val (speedMult, spawnIntervalMs) = when {
//                        elapsed <= 10f -> 1.2f to 1800f
//                        elapsed <= 30f -> 1.8f to 1300f
//                        else -> 2.5f to 900f
//                    }
//
//                    timeSinceLastSpawnMs += deltaMs
//                    val nextBalls = mutableListOf<ActiveBall>()
//                    val nextParticles = mutableListOf<Particle>()
//
//                    for (p in activeParticles) {
//                        val newLifeTime = p.lifeTimeMs + deltaMs
//                        if (newLifeTime < p.maxLifeTimeMs) {
//                            nextParticles.add(p.copy(
//                                x = p.x + p.vx * timeMultiplier, y = p.y + p.vy * timeMultiplier,
//                                vy = p.vy + (0.5f * timeMultiplier), lifeTimeMs = newLifeTime
//                            ))
//                        }
//                    }
//
//                    if (timeSinceLastSpawnMs >= spawnIntervalMs) {
//                        val isRed = (0..1).random() == 0
//                        nextBalls.add(ActiveBall(id = ballIdCounter++, color = if (isRed) RedColor else Green, isRed = isRed, y = pipeTopY, phase = BallPhase.IN_PIPE))
//                        timeSinceLastSpawnMs = 0f
//                    }
//
//                    val boxWidthPx = w * 0.3f
//                    val centerGapPx = w * 0.15f
//                    val leftBoxStart = (w / 2) - (centerGapPx / 2) - boxWidthPx
//                    val leftBoxEnd = leftBoxStart + boxWidthPx
//                    val rightBoxStart = (w / 2) + (centerGapPx / 2)
//                    val rightBoxEnd = rightBoxStart + boxWidthPx
//
//                    for (ball in activeBalls) {
//                        when (ball.phase) {
//                            BallPhase.IN_PIPE -> {
//                                val newY = ball.y + (9.5f * speedMult * timeMultiplier)
//                                if (newY >= pipeBottomY) nextBalls.add(ball.copy(y = newY, phase = BallPhase.FALLING, exitX = pipeBottomX))
//                                else nextBalls.add(ball.copy(y = newY))
//                            }
//                            BallPhase.FALLING -> {
//                                val newY = ball.y + (22f * speedMult * timeMultiplier)
//                                if (newY >= h - boxHeightPx) {
//                                    val hitLeftBox = ball.exitX in leftBoxStart..leftBoxEnd
//                                    val hitRightBox = ball.exitX in rightBoxStart..rightBoxEnd
//                                    var hitSuccess = false
//                                    if (ball.isRed && hitLeftBox) { score += 10; redCollected++; hitSuccess = true }
//                                    else if (!ball.isRed && hitRightBox) { score += 10; greenCollected++; hitSuccess = true }
//                                    else { missedCount++ }
//
//                                    if (hitSuccess) {
//                                        for (i in 0..12) nextParticles.add(Particle(x = ball.exitX, y = h - boxHeightPx, vx = (Random.nextFloat() * 10f - 5f), vy = -(Random.nextFloat() * 8f + 4f), color = ball.color))
//                                    }
//                                } else {
//                                    nextBalls.add(ball.copy(y = newY))
//                                }
//                            }
//                        }
//                    }
//                    activeBalls = nextBalls
//                    activeParticles = nextParticles
//                }
//            }
//        }
//    }
//
//    Box(modifier = Modifier.fillMaxSize()) {
//        Canvas(modifier = Modifier.fillMaxSize()) {
//            canvasSize = size
//            val w = size.width
//            val h = size.height
//
//            val pipeTopY = h * 0.35f
//            val pipeBottomY = h - boxHeightPx - 20f
//            val pipeTopX = w / 2
//
//            val normalizedPosition = calculateNormalizedPosition(currentRoll, leftMax, rightMax)
//            val pipeBottomX = (w / 2) + (normalizedPosition * (w / 2 - pipeWidthPx))
//
//            val boxWidth = w * 0.3f
//            val centerGap = w * 0.15f
//            val leftBoxX = (w / 2) - (centerGap / 2) - boxWidth
//            val rightBoxX = (w / 2) + (centerGap / 2)
//
//            drawRoundRect(color = RedColor, topLeft = Offset(leftBoxX, h - boxHeightPx), size = Size(boxWidth, boxHeightPx), cornerRadius = CornerRadius(20f, 20f), style = Stroke(width = strokeWidthPx))
//            drawRoundRect(color = Green, topLeft = Offset(rightBoxX, h - boxHeightPx), size = Size(boxWidth, boxHeightPx), cornerRadius = CornerRadius(20f, 20f), style = Stroke(width = strokeWidthPx))
//
//            val redTextResult = textMeasurer.measure(text = redCollected.toString(), style = TextStyle(color = RedColor, fontSize = 48.sp, fontWeight = FontWeight.Bold))
//            drawText(textLayoutResult = redTextResult, topLeft = Offset(x = leftBoxX + (boxWidth - redTextResult.size.width) / 2, y = (h - boxHeightPx) + (boxHeightPx - redTextResult.size.height) / 2))
//
//            val greenTextResult = textMeasurer.measure(text = greenCollected.toString(), style = TextStyle(color = Green, fontSize = 48.sp, fontWeight = FontWeight.Bold))
//            drawText(textLayoutResult = greenTextResult, topLeft = Offset(x = rightBoxX + (boxWidth - greenTextResult.size.width) / 2, y = (h - boxHeightPx) + (boxHeightPx - greenTextResult.size.height) / 2))
//
//            drawRoundRect(color = Color(0xFF8D99AE), topLeft = Offset(w / 2 - 40f, 0f), size = Size(80f, pipeTopY), cornerRadius = CornerRadius(10f, 10f))
//            drawLine(color = Color(0xFFEDF2F4).copy(alpha = 0.3f), start = Offset(pipeTopX, pipeTopY), end = Offset(pipeBottomX, pipeBottomY), strokeWidth = pipeWidthPx, cap = StrokeCap.Round)
//
//            activeParticles.forEach { p ->
//                val alpha = (1f - (p.lifeTimeMs / p.maxLifeTimeMs)).coerceIn(0f, 1f)
//                drawCircle(color = p.color.copy(alpha = alpha), radius = particleRadiusPx, center = Offset(p.x, p.y))
//            }
//
//            activeBalls.forEach { ball ->
//                val ballX = when (ball.phase) {
//                    BallPhase.IN_PIPE -> {
//                        val progress = (ball.y - pipeTopY) / (pipeBottomY - pipeTopY)
//                        pipeTopX + (pipeBottomX - pipeTopX) * progress
//                    }
//                    BallPhase.FALLING -> ball.exitX
//                }
//                drawCircle(color = ball.color, radius = ballRadiusPx, center = Offset(ballX, ball.y))
//                drawCircle(color = Color.White.copy(alpha = 0.4f), radius = ballRadiusPx * 0.4f, center = Offset(ballX - 4f, ball.y - 4f))
//            }
//        }
//
//        SorterGameOverlay(
//            score = score,
//            missedCount = missedCount,
//            timeLeft = ceil(gameTimeLeft).toInt(),
//            isGameOver = isGameOver,
//            onRestart = {
//                activeBalls = emptyList(); activeParticles = emptyList()
//                score = 0; missedCount = 0; redCollected = 0; greenCollected = 0
//                gameTimeLeft = 45f; isGameOver = false; dataSaved = false
//            },
//            onFinish = onFinish
//        )
//    }
//}
//
//@Composable
//private fun SorterGameOverlay(
//    score: Int,
//    missedCount: Int,
//    timeLeft: Int,
//    isGameOver: Boolean,
//    onRestart: () -> Unit,
//    onFinish: () -> Unit
//) {
//    Column(modifier = Modifier.fillMaxSize()) {
//        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
//            Text(text = "Score: $score", color = WhiteColor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
//            Text(text = "Time: ${timeLeft}s", color = if (timeLeft <= 15) Color.Red else WhiteColor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
//        }
//
//        if (isGameOver) {
//            Dialog(
//                onDismissRequest = { /* Prevent dismiss to force user to click a button */ },
//                properties = DialogProperties(
//                    usePlatformDefaultWidth = false,
//                    dismissOnBackPress = false,
//                    dismissOnClickOutside = false
//                )
//            ) {
//                Column(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .background(Color(0xFF2B2D42)), // Uses the same background color as the game area
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                    verticalArrangement = Arrangement.Center
//                ) {
//                    Text("TIME'S UP!", color = Color.White, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
//
//                    Spacer(Modifier.height(16.dp))
//
//                    Text("FINAL SCORE", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
//                    Text("$score", color = Green, fontSize = 96.sp, fontWeight = FontWeight.Black)
//
//                    Spacer(Modifier.height(16.dp))
//                    Text("Missed/Dropped: $missedCount", color = RedColor, style = MaterialTheme.typography.titleLarge)
//
//                    Spacer(modifier = Modifier.height(64.dp))
//
//                    Button(
//                        onClick = onRestart,
//                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
//                        shape = RoundedCornerShape(12.dp),
//                        modifier = Modifier.height(56.dp).fillMaxWidth(0.7f)
//                    ) {
//                        Text(text = "PLAY AGAIN", color = WhiteColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
//                    }
//
//                    Spacer(Modifier.height(16.dp))
//
//                    OutlinedButton(
//                        onClick = onFinish,
//                        border = BorderStroke(2.dp, PrimaryColor),
//                        modifier = Modifier.height(56.dp).fillMaxWidth(0.7f),
//                        shape = RoundedCornerShape(12.dp)
//                    ) {
//                        Text("FINISHED GAME", color = PrimaryColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
//                    }
//                }
//            }
//        }
//    }
//}
//
//private fun calculateNormalizedPosition(currentRoll: Float, leftMax: Float, rightMax: Float): Float {
//    val range = rightMax - leftMax
//    if (abs(range) < 0.1f) return 0f
//    return (2 * (currentRoll - leftMax) / range - 1).coerceIn(-1f, 1f)
//}




package com.ripplehealthcare.bproboard.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.ripplehealthcare.bproboard.domain.model.ColorSorterResult
import com.ripplehealthcare.bproboard.ui.viewmodel.BluetoothViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel
import com.ripplehealthcare.bproboard.ui.components.TopBar
import com.ripplehealthcare.bproboard.ui.theme.PrimaryColor
import com.ripplehealthcare.bproboard.ui.theme.RedColor
import com.ripplehealthcare.bproboard.ui.theme.Green
import com.ripplehealthcare.bproboard.ui.theme.WhiteColor
import java.util.UUID
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.random.Random

private sealed class SorterGameState {
    object ReadyToStart : SorterGameState()
    object Playing : SorterGameState()
}

private enum class BallPhase { IN_PIPE, FALLING }

private data class ActiveBall(
    val id: Int, val color: Color, val isRed: Boolean, val y: Float = 0f, val exitX: Float = 0f, val phase: BallPhase = BallPhase.IN_PIPE
)

private data class Particle(
    val x: Float, val y: Float, val vx: Float, val vy: Float, val color: Color, val lifeTimeMs: Float = 0f, val maxLifeTimeMs: Float = 600f
)

@Composable
fun ColorSorterLaunchScreen(onStart: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2B2D42))
            .padding(24.dp)
    ) {
        Text(
            text = "COLOR SORTER",
            color = WhiteColor,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(32.dp))

        Text("• Sort the falling balls into the correct bins", color = Color.LightGray, fontSize = 18.sp, textAlign = TextAlign.Start)
        Spacer(Modifier.height(16.dp))
        Text("• LEAN LEFT to catch RED 🔴", color = Color(0xFFE53935), fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start)
        Spacer(Modifier.height(12.dp))
        Text("• LEAN RIGHT to catch GREEN 🟢", color = Color(0xFF4CAF50), fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start)
        Spacer(Modifier.height(16.dp))
        Text("• You have 45 Seconds", color = Color(0xFFFDD835), fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start)

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
        ) {
            Text("START SORTING", color = WhiteColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ColorSorterGame(
    navController: NavController,
    bluetoothViewModel: BluetoothViewModel,
    testViewModel: TestViewModel
) {
    val sensorData by bluetoothViewModel.sensorData.collectAsState()

    val deltaYaw = (sensorData?.centerYaw ?: bluetoothViewModel.centerYaw) - bluetoothViewModel.centerYaw
    val currentRoll = -deltaYaw

    val leftMaxRoll = -20f
    val rightMaxRoll = 20f

    var gameState by remember { mutableStateOf<SorterGameState>(SorterGameState.ReadyToStart) }

    Scaffold(
        topBar = { TopBar("Color Sorter", onBackClick = { navController.popBackStack() }) },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (gameState) {
                is SorterGameState.ReadyToStart -> {
                    ColorSorterLaunchScreen { gameState = SorterGameState.Playing }
                }
                is SorterGameState.Playing -> {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0xFF2B2D42), MaterialTheme.shapes.medium)) {
                        SorterCanvas(
                            currentRoll = currentRoll,
                            leftMax = leftMaxRoll,
                            rightMax = rightMaxRoll,
                            onSaveData = { score, missed, red, green ->
                                val result = ColorSorterResult(
                                    sessionId = testViewModel.activeSessionId ?: java.util.UUID.randomUUID().toString(),
                                    patientId = testViewModel.patient.value.patientId,
                                    score = score,
                                    missedCount = missed,
                                    redCollected = red,
                                    greenCollected = green
                                )
                                testViewModel.saveColorSorterResult(result)
                            },
                            onFinish = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SorterCanvas(
    currentRoll: Float,
    leftMax: Float,
    rightMax: Float,
    onSaveData: (Int, Int, Int, Int) -> Unit,
    onFinish: () -> Unit
) {
    var activeBalls by remember { mutableStateOf<List<ActiveBall>>(emptyList()) }
    var activeParticles by remember { mutableStateOf<List<Particle>>(emptyList()) }

    var score by remember { mutableIntStateOf(0) }
    var missedCount by remember { mutableIntStateOf(0) }
    var redCollected by remember { mutableIntStateOf(0) }
    var greenCollected by remember { mutableIntStateOf(0) }

    var isGameOver by remember { mutableStateOf(false) }
    var dataSaved by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var gameTimeLeft by remember { mutableFloatStateOf(45f) }

    val currentRollState by rememberUpdatedState(currentRoll)
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val ballRadiusPx = with(density) { 18.dp.toPx() }
    val particleRadiusPx = with(density) { 4.dp.toPx() }
    val pipeWidthPx = with(density) { 50.dp.toPx() }
    val boxHeightPx = with(density) { 130.dp.toPx() }
    val strokeWidthPx = with(density) { 6.dp.toPx() }

    LaunchedEffect(isGameOver) {
        if (isGameOver) return@LaunchedEffect

        var lastFrameTime = withFrameNanos { it }
        var timeSinceLastSpawnMs = 0f
        var ballIdCounter = 0

        while (true) {
            withFrameNanos { frameTime ->
                val deltaMs = (frameTime - lastFrameTime) / 1_000_000f
                lastFrameTime = frameTime
                val timeMultiplier = deltaMs / 16f

                gameTimeLeft -= (deltaMs / 1000f)

                if (gameTimeLeft <= 0f) {
                    gameTimeLeft = 0f
                    if (!dataSaved) {
                        dataSaved = true
                        isGameOver = true
                        onSaveData(score, missedCount, redCollected, greenCollected)
                    }
                }

                if (canvasSize != Size.Zero && !isGameOver) {
                    val w = canvasSize.width
                    val h = canvasSize.height

                    val pipeTopY = h * 0.35f
                    val pipeBottomY = h - boxHeightPx - 20f

                    val normalizedPosition = calculateNormalizedPosition(currentRollState, leftMax, rightMax)
                    val pipeBottomX = (w / 2) + (normalizedPosition * (w / 2 - pipeWidthPx))

                    val elapsed = 45f - gameTimeLeft
                    val (speedMult, spawnIntervalMs) = when {
                        elapsed <= 10f -> 1.2f to 1800f
                        elapsed <= 30f -> 1.8f to 1300f
                        else -> 2.5f to 900f
                    }

                    timeSinceLastSpawnMs += deltaMs
                    val nextBalls = mutableListOf<ActiveBall>()
                    val nextParticles = mutableListOf<Particle>()

                    for (p in activeParticles) {
                        val newLifeTime = p.lifeTimeMs + deltaMs
                        if (newLifeTime < p.maxLifeTimeMs) {
                            nextParticles.add(p.copy(
                                x = p.x + p.vx * timeMultiplier, y = p.y + p.vy * timeMultiplier,
                                vy = p.vy + (0.5f * timeMultiplier), lifeTimeMs = newLifeTime
                            ))
                        }
                    }

                    if (timeSinceLastSpawnMs >= spawnIntervalMs) {
                        val isRed = (0..1).random() == 0
                        nextBalls.add(ActiveBall(id = ballIdCounter++, color = if (isRed) RedColor else Green, isRed = isRed, y = pipeTopY, phase = BallPhase.IN_PIPE))
                        timeSinceLastSpawnMs = 0f
                    }

                    val boxWidthPx = w * 0.3f
                    val centerGapPx = w * 0.15f
                    val leftBoxStart = (w / 2) - (centerGapPx / 2) - boxWidthPx
                    val leftBoxEnd = leftBoxStart + boxWidthPx
                    val rightBoxStart = (w / 2) + (centerGapPx / 2)
                    val rightBoxEnd = rightBoxStart + boxWidthPx

                    for (ball in activeBalls) {
                        when (ball.phase) {
                            BallPhase.IN_PIPE -> {
                                val newY = ball.y + (9.5f * speedMult * timeMultiplier)
                                if (newY >= pipeBottomY) nextBalls.add(ball.copy(y = newY, phase = BallPhase.FALLING, exitX = pipeBottomX))
                                else nextBalls.add(ball.copy(y = newY))
                            }
                            BallPhase.FALLING -> {
                                val newY = ball.y + (22f * speedMult * timeMultiplier)
                                if (newY >= h - boxHeightPx) {
                                    val hitLeftBox = ball.exitX in leftBoxStart..leftBoxEnd
                                    val hitRightBox = ball.exitX in rightBoxStart..rightBoxEnd
                                    var hitSuccess = false
                                    if (ball.isRed && hitLeftBox) { score += 10; redCollected++; hitSuccess = true }
                                    else if (!ball.isRed && hitRightBox) { score += 10; greenCollected++; hitSuccess = true }
                                    else { missedCount++ }

                                    if (hitSuccess) {
                                        for (i in 0..12) nextParticles.add(Particle(x = ball.exitX, y = h - boxHeightPx, vx = (Random.nextFloat() * 10f - 5f), vy = -(Random.nextFloat() * 8f + 4f), color = ball.color))
                                    }
                                } else {
                                    nextBalls.add(ball.copy(y = newY))
                                }
                            }
                        }
                    }
                    activeBalls = nextBalls
                    activeParticles = nextParticles
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            canvasSize = size
            val w = size.width
            val h = size.height

            val pipeTopY = h * 0.35f
            val pipeBottomY = h - boxHeightPx - 20f
            val pipeTopX = w / 2

            val normalizedPosition = calculateNormalizedPosition(currentRoll, leftMax, rightMax)
            val pipeBottomX = (w / 2) + (normalizedPosition * (w / 2 - pipeWidthPx))

            val boxWidth = w * 0.3f
            val centerGap = w * 0.15f
            val leftBoxX = (w / 2) - (centerGap / 2) - boxWidth
            val rightBoxX = (w / 2) + (centerGap / 2)

            drawRoundRect(color = RedColor, topLeft = Offset(leftBoxX, h - boxHeightPx), size = Size(boxWidth, boxHeightPx), cornerRadius = CornerRadius(20f, 20f), style = Stroke(width = strokeWidthPx))
            drawRoundRect(color = Green, topLeft = Offset(rightBoxX, h - boxHeightPx), size = Size(boxWidth, boxHeightPx), cornerRadius = CornerRadius(20f, 20f), style = Stroke(width = strokeWidthPx))

            val redTextResult = textMeasurer.measure(text = redCollected.toString(), style = TextStyle(color = RedColor, fontSize = 48.sp, fontWeight = FontWeight.Bold))
            drawText(textLayoutResult = redTextResult, topLeft = Offset(x = leftBoxX + (boxWidth - redTextResult.size.width) / 2, y = (h - boxHeightPx) + (boxHeightPx - redTextResult.size.height) / 2))

            val greenTextResult = textMeasurer.measure(text = greenCollected.toString(), style = TextStyle(color = Green, fontSize = 48.sp, fontWeight = FontWeight.Bold))
            drawText(textLayoutResult = greenTextResult, topLeft = Offset(x = rightBoxX + (boxWidth - greenTextResult.size.width) / 2, y = (h - boxHeightPx) + (boxHeightPx - greenTextResult.size.height) / 2))

            drawRoundRect(color = Color(0xFF8D99AE), topLeft = Offset(w / 2 - 40f, 0f), size = Size(80f, pipeTopY), cornerRadius = CornerRadius(10f, 10f))
            drawLine(color = Color(0xFFEDF2F4).copy(alpha = 0.3f), start = Offset(pipeTopX, pipeTopY), end = Offset(pipeBottomX, pipeBottomY), strokeWidth = pipeWidthPx, cap = StrokeCap.Round)

            activeParticles.forEach { p ->
                val alpha = (1f - (p.lifeTimeMs / p.maxLifeTimeMs)).coerceIn(0f, 1f)
                drawCircle(color = p.color.copy(alpha = alpha), radius = particleRadiusPx, center = Offset(p.x, p.y))
            }

            activeBalls.forEach { ball ->
                val ballX = when (ball.phase) {
                    BallPhase.IN_PIPE -> {
                        val progress = (ball.y - pipeTopY) / (pipeBottomY - pipeTopY)
                        pipeTopX + (pipeBottomX - pipeTopX) * progress
                    }
                    BallPhase.FALLING -> ball.exitX
                }
                drawCircle(color = ball.color, radius = ballRadiusPx, center = Offset(ballX, ball.y))
                drawCircle(color = Color.White.copy(alpha = 0.4f), radius = ballRadiusPx * 0.4f, center = Offset(ballX - 4f, ball.y - 4f))
            }
        }

        SorterGameOverlay(
            score = score,
            missedCount = missedCount,
            timeLeft = ceil(gameTimeLeft).toInt(),
            isGameOver = isGameOver,
            onRestart = {
                activeBalls = emptyList(); activeParticles = emptyList()
                score = 0; missedCount = 0; redCollected = 0; greenCollected = 0
                gameTimeLeft = 45f; isGameOver = false; dataSaved = false
            },
            onFinish = onFinish
        )
    }
}

@Composable
private fun SorterGameOverlay(
    score: Int,
    missedCount: Int,
    timeLeft: Int,
    isGameOver: Boolean,
    onRestart: () -> Unit,
    onFinish: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Score: $score", color = WhiteColor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = "Time: ${timeLeft}s", color = if (timeLeft <= 15) Color.Red else WhiteColor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        if (isGameOver) {
            Dialog(
                onDismissRequest = { },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF2B2D42)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("TIME'S UP!", color = Color.White, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)

                    Spacer(Modifier.height(16.dp))

                    Text("FINAL SCORE", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("$score", color = Green, fontSize = 96.sp, fontWeight = FontWeight.Black)

                    Spacer(Modifier.height(16.dp))
                    Text("Missed/Dropped: $missedCount", color = RedColor, style = MaterialTheme.typography.titleLarge)

                    Spacer(modifier = Modifier.height(64.dp))

                    Button(
                        onClick = onRestart,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp).fillMaxWidth(0.7f)
                    ) {
                        Text(text = "PLAY AGAIN", color = WhiteColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onFinish,
                        border = BorderStroke(2.dp, PrimaryColor),
                        modifier = Modifier.height(56.dp).fillMaxWidth(0.7f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("FINISHED GAME", color = PrimaryColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun calculateNormalizedPosition(currentRoll: Float, leftMax: Float, rightMax: Float): Float {
    val range = rightMax - leftMax
    if (abs(range) < 0.1f) return 0f
    return (2 * (currentRoll - leftMax) / range - 1).coerceIn(-1f, 1f)
}