package com.ripplehealthcare.frst.ui.screens

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ripplehealthcare.frst.ui.viewmodel.BluetoothViewModel
import com.ripplehealthcare.frst.ui.components.TopBar
import com.ripplehealthcare.frst.ui.theme.CardColor
import com.ripplehealthcare.frst.ui.theme.Green
import com.ripplehealthcare.frst.ui.theme.PrimaryColor
import com.ripplehealthcare.frst.ui.theme.RedColor
import com.ripplehealthcare.frst.ui.theme.TextBlack
import com.ripplehealthcare.frst.ui.theme.TextGray
import com.ripplehealthcare.frst.ui.theme.WhiteColor
import kotlinx.coroutines.delay

private sealed class RunnerGameState {
    object SelectingLeg : RunnerGameState()
    object CalibratingStanding : RunnerGameState()
    object CalibratingTarget : RunnerGameState()
    object ReadyToStart : RunnerGameState()
    object Playing : RunnerGameState()
}

@Composable
fun EndlessRunnerGame(navController: NavController, bluetoothViewModel: BluetoothViewModel) {
    val sensorData by bluetoothViewModel.sensorData.collectAsState()
//    val currentPitch = ((sensorData?.leftPitch ?: 0f) + (sensorData?.rightPitch ?: 0f)) / 2f
    var selectedLegIsLeft by remember { mutableStateOf(true) }

    val currentPitch = if (selectedLegIsLeft) {
        sensorData?.leftPitch ?: 0f
    } else {
        sensorData?.rightPitch ?: 0f
    }
    var runnerGameState by remember { mutableStateOf<RunnerGameState>(RunnerGameState.SelectingLeg) }
    var standingPitch by remember { mutableFloatStateOf(0f) }
    var maxTargetPitch by remember { mutableFloatStateOf(45f) }

    Scaffold(
        topBar = {
            TopBar("Leg Dodge",
                onBackClick = {
                    navController.popBackStack()
                })
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (runnerGameState) {
                is RunnerGameState.SelectingLeg -> {
                    LegSelectionScreen { isLeft ->
                        selectedLegIsLeft = isLeft
                        runnerGameState = RunnerGameState.CalibratingStanding
                    }
                }
                is RunnerGameState.CalibratingStanding -> {
                    CalibrationOverlay("Step 1: Stand Straight", "Neutral position", "Set Standing") {
                        standingPitch = currentPitch
                        runnerGameState = RunnerGameState.CalibratingTarget
                    }
                }
                is RunnerGameState.CalibratingTarget -> {
                    CalibrationOverlay("Target Lift", "Lift your ${if (selectedLegIsLeft) "Left" else "Right"} knee to your max height.", "Set Target") {
                        maxTargetPitch = currentPitch
                        runnerGameState = RunnerGameState.ReadyToStart
                    }
                }
                is RunnerGameState.ReadyToStart -> {
                    ReadyToStartScreen { runnerGameState = RunnerGameState.Playing }
                }
                is RunnerGameState.Playing -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(WhiteColor, MaterialTheme.shapes.medium)
                    ) {
                        RunnerCanvas(
                            currentPitch = currentPitch,
                            minPitch = standingPitch,
                            maxPitch = maxTargetPitch,
                            onLegChange = {
                                runnerGameState = RunnerGameState.SelectingLeg
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LegSelectionScreen(onLegSelected: (Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        Text("Which leg do you want to exercise?", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { onLegSelected(true) },
                modifier = Modifier.weight(1f).height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) {
                Text("LEFT LEG", color = WhiteColor)
            }
            Button(
                onClick = { onLegSelected(false) },
                modifier = Modifier.weight(1f).height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) {
                Text("RIGHT LEG", color = WhiteColor)
            }
        }
    }
}

@Composable
fun RunnerCanvas(currentPitch: Float, minPitch: Float, maxPitch: Float, onLegChange:()->Unit) {
    var obstacles by remember { mutableStateOf(listOf<Obstacle>()) }
    var isGameOver by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }

    val range = maxPitch - minPitch
    val normalizedPosition = ((currentPitch - minPitch) / if (range == 0f) 1f else range).coerceIn(0f, 1f)

    LaunchedEffect(isGameOver) {
        if (isGameOver) return@LaunchedEffect
        while (true) {
            obstacles = obstacles.map { it.copy(x = it.x - 9f) }.filter { it.x > -150f }

            if (obstacles.isEmpty() || obstacles.last().x < 600f) {
                val isTop = (0..1).random() == 1
                obstacles = obstacles + Obstacle(
                    x = 1800f,
                    isHigh = isTop
                )
            }
            score++
            delay(16)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasHeight = size.height
            val canvasWidth = size.width

            // The ball moves between 25% and 75% of screen height
            val trackTop = canvasHeight * 0.25f
            val trackBottom = canvasHeight * 0.75f
            val playerY = trackBottom - (normalizedPosition * (trackBottom - trackTop))
            val playerX = 200f
            val playerRadius = 50f

            // Make obstacles tall enough to reach into the 'track' area
            val obstacleWidth = 140f
            val obstacleHeight = canvasHeight * 0.55f

            obstacles.forEach { obs ->
                val rectTop = if (obs.isHigh) 0f else canvasHeight - obstacleHeight
                val rectOffset = Offset(obs.x, rectTop)
                val rectSize = Size(obstacleWidth, obstacleHeight)

                drawRect(
                    color = if (obs.isHigh) Color(0xFFFF5252) else Color(0xFFFFEB3B),
                    topLeft = rectOffset,
                    size = rectSize
                )

                // Check if ball X is within obstacle width
                if (playerX + playerRadius > obs.x && playerX - playerRadius < obs.x + obstacleWidth) {
                    // Check if ball Y hits the top or bottom obstacle
                    if (obs.isHigh && playerY - playerRadius < obstacleHeight) {
                        isGameOver = true
                    } else if (!obs.isHigh && playerY + playerRadius > canvasHeight - obstacleHeight) {
                        isGameOver = true
                    }
                }
            }

            // Draw Player
            drawCircle(Color.Red, playerRadius, Offset(playerX, playerY))
        }

        // Score Board (Top Right)
        Text(
            text = "SCORE: ${score / 10}",
            color = TextBlack,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.TopEnd).padding(20.dp)
        )

        if (isGameOver) {
            GameOverScreen(finalScore = score / 10, onLegChange) {
                obstacles = emptyList()
                score = 0
                isGameOver = false
            }
        }
    }
}

@Composable
fun CalibrationOverlay(title: String, instruction: String, buttonText: String, onConfirm: () -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = CardColor, disabledContainerColor = CardColor),
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = TextBlack)
            Spacer(Modifier.height(8.dp))
            Text(instruction, textAlign = TextAlign.Center, color = TextGray)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) {
                Text(buttonText, color= WhiteColor)
            }
        }
    }
}

@Composable
fun ReadyToStartScreen(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Calibration Complete!", color = Green, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Stand ready to begin the exercise.", color = TextBlack)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.height(56.dp)
                .fillMaxWidth(0.6f),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
        ) {
            Text("START GAME", color = WhiteColor)
        }
    }
}

@Composable
fun GameOverScreen(finalScore: Int, onLegChange: () -> Unit, onRestart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(WhiteColor.copy(alpha = 0.9f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("GAME OVER", color = Color.Red, style = MaterialTheme.typography.displayLarge)
        Text("Total Score: $finalScore", color = TextBlack, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(30.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) {
                Text("PLAY AGAIN", color = WhiteColor)
            }
            Button(
                onClick = onLegChange,
                colors = ButtonDefaults.buttonColors(containerColor = RedColor)
            ) {
                Text("CHANGE LEG", color = WhiteColor)
            }
        }
    }
}

data class Obstacle(val x: Float, val isHigh: Boolean)