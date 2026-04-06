package com.ripplehealthcare.bproboard.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ripplehealthcare.bproboard.ui.viewmodel.BluetoothViewModel
import com.ripplehealthcare.bproboard.ui.components.TopBar
import com.ripplehealthcare.bproboard.ui.theme.PrimaryColor
import com.ripplehealthcare.bproboard.ui.theme.TextBlack
import com.ripplehealthcare.bproboard.ui.theme.WhiteColor
import kotlinx.coroutines.delay
import kotlin.math.abs

private sealed class DebrisGameState {
    object CalibratingLeft : DebrisGameState()
    object CalibratingRight : DebrisGameState()
    object ReadyToStart : DebrisGameState()
    object Playing : DebrisGameState()
}

@Composable
fun SpaceDebrisGame(navController: NavController, bluetoothViewModel: BluetoothViewModel) {
    val sensorData by bluetoothViewModel.sensorData.collectAsState()
    val currentRoll = -(sensorData?.centerYaw ?: 0f) // Keep your current inversion

    // Simplified States: No more CalibratingCenter
    var gameState by remember { mutableStateOf<DebrisGameState>(DebrisGameState.CalibratingLeft) }
    var leftMaxRoll by remember { mutableFloatStateOf(-20f) }
    var rightMaxRoll by remember { mutableFloatStateOf(20f) }

    Scaffold(
        topBar = { TopBar("Space Debris", onBackClick = { navController.popBackStack() }) },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (gameState) {
                is DebrisGameState.CalibratingLeft -> {
                    CalibrationOverlay("Step 1: Lean Left", "Lean to your left comfortable limit.", "Set Left Limit") {
                        leftMaxRoll = currentRoll
                        gameState = DebrisGameState.CalibratingRight
                    }
                }
                is DebrisGameState.CalibratingRight -> {
                    CalibrationOverlay("Step 2: Lean Right", "Lean to your right comfortable limit.", "Set Right Limit") {
                        rightMaxRoll = currentRoll
                        gameState = DebrisGameState.ReadyToStart
                    }
                }
                is DebrisGameState.ReadyToStart -> {
                    ReadyToStartScreen { gameState = DebrisGameState.Playing }
                }
                is DebrisGameState.Playing -> {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0xFF0D1B2A), MaterialTheme.shapes.medium)) {
                        DebrisCanvas(
                            currentRoll = currentRoll,
                            leftMax = leftMaxRoll,
                            rightMax = rightMaxRoll
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DebrisCanvas(
    currentRoll: Float,
    leftMax: Float,
    rightMax: Float,
) {
    var debrisList by remember { mutableStateOf(listOf<DebrisItem>()) }
    var score by remember { mutableIntStateOf(0) }
    var missedCount by remember { mutableIntStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    // Constants
    val basketWidth = 120.dp
    val basketHeight = 30.dp
    val debrisRadius = 20.dp
    val basketYOffset = 150.dp

    // Convert Dp to pixels using LocalDensity
    val density = LocalDensity.current
    val basketWidthPx = with(density) { basketWidth.toPx() }
    val basketHeightPx = with(density) { basketHeight.toPx() }
    val debrisRadiusPx = with(density) { debrisRadius.toPx() }
    val basketYOffsetPx = with(density) { basketYOffset.toPx() }

    // Calculate normalized position (between -1 and 1)
    val normalizedPosition = calculateNormalizedPosition(
        currentRoll = currentRoll,
        leftMax = leftMax,
        rightMax = rightMax
    )

    LaunchedEffect(isGameOver, currentRoll, leftMax, rightMax) {
        if (isGameOver) return@LaunchedEffect

        while (true) {
            if (canvasSize != Size.Zero && !isGameOver) {
                val result = updateGameState(
                    debrisList = debrisList,
                    canvasWidth = canvasSize.width,
                    canvasHeight = canvasSize.height,
                    currentRoll = currentRoll,
                    leftMax = leftMax,
                    rightMax = rightMax,
                    basketWidthPx = basketWidthPx,
                    basketHeightPx = basketHeightPx,
                    basketYOffsetPx = basketYOffsetPx,
                    debrisRadiusPx = debrisRadiusPx,
                    currentScore = score,
                    currentMissed = missedCount
                )
                debrisList = result.updatedList
                score = result.newScore
                missedCount = result.newMissedCount
                isGameOver = result.isGameOver
            }
            delay(16) // ~60 FPS
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            canvasSize = size // Store canvas size for game logic

            val w = size.width
            val h = size.height

            // Calculate basket position
            val basketX = (w / 2) + (normalizedPosition * (w / 2 - basketWidthPx / 2))
            val basketY = h - basketYOffsetPx

            // Draw debris
            debrisList.forEach { item ->
                if (!item.isCaught) {
                    drawCircle(
                        color = Color.White,
                        radius = debrisRadiusPx,
                        center = Offset(item.x, item.y)
                    )
                }
            }

            // Draw basket
            drawRoundRect(
                color = PrimaryColor,
                topLeft = Offset(basketX - basketWidthPx / 2, basketY),
                size = Size(basketWidthPx, basketHeightPx),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(15f, 15f)
            )
        }

        // UI Overlay
        GameUIOverlay(
            score = score,
            missedCount = missedCount,
            maxMisses = 5,
            isGameOver = isGameOver,
            onRestart = {
                debrisList = emptyList()
                score = 0
                missedCount = 0
                isGameOver = false
            }
        )
    }
}

private fun calculateNormalizedPosition(
    currentRoll: Float,
    leftMax: Float,
    rightMax: Float
): Float {
    val range = rightMax - leftMax
    if (abs(range) < 0.1f) return 0f

    val normalized = 2 * (currentRoll - leftMax) / range - 1
    return normalized.coerceIn(-1f, 1f)
}

private data class GameUpdateResult(
    val updatedList: List<DebrisItem>,
    val newScore: Int,
    val newMissedCount: Int,
    val isGameOver: Boolean
)

private fun updateGameState(
    debrisList: List<DebrisItem>,
    canvasWidth: Float,
    canvasHeight: Float,
    currentRoll: Float,
    leftMax: Float,
    rightMax: Float,
    basketWidthPx: Float,
    basketHeightPx: Float,
    basketYOffsetPx: Float,
    debrisRadiusPx: Float,
    currentScore: Int,
    currentMissed: Int
): GameUpdateResult {
    var score = currentScore
    var missedCount = currentMissed

    // 1. Precise Position Calculation
    val range = rightMax - leftMax
    val normalizedPos = if (abs(range) > 0.1f) {
        (2 * (currentRoll - leftMax) / range - 1).coerceIn(-1f, 1f)
    } else 0f

    // 2. Synchronize Basket Center with Canvas Drawing
    val basketX = (canvasWidth / 2) + (normalizedPos * (canvasWidth / 2 - basketWidthPx / 2))
    val basketTopY = canvasHeight - basketYOffsetPx
    val basketBottomY = basketTopY + basketHeightPx

    val movedList = debrisList.map { it.copy(y = it.y + 7f) }

    val updatedList = movedList.map { item ->
        var newItem = item
        if (newItem.isCaught || newItem.isCountedAsMiss) return@map newItem

        // 3. ACCURATE HORIZONTAL CHECK
        // Calculate the absolute distance between the center of the ball and center of the basket
        val horizontalDistance = abs(item.x - basketX)

        // Catch happens if the distance is less than half the basket width PLUS the ball radius
        val isWithinWidth = horizontalDistance <= (basketWidthPx / 2 + debrisRadiusPx)

        // 4. ACCURATE VERTICAL CHECK
        // Check if any part of the ball is vertically overlapping the basket's top surface
        val debrisBottom = item.y + debrisRadiusPx
        val debrisTop = item.y - debrisRadiusPx

        // The ball is "at height" if its bottom is past the top of the basket
        // BUT its top hasn't passed the bottom of the basket yet
        val isAtCatchHeight = debrisBottom >= basketTopY && debrisTop <= basketBottomY

        if (isWithinWidth && isAtCatchHeight) {
            score += 10
            newItem = newItem.copy(isCaught = true)
        } else if (item.y > basketBottomY + 50f) {
            missedCount++
            newItem = newItem.copy(isCountedAsMiss = true)
        }

        newItem
    }.filter { it.y < canvasHeight + 100f }

    // Spawn logic
    val finalList = if (updatedList.size < 4 && (0..100).random() < 3) {
        val randomXFactor = 0.15f + (kotlin.random.Random.nextFloat() * 0.7f)
        updatedList + DebrisItem(
            x = randomXFactor * canvasWidth,
            y = -debrisRadiusPx
        )
    } else updatedList

    return GameUpdateResult(finalList, score, missedCount, missedCount >= 5)
}

@Composable
private fun GameUIOverlay(
    score: Int,
    missedCount: Int,
    maxMisses: Int,
    isGameOver: Boolean,
    onRestart: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Score and Lives
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Score: $score",
                color = WhiteColor,
                style = MaterialTheme.typography.headlineSmall
            )

            // Lives Indicator
            Row {
                repeat(maxMisses) { i ->
                    val color = if (i < missedCount) Color.Gray else Color.Red
                    Text(
                        text = "●",
                        color = color,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }

        // Game Over Screen
        if (isGameOver) {
            DebrisGameOverScreen(
                finalScore = score,
                onRestart = onRestart
            )
        }
    }
}

@Composable
fun DebrisGameOverScreen(finalScore: Int, onRestart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteColor.copy(alpha = 0.9f))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "GAME OVER",
            color = Color.Red,
            style = MaterialTheme.typography.displayLarge
        )
        Text(
            text = "Total Score: $finalScore",
            color = TextBlack,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(30.dp))
        Button(
            onClick = onRestart,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
        ) {
            Text(text = "PLAY AGAIN", color = WhiteColor)
        }
    }
}

data class DebrisItem(
    val x: Float,  // Now stores actual X coordinate in pixels
    val y: Float,  // Y coordinate in pixels
    val isCaught: Boolean = false,
    val isCountedAsMiss: Boolean = false
)