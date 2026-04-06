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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ripplehealthcare.bproboard.domain.model.ShapeTrainingResult
import com.ripplehealthcare.bproboard.ui.viewmodel.BluetoothViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.abs
import androidx.compose.ui.text.style.TextAlign
enum class ShapeType { CIRCLE, SQUARE, TRIANGLE, STAR }

data class TargetNode(val type: ShapeType, val color: Color, val angleDegrees: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShapeTrainingScreen(
    navController: NavController,
    bluetoothViewModel: BluetoothViewModel,
    testViewModel: TestViewModel
) {
    val sensorData by bluetoothViewModel.sensorData.collectAsState()
    val baselinePitch = bluetoothViewModel.centerPitch
    val baselineYaw = bluetoothViewModel.centerYaw

    val gameMode = testViewModel.trainingPosture
    val maxTiltDegrees = if (gameMode == "SITTING") 14f else 16f

    // --- NEW LEVEL STATES ---
    var currentLevel by remember { mutableIntStateOf(1) }
    var highestUnlockedLevel by remember { mutableIntStateOf(1) }

    // Shrink the nodes as the level goes up
    val nodeRadiusDp = when (currentLevel) {
        1 -> 18f
        2 -> 14f
        else -> 10f
    }

    val currentPitch = sensorData?.centerPitch ?: baselinePitch
    val currentYaw = -(sensorData?.centerYaw ?: baselineYaw)
    val deltaPitch = currentPitch - baselinePitch
    val deltaYaw = currentYaw - baselineYaw

    // We shuffle the 8 standard angles so the shapes are in a new configuration every time!
    val targetNodes = remember {
        val shuffledAngles = listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f).shuffled()
        listOf(
            TargetNode(ShapeType.CIRCLE, Color(0xFFE53935), shuffledAngles[0]),      // Red
            TargetNode(ShapeType.SQUARE, Color(0xFF1E88E5), shuffledAngles[1]),      // Blue
            TargetNode(ShapeType.TRIANGLE, Color(0xFF43A047), shuffledAngles[2]),    // Green
            TargetNode(ShapeType.STAR, Color(0xFFFDD835), shuffledAngles[3]),        // Yellow
            TargetNode(ShapeType.CIRCLE, Color(0xFF8E24AA), shuffledAngles[4]),      // Purple
            TargetNode(ShapeType.SQUARE, Color(0xFFF4511E), shuffledAngles[5]),      // Orange
            TargetNode(ShapeType.TRIANGLE, Color(0xFF00ACC1), shuffledAngles[6]),    // Cyan
            TargetNode(ShapeType.STAR, Color(0xFFD81B60), shuffledAngles[7])         // Pink
        )
    }

    val maxGameTimeMs = 30_000L
    var isPlaying by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var activeTimeMs by remember { mutableLongStateOf(0L) }
    var showResultDialog by remember { mutableStateOf(false) }
    var fallCount by remember { mutableIntStateOf(0) }

    var currentTargetIndex by remember { mutableIntStateOf((0..7).random()) }
    val targetSequenceTrack = remember { mutableStateListOf<Int>(currentTargetIndex) }
    val pathHistory = remember { mutableStateListOf<Offset>() }

    val frontalDataPoints = remember { mutableListOf<Float>() }
    val sagittalDataPoints = remember { mutableListOf<Float>() }
    val angularErrors = remember { mutableStateListOf<Float>() }
    var lastSampleTime by remember { mutableLongStateOf(0L) }

    val playerDotColor = Color(0xFFE53935)
    val trailColor = Color(0xFF188B97).copy(alpha = 0.3f)

    val stopAndSaveData = {
        val result = ShapeTrainingResult(
            sessionId = java.util.UUID.randomUUID().toString(),
            patientId = testViewModel.patient.value.patientId,
            score = score,
            timeTakenMs = activeTimeMs,
            fallCount = fallCount,
            level = currentLevel, // <--- SAVING LEVEL
            angularErrors = angularErrors.toList(),
            frontalData = frontalDataPoints.toList(),
            sagittalData = sagittalDataPoints.toList(),
            targetSequence = targetSequenceTrack.toList(),
            targetAngles = targetNodes.map { it.angleDegrees },
            gameMode = gameMode
        )
        testViewModel.saveShapeTrainingResult(result)
        isPlaying = false
        showResultDialog = true
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            frontalDataPoints.clear()
            sagittalDataPoints.clear()
            angularErrors.clear()

            var lastTime = System.currentTimeMillis()
            lastSampleTime = lastTime
            var wasOut = false

            while (isPlaying && activeTimeMs < maxGameTimeMs) {
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

                        val pitchRatio = pitchDiff / maxTiltDegrees
                        val yawRatio = yawDiff / maxTiltDegrees
                        val distRatio = sqrt(pitchRatio.pow(2) + yawRatio.pow(2))

                        if (distRatio >= 1.0f) {
                            if (!wasOut) { fallCount++; wasOut = true }
                        } else { wasOut = false }

                        lastSampleTime = now
                    }

                    if (activeTimeMs + delta >= maxGameTimeMs) {
                        activeTimeMs = maxGameTimeMs
                        stopAndSaveData()
                    } else {
                        activeTimeMs += delta
                    }
                }
            }
        }
    }

    val handlePlayStopClick = {
        if (isPlaying) {
            stopAndSaveData()
        } else {
            if (activeTimeMs >= maxGameTimeMs) {
                activeTimeMs = 0L; score = 0; pathHistory.clear(); fallCount = 0
                currentTargetIndex = (0..7).random()
                targetSequenceTrack.clear()
                targetSequenceTrack.add(currentTargetIndex)
            }
            isPlaying = true
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(containerColor = Color(0xFFFAFAFA)) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Column(
                        modifier = Modifier.weight(0.3f).fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ShapeHeaderSection(navController, "Shape Training")
                        ShapeStatsCard(score, activeTimeMs, deltaPitch, deltaYaw, fallCount, currentLevel, targetNodes[currentTargetIndex])
                        ShapeControlButtons(isPlaying = isPlaying, onTogglePlay = handlePlayStopClick)

                        // LEVEL CHIPS RENDERED HERE
                        ShapeLevelSelectionChips(currentLevel, highestUnlockedLevel) { currentLevel = it }
                    }
                    Box(modifier = Modifier.weight(0.7f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        ShapeCanvas(deltaPitch, deltaYaw, maxTiltDegrees, nodeRadiusDp, isPlaying, pathHistory, targetNodes, currentTargetIndex, playerDotColor, trailColor) { hitTarget, error ->
                            if (hitTarget) {
                                angularErrors.add(error)
                                score++; pathHistory.clear()
                                var newTarget = (0..7).random()
                                while (newTarget == currentTargetIndex) newTarget = (0..7).random()
                                currentTargetIndex = newTarget
                                targetSequenceTrack.add(newTarget)
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        ShapeHeaderSection(navController, "Shape Training")
                        // LEVEL CHIPS RENDERED HERE
                        ShapeLevelSelectionChips(currentLevel, highestUnlockedLevel) { currentLevel = it }
                    }

                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                        ShapeCanvas(deltaPitch, deltaYaw, maxTiltDegrees, nodeRadiusDp, isPlaying, pathHistory, targetNodes, currentTargetIndex, playerDotColor, trailColor) { hitTarget, error ->
                            if (hitTarget) {
                                angularErrors.add(error)
                                score++; pathHistory.clear()
                                var newTarget = (0..7).random()
                                while (newTarget == currentTargetIndex) newTarget = (0..7).random()
                                currentTargetIndex = newTarget
                                targetSequenceTrack.add(newTarget)
                            }
                        }
                    }

                    ShapeStatsCard(score, activeTimeMs, deltaPitch, deltaYaw, fallCount, currentLevel, targetNodes[currentTargetIndex], modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth())
                    ShapeControlButtons(isPlaying = isPlaying, modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth(), onTogglePlay = handlePlayStopClick)
                }
            }
        }
    }

    if (showResultDialog) {
        val isWin = score >= 5 // Win Condition: Score 5 points to advance
        val hasNextLevel = currentLevel < 3

        LaunchedEffect(showResultDialog) {
            if (isWin && currentLevel == highestUnlockedLevel && highestUnlockedLevel < 3) {
                highestUnlockedLevel++
            }
        }

        AlertDialog(
            onDismissRequest = { showResultDialog = false },
            title = {
                Text(
                    text = if (isWin && !hasNextLevel) "All Levels Complete! 🏆"
                    else if (isWin) "Level $currentLevel Passed! 🎉"
                    else "Time's Up! ⏱️"
                )
            },
            text = { Text("Great job! You collected $score shapes in ${activeTimeMs / 1000} seconds. You had $fallCount falls.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF188B97)),
                    onClick = {
                        showResultDialog = false; activeTimeMs = 0L; score = 0; pathHistory.clear(); fallCount = 0
                        currentTargetIndex = (0..7).random()
                        targetSequenceTrack.clear()
                        targetSequenceTrack.add(currentTargetIndex)
                        if (isWin && hasNextLevel) {
                            currentLevel++
                        }
                    }
                ) { Text(if (isWin && hasNextLevel) "Proceed to Level ${currentLevel + 1}" else "Retry Level") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResultDialog = false; navController.popBackStack() }) { Text("End Training", color = Color.Red) }
            }
        )
    }
}

@Composable
fun ShapeLevelSelectionChips(currentLevel: Int, highestUnlockedLevel: Int, onLevelChange: (Int) -> Unit) {
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

@Composable
fun ShapeCanvas(deltaPitch: Float, deltaYaw: Float, maxTiltDegrees: Float, nodeRadiusDp: Float, isPlaying: Boolean, pathHistory: MutableList<Offset>, targetNodes: List<TargetNode>, currentTargetIndex: Int, playerDotColor: Color, trailColor: Color, onTargetHit: (Boolean, Float) -> Unit) {
    Canvas(modifier = Modifier.fillMaxSize(0.95f)) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val outerRadius = size.width / 2.2f
        val targetPlacementRadius = outerRadius * 0.75f
        val nodeRadius = nodeRadiusDp.dp.toPx() // <--- USES DYNAMIC LEVEL SIZE

        drawCircle(color = Color(0xFF188B97), radius = outerRadius, center = Offset(centerX, centerY), style = Stroke(width = 4f))
        drawCircle(color = Color.LightGray, radius = 6.dp.toPx(), center = Offset(centerX, centerY))

        targetNodes.forEachIndexed { index, node ->
            val angleRad = Math.toRadians(node.angleDegrees.toDouble())
            val nodeX = centerX + (targetPlacementRadius * cos(angleRad)).toFloat()
            val nodeY = centerY + (targetPlacementRadius * sin(angleRad)).toFloat()

            if (index == currentTargetIndex && isPlaying) {
                drawCircle(color = node.color.copy(alpha = 0.3f), radius = nodeRadius * 1.6f, center = Offset(nodeX, nodeY))
            }
            drawShape(node.type, node.color, nodeX, nodeY, nodeRadius)
        }

        val rawDotXOffset = (deltaYaw / maxTiltDegrees) * outerRadius
        val rawDotYOffset = -(deltaPitch / maxTiltDegrees) * outerRadius

        val rawDistancePx = sqrt(rawDotXOffset.pow(2) + rawDotYOffset.pow(2))
        val scale = if (rawDistancePx > outerRadius) outerRadius / rawDistancePx else 1f

        val dotX = centerX + (rawDotXOffset * scale)
        val dotY = centerY + (rawDotYOffset * scale)
        val currentDotOffset = Offset(dotX, dotY)

        if (isPlaying) {
            val activeTarget = targetNodes[currentTargetIndex]
            val activeAngleRad = Math.toRadians(activeTarget.angleDegrees.toDouble())
            val targetX = centerX + (targetPlacementRadius * cos(activeAngleRad)).toFloat()
            val targetY = centerY + (targetPlacementRadius * sin(activeAngleRad)).toFloat()

            val distToTarget = sqrt((dotX - targetX).pow(2) + (dotY - targetY).pow(2))

            if (distToTarget <= nodeRadius * 1.5f) {
                val userAngleRad = atan2(rawDotYOffset.toDouble(), rawDotXOffset.toDouble())
                var userAngleDeg = Math.toDegrees(userAngleRad).toFloat()
                if (userAngleDeg < 0) userAngleDeg += 360f

                var targetDeg = activeTarget.angleDegrees
                if (targetDeg < 0) targetDeg += 360f

                var error = abs(targetDeg - userAngleDeg)
                if (error > 180f) error = 360f - error

                onTargetHit(true, error)
            }

            if (pathHistory.isEmpty() || (currentDotOffset - pathHistory.last()).getDistance() > 5f) {
                pathHistory.add(currentDotOffset)
            }
        }

        if (pathHistory.size > 1) {
            val path = Path().apply {
                moveTo(pathHistory.first().x, pathHistory.first().y)
                for (i in 1 until pathHistory.size) { lineTo(pathHistory[i].x, pathHistory[i].y) }
            }
            drawPath(path = path, color = trailColor, style = Stroke(width = 6f))
        }

        drawCircle(color = playerDotColor, radius = 14.dp.toPx(), center = currentDotOffset)
    }
}

@Composable
fun ShapeHeaderSection(navController: NavController, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF188B97))) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable
fun ShapeStatsCard(score: Int, timeMs: Long, deltaPitch: Float, deltaYaw: Float, fallCount: Int, currentLevel: Int, activeNode: TargetNode, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF188B97)), shape = RoundedCornerShape(16.dp)) {
        Column {
            Text(text = "Level $currentLevel", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center)
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)

            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Current Target", color = Color.White, fontSize = 16.sp)
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(16.dp)) { drawShape(activeNode.type, activeNode.color, size.width / 2, size.height / 2, size.width / 2) }
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
            ShapeStatRow("Score", "$score")
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
            ShapeStatRow("Time Remaining", "${(30000L - timeMs) / 1000}s")
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
            ShapeStatRow("Falls", "$fallCount")
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
            ShapeStatRow("Frontal", String.format("%.1f Degree", deltaPitch))
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
            ShapeStatRow("Sagittal", String.format("%.1f Degree", deltaYaw))
        }
    }
}

@Composable
fun ShapeStatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = Color.White, fontSize = 14.sp)
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ShapeControlButtons(isPlaying: Boolean, modifier: Modifier = Modifier, onTogglePlay: () -> Unit) {
    Button(onClick = onTogglePlay, modifier = modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF188B97)), shape = RoundedCornerShape(28.dp)) {
        Text(if (isPlaying) "Stop Session" else "Start 30s Challenge", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

private fun DrawScope.drawShape(type: ShapeType, color: Color, cx: Float, cy: Float, radius: Float) {
    when (type) {
        ShapeType.CIRCLE -> drawCircle(color = color, radius = radius, center = Offset(cx, cy))
        ShapeType.SQUARE -> drawRect(color = color, topLeft = Offset(cx - radius, cy - radius), size = Size(radius * 2, radius * 2))
        ShapeType.TRIANGLE -> {
            val path = Path().apply { moveTo(cx, cy - radius); lineTo(cx + radius, cy + radius); lineTo(cx - radius, cy + radius); close() }
            drawPath(path, color)
        }
        ShapeType.STAR -> {
            val path = Path()
            val innerRadius = radius * 0.4f
            var angle = -Math.PI / 2
            for (i in 0 until 10) {
                val r = if (i % 2 == 0) radius else innerRadius
                val x = cx + (r * cos(angle)).toFloat()
                val y = cy + (r * sin(angle)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                angle += Math.PI / 5
            }
            path.close()
            drawPath(path, color)
        }
    }
}