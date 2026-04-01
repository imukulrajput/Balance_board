package com.ripplehealthcare.frst.ui.screens

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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.ripplehealthcare.frst.domain.model.RatPuzzleResult
import com.ripplehealthcare.frst.ui.components.TopBar
import com.ripplehealthcare.frst.ui.theme.*
import com.ripplehealthcare.frst.ui.viewmodel.BluetoothViewModel
import com.ripplehealthcare.frst.ui.viewmodel.TestViewModel
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

private sealed class MazeGameState {
    object Playing : MazeGameState()
    object GameOver : MazeGameState()
    object LevelComplete : MazeGameState()
}

private data class Wall(val left: Float, val top: Float, val right: Float, val bottom: Float)
private data class Hole(val x: Float, val y: Float, val radius: Float)
private data class TargetZone(val left: Float, val top: Float, val right: Float, val bottom: Float)

private class MazeCell(val col: Int, val row: Int) {
    var topWall = true
    var rightWall = true
    var bottomWall = true
    var leftWall = true
    var visited = false
}

@Composable
fun RatPuzzleGame(
    navController: NavController,
    bluetoothViewModel: BluetoothViewModel,
    testViewModel: TestViewModel
) {
    val sensorData by bluetoothViewModel.sensorData.collectAsState()

    val deltaYaw = (sensorData?.centerYaw ?: bluetoothViewModel.centerYaw) - bluetoothViewModel.centerYaw
    val deltaPitch = (sensorData?.centerPitch ?: bluetoothViewModel.centerPitch) - bluetoothViewModel.centerPitch
    val currentXAngle = -deltaYaw
    val currentYAngle = -deltaPitch

    var gameState by remember { mutableStateOf<MazeGameState>(MazeGameState.Playing) }

    BackHandler(enabled = gameState == MazeGameState.Playing) {
        // Block back button during active play
    }

    Scaffold(
        topBar = {
            TopBar(
                title = "Maze Balance",
                onBackClick = {
                    if (gameState != MazeGameState.Playing) navController.popBackStack()
                }
            )
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFFE5CFA5), MaterialTheme.shapes.medium)
            ) {
                MazeCanvas(
                    currentXAngle = currentXAngle,
                    currentYAngle = currentYAngle,
                    gameState = gameState,
                    onStateChange = { gameState = it },
                    onFinishGame = { navController.popBackStack() },
                    onSaveData = { isWin, timeMs, lives ->
                        val result = RatPuzzleResult(
                            sessionId = UUID.randomUUID().toString(),
                            patientId = testViewModel.patient.value.patientId,
                            isWin = isWin,
                            timeTakenMs = timeMs,
                            livesRemaining = lives
                        )
                        testViewModel.saveRatPuzzleResult(result)
                    }
                )
            }
        }
    }
}

@Composable
private fun MazeCanvas(
    currentXAngle: Float,
    currentYAngle: Float,
    gameState: MazeGameState,
    onStateChange: (MazeGameState) -> Unit,
    onFinishGame: () -> Unit,
    onSaveData: (Boolean, Long, Int) -> Unit
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    var ballX by remember { mutableFloatStateOf(0f) }
    var ballY by remember { mutableFloatStateOf(0f) }
    var velX by remember { mutableFloatStateOf(0f) }
    var velY by remember { mutableFloatStateOf(0f) }

    var safeX by remember { mutableFloatStateOf(0f) }
    var safeY by remember { mutableFloatStateOf(0f) }

    var lives by remember { mutableIntStateOf(3) }
    var isInitialized by remember { mutableStateOf(false) }
    var isRespawning by remember { mutableStateOf(false) }

    var timeTakenMs by remember { mutableLongStateOf(0L) }
    var dataSaved by remember { mutableStateOf(false) }

    var walls by remember { mutableStateOf(listOf<Wall>()) }
    var holes by remember { mutableStateOf(listOf<Hole>()) }
    var target by remember { mutableStateOf(TargetZone(0f, 0f, 0f, 0f)) }
    var logicalHeight by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val ballRadiusPx = with(density) { 8.5.dp.toPx() }
    val holeRadiusPx = with(density) { 14.dp.toPx() }
    val wallThickness = with(density) { 9.dp.toPx() }

    val currentXState by rememberUpdatedState(currentXAngle)
    val currentYState by rememberUpdatedState(currentYAngle)

    // REDUCED SENSITIVITY CONSTANTS
    val sensitivity = 0.05f
    val speedMultiplier = 7f

    LaunchedEffect(gameState, canvasSize) {
        if (gameState != MazeGameState.Playing || canvasSize == Size.Zero) return@LaunchedEffect

        val w = canvasSize.width
        val h = canvasSize.height

        val cols = 8
        val cellSize = w / cols
        val targetAspect = 5.2f
        val targetLogicalHeight = h * targetAspect
        val rows = (targetLogicalHeight / cellSize).toInt().coerceAtLeast(20)

        logicalHeight = rows * cellSize

        // ────────────────────────────────────────────────
        // MAZE GENERATION (DFS)
        // ────────────────────────────────────────────────
        val grid = Array(cols) { c -> Array(rows) { r -> MazeCell(c, r) } }
        val stack = mutableListOf<MazeCell>()

        var current = grid[0][0]
        current.visited = true
        var remaining = cols * rows - 1

        val pattern = Random.nextInt(1, 6)
        var preferredDir = -1

        while (remaining > 0) {
            val candidates = mutableListOf<Pair<MazeCell, Int>>()
            val cx = current.col; val cy = current.row

            if (cy > 0         && !grid[cx][cy-1].visited) candidates += grid[cx][cy-1] to 0
            if (cx < cols-1    && !grid[cx+1][cy].visited) candidates += grid[cx+1][cy] to 1
            if (cy < rows-1    && !grid[cx][cy+1].visited) candidates += grid[cx][cy+1] to 2
            if (cx > 0         && !grid[cx-1][cy].visited) candidates += grid[cx-1][cy] to 3

            if (candidates.isNotEmpty()) {
                val nextPair = when (pattern) {
                    2 -> candidates.find { it.second == preferredDir }
                        ?.takeIf { Random.nextInt(1, 101) <= 82 }
                        ?: candidates.random()

                    5 -> candidates.filter { it.second != preferredDir }
                        .takeIf { it.isNotEmpty() && Random.nextInt(1, 101) <= 78 }
                        ?.random()
                        ?: candidates.random()

                    else -> candidates.random()
                }

                val next = nextPair.first
                preferredDir = nextPair.second
                stack += current

                when (nextPair.second) {
                    0 -> { current.topWall = false; next.bottomWall = false }
                    1 -> { current.rightWall = false; next.leftWall = false }
                    2 -> { current.bottomWall = false; next.topWall = false }
                    3 -> { current.leftWall = false; next.rightWall = false }
                }

                current = next
                current.visited = true
                remaining--
            } else if (stack.isNotEmpty()) {
                current = stack.removeLast()
            }
        }

        // ────────────────────────────────────────────────
        // NEW: MULTIPLE PATHS / BRAID MAZE LOGIC
        // ────────────────────────────────────────────────

        // 1. Remove dead ends. This guarantees loops and multiple routes!
        for (c in 0 until cols) {
            for (r in 0 until rows) {
                val cell = grid[c][r]
                var wallCount = 0
                if (cell.topWall) wallCount++
                if (cell.rightWall) wallCount++
                if (cell.bottomWall) wallCount++
                if (cell.leftWall) wallCount++

                // If it's a dead end (3 walls), break one wall to connect it
                if (wallCount >= 3) {
                    val breakable = mutableListOf<Int>()
                    if (cell.topWall && r > 0) breakable.add(0)
                    if (cell.rightWall && c < cols - 1) breakable.add(1)
                    if (cell.bottomWall && r < rows - 1) breakable.add(2)
                    if (cell.leftWall && c > 0) breakable.add(3)

                    if (breakable.isNotEmpty()) {
                        when (breakable.random()) {
                            0 -> { cell.topWall = false; grid[c][r-1].bottomWall = false }
                            1 -> { cell.rightWall = false; grid[c+1][r].leftWall = false }
                            2 -> { cell.bottomWall = false; grid[c][r+1].topWall = false }
                            3 -> { cell.leftWall = false; grid[c-1][r].rightWall = false }
                        }
                    }
                }
            }
        }

        // 2. Punch a few random extra holes to create major shortcuts across the maze
        val extraLoops = (cols * rows * 0.08).toInt() // Open ~8% of extra walls
        repeat(extraLoops) {
            val c = Random.nextInt(1, cols - 1)
            val r = Random.nextInt(1, rows - 1)
            if (Random.nextBoolean()) {
                grid[c][r].rightWall = false
                grid[c+1][r].leftWall = false
            } else {
                grid[c][r].bottomWall = false
                grid[c][r+1].topWall = false
            }
        }

        // Multiple paths to goal (open last few columns / rows for final target approach)
        val finalRow = rows - 1
        for (c in (cols - 3) until cols) {
            if (Random.nextBoolean()) {
                grid[c][finalRow - 1].bottomWall = false
            }
            if (c < cols - 1 && Random.nextInt(100) < 70) {
                grid[c][finalRow].rightWall = false
            }
        }
        grid[cols - 3][finalRow - 1].bottomWall = false
        grid[cols - 2][finalRow - 1].bottomWall = false

        // Build walls & holes
        val wt = wallThickness / 2f
        val newWalls = mutableListOf<Wall>()

        // Outer boundary
        newWalls += Wall(0f, 0f, w, wt*2)
        newWalls += Wall(0f, logicalHeight - wt*2, w, logicalHeight)
        newWalls += Wall(0f, 0f, wt*2, logicalHeight)
        newWalls += Wall(w - wt*2, 0f, w, logicalHeight)

        val newHoles = mutableListOf<Hole>()

        for (c in 0 until cols) {
            for (r in 0 until rows) {
                val cell = grid[c][r]
                val x = c * cellSize
                val y = r * cellSize

                if (cell.rightWall && c < cols-1) {
                    newWalls += Wall(
                        x + cellSize - wt, y - wt,
                        x + cellSize + wt, y + cellSize + wt
                    )
                }
                if (cell.bottomWall && r < rows-1) {
                    newWalls += Wall(
                        x - wt, y + cellSize - wt,
                        x + cellSize + wt, y + cellSize + wt
                    )
                }

                if ((c == 0 && r == 0) || (c == cols-1 && r == rows-1)) continue

                val wallCount = listOf(cell.topWall, cell.rightWall, cell.bottomWall, cell.leftWall).count { it }

                // Determine trap/hole logic
                if (wallCount >= 3 && Random.nextInt(1, 101) <= 85) {
                    newHoles += Hole(x + cellSize/2, y + cellSize/2, holeRadiusPx)
                } else if (pattern == 5 && wallCount == 2 && Random.nextInt(1, 101) <= 45) {
                    newHoles += Hole(x + cellSize/2, y + cellSize/2, holeRadiusPx * 0.8f)
                } else if (pattern == 4 && wallCount <= 1 && Random.nextInt(1, 101) <= 28) {
                    newHoles += Hole(x + cellSize/2, y + cellSize/2, holeRadiusPx * 0.9f)
                } else if (Random.nextInt(1, 101) <= 12) {
                    newHoles += Hole(x + cellSize/2, y + cellSize/2, holeRadiusPx * 0.85f)
                }
            }
        }

        ballX = cellSize / 2f
        ballY = cellSize / 2f
        safeX = ballX
        safeY = ballY

        target = TargetZone(
            w - cellSize * 1.6f + wt,
            logicalHeight - cellSize * 1.4f + wt,
            w - wt,
            logicalHeight - wt
        )

        walls = newWalls
        holes = newHoles
        isInitialized = true

        var lastFrameTime = -1L

        while (true) {
            withFrameMillis { frameTime ->
                if (lastFrameTime == -1L) lastFrameTime = frameTime
                val deltaMs = frameTime - lastFrameTime
                lastFrameTime = frameTime

                if (!isRespawning) {
                    timeTakenMs += deltaMs

                    val speed = speedMultiplier
                    // REDUCED AND CLAMPED VELOCITY LIMITS
                    velX = (currentXState * sensitivity * speed).coerceIn(-6f, 6f)
                    velY = (currentYState * sensitivity * speed).coerceIn(-6f, 6f)

                    var nextX = ballX + velX
                    var nextY = ballY + velY

                    for (wall in walls) {
                        val closestX = nextX.coerceIn(wall.left, wall.right)
                        val closestY = nextY.coerceIn(wall.top, wall.bottom)
                        val dx = nextX - closestX
                        val dy = nextY - closestY
                        val distSq = dx*dx + dy*dy

                        if (distSq < ballRadiusPx*ballRadiusPx) {
                            val dist = sqrt(distSq)
                            if (dist > 0.001f) {
                                val overlap = ballRadiusPx - dist
                                val nx = dx / dist
                                val ny = dy / dist
                                nextX += nx * overlap
                                nextY += ny * overlap

                                val dot = velX * nx + velY * ny
                                velX -= 2 * dot * nx * 0.32f
                                velY -= 2 * dot * ny * 0.32f
                            }
                        }
                    }

                    ballX = nextX
                    ballY = nextY

                    if (abs(velX) < 0.9f && abs(velY) < 0.9f) {
                        var safe = true
                        for (hole in holes) {
                            val d = sqrt((ballX - hole.x)*(ballX - hole.x) + (ballY - hole.y)*(ballY - hole.y))
                            if (d < hole.radius * 3.2f) {
                                safe = false
                                break
                            }
                        }
                        if (safe) {
                            safeX = ballX
                            safeY = ballY
                        }
                    }

                    var died = false
                    for (hole in holes) {
                        if (sqrt((ballX-hole.x)*(ballX-hole.x) + (ballY-hole.y)*(ballY-hole.y)) < hole.radius) {
                            died = true
                            break
                        }
                    }

                    if (died) {
                        lives--
                        if (lives <= 0) {
                            if (!dataSaved) {
                                dataSaved = true
                                onSaveData(false, timeTakenMs, lives)
                            }
                            onStateChange(MazeGameState.GameOver)
                        } else {
                            isRespawning = true
                        }
                    } else if (ballX > target.left && ballX < target.right &&
                        ballY > target.top  && ballY < target.bottom) {
                        if (!dataSaved) {
                            dataSaved = true
                            onSaveData(true, timeTakenMs, lives)
                        }
                        onStateChange(MazeGameState.LevelComplete)
                    }
                }
            }

            if (isRespawning) {
                delay(1200L)
                ballX = safeX
                ballY = safeY
                velX = 0f
                velY = 0f
                isRespawning = false
                lastFrameTime = -1L
            }
        }
    }

    val cameraY = if (canvasSize != Size.Zero) {
        (ballY - canvasSize.height / 2f).coerceIn(0f, logicalHeight - canvasSize.height)
    } else 0f

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            canvasSize = size

            translate(left = 0f, top = -cameraY) {
                drawRoundRect(
                    color = Green.copy(alpha = 0.65f),
                    topLeft = Offset(target.left, target.top),
                    size = Size(target.right - target.left, target.bottom - target.top),
                    cornerRadius = CornerRadius(12f, 12f)
                )

                for (wall in walls) {
                    drawRoundRect(
                        color = Color(0xFF5D4037),
                        topLeft = Offset(wall.left, wall.top),
                        size = Size(wall.right - wall.left, wall.bottom - wall.top),
                        cornerRadius = CornerRadius(5f, 5f)
                    )
                }

                for (hole in holes) {
                    drawCircle(Color.Black, hole.radius, Offset(hole.x, hole.y))
                    drawCircle(Color(0xFF212121), hole.radius * 0.82f, Offset(hole.x, hole.y))
                }

                if (isInitialized && !isRespawning) {
                    drawCircle(PrimaryColor, ballRadiusPx, Offset(ballX, ballY))
                    drawCircle(WhiteColor.copy(alpha = 0.45f), ballRadiusPx * 0.38f, Offset(ballX - 3f, ballY - 3f))
                }
            }
        }

        MazeOverlay(
            lives = lives,
            timeMs = timeTakenMs,
            gameState = gameState,
            onRestart = {
                lives = 3
                timeTakenMs = 0L
                isInitialized = false
                isRespawning = false
                dataSaved = false
                onStateChange(MazeGameState.Playing)
            },
            onFinish = onFinishGame
        )
    }
}

@Composable
private fun MazeOverlay(
    lives: Int,
    timeMs: Long,
    gameState: MazeGameState,
    onRestart: () -> Unit,
    onFinish: () -> Unit
) {
    val totalSec = timeMs / 1000
    val timeStr = String.format("%02d:%02d", totalSec / 60, totalSec % 60)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Time: $timeStr",
                color = Color.Black,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row {
                Text(
                    text = "Lives: ",
                    color = Color.Black,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                repeat(3) { i ->
                    Text(
                        text = if (i < lives) "♥" else "♡",
                        color = if (i < lives) Color.Red else Color.Gray,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 5.dp)
                    )
                }
            }
        }

        // SHOW END SCREEN AS A FULL-SCREEN DIALOG OVER EVERYTHING
        if (gameState == MazeGameState.GameOver || gameState == MazeGameState.LevelComplete) {
            Dialog(
                onDismissRequest = { /* Don't dismiss on outside click to force a choice */ },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (gameState == MazeGameState.LevelComplete)
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
                        val isWin = gameState == MazeGameState.LevelComplete

                        Text(
                            text = if (isWin) "🎉  PUZZLE SOLVED!  🎉" else "💔  OUT OF LIVES  💔",
                            color = if (isWin) Green else RedColor,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "Time: $timeStr",
                            color = Color.DarkGray,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Lives remaining: $lives",
                            color = Color(0xFF555555),
                            fontSize = 22.sp
                        )

                        Spacer(Modifier.height(40.dp))

                        if (isWin) {
                            Text(
                                text = "Great balance control!\nYou navigated the maze skillfully.",
                                color = Color(0xFF388E3C),
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "Don't give up — try again!\nSmaller tilts might help.",
                                color = Color(0xFFD32F2F),
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(Modifier.height(56.dp))

                        Button(
                            onClick = onRestart,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isWin) Green else PrimaryColor
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .height(64.dp)
                                .fillMaxWidth(0.85f)
                        ) {
                            Text(
                                text = if (isWin) "PLAY AGAIN →" else "TRY AGAIN",
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
                                "FINISH SESSION",
                                color = PrimaryColor,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}