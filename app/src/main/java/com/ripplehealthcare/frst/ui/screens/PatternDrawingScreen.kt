package com.ripplehealthcare.frst.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ripplehealthcare.frst.domain.model.PatternDrawingResult
import com.ripplehealthcare.frst.ui.viewmodel.BluetoothViewModel
import com.ripplehealthcare.frst.ui.viewmodel.TestViewModel
import kotlin.math.*

data class DrawingLevelConfig(
    val levelName: String,
    val dotRadiusDp: Float,
    val maxTiltDegrees: Float,
    val targetRadiusMultiplier: Float,
    val targets: List<Offset>,
    val maxTimeMs: Long
)

enum class DrawingGameMode { SITTING, STANDING }

private fun generateTargetsForLevel(level: Int): List<Offset> {
    val targets = mutableListOf<Offset>()
    val numSpokes = when (level) { 1 -> 4; 2 -> 6; else -> 8 }
    val outerRadius = 0.8f
    targets.add(Offset(0f, 0f))
    for (i in 0 until numSpokes) {
        val angle = PI * 2 * i / numSpokes - (PI / 2)
        targets.add(Offset(cos(angle).toFloat() * outerRadius, sin(angle).toFloat() * outerRadius))
        targets.add(Offset(0f, 0f))
    }
    return targets
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternDrawingScreen(
    navController: NavController,
    bluetoothViewModel: BluetoothViewModel,
    testViewModel: TestViewModel
) {
    val sensorData by bluetoothViewModel.sensorData.collectAsState()
    val baselinePitch = bluetoothViewModel.centerPitch
    val baselineYaw = bluetoothViewModel.centerYaw

    val gameMode = if (testViewModel.trainingPosture == "SITTING") DrawingGameMode.SITTING else DrawingGameMode.STANDING
    var currentLevel by remember { mutableIntStateOf(1) }
    var highestUnlockedLevel by remember { mutableIntStateOf(1) }

    val infiniteTransition = rememberInfiniteTransition(label = "Goal Pulsing")
    val goalPulseMultiplier by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "Pulsing Value"
    )

    val gameConfig = remember(gameMode, currentLevel) {
        val sensitivity = if (gameMode == DrawingGameMode.SITTING) 14f else 16f
        when (currentLevel) {
            1 -> DrawingLevelConfig("Level 1 (4-Point Star)", 12f, sensitivity, 0.12f, generateTargetsForLevel(1), 30_000L)
            2 -> DrawingLevelConfig("Level 2 (6-Point Star)", 12f, sensitivity, 0.10f, generateTargetsForLevel(2), 45_000L)
            else -> DrawingLevelConfig("Level 3 (8-Point Star)", 12f, sensitivity, 0.08f, generateTargetsForLevel(3), 60_000L)
        }
    }

    val currentPitch = sensorData?.centerPitch ?: baselinePitch
    val currentYaw = -(sensorData?.centerYaw ?: baselineYaw)
    val deltaPitch = currentPitch - baselinePitch
    val deltaYaw = currentYaw - baselineYaw

    val maxGameTimeMs = gameConfig.maxTimeMs
    var isPlaying by remember { mutableStateOf(false) }
    var totalTimeMs by remember { mutableLongStateOf(0L) }
    var showResultDialog by remember { mutableStateOf(false) }
    var fallCount by remember { mutableIntStateOf(0) }

    var currentGoalIndex by remember { mutableIntStateOf(0) }
    var completedSegmentsPath by remember { mutableStateOf(Path()) }
    val pathHistoryFree = remember { mutableStateListOf<Offset>() }

    val frontalDataPoints = remember { mutableListOf<Float>() }
    val sagittalDataPoints = remember { mutableListOf<Float>() }
    val angularErrors = remember { mutableStateListOf<Float>() } // <--- ANGULAR ERROR TRACKER
    var lastSampleTime by remember { mutableLongStateOf(0L) }

    val stopAndSaveData = {
        val result = PatternDrawingResult(
            sessionId = java.util.UUID.randomUUID().toString(),
            patientId = testViewModel.patient.value.patientId,
            gameMode = gameMode.name,
            level = currentLevel,
            levelName = gameConfig.levelName,
            timeTakenMs = totalTimeMs,
            targetsHit = currentGoalIndex,
            totalTargets = gameConfig.targets.size,
            fallCount = fallCount,
            angularErrors = angularErrors.toList(), // <--- SAVING THE LIST
            frontalData = frontalDataPoints.toList(),
            sagittalData = sagittalDataPoints.toList()
        )
        testViewModel.savePatternDrawingResult(result)
        isPlaying = false
        showResultDialog = true
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            frontalDataPoints.clear()
            sagittalDataPoints.clear()
            angularErrors.clear() // Reset on play

            var lastTime = System.currentTimeMillis()
            lastSampleTime = lastTime
            var wasOut = false

            while (isPlaying && totalTimeMs < maxGameTimeMs) {
                withFrameMillis {
                    val now = System.currentTimeMillis()
                    val delta = now - lastTime
                    lastTime = now

                    if (now - lastSampleTime >= 100) {
                        val latestSensor = bluetoothViewModel.sensorData.value
                        val latestPitch = latestSensor?.centerPitch ?: baselinePitch
                        val latestYaw = -(latestSensor?.centerYaw ?: baselineYaw)
                        val pitchDiff = latestPitch - baselinePitch
                        val yawDiff = latestYaw - baselineYaw

                        frontalDataPoints.add(pitchDiff)
                        sagittalDataPoints.add(yawDiff)

                        val pitchRatio = pitchDiff / gameConfig.maxTiltDegrees
                        val yawRatio = yawDiff / gameConfig.maxTiltDegrees
                        val distRatio = sqrt(pitchRatio.pow(2) + yawRatio.pow(2))

                        if (distRatio >= 1.0f) {
                            if (!wasOut) { fallCount++; wasOut = true }
                        } else { wasOut = false }

                        lastSampleTime = now
                    }

                    if (totalTimeMs + delta >= maxGameTimeMs || currentGoalIndex >= gameConfig.targets.size) {
                        totalTimeMs = (totalTimeMs + delta).coerceAtMost(maxGameTimeMs)
                        stopAndSaveData()
                    } else {
                        totalTimeMs += delta
                    }
                }
            }
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val handlePlayStopClick = {
        if (isPlaying) {
            stopAndSaveData()
        } else {
            if (totalTimeMs >= maxGameTimeMs || currentGoalIndex >= gameConfig.targets.size) {
                totalTimeMs = 0L; pathHistoryFree.clear(); completedSegmentsPath = Path(); currentGoalIndex = 0; fallCount = 0
            }
            isPlaying = true
        }
    }

    Scaffold(containerColor = Color(0xFFFAFAFA)) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Column(
                        modifier = Modifier.weight(0.3f).fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PatternHeaderSection(navController, "Pattern Drawing")
                        PatternStatsCard(totalTimeMs, maxGameTimeMs, currentGoalIndex, gameConfig.targets.size, gameConfig.levelName, deltaPitch, deltaYaw, fallCount)
                        PatternControlButtons(isPlaying = isPlaying, onTogglePlay = handlePlayStopClick)

                        LevelSelectionChips(gameMode, currentLevel, highestUnlockedLevel = highestUnlockedLevel, onLevelChange = { currentLevel = it })
                    }
                    Box(modifier = Modifier.weight(0.7f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        PatternCanvas(deltaPitch, deltaYaw, gameConfig, isPlaying, currentGoalIndex, goalPulseMultiplier, completedSegmentsPath, pathHistoryFree) { newPath, index, isWin, error ->
                            if (error > 0f) angularErrors.add(error)
                            completedSegmentsPath = newPath; currentGoalIndex = index
                            if (isWin) stopAndSaveData()
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        PatternHeaderSection(navController, "Pattern Drawing")
                        LevelSelectionChips(gameMode, currentLevel, highestUnlockedLevel = highestUnlockedLevel, onLevelChange = { currentLevel = it })
                    }

                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                        PatternCanvas(deltaPitch, deltaYaw, gameConfig, isPlaying, currentGoalIndex, goalPulseMultiplier, completedSegmentsPath, pathHistoryFree) { newPath, index, isWin, error ->
                            if (error > 0f) angularErrors.add(error)
                            completedSegmentsPath = newPath; currentGoalIndex = index
                            if (isWin) stopAndSaveData()
                        }
                    }

                    PatternStatsCard(totalTimeMs, maxGameTimeMs, currentGoalIndex, gameConfig.targets.size, gameConfig.levelName, deltaPitch, deltaYaw, fallCount, modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth())
                    PatternControlButtons(isPlaying = isPlaying, modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth(), onTogglePlay = handlePlayStopClick)
                }
            }
        }
    }

    if (showResultDialog) {
        val completedPoints = currentGoalIndex
        val totalPoints = gameConfig.targets.size
        val isWin = completedPoints >= totalPoints
        val hasNextLevel = currentLevel < 3 && gameMode == DrawingGameMode.STANDING

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
                    text = if (isWin) "Excellent! You completed the pattern with $fallCount falls. Your results have been saved."
                    else "You hit $completedPoints out of $totalPoints points with $fallCount falls. Your results have been saved."
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeTeal),
                    onClick = {
                        showResultDialog = false
                        totalTimeMs = 0L; pathHistoryFree.clear(); completedSegmentsPath = Path(); currentGoalIndex = 0; fallCount = 0
                        if (isWin && hasNextLevel) {
                            currentLevel++
                        }
                    }
                ) {
                    Text(if (isWin && hasNextLevel) "Proceed to Level ${currentLevel + 1}" else "Retry Level")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResultDialog = false; navController.popBackStack() }) { Text("End Training", color = Color.Red) }
            }
        )
    }
}

@Composable
fun PatternStatsCard(totalTimeMs: Long, maxTimeMs: Long, currentGoalIndex: Int, totalTargets: Int, levelName: String, deltaPitch: Float, deltaYaw: Float, fallCount: Int, modifier: Modifier = Modifier) {
    val progressText = if(totalTimeMs > 0 || currentGoalIndex > 0) "$currentGoalIndex / $totalTargets points" else "0 / $totalTargets points"
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = ThemeTeal), shape = RoundedCornerShape(16.dp)) {
        Column {
            Text(text = levelName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center)
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)

            PatternStatRow("Round Timer", formatPatternTime(totalTimeMs) + " / ${formatPatternTime(maxTimeMs)}")
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
            PatternStatRow("Progress", progressText)
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
            PatternStatRow("Falls", "$fallCount")
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
            PatternStatRow("Frontal", String.format("%.1f Degree", deltaPitch))
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
            PatternStatRow("Sagittal", String.format("%.1f Degree", deltaYaw))
        }
    }
}

@Composable
fun LevelSelectionChips(gameMode: DrawingGameMode, currentLevel: Int, highestUnlockedLevel: Int, onLevelChange: (Int) -> Unit) {
    if (gameMode == DrawingGameMode.STANDING) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 2, 3).forEach { lvl ->
                val isLocked = lvl > highestUnlockedLevel
                FilterChip(selected = currentLevel == lvl, onClick = { onLevelChange(lvl) }, label = { Text(if (isLocked) "Level $lvl 🔒" else "Level $lvl") }, enabled = !isLocked)
            }
        }
    }
}

@Composable
fun PatternCanvas(
    deltaPitch: Float, deltaYaw: Float, gameConfig: DrawingLevelConfig, isPlaying: Boolean, currentGoalIndex: Int, goalPulseMultiplier: Float,
    completedSegmentsPath: Path, pathHistoryFree: MutableList<Offset>, onGoalHit: (Path, Int, Boolean, Float) -> Unit // <--- ADDED FLOAT
) {
    val targetGreen = Color(0xFF4CAF50)
    val targetYellowGoal = Color(0xFFFBC02D)
    val trailColorFree = ThemeTeal.copy(alpha = 0.4f)
    val pointColorInactive = Color.LightGray.copy(alpha = 0.6f)
    val targetRed = Color(0xFFE53935)

    Canvas(modifier = Modifier.fillMaxSize(0.95f)) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val outerRadius = size.width / 2.2f
        val hitRadiusPx = outerRadius * gameConfig.targetRadiusMultiplier

        drawCircle(color = ThemeTeal.copy(alpha = 0.5f), radius = outerRadius, center = Offset(centerX, centerY), style = Stroke(width = 4f))

        drawLine(color = Color.LightGray.copy(alpha = 0.5f), start = Offset(centerX - outerRadius, centerY), end = Offset(centerX + outerRadius, centerY), strokeWidth = 2f)
        drawLine(color = Color.LightGray.copy(alpha = 0.5f), start = Offset(centerX, centerY - outerRadius), end = Offset(centerX, centerY + outerRadius), strokeWidth = 2f)

        drawPath(path = completedSegmentsPath, color = targetGreen, style = Stroke(width = 8f))

        if (pathHistoryFree.size > 1) {
            val freePath = Path().apply {
                moveTo(pathHistoryFree.first().x, pathHistoryFree.first().y)
                for (i in 1 until pathHistoryFree.size) { lineTo(pathHistoryFree[i].x, pathHistoryFree[i].y) }
            }
            drawPath(path = freePath, color = trailColorFree, style = Stroke(width = 4f))
        }

        if (isPlaying || completedSegmentsPath.isEmpty.not()) {
            val targets = gameConfig.targets
            val actualTargetOffsets = targets.map { relOffset ->
                Offset(centerX + (relOffset.x * outerRadius), centerY + (relOffset.y * outerRadius))
            }

            if (actualTargetOffsets.size > 1) {
                val backgroundShapePath = Path().apply {
                    moveTo(actualTargetOffsets.first().x, actualTargetOffsets.first().y)
                    for (i in 1 until actualTargetOffsets.size) { lineTo(actualTargetOffsets[i].x, actualTargetOffsets[i].y) }
                }
                drawPath(path = backgroundShapePath, color = Color.LightGray.copy(alpha = 0.4f), style = Stroke(width = 2f))
            }

            for (i in targets.indices) {
                val actualOffset = actualTargetOffsets[i]
                if (i == currentGoalIndex && isPlaying) {
                    val pulsatingGoalRadius = hitRadiusPx * goalPulseMultiplier
                    drawCircle(color = targetYellowGoal, radius = pulsatingGoalRadius, center = actualOffset)
                    drawCircle(color = targetYellowGoal, radius = hitRadiusPx, center = actualOffset, style = Stroke(width = 2f))
                } else if (i < currentGoalIndex) {
                    drawCircle(color = targetGreen.copy(alpha = 0.8f), radius = hitRadiusPx * 0.8f, center = actualOffset)
                } else {
                    drawCircle(color = pointColorInactive, radius = hitRadiusPx * 0.7f, center = actualOffset)
                }
            }
        }

        val rawDotXOffset = (deltaYaw / gameConfig.maxTiltDegrees) * outerRadius
        val rawDotYOffset = -(deltaPitch / gameConfig.maxTiltDegrees) * outerRadius
        val rawDistancePx = sqrt(rawDotXOffset.pow(2) + rawDotYOffset.pow(2))
        val scale = if (rawDistancePx > outerRadius) outerRadius / rawDistancePx else 1f

        val dotX = centerX + (rawDotXOffset * scale)
        val dotY = centerY + (rawDotYOffset * scale)
        val currentDotOffsetBoardPx = Offset(dotX, dotY)

        if (isPlaying) {
            if (pathHistoryFree.isEmpty() || (currentDotOffsetBoardPx - pathHistoryFree.last()).getDistance() > 3f) {
                pathHistoryFree.add(currentDotOffsetBoardPx)
            }

            if (currentGoalIndex < gameConfig.targets.size) {
                val currentGoalRel = gameConfig.targets[currentGoalIndex]
                val goalActualPx = Offset(centerX + (currentGoalRel.x * outerRadius), centerY + (currentGoalRel.y * outerRadius))
                val distToGoal = sqrt((dotX - goalActualPx.x).pow(2) + (dotY - goalActualPx.y).pow(2))

                if (distToGoal <= hitRadiusPx) {
                    // --- ANGULAR ERROR MATH ---
                    var error = 0f
                    if (abs(currentGoalRel.x) > 0.01f || abs(currentGoalRel.y) > 0.01f) {
                        val userAngleRad = atan2(rawDotYOffset.toDouble(), rawDotXOffset.toDouble())
                        var userAngleDeg = Math.toDegrees(userAngleRad).toFloat()
                        if (userAngleDeg < 0) userAngleDeg += 360f

                        val targetAngleRad = atan2(currentGoalRel.y.toDouble(), currentGoalRel.x.toDouble())
                        var targetAngleDeg = Math.toDegrees(targetAngleRad).toFloat()
                        if (targetAngleDeg < 0) targetAngleDeg += 360f

                        error = abs(targetAngleDeg - userAngleDeg)
                        if (error > 180f) error = 360f - error
                    }
                    // --------------------------

                    val newPath = Path()
                    if (currentGoalIndex == 0) {
                        newPath.moveTo(goalActualPx.x, goalActualPx.y)
                    } else {
                        newPath.addPath(completedSegmentsPath)
                        newPath.lineTo(goalActualPx.x, goalActualPx.y)
                    }
                    val isWin = (currentGoalIndex + 1) >= gameConfig.targets.size
                    onGoalHit(newPath, currentGoalIndex + 1, isWin, error) // Passing Error!
                }
            }
        }
        drawCircle(color = targetRed, radius = gameConfig.dotRadiusDp.dp.toPx(), center = currentDotOffsetBoardPx)
    }
}

@Composable
fun PatternHeaderSection(navController: NavController, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(48.dp).clip(CircleShape).background(ThemeTeal)) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}
@Composable
fun PatternStatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = Color.White, fontSize = 14.sp)
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
@Composable
fun PatternControlButtons(isPlaying: Boolean, modifier: Modifier = Modifier, onTogglePlay: () -> Unit) {
    Button(onClick = onTogglePlay, modifier = modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = ThemeTeal, disabledContainerColor = Color.LightGray), shape = RoundedCornerShape(28.dp)) {
        Text(if (isPlaying) "Stop" else "Start / Retry", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
private fun formatPatternTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}