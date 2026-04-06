package com.ripplehealthcare.bproboard.ui.screens

import android.graphics.Paint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ripplehealthcare.bproboard.domain.model.*
import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class ChartData(val label: String, val value: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphViewScreen(
    navController: NavController,
    testViewModel: TestViewModel
) {
    val patient by testViewModel.patient.collectAsState()
    val staticBalanceResults by testViewModel.staticBalanceResults.collectAsState()
    val patternDrawingResults by testViewModel.patternDrawingResults.collectAsState()
    val shapeTrainingResults by testViewModel.shapeTrainingResults.collectAsState()
    val colorSorterResults by testViewModel.colorSorterResults.collectAsState()
    val ratPuzzleResults by testViewModel.ratPuzzleResults.collectAsState()
    val starshipResults by testViewModel.starshipResults.collectAsState()
    val holePuzzleResults by testViewModel.holePuzzleResults.collectAsState()
    val stepGameResults by testViewModel.stepGameResults.collectAsState()

    var selectedMode by remember { mutableStateOf("Training") }
    var selectedGame by remember { mutableStateOf("Overview Dashboard") }
    var selectedMetric by remember { mutableStateOf("Efficiency") }

    var selectedStaticSession by remember { mutableStateOf<StaticBalanceResult?>(null) }
    var selectedPatternSession by remember { mutableStateOf<PatternDrawingResult?>(null) }
    var selectedShapeSession by remember { mutableStateOf<ShapeTrainingResult?>(null) }
    var selectedColorSorterSession by remember { mutableStateOf<ColorSorterResult?>(null) }
    var selectedRatSession by remember { mutableStateOf<RatPuzzleResult?>(null) }
    var selectedStarshipSession by remember { mutableStateOf<StarshipResult?>(null) }
    var selectedHoleSession by remember { mutableStateOf<HolePuzzleResult?>(null) }
    var selectedStepSession by remember { mutableStateOf<StepGameResult?>(null) }

    val gameList = listOf("Color Sorter", "Rat Puzzle", "Starship Defender", "Hole Navigator", "Step Game")
    val trainingList = listOf("Overview Dashboard", "Static Balance", "Pattern Drawing", "Shape Training")
    val currentList = if (selectedMode == "Game") gameList else trainingList

    val primaryColor = Color(0xFF4A44D4)
    val lightPrimary = Color(0xFF8C9EFF)
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFE0F7FA), Color(0xFFB2EBF2))
    )
    val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

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

    // Tools for drawing clinical annotation text on Canvas
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
        if (!currentList.contains(selectedGame)) {
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

    LaunchedEffect(selectedGame) {
        selectedStaticSession = null
        selectedPatternSession = null
        selectedShapeSession = null
        selectedColorSorterSession = null
        selectedRatSession = null
        selectedStarshipSession = null
        selectedHoleSession = null
        selectedStepSession = null

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
                // Mode Toggle (Game / Training)
                Box(modifier = Modifier.width(300.dp)) {
                    SegmentedToggle(
                        options = listOf("Game", "Training"),
                        selectedOption = selectedMode,
                        onOptionSelected = { selectedMode = it },
                        primaryColor = primaryColor
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Module Selector Dropdown
                GameSelectorDropdown(
                    items = currentList,
                    selectedItem = selectedGame,
                    onItemSelected = { selectedGame = it },
                    primaryColor = primaryColor
                )
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
                        when (selectedGame) {
                            // ==========================================
                            // OVERVIEW DASHBOARD LOGIC
                            // ==========================================
                            "Overview Dashboard" -> {
                                val latestStatic = staticBalanceResults.maxByOrNull { it.timestamp }
                                val latestPattern = patternDrawingResults.maxByOrNull { it.timestamp }
                                val latestShape = shapeTrainingResults.maxByOrNull { it.timestamp }

                                if (latestStatic == null && latestPattern == null && latestShape == null) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("No training data available yet to generate a report.", color = Color.Gray)
                                    }
                                } else {
                                    val sortedStatic = staticBalanceResults.sortedByDescending { it.timestamp }
                                    val prevStatic = if (sortedStatic.size > 1) sortedStatic[1] else null

                                    val sortedPattern = patternDrawingResults.sortedByDescending { it.timestamp }
                                    val prevPattern = if (sortedPattern.size > 1) sortedPattern[1] else null

                                    val sortedShape = shapeTrainingResults.sortedByDescending { it.timestamp }
                                    val prevShape = if (sortedShape.size > 1) sortedShape[1] else null

                                    val currentFalls = (latestStatic?.fallCount ?: 0) + (latestPattern?.fallCount ?: 0) + (latestShape?.fallCount ?: 0)
                                    val prevFalls = (prevStatic?.fallCount ?: 0) + (prevPattern?.fallCount ?: 0) + (prevShape?.fallCount ?: 0)

                                    val staticScore = (latestStatic?.efficiencyPercentage?.toFloat() ?: 0f) / 100f
                                    val patternScore = if (latestPattern != null && latestPattern.totalTargets > 0) (latestPattern.targetsHit.toFloat() / latestPattern.totalTargets) else 0f
                                    val shapeScore = (latestShape?.score?.toFloat() ?: 0f) / 10f

                                    val prevStaticScore = (prevStatic?.efficiencyPercentage?.toFloat() ?: 0f) / 100f
                                    val prevPatternScore = if (prevPattern != null && prevPattern.totalTargets > 0) (prevPattern.targetsHit.toFloat() / prevPattern.totalTargets) else 0f
                                    val prevShapeScore = (prevShape?.score?.toFloat() ?: 0f) / 10f

                                    val globalScore = ((staticScore + patternScore + shapeScore.coerceAtMost(1f)) / 3f) * 100
                                    val prevGlobalScore = ((prevStaticScore + prevPatternScore + prevShapeScore.coerceAtMost(1f)) / 3f) * 100

                                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                        Text("Global Stability Report", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))

                                        ClinicalComparisonCard(
                                            title = "FRST Composite Stability Score",
                                            currentValue = globalScore,
                                            previousValue = prevGlobalScore,
                                            unit = "/ 100",
                                            isLowerBetter = false,
                                            primaryColor = primaryColor
                                        )

                                        ClinicalComparisonCard(
                                            title = "Combined Fall Risk (Total Incidents)",
                                            currentValue = currentFalls.toFloat(),
                                            previousValue = prevFalls.toFloat(),
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
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(16.dp), contentAlignment = Alignment.Center) {
                                                    ClinicalRadarChart(
                                                        currentScores = listOf(staticScore, patternScore, shapeScore.coerceAtMost(1f)),
                                                        prevScores = listOf(prevStaticScore, prevPatternScore, prevShapeScore.coerceAtMost(1f)),
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
                                                Text("Overlay of Patient's Center of Mass across all recent training modules.", fontSize = 12.sp, color = Color.Gray)
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
                                        Text("Select a Session to View Graphs", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))

                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items(staticBalanceResults.sortedByDescending { it.timestamp }) { session ->
                                                val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
                                                val dateString = dateFormat.format(session.timestamp)

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
                                    val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
                                    val dateString = dateFormat.format(session.timestamp)

                                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                        Row(modifier = Modifier.clickable { selectedStaticSession = null }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Back to Sessions", color = primaryColor, fontWeight = FontWeight.Bold)
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
                                        Text("Select a Pattern Session", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items(patternDrawingResults.sortedByDescending { it.timestamp }) { session ->
                                                val dateString = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(session.timestamp)
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
                                    val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
                                    val dateString = dateFormat.format(session.timestamp)

                                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                        Row(modifier = Modifier.clickable { selectedPatternSession = null }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Back to Sessions", color = primaryColor, fontWeight = FontWeight.Bold)
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
                                        Text("Select a Shape Training Session", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items(shapeTrainingResults.sortedByDescending { it.timestamp }) { session ->
                                                val dateString = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(session.timestamp)
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
                                    val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
                                    val dateString = dateFormat.format(session.timestamp)

                                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                        Row(modifier = Modifier.clickable { selectedShapeSession = null }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Back to Sessions", color = primaryColor, fontWeight = FontWeight.Bold)
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
                                        Text("Select a Color Sorter Session", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items(colorSorterResults.sortedByDescending { it.timestamp }) { session ->
                                                val dateString = java.text.SimpleDateFormat("MMM dd, yyyy - hh:mm a", java.util.Locale.getDefault()).format(session.timestamp)
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
                                        Row(modifier = Modifier.clickable { selectedColorSorterSession = null }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Back to Sessions", color = primaryColor, fontWeight = FontWeight.Bold)
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
                                        Text("Select a Maze Session", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items(ratPuzzleResults.sortedByDescending { it.timestamp }) { session ->
                                                val dateString = java.text.SimpleDateFormat("MMM dd, yyyy - hh:mm a", java.util.Locale.getDefault()).format(session.timestamp)
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
                                        Row(modifier = Modifier.clickable { selectedRatSession = null }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Back to Sessions", color = primaryColor, fontWeight = FontWeight.Bold)
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
                                        Text("Select a Mission Log", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items(starshipResults.sortedByDescending { it.timestamp }) { session ->
                                                val dateString = java.text.SimpleDateFormat("MMM dd, yyyy - hh:mm a", java.util.Locale.getDefault()).format(session.timestamp)
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
                                        Row(modifier = Modifier.clickable { selectedStarshipSession = null }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Back to Sessions", color = primaryColor, fontWeight = FontWeight.Bold)
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
                                        Text("Select a Run Session", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items(holePuzzleResults.sortedByDescending { it.timestamp }) { session ->
                                                val dateString = java.text.SimpleDateFormat("MMM dd, yyyy - hh:mm a", java.util.Locale.getDefault()).format(session.timestamp)
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
                                        Row(modifier = Modifier.clickable { selectedHoleSession = null }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Back to Sessions", color = primaryColor, fontWeight = FontWeight.Bold)
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
                                        Text("Select a Cognitive Session", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items(stepGameResults.sortedByDescending { it.timestamp }) { session ->
                                                val dateString = java.text.SimpleDateFormat("MMM dd, yyyy - hh:mm a", java.util.Locale.getDefault()).format(session.timestamp)
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
                                        Row(modifier = Modifier.clickable { selectedStepSession = null }.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Back to Sessions", color = primaryColor, fontWeight = FontWeight.Bold)
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

// ==========================================
// NEW OVERVIEW COMPONENTS
// ==========================================

@Composable
fun ClinicalRadarChart(currentScores: List<Float>, prevScores: List<Float>, labels: List<String>, primaryColor: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = size.width / 2.5f

        // Draw grid
        for (i in 1..4) {
            val r = maxRadius * (i / 4f)
            drawCircle(color = Color.LightGray.copy(alpha = 0.5f), radius = r, center = Offset(centerX, centerY), style = Stroke(width = 1.dp.toPx()))
        }

        // Draw axes & labels
        val angles = listOf(-Math.PI / 2, Math.PI / 6, 5 * Math.PI / 6) // Top, Bottom Right, Bottom Left

        val textPaint = Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 36f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        angles.forEachIndexed { index, angle ->
            val endX = centerX + (maxRadius * cos(angle)).toFloat()
            val endY = centerY + (maxRadius * sin(angle)).toFloat()
            drawLine(color = Color.LightGray, start = Offset(centerX, centerY), end = Offset(endX, endY), strokeWidth = 2f)

            // Draw Label slightly outside
            val labelX = centerX + (maxRadius * 1.2f * cos(angle)).toFloat()
            val labelY = centerY + (maxRadius * 1.2f * sin(angle)).toFloat()
            drawContext.canvas.nativeCanvas.drawText(labels[index], labelX, labelY + 12f, textPaint)
        }

        // Helper to build path
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

        // Draw Previous (Gray)
        if (prevScores.isNotEmpty() && prevScores.any { it > 0f }) {
            val prevPath = buildRadarPath(prevScores)
            drawPath(path = prevPath, color = Color.Gray.copy(alpha = 0.2f))
            drawPath(path = prevPath, color = Color.Gray.copy(alpha = 0.8f), style = Stroke(width = 4f))
        }

        // Draw Current (Primary Color)
        if (currentScores.isNotEmpty() && currentScores.any { it > 0f }) {
            val currPath = buildRadarPath(currentScores)
            drawPath(path = currPath, color = primaryColor.copy(alpha = 0.3f))
            drawPath(path = currPath, color = primaryColor, style = Stroke(width = 6f, join = StrokeJoin.Round))

            // Dots
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

        // Background limits
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

// ==========================================
// DASHBOARD COMPONENTS
// ==========================================

@Composable
fun GameSelectorDropdown(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    primaryColor: Color
) {
    var expanded by remember { mutableStateOf(false) }

    Box(contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { expanded = true },
            color = primaryColor.copy(alpha = 0.2f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = selectedItem,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Select Module",
                    tint = primaryColor
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item,
                            color = if (item == selectedItem) primaryColor else Color.DarkGray,
                            fontWeight = if (item == selectedItem) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SessionSummaryBanner(dateString: String, posture: String, level: Int) {
    val isStanding = posture.equals("STANDING", ignoreCase = true)
    val bgColor = if (isStanding) Color(0xFFE3F2FD) else Color(0xFFFFF3E0)
    val textColor = if (isStanding) Color(0xFF1565C0) else Color(0xFFE65100)

    val levelText = if (posture.equals("SITTING", ignoreCase = true)) "" else " • LVL $level"

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
            Column(horizontalAlignment = Alignment.End) {
                Text("Posture & Level", color = textColor.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("${posture.uppercase()}$levelText", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
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
                // Y-Axis labels
                Column(
                    modifier = Modifier.fillMaxHeight().padding(bottom = 20.dp, end = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text("${chartTop.toInt()}°", fontSize = 10.sp, color = Color.Gray)
                    Text("${(chartTop / 2).toInt()}°", fontSize = 10.sp, color = Color.Gray)
                    Text("0°", fontSize = 10.sp, color = Color.Gray)
                }

                // Bars and X-Axis
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

@Composable
fun MiniLineGraph(data: List<Float>, lineColor: Color) {
    if (data.isEmpty()) {
        Text("No data points recorded", fontSize = 10.sp, color = Color.LightGray, modifier = Modifier.padding(4.dp))
        return
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        val max = data.maxOrNull() ?: 0f
        val min = data.minOrNull() ?: 0f
        val range = (max - min).coerceAtLeast(0.1f)

        val path = Path()
        val stepX = size.width / (data.size.coerceAtLeast(2) - 1)

        data.forEachIndexed { index, value ->
            val x = index * stepX
            val y = size.height - ((value - min) / range * size.height)
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}