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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
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
import androidx.navigation.NavController
import com.ripplehealthcare.bproboard.domain.model.AccPoint
import com.ripplehealthcare.bproboard.domain.model.Gender
import com.ripplehealthcare.bproboard.ui.components.TopBar
import com.ripplehealthcare.bproboard.ui.theme.PrimaryColor
import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel
import com.ripplehealthcare.bproboard.utils.BalanceScoreCalculator
import com.ripplehealthcare.bproboard.utils.NormativeRange
import com.ripplehealthcare.bproboard.utils.getTugNorm
import kotlin.math.sqrt

// Configuration
private const val ACC_REPORT_RANGE = 10.0f // Graph scale +/- 10

@Composable
fun TugReportScreen(
    navController: NavController,
    testViewModel: TestViewModel
) {
    // 1. Observe the SELECTED REPORT
    val selectedReport by testViewModel.selectedTugReport.collectAsState()
    val patient by testViewModel.patient.collectAsState()

    val tugScoreData = remember(selectedReport, patient) {
        selectedReport?.let { report ->
            val age = patient.age.toIntOrNull() ?: 65
            val gender = Gender.entries.find { it.displayName == patient.gender } ?: Gender.MALE

            val norm = getTugNorm(age, gender)?.let {
                NormativeRange(it.meanValue, it.failureValue)
            } ?: NormativeRange(10.0, 20.0)

            val score = BalanceScoreCalculator.calculateTimedScore(
                report.totalTimeSeconds.toDouble(),
                norm
            )
            score.toInt()
        }
    }

    var selectedSensorTab by remember { mutableIntStateOf(0) }
    val sensorTabs = listOf("Waist", "Left Leg", "Right Leg")

    Scaffold(
        topBar = { TopBar(title = "TUG Test Report", onBackClick = { navController.popBackStack() }) },
        containerColor = Color(0xFFF8F9FA)
    ) { innerPadding ->
        if (selectedReport == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No report data found.", color = Color.Gray)
            }
        } else {
            val report = selectedReport!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- 1. SUMMARY CARD ---
                TugSummaryCard(totalTime = report.totalTimeSeconds, turnSpeed = report.turnSpeed, score= tugScoreData)

                Spacer(modifier = Modifier.height(24.dp))

                // --- 2. SENSOR TABS ---
                Text(
                    "Acceleration Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                TabRow(
                    selectedTabIndex = selectedSensorTab,
                    containerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedSensorTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    sensorTabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedSensorTab == index,
                            onClick = { selectedSensorTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- 3. DATA VISUALIZATION ---
                val dataToShow = when (selectedSensorTab) {
                    0 -> report.accCenter
                    1 -> report.accLeft
                    else -> report.accRight
                }

                if (dataToShow.isNotEmpty()) {
                    // Graph
                    TugAccelerationChart(accPoints = dataToShow)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Detailed Stats
                    TugStatsGrid(accPoints = dataToShow)
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No sensor data recorded for this device.", color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// --- COMPONENTS ---
@Composable
fun TugSummaryCard(totalTime: Int, turnSpeed: Float, score: Int?) {
    val (statusColor, riskLevel) = when {
        score == null -> Color.Gray to "N/A"
        score >= 80 -> Color(0xFF4CAF50) to "Low Risk"
        score >= 50 -> Color(0xFFFF9800) to "Moderate Risk"
        else -> Color(0xFFF44336) to "High Risk"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Time and Turn Speed Metrics
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, null, tint = PrimaryColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Total Time: ${totalTime}s", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Refresh, null, tint = Color(0xFFEF6C00), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Turn Speed: %.1f°/s".format(turnSpeed), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Risk Level Badge
                    Surface(
                        color = statusColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = riskLevel,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = statusColor,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // TUG Score Gauge
                if (score != null) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = score / 100f,
                            modifier = Modifier.size(80.dp),
                            color = statusColor,
                            strokeWidth = 8.dp,
                            trackColor = statusColor.copy(alpha = 0.1f)
                        )
                        Text(
                            text = "$score",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TugAccelerationChart(accPoints: List<AccPoint>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TugLegendItem("X (Left-Right)", Color.Red)
                TugLegendItem("Y (Up-Down)", Color.Green)
                TugLegendItem("Z (Forward-Backward)", Color.Blue)
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
                val labels = listOf(8,6, 4, 2, 0, -2, -4, -6,-8)

                val textPaint = Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 24f
                    textAlign = Paint.Align.RIGHT
                }

                // Grid Lines
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

                // Plotting Lines
                if (accPoints.isNotEmpty()) {
                    val xStep = graphWidth / (accPoints.size - 1).coerceAtLeast(1)
                    val pathX = Path(); val pathY = Path(); val pathZ = Path()

                    accPoints.forEachIndexed { index, point ->
                        val xPos = labelPadding + (index * xStep)
                        // Invert Y axis for drawing so positive is up
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
fun TugStatsGrid(accPoints: List<AccPoint>) {
    val xVals = accPoints.map { it.x }
    val yVals = accPoints.map { it.y }
    val zVals = accPoints.map { it.z }

    // Calculate Motion Intensity (Vector Magnitude)
    val magnitudeAvg = accPoints.map { sqrt(it.x*it.x + it.y*it.y + it.z*it.z) }.average()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Detailed Metrics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))

        // Header
        Row(Modifier.fillMaxWidth().background(Color(0xFFEEEEEE), RoundedCornerShape(4.dp)).padding(8.dp)) {
            Text("Axis", Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("Max", Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            Text("Min", Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            Text("Avg", Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
        }

        TugStatRow("X", Color.Red, xVals.maxOrNull()?:0f, xVals.minOrNull()?:0f, xVals.average().toFloat())
        Divider()
        TugStatRow("Y", Color.Green, yVals.maxOrNull()?:0f, yVals.minOrNull()?:0f, yVals.average().toFloat())
        Divider()
        TugStatRow("Z", Color.Blue, zVals.maxOrNull()?:0f, zVals.minOrNull()?:0f, zVals.average().toFloat())

        Spacer(modifier = Modifier.height(16.dp))

        // Intensity Metric
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Avg Acceleration Intensity", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("(Vector Magnitude)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Text(
                    text = "%.2f".format(magnitudeAvg),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun TugStatRow(label: String, color: Color, max: Float, min: Float, avg: Float) {
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
fun TugLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}