package com.ripplehealthcare.frst.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ripplehealthcare.frst.ui.components.TopBar
import com.ripplehealthcare.frst.ui.theme.*
import com.ripplehealthcare.frst.ui.viewmodel.BluetoothViewModel
import kotlin.math.abs
import kotlin.random.Random

private sealed class ClimbingGameState {
    object CalibratingNeutral : ClimbingGameState()
    object CalibratingLeft : ClimbingGameState()
    object CalibratingRight : ClimbingGameState()
    object ReadyToStart : ClimbingGameState()
    object Playing : ClimbingGameState()
}

// Represents a rock on the wall
private data class Rock(
    val id: Int,
    val isLeft: Boolean?, // null represents the starting ground in the center
    val yOffset: Float    // The vertical position on the infinite climbing wall
)

@Composable
fun RockClimbingGame(navController: NavController, bluetoothViewModel: BluetoothViewModel) {
    val sensorData by bluetoothViewModel.sensorData.collectAsState()

    // NOTE: For side leg raises (hip abduction), this is typically the Roll axis.
    // Update these to leftPitch/rightPitch if your specific hardware orientation requires it.
    val currentLeftRoll = sensorData?.leftRoll ?: 0f
    val currentRightRoll = sensorData?.rightRoll ?: 0f

    var gameState by remember { mutableStateOf<ClimbingGameState>(ClimbingGameState.CalibratingNeutral) }

    // Calibration Data
    var neutralLeft by remember { mutableFloatStateOf(0f) }
    var neutralRight by remember { mutableFloatStateOf(0f) }
    var maxLeft by remember { mutableFloatStateOf(0f) }
    var maxRight by remember { mutableFloatStateOf(0f) }

    Scaffold(
        topBar = { TopBar("Rock Climbing", onBackClick = { navController.popBackStack() }) },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (gameState) {
                is ClimbingGameState.CalibratingNeutral -> {
                    CalibrationOverlay("Step 1: Stand Straight", "Stand with both feet flat on the floor.", "Set Neutral") {
                        neutralLeft = currentLeftRoll
                        neutralRight = currentRightRoll
                        gameState = ClimbingGameState.CalibratingLeft
                    }
                }
                is ClimbingGameState.CalibratingLeft -> {
                    CalibrationOverlay("Step 2: Left Leg Raise", "Do a comfortable side leg raise with your LEFT leg.", "Set Left Limit") {
                        maxLeft = currentLeftRoll
                        gameState = ClimbingGameState.CalibratingRight
                    }
                }
                is ClimbingGameState.CalibratingRight -> {
                    CalibrationOverlay("Step 3: Right Leg Raise", "Do a comfortable side leg raise with your RIGHT leg.", "Set Right Limit") {
                        maxRight = currentRightRoll
                        gameState = ClimbingGameState.ReadyToStart
                    }
                }
                is ClimbingGameState.ReadyToStart -> {
                    ReadyToStartScreen { gameState = ClimbingGameState.Playing }
                }
                is ClimbingGameState.Playing -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFFE0E5EC), MaterialTheme.shapes.medium) // Light stone background
                    ) {
                        ClimbingCanvas(
                            currentLeftRoll = currentLeftRoll,
                            currentRightRoll = currentRightRoll,
                            neutralLeft = neutralLeft,
                            neutralRight = neutralRight,
                            maxLeft = maxLeft,
                            maxRight = maxRight
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClimbingCanvas(
    currentLeftRoll: Float, currentRightRoll: Float,
    neutralLeft: Float, neutralRight: Float,
    maxLeft: Float, maxRight: Float
) {
    // Generate the climbing route
    val totalRocks = 15
    val rocks by remember {
        mutableStateOf(generateRockPath(totalRocks))
    }

    var currentRockIndex by remember { mutableIntStateOf(0) }
    var totalMoves by remember { mutableIntStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }

    // Hysteresis states: Prevent a single raise from triggering multiple jumps
    var isLeftLegRaised by remember { mutableStateOf(false) }
    var isRightLegRaised by remember { mutableStateOf(false) }

    // Normalize sensor data (0.0 = neutral, 1.0 = max raise)
    val leftProgress = if (abs(maxLeft - neutralLeft) > 0.1f) abs(currentLeftRoll - neutralLeft) / abs(maxLeft - neutralLeft) else 0f
    val rightProgress = if (abs(maxRight - neutralRight) > 0.1f) abs(currentRightRoll - neutralRight) / abs(maxRight - neutralRight) else 0f

    // --- GAME LOGIC (Rep Detection) ---
    LaunchedEffect(currentLeftRoll, currentRightRoll, isGameOver) {
        if (isGameOver) return@LaunchedEffect

        // Left Leg Raise Logic
        if (!isLeftLegRaised && leftProgress > 0.6f) { // Trigger at 60% of max raise
            isLeftLegRaised = true
            handleLegRaise(isLeftMove = true, currentRockIndex, rocks) { newIndex ->
                currentRockIndex = newIndex
                totalMoves++
                if (currentRockIndex == rocks.size - 1) isGameOver = true
            }
        } else if (isLeftLegRaised && leftProgress < 0.2f) { // Reset when leg comes back down below 20%
            isLeftLegRaised = false
        }

        // Right Leg Raise Logic
        if (!isRightLegRaised && rightProgress > 0.6f) { // Trigger at 60%
            isRightLegRaised = true
            handleLegRaise(isLeftMove = false, currentRockIndex, rocks) { newIndex ->
                currentRockIndex = newIndex
                totalMoves++
                if (currentRockIndex == rocks.size - 1) isGameOver = true
            }
        } else if (isRightLegRaised && rightProgress < 0.2f) { // Reset below 20%
            isRightLegRaised = false
        }
    }

    // --- RENDERING & ANIMATION ---
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = maxWidth.value * LocalDensity.current.density
        val h = maxHeight.value * LocalDensity.current.density

        // Calculate the physical X position for Left, Center, and Right lanes
        val leftX = w * 0.25f
        val rightX = w * 0.75f
        val centerX = w * 0.5f

        // 1. Animate Player Position
        val currentRock = rocks[currentRockIndex]
        val targetPlayerX = when (currentRock.isLeft) {
            true -> leftX
            false -> rightX
            null -> centerX
        }
        val targetPlayerY = currentRock.yOffset

        val playerOffset by animateOffsetAsState(
            targetValue = Offset(targetPlayerX, targetPlayerY),
            animationSpec = tween(durationMillis = 400),
            label = "player_jump"
        )

        // 2. Animate Camera (Scroll the wall down as the player goes up)
        // We offset the camera so the player stays roughly in the lower third of the screen
        val cameraTargetY = -currentRock.yOffset + (h * 0.7f)
        val cameraOffsetY by animateFloatAsState(
            targetValue = cameraTargetY,
            animationSpec = tween(durationMillis = 500),
            label = "camera_scroll"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            translate(top = cameraOffsetY) {

                // Draw connecting path (route line)
                val path = Path()
                rocks.forEachIndexed { index, rock ->
                    val rx = when (rock.isLeft) { true -> leftX; false -> rightX; null -> centerX }
                    if (index == 0) path.moveTo(rx, rock.yOffset) else path.lineTo(rx, rock.yOffset)
                }
                drawPath(
                    path = path,
                    color = Color.Gray.copy(alpha = 0.3f),
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Draw the Finish Line
                val finishRock = rocks.last()
                drawRoundRect(
                    color = Green,
                    topLeft = Offset(100f, finishRock.yOffset - 100f),
                    size = Size(w - 200f, 60f),
                    cornerRadius = CornerRadius(20f, 20f)
                )

                // Draw the Rocks
                rocks.forEachIndexed { index, rock ->
                    val rx = when (rock.isLeft) { true -> leftX; false -> rightX; null -> centerX }
                    val isNextTarget = index == currentRockIndex + 1
                    val isPassed = index <= currentRockIndex

                    val rockColor = when {
                        isPassed -> Color(0xFF9E9E9E) // Gray for rocks already climbed
                        isNextTarget -> Color(0xFFFFB703) // Bright Yellow/Orange to highlight next target
                        else -> Color(0xFF607D8B) // Darker gray for future rocks
                    }

                    if (rock.isLeft != null) { // Don't draw a rock for the starting ground
                        drawRoundRect(
                            color = rockColor,
                            topLeft = Offset(rx - 60f, rock.yOffset - 30f),
                            size = Size(120f, 60f),
                            cornerRadius = CornerRadius(15f, 15f)
                        )
                    }
                }

                // Draw the Starting Ground
                drawRoundRect(
                    color = Color(0xFF795548),
                    topLeft = Offset(centerX - 150f, rocks[0].yOffset - 20f),
                    size = Size(300f, 100f),
                    cornerRadius = CornerRadius(10f, 10f)
                )

                // Draw the Player Character (Circle)
                drawCircle(
                    color = PrimaryColor,
                    radius = 40f,
                    center = playerOffset
                )
                // Player Inner highlight
                drawCircle(
                    color = WhiteColor,
                    radius = 15f,
                    center = Offset(playerOffset.x, playerOffset.y - 10f)
                )
            }
        }

        // Overlay UI (Score and Game Over Screen)
        ClimbingGameOverlay(
            currentMoves = totalMoves,
            minimumMoves = rocks.size - 1, // Start rock doesn't count
            isGameOver = isGameOver,
            onRestart = {
                currentRockIndex = 0
                totalMoves = 0
                isGameOver = false
            }
        )
    }
}

// Generates a path of rocks with vertical spacing
private fun generateRockPath(count: Int): List<Rock> {
    val rocks = mutableListOf<Rock>()
    rocks.add(Rock(0, isLeft = null, yOffset = 0f)) // Starting ground at bottom

    var currentY = -300f // Move upwards on the canvas
    for (i in 1 until count) {
        val isLeft = Random.nextBoolean()
        rocks.add(Rock(i, isLeft, currentY))
        currentY -= 300f // Space rocks out vertically
    }
    return rocks
}

// Game Rules: Checks if the user raised the correct leg to reach the next rock
private fun handleLegRaise(
    isLeftMove: Boolean,
    currentIndex: Int,
    rocks: List<Rock>,
    onUpdateIndex: (Int) -> Unit
) {
    if (currentIndex >= rocks.size - 1) return // Already at finish

    val nextRock = rocks[currentIndex + 1]
    if (nextRock.isLeft == isLeftMove) {
        // Success! They raised the correct leg. Move to next rock.
        onUpdateIndex(currentIndex + 1)
    } else {
        // Penalty logic could go here (e.g., character shakes, or plays an error sound)
        // For now, we simply don't advance the index, but totalMoves will still increment.
    }
}

@Composable
private fun ClimbingGameOverlay(
    currentMoves: Int,
    minimumMoves: Int,
    isGameOver: Boolean,
    onRestart: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Score Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Moves: $currentMoves",
                color = TextBlack,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Target: $minimumMoves",
                color = Color.Gray,
                style = MaterialTheme.typography.titleMedium
            )
        }

        // Game Over Screen
        if (isGameOver) {
            val efficiency = if (currentMoves > 0) ((minimumMoves.toFloat() / currentMoves) * 100).toInt() else 0

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.95f))
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("SUMMIT REACHED!", color = Green, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Text("Total Leg Raises: $currentMoves", color = TextBlack, style = MaterialTheme.typography.headlineMedium)
                Text("Perfect Run: $minimumMoves", color = Color.Gray, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))

                // Efficiency Rating
                Text(
                    text = "Efficiency: $efficiency%",
                    color = if (efficiency >= 90) Green else if (efficiency >= 70) Color(0xFFFFB703) else RedColor,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(40.dp))
                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp).fillMaxWidth(0.8f)
                ) {
                    Text(text = "CLIMB AGAIN", color = WhiteColor, fontSize = MaterialTheme.typography.titleLarge.fontSize)
                }
            }
        }
    }
}