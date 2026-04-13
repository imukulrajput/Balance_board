package com.ripplehealthcare.bproboard.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ripplehealthcare.bproboard.domain.model.StarshipResult
import com.ripplehealthcare.bproboard.ui.viewmodel.BluetoothViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel
import com.ripplehealthcare.bproboard.ui.components.TopBar
import com.ripplehealthcare.bproboard.ui.theme.PrimaryColor
import com.ripplehealthcare.bproboard.ui.theme.RedColor
import com.ripplehealthcare.bproboard.ui.theme.Green
import com.ripplehealthcare.bproboard.ui.theme.WhiteColor
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.math.abs

private sealed class StarshipState {
    object ReadyToStart : StarshipState()
    object Playing : StarshipState()
}

@Composable
fun StarshipDefenderGame(
    navController: NavController,
    bluetoothViewModel: BluetoothViewModel,
    testViewModel: TestViewModel
) {
    // Intercept the physical system back button
    BackHandler {
        navController.popBackStack()
    }

    val sensorData by bluetoothViewModel.sensorData.collectAsState()

    val deltaYaw = (sensorData?.centerYaw ?: bluetoothViewModel.centerYaw) - bluetoothViewModel.centerYaw
    val deltaPitch = (sensorData?.centerPitch ?: bluetoothViewModel.centerPitch) - bluetoothViewModel.centerPitch
    val currentRoll = -deltaYaw
    val currentPitch = deltaPitch

    var gameState by remember { mutableStateOf<StarshipState>(StarshipState.ReadyToStart) }

    Scaffold(
        topBar = { TopBar("Starship Defender", onBackClick = { navController.popBackStack() }) },
        containerColor = Color(0xFFF8F9FA) // FIX: Changed to Light color so TopBar Back Button is clearly visible!
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0B0C10)) // FIX: Game area kept dark for space theme
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (gameState) {
                is StarshipState.ReadyToStart -> { StarshipLaunchScreen { gameState = StarshipState.Playing } }
                is StarshipState.Playing -> {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0xFF1F2833), MaterialTheme.shapes.medium)) {
                        StarshipCanvas(
                            currentRoll = currentRoll,
                            currentPitch = currentPitch,
                            onSaveData = { score, aliensHit, timeSurvivedMs, isWin ->
                                val result = StarshipResult(
                                    sessionId = testViewModel.activeSessionId ?: java.util.UUID.randomUUID().toString(),
                                    patientId = testViewModel.patient.value.patientId,
                                    score = score, aliensDestroyed = aliensHit,
                                    timeSurvivedMs = timeSurvivedMs, isWin = isWin
                                )
                                testViewModel.saveStarshipResult(result)
                            },
                            onFinishGame = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StarshipCanvas(
    currentRoll: Float, currentPitch: Float,
    onSaveData: (Int, Int, Long, Boolean) -> Unit,
    onFinishGame: () -> Unit
) {
    val latestRoll by rememberUpdatedState(currentRoll)
    val latestPitch by rememberUpdatedState(currentPitch)

    var obstacles by remember { mutableStateOf(listOf<SpaceEntity>()) }
    var lasers by remember { mutableStateOf(listOf<LaserBeam>()) }

    var score by remember { mutableIntStateOf(0) }
    var aliensDestroyed by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }

    // EXACT 1-MINUTE TIMER STATE
    val maxGameTimeMs = 60_000L
    var timeLeftMs by remember { mutableLongStateOf(maxGameTimeMs) }
    var gameStartTime by remember { mutableLongStateOf(0L) }

    var isGameOver by remember { mutableStateOf(false) }
    var dataSaved by remember { mutableStateOf(false) }

    var fireCooldown by remember { mutableIntStateOf(0) }
    var flameFlicker by remember { mutableFloatStateOf(0f) }

    var canvasWidth by remember { mutableFloatStateOf(0f) }
    var canvasHeight by remember { mutableFloatStateOf(0f) }
    var smoothedNormalizedX by remember { mutableFloatStateOf(0f) }

    // SLEEK FIGHTER SHIP SHAPE
    val shipWidth = 100f
    val shipHeight = 120f
    val shipYOffset = 100f

    val baseAlienPath = remember {
        Path().apply {
            moveTo(0f, -30f); lineTo(30f, 0f); lineTo(0f, 30f); lineTo(-30f, 0f); close()
        }
    }

    val baseShipPath = remember {
        Path().apply {
            moveTo(0f, -shipHeight / 2)
            lineTo(shipWidth * 0.2f, -shipHeight * 0.1f)
            lineTo(shipWidth / 2, shipHeight * 0.2f)
            lineTo(shipWidth * 0.3f, shipHeight / 2)
            lineTo(shipWidth * 0.1f, shipHeight * 0.4f)
            lineTo(0f, shipHeight * 0.3f)
            lineTo(-shipWidth * 0.1f, shipHeight * 0.4f)
            lineTo(-shipWidth * 0.3f, shipHeight / 2)
            lineTo(-shipWidth / 2, shipHeight * 0.2f)
            lineTo(-shipWidth * 0.2f, -shipHeight * 0.1f)
            close()
        }
    }

    LaunchedEffect(isGameOver) {
        if (isGameOver) return@LaunchedEffect

        // Lock in the start time exactly once
        if (gameStartTime == 0L) gameStartTime = System.currentTimeMillis()

        while (true) {
            withFrameMillis { frameTime ->
                if (canvasWidth > 0 && canvasHeight > 0) {

                    // --- EXACT TIMER LOGIC ---
                    val elapsedMs = System.currentTimeMillis() - gameStartTime
                    timeLeftMs = (maxGameTimeMs - elapsedMs).coerceAtLeast(0L)
                    val elapsedSec = elapsedMs / 1000f

                    if (timeLeftMs <= 0 || lives <= 0) {
                        if (!dataSaved) {
                            dataSaved = true
                            isGameOver = true
                            val survivedMs = maxGameTimeMs - timeLeftMs
                            onSaveData(score, aliensDestroyed, survivedMs, timeLeftMs <= 0)
                        }
                    }

                    // --- PROGRESSIVE DIFFICULTY ENGINE ---
                    val speedMultiplier = 1f + (elapsedSec / 40f)
                    val currentSpawnChance = 3 + (elapsedSec / 10f).toInt()

                    flameFlicker = (0..15).random().toFloat()

                    val targetNormalizedX = (latestRoll / 20f).coerceIn(-1f, 1f)
                    smoothedNormalizedX += (targetNormalizedX - smoothedNormalizedX) * 0.08f

                    val shipX = (canvasWidth / 2) + (smoothedNormalizedX * (canvasWidth / 2 - shipWidth / 2))
                    val shipY = canvasHeight - shipYOffset

                    if (fireCooldown > 0) fireCooldown--

                    // Fire lasers - Reduced pitch threshold to 3f
                    if (latestPitch > 3f && fireCooldown <= 0) {
                        val newLasers = listOf(
                            LaserBeam(x = shipX - shipWidth * 0.4f, y = shipY, isLeft = true),
                            LaserBeam(x = shipX + shipWidth * 0.4f, y = shipY, isLeft = false)
                        )
                        lasers = lasers + newLasers
                        fireCooldown = 12
                    }

                    val currentLasers = lasers.map { it.copy(y = it.y - 30f) }.filter { it.y > -50f }
                    val remainingObstacles = mutableListOf<SpaceEntity>()
                    val remainingLasers = currentLasers.toMutableList()

                    obstacles.forEach { obs ->
                        val movedObs = obs.copy(y = obs.y + obs.speed)
                        var obstacleDestroyed = false

                        val laserHitIndex = remainingLasers.indexOfFirst { laser ->
                            abs(laser.x - movedObs.x) < movedObs.radius && abs(laser.y - movedObs.y) < movedObs.radius
                        }

                        if (laserHitIndex != -1) {
                            remainingLasers.removeAt(laserHitIndex)
                            if (movedObs.type == EntityType.ALIEN) {
                                score += 10
                                aliensDestroyed++
                                obstacleDestroyed = true
                            }
                        }

                        if (!obstacleDestroyed && abs(shipX - movedObs.x) < (shipWidth / 2.5f + movedObs.radius * 0.8f) &&
                            abs(shipY - movedObs.y) < (shipHeight / 2.5f + movedObs.radius * 0.8f)) {
                            lives -= 1
                            obstacleDestroyed = true
                        }

                        if (!obstacleDestroyed && movedObs.y < canvasHeight + 100f) {
                            remainingObstacles.add(movedObs)
                        }
                    }

                    lasers = remainingLasers
                    obstacles = remainingObstacles

                    // --- SPAWNER ---
                    if (obstacles.isEmpty() || obstacles.last().y > (150f / speedMultiplier)) {
                        if ((1..100).random() <= currentSpawnChance) {
                            val isAlien = (1..100).random() > 40
                            val minSpawn = 50; val maxSpawn = (canvasWidth - 50).toInt()
                            if (maxSpawn > minSpawn) {
                                val randomX = (minSpawn..maxSpawn).random().toFloat()
                                val baseSpeed = (6..11).random().toFloat()

                                obstacles = obstacles + SpaceEntity(
                                    x = randomX, y = -50f,
                                    speed = baseSpeed * speedMultiplier,
                                    radius = 30f,
                                    type = if (isAlien) EntityType.ALIEN else EntityType.METEOR
                                )
                            }
                        }
                    }
                }
            }
            delay(16)
        }
    }

    Box(modifier = Modifier.fillMaxSize().onSizeChanged { size -> canvasWidth = size.width.toFloat(); canvasHeight = size.height.toFloat() }) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val shipX = (canvasWidth / 2) + (smoothedNormalizedX * (canvasWidth / 2 - shipWidth / 2))
            val shipY = canvasHeight - shipYOffset

            lasers.forEach { laser ->
                drawLine(color = if (laser.isLeft) Color.Cyan else Color.Magenta, start = Offset(laser.x, laser.y), end = Offset(laser.x, laser.y - 40f), strokeWidth = 6f)
            }

            obstacles.forEach { obs ->
                if (obs.type == EntityType.METEOR) {
                    drawCircle(color = Color.Gray, radius = obs.radius, center = Offset(obs.x, obs.y))
                    drawCircle(color = Color.DarkGray, radius = obs.radius * 0.3f, center = Offset(obs.x - 5f, obs.y - 5f))
                } else {
                    translate(left = obs.x, top = obs.y) { drawPath(path = baseAlienPath, color = Color(0xFF66FCF1)) }
                }
            }

            translate(left = shipX, top = shipY) {
                drawPath(path = baseShipPath, color = Color(0xFF4285F4))

                val flamePath = Path().apply {
                    moveTo(-10f, shipHeight * 0.3f)
                    lineTo(0f, shipHeight * 0.6f + flameFlicker)
                    lineTo(10f, shipHeight * 0.3f)
                    close()
                }
                drawPath(path = flamePath, color = Color(0xFFFF9800))
            }
        }

        StarshipHUD(score = score, lives = lives, timeLeftMs = timeLeftMs)

        if (isGameOver) {
            val isWin = timeLeftMs <= 0
            StarshipGameOverMenu(
                finalScore = score,
                aliensDestroyed = aliensDestroyed,
                isWin = isWin,
                onRestart = {
                    obstacles = emptyList(); lasers = emptyList(); score = 0; lives = 3; aliensDestroyed = 0
                    timeLeftMs = maxGameTimeMs; gameStartTime = 0L; smoothedNormalizedX = 0f; isGameOver = false; dataSaved = false
                },
                onFinish = onFinishGame
            )
        }
    }
}

@Composable
fun StarshipHUD(score: Int, lives: Int, timeLeftMs: Long) {
    val seconds = timeLeftMs / 1000
    val timeFormatted = String.format("00:%02d", seconds)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text("SCORE", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("$score", color = Color(0xFF66FCF1), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TIME", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                text = timeFormatted,
                color = if (seconds <= 10) Color.Red else WhiteColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text("LIVES", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.padding(top = 4.dp)) {
                repeat(3) { i ->
                    Text(
                        text = if (i < lives) "♥" else "♡",
                        color = if (i < lives) Color.Red else Color.DarkGray,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StarshipLaunchScreen(onStart: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        Text("SYSTEMS ONLINE", color = Color.Green, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Text("• SURVIVE for 60 Seconds", color = Color.Yellow, textAlign = TextAlign.Start, fontWeight = FontWeight.Bold)
        Text("• LEAN Left and Right to Steer", color = WhiteColor, textAlign = TextAlign.Start)
        Text("• TILT FORWARD to fire Cannons", color = Color.Cyan, textAlign = TextAlign.Start)
        Spacer(Modifier.height(16.dp))
        Text("Avoid Gray Meteors. Shoot Green Aliens.", color = Color.LightGray, textAlign = TextAlign.Center)
        Spacer(Modifier.height(40.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth(0.8f).height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = RedColor)) {
            Text("LAUNCH FIGHTER", color = WhiteColor, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun StarshipGameOverMenu(
    finalScore: Int,
    aliensDestroyed: Int,
    isWin: Boolean,
    onRestart: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0B0C10).copy(alpha = 0.9f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val title = if (isWin) "MISSION COMPLETE" else "HULL BREACHED"
        val titleColor = if (isWin) Green else Color.Red

        Text(title, color = titleColor, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("Final Score: $finalScore", color = Color(0xFF66FCF1), style = MaterialTheme.typography.headlineMedium)
        Text("Aliens Destroyed: $aliensDestroyed", color = WhiteColor, style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(40.dp))

        Button(
            onClick = onRestart,
            modifier = Modifier.height(55.dp).fillMaxWidth(0.6f),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
        ) {
            Text("DEPLOY NEW SHIP", color = WhiteColor, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = onFinish,
            border = BorderStroke(2.5.dp, Color.White),
            modifier = Modifier.height(55.dp).fillMaxWidth(0.6f)
        ) {
            Text(
                text = "FINISH SESSION",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

enum class EntityType { ALIEN, METEOR }
data class SpaceEntity(val x: Float, val y: Float, val speed: Float, val radius: Float, val type: EntityType)
data class LaserBeam(val x: Float, val y: Float, val isLeft: Boolean)