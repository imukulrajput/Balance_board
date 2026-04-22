package com.ripplehealthcare.bproboard.ui.screens

import android.graphics.Paint
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ripplehealthcare.bproboard.domain.model.*
import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel
import com.ripplehealthcare.bproboard.util.ClinicalReportPdfManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// --- Helper to calculate clinical difficulty weight ---
fun calculateClinicalWeight(posture: String?, level: Int?): Float {
    val pWeight = if (posture.equals("STANDING", ignoreCase = true)) 1.2f else 1.0f
    val lWeight = when (level) {
        3 -> 1.5f
        2 -> 1.25f
        else -> 1.0f
    }
    return pWeight * lWeight
}

data class ChartData(val label: String, val value: Float)

// --- Data Class to group all results by a single Session ID ---
data class GroupedSession(
    val sessionId: String,
    val timestamp: Date,
    val staticResults: List<StaticBalanceResult>,
    val patternResults: List<PatternDrawingResult>,
    val shapeResults: List<ShapeTrainingResult>,
    val colorSorterResults: List<ColorSorterResult>,
    val ratPuzzleResults: List<RatPuzzleResult>,
    val starshipResults: List<StarshipResult>,
    val holePuzzleResults: List<HolePuzzleResult>,
    val stepGameResults: List<StepGameResult>
) {
    val totalModulesPlayed = staticResults.size + patternResults.size + shapeResults.size +
            colorSorterResults.size + ratPuzzleResults.size + starshipResults.size +
            holePuzzleResults.size + stepGameResults.size

    val totalFalls = staticResults.sumOf { it.fallCount } + patternResults.sumOf { it.fallCount } + shapeResults.sumOf { it.fallCount }

    val totalTimeMs = staticResults.sumOf { it.totalTimeMs } + patternResults.sumOf { it.timeTakenMs } + shapeResults.sumOf { it.timeTakenMs } +
            ratPuzzleResults.sumOf { it.timeTakenMs } + starshipResults.sumOf { it.timeSurvivedMs } + holePuzzleResults.sumOf { it.timeSurvivedMs }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphViewScreen(
    navController: NavController,
    testViewModel: TestViewModel
) {
    val context = LocalContext.current
    val patient by testViewModel.patient.collectAsState()
    val staticBalanceResults by testViewModel.staticBalanceResults.collectAsState()
    val patternDrawingResults by testViewModel.patternDrawingResults.collectAsState()
    val shapeTrainingResults by testViewModel.shapeTrainingResults.collectAsState()
    val colorSorterResults by testViewModel.colorSorterResults.collectAsState()
    val ratPuzzleResults by testViewModel.ratPuzzleResults.collectAsState()
    val starshipResults by testViewModel.starshipResults.collectAsState()
    val holePuzzleResults by testViewModel.holePuzzleResults.collectAsState()
    val stepGameResults by testViewModel.stepGameResults.collectAsState()

    var selectedMode by remember { mutableStateOf("Session") }
    var selectedGame by remember { mutableStateOf("Overview Dashboard") }
    var selectedMetric by remember { mutableStateOf("Efficiency") }

    var selectedGroupedSession by remember { mutableStateOf<GroupedSession?>(null) }
    var selectedOverviewSession by remember { mutableStateOf<GroupedSession?>(null) }

    var selectedStaticSession by remember { mutableStateOf<StaticBalanceResult?>(null) }
    var selectedPatternSession by remember { mutableStateOf<PatternDrawingResult?>(null) }
    var selectedShapeSession by remember { mutableStateOf<ShapeTrainingResult?>(null) }
    var selectedColorSorterSession by remember { mutableStateOf<ColorSorterResult?>(null) }
    var selectedRatSession by remember { mutableStateOf<RatPuzzleResult?>(null) }
    var selectedStarshipSession by remember { mutableStateOf<StarshipResult?>(null) }
    var selectedHoleSession by remember { mutableStateOf<HolePuzzleResult?>(null) }
    var selectedStepSession by remember { mutableStateOf<StepGameResult?>(null) }

    val clearDetailedSessions = {
        selectedStaticSession = null
        selectedPatternSession = null
        selectedShapeSession = null
        selectedColorSorterSession = null
        selectedRatSession = null
        selectedStarshipSession = null
        selectedHoleSession = null
        selectedStepSession = null
    }

    val gameList = listOf("Overview Dashboard", "Color Sorter", "Rat Puzzle", "Starship Defender", "Hole Navigator", "Step Game")
    val trainingList = listOf("Overview Dashboard", "Static Balance", "Pattern Drawing", "Shape Training")
    val currentList = if (selectedMode == "Game") gameList else trainingList

    val primaryColor = Color(0xFF4A44D4)
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFE0F7FA), Color(0xFFB2EBF2))
    )
    val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    val fullDateFormatter = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())

    val availableMetrics = when (selectedGame) {
        "Static Balance" -> listOf("Efficiency", "Falls", "Balance Time", "Avg Fall Error (°)")
        "Pattern Drawing" -> listOf("Accuracy", "Falls", "Time", "Avg Error (°)")
        "Shape Training" -> listOf("Score", "Falls", "Time", "Avg Error (°)")
        "Color Sorter" -> listOf("Score", "Accuracy")
        "Starship Defender" -> listOf("Score", "Time Survived")
        "Hole Navigator" -> listOf("Score", "Obstacles")
        "Step Game" -> listOf("Score", "Accuracy")
        else -> listOf("Time", "Score")
    }

    // Aggregating all data into GroupedSessions
    val groupedSessions = remember(
        staticBalanceResults, patternDrawingResults, shapeTrainingResults,
        colorSorterResults, ratPuzzleResults, starshipResults, holePuzzleResults, stepGameResults
    ) {
        val allIds = (staticBalanceResults.map { it.sessionId } +
                patternDrawingResults.map { it.sessionId } +
                shapeTrainingResults.map { it.sessionId } +
                colorSorterResults.map { it.sessionId } +
                ratPuzzleResults.map { it.sessionId } +
                starshipResults.map { it.sessionId } +
                holePuzzleResults.map { it.sessionId } +
                stepGameResults.map { it.sessionId }).distinct()

        allIds.mapNotNull { sid ->
            val statics = staticBalanceResults.filter { it.sessionId == sid }
            val patterns = patternDrawingResults.filter { it.sessionId == sid }
            val shapes = shapeTrainingResults.filter { it.sessionId == sid }
            val colors = colorSorterResults.filter { it.sessionId == sid }
            val rats = ratPuzzleResults.filter { it.sessionId == sid }
            val stars = starshipResults.filter { it.sessionId == sid }
            val holes = holePuzzleResults.filter { it.sessionId == sid }
            val steps = stepGameResults.filter { it.sessionId == sid }

            val minTime = listOfNotNull(
                statics.minOfOrNull { it.timestamp }, patterns.minOfOrNull { it.timestamp },
                shapes.minOfOrNull { it.timestamp }, colors.minOfOrNull { it.timestamp },
                rats.minOfOrNull { it.timestamp }, stars.minOfOrNull { it.timestamp },
                holes.minOfOrNull { it.timestamp }, steps.minOfOrNull { it.timestamp }
            ).minOrNull()

            if (minTime != null) {
                GroupedSession(sid, minTime, statics, patterns, shapes, colors, rats, stars, holes, steps)
            } else null
        }.sortedByDescending { it.timestamp }
    }

    val density = LocalDensity.current
    val textPaintRight = remember(density) {
        Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = with(density) { 10.sp.toPx() }
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }
    }
    val textPaintLeft = remember(density) {
        Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = with(density) { 10.sp.toPx() }
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
    }

    LaunchedEffect(selectedMode) {
        if (selectedMode != "Session" && !currentList.contains(selectedGame)) {
            selectedGame = currentList.firstOrNull() ?: ""
        }
    }

    LaunchedEffect(patient.patientId) {
        if (patient.patientId.isNotEmpty() && patient.centerId.isNotEmpty()) {
            testViewModel.fetchStaticBalanceResults(patient.centerId, patient.patientId)
            testViewModel.fetchPatternDrawingResults(patient.centerId, patient.patientId)
            testViewModel.fetchShapeTrainingResults(patient.centerId, patient.patientId)
            testViewModel.fetchColorSorterResults(patient.centerId, patient.patientId)
            testViewModel.fetchRatPuzzleResults(patient.centerId, patient.patientId)
            testViewModel.fetchStarshipResults(patient.centerId, patient.patientId)
            testViewModel.fetchHolePuzzleResults(patient.centerId, patient.patientId)
            testViewModel.fetchStepGameResults(patient.centerId, patient.patientId)
        }
    }

    LaunchedEffect(selectedGame, selectedMode) {
        selectedStaticSession = null
        selectedPatternSession = null
        selectedShapeSession = null
        selectedColorSorterSession = null
        selectedRatSession = null
        selectedStarshipSession = null
        selectedHoleSession = null
        selectedStepSession = null
        selectedGroupedSession = null
        selectedOverviewSession = null

        if (selectedGame != "Overview Dashboard" && !availableMetrics.contains(selectedMetric)) {
            selectedMetric = availableMetrics.first()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            GraphTopBar(
                onBackClick = { navController.popBackStack() },
                onSaveClick = { },
                primaryColor = primaryColor
            )

            // --- CENTERED TOP CONTROLS ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mode Toggle (Session / Training / Game)
                Box(modifier = Modifier.fillMaxWidth(0.9f)) {
                    SegmentedToggle(
                        options = listOf("Session", "Training", "Game"),
                        selectedOption = selectedMode,
                        onOptionSelected = {
                            if (selectedMode != it) {
                                selectedMode = it
                                selectedGame = if (it == "Game") gameList.first() else trainingList.first()
                                clearDetailedSessions()
                                selectedGroupedSession = null
                                selectedOverviewSession = null
                            }
                        },
                        primaryColor = primaryColor
                    )
                }
            }

            // --- FULL WIDTH GRAPH CARD ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {

                        // ==========================================
                        // SESSION WISE LOGIC BRANCH
                        // ==========================================
                        if (selectedMode == "Session") {
                            if (groupedSessions.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No sessions recorded yet.", color = Color.Gray)
                                }
                            } else if (selectedGroupedSession == null) {
                                // --- UNIFIED PATIENT JOURNEY DASHBOARD ---
                                PatientJourneyDashboard(
                                    groupedSessions = groupedSessions,
                                    primaryColor = primaryColor,
                                    fullDateFormatter = fullDateFormatter,
                                    onSessionClick = { selectedGroupedSession = it }
                                )
                            } else {
                                // --- SESSION DETAIL REPORT ---
                                val session = selectedGroupedSession!!

                                val sortedAsc = groupedSessions.sortedBy { it.timestamp }
                                val currentIndex = sortedAsc.indexOf(session)
                                val prevSession = if (currentIndex > 0) sortedAsc[currentIndex - 1] else null

                                val latestStatic = session.staticResults.maxByOrNull { it.timestamp }
                                val latestPattern = session.patternResults.maxByOrNull { it.timestamp }
                                val latestShape = session.shapeResults.maxByOrNull { it.timestamp }

                                // 1. Calculate Raw Percentages
                                val rawStatic = (latestStatic?.efficiencyPercentage?.toFloat() ?: 0f) / 100f
                                val rawPattern = if (latestPattern != null && latestPattern.totalTargets > 0) (latestPattern.targetsHit.toFloat() / latestPattern.totalTargets) else 0f
                                val rawShape = (latestShape?.score?.toFloat() ?: 0f) / 10f

                                val prevRawStatic = (prevSession?.staticResults?.maxByOrNull { it.timestamp }?.efficiencyPercentage?.toFloat() ?: 0f) / 100f
                                val prevRawPattern = prevSession?.patternResults?.maxByOrNull { it.timestamp }?.let { if (it.totalTargets > 0) it.targetsHit.toFloat() / it.totalTargets else 0f } ?: 0f
                                val prevRawShape = (prevSession?.shapeResults?.maxByOrNull { it.timestamp }?.score?.toFloat() ?: 0f) / 10f

                                // 2. Apply Weighted Multiplier and Cap at 100%
                                val staticScore = (rawStatic * calculateClinicalWeight(latestStatic?.gameMode, latestStatic?.level)).coerceAtMost(1f)
                                val patternScore = (rawPattern * calculateClinicalWeight(latestPattern?.gameMode, latestPattern?.level)).coerceAtMost(1f)
                                val shapeScore = (rawShape * calculateClinicalWeight(latestShape?.gameMode, latestShape?.level)).coerceAtMost(1f)

                                val prevStaticScore = (prevRawStatic * calculateClinicalWeight(prevSession?.staticResults?.maxByOrNull { it.timestamp }?.gameMode, prevSession?.staticResults?.maxByOrNull { it.timestamp }?.level)).coerceAtMost(1f)
                                val prevPatternScore = (prevRawPattern * calculateClinicalWeight(prevSession?.patternResults?.maxByOrNull { it.timestamp }?.gameMode, prevSession?.patternResults?.maxByOrNull { it.timestamp }?.level)).coerceAtMost(1f)
                                val prevShapeScore = (prevRawShape * calculateClinicalWeight(prevSession?.shapeResults?.maxByOrNull { it.timestamp }?.gameMode, prevSession?.shapeResults?.maxByOrNull { it.timestamp }?.level)).coerceAtMost(1f)

                                // 3. Dynamic Averaging
                                val currentModulesPlayed = listOfNotNull(latestStatic, latestPattern, latestShape).size
                                val globalScore = if (currentModulesPlayed > 0) {
                                    ((staticScore + patternScore + shapeScore) / currentModulesPlayed.toFloat()) * 100
                                } else 0f

                                val prevModulesPlayed = listOfNotNull(
                                    prevSession?.staticResults?.maxByOrNull { it.timestamp },
                                    prevSession?.patternResults?.maxByOrNull { it.timestamp },
                                    prevSession?.shapeResults?.maxByOrNull { it.timestamp }
                                ).size
                                val prevGlobalScore = if (prevModulesPlayed > 0) {
                                    ((prevStaticScore + prevPatternScore + prevShapeScore) / prevModulesPlayed.toFloat()) * 100
                                } else 0f

                                val displayPosture = latestStatic?.gameMode ?: latestPattern?.gameMode ?: latestShape?.gameMode
                                val displayLevel = latestStatic?.level ?: latestPattern?.level ?: latestShape?.level

                                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                    Row(modifier = Modifier.clickable { selectedGroupedSession = null }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Back to Journey", color = primaryColor, fontWeight = FontWeight.Bold)
                                    }

                                    SessionSummaryBanner(dateString = fullDateFormatter.format(session.timestamp), posture = displayPosture, level = displayLevel)

                                    if (session.staticResults.isNotEmpty() || session.patternResults.isNotEmpty() || session.shapeResults.isNotEmpty()) {
                                        Text("Session Clinical Report", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))

                                        ClinicalComparisonCard(
                                            title = "BPro Composite Stability Score",
                                            currentValue = globalScore,
                                            previousValue = if (prevSession != null) prevGlobalScore else null,
                                            unit = "/ 100",
                                            isLowerBetter = false,
                                            primaryColor = primaryColor
                                        )
                                        InfoSection("Calculated by taking the weighted average of your performance across all training modules played in this session. A higher score indicates better overall stability.")

                                        ClinicalComparisonCard(
                                            title = "Total Session Falls",
                                            currentValue = session.totalFalls.toFloat(),
                                            previousValue = prevSession?.totalFalls?.toFloat(),
                                            unit = "Falls",
                                            isLowerBetter = true,
                                            primaryColor = Color(0xFFE53935)
                                        )

                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Functional Domains Profile", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 16.sp)
                                                Text("Visual representation of your strengths and areas for improvement across different training modalities.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(16.dp), contentAlignment = Alignment.Center) {
                                                    ClinicalRadarChart(
                                                        currentScores = listOf(staticScore, patternScore, shapeScore),
                                                        prevScores = listOf(prevStaticScore, prevPatternScore, prevShapeScore),
                                                        labels = listOf("Static Endurance", "Kinematic Control", "Reactive Agility"),
                                                        primaryColor = primaryColor
                                                    )
                                                }
                                            }
                                        }

                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Text("Combined Biomechanical Sway Profile", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 16.sp)
                                                Text("Visualizes your center of mass movement. A tighter cluster indicates better balance and real-world postural control.", fontSize = 12.sp, color = Color.Gray)
                                                Spacer(modifier = Modifier.height(16.dp))

                                                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                                    CombinedHeatmap(latestStatic, latestPattern, latestShape)
                                                }

                                                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                                    HeatmapLegendItem("Static", Color(0xFF188B97))
                                                    HeatmapLegendItem("Pattern", Color(0xFF4CAF50))
                                                    HeatmapLegendItem("Shape", Color(0xFFE53935))
                                                }
                                            }
                                        }

                                        // Render Training Scores for Click-through
                                        Text("Clinical Modules", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))

                                        session.staticResults.forEach { res ->
                                            GameMiniCard(title = "Static Balance", scoreText = "${res.efficiencyPercentage}%", subText = "Falls: ${res.fallCount}", onClick = {
                                                clearDetailedSessions()
                                                selectedMode = "Training"
                                                selectedGame = "Static Balance"
                                                selectedStaticSession = res
                                            })
                                        }
                                        session.patternResults.forEach { res ->
                                            GameMiniCard(title = "Pattern Drawing", scoreText = "${res.targetsHit}/${res.totalTargets}", subText = "Falls: ${res.fallCount}", onClick = {
                                                clearDetailedSessions()
                                                selectedMode = "Training"
                                                selectedGame = "Pattern Drawing"
                                                selectedPatternSession = res
                                            })
                                        }
                                        session.shapeResults.forEach { res ->
                                            GameMiniCard(title = "Shape Training", scoreText = "${res.score} pts", subText = "Falls: ${res.fallCount}", onClick = {
                                                clearDetailedSessions()
                                                selectedMode = "Training"
                                                selectedGame = "Shape Training"
                                                selectedShapeSession = res
                                            })
                                        }
                                    }

                                    // Render Gamified Scores for Click-through
                                    if (session.colorSorterResults.isNotEmpty() || session.ratPuzzleResults.isNotEmpty() || session.starshipResults.isNotEmpty() || session.holePuzzleResults.isNotEmpty() || session.stepGameResults.isNotEmpty()) {
                                        Text("Gamification Results", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))

                                        session.colorSorterResults.forEach { res ->
                                            GameMiniCard(title = "Color Sorter", scoreText = "${res.score} pts", subText = "Missed: ${res.missedCount}", onClick = {
                                                clearDetailedSessions()
                                                selectedMode = "Game"
                                                selectedGame = "Color Sorter"
                                                selectedColorSorterSession = res
                                            })
                                        }
                                        session.ratPuzzleResults.forEach { res ->
                                            GameMiniCard(title = "Maze Balance", scoreText = if(res.isWin) "Solved" else "Failed", subText = "Time: ${res.timeTakenMs/1000}s", isSuccess = res.isWin, onClick = {
                                                clearDetailedSessions()
                                                selectedMode = "Game"
                                                selectedGame = "Rat Puzzle"
                                                selectedRatSession = res
                                            })
                                        }
                                        session.starshipResults.forEach { res ->
                                            GameMiniCard(title = "Starship Defender", scoreText = "${res.score} pts", subText = "Survived: ${res.timeSurvivedMs/1000}s", onClick = {
                                                clearDetailedSessions()
                                                selectedMode = "Game"
                                                selectedGame = "Starship Defender"
                                                selectedStarshipSession = res
                                            })
                                        }
                                        session.holePuzzleResults.forEach { res ->
                                            GameMiniCard(title = "Hole Navigator", scoreText = "${res.score} pts", subText = "Dodged: ${res.holesDodged}", onClick = {
                                                clearDetailedSessions()
                                                selectedMode = "Game"
                                                selectedGame = "Hole Navigator"
                                                selectedHoleSession = res
                                            })
                                        }
                                        session.stepGameResults.forEach { res ->
                                            GameMiniCard(title = "Cognitive Stepping", scoreText = "${res.score} pts", subText = "Hits: ${res.correctHits} / Misses: ${res.incorrectHits}", onClick = {
                                                clearDetailedSessions()
                                                selectedMode = "Game"
                                                selectedGame = "Step Game"
                                                selectedStepSession = res
                                            })
                                        }
                                    }

                                    ReportActionButtons(
                                        primaryColor = primaryColor,
                                        onShareClick = {
                                            ClinicalReportPdfManager.generateAndSharePdf(context, patient, session, true)
                                        },
                                        onDownloadClick = {
                                            ClinicalReportPdfManager.generateAndSharePdf(context, patient, session, false)
                                        }
                                    )
                                }
                            }
                        } else {
                            // ==========================================
                            // EXISTING MODULE-SPECIFIC LOGIC
                            // ==========================================
                            when (selectedGame) {
                                // ==========================================
                                // OVERVIEW DASHBOARD LOGIC (TRAINING OR GAME)
                                // ==========================================
                                "Overview Dashboard" -> {
                                    if (selectedMode == "Training") {
                                        val trainingSessions = groupedSessions.filter {
                                            it.staticResults.isNotEmpty() || it.patternResults.isNotEmpty() || it.shapeResults.isNotEmpty()
                                        }

                                        if (trainingSessions.isEmpty()) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text("No training data available yet to generate a report.", color = Color.Gray)
                                            }
                                        } else if (selectedOverviewSession == null) {
                                            // Enhanced List View with Summary
                                            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                                Text("Training Overview", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))

                                                // Calculate Summaries
                                                val totalTrainingSessions = trainingSessions.size
                                                val totalTrainingTimeSecs = trainingSessions.sumOf { s ->
                                                    s.staticResults.sumOf { it.totalTimeMs } +
                                                            s.patternResults.sumOf { it.timeTakenMs } +
                                                            s.shapeResults.sumOf { it.timeTakenMs }
                                                } / 1000f

                                                val avgScores = trainingSessions.mapNotNull { session ->
                                                    val latestStatic = session.staticResults.maxByOrNull { it.timestamp }
                                                    val latestPattern = session.patternResults.maxByOrNull { it.timestamp }
                                                    val latestShape = session.shapeResults.maxByOrNull { it.timestamp }

                                                    val staticScore = ((latestStatic?.efficiencyPercentage?.toFloat() ?: 0f) / 100f * calculateClinicalWeight(latestStatic?.gameMode, latestStatic?.level)).coerceAtMost(1f)
                                                    val patternScore = (if (latestPattern != null && latestPattern.totalTargets > 0) (latestPattern.targetsHit.toFloat() / latestPattern.totalTargets) else 0f * calculateClinicalWeight(latestPattern?.gameMode, latestPattern?.level)).coerceAtMost(1f)
                                                    val shapeScore = ((latestShape?.score?.toFloat() ?: 0f) / 10f * calculateClinicalWeight(latestShape?.gameMode, latestShape?.level)).coerceAtMost(1f)

                                                    val mods = listOfNotNull(latestStatic, latestPattern, latestShape).size
                                                    if (mods > 0) ((staticScore + patternScore + shapeScore) / mods.toFloat()) * 100 else null
                                                }
                                                val overallAvgScore = if (avgScores.isNotEmpty()) avgScores.average() else 0.0

                                                // Trend analysis
                                                val trendText = if(avgScores.size > 1) {
                                                    val latest = avgScores.first()
                                                    val previous = avgScores[1]
                                                    if(latest > previous) "📈 Improving" else if(latest < previous) "📉 Declining" else "➡️ Stable"
                                                } else "➡️ Baseline"

                                                val chronologicalScores = avgScores.take(10).reversed().map { it.toFloat() }

                                                Card(
                                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
                                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(16.dp)) {
                                                        Text("All-Time Training Metrics", fontWeight = FontWeight.Bold, color = Color(0xFF6A1B9A), fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                            StatBox("Sessions", "$totalTrainingSessions", Color.DarkGray)
                                                            StatBox("Avg Score", "${overallAvgScore.toInt()}/100", primaryColor)
                                                            StatBox("Total Time", "${totalTrainingTimeSecs.toInt()}s", Color.DarkGray)

                                                        }
                                                        Spacer(modifier = Modifier.height(12.dp))
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text("Performance Trend: ", color = Color.Gray, fontSize = 12.sp)
                                                            Text(trendText, fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 14.sp)
                                                        }
                                                        Text("Score represents the BPro Composite Stability average.", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                                                    }
                                                }

                                                // --- NEW: TREND CHART ---
                                                PerformanceTrendChart(scores = chronologicalScores, targetScore = 80f, primaryColor = primaryColor)

                                                // --- NEW: MODULE BREAKDOWN ---
                                                Card(
                                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(16.dp)) {
                                                        Text("Module Breakdown", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))

                                                        // NEW: Column Headers
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text("MODULE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                                                            Text("PLAYS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                                                            Text("TIME", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                                                            Text("SCORE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                                                            Spacer(modifier = Modifier.width(20.dp)) // Aligns with the arrow icon
                                                        }
                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        BreakdownRow(
                                                            title = "Static Balance",
                                                            plays = staticBalanceResults.size,
                                                            timeStr = "${staticBalanceResults.sumOf { it.totalTimeMs } / 1000L}s",
                                                            scoreStr = "${if(staticBalanceResults.isNotEmpty()) staticBalanceResults.map{it.efficiencyPercentage}.average().toInt() else 0}% Avg",
                                                            color = primaryColor,
                                                            onClick = { selectedGame = "Static Balance" }
                                                        )

                                                        BreakdownRow(
                                                            title = "Pattern Drawing",
                                                            plays = patternDrawingResults.size,
                                                            timeStr = "${patternDrawingResults.sumOf { it.timeTakenMs } / 1000L}s",
                                                            scoreStr = "${if(patternDrawingResults.isNotEmpty()) patternDrawingResults.map{ if(it.totalTargets>0) (it.targetsHit.toFloat()/it.totalTargets)*100 else 0f }.average().toInt() else 0}% Avg",
                                                            color = primaryColor,
                                                            onClick = { selectedGame = "Pattern Drawing" }
                                                        )

                                                        BreakdownRow(
                                                            title = "Shape Training",
                                                            plays = shapeTrainingResults.size,
                                                            timeStr = "${shapeTrainingResults.sumOf { it.timeTakenMs } / 1000L}s",
                                                            scoreStr = "${if(shapeTrainingResults.isNotEmpty()) shapeTrainingResults.map{it.score}.average().toInt() else 0} pts",
                                                            color = primaryColor,
                                                            onClick = { selectedGame = "Shape Training" }
                                                        )
                                                    }
                                                }

                                                Text("Select a Training Session", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))

                                                trainingSessions.forEach { session ->
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { selectedOverviewSession = session },
                                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column {
                                                                Text(text = fullDateFormatter.format(session.timestamp), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)

                                                                val moduleCount = (if(session.staticResults.isNotEmpty()) 1 else 0) +
                                                                        (if(session.patternResults.isNotEmpty()) 1 else 0) +
                                                                        (if(session.shapeResults.isNotEmpty()) 1 else 0)
                                                                Text(text = "$moduleCount Training Modules Played", fontSize = 14.sp, color = Color.DarkGray)
                                                            }
                                                            Icon(Icons.Default.ArrowForward, contentDescription = "View Report", tint = primaryColor)
                                                        }
                                                    }
                                                }

                                                ReportActionButtons(
                                                    primaryColor = primaryColor,
                                                    onShareClick = {
                                                        trainingSessions.firstOrNull()?.let { session ->
                                                            ClinicalReportPdfManager.generateAndSharePdf(context, patient, session, true)
                                                        } ?: Toast.makeText(context, "No session data available", Toast.LENGTH_SHORT).show()
                                                    },
                                                    onDownloadClick = {
                                                        trainingSessions.firstOrNull()?.let { session ->
                                                            ClinicalReportPdfManager.generateAndSharePdf(context, patient, session, false)
                                                        } ?: Toast.makeText(context, "No session data available", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            }
                                        } else {
                                            val session = selectedOverviewSession!!

                                            val sortedAsc = trainingSessions.sortedBy { it.timestamp }
                                            val currentIndex = sortedAsc.indexOf(session)
                                            val prevSession = if (currentIndex > 0) sortedAsc[currentIndex - 1] else null

                                            val latestStatic = session.staticResults.maxByOrNull { it.timestamp }
                                            val latestPattern = session.patternResults.maxByOrNull { it.timestamp }
                                            val latestShape = session.shapeResults.maxByOrNull { it.timestamp }

                                            val currentFalls = (latestStatic?.fallCount ?: 0) + (latestPattern?.fallCount ?: 0) + (latestShape?.fallCount ?: 0)
                                            val prevFalls = (prevSession?.staticResults?.maxByOrNull { it.timestamp }?.fallCount ?: 0) +
                                                    (prevSession?.patternResults?.maxByOrNull { it.timestamp }?.fallCount ?: 0) +
                                                    (prevSession?.shapeResults?.maxByOrNull { it.timestamp }?.fallCount ?: 0)

                                            // 1. Calculate Raw Percentages
                                            val rawStatic = (latestStatic?.efficiencyPercentage?.toFloat() ?: 0f) / 100f
                                            val rawPattern = if (latestPattern != null && latestPattern.totalTargets > 0) (latestPattern.targetsHit.toFloat() / latestPattern.totalTargets) else 0f
                                            val rawShape = (latestShape?.score?.toFloat() ?: 0f) / 10f

                                            val prevRawStatic = (prevSession?.staticResults?.maxByOrNull { it.timestamp }?.efficiencyPercentage?.toFloat() ?: 0f) / 100f
                                            val prevRawPattern = prevSession?.patternResults?.maxByOrNull { it.timestamp }?.let { if (it.totalTargets > 0) it.targetsHit.toFloat() / it.totalTargets else 0f } ?: 0f
                                            val prevRawShape = (prevSession?.shapeResults?.maxByOrNull { it.timestamp }?.score?.toFloat() ?: 0f) / 10f

                                            // 2. Apply Weighted Multiplier and Cap at 100%
                                            val staticScore = (rawStatic * calculateClinicalWeight(latestStatic?.gameMode, latestStatic?.level)).coerceAtMost(1f)
                                            val patternScore = (rawPattern * calculateClinicalWeight(latestPattern?.gameMode, latestPattern?.level)).coerceAtMost(1f)
                                            val shapeScore = (rawShape * calculateClinicalWeight(latestShape?.gameMode, latestShape?.level)).coerceAtMost(1f)

                                            val prevStaticScore = (prevRawStatic * calculateClinicalWeight(prevSession?.staticResults?.maxByOrNull { it.timestamp }?.gameMode, prevSession?.staticResults?.maxByOrNull { it.timestamp }?.level)).coerceAtMost(1f)
                                            val prevPatternScore = (prevRawPattern * calculateClinicalWeight(prevSession?.patternResults?.maxByOrNull { it.timestamp }?.gameMode, prevSession?.patternResults?.maxByOrNull { it.timestamp }?.level)).coerceAtMost(1f)
                                            val prevShapeScore = (prevRawShape * calculateClinicalWeight(prevSession?.shapeResults?.maxByOrNull { it.timestamp }?.gameMode, prevSession?.shapeResults?.maxByOrNull { it.timestamp }?.level)).coerceAtMost(1f)

                                            // 3. Dynamic Averaging
                                            val currentModulesPlayed = listOfNotNull(latestStatic, latestPattern, latestShape).size
                                            val globalScore = if (currentModulesPlayed > 0) {
                                                ((staticScore + patternScore + shapeScore) / currentModulesPlayed.toFloat()) * 100
                                            } else 0f

                                            val prevModulesPlayed = listOfNotNull(
                                                prevSession?.staticResults?.maxByOrNull { it.timestamp },
                                                prevSession?.patternResults?.maxByOrNull { it.timestamp },
                                                prevSession?.shapeResults?.maxByOrNull { it.timestamp }
                                            ).size
                                            val prevGlobalScore = if (prevModulesPlayed > 0) {
                                                ((prevStaticScore + prevPatternScore + prevShapeScore) / prevModulesPlayed.toFloat()) * 100
                                            } else 0f

                                            val displayPosture = latestStatic?.gameMode ?: latestPattern?.gameMode ?: latestShape?.gameMode
                                            val displayLevel = latestStatic?.level ?: latestPattern?.level ?: latestShape?.level

                                            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                                Row(modifier = Modifier.clickable { selectedOverviewSession = null }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Back to Sessions", color = primaryColor, fontWeight = FontWeight.Bold)
                                                }

                                                SessionSummaryBanner(dateString = fullDateFormatter.format(session.timestamp), posture = displayPosture, level = displayLevel)

                                                Text("Global Stability Report", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))

                                                ClinicalComparisonCard(
                                                    title = "BPro Composite Stability Score",
                                                    currentValue = globalScore,
                                                    previousValue = if (prevSession != null) prevGlobalScore else null,
                                                    unit = "/ 100",
                                                    isLowerBetter = false,
                                                    primaryColor = primaryColor
                                                )
                                                InfoSection("Calculated by taking the weighted average of your performance across all training modules played in this session. A higher score indicates better overall stability.")

                                                ClinicalComparisonCard(
                                                    title = "Combined Fall Risk (Total Incidents)",
                                                    currentValue = currentFalls.toFloat(),
                                                    previousValue = if (prevSession != null) prevFalls.toFloat() else null,
                                                    unit = "Falls",
                                                    isLowerBetter = true,
                                                    primaryColor = Color(0xFFE53935)
                                                )

                                                Card(
                                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("Functional Domains Profile", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 16.sp)
                                                        Text("Visual representation of your strengths and areas for improvement across different training modalities.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                                                        Spacer(modifier = Modifier.height(16.dp))
                                                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(16.dp), contentAlignment = Alignment.Center) {
                                                            ClinicalRadarChart(
                                                                currentScores = listOf(staticScore, patternScore, shapeScore),
                                                                prevScores = listOf(prevStaticScore, prevPatternScore, prevShapeScore),
                                                                labels = listOf("Static Endurance", "Kinematic Control", "Reactive Agility"),
                                                                primaryColor = primaryColor
                                                            )
                                                        }
                                                    }
                                                }

                                                Card(
                                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(16.dp)) {
                                                        Text("Combined Biomechanical Sway Profile", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 16.sp)
                                                        Text("Visualizes your center of mass movement. A tighter cluster indicates better balance and real-world postural control.", fontSize = 12.sp, color = Color.Gray)
                                                        Spacer(modifier = Modifier.height(16.dp))

                                                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                                            CombinedHeatmap(latestStatic, latestPattern, latestShape)
                                                        }

                                                        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                                            HeatmapLegendItem("Static", Color(0xFF188B97))
                                                            HeatmapLegendItem("Pattern", Color(0xFF4CAF50))
                                                            HeatmapLegendItem("Shape", Color(0xFFE53935))
                                                        }
                                                    }
                                                }

                                                ReportActionButtons(
                                                    primaryColor = primaryColor,
                                                    onShareClick = {
                                                        ClinicalReportPdfManager.generateAndSharePdf(context, patient, session, true)
                                                    },
                                                    onDownloadClick = {
                                                        ClinicalReportPdfManager.generateAndSharePdf(context, patient, session, false)
                                                    }
                                                )
                                            }
                                        }
                                    } else if (selectedMode == "Game") {
                                        val gameSessions = groupedSessions.filter {
                                            it.colorSorterResults.isNotEmpty() || it.ratPuzzleResults.isNotEmpty() ||
                                                    it.starshipResults.isNotEmpty() || it.holePuzzleResults.isNotEmpty() ||
                                                    it.stepGameResults.isNotEmpty()
                                        }

                                        if (gameSessions.isEmpty()) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text("No gamification data available yet to generate a report.", color = Color.Gray)
                                            }
                                        } else if (selectedOverviewSession == null) {
                                            // Enhanced List View with Summary for Games
                                            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                                Text("Game Overview", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))

                                                val totalGameSessions = gameSessions.size
                                                val totalGameTimeSecs = gameSessions.sumOf { s ->
                                                    s.ratPuzzleResults.sumOf { it.timeTakenMs } +
                                                            s.starshipResults.sumOf { it.timeSurvivedMs } +
                                                            s.holePuzzleResults.sumOf { it.timeSurvivedMs }
                                                } / 1000f

                                                val avgScores = gameSessions.mapNotNull { session ->
                                                    val latestColor = session.colorSorterResults.maxByOrNull { it.timestamp }
                                                    val latestRat = session.ratPuzzleResults.maxByOrNull { it.timestamp }
                                                    val latestStarship = session.starshipResults.maxByOrNull { it.timestamp }
                                                    val latestHole = session.holePuzzleResults.maxByOrNull { it.timestamp }
                                                    val latestStep = session.stepGameResults.maxByOrNull { it.timestamp }

                                                    val colorScore = latestColor?.let { ((it.redCollected + it.greenCollected) / 30f).coerceIn(0f, 1f) } ?: 0f
                                                    val ratScore = latestRat?.let { if (it.isWin) 1f else 0f } ?: 0f
                                                    val starshipScore = latestStarship?.let { (it.timeSurvivedMs / 60000f).coerceIn(0f, 1f) } ?: 0f
                                                    val holeScore = latestHole?.let { (it.timeSurvivedMs / 60000f).coerceIn(0f, 1f) } ?: 0f
                                                    val stepScore = latestStep?.let { val tot = it.correctHits + it.incorrectHits; if (tot > 0) it.correctHits.toFloat() / tot else 0f } ?: 0f

                                                    val gamesPlayed = listOfNotNull(latestColor, latestRat, latestStarship, latestHole, latestStep).size
                                                    if (gamesPlayed > 0) ((colorScore + ratScore + starshipScore + holeScore + stepScore) / gamesPlayed.toFloat()) * 100 else null
                                                }
                                                val overallAvgScore = if (avgScores.isNotEmpty()) avgScores.average() else 0.0

                                                // Trend analysis
                                                val trendText = if(avgScores.size > 1) {
                                                    val latest = avgScores.first()
                                                    val previous = avgScores[1]
                                                    if(latest > previous) "📈 Improving" else if(latest < previous) "📉 Declining" else "➡️ Stable"
                                                } else "➡️ Baseline"

                                                val chronologicalScores = avgScores.take(10).reversed().map { it.toFloat() }

                                                Card(
                                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6)),
                                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(16.dp)) {
                                                        Text("All-Time Gamification Metrics", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                            StatBox("Sessions", "$totalGameSessions", Color.DarkGray)
                                                            StatBox("Avg Score", "${overallAvgScore.toInt()}/100", primaryColor)
                                                            StatBox("Survival Time", "${totalGameTimeSecs.toInt()}s", Color.DarkGray)

                                                        }
                                                        Spacer(modifier = Modifier.height(12.dp))
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text("Performance Trend: ", color = Color.Gray, fontSize = 12.sp)
                                                            Text(trendText, fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 14.sp)
                                                        }
                                                        Text("Score represents the Composite Neuro-Motor average.", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                                                    }
                                                }

                                                PerformanceTrendChart(scores = chronologicalScores, targetScore = 80f, primaryColor = primaryColor)

                                                Card(
                                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(16.dp)) {
                                                        Text("Game Breakdown", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))

                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text("GAME", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                                                            Text("PLAYS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                                                            Text("TIME", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                                                            Text("SCORE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                                                            Spacer(modifier = Modifier.width(20.dp))
                                                        }
                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        BreakdownRow(
                                                            title = "Color Sorter",
                                                            plays = colorSorterResults.size,
                                                            timeStr = "-",
                                                            scoreStr = "${if(colorSorterResults.isNotEmpty()) colorSorterResults.map{it.score}.average().toInt() else 0} pts",
                                                            color = primaryColor,
                                                            onClick = { selectedGame = "Color Sorter" }
                                                        )

                                                        BreakdownRow(
                                                            title = "Maze Balance",
                                                            plays = ratPuzzleResults.size,
                                                            timeStr = "${ratPuzzleResults.sumOf { it.timeTakenMs } / 1000L}s",
                                                            scoreStr = "${if(ratPuzzleResults.isNotEmpty()) (ratPuzzleResults.count{it.isWin}.toFloat()/ratPuzzleResults.size*100).toInt() else 0}% Win",
                                                            color = primaryColor,
                                                            onClick = { selectedGame = "Rat Puzzle" }
                                                        )

                                                        BreakdownRow(
                                                            title = "Starship Def.",
                                                            plays = starshipResults.size,
                                                            timeStr = "${starshipResults.sumOf { it.timeSurvivedMs } / 1000L}s",
                                                            scoreStr = "${if(starshipResults.isNotEmpty()) starshipResults.map{it.score}.average().toInt() else 0} pts",
                                                            color = primaryColor,
                                                            onClick = { selectedGame = "Starship Defender" }
                                                        )

                                                        BreakdownRow(
                                                            title = "Hole Navigator",
                                                            plays = holePuzzleResults.size,
                                                            timeStr = "${holePuzzleResults.sumOf { it.timeSurvivedMs } / 1000L}s",
                                                            scoreStr = "${if(holePuzzleResults.isNotEmpty()) holePuzzleResults.map{it.score}.average().toInt() else 0} pts",
                                                            color = primaryColor,
                                                            onClick = { selectedGame = "Hole Navigator" }
                                                        )

                                                        BreakdownRow(
                                                            title = "Cog. Stepping",
                                                            plays = stepGameResults.size,
                                                            timeStr = "-",
                                                            scoreStr = "${if(stepGameResults.isNotEmpty()) stepGameResults.map{it.score}.average().toInt() else 0} pts",
                                                            color = primaryColor,
                                                            onClick = { selectedGame = "Step Game" }
                                                        )
                                                    }
                                                }

                                                Text("Select a Game Session", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))

                                                gameSessions.forEach { session ->
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { selectedOverviewSession = session },
                                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column {
                                                                Text(text = fullDateFormatter.format(session.timestamp), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)

                                                                val moduleCount = session.colorSorterResults.size + session.ratPuzzleResults.size +
                                                                        session.starshipResults.size + session.holePuzzleResults.size +
                                                                        session.stepGameResults.size
                                                                Text(text = "$moduleCount Game Modules Played", fontSize = 14.sp, color = Color.DarkGray)
                                                            }
                                                            Icon(Icons.Default.ArrowForward, contentDescription = "View Report", tint = primaryColor)
                                                        }
                                                    }
                                                }

                                                ReportActionButtons(
                                                    primaryColor = primaryColor,
                                                    onShareClick = {
                                                        gameSessions.firstOrNull()?.let { session ->
                                                            ClinicalReportPdfManager.generateAndSharePdf(context, patient, session, true)
                                                        } ?: Toast.makeText(context, "No session data available", Toast.LENGTH_SHORT).show()
                                                    },
                                                    onDownloadClick = {
                                                        gameSessions.firstOrNull()?.let { session ->
                                                            ClinicalReportPdfManager.generateAndSharePdf(context, patient, session, false)
                                                        } ?: Toast.makeText(context, "No session data available", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            }
                                        } else {
                                            val session = selectedOverviewSession!!
                                            val sortedAsc = gameSessions.sortedBy { it.timestamp }
                                            val currentIndex = sortedAsc.indexOf(session)
                                            val prevSession = if (currentIndex > 0) sortedAsc[currentIndex - 1] else null

                                            val latestColor = session.colorSorterResults.maxByOrNull { it.timestamp }
                                            val latestRat = session.ratPuzzleResults.maxByOrNull { it.timestamp }
                                            val latestStarship = session.starshipResults.maxByOrNull { it.timestamp }
                                            val latestHole = session.holePuzzleResults.maxByOrNull { it.timestamp }
                                            val latestStep = session.stepGameResults.maxByOrNull { it.timestamp }

                                            val colorScore = latestColor?.let { ((it.redCollected + it.greenCollected) / 30f).coerceIn(0f, 1f) } ?: 0f
                                            val ratScore = latestRat?.let { if (it.isWin) 1f else 0f } ?: 0f
                                            val starshipScore = latestStarship?.let { (it.timeSurvivedMs / 60000f).coerceIn(0f, 1f) } ?: 0f
                                            val holeScore = latestHole?.let { (it.timeSurvivedMs / 60000f).coerceIn(0f, 1f) } ?: 0f
                                            val stepScore = latestStep?.let { val tot = it.correctHits + it.incorrectHits; if (tot > 0) it.correctHits.toFloat() / tot else 0f } ?: 0f

                                            val gamesPlayed = listOfNotNull(latestColor, latestRat, latestStarship, latestHole, latestStep).size
                                            val globalGameScore = if (gamesPlayed > 0) {
                                                ((colorScore + ratScore + starshipScore + holeScore + stepScore) / gamesPlayed.toFloat()) * 100
                                            } else 0f

                                            val prevColorScore = prevSession?.colorSorterResults?.maxByOrNull { it.timestamp }?.let { ((it.redCollected + it.greenCollected) / 30f).coerceIn(0f, 1f) } ?: 0f
                                            val prevRatScore = prevSession?.ratPuzzleResults?.maxByOrNull { it.timestamp }?.let { if (it.isWin) 1f else 0f } ?: 0f
                                            val prevStarshipScore = prevSession?.starshipResults?.maxByOrNull { it.timestamp }?.let { (it.timeSurvivedMs / 60000f).coerceIn(0f, 1f) } ?: 0f
                                            val prevHoleScore = prevSession?.holePuzzleResults?.maxByOrNull { it.timestamp }?.let { (it.timeSurvivedMs / 60000f).coerceIn(0f, 1f) } ?: 0f
                                            val prevStepScore = prevSession?.stepGameResults?.maxByOrNull { it.timestamp }?.let { val tot = it.correctHits + it.incorrectHits; if (tot > 0) it.correctHits.toFloat() / tot else 0f } ?: 0f

                                            val prevGamesPlayed = listOfNotNull(
                                                prevSession?.colorSorterResults?.maxByOrNull { it.timestamp },
                                                prevSession?.ratPuzzleResults?.maxByOrNull { it.timestamp },
                                                prevSession?.starshipResults?.maxByOrNull { it.timestamp },
                                                prevSession?.holePuzzleResults?.maxByOrNull { it.timestamp },
                                                prevSession?.stepGameResults?.maxByOrNull { it.timestamp }
                                            ).size
                                            val prevGlobalGameScore = if (prevGamesPlayed > 0) {
                                                ((prevColorScore + prevRatScore + prevStarshipScore + prevHoleScore + prevStepScore) / prevGamesPlayed.toFloat()) * 100
                                            } else 0f

                                            val currentSurvival = (latestStarship?.timeSurvivedMs ?: 0L) + (latestHole?.timeSurvivedMs ?: 0L) + (latestRat?.timeTakenMs ?: 0L)
                                            val prevSurvival = (prevSession?.starshipResults?.maxByOrNull { it.timestamp }?.timeSurvivedMs ?: 0L) +
                                                    (prevSession?.holePuzzleResults?.maxByOrNull { it.timestamp }?.timeSurvivedMs ?: 0L) +
                                                    (prevSession?.ratPuzzleResults?.maxByOrNull { it.timestamp }?.timeTakenMs ?: 0L)

                                            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                                Row(modifier = Modifier.clickable { selectedOverviewSession = null }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Back to Sessions", color = primaryColor, fontWeight = FontWeight.Bold)
                                                }

                                                SessionSummaryBanner(dateString = fullDateFormatter.format(session.timestamp), posture = null, level = null)

                                                Text("Neuro-Motor Cognitive Report", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))

                                                ClinicalComparisonCard(
                                                    title = "Composite Neuro-Motor Score",
                                                    currentValue = globalGameScore,
                                                    previousValue = if (prevSession != null) prevGlobalGameScore else null,
                                                    unit = "/ 100",
                                                    isLowerBetter = false,
                                                    primaryColor = primaryColor
                                                )
                                                InfoSection("Calculated by aggregating your normalized performance across all games. It measures cognitive processing, reaction speed, and dynamic endurance.")

                                                ClinicalComparisonCard(
                                                    title = "Total Survival & Completion Time",
                                                    currentValue = currentSurvival / 1000f,
                                                    previousValue = if (prevSession != null) prevSurvival / 1000f else null,
                                                    unit = "Sec",
                                                    isLowerBetter = false,
                                                    primaryColor = Color(0xFF4CAF50)
                                                )

                                                Card(
                                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("Cognitive & Reflex Domains", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 16.sp)
                                                        Text("Cognitive abilities (memory, attention, decision-making) and Reflex performance (reaction time, responsiveness to visual stimuli).", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                                                        Spacer(modifier = Modifier.height(16.dp))
                                                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(16.dp), contentAlignment = Alignment.Center) {
                                                            ClinicalRadarChart(
                                                                currentScores = listOf(colorScore, ratScore, starshipScore, holeScore, stepScore),
                                                                prevScores = listOf(prevColorScore, prevRatScore, prevStarshipScore, prevHoleScore, prevStepScore),
                                                                labels = listOf("Reactive Speed", "Motor Precision", "Targeting", "Dynamic Endurance", "Processing"),
                                                                primaryColor = primaryColor
                                                            )
                                                        }
                                                    }
                                                }

                                                Text("Gamification Results", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))

                                                session.colorSorterResults.forEach { res ->
                                                    GameMiniCard(title = "Color Sorter", scoreText = "${res.score} pts", subText = "Missed: ${res.missedCount}", onClick = {
                                                        clearDetailedSessions()
                                                        selectedGame = "Color Sorter"
                                                        selectedColorSorterSession = res
                                                    })
                                                }
                                                session.ratPuzzleResults.forEach { res ->
                                                    GameMiniCard(title = "Maze Balance", scoreText = if(res.isWin) "Solved" else "Failed", subText = "Time: ${res.timeTakenMs/1000}s", isSuccess = res.isWin, onClick = {
                                                        clearDetailedSessions()
                                                        selectedGame = "Rat Puzzle"
                                                        selectedRatSession = res
                                                    })
                                                }
                                                session.starshipResults.forEach { res ->
                                                    GameMiniCard(title = "Starship Defender", scoreText = "${res.score} pts", subText = "Survived: ${res.timeSurvivedMs/1000}s", onClick = {
                                                        clearDetailedSessions()
                                                        selectedGame = "Starship Defender"
                                                        selectedStarshipSession = res
                                                    })
                                                }
                                                session.holePuzzleResults.forEach { res ->
                                                    GameMiniCard(title = "Hole Navigator", scoreText = "${res.score} pts", subText = "Dodged: ${res.holesDodged}", onClick = {
                                                        clearDetailedSessions()
                                                        selectedGame = "Hole Navigator"
                                                        selectedHoleSession = res
                                                    })
                                                }
                                                session.stepGameResults.forEach { res ->
                                                    GameMiniCard(title = "Cognitive Stepping", scoreText = "${res.score} pts", subText = "Hits: ${res.correctHits} / Misses: ${res.incorrectHits}", onClick = {
                                                        clearDetailedSessions()
                                                        selectedGame = "Step Game"
                                                        selectedStepSession = res
                                                    })
                                                }

                                                ReportActionButtons(
                                                    primaryColor = primaryColor,
                                                    onShareClick = {
                                                        ClinicalReportPdfManager.generateAndSharePdf(context, patient, session, true)
                                                    },
                                                    onDownloadClick = {
                                                        ClinicalReportPdfManager.generateAndSharePdf(context, patient, session, false)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // ==========================================
                                // STATIC BALANCE LOGIC
                                // ==========================================
                                "Static Balance" -> {
                                    if (staticBalanceResults.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("No data available yet.", color = Color.Gray)
                                        }
                                    } else if (selectedStaticSession == null) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            Row(modifier = Modifier.clickable { selectedGame = "Overview Dashboard" }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Back to Overview", color = primaryColor, fontWeight = FontWeight.Bold)
                                            }
                                            Text("Select a Session to View Graphs", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))

                                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                items(staticBalanceResults.sortedByDescending { it.timestamp }) { session ->
                                                    val dateString = fullDateFormatter.format(session.timestamp)

                                                    Card(
                                                        modifier = Modifier.fillMaxWidth().clickable { selectedStaticSession = session },
                                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column {
                                                                Text(text = dateString, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                                                Text(
                                                                    text = if (session.gameMode.equals("SITTING", ignoreCase = true)) "Mode: ${session.gameMode}" else "Mode: ${session.gameMode} | Level: ${session.level}",
                                                                    fontSize = 14.sp,
                                                                    color = Color.DarkGray
                                                                )
                                                            }
                                                            Text(text = "${session.efficiencyPercentage}% Eff.", fontWeight = FontWeight.Bold, color = primaryColor)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        val session = selectedStaticSession!!
                                        val sorted = staticBalanceResults.sortedBy { it.timestamp }
                                        val currentIndex = sorted.indexOf(session)
                                        val prevSession = if (currentIndex > 0) sorted[currentIndex - 1] else null
                                        val historyWindow = sorted.subList(maxOf(0, currentIndex - 4), currentIndex + 1)

                                        val recentData = historyWindow.mapIndexed { index, it ->
                                            val label = if (index == historyWindow.lastIndex) "Selected" else dateFormatter.format(it.timestamp)
                                            val value = when(selectedMetric) {
                                                "Falls" -> it.fallCount.toFloat()
                                                "Balance Time" -> it.balanceTimeMs / 1000f
                                                "Avg Fall Error (°)" -> it.fallErrors.takeIf { e -> e.isNotEmpty() }?.average()?.toFloat() ?: 0f
                                                else -> it.efficiencyPercentage.toFloat()
                                            }
                                            ChartData(label, value)
                                        }

                                        val pb = when(selectedMetric) {
                                            "Falls" -> sorted.minOfOrNull { it.fallCount.toFloat() } ?: 0f
                                            "Balance Time" -> sorted.maxOfOrNull { it.balanceTimeMs / 1000f } ?: 0f
                                            "Avg Fall Error (°)" -> sorted.minOfOrNull { it.fallErrors.takeIf { e -> e.isNotEmpty() }?.average()?.toFloat() ?: 0f } ?: 0f
                                            else -> sorted.maxOfOrNull { it.efficiencyPercentage.toFloat() } ?: 0f
                                        }
                                        val isLowerBetter = selectedMetric == "Falls" || selectedMetric == "Avg Fall Error (°)"
                                        val dateString = fullDateFormatter.format(session.timestamp)

                                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                            Row(modifier = Modifier.clickable {
                                                if(selectedGroupedSession != null) selectedMode = "Session"
                                                else if(selectedOverviewSession != null) selectedGame = "Overview Dashboard"
                                                selectedStaticSession = null
                                            }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(if(selectedGroupedSession != null || selectedOverviewSession != null) "Back to Session Report" else "Back to List", color = primaryColor, fontWeight = FontWeight.Bold)
                                            }

                                            SessionSummaryBanner(dateString = dateString, posture = session.gameMode, level = session.level)

                                            MetricSelector(metrics = availableMetrics, selectedMetric = selectedMetric, onMetricSelected = { selectedMetric = it }, primaryColor = primaryColor)

                                            PerformanceDashboard(
                                                title = "$selectedMetric Progression",
                                                metrics = recentData,
                                                personalBest = pb,
                                                isLowerBetter = isLowerBetter,
                                                primaryColor = primaryColor
                                            )

                                            Spacer(modifier = Modifier.height(16.dp))

                                            ClinicalComparisonCard(
                                                title = "Falls vs Previous Session",
                                                currentValue = session.fallCount.toFloat(),
                                                previousValue = prevSession?.fallCount?.toFloat(),
                                                unit = "Falls",
                                                isLowerBetter = true,
                                                primaryColor = primaryColor
                                            )

                                            val currentAvgError = if (session.fallErrors.isNotEmpty()) session.fallErrors.average().toFloat() else 0f
                                            val prevAvgError = if (prevSession?.fallErrors?.isNotEmpty() == true) prevSession.fallErrors.average().toFloat() else null
                                            ClinicalComparisonCard(
                                                title = "Avg Fall Error vs Previous",
                                                currentValue = currentAvgError,
                                                previousValue = prevAvgError,
                                                unit = "° (Degrees)",
                                                isLowerBetter = true,
                                                primaryColor = primaryColor
                                            )

                                            val baseLimit = if (session.gameMode == "SITTING") 6f else 10f
                                            ClinicalErrorBarChart(
                                                title = "Total Sway Angle vs Safe Limit (°)",
                                                errors = session.fallErrors,
                                                barLabelPrefix = "F",
                                                baseLimit = baseLimit
                                            )

                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    Text("Balance Path & Stats", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 16.sp)
                                                    Spacer(modifier = Modifier.height(16.dp))

                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .aspectRatio(1f)
                                                            .background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp))
                                                            .padding(16.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                                            val centerX = size.width / 2
                                                            val centerY = size.height / 2
                                                            val outerRadius = size.width / 2.8f

                                                            val maxTiltDegrees = if (session.gameMode == "SITTING") 6f else 10f
                                                            val innerMultiplier = if (session.gameMode == "SITTING") 0.07f else when(session.level) { 1 -> 0.20f; 2 -> 0.30f; else -> 0.40f }
                                                            val middleMultiplier = if (session.gameMode == "SITTING") 0.40f else when(session.level) { 1 -> 0.50f; 2 -> 0.60f; else -> 0.70f }

                                                            val middleRadius = outerRadius * middleMultiplier
                                                            val innerRadius = outerRadius * innerMultiplier

                                                            val colorOuterBg = Color(0xFFF3D8D9)
                                                            val colorOuterStroke = Color(0xFFCD5A5F)
                                                            val colorMiddleBg = Color(0xFFF9D199)
                                                            val colorTargetGreen = Color(0xFF5CB85C)
                                                            val colorTeal = Color(0xFF188B97)

                                                            drawCircle(color = colorOuterBg, radius = outerRadius, center = Offset(centerX, centerY))
                                                            drawCircle(color = colorOuterStroke, radius = outerRadius, center = Offset(centerX, centerY), style = Stroke(width = 4.dp.toPx()))
                                                            drawCircle(color = colorMiddleBg, radius = middleRadius, center = Offset(centerX, centerY))
                                                            drawCircle(color = Color.White, radius = middleRadius, center = Offset(centerX, centerY), style = Stroke(width = 2.dp.toPx()))
                                                            drawCircle(color = colorTargetGreen.copy(alpha = 0.4f), radius = innerRadius, center = Offset(centerX, centerY))
                                                            drawCircle(
                                                                brush = Brush.radialGradient(
                                                                    colors = listOf(colorTargetGreen.copy(alpha = 0.8f), colorTargetGreen.copy(alpha = 0.2f), Color.Transparent),
                                                                    center = Offset(centerX, centerY),
                                                                    radius = innerRadius * 1.2f
                                                                ),
                                                                radius = innerRadius * 1.2f,
                                                                center = Offset(centerX, centerY)
                                                            )
                                                            drawCircle(color = Color.White, radius = innerRadius, center = Offset(centerX, centerY), style = Stroke(width = 2.dp.toPx()))

                                                            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                                                            drawLine(color = colorTeal.copy(alpha = 0.3f), start = Offset(centerX - outerRadius, centerY), end = Offset(centerX + outerRadius, centerY), strokeWidth = 3.dp.toPx(), pathEffect = pathEffect)
                                                            drawLine(color = colorTeal.copy(alpha = 0.3f), start = Offset(centerX, centerY - outerRadius), end = Offset(centerX, centerY + outerRadius), strokeWidth = 3.dp.toPx(), pathEffect = pathEffect)

                                                            var lastX = centerX
                                                            var lastY = centerY

                                                            if (session.frontalData.isNotEmpty() && session.sagittalData.isNotEmpty()) {
                                                                val userPath = Path()
                                                                val sizeToUse = minOf(session.frontalData.size, session.sagittalData.size)

                                                                for (i in 0 until sizeToUse) {
                                                                    val pitch = session.frontalData[i]
                                                                    val yaw = session.sagittalData[i]

                                                                    val rawDotXOffset = (yaw / maxTiltDegrees) * outerRadius
                                                                    val rawDotYOffset = -(pitch / maxTiltDegrees) * outerRadius
                                                                    val rawDistancePx = sqrt(rawDotXOffset * rawDotXOffset + rawDotYOffset * rawDotYOffset)
                                                                    val scale = if (rawDistancePx > outerRadius) outerRadius / rawDistancePx else 1f

                                                                    val pxX = centerX + (rawDotXOffset * scale)
                                                                    val pxY = centerY + (rawDotYOffset * scale)

                                                                    if (i == 0) userPath.moveTo(pxX, pxY) else userPath.lineTo(pxX, pxY)

                                                                    if (i == sizeToUse - 1) {
                                                                        lastX = pxX
                                                                        lastY = pxY
                                                                    }
                                                                }
                                                                drawPath(path = userPath, color = colorTeal.copy(alpha = 0.7f), style = Stroke(width = 4.dp.toPx(), join = StrokeJoin.Round))
                                                            }

                                                            val annotationDash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                                                            val bpStartX = centerX - outerRadius * 1.15f
                                                            val bpStartY = centerY - outerRadius * 1.15f
                                                            drawLine(
                                                                color = Color.DarkGray,
                                                                start = Offset(bpStartX, bpStartY),
                                                                end = Offset(centerX - innerRadius * 0.5f, centerY - innerRadius * 0.5f),
                                                                strokeWidth = 1.dp.toPx(),
                                                                pathEffect = annotationDash
                                                            )
                                                            drawContext.canvas.nativeCanvas.drawText("Balance Point", bpStartX - 10f, bpStartY + 10f, textPaintLeft)

                                                            val flStartX = centerX + outerRadius * 1.15f
                                                            val flStartY = centerY - outerRadius * 1.15f
                                                            drawLine(
                                                                color = Color.Red.copy(alpha = 0.7f),
                                                                start = Offset(flStartX, flStartY),
                                                                end = Offset(centerX + outerRadius * 0.8f, centerY - outerRadius * 0.6f),
                                                                strokeWidth = 1.dp.toPx(),
                                                                pathEffect = annotationDash
                                                            )
                                                            drawContext.canvas.nativeCanvas.drawText("Fall Limit ($maxTiltDegrees°)", flStartX + 10f, flStartY + 10f, textPaintRight)

                                                            if (session.frontalData.isNotEmpty()) {
                                                                drawCircle(color = colorTeal, radius = 4.dp.toPx(), center = Offset(lastX, lastY))
                                                                val spStartX = centerX + outerRadius * 1.15f
                                                                val spStartY = centerY + outerRadius * 1.15f
                                                                drawLine(
                                                                    color = colorTeal,
                                                                    start = Offset(spStartX, spStartY),
                                                                    end = Offset(lastX, lastY),
                                                                    strokeWidth = 1.dp.toPx(),
                                                                    pathEffect = annotationDash
                                                                )
                                                                drawContext.canvas.nativeCanvas.drawText("Patient Sway", spStartX + 10f, spStartY, textPaintRight)
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                                    Spacer(modifier = Modifier.height(16.dp))

                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                                        StatBox("Total Time", "${String.format(Locale.US, "%.1f", session.totalTimeMs / 1000f)}s", Color.DarkGray)
                                                        StatBox("Balanced", "${String.format(Locale.US, "%.1f", session.balanceTimeMs / 1000f)}s", Color(0xFF4CAF50))
                                                        StatBox("Imbalanced", "${String.format(Locale.US, "%.1f", session.imbalanceTimeMs / 1000f)}s", Color(0xFFFF9800))
                                                        StatBox("Falls", "${session.fallCount}", Color.Red)
                                                    }
                                                }
                                            }

                                            val parentSession = groupedSessions.find { it.sessionId == session.sessionId }
                                            ReportActionButtons(
                                                primaryColor = primaryColor,
                                                onShareClick = {
                                                    parentSession?.let { ClinicalReportPdfManager.generateAndSharePdf(context, patient, it, true) }
                                                        ?: Toast.makeText(context, "Session data not found", Toast.LENGTH_SHORT).show()
                                                },
                                                onDownloadClick = {
                                                    parentSession?.let { ClinicalReportPdfManager.generateAndSharePdf(context, patient, it, false) }
                                                        ?: Toast.makeText(context, "Session data not found", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                                }

                                // ==========================================
                                // PATTERN DRAWING LOGIC
                                // ==========================================
                                "Pattern Drawing" -> {
                                    if (patternDrawingResults.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("No data available yet.", color = Color.Gray)
                                        }
                                    } else if (selectedPatternSession == null) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            Row(modifier = Modifier.clickable { selectedGame = "Overview Dashboard" }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Back to Overview", color = primaryColor, fontWeight = FontWeight.Bold)
                                            }
                                            Text("Select a Pattern Session", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
                                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                items(patternDrawingResults.sortedByDescending { it.timestamp }) { session ->
                                                    val dateString = fullDateFormatter.format(session.timestamp)
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth().clickable { selectedPatternSession = session },
                                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                                    ) {
                                                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                            Column {
                                                                Text(text = dateString, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                                                Text(text = "${session.levelName} (${session.gameMode})", fontSize = 14.sp, color = Color.DarkGray)
                                                            }
                                                            Text(text = "${session.targetsHit}/${session.totalTargets} Hits", fontWeight = FontWeight.Bold, color = primaryColor)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        val session = selectedPatternSession!!
                                        val sorted = patternDrawingResults.sortedBy { it.timestamp }
                                        val currentIndex = sorted.indexOf(session)
                                        val prevSession = if (currentIndex > 0) sorted[currentIndex - 1] else null
                                        val historyWindow = sorted.subList(maxOf(0, currentIndex - 4), currentIndex + 1)

                                        val recentData = historyWindow.mapIndexed { index, it ->
                                            val label = if (index == historyWindow.lastIndex) "Selected" else dateFormatter.format(it.timestamp)
                                            val value = when(selectedMetric) {
                                                "Falls" -> it.fallCount.toFloat()
                                                "Time" -> it.timeTakenMs / 1000f
                                                "Avg Error (°)" -> it.angularErrors.takeIf { e -> e.isNotEmpty() }?.average()?.toFloat() ?: 0f
                                                else -> it.targetsHit.toFloat()
                                            }
                                            ChartData(label, value)
                                        }

                                        val pb = when(selectedMetric) {
                                            "Falls" -> sorted.minOfOrNull { it.fallCount.toFloat() } ?: 0f
                                            "Time" -> sorted.minOfOrNull { it.timeTakenMs / 1000f } ?: 0f
                                            "Avg Error (°)" -> sorted.minOfOrNull { it.angularErrors.takeIf { e -> e.isNotEmpty() }?.average()?.toFloat() ?: 0f } ?: 0f
                                            else -> sorted.maxOfOrNull { it.targetsHit.toFloat() } ?: 0f
                                        }

                                        val isLowerBetter = selectedMetric == "Falls" || selectedMetric == "Time" || selectedMetric == "Avg Error (°)"
                                        val dateString = fullDateFormatter.format(session.timestamp)

                                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                            Row(modifier = Modifier.clickable {
                                                if(selectedGroupedSession != null) selectedMode = "Session"
                                                else if(selectedOverviewSession != null) selectedGame = "Overview Dashboard"
                                                selectedPatternSession = null
                                            }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(if(selectedGroupedSession != null || selectedOverviewSession != null) "Back to Session Report" else "Back to List", color = primaryColor, fontWeight = FontWeight.Bold)
                                            }

                                            SessionSummaryBanner(dateString = dateString, posture = session.gameMode, level = session.level)

                                            MetricSelector(metrics = availableMetrics, selectedMetric = selectedMetric, onMetricSelected = { selectedMetric = it }, primaryColor = primaryColor)

                                            PerformanceDashboard(
                                                title = "$selectedMetric Progression",
                                                metrics = recentData,
                                                personalBest = pb,
                                                isLowerBetter = isLowerBetter,
                                                primaryColor = primaryColor
                                            )

                                            Spacer(modifier = Modifier.height(16.dp))

                                            ClinicalComparisonCard(
                                                title = "Falls vs Previous Session",
                                                currentValue = session.fallCount.toFloat(),
                                                previousValue = prevSession?.fallCount?.toFloat(),
                                                unit = "Falls",
                                                isLowerBetter = true,
                                                primaryColor = primaryColor
                                            )

                                            val currentAvgError = if (session.angularErrors.isNotEmpty()) session.angularErrors.average().toFloat() else 0f
                                            val prevAvgError = if (prevSession?.angularErrors?.isNotEmpty() == true) prevSession.angularErrors.average().toFloat() else null
                                            ClinicalComparisonCard(
                                                title = "Avg Angular Error vs Previous",
                                                currentValue = currentAvgError,
                                                previousValue = prevAvgError,
                                                unit = "° (Degrees)",
                                                isLowerBetter = true,
                                                primaryColor = primaryColor
                                            )

                                            val toleranceLimit = when (session.level) {
                                                1 -> 15f
                                                2 -> 12f
                                                else -> 10f
                                            }
                                            ClinicalErrorBarChart(
                                                title = "Target Error vs Acceptable Tolerance (°)",
                                                errors = session.angularErrors,
                                                barLabelPrefix = "T",
                                                baseLimit = toleranceLimit
                                            )

                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    Text("Accuracy & Path Tolerance Overlay", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 16.sp)
                                                    Spacer(modifier = Modifier.height(16.dp))

                                                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                                            val centerX = size.width / 2
                                                            val centerY = size.height / 2
                                                            val outerRadius = size.width / 2.8f
                                                            val maxTiltDegrees = if (session.gameMode == "SITTING") 14f else 20f

                                                            val numSpokes = when (session.level) { 1 -> 4; 2 -> 6; else -> 8 }
                                                            val targetOffsets = mutableListOf<Offset>()
                                                            targetOffsets.add(Offset(0f, 0f))
                                                            for (i in 0 until numSpokes) {
                                                                val angle = Math.PI * 2 * i / numSpokes - (Math.PI / 2)
                                                                targetOffsets.add(Offset(cos(angle).toFloat() * 0.8f, sin(angle).toFloat() * 0.8f))
                                                                targetOffsets.add(Offset(0f, 0f))
                                                            }

                                                            val idealPath = Path().apply {
                                                                val start = Offset(centerX + (targetOffsets.first().x * outerRadius), centerY + (targetOffsets.first().y * outerRadius))
                                                                moveTo(start.x, start.y)
                                                                for (i in 1 until targetOffsets.size) {
                                                                    val px = Offset(centerX + (targetOffsets[i].x * outerRadius), centerY + (targetOffsets[i].y * outerRadius))
                                                                    lineTo(px.x, px.y)
                                                                }
                                                            }

                                                            drawPath(path = idealPath, color = Color.LightGray.copy(alpha = 0.5f), style = Stroke(width = 40f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                                            drawPath(path = idealPath, color = Color.Gray.copy(alpha = 0.8f), style = Stroke(width = 4f))

                                                            var lastX = centerX
                                                            var lastY = centerY

                                                            if (session.frontalData.isNotEmpty() && session.sagittalData.isNotEmpty()) {
                                                                val userPath = Path()
                                                                val sizeToUse = minOf(session.frontalData.size, session.sagittalData.size)

                                                                for (i in 0 until sizeToUse) {
                                                                    val pitch = session.frontalData[i]
                                                                    val yaw = session.sagittalData[i]

                                                                    val rawDotX = (yaw / maxTiltDegrees) * outerRadius
                                                                    val rawDotY = -(pitch / maxTiltDegrees) * outerRadius

                                                                    val dist = sqrt(rawDotX * rawDotX + rawDotY * rawDotY)
                                                                    val scale = if (dist > outerRadius) outerRadius / dist else 1f

                                                                    val pxX = centerX + (rawDotX * scale)
                                                                    val pxY = centerY + (rawDotY * scale)

                                                                    if (i == 0) userPath.moveTo(pxX, pxY) else userPath.lineTo(pxX, pxY)

                                                                    if (i == sizeToUse - 1) {
                                                                        lastX = pxX
                                                                        lastY = pxY
                                                                    }
                                                                }
                                                                drawPath(path = userPath, color = Color(0xFF188B97).copy(alpha = 0.8f), style = Stroke(width = 6f, join = StrokeJoin.Round))
                                                            }

                                                            val annotationDash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                                                            val tpStartX = centerX - outerRadius * 1.15f
                                                            val tpStartY = centerY - outerRadius * 1.15f
                                                            val tgt1 = targetOffsets.getOrNull(1) ?: Offset(0f, 0f)
                                                            drawLine(
                                                                color = Color.Gray,
                                                                start = Offset(tpStartX, tpStartY),
                                                                end = Offset(centerX + tgt1.x * outerRadius, centerY + tgt1.y * outerRadius),
                                                                strokeWidth = 1.dp.toPx(),
                                                                pathEffect = annotationDash
                                                            )
                                                            drawContext.canvas.nativeCanvas.drawText("Ideal Path", tpStartX - 10f, tpStartY + 10f, textPaintLeft)

                                                            val coStartX = centerX - outerRadius * 1.15f
                                                            val coStartY = centerY + outerRadius * 1.15f
                                                            drawLine(
                                                                color = Color.DarkGray,
                                                                start = Offset(coStartX, coStartY),
                                                                end = Offset(centerX, centerY),
                                                                strokeWidth = 1.dp.toPx(),
                                                                pathEffect = annotationDash
                                                            )
                                                            drawContext.canvas.nativeCanvas.drawText("Center Start", coStartX - 10f, coStartY, textPaintLeft)

                                                            if (session.frontalData.isNotEmpty()) {
                                                                drawCircle(color = Color(0xFF188B97), radius = 4.dp.toPx(), center = Offset(lastX, lastY))
                                                                val pmStartX = centerX + outerRadius * 1.15f
                                                                val pmStartY = centerY - outerRadius * 1.15f
                                                                drawLine(
                                                                    color = Color(0xFF188B97),
                                                                    start = Offset(pmStartX, pmStartY),
                                                                    end = Offset(lastX, lastY),
                                                                    strokeWidth = 1.dp.toPx(),
                                                                    pathEffect = annotationDash
                                                                )
                                                                drawContext.canvas.nativeCanvas.drawText("Patient Move", pmStartX + 10f, pmStartY + 10f, textPaintRight)
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                                    Spacer(modifier = Modifier.height(16.dp))

                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                                        StatBox("Score", "${session.targetsHit} / ${session.totalTargets}", primaryColor)
                                                        StatBox("Time", "${String.format(Locale.US, "%.1f", session.timeTakenMs / 1000f)}s", Color.DarkGray)
                                                        StatBox("Falls", "${session.fallCount}", Color.Red)
                                                    }
                                                }
                                            }

                                            val parentSession = groupedSessions.find { it.sessionId == session.sessionId }
                                            ReportActionButtons(
                                                primaryColor = primaryColor,
                                                onShareClick = {
                                                    parentSession?.let { ClinicalReportPdfManager.generateAndSharePdf(context, patient, it, true) }
                                                        ?: Toast.makeText(context, "Session data not found", Toast.LENGTH_SHORT).show()
                                                },
                                                onDownloadClick = {
                                                    parentSession?.let { ClinicalReportPdfManager.generateAndSharePdf(context, patient, it, false) }
                                                        ?: Toast.makeText(context, "Session data not found", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                                }

                                // ==========================================
                                // SHAPE TRAINING LOGIC
                                // ==========================================
                                "Shape Training" -> {
                                    if (shapeTrainingResults.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("No data available yet.", color = Color.Gray)
                                        }
                                    } else if (selectedShapeSession == null) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            Row(modifier = Modifier.clickable { selectedGame = "Overview Dashboard" }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Back to Overview", color = primaryColor, fontWeight = FontWeight.Bold)
                                            }
                                            Text("Select a Shape Training Session", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
                                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                items(shapeTrainingResults.sortedByDescending { it.timestamp }) { session ->
                                                    val dateString = fullDateFormatter.format(session.timestamp)
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth().clickable { selectedShapeSession = session },
                                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                                    ) {
                                                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                            Column {
                                                                Text(text = dateString, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                                                Text(text = "Time: ${session.timeTakenMs / 1000}s", fontSize = 14.sp, color = Color.DarkGray)
                                                            }
                                                            Text(text = "Score: ${session.score}", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        val session = selectedShapeSession!!
                                        val sorted = shapeTrainingResults.sortedBy { it.timestamp }
                                        val currentIndex = sorted.indexOf(session)
                                        val prevSession = if (currentIndex > 0) sorted[currentIndex - 1] else null
                                        val historyWindow = sorted.subList(maxOf(0, currentIndex - 4), currentIndex + 1)

                                        val recentData = historyWindow.mapIndexed { index, it ->
                                            val label = if (index == historyWindow.lastIndex) "Selected" else dateFormatter.format(it.timestamp)
                                            val value = when(selectedMetric) {
                                                "Falls" -> it.fallCount.toFloat()
                                                "Time" -> it.timeTakenMs / 1000f
                                                "Avg Error (°)" -> it.angularErrors.takeIf { e -> e.isNotEmpty() }?.average()?.toFloat() ?: 0f
                                                else -> it.score.toFloat()
                                            }
                                            ChartData(label, value)
                                        }

                                        val pb = when(selectedMetric) {
                                            "Falls" -> sorted.minOfOrNull { it.fallCount.toFloat() } ?: 0f
                                            "Time" -> sorted.minOfOrNull { it.timeTakenMs / 1000f } ?: 0f
                                            "Avg Error (°)" -> sorted.minOfOrNull { it.angularErrors.takeIf { e -> e.isNotEmpty() }?.average()?.toFloat() ?: 0f } ?: 0f
                                            else -> sorted.maxOfOrNull { it.score.toFloat() } ?: 0f
                                        }
                                        val isLowerBetter = selectedMetric == "Falls" || selectedMetric == "Time" || selectedMetric == "Avg Error (°)"
                                        val dateString = fullDateFormatter.format(session.timestamp)

                                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                            Row(modifier = Modifier.clickable {
                                                if(selectedGroupedSession != null) selectedMode = "Session"
                                                else if(selectedOverviewSession != null) selectedGame = "Overview Dashboard"
                                                selectedShapeSession = null
                                            }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(if(selectedGroupedSession != null || selectedOverviewSession != null) "Back to Session Report" else "Back to List", color = primaryColor, fontWeight = FontWeight.Bold)
                                            }

                                            SessionSummaryBanner(dateString = dateString, posture = session.gameMode, level = session.level)

                                            MetricSelector(metrics = availableMetrics, selectedMetric = selectedMetric, onMetricSelected = { selectedMetric = it }, primaryColor = primaryColor)

                                            PerformanceDashboard(
                                                title = "$selectedMetric Progression",
                                                metrics = recentData,
                                                personalBest = pb,
                                                isLowerBetter = isLowerBetter,
                                                primaryColor = primaryColor
                                            )

                                            Spacer(modifier = Modifier.height(16.dp))

                                            ClinicalComparisonCard(
                                                title = "Falls vs Previous Session",
                                                currentValue = session.fallCount.toFloat(),
                                                previousValue = prevSession?.fallCount?.toFloat(),
                                                unit = "Falls",
                                                isLowerBetter = true,
                                                primaryColor = primaryColor
                                            )

                                            val currentAvgError = if (session.angularErrors.isNotEmpty()) session.angularErrors.average().toFloat() else 0f
                                            val prevAvgError = if (prevSession?.angularErrors?.isNotEmpty() == true) prevSession.angularErrors.average().toFloat() else null
                                            ClinicalComparisonCard(
                                                title = "Avg Angular Error vs Previous",
                                                currentValue = currentAvgError,
                                                previousValue = prevAvgError,
                                                unit = "° (Degrees)",
                                                isLowerBetter = true,
                                                primaryColor = primaryColor
                                            )

                                            val toleranceLimit = when (session.level) {
                                                1 -> 15f
                                                2 -> 12f
                                                else -> 10f
                                            }
                                            ClinicalErrorBarChart(
                                                title = "Target Error vs Acceptable Tolerance (°)",
                                                errors = session.angularErrors,
                                                barLabelPrefix = "T",
                                                baseLimit = toleranceLimit
                                            )

                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    Text("Accuracy & Target Path Overlay", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 16.sp)
                                                    Spacer(modifier = Modifier.height(16.dp))

                                                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp)).padding(bottom = 16.dp), contentAlignment = Alignment.Center) {
                                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                                            val centerX = size.width / 2
                                                            val centerY = size.height / 2
                                                            val outerRadius = size.width / 2.8f
                                                            val targetPlacementRadius = outerRadius * 0.75f
                                                            val nodeRadius = 18.dp.toPx()
                                                            val maxTiltDegrees = if (session.gameMode == "SITTING") 14f else 16f

                                                            drawCircle(color = Color.LightGray.copy(alpha = 0.5f), radius = outerRadius, center = Offset(centerX, centerY), style = Stroke(width = 4f))
                                                            drawCircle(color = Color.LightGray.copy(alpha = 0.8f), radius = 6.dp.toPx(), center = Offset(centerX, centerY))

                                                            val angles = if (session.targetAngles.isNotEmpty()) session.targetAngles else listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f)
                                                            var tgtLastX = centerX
                                                            var tgtLastY = centerY

                                                            if (session.targetSequence.isNotEmpty()) {
                                                                val idealPath = Path()
                                                                idealPath.moveTo(centerX, centerY)

                                                                session.targetSequence.forEachIndexed { i, targetIndex ->
                                                                    val tx = centerX + (targetPlacementRadius * cos(Math.toRadians(angles[targetIndex].toDouble()))).toFloat()
                                                                    val ty = centerY + (targetPlacementRadius * sin(Math.toRadians(angles[targetIndex].toDouble()))).toFloat()
                                                                    idealPath.lineTo(tx, ty)

                                                                    if(i == 0) {
                                                                        tgtLastX = tx
                                                                        tgtLastY = ty
                                                                    }
                                                                }

                                                                drawPath(path = idealPath, color = Color.LightGray.copy(alpha = 0.5f), style = Stroke(width = 40f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                                                drawPath(path = idealPath, color = Color.Gray.copy(alpha = 0.8f), style = Stroke(width = 4f))
                                                            }

                                                            val colors = listOf(Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFDD835), Color(0xFF8E24AA), Color(0xFFF4511E), Color(0xFF00ACC1), Color(0xFFD81B60))
                                                            val shapes = listOf(0, 1, 2, 3, 0, 1, 2, 3)

                                                            for (i in 0 until 8) {
                                                                val angleRad = Math.toRadians(angles[i].toDouble())
                                                                val cx = centerX + (targetPlacementRadius * cos(angleRad)).toFloat()
                                                                val cy = centerY + (targetPlacementRadius * sin(angleRad)).toFloat()
                                                                val color = colors[i].copy(alpha = 0.3f)

                                                                when (shapes[i]) {
                                                                    0 -> drawCircle(color = color, radius = nodeRadius, center = Offset(cx, cy))
                                                                    1 -> drawRect(color = color, topLeft = Offset(cx - nodeRadius, cy - nodeRadius), size = androidx.compose.ui.geometry.Size(nodeRadius * 2, nodeRadius * 2))
                                                                    2 -> {
                                                                        val p = Path().apply { moveTo(cx, cy - nodeRadius); lineTo(cx + nodeRadius, cy + nodeRadius); lineTo(cx - nodeRadius, cy + nodeRadius); close() }
                                                                        drawPath(p, color)
                                                                    }
                                                                    3 -> {
                                                                        val p = Path()
                                                                        val innerRadius = nodeRadius * 0.4f
                                                                        var a = -Math.PI / 2
                                                                        for (j in 0 until 10) {
                                                                            val r = if (j % 2 == 0) nodeRadius else innerRadius
                                                                            val x = cx + (r * cos(a)).toFloat()
                                                                            val y = cy + (r * sin(a)).toFloat()
                                                                            if (j == 0) p.moveTo(x, y) else p.lineTo(x, y)
                                                                            a += Math.PI / 5
                                                                        }
                                                                        p.close()
                                                                        drawPath(p, color)
                                                                    }
                                                                }
                                                            }

                                                            var lastX = centerX
                                                            var lastY = centerY

                                                            if (session.frontalData.isNotEmpty() && session.sagittalData.isNotEmpty()) {
                                                                val userPath = Path()
                                                                val sizeToUse = minOf(session.frontalData.size, session.sagittalData.size)

                                                                for (i in 0 until sizeToUse) {
                                                                    val pitch = session.frontalData[i]
                                                                    val yaw = session.sagittalData[i]

                                                                    val rawDotX = (yaw / maxTiltDegrees) * outerRadius
                                                                    val rawDotY = -(pitch / maxTiltDegrees) * outerRadius
                                                                    val dist = sqrt(rawDotX * rawDotX + rawDotY * rawDotY)
                                                                    val scale = if (dist > outerRadius) outerRadius / dist else 1f

                                                                    val pxX = centerX + (rawDotX * scale)
                                                                    val pxY = centerY + (rawDotY * scale)

                                                                    if (i == 0) userPath.moveTo(pxX, pxY) else userPath.lineTo(pxX, pxY)

                                                                    if (i == sizeToUse - 1) {
                                                                        lastX = pxX
                                                                        lastY = pxY
                                                                    }
                                                                }
                                                                drawPath(path = userPath, color = Color(0xFF188B97).copy(alpha = 0.8f), style = Stroke(width = 6f, join = StrokeJoin.Round))
                                                            }

                                                            val annotationDash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                                                            val tsStartX = centerX - outerRadius * 1.15f
                                                            val tsStartY = centerY - outerRadius * 1.15f
                                                            drawLine(
                                                                color = Color.Gray,
                                                                start = Offset(tsStartX, tsStartY),
                                                                end = Offset(tgtLastX, tgtLastY),
                                                                strokeWidth = 1.dp.toPx(),
                                                                pathEffect = annotationDash
                                                            )
                                                            drawContext.canvas.nativeCanvas.drawText("Target Path", tsStartX - 10f, tsStartY + 10f, textPaintLeft)

                                                            if (session.frontalData.isNotEmpty()) {
                                                                drawCircle(color = Color(0xFF188B97), radius = 4.dp.toPx(), center = Offset(lastX, lastY))
                                                                val pmStartX = centerX + outerRadius * 1.15f
                                                                val pmStartY = centerY - outerRadius * 1.15f
                                                                drawLine(
                                                                    color = Color(0xFF188B97),
                                                                    start = Offset(pmStartX, pmStartY),
                                                                    end = Offset(lastX, lastY),
                                                                    strokeWidth = 1.dp.toPx(),
                                                                    pathEffect = annotationDash
                                                                )
                                                                drawContext.canvas.nativeCanvas.drawText("Patient Move", pmStartX + 10f, pmStartY + 10f, textPaintRight)
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                                    Spacer(modifier = Modifier.height(16.dp))

                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                                        StatBox("Score", "${session.score}", primaryColor)
                                                        StatBox("Time", "${String.format(Locale.US, "%.1f", session.timeTakenMs / 1000f)}s", Color.DarkGray)
                                                        StatBox("Falls", "${session.fallCount}", Color.Red)
                                                    }
                                                }
                                            }

                                            val parentSession = groupedSessions.find { it.sessionId == session.sessionId }
                                            ReportActionButtons(
                                                primaryColor = primaryColor,
                                                onShareClick = {
                                                    parentSession?.let { ClinicalReportPdfManager.generateAndSharePdf(context, patient, it, true) }
                                                        ?: Toast.makeText(context, "Session data not found", Toast.LENGTH_SHORT).show()
                                                },
                                                onDownloadClick = {
                                                    parentSession?.let { ClinicalReportPdfManager.generateAndSharePdf(context, patient, it, false) }
                                                        ?: Toast.makeText(context, "Session data not found", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                                }

                                // ==========================================
                                // COLOR SORTER LOGIC
                                // ==========================================
                                "Color Sorter" -> {
                                    if (colorSorterResults.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("No data available yet.", color = Color.Gray)
                                        }
                                    } else if (selectedColorSorterSession == null) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            Row(modifier = Modifier.clickable { selectedGame = "Overview Dashboard" }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Back to Overview", color = primaryColor, fontWeight = FontWeight.Bold)
                                            }
                                            Text("Select a Color Sorter Session", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
                                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                items(colorSorterResults.sortedByDescending { it.timestamp }) { session ->
                                                    val dateString = fullDateFormatter.format(session.timestamp)
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth().clickable { selectedColorSorterSession = session },
                                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                                    ) {
                                                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                            Text(text = dateString, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                                            Text(text = "Score: ${session.score}", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        val session = selectedColorSorterSession!!
                                        val sorted = colorSorterResults.sortedBy { it.timestamp }
                                        val currentIndex = sorted.indexOf(session)
                                        val historyWindow = sorted.subList(maxOf(0, currentIndex - 4), currentIndex + 1)
                                        val recentData = historyWindow.mapIndexed { index, it ->
                                            val label = if (index == historyWindow.lastIndex) "Selected" else dateFormatter.format(it.timestamp)
                                            ChartData(label, it.score.toFloat())
                                        }
                                        val pb = sorted.maxOfOrNull { it.score.toFloat() } ?: 0f

                                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                            Row(modifier = Modifier.clickable {
                                                if(selectedGroupedSession != null) selectedMode = "Session"
                                                else if(selectedOverviewSession != null) selectedGame = "Overview Dashboard"
                                                selectedColorSorterSession = null
                                            }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(if(selectedGroupedSession != null || selectedOverviewSession != null) "Back to Session Report" else "Back to List", color = primaryColor, fontWeight = FontWeight.Bold)
                                            }

                                            PerformanceDashboard(
                                                title = "Performance Dashboard",
                                                metrics = recentData,
                                                personalBest = pb,
                                                primaryColor = primaryColor
                                            )

                                            Spacer(modifier = Modifier.height(16.dp))

                                            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                                                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                        Text("Final Score: ${session.score}", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                                        Text("Missed: ${session.missedCount}", color = Color.Red, fontSize = 16.sp)
                                                    }
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text("🔴 Red Sorted: ${session.redCollected}", color = Color.DarkGray)
                                                    Text("🟢 Green Sorted: ${session.greenCollected}", color = Color.DarkGray)
                                                }
                                            }

                                            val parentSession = groupedSessions.find { it.sessionId == session.sessionId }
                                            ReportActionButtons(
                                                primaryColor = primaryColor,
                                                onShareClick = {
                                                    parentSession?.let { ClinicalReportPdfManager.generateAndSharePdf(context, patient, it, true) }
                                                        ?: Toast.makeText(context, "Session data not found", Toast.LENGTH_SHORT).show()
                                                },
                                                onDownloadClick = {
                                                    parentSession?.let { ClinicalReportPdfManager.generateAndSharePdf(context, patient, it, false) }
                                                        ?: Toast.makeText(context, "Session data not found", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                                }

                                // ==========================================
                                // RAT PUZZLE LOGIC
                                // ==========================================
                                "Rat Puzzle" -> {
                                    if (ratPuzzleResults.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("No data available yet.", color = Color.Gray)
                                        }
                                    } else if (selectedRatSession == null) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            Row(modifier = Modifier.clickable { selectedGame = "Overview Dashboard" }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Back to Overview", color = primaryColor, fontWeight = FontWeight.Bold)
                                            }
                                            Text("Select a Maze Session", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
                                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                items(ratPuzzleResults.sortedByDescending { it.timestamp }) { session ->
                                                    val dateString = fullDateFormatter.format(session.timestamp)
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth().clickable { selectedRatSession = session },
                                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                                    ) {
                                                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                            Column {
                                                                Text(text = dateString, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                                                Text(text = if (session.isWin) "🏆 Completed" else "☠️ Failed", color = if (session.isWin) Color(0xFF4CAF50) else Color.Red, fontSize = 14.sp)
                                                            }
                                                            Text(text = "${session.timeTakenMs / 1000}s", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        val session = selectedRatSession!!
                                        val sorted = ratPuzzleResults.sortedBy { it.timestamp }
                                        val currentIndex = sorted.indexOf(session)
                                        val historyWindow = sorted.subList(maxOf(0, currentIndex - 4), currentIndex + 1)
                                        val recentData = historyWindow.mapIndexed { index, it ->
                                            val label = if (index == historyWindow.lastIndex) "Selected" else dateFormatter.format(it.timestamp)
                                            ChartData(label, it.timeTakenMs / 1000f)
                                        }
                                        val wins = sorted.filter { it.isWin }
                                        val pb = if (wins.isNotEmpty()) wins.minOf { it.timeTakenMs / 1000f } else sorted.minOfOrNull { it.timeTakenMs / 1000f } ?: 0f

                                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                            Row(modifier = Modifier.clickable {
                                                if(selectedGroupedSession != null) selectedMode = "Session"
                                                else if(selectedOverviewSession != null) selectedGame = "Overview Dashboard"
                                                selectedRatSession = null
                                            }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(if(selectedGroupedSession != null || selectedOverviewSession != null) "Back to Session Report" else "Back to List", color = primaryColor, fontWeight = FontWeight.Bold)
                                            }

                                            PerformanceDashboard(
                                                title = "Performance Dashboard",
                                                metrics = recentData,
                                                personalBest = pb,
                                                isLowerBetter = true,
                                                primaryColor = primaryColor
                                            )

                                            Spacer(modifier = Modifier.height(16.dp))

                                            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                                                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                        Text(if (session.isWin) "Result: Solved!" else "Result: Failed", color = if (session.isWin) Color(0xFF4CAF50) else Color.Red, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                                        Text("Time: ${session.timeTakenMs / 1000}s", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                                    }
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text("Lives Remaining: ${session.livesRemaining} / 3", color = Color.DarkGray)
                                                }
                                            }

                                            val parentSession = groupedSessions.find { it.sessionId == session.sessionId }
                                            ReportActionButtons(
                                                primaryColor = primaryColor,
                                                onShareClick = {
                                                    parentSession?.let { ClinicalReportPdfManager.generateAndSharePdf(context, patient, it, true) }
                                                        ?: Toast.makeText(context, "Session data not found", Toast.LENGTH_SHORT).show()
                                                },
                                                onDownloadClick = {
                                                    parentSession?.let { ClinicalReportPdfManager.generateAndSharePdf(context, patient, it, false) }
                                                        ?: Toast.makeText(context, "Session data not found", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                                }

                                // ==========================================
                                // STARSHIP DEFENDER LOGIC
                                // ==========================================
                                "Starship Defender" -> {
                                    if (starshipResults.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("No data available yet.", color = Color.Gray)
                                        }
                                    } else if (selectedStarshipSession == null) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            Row(modifier = Modifier.clickable { selectedGame = "Overview Dashboard" }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Back to Overview", color = primaryColor, fontWeight = FontWeight.Bold)
                                            }
                                            Text("Select a Mission Log", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
                                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                items(starshipResults.sortedByDescending { it.timestamp }) { session ->
                                                    val dateString = fullDateFormatter.format(session.timestamp)
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth().clickable { selectedStarshipSession = session },
                                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                                    ) {
                                                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                            Column {
                                                                Text(text = dateString, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                                                Text(text = if (session.isWin) "✅ Survived 60s" else "💥 Destroyed at ${session.timeSurvivedMs / 1000}s", color = if (session.isWin) Color(0xFF4CAF50) else Color.Red, fontSize = 14.sp)
                                                            }
                                                            Text(text = "Score: ${session.score}", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        val session = selectedStarshipSession!!
                                        val sorted = starshipResults.sortedBy { it.timestamp }
                                        val currentIndex = sorted.indexOf(session)
                                        val historyWindow = sorted.subList(maxOf(0, currentIndex - 4), currentIndex + 1)
                                        val recentData = historyWindow.mapIndexed { index, it ->
                                            val label = if (index == historyWindow.lastIndex) "Selected" else dateFormatter.format(it.timestamp)
                                            ChartData(label, it.score.toFloat())
                                        }
                                        val pb = sorted.maxOfOrNull { it.score.toFloat() } ?: 0f

                                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                            Row(modifier = Modifier.clickable {
                                                if(selectedGroupedSession != null) selectedMode = "Session"
                                                else if(selectedOverviewSession != null) selectedGame = "Overview Dashboard"
                                                selectedStarshipSession = null
                                            }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(if(selectedGroupedSession != null || selectedOverviewSession != null) "Back to Session Report" else "Back to List", color = primaryColor, fontWeight = FontWeight.Bold)
                                            }

                                            PerformanceDashboard(
                                                title = "Performance Dashboard",
                                                metrics = recentData,
                                                personalBest = pb,
                                                primaryColor = primaryColor
                                            )

                                            Spacer(modifier = Modifier.height(16.dp))

                                            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                                                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                        Text(if (session.isWin) "Mission Success!" else "Hull Breached", color = if (session.isWin) Color(0xFF4CAF50) else Color.Red, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                                        Text("Score: ${session.score}", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                                    }
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text("Time Survived: ${session.timeSurvivedMs / 1000}s", color = Color.DarkGray)
                                                    Text("Aliens Destroyed: ${session.aliensDestroyed}", color = Color.DarkGray)
                                                }
                                            }

                                            val parentSession = groupedSessions.find { it.sessionId == session.sessionId }
                                            ReportActionButtons(
                                                primaryColor = primaryColor,
                                                onShareClick = {
                                                    parentSession?.let { ClinicalReportPdfManager.generateAndSharePdf(context, patient, it, true) }
                                                        ?: Toast.makeText(context, "Session data not found", Toast.LENGTH_SHORT).show()
                                                },
                                                onDownloadClick = {
                                                    parentSession?.let { ClinicalReportPdfManager.generateAndSharePdf(context, patient, it, false) }
                                                        ?: Toast.makeText(context, "Session data not found", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                                }

                                // ==========================================
                                // HOLE NAVIGATOR LOGIC
                                // ==========================================
                                "Hole Navigator" -> {
                                    if (holePuzzleResults.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("No data available yet.", color = Color.Gray)
                                        }
                                    } else if (selectedHoleSession == null) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            Row(modifier = Modifier.clickable { selectedGame = "Overview Dashboard" }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Back to Overview", color = primaryColor, fontWeight = FontWeight.Bold)
                                            }
                                            Text("Select a Run Session", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
                                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                items(holePuzzleResults.sortedByDescending { it.timestamp }) { session ->
                                                    val dateString = fullDateFormatter.format(session.timestamp)
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth().clickable { selectedHoleSession = session },
                                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                                    ) {
                                                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                            Column {
                                                                Text(text = dateString, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                                                Text(text = if (session.isWin) "✅ Reached Goal" else "💥 Fell in Hole", color = if (session.isWin) Color(0xFF4CAF50) else Color.Red, fontSize = 14.sp)
                                                            }
                                                            Text(text = "Score: ${session.score}", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        val session = selectedHoleSession!!
                                        val sorted = holePuzzleResults.sortedBy { it.timestamp }
                                        val currentIndex = sorted.indexOf(session)
                                        val historyWindow = sorted.subList(maxOf(0, currentIndex - 4), currentIndex + 1)
                                        val recentData = historyWindow.mapIndexed { index, it ->
                                            val label = if (index == historyWindow.lastIndex) "Selected" else dateFormatter.format(it.timestamp)
                                            ChartData(label, it.score.toFloat())
                                        }
                                        val pb = sorted.maxOfOrNull { it.score.toFloat() } ?: 0f

                                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                            Row(modifier = Modifier.clickable {
                                                if(selectedGroupedSession != null) selectedMode = "Session"
                                                else if(selectedOverviewSession != null) selectedGame = "Overview Dashboard"
                                                selectedHoleSession = null
                                            }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(if(selectedGroupedSession != null || selectedOverviewSession != null) "Back to Session Report" else "Back to List", color = primaryColor, fontWeight = FontWeight.Bold)
                                            }

                                            PerformanceDashboard(
                                                title = "Performance Dashboard",
                                                metrics = recentData,
                                                personalBest = pb,
                                                primaryColor = primaryColor
                                            )

                                            Spacer(modifier = Modifier.height(16.dp))

                                            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                                                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                        Text(if (session.isWin) "Goal Reached!" else "Run Ended", color = if (session.isWin) Color(0xFF4CAF50) else Color.Red, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                                        Text("Score: ${session.score}", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                                    }
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text("Time Survived: ${session.timeSurvivedMs / 1000}s", color = Color.DarkGray)
                                                    Text("Obstacles Dodged: ${session.holesDodged}", color = Color.DarkGray)
                                                }
                                            }

                                            val parentSession = groupedSessions.find { it.sessionId == session.sessionId }
                                            ReportActionButtons(
                                                primaryColor = primaryColor,
                                                onShareClick = {
                                                    parentSession?.let { ClinicalReportPdfManager.generateAndSharePdf(context, patient, it, true) }
                                                        ?: Toast.makeText(context, "Session data not found", Toast.LENGTH_SHORT).show()
                                                },
                                                onDownloadClick = {
                                                    parentSession?.let { ClinicalReportPdfManager.generateAndSharePdf(context, patient, it, false) }
                                                        ?: Toast.makeText(context, "Session data not found", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                                }

                                // ==========================================
                                // STEP GAME LOGIC
                                // ==========================================
                                "Step Game" -> {
                                    if (stepGameResults.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("No data available yet.", color = Color.Gray)
                                        }
                                    } else if (selectedStepSession == null) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            Row(modifier = Modifier.clickable { selectedGame = "Overview Dashboard" }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Back to Overview", color = primaryColor, fontWeight = FontWeight.Bold)
                                            }
                                            Text("Select a Cognitive Session", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
                                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                items(stepGameResults.sortedByDescending { it.timestamp }) { session ->
                                                    val dateString = fullDateFormatter.format(session.timestamp)
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth().clickable { selectedStepSession = session },
                                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                                    ) {
                                                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                            Column {
                                                                Text(text = dateString, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                                                Text(text = "Correct Hits: ${session.correctHits}", color = Color(0xFF4CAF50), fontSize = 14.sp)
                                                            }
                                                            Text(text = "Score: ${session.score}", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        val session = selectedStepSession!!
                                        val sorted = stepGameResults.sortedBy { it.timestamp }
                                        val currentIndex = sorted.indexOf(session)
                                        val historyWindow = sorted.subList(maxOf(0, currentIndex - 4), currentIndex + 1)
                                        val recentData = historyWindow.mapIndexed { index, it ->
                                            val label = if (index == historyWindow.lastIndex) "Selected" else dateFormatter.format(it.timestamp)
                                            ChartData(label, it.score.toFloat())
                                        }
                                        val pb = sorted.maxOfOrNull { it.score.toFloat() } ?: 0f

                                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                            Row(modifier = Modifier.clickable {
                                                if(selectedGroupedSession != null) selectedMode = "Session"
                                                else if(selectedOverviewSession != null) selectedGame = "Overview Dashboard"
                                                selectedStepSession = null
                                            }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(if(selectedGroupedSession != null || selectedOverviewSession != null) "Back to Session Report" else "Back to List", color = primaryColor, fontWeight = FontWeight.Bold)
                                            }

                                            PerformanceDashboard(
                                                title = "Performance Dashboard",
                                                metrics = recentData,
                                                personalBest = pb,
                                                primaryColor = primaryColor
                                            )

                                            Spacer(modifier = Modifier.height(16.dp))

                                            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                                                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                        Text("Cognitive Score", color = Color.DarkGray, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                                        Text("${session.score}", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                                    }
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text("✅ Correct Selections: ${session.correctHits}", color = Color(0xFF4CAF50))
                                                    Text("❌ Incorrect Selections: ${session.incorrectHits}", color = Color.Red)
                                                }
                                            }

                                            val parentSession = groupedSessions.find { it.sessionId == session.sessionId }
                                            ReportActionButtons(
                                                primaryColor = primaryColor,
                                                onShareClick = {
                                                    parentSession?.let { ClinicalReportPdfManager.generateAndSharePdf(context, patient, it, true) }
                                                        ?: Toast.makeText(context, "Session data not found", Toast.LENGTH_SHORT).show()
                                                },
                                                onDownloadClick = {
                                                    parentSession?.let { ClinicalReportPdfManager.generateAndSharePdf(context, patient, it, false) }
                                                        ?: Toast.makeText(context, "Session data not found", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                                }

                                else -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(text = "Select a valid module.", color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// UNIFIED PATIENT JOURNEY DASHBOARD
// ==========================================
@Composable
fun PatientJourneyDashboard(groupedSessions: List<GroupedSession>, primaryColor: Color, fullDateFormatter: SimpleDateFormat, onSessionClick: (GroupedSession) -> Unit) {
    if (groupedSessions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Your journey begins here. Complete a session to see your progress!", color = Color.Gray)
        }
        return
    }

    // Goal Calculations (Rolling 7 days logic simulated)
    val weeklyTimeGoalSecs = 1800f // 30 minutes
    val weeklyStabilityGoal = 80f  // 80%

    // Take up to the last 5 sessions for the "current week"
    val recentSessions = groupedSessions.take(5)

    val totalTimeSecs = recentSessions.sumOf { it.totalTimeMs } / 1000f

    // Calculate an average stability score for the goals
    val avgScores = recentSessions.mapNotNull { session ->
        val latestStatic = session.staticResults.maxByOrNull { it.timestamp }
        val latestPattern = session.patternResults.maxByOrNull { it.timestamp }
        val latestShape = session.shapeResults.maxByOrNull { it.timestamp }
        val staticScore = ((latestStatic?.efficiencyPercentage?.toFloat() ?: 0f) / 100f * calculateClinicalWeight(latestStatic?.gameMode, latestStatic?.level)).coerceAtMost(1f)
        val patternScore = (if (latestPattern != null && latestPattern.totalTargets > 0) (latestPattern.targetsHit.toFloat() / latestPattern.totalTargets) else 0f * calculateClinicalWeight(latestPattern?.gameMode, latestPattern?.level)).coerceAtMost(1f)
        val shapeScore = ((latestShape?.score?.toFloat() ?: 0f) / 10f * calculateClinicalWeight(latestShape?.gameMode, latestShape?.level)).coerceAtMost(1f)
        val mods = listOfNotNull(latestStatic, latestPattern, latestShape).size
        if (mods > 0) ((staticScore + patternScore + shapeScore) / mods.toFloat()) * 100 else null
    }
    val currentStability = if (avgScores.isNotEmpty()) avgScores.average().toFloat() else 0f

    val timeProgress = (totalTimeSecs / weeklyTimeGoalSecs).coerceIn(0f, 1f)
    val stabilityProgress = (currentStability / weeklyStabilityGoal).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxSize()) {

        // --- CONCEPT 1: GOAL RINGS ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, Color(0xFFEEEEEE))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Weekly Objectives", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    GoalRing(
                        progress = timeProgress,
                        color = Color(0xFF4CAF50),
                        icon = Icons.Default.Favorite,
                        label = "Active Time",
                        valueText = "${(totalTimeSecs / 60).toInt()}m",
                        targetText = "Goal: 30m"
                    )

                    GoalRing(
                        progress = stabilityProgress,
                        color = primaryColor,
                        icon = Icons.Default.Star,
                        label = "Avg Stability",
                        valueText = "${currentStability.toInt()}%",
                        targetText = "Goal: 80%"
                    )
                }
            }
        }

        Text("Your Recovery Journey", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = primaryColor, modifier = Modifier.padding(bottom = 16.dp))

        // --- CONCEPT 2: TIMELINE ---
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            itemsIndexed(groupedSessions) { index, session ->
                val isFirst = index == 0
                val isLast = index == groupedSessions.lastIndex

                // Determine Milestone dynamically
                val milestone = when {
                    session.totalFalls == 0 && session.totalModulesPlayed > 0 -> "🏆 Flawless Balance!"
                    session.totalModulesPlayed >= 4 -> "🔥 Mega Session!"
                    else -> "✅ Session Complete"
                }

                val milestoneColor = if (session.totalFalls == 0 && session.totalModulesPlayed > 0) Color(0xFFFFC107) else primaryColor

                TimelineNode(
                    isFirst = isFirst,
                    isLast = isLast,
                    nodeColor = milestoneColor,
                    dateText = fullDateFormatter.format(session.timestamp),
                    milestoneText = milestone,
                    modulesText = "${session.totalModulesPlayed} Exercises Played",
                    onClick = { onSessionClick(session) }
                )
            }
        }
    }
}

@Composable
fun GoalRing(progress: Float, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, valueText: String, targetText: String) {
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000))

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
            CircularProgressIndicator(
                progress = 1f,
                color = Color(0xFFEEEEEE),
                strokeWidth = 10.dp,
                modifier = Modifier.fillMaxSize()
            )
            CircularProgressIndicator(
                progress = animatedProgress,
                color = color,
                strokeWidth = 10.dp,
                strokeCap = StrokeCap.Round,
                modifier = Modifier.fillMaxSize()
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                Text(valueText, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.DarkGray)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.DarkGray)
        Text(targetText, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun TimelineNode(isFirst: Boolean, isLast: Boolean, nodeColor: Color, dateText: String, milestoneText: String, modulesText: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Timeline Line & Dot
        Box(modifier = Modifier.width(40.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            if (!isLast) {
                Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.LightGray))
            }

            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(16.dp)
                    .background(nodeColor, CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            )
        }

        // Content Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, end = 8.dp)
                .clickable { onClick() },
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(dateText, fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (milestoneText.contains("Flawless") || milestoneText.contains("Mega")) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = nodeColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(milestoneText, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = nodeColor)
                }
                Text(modulesText, fontSize = 14.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

// ==========================================
// NEW: TREND CHART (Line Graph)
// ==========================================
@Composable
fun PerformanceTrendChart(scores: List<Float>, targetScore: Float = 50f, primaryColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (scores.isEmpty()) {
                Text("Performance trend", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 16.sp, modifier = Modifier.padding(bottom = 16.dp))
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("Not enough data to show trend.", color = Color.Gray)
                }
                return@Card
            }

            Box(modifier = Modifier.fillMaxWidth().height(260.dp).padding(top = 16.dp, bottom = 8.dp, end = 8.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    // Padding for axes and labels
                    val paddingLeft = 50.dp.toPx()
                    val paddingBottom = 40.dp.toPx()
                    val paddingTop = 40.dp.toPx()
                    val paddingRight = 20.dp.toPx()

                    val chartWidth = canvasWidth - paddingLeft - paddingRight
                    val chartHeight = canvasHeight - paddingBottom - paddingTop

                    // Paints for Text
                    val titlePaint = Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 18.sp.toPx()
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                        isFakeBoldText = true
                    }
                    val labelPaint = Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 14.sp.toPx()
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    val yAxisLabelPaint = Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 16.sp.toPx()
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }

                    // 1. Draw Title
                    drawContext.canvas.nativeCanvas.drawText(
                        "Performance trend",
                        canvasWidth / 2,
                        paddingTop / 2,
                        titlePaint
                    )

                    // 2. Draw Y-Axis Label (Rotated)
                    drawContext.canvas.nativeCanvas.apply {
                        save()
                        rotate(-90f, paddingLeft / 2 - 10f, canvasHeight / 2)
                        drawText("Score", paddingLeft / 2 - 10f, canvasHeight / 2, yAxisLabelPaint)
                        restore()
                    }

                    // Calculate max scale dynamically (with a minimum bounds to make it look good)
                    val maxScore = maxOf(100f, scores.maxOrNull() ?: 100f, targetScore * 1.5f)

                    // 3. Draw Grid Lines
                    val numHGrid = 6
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                    // Horizontal Grid
                    for (i in 0..numHGrid) {
                        val y = paddingTop + (chartHeight * i / numHGrid)
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.7f),
                            start = Offset(paddingLeft, y),
                            end = Offset(canvasWidth - paddingRight, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashEffect
                        )
                    }

                    val xStep = if (scores.size > 1) chartWidth / (scores.size - 1).toFloat() else chartWidth

                    // Vertical Grid & X-Axis Labels
                    for (i in 0 until scores.size) {
                        val x = paddingLeft + (i * xStep)
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.7f),
                            start = Offset(x, paddingTop),
                            end = Offset(x, canvasHeight - paddingBottom),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashEffect
                        )
                        // X-Axis Labels (S 1, S 2, etc.)
                        drawContext.canvas.nativeCanvas.drawText("S ${i + 1}", x, canvasHeight - paddingBottom + 25.dp.toPx(), labelPaint)
                    }

                    // 4. Draw Baseline (Target Score)
                    val targetY = paddingTop + chartHeight - ((targetScore / maxScore) * chartHeight)
                    drawLine(
                        color = Color.Gray,
                        start = Offset(paddingLeft, targetY),
                        end = Offset(canvasWidth - paddingRight, targetY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                    )

                    // 5. Draw Solid Axes with Arrows
                    val axisColor = Color(0xFF333333)
                    val axisStroke = 1.5.dp.toPx()

                    // Y-Axis
                    drawLine(
                        color = axisColor,
                        start = Offset(paddingLeft, paddingTop - 15f),
                        end = Offset(paddingLeft, canvasHeight - paddingBottom),
                        strokeWidth = axisStroke
                    )
                    // Y-Axis Arrow
                    val yArrow = Path().apply {
                        moveTo(paddingLeft, paddingTop - 25f)
                        lineTo(paddingLeft - 8f, paddingTop - 5f)
                        lineTo(paddingLeft + 8f, paddingTop - 5f)
                        close()
                    }
                    drawPath(yArrow, axisColor)

                    // X-Axis
                    drawLine(
                        color = axisColor,
                        start = Offset(paddingLeft, canvasHeight - paddingBottom),
                        end = Offset(canvasWidth - paddingRight + 15f, canvasHeight - paddingBottom),
                        strokeWidth = axisStroke
                    )
                    // X-Axis Arrow
                    val xArrow = Path().apply {
                        moveTo(canvasWidth - paddingRight + 25f, canvasHeight - paddingBottom)
                        lineTo(canvasWidth - paddingRight + 5f, canvasHeight - paddingBottom - 8f)
                        lineTo(canvasWidth - paddingRight + 5f, canvasHeight - paddingBottom + 8f)
                        close()
                    }
                    drawPath(xArrow, axisColor)

                    // Calculate point coordinates
                    val points = scores.mapIndexed { index, score ->
                        val x = paddingLeft + (index * xStep)
                        val y = paddingTop + chartHeight - ((score / maxScore) * chartHeight)
                        Offset(x, y)
                    }

                    // 6. Draw Connecting Line (Solid Black)
                    if (points.size > 1) {
                        val linePath = Path()
                        points.forEachIndexed { index, offset ->
                            if (index == 0) linePath.moveTo(offset.x, offset.y)
                            else linePath.lineTo(offset.x, offset.y)
                        }
                        drawPath(
                            path = linePath,
                            color = Color.Black,
                            style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round)
                        )
                    }

                    // 7. Draw Nodes (White fill, Colored stroke based on Y position)
                    points.forEach { offset ->
                        // Visual match: Lower Y (higher on screen) = Red, Middle = Orange, Lower = Green
                        val nodeColor = when {
                            offset.y < targetY - 20f -> Color(0xFFD32F2F) // Red (Worse / High)
                            offset.y > targetY + 20f -> Color(0xFF388E3C) // Green (Better / Low)
                            else -> Color(0xFFFBC02D)                     // Orange/Yellow (Near Target)
                        }

                        // Inner white circle
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = offset
                        )
                        // Thick colored outer ring
                        drawCircle(
                            color = nodeColor,
                            radius = 6.dp.toPx(),
                            center = offset,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// EXISTING HELPER COMPONENTS
// ==========================================

@Composable
fun ReportActionButtons(
    primaryColor: Color,
    onShareClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(
            onClick = onShareClick,
            modifier = Modifier.weight(1f),
            border = BorderStroke(1.dp, primaryColor),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
        ) {
            Text("Share", fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = onDownloadClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
        ) {
            Text("Download PDF", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BreakdownRow(title: String, plays: Int, timeStr: String, scoreStr: String, color: Color, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 8.dp, horizontal = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 14.sp, modifier = Modifier.weight(2f))
            Text("$plays Plays", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
            Text(timeStr, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
            Text(scoreStr, fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
            Icon(Icons.Default.ArrowForward, contentDescription = "View", tint = Color.LightGray, modifier = Modifier.size(16.dp).padding(start = 4.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color(0xFFEEEEEE))
    }
}

@Composable
fun InfoSection(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.Gray, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontSize = 12.sp, color = Color.DarkGray)
    }
}

@Composable
fun GameMiniCard(title: String, scoreText: String, subText: String, isSuccess: Boolean = true, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 16.sp)
                Text(text = subText, color = Color.Gray, fontSize = 12.sp)
            }
            Text(
                text = scoreText,
                fontWeight = FontWeight.Bold,
                color = if(isSuccess) Color(0xFF4A44D4) else Color.Red,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun ClinicalRadarChart(currentScores: List<Float>, prevScores: List<Float>, labels: List<String>, primaryColor: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = size.width / 2.5f

        val n = labels.size.coerceAtLeast(3)
        for (i in 1..4) {
            val r = maxRadius * (i / 4f)
            drawCircle(color = Color.LightGray.copy(alpha = 0.5f), radius = r, center = Offset(centerX, centerY), style = Stroke(width = 1.dp.toPx()))
        }

        val angles = List(n) { i -> -Math.PI / 2 + (2 * Math.PI * i / n).toFloat() }

        val textPaint = Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 36f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        angles.forEachIndexed { index, angle ->
            val endX = centerX + (maxRadius * 1.2f * cos(angle)).toFloat()
            val endY = centerY + (maxRadius * 1.2f * sin(angle)).toFloat()
            drawLine(color = Color.LightGray, start = Offset(centerX, centerY), end = Offset(endX, endY), strokeWidth = 2f)

            val labelX = centerX + (maxRadius * 1.3f * cos(angle)).toFloat()
            val labelY = centerY + (maxRadius * 1.3f * sin(angle)).toFloat()
            drawContext.canvas.nativeCanvas.drawText(labels[index], labelX, labelY + 12f, textPaint)
        }

        fun buildRadarPath(scores: List<Float>): Path {
            val path = Path()
            scores.forEachIndexed { index, score ->
                val r = maxRadius * score.coerceIn(0f, 1f)
                val x = centerX + (r * cos(angles[index])).toFloat()
                val y = centerY + (r * sin(angles[index])).toFloat()
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            return path
        }

        if (prevScores.isNotEmpty() && prevScores.any { it > 0f }) {
            val prevPath = buildRadarPath(prevScores)
            drawPath(path = prevPath, color = Color.Gray.copy(alpha = 0.2f))
            drawPath(path = prevPath, color = Color.Gray.copy(alpha = 0.8f), style = Stroke(width = 4f))
        }

        if (currentScores.isNotEmpty() && currentScores.any { it > 0f }) {
            val currPath = buildRadarPath(currentScores)
            drawPath(path = currPath, color = primaryColor.copy(alpha = 0.3f))
            drawPath(path = currPath, color = primaryColor, style = Stroke(width = 6f, join = StrokeJoin.Round))

            currentScores.forEachIndexed { index, score ->
                val r = maxRadius * score.coerceIn(0f, 1f)
                val x = centerX + (r * cos(angles[index])).toFloat()
                val y = centerY + (r * sin(angles[index])).toFloat()
                drawCircle(color = primaryColor, radius = 12f, center = Offset(x, y))
            }
        }
    }
}

@Composable
fun CombinedHeatmap(staticResult: StaticBalanceResult?, patternResult: PatternDrawingResult?, shapeResult: ShapeTrainingResult?) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val outerRadius = size.width / 2.2f

        drawCircle(color = Color.LightGray.copy(alpha = 0.3f), radius = outerRadius, center = Offset(centerX, centerY), style = Stroke(width = 2f))
        drawLine(color = Color.LightGray, start = Offset(centerX - outerRadius, centerY), end = Offset(centerX + outerRadius, centerY))
        drawLine(color = Color.LightGray, start = Offset(centerX, centerY - outerRadius), end = Offset(centerX, centerY + outerRadius))
        drawCircle(color = Color.DarkGray, radius = 6f, center = Offset(centerX, centerY))

        val globalMaxTilt = 16f

        fun plotPoints(frontal: List<Float>, sagittal: List<Float>, pointColor: Color) {
            val sizeToUse = minOf(frontal.size, sagittal.size)
            for (i in 0 until sizeToUse) {
                val pitch = frontal[i]
                val yaw = sagittal[i]

                val rawX = (yaw / globalMaxTilt) * outerRadius
                val rawY = -(pitch / globalMaxTilt) * outerRadius
                val dist = sqrt(rawX * rawX + rawY * rawY)
                val scale = if (dist > outerRadius) outerRadius / dist else 1f

                val px = centerX + (rawX * scale)
                val py = centerY + (rawY * scale)

                drawCircle(color = pointColor.copy(alpha = 0.15f), radius = 6f, center = Offset(px, py))
            }
        }

        if (staticResult != null) plotPoints(staticResult.frontalData, staticResult.sagittalData, Color(0xFF188B97))
        if (patternResult != null) plotPoints(patternResult.frontalData, patternResult.sagittalData, Color(0xFF4CAF50))
        if (shapeResult != null) plotPoints(shapeResult.frontalData, shapeResult.sagittalData, Color(0xFFE53935))
    }
}

@Composable
fun HeatmapLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 12.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SessionSummaryBanner(dateString: String, posture: String?, level: Int?) {
    val hasPosture = posture != null && posture != "Unknown"
    val isStanding = posture?.equals("STANDING", ignoreCase = true) == true

    val bgColor = if (hasPosture) {
        if (isStanding) Color(0xFFE3F2FD) else Color(0xFFFFF3E0)
    } else {
        Color(0xFFF3E5F5)
    }

    val textColor = if (hasPosture) {
        if (isStanding) Color(0xFF1565C0) else Color(0xFFE65100)
    } else {
        Color(0xFF6A1B9A)
    }

    val levelText = if (hasPosture && !posture!!.equals("SITTING", ignoreCase = true)) " • LVL $level" else ""

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Session Info", color = textColor.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(dateString, color = Color.DarkGray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            if (hasPosture) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Posture & Level", color = textColor.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${posture!!.uppercase()}$levelText", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Session Mode", color = textColor.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("GAMIFICATION", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun ClinicalComparisonCard(
    title: String,
    currentValue: Float,
    previousValue: Float?,
    unit: String,
    isLowerBetter: Boolean,
    primaryColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Previous", color = Color.Gray, fontSize = 12.sp)
                    Text(
                        text = previousValue?.let { if (it % 1 == 0f) it.toInt().toString() else String.format("%.1f", it) } ?: "-",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.Gray
                    )
                    Text(unit, color = Color.Gray, fontSize = 10.sp)
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "To",
                    tint = Color.LightGray,
                    modifier = Modifier.size(24.dp)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Current", color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (currentValue % 1 == 0f) currentValue.toInt().toString() else String.format("%.1f", currentValue),
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = primaryColor
                    )
                    Text(unit, color = primaryColor, fontSize = 10.sp)
                }
            }

            if (previousValue != null) {
                val diff = currentValue - previousValue
                if (diff != 0f) {
                    val isImprovement = if (isLowerBetter) diff < 0 else diff > 0
                    val statusColor = if (isImprovement) Color(0xFF4CAF50) else Color(0xFFE53935)
                    val statusText = if (isImprovement) "Improved" else "Declined"
                    val symbol = if (diff > 0) "▲" else "▼"

                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$symbol ${abs(diff).let { if (it % 1 == 0f) it.toInt().toString() else String.format("%.1f", it) }} $unit ($statusText)",
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No change from previous session", color = Color.DarkGray, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ClinicalErrorBarChart(title: String, errors: List<Float>, barLabelPrefix: String = "T", baseLimit: Float? = null) {
    if (errors.isEmpty()) return

    val maxError = errors.maxOrNull() ?: 0f
    val maxTotal = if (baseLimit != null) baseLimit + maxError else maxError
    val chartTop = (maxTotal * 1.2f).coerceAtLeast(15f)

    Card(
        modifier = Modifier.fillMaxWidth().height(260.dp).padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            Text(title, fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            if (baseLimit != null) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(Color(0xFF188B97).copy(alpha = 0.3f)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tolerance Limit", fontSize = 10.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(Color(0xFFF44336)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Out of Bounds", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxHeight().padding(bottom = 20.dp, end = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text("${chartTop.toInt()}°", fontSize = 10.sp, color = Color.Gray)
                    Text("${(chartTop / 2).toInt()}°", fontSize = 10.sp, color = Color.Gray)
                    Text("0°", fontSize = 10.sp, color = Color.Gray)
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Column(modifier = Modifier.fillMaxSize().padding(bottom = 20.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        HorizontalDivider(color = Color.LightGray)
                    }

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        errors.forEachIndexed { index, err ->
                            val barColor = if (baseLimit != null) {
                                Color(0xFFF44336)
                            } else {
                                when {
                                    err <= 10f -> Color(0xFF4CAF50)
                                    err <= 20f -> Color(0xFFFFC107)
                                    else -> Color(0xFFF44336)
                                }
                            }
                            Column(
                                modifier = Modifier.fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Canvas(modifier = Modifier.width(20.dp).weight(1f)) {
                                    val bottomPx = size.height
                                    val limitHeightPx = if (baseLimit != null) (baseLimit / chartTop) * size.height else 0f
                                    val errHeightPx = (err / chartTop) * size.height

                                    if (baseLimit != null) {
                                        drawRect(
                                            color = Color(0xFF188B97).copy(alpha = 0.3f),
                                            topLeft = Offset(0f, bottomPx - limitHeightPx),
                                            size = Size(size.width, limitHeightPx)
                                        )
                                    }

                                    val cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    drawRoundRect(
                                        color = barColor,
                                        topLeft = Offset(0f, bottomPx - limitHeightPx - errHeightPx),
                                        size = Size(size.width, errHeightPx),
                                        cornerRadius = cornerRadius
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$barLabelPrefix${index + 1}", fontSize = 10.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PerformanceDashboard(
    title: String,
    metrics: List<ChartData>,
    personalBest: Float,
    isLowerBetter: Boolean = false,
    primaryColor: Color
) {
    if (metrics.isEmpty()) return

    val latest = metrics.last().value
    val previous = if (metrics.size > 1) metrics[metrics.size - 2].value else latest

    val diff = if (isLowerBetter) previous - latest else latest - previous
    val improvementStr = if (diff >= 0) "+${String.format("%.1f", diff)}" else String.format("%.1f", diff)
    val diffColor = if (diff >= 0) Color(0xFF4CAF50) else Color(0xFFE53935)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("SCORE", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(if (title.contains("Avg Error") || title.contains("Fall Error")) String.format("%.1f", latest) else "${latest.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("PERSONAL BEST", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(if (title.contains("Avg Error") || title.contains("Fall Error")) String.format("%.1f", personalBest) else "${personalBest.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("VS LAST", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(improvementStr, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = diffColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                    val progressRatio = if (isLowerBetter) {
                        if (latest > 0) (personalBest / latest).coerceIn(0f, 1f) else 1f
                    } else {
                        if (personalBest > 0) (latest / personalBest).coerceIn(0f, 1f) else 1f
                    }

                    CircularProgressIndicator(
                        progress = progressRatio,
                        strokeWidth = 10.dp,
                        color = primaryColor,
                        trackColor = Color(0xFFEEEEEE),
                        modifier = Modifier.fillMaxSize(),
                        strokeCap = StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (title.contains("Avg Error") || title.contains("Fall Error")) String.format("%.1f", latest) else "${latest.toInt()}", fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Color.DarkGray)
                        Text("CURRENT", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color(0xFFF9F9F9), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val chartTop = if (isLowerBetter) {
                    (metrics.maxOfOrNull { it.value } ?: 10f) * 1.2f
                } else {
                    maxOf(personalBest, metrics.maxOfOrNull { it.value } ?: 0f).coerceAtLeast(10f) * 1.2f
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    metrics.forEachIndexed { index, data ->
                        val isLatest = index == metrics.lastIndex
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .fillMaxHeight((if (chartTop > 0) data.value / chartTop else 0f).coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(if (isLatest) primaryColor else primaryColor.copy(alpha = 0.4f))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = data.label,
                                fontSize = 10.sp,
                                color = if (isLatest) primaryColor else Color.Gray,
                                fontWeight = if (isLatest) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricSelector(metrics: List<String>, selectedMetric: String, onMetricSelected: (String) -> Unit, primaryColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        metrics.forEach { metric ->
            FilterChip(
                selected = selectedMetric == metric,
                onClick = { onMetricSelected(metric) },
                label = { Text(metric) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = primaryColor.copy(alpha = 0.2f),
                    selectedLabelColor = primaryColor
                )
            )
        }
    }
}

@Composable
fun GraphTopBar(
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    primaryColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clickable { onBackClick() }
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = primaryColor,
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = "Progress Dashboard",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = primaryColor
        )

        Spacer(modifier = Modifier.width(32.dp))
    }
}

@Composable
fun SegmentedToggle(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    primaryColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) primaryColor else Color.Transparent)
                    .clickable { onOptionSelected(option) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    color = if (isSelected) Color.White else primaryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}