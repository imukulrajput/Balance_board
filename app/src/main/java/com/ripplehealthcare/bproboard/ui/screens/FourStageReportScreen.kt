package com.ripplehealthcare.bproboard.ui.screens

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ripplehealthcare.bproboard.domain.model.AccPoint
import com.ripplehealthcare.bproboard.domain.model.SwayPoint
import com.ripplehealthcare.bproboard.ui.components.TopBar
import com.ripplehealthcare.bproboard.ui.theme.PrimaryColor
import com.ripplehealthcare.bproboard.ui.theme.WhiteColor
import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel
import com.ripplehealthcare.bproboard.utils.BalanceScoreCalculator
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

// Configuration
private const val SAFE_ZONE_ANGLE = 5f
private const val WARNING_ZONE_ANGLE = 12f
private const val MAX_TILT_ANGLE = 15f
private const val ACC_REPORT_RANGE = 8.0f // Graph scale +/- 8g

@Composable
fun FourStageReportScreen(
    navController: NavController,
    testViewModel: TestViewModel
) {
    // 1. Observe the SELECTED REPORT instead of live map
    val selectedReport by testViewModel.selectedReport.collectAsState()

    // 2. Convert List back to Map for easier index access (Stage 1, Stage 2...)
    val stageResultsMap = remember(selectedReport) {
        selectedReport?.stages?.associateBy { it.stageNumber } ?: emptyMap()
    }

    val fourStageScore = remember(selectedReport) {
        selectedReport?.let { report ->
            BalanceScoreCalculator.calculateFourStageScore(
                report.stages.associateBy { it.stageNumber }
            )
        } ?: 0.0
    }

    var selectedStageIndex by remember { mutableIntStateOf(0) }
    var selectedSensorTab by remember { mutableIntStateOf(0) }

    val tabs = listOf("Stage 1", "Stage 2", "Stage 3", "Stage 4")
    val sensorTabs = listOf("Waist", "Left Leg", "Right Leg")
    val stageTitles = listOf("Feet Together Stand", "Semi-Tandem Stand", "Tandem Stand", "One Leg Stand")

    Scaffold(
        topBar = { TopBar(title = "Test Report", onBackClick = { navController.popBackStack() }) },
        containerColor = Color(0xFFF8F9FA)
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {

            OverallScoreCard(title= "Four Stage Score", score = fourStageScore.toInt())
            Spacer(modifier = Modifier.height(16.dp))
            // --- Tabs ---
            TabRow(
                selectedTabIndex = selectedStageIndex,
                containerColor = WhiteColor,
                contentColor = PrimaryColor,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedStageIndex]),
                        color = PrimaryColor
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedStageIndex == index,
                        onClick = { selectedStageIndex = index },
                        text = { Text(title) },
                    )
                }
            }

            // --- Content ---
            val currentStageId = selectedStageIndex + 1
            // 3. Use the converted map here
            val data = stageResultsMap[currentStageId]

            if (data == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No data available for ${tabs[selectedStageIndex]}", color = Color.Gray)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stageTitles[selectedStageIndex],
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- VISUALS (Reusing your existing components) ---

                    Text("Waist Sway", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    ReportCanvas(dataPoints = data.pointsCenter)
                    MetricsSection(dataPoints = data.pointsCenter)

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Leg Sway", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Left Leg", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Box(Modifier.aspectRatio(1f)) { ReportCanvas(dataPoints = data.pointsLeft) }
                        }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Right Leg", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Box(Modifier.aspectRatio(1f)) { ReportCanvas(dataPoints = data.pointsRight) }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(24.dp))

                    // --- ACCELERATION ---
                    Text("Acceleration Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())

                    TabRow(selectedTabIndex = selectedSensorTab, containerColor = Color.Transparent) {
                        sensorTabs.forEachIndexed { index, title ->
                            Tab(selected = selectedSensorTab == index, onClick = { selectedSensorTab = index }, text = { Text(title) })
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val accDataToShow = when(selectedSensorTab) {
                        0 -> data.accelerationCenter
                        1 -> data.accelerationLeft
                        else -> data.accelerationRight
                    }

                    if (accDataToShow.isNotEmpty()) {
                        AccelerationReportChart(accPoints = accDataToShow)
                        Spacer(modifier = Modifier.height(16.dp))
                        AccelerationStatsGrid(accPoints = accDataToShow)
                    } else {
                        Box(Modifier.fillMaxWidth().height(100.dp), Alignment.Center) {
                            Text("No acceleration data", color = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

// --- UPDATED REPORT CANVAS (Using SwayPoint) ---
@Composable
fun ReportCanvas(dataPoints: List<SwayPoint>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Ensures it's always a square
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, Color.LightGray, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val centerOffset = Offset(centerX, centerY)
            val maxRadius = min(centerX, centerY)
            val scalePixelsPerDegree = maxRadius / MAX_TILT_ANGLE

            // Zones
            drawCircle(Color(0xFFFFCDD2), radius = maxRadius, center = centerOffset)
            drawCircle(Color(0xFFFFF9C4), radius = maxRadius * (WARNING_ZONE_ANGLE / MAX_TILT_ANGLE), center = centerOffset)
            drawCircle(Color(0xFFC8E6C9), radius = maxRadius * (SAFE_ZONE_ANGLE / MAX_TILT_ANGLE), center = centerOffset)

            // Crosshair
            drawLine(Color.Gray.copy(0.5f), Offset(centerX, centerY - maxRadius), Offset(centerX, centerY + maxRadius), 2f)
            drawLine(Color.Gray.copy(0.5f), Offset(centerX - maxRadius, centerY), Offset(centerX + maxRadius, centerY), 2f)

            if (dataPoints.isNotEmpty()) {
                val path = Path()

                // --- HELPER: Clamp points to stay inside the circle ---
                fun getClampedOffset(point: SwayPoint): Offset {
                    val magnitude = sqrt(point.x * point.x + point.y * point.y)

                    // If magnitude > MAX_TILT_ANGLE, scale it down to exactly MAX_TILT_ANGLE
                    val scaleFactor = if (magnitude > MAX_TILT_ANGLE && magnitude > 0) {
                        MAX_TILT_ANGLE / magnitude
                    } else {
                        1.0f
                    }

                    val clampedX = point.x * scaleFactor
                    val clampedY = point.y * scaleFactor

                    return Offset(
                        x = centerX + (clampedX * scalePixelsPerDegree),
                        y = centerY - (clampedY * scalePixelsPerDegree)
                    )
                }

                // Draw Path using Clamped Offsets
                dataPoints.forEachIndexed { index, point ->
                    val offset = getClampedOffset(point)
                    if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
                }

                drawPath(
                    path = path,
                    color = Color.Blue,
                    style = Stroke(width = 4.dp.toPx(), pathEffect = PathEffect.cornerPathEffect(10f))
                )

                // Start/End dots (also clamped)
                val startOffset = getClampedOffset(dataPoints.first())
                val endOffset = getClampedOffset(dataPoints.last())

                drawCircle(Color.Green, 6.dp.toPx(), startOffset)
                drawCircle(Color.Red, 6.dp.toPx(), endOffset)
            }
        }

        // Canvas Legend
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(Color.Green, CircleShape))
                Spacer(Modifier.width(4.dp))
                Text("Start", fontSize = 10.sp)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(Color.Red, CircleShape))
                Spacer(Modifier.width(4.dp))
                Text("End", fontSize = 10.sp)
            }
        }
    }
}

// --- METRICS SECTION (Updated for SwayPoint) ---
@Composable
fun MetricsSection(dataPoints: List<SwayPoint>) {
    if (dataPoints.isEmpty()) return

    val maxXTilt = dataPoints.maxOfOrNull { abs(it.x) } ?: 0f
    val maxYTilt = dataPoints.maxOfOrNull { abs(it.y) } ?: 0f
    val maxOverallTilt = dataPoints.maxOfOrNull { sqrt((it.x * it.x) + (it.y * it.y)) } ?: 0f

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Max X-Tilt", "%.1f°".format(maxXTilt), Modifier.weight(1f), Color(0xFFE3F2FD))
            MetricCard("Max Y-Tilt", "%.1f°".format(maxYTilt), Modifier.weight(1f), Color(0xFFF3E5F5))
        }
        Spacer(Modifier.height(8.dp))
        MetricCard("Max Overall Tilt", "%.1f°".format(maxOverallTilt), Modifier.fillMaxWidth(), if(maxOverallTilt > WARNING_ZONE_ANGLE) Color(0xFFFFEBEE) else Color(0xFFE8F5E9))
    }
}

@Composable
fun MetricCard(title: String, value: String, modifier: Modifier = Modifier, color: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

// --- ACCELERATION CHART (Reused) ---
@Composable
fun AccelerationReportChart(accPoints: List<AccPoint>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem("X (Left-Right)", Color.Red)
                LegendItem("Y (Up-Down)", Color.Green)
                LegendItem("Z (Forward-Backward)", Color.Blue)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
            ) {
                val width = size.width
                val height = size.height
                val midHeight = height / 2f
                val labelPadding = 35.dp.toPx()
                val graphWidth = width - labelPadding
                val labels = listOf(6 ,4, 2, 0, -2, -4, -6)

                val textPaint = Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 24f
                    textAlign = Paint.Align.RIGHT
                }

                labels.forEach { value ->
                    val yPos = midHeight - (value / ACC_REPORT_RANGE) * midHeight
                    drawLine(
                        color = if (value == 0) Color.Gray else Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(labelPadding, yPos),
                        end = Offset(width, yPos),
                        strokeWidth = if (value == 0) 2f else 1f,
                        pathEffect = if (value != 0) PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) else null
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        value.toString(),
                        labelPadding - 10f,
                        yPos + 8f,
                        textPaint
                    )
                }

                if (accPoints.isNotEmpty()) {
                    val xStep = graphWidth / (accPoints.size - 1).coerceAtLeast(1)
                    val pathX = Path(); val pathY = Path(); val pathZ = Path()

                    accPoints.forEachIndexed { index, point ->
                        val xPos = labelPadding + (index * xStep)
                        val yX = midHeight - (point.x / ACC_REPORT_RANGE) * midHeight
                        val yY = midHeight - (point.y / ACC_REPORT_RANGE) * midHeight
                        val yZ = midHeight - (point.z / ACC_REPORT_RANGE) * midHeight

                        if (index == 0) {
                            pathX.moveTo(xPos, yX); pathY.moveTo(xPos, yY); pathZ.moveTo(xPos, yZ)
                        } else {
                            pathX.lineTo(xPos, yX); pathY.lineTo(xPos, yY); pathZ.lineTo(xPos, yZ)
                        }
                    }
                    drawPath(pathX, Color.Red, style = Stroke(2.dp.toPx()))
                    drawPath(pathY, Color.Green, style = Stroke(2.dp.toPx()))
                    drawPath(pathZ, Color.Blue, style = Stroke(2.dp.toPx()))
                }
            }
        }
    }
}

@Composable
fun AccelerationStatsGrid(accPoints: List<AccPoint>) {
    if (accPoints.isEmpty()) return
    val xVals = accPoints.map { it.x }
    val yVals = accPoints.map { it.y }
    val zVals = accPoints.map { it.z }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Metrics (Acceleration)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().background(Color(0xFFEEEEEE), RoundedCornerShape(4.dp)).padding(8.dp)) {
            Text("Axis", Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("Max", Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            Text("Min", Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            Text("Avg", Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
        }
        StatRow("X", Color.Red, xVals.maxOrNull()?:0f, xVals.minOrNull()?:0f, xVals.average().toFloat())
        Divider()
        StatRow("Y", Color.Green, yVals.maxOrNull()?:0f, yVals.minOrNull()?:0f, yVals.average().toFloat())
        Divider()
        StatRow("Z", Color.Blue, zVals.maxOrNull()?:0f, zVals.minOrNull()?:0f, zVals.average().toFloat())
    }
}

@Composable
fun StatRow(label: String, color: Color, max: Float, min: Float, avg: Float) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(color, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.SemiBold)
        }
        Text("%.2f".format(max), Modifier.weight(1f), textAlign = TextAlign.End)
        Text("%.2f".format(min), Modifier.weight(1f), textAlign = TextAlign.End)
        Text("%.2f".format(avg), Modifier.weight(1f), textAlign = TextAlign.End)
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}