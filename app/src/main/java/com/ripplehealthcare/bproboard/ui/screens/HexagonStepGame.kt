package com.ripplehealthcare.bproboard.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.ripplehealthcare.bproboard.domain.model.StepGameResult
import com.ripplehealthcare.bproboard.ui.components.TopBar
import com.ripplehealthcare.bproboard.ui.theme.*
import com.ripplehealthcare.bproboard.ui.viewmodel.BluetoothViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private sealed class StepGameState {
    object Playing : StepGameState()
    data class GameOver(val finalScore: Int) : StepGameState()
}

enum class HexDirection { RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT, LEFT, TOP_LEFT, TOP_RIGHT }

private data class HexBox(
    val direction: HexDirection, val number: Int, val color: Color,
    val isHighlighted: Boolean = false, val isError: Boolean = false
)

val GameColors = listOf(Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFFB300))

@Composable
fun HexagonStepGame(
    navController: NavController,
    bluetoothViewModel: BluetoothViewModel,
    testViewModel: TestViewModel
) {
    val sensorData by bluetoothViewModel.sensorData.collectAsState()

    val currentPitch = bluetoothViewModel.centerPitch - (sensorData?.centerPitch ?: bluetoothViewModel.centerPitch)
    val currentYaw = -((sensorData?.centerYaw ?: bluetoothViewModel.centerYaw) - bluetoothViewModel.centerYaw)

    var gameState by remember { mutableStateOf<StepGameState>(StepGameState.Playing) }

    Scaffold(
        topBar = { TopBar("Cognitive Stepping", onBackClick = { navController.popBackStack() }) },
        containerColor = Color(0xFF1E1E24)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when (val state = gameState) {
                is StepGameState.Playing -> {
                    StepGameCanvas(
                        currentPitch = currentPitch, currentYaw = currentYaw,
                        onGameOver = { score, correct, incorrect ->
                            val result = StepGameResult(
                                sessionId = testViewModel.activeSessionId ?: java.util.UUID.randomUUID().toString(),
                                patientId = testViewModel.patient.value.patientId,
                                score = score, correctHits = correct, incorrectHits = incorrect
                            )
                            testViewModel.saveStepGameResult(result)
                            gameState = StepGameState.GameOver(score)
                        }
                    )
                }
                is StepGameState.GameOver -> {
                    StepGameOverScreen(
                        score = state.finalScore,
                        onRestart = { gameState = StepGameState.Playing },
                        onFinish = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepGameCanvas(
    currentPitch: Float, currentYaw: Float,
    onGameOver: (Int, Int, Int) -> Unit
) {
    var score by remember { mutableIntStateOf(0) }
    var correctHits by remember { mutableIntStateOf(0) }
    var incorrectHits by remember { mutableIntStateOf(0) }

    var targetNumber by remember { mutableIntStateOf((10..99).random()) }
    var targetColor by remember { mutableStateOf(GameColors.random()) }
    var boxes by remember { mutableStateOf(generateCognitiveRound(targetNumber, targetColor)) }

    var needsToCenter by remember { mutableStateOf(false) }
    var hoveredDirection by remember { mutableStateOf<HexDirection?>(null) }
    var showFeedback by remember { mutableStateOf<Boolean?>(null) }

    val maxGameTimeMs = 90_000L
    var timeLeftMs by remember { mutableLongStateOf(maxGameTimeMs) }
    var gameStartTime by remember { mutableLongStateOf(0L) }
    var dataSaved by remember { mutableStateOf(false) }

    val stepThreshold = 11f
    val maxTilt = 20f

    val currentPitchState by rememberUpdatedState(currentPitch)
    val currentYawState by rememberUpdatedState(currentYaw)

    LaunchedEffect(Unit) {
        if (gameStartTime == 0L) gameStartTime = System.currentTimeMillis()

        var lastInteractionTime = 0L
        var stateStage = 0

        while (true) {
            withFrameMillis { frameTime ->
                val now = System.currentTimeMillis()
                val elapsedMs = now - gameStartTime
                timeLeftMs = (maxGameTimeMs - elapsedMs).coerceAtLeast(0L)

                if (timeLeftMs <= 0 && !dataSaved) {
                    dataSaved = true
                    onGameOver(score, correctHits, incorrectHits)
                }

                if (timeLeftMs > 0) {
                    val p = currentPitchState
                    val y = currentYawState
                    val tiltMagnitude = sqrt(y * y + p * p)

                    when (stateStage) {
                        0 -> {
                            val angleDeg = Math.toDegrees(atan2(-p.toDouble(), y.toDouble()))
                            val pointedDirection = mapAngleToHexDirection(angleDeg)

                            if (tiltMagnitude > 4f) { hoveredDirection = pointedDirection }
                            else { hoveredDirection = null }

                            if (tiltMagnitude > stepThreshold) {
                                hoveredDirection = null

                                val selectedBox = boxes.find { it.direction == pointedDirection }

                                if (selectedBox?.number == targetNumber && selectedBox.color == targetColor) {
                                    score += 10
                                    correctHits++
                                    showFeedback = true
                                    boxes = boxes.map { if (it.direction == pointedDirection) it.copy(isHighlighted = true) else it }
                                } else {
                                    if (selectedBox?.number == targetNumber) score += 5
                                    incorrectHits++
                                    showFeedback = false
                                    boxes = boxes.map { if (it.direction == pointedDirection) it.copy(isError = true) else it }
                                }

                                lastInteractionTime = now
                                stateStage = 1
                            }
                        }
                        1 -> {
                            if (now - lastInteractionTime > 800L) {
                                needsToCenter = true
                                showFeedback = null
                                stateStage = 2
                            }
                        }
                        2 -> {
                            if (tiltMagnitude < 5f) {
                                targetNumber = (10..99).random()
                                targetColor = GameColors.random()
                                boxes = generateCognitiveRound(targetNumber, targetColor)

                                needsToCenter = false
                                stateStage = 0
                            }
                        }
                    }
                }
            }
            delay(16)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val seconds = timeLeftMs / 1000
        val timeFormatted = String.format("%02d:%02d", seconds / 60, seconds % 60)

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("TIME", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(timeFormatted, color = if (seconds <= 10) Color.Red else WhiteColor, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }
            if (needsToCenter) {
                Text("RETURN TO CENTER!", color = Color.Yellow, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterVertically))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("SCORE", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("$score", color = Color(0xFF66FCF1), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val radiusX = maxWidth / 3f
            val radiusY = maxHeight / 3.5f

            boxes.forEach { box ->
                val offset = getOffsetForDirection(box.direction, radiusX, radiusY)
                BoxCard(
                    number = box.number, baseColor = box.color,
                    isHovered = box.direction == hoveredDirection,
                    isHighlighted = box.isHighlighted, isError = box.isError,
                    modifier = Modifier.offset(x = offset.first, y = offset.second)
                )
            }

            Box(modifier = Modifier.size(130.dp).clip(CircleShape).background(if (needsToCenter) Color.Yellow else targetColor), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(110.dp).clip(CircleShape).background(Color(0xFF1E1E24)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (needsToCenter) "CENTER" else "FIND", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelLarge)
                        if (!needsToCenter) {
                            Text(text = targetNumber.toString(), color = targetColor, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            FeedbackAnimation(
                showFeedback = showFeedback,
                modifier = Modifier.align(Alignment.Center).offset(y = (-40).dp)
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2; val cy = size.height / 2
                val normalizedX = (currentYawState / maxTilt).coerceIn(-1f, 1f)
                val normalizedY = (currentPitchState / maxTilt).coerceIn(-1f, 1f)
                val cursorX = cx + (normalizedX * (radiusX.toPx() * 1.5f))
                val cursorY = cy + (normalizedY * (radiusY.toPx() * 1.5f))

                drawLine(color = Color.White.copy(alpha = 0.3f), start = Offset(cx, cy), end = Offset(cursorX, cursorY), strokeWidth = 4f)
                drawCircle(color = Color(0xFF66FCF1).copy(alpha = 0.4f), radius = 30f, center = Offset(cursorX, cursorY))
                drawCircle(color = Color.White, radius = 10f, center = Offset(cursorX, cursorY))
            }
        }
    }
}

@Composable
private fun FeedbackAnimation(showFeedback: Boolean?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = showFeedback != null,
        enter = scaleIn(spring(dampingRatio = 0.5f)) + fadeIn(),
        exit = scaleOut(tween(200)) + fadeOut(),
        modifier = modifier
    ) {
        val isSuccess = showFeedback == true
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Color(0xBB000000), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Filled.ThumbUp else Icons.Filled.ThumbDown,
                contentDescription = if (isSuccess) "Correct!" else "Wrong!",
                tint = if (isSuccess) Color.Green else Color.Red,
                modifier = Modifier.size(60.dp)
            )
        }
    }
}

@Composable
private fun BoxCard(number: Int, baseColor: Color, isHovered: Boolean, isHighlighted: Boolean, isError: Boolean, modifier: Modifier = Modifier) {
    val bgColor by animateColorAsState(targetValue = when { isHighlighted -> Green; isError -> RedColor; else -> baseColor }, animationSpec = tween(200), label = "boxColor")
    val scale by animateFloatAsState(targetValue = if (isHovered && !isHighlighted && !isError) 1.2f else 1.0f, label = "scale")

    Card(modifier = modifier.size(90.dp).scale(scale), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = bgColor), elevation = CardDefaults.cardElevation(if (isHovered) 16.dp else 8.dp)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = number.toString(), color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }
    }
}

private fun generateCognitiveRound(targetNumber: Int, targetColor: Color): List<HexBox> {
    val directions = HexDirection.values().toMutableList().apply { shuffle() }
    val newBoxes = mutableListOf<HexBox>()

    newBoxes.add(HexBox(directions.removeAt(0), targetNumber, targetColor))
    val wrongColor1 = GameColors.filter { it != targetColor }.random()
    newBoxes.add(HexBox(directions.removeAt(0), targetNumber, wrongColor1))
    val wrongColor2 = GameColors.filter { it != targetColor && it != wrongColor1 }.random()
    newBoxes.add(HexBox(directions.removeAt(0), targetNumber, wrongColor2))
    var wrongNumber1 = (10..99).random()
    while (wrongNumber1 == targetNumber) wrongNumber1 = (10..99).random()
    newBoxes.add(HexBox(directions.removeAt(0), wrongNumber1, targetColor))

    for (i in 0..1) {
        var randomNum = (10..99).random()
        while (randomNum == targetNumber) randomNum = (10..99).random()
        newBoxes.add(HexBox(directions.removeAt(0), randomNum, GameColors.random()))
    }
    return newBoxes
}

private fun mapAngleToHexDirection(angleDeg: Double): HexDirection = when {
    angleDeg >= -30 && angleDeg < 30 -> HexDirection.RIGHT
    angleDeg >= 30 && angleDeg < 90 -> HexDirection.TOP_RIGHT
    angleDeg >= 90 && angleDeg < 150 -> HexDirection.TOP_LEFT
    angleDeg >= 150 || angleDeg < -150 -> HexDirection.LEFT
    angleDeg >= -150 && angleDeg < -90 -> HexDirection.BOTTOM_LEFT
    else -> HexDirection.BOTTOM_RIGHT
}

private fun getOffsetForDirection(direction: HexDirection, radiusX: Dp, radiusY: Dp): Pair<Dp, Dp> {
    val angleRad = Math.toRadians(when (direction) { HexDirection.RIGHT -> 0.0; HexDirection.TOP_RIGHT -> 60.0; HexDirection.TOP_LEFT -> 120.0; HexDirection.LEFT -> 180.0; HexDirection.BOTTOM_LEFT -> 240.0; HexDirection.BOTTOM_RIGHT -> 300.0 })
    return Pair(radiusX * cos(angleRad).toFloat(), -(radiusY * sin(angleRad).toFloat()))
}

@Composable
fun StepGameOverScreen(score: Int, onRestart: () -> Unit, onFinish: () -> Unit) {
    Dialog(
        onDismissRequest = { /* Prevent dismiss to force user to click a button */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E24)), // Removed alpha so it fully blocks the screen behind it
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("TIME'S UP!", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(16.dp))

            Text("FINAL SCORE", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("$score", color = Color(0xFF66FCF1), fontSize = 96.sp, fontWeight = FontWeight.Black)

            Spacer(Modifier.height(64.dp))

            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                modifier = Modifier.height(56.dp).fillMaxWidth(0.7f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("TRY AGAIN", color = WhiteColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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