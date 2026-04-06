package com.ripplehealthcare.bproboard.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ripplehealthcare.bproboard.domain.model.StaticBalanceResult
import com.ripplehealthcare.bproboard.ui.viewmodel.BluetoothViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel
import kotlin.math.pow
import kotlin.math.sqrt

// --- Theme Colors ---
val ThemeTeal = Color(0xFF188B97)
val OuterBgPink = Color(0xFFF3D8D9)
val OuterStrokeRed = Color(0xFFCD5A5F)
val MiddleBgOrange = Color(0xFFF9D199)
val TargetGreen = Color(0xFF5CB85C)
val LightBackground = Color(0xFFFFFFFF)

// --- Game Configuration Data Class ---
data class GameLevelConfig(
    val dotRadiusDp: Float,
    val innerCircleMultiplier: Float,
    val middleCircleMultiplier: Float,
    val maxTiltDegrees: Float,
    val requiredBalanceMs: Long
)

enum class GameMode { SITTING, STANDING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaticBalanceScreen(
    navController: NavController,
    bluetoothViewModel: BluetoothViewModel,
    testViewModel: TestViewModel
) {
    val sensorData by bluetoothViewModel.sensorData.collectAsState()

    val baselinePitch = bluetoothViewModel.centerPitch
    val baselineYaw = bluetoothViewModel.centerYaw

    val gameMode = if (testViewModel.trainingPosture == "SITTING") GameMode.SITTING else GameMode.STANDING
    var currentLevel by remember { mutableIntStateOf(1) }
    var highestUnlockedLevel by remember { mutableIntStateOf(1) }

    val gameConfig = remember(gameMode, currentLevel) {
        if (gameMode == GameMode.SITTING) {
            GameLevelConfig(8f, 0.07f, 0.40f, 6f, 10_000L)
        } else {
            when (currentLevel) {
                1 -> GameLevelConfig(12f, 0.20f, 0.50f, 10f, 10_000L)
                2 -> GameLevelConfig(12f, 0.30f, 0.60f, 10f, 14_000L)
                else -> GameLevelConfig(12f, 0.40f, 0.70f, 10f, 17_000L)
            }
        }
    }

    // Live Sensor values
    val currentPitch = sensorData?.centerPitch ?: baselinePitch
    val currentYaw = -(sensorData?.centerYaw ?: baselineYaw)

    val deltaPitch = currentPitch - baselinePitch
    val deltaYaw = currentYaw - baselineYaw

    // Timers & Game States
    val maxGameTimeMs = 20_000L
    var isPlaying by remember { mutableStateOf(false) }
    var totalTimeMs by remember { mutableLongStateOf(0L) }
    var balanceTimeMs by remember { mutableLongStateOf(0L) }
    var showResultDialog by remember { mutableStateOf(false) }
    var fallCount by remember { mutableIntStateOf(0) }

    // Logic tracking
    var isBalanced by remember { mutableStateOf(true) }
    val pathHistory = remember { mutableStateListOf<Offset>() }

    // Data lists for saving results
    val frontalDataPoints = remember { mutableListOf<Float>() }
    val sagittalDataPoints = remember { mutableListOf<Float>() }
    val fallErrors = remember { mutableStateListOf<Float>() }

    // --- MASTER SAVE FUNCTION ---
    var wasOutTracker = false
    var currentMaxFallErrorTracker = 0f

    val stopAndSaveData = {
        // If the game ended while they were out of bounds, save that last error
        if (wasOutTracker) {
            fallErrors.add(currentMaxFallErrorTracker)
            wasOutTracker = false
            currentMaxFallErrorTracker = 0f
        }

        val imbalanceTime = (totalTimeMs - balanceTimeMs).coerceAtLeast(0)
        val efficiency = if (totalTimeMs > 0L) ((balanceTimeMs.toFloat() / totalTimeMs.toFloat()) * 100).toInt() else 0

        val result = StaticBalanceResult(
            sessionId = java.util.UUID.randomUUID().toString(),
            patientId = testViewModel.patient.value.patientId,
            gameMode = gameMode.name,
            level = if (gameMode == GameMode.STANDING) currentLevel else 1, // <--- Locks Sitting to Level 1
            totalTimeMs = totalTimeMs,
            balanceTimeMs = balanceTimeMs,
            imbalanceTimeMs = imbalanceTime,
            efficiencyPercentage = efficiency,
            fallCount = fallCount,
            fallErrors = fallErrors.toList(),
            frontalData = frontalDataPoints.toList(),
            sagittalData = sagittalDataPoints.toList()
        )

        testViewModel.saveStaticBalanceResult(result)
        isPlaying = false
        showResultDialog = true
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            frontalDataPoints.clear()
            sagittalDataPoints.clear()
            fallErrors.clear()

            var lastTime = System.currentTimeMillis()
            var lastSampleTime = lastTime
            wasOutTracker = false
            currentMaxFallErrorTracker = 0f

            while (isPlaying && totalTimeMs < maxGameTimeMs) {
                withFrameMillis {
                    val now = System.currentTimeMillis()
                    val delta = now - lastTime
                    lastTime = now

                    // --- 100ms Data Sampling ---
                    if (now - lastSampleTime >= 100) {
                        val latestSensor = bluetoothViewModel.sensorData.value
                        val latestPitch = latestSensor?.centerPitch ?: baselinePitch
                        val latestYaw = -(latestSensor?.centerYaw ?: baselineYaw)

                        val pitchDiff = latestPitch - baselinePitch
                        val yawDiff = latestYaw - baselineYaw

                        frontalDataPoints.add(pitchDiff)
                        sagittalDataPoints.add(yawDiff)

                        // --- FALL DETECTION & ERROR LOGIC ---
                        val pitchRatio = pitchDiff / gameConfig.maxTiltDegrees
                        val yawRatio = yawDiff / gameConfig.maxTiltDegrees
                        val distRatio = sqrt(pitchRatio.pow(2) + yawRatio.pow(2))

                        if (distRatio >= 1.0f) {
                            // Calculate how many degrees they are outside the circle
                            val errorDegrees = (distRatio - 1.0f) * gameConfig.maxTiltDegrees
                            if (errorDegrees > currentMaxFallErrorTracker) {
                                currentMaxFallErrorTracker = errorDegrees // Keep track of the worst point of this fall
                            }

                            if (!wasOutTracker) {
                                fallCount++
                                wasOutTracker = true
                            }
                        } else {
                            if (wasOutTracker) {
                                // They came back inside the circle, save the max error from that fall
                                fallErrors.add(currentMaxFallErrorTracker)
                                currentMaxFallErrorTracker = 0f
                                wasOutTracker = false
                            }
                        }

                        lastSampleTime = now
                    }

                    if (totalTimeMs + delta >= maxGameTimeMs) {
                        val remainingDelta = maxGameTimeMs - totalTimeMs
                        totalTimeMs = maxGameTimeMs
                        if (isBalanced) balanceTimeMs += remainingDelta
                        stopAndSaveData()
                    } else {
                        totalTimeMs += delta
                        if (isBalanced) {
                            balanceTimeMs += delta
                        }
                    }
                }
            }
        }
    }

    val handlePlayStopClick = {
        if (isPlaying) {
            stopAndSaveData()
        } else {
            if (totalTimeMs >= maxGameTimeMs) {
                totalTimeMs = 0L; balanceTimeMs = 0L; pathHistory.clear(); fallCount = 0; fallErrors.clear()
            }
            isPlaying = true
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(containerColor = LightBackground) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Column(
                        modifier = Modifier.weight(0.25f).fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        HeaderSection(navController)
                        StatsCard(totalTimeMs, balanceTimeMs, deltaPitch, deltaYaw, fallCount)
                        ControlButtons(isPlaying = isPlaying, onTogglePlay = handlePlayStopClick)

                        LevelSelectionChips(
                            gameMode, currentLevel,
                            highestUnlockedLevel = highestUnlockedLevel,
                            onLevelChange = { currentLevel = it }
                        )
                    }
                    Box(modifier = Modifier.weight(0.75f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        BalanceCanvas(deltaPitch, deltaYaw, gameConfig, isPlaying, pathHistory) { isBalanced = it }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        HeaderSection(navController)

                        LevelSelectionChips(
                            gameMode, currentLevel,
                            highestUnlockedLevel = highestUnlockedLevel,
                            onLevelChange = { currentLevel = it }
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                        BalanceCanvas(deltaPitch, deltaYaw, gameConfig, isPlaying, pathHistory) { isBalanced = it }
                    }

                    StatsCard(
                        totalTimeMs, balanceTimeMs, deltaPitch, deltaYaw, fallCount,
                        modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth()
                    )

                    ControlButtons(
                        isPlaying = isPlaying,
                        modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth(),
                        onTogglePlay = handlePlayStopClick
                    )
                }
            }
        }
    }
    if (showResultDialog) {
        val isWin = balanceTimeMs >= gameConfig.requiredBalanceMs
        val hasNextLevel = currentLevel < 3 && gameMode == GameMode.STANDING

        LaunchedEffect(showResultDialog) {
            if (isWin && currentLevel == highestUnlockedLevel && highestUnlockedLevel < 3) {
                highestUnlockedLevel++
            }
        }

        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    text = if (isWin && !hasNextLevel) "All Levels Complete! 🏆"
                    else if (isWin) "Level $currentLevel Passed! 🎉"
                    else "Time's Up! ⏱️"
                )
            },
            text = {
                Text(
                    text = if (isWin) "Great job! You stayed balanced for ${formatTime(balanceTimeMs)}. You had $fallCount falls."
                    else "You balanced for ${formatTime(balanceTimeMs)}, but needed ${formatTime(gameConfig.requiredBalanceMs)}. You had $fallCount falls."
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeTeal),
                    onClick = {
                        showResultDialog = false
                        totalTimeMs = 0L; balanceTimeMs = 0L; pathHistory.clear(); fallCount = 0; fallErrors.clear()
                        if (isWin && hasNextLevel) {
                            currentLevel++
                        }
                    }
                ) {
                    Text(if (isWin && hasNextLevel) "Proceed to Level ${currentLevel + 1}" else "Retry Level")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showResultDialog = false
                        navController.popBackStack()
                    }
                ) {
                    Text("End Training", color = Color.Red)
                }
            }
        )
    }
}

@Composable
fun LevelSelectionChips(
    gameMode: GameMode,
    currentLevel: Int,
    highestUnlockedLevel: Int,
    onLevelChange: (Int) -> Unit
) {
    // <--- ONLY SHOWS IF STANDING --->
    if (gameMode == GameMode.STANDING) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 2, 3).forEach { lvl ->
                val isLocked = lvl > highestUnlockedLevel
                FilterChip(
                    selected = currentLevel == lvl,
                    onClick = { onLevelChange(lvl) },
                    label = { Text(if (isLocked) "Level $lvl 🔒" else "Level $lvl") },
                    enabled = !isLocked
                )
            }
        }
    }
}

@Composable
fun HeaderSection(navController: NavController) {
    IconButton(
        onClick = { navController.popBackStack() },
        modifier = Modifier.size(48.dp).clip(CircleShape).background(ThemeTeal)
    ) {
        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
    }
}

@Composable
fun StatsCard(totalTimeMs: Long, balanceTimeMs: Long, deltaPitch: Float, deltaYaw: Float, fallCount: Int, modifier: Modifier = Modifier) {
    val imbalanceTimeMs = (totalTimeMs - balanceTimeMs).coerceAtLeast(0)
    val efficiency = if (totalTimeMs > 0) ((balanceTimeMs.toFloat() / totalTimeMs.toFloat()) * 100).toInt() else 0

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ThemeTeal),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Text(
                text = "Static Balance mode", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)

            StatRow("Total time", "${totalTimeMs / 1000} sec")
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
            StatRow("Balance time", "${balanceTimeMs / 1000} sec")
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
            StatRow("Imbalance time", "${imbalanceTimeMs / 1000} sec")
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
            StatRow("Efficiency", "$efficiency %")
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
            StatRow("Falls", "$fallCount")
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
            StatRow("Frontal", String.format("%.1f Degree", deltaPitch))
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
            StatRow("Sagittal", String.format("%.1f Degree", deltaYaw))
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.White, fontSize = 14.sp)
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ControlButtons(isPlaying: Boolean, modifier: Modifier = Modifier, onTogglePlay: () -> Unit) {
    Button(
        onClick = onTogglePlay, modifier = modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ThemeTeal, disabledContainerColor = Color.LightGray),
        shape = RoundedCornerShape(28.dp)
    ) {
        Text(if (isPlaying) "Stop" else "Start / Retry", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BalanceCanvas(
    deltaPitch: Float, deltaYaw: Float, gameConfig: GameLevelConfig, isPlaying: Boolean,
    pathHistory: MutableList<Offset>, onBalanceChange: (Boolean) -> Unit
) {
    Canvas(modifier = Modifier.fillMaxSize(0.95f)) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val outerRadius = size.width / 2f
        val middleRadius = outerRadius * gameConfig.middleCircleMultiplier
        val innerRadius = outerRadius * gameConfig.innerCircleMultiplier

        drawCircle(color = OuterBgPink, radius = outerRadius, center = Offset(centerX, centerY))
        drawCircle(color = OuterStrokeRed, radius = outerRadius, center = Offset(centerX, centerY), style = Stroke(width = 4.dp.toPx()))

        drawCircle(color = MiddleBgOrange, radius = middleRadius, center = Offset(centerX, centerY))
        drawCircle(color = Color.White, radius = middleRadius, center = Offset(centerX, centerY), style = Stroke(width = 2.dp.toPx()))

        drawCircle(color = TargetGreen.copy(alpha = 0.4f), radius = innerRadius, center = Offset(centerX, centerY))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(TargetGreen.copy(alpha = 0.8f), TargetGreen.copy(alpha = 0.2f), Color.Transparent),
                center = Offset(centerX, centerY), radius = innerRadius * 1.2f
            ), radius = innerRadius * 1.2f, center = Offset(centerX, centerY)
        )
        drawCircle(color = Color.White, radius = innerRadius, center = Offset(centerX, centerY), style = Stroke(width = 2.dp.toPx()))

        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
        drawLine(color = ThemeTeal.copy(alpha = 0.3f), start = Offset(centerX - outerRadius, centerY), end = Offset(centerX + outerRadius, centerY), strokeWidth = 3.dp.toPx(), pathEffect = pathEffect)
        drawLine(color = ThemeTeal.copy(alpha = 0.3f), start = Offset(centerX, centerY - outerRadius), end = Offset(centerX, centerY + outerRadius), strokeWidth = 3.dp.toPx(), pathEffect = pathEffect)

        val rawDotXOffset = (deltaYaw / gameConfig.maxTiltDegrees) * outerRadius
        val rawDotYOffset = -(deltaPitch / gameConfig.maxTiltDegrees) * outerRadius

        val rawDistancePx = sqrt(rawDotXOffset.pow(2) + rawDotYOffset.pow(2))
        val scale = if (rawDistancePx > outerRadius) outerRadius / rawDistancePx else 1f

        val dotXOffset = rawDotXOffset * scale
        val dotYOffset = rawDotYOffset * scale
        val distancePx = sqrt(dotXOffset.pow(2) + dotYOffset.pow(2))

        val currentDotOffset = Offset(centerX + dotXOffset, centerY + dotYOffset)

        if (isPlaying) {
            onBalanceChange(distancePx <= innerRadius)
            if (pathHistory.isEmpty() || (currentDotOffset - pathHistory.last()).getDistance() > 3f) {
                pathHistory.add(currentDotOffset)
            }
        } else {
            onBalanceChange(distancePx <= innerRadius)
        }

        if (pathHistory.size > 1) {
            val path = Path().apply {
                moveTo(pathHistory.first().x, pathHistory.first().y)
                for (i in 1 until pathHistory.size) { lineTo(pathHistory[i].x, pathHistory[i].y) }
            }
            drawPath(path = path, color = ThemeTeal.copy(alpha = 0.6f), style = Stroke(width = 6.dp.toPx()))
        }

        drawCircle(color = TargetGreen, radius = gameConfig.dotRadiusDp.dp.toPx(), center = currentDotOffset)
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}