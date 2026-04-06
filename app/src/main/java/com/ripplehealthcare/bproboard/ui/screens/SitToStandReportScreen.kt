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
import androidx.compose.ui.geometry.Size
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
import com.ripplehealthcare.bproboard.domain.model.FiveRepResult
import com.ripplehealthcare.bproboard.domain.model.Gender
import com.ripplehealthcare.bproboard.domain.model.RepetitionStats
import com.ripplehealthcare.bproboard.domain.model.SensorAxisStats
import com.ripplehealthcare.bproboard.domain.model.TestType
import com.ripplehealthcare.bproboard.domain.model.ThirtySecResult
import com.ripplehealthcare.bproboard.ui.components.TopBar
import com.ripplehealthcare.bproboard.ui.theme.PrimaryColor
import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel
import com.ripplehealthcare.bproboard.utils.BalanceScoreCalculator
import com.ripplehealthcare.bproboard.utils.NormativeRange
import com.ripplehealthcare.bproboard.utils.getFiveSTSNorm
import com.ripplehealthcare.bproboard.utils.getThirtySTSNorm
import java.text.SimpleDateFormat
import java.util.Locale

// Configuration for charts
private const val ACC_REPORT_RANGE = 10.0f

@Composable
fun SitToStandReportScreen(
    navController: NavController,
    testViewModel: TestViewModel,
    testTypeStr: String // Pass "FIVE_REPS" or "THIRTY_SECONDS" via nav arguments
) {
    val testType = if (testTypeStr == "FIVE_REPS") TestType.FIVE_REPS else TestType.THIRTY_SECONDS
    val patient by testViewModel.patient.collectAsState()

    // Observe appropriate report based on type
    val fiveRepReport by testViewModel.selectedFiveRepReport.collectAsState()
    val thirtySecReport by testViewModel.selectedThirtySecReport.collectAsState()

    val reportData = remember(fiveRepReport, thirtySecReport, patient) {
        val age = patient.age.toIntOrNull() ?: 65
        val gender = Gender.entries.find { it.displayName == patient.gender } ?: Gender.OTHER

        when (testType) {
            TestType.FIVE_REPS -> fiveRepReport?.let { report ->
                val norm = getFiveSTSNorm(age, gender)?.let { NormativeRange(it.meanValue, it.failureValue) }
                    ?: NormativeRange(12.0, 25.0)
                val score = BalanceScoreCalculator.calculateTimedScore(report.totalTimeSeconds.toDouble(), norm)
                StsReportData(report).copy(calculatedScore = score)
            }
            TestType.THIRTY_SECONDS -> thirtySecReport?.let { report ->
                val norm = getThirtySTSNorm(age, gender)?.let { NormativeRange(it.meanValue, it.failureValue) }
                    ?: NormativeRange(12.0, 5.0)
                val score = BalanceScoreCalculator.calculateRepetitionScore(report.totalRepetitions, norm)
                StsReportData(report).copy(calculatedScore = score)
            }
            else -> null
        }
    }

    val title = if (testType == TestType.FIVE_REPS) "5-Rep STS Report" else "30-Sec STS Report"
    var selectedSensorTab by remember { mutableIntStateOf(0) }
    val sensorTabs = listOf("Waist", "Left Leg", "Right Leg")

    Scaffold(
        topBar = { TopBar(title = title, onBackClick = { navController.popBackStack() }) },
        containerColor = Color(0xFFF8F9FA)
    ) { innerPadding ->
        val scrollState = rememberScrollState()

        if (reportData == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No report data found.", color = Color.Gray)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                // --- 1. SUMMARY SECTION ---
                StsSummaryCard(data = reportData)
                Spacer(modifier = Modifier.height(24.dp))

                // --- 2. REPETITION CONSISTENCY (Bar Chart) ---
                Text("Repetition Consistency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                RepetitionDurationChart(repStats = reportData.repStats)
                Spacer(modifier = Modifier.height(24.dp))

                // --- 3. RAW Acceleration ANALYSIS (Line Graph) ---
                Text("Raw Acceleration Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                // Sensor Tabs
                TabRow(
                    selectedTabIndex = selectedSensorTab,
                    containerColor = Color.White,
                    contentColor = PrimaryColor,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(Modifier.tabIndicatorOffset(tabPositions[selectedSensorTab]), color = PrimaryColor)
                    }
                ) {
                    sensorTabs.forEachIndexed { index, t ->
                        Tab(selected = selectedSensorTab == index, onClick = { selectedSensorTab = index }, text = { Text(t) })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                val accDataToShow = when (selectedSensorTab) {
                    0 -> reportData.accCenter
                    1 -> reportData.accLeft
                    else -> reportData.accRight
                }
                GenericAccelerationChart(accPoints = accDataToShow)
                Spacer(modifier = Modifier.height(24.dp))


                // --- 4. DETAILED STATISTICS (The "Deep Dive") ---
                Text("Detailed Repetition Statistics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Min / Max / Avg acceleration per axis.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))

                DetailedStatsSection(repStats = reportData.repStats)

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun StsSummaryCard(data: StsReportData) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val scoreInt = data.calculatedScore.toInt()

    val (statusColor, riskLevel) = when {
        scoreInt >= 80 -> Color(0xFF4CAF50) to "Low Risk"
        scoreInt >= 50 -> Color(0xFFFF9800) to "Moderate Risk"
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = data.mainScoreLabel, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Text(text = data.mainScoreValue, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Risk Badge
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

                // Score Gauge
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = scoreInt / 100f,
                        modifier = Modifier.size(80.dp),
                        color = statusColor,
                        strokeWidth = 8.dp,
                        trackColor = statusColor.copy(alpha = 0.1f)
                    )
                    Text(
                        text = "$scoreInt",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Date: ${dateFormat.format(data.timestamp)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(text = "Test Score: $scoreInt/100", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RepetitionDurationChart(repStats: List<RepetitionStats>) {
    if (repStats.isEmpty()) return

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth().height(220.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Duration per Repetition (seconds)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))

            Canvas(modifier = Modifier.fillMaxSize()) {
                val maxDuration = repStats.maxOfOrNull { it.duration }?.toFloat() ?: 1f
                // Add 20% buffer to top
                val chartMaxY = maxDuration * 1.2f

                val barWidth = size.width / (repStats.size * 1.5f)
                val gapWidth = barWidth * 0.5f
                val chartHeight = size.height - 30.dp.toPx() // Space for labels bottom

                repStats.forEachIndexed { index, stat ->
                    val xOffset = index * (barWidth + gapWidth)
                    val barHeight = (stat.duration / chartMaxY) * chartHeight

                    // Draw Bar
                    drawRect(
                        color = PrimaryColor,
                        topLeft = Offset(xOffset, chartHeight - barHeight),
                        size = Size(barWidth, barHeight)
                    )

                    // Draw Label (Rep Number)
                    drawContext.canvas.nativeCanvas.drawText(
                        "${stat.repIndex}",
                        xOffset + barWidth / 2,
                        size.height - 5.dp.toPx(),
                        Paint().apply { color = android.graphics.Color.GRAY; textSize = 32f; textAlign = Paint.Align.CENTER }
                    )

                    // Draw Value on top of bar
                    drawContext.canvas.nativeCanvas.drawText(
                        "%.1fs".format(stat.duration / 1000f),
                        xOffset + barWidth / 2,
                        chartHeight - barHeight - 10.dp.toPx(),
                        Paint().apply { color = android.graphics.Color.BLACK; textSize = 28f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
                    )
                }
            }
        }
    }
}

@Composable
fun DetailedStatsSection(repStats: List<RepetitionStats>) {
    repStats.forEach { stat ->
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Rep Header
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFE3F2FD), RoundedCornerShape(4.dp)).padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Repetition ${stat.repIndex}", fontWeight = FontWeight.Bold, color = PrimaryColor)
                    Text("Duration: %.2fs".format(stat.duration / 1000f), fontWeight = FontWeight.Bold, color = PrimaryColor)
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Sensor Stats Grids - Passing specific X, Y, Z stats for each sensor
                SensorStatsGrid(
                    title = "Waist Sensor",
                    xStats = stat.centerX,
                    yStats = stat.centerY,
                    zStats = stat.centerZ
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                SensorStatsGrid(
                    title = "Left Leg Sensor",
                    xStats = stat.leftX,
                    yStats = stat.leftY,
                    zStats = stat.leftZ
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                SensorStatsGrid(
                    title = "Right Leg Sensor",
                    xStats = stat.rightX,
                    yStats = stat.rightY,
                    zStats = stat.rightZ
                )
            }
        }
    }
}

@Composable
fun SensorStatsGrid(
    title: String,
    xStats: SensorAxisStats,
    yStats: SensorAxisStats,
    zStats: SensorAxisStats
) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))

        // Header Row
        Row(Modifier.fillMaxWidth()) {
            Text("Axis", Modifier.weight(0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Avg", Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            Text("Min", Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            Text("Max", Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
        }
        Spacer(modifier = Modifier.height(4.dp))

        // Data Rows
        AxisStatRow("X", xStats, Color.Red)
        AxisStatRow("Y", yStats, Color.Green)
        AxisStatRow("Z", zStats, Color.Blue)
    }
}

@Composable
fun AxisStatRow(label: String, axisStats: SensorAxisStats, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(0.8f)) {
            Box(Modifier.size(8.dp).background(color, CircleShape).align(Alignment.CenterVertically))
            Spacer(Modifier.width(4.dp))
            Text(label, fontWeight = FontWeight.Medium)
        }
        Text("%.2f".format(axisStats.avg), Modifier.weight(1f), textAlign = TextAlign.End, fontSize = 13.sp)
        Text("%.2f".format(axisStats.min), Modifier.weight(1f), textAlign = TextAlign.End, fontSize = 13.sp)
        Text("%.2f".format(axisStats.max), Modifier.weight(1f), textAlign = TextAlign.End, fontSize = 13.sp)
    }
}

// ================= REUSED/GENERIC CHART COMPONENT =================
// (This is basically the same chart from TugReportScreen, made generic)
@Composable
fun GenericAccelerationChart(accPoints: List<AccPoint>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem("X (Left-Right)", Color.Red)
                LegendItem("Y (Up-Down)", Color.Green)
                LegendItem("Z (Forward-Backward)", Color.Blue)
            }
            Spacer(modifier = Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
            ) {
                // ... (Exact same drawing logic as TugAccelerationChart in previous response) ...
                // Copy the Canvas content from TugReportScreen's TugAccelerationChart here.
                // It's omitted for brevity as it's identical code.
                val width = size.width
                val height = size.height
                val midHeight = height / 2f
                val labelPadding = 35.dp.toPx()
                val graphWidth = width - labelPadding
                val labels = listOf(8, 6, 4, 2, 0, -2, -4, -6, -8)

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

// ================= HELPER DATA CLASS =================
// Normalizes the data between 5-Rep and 30-Sec results for easier UI rendering
data class StsReportData(
    val isFiveRep: Boolean,
    val mainScoreLabel: String,
    val mainScoreValue: String,
    val timestamp: java.util.Date,
    val repStats: List<RepetitionStats>,
    val accCenter: List<AccPoint>,
    val accLeft: List<AccPoint>,
    val accRight: List<AccPoint>,
    val calculatedScore: Double
) {
    constructor(fiveRep: FiveRepResult) : this(
        isFiveRep = true,
        mainScoreLabel = "Total Time",
        mainScoreValue = "%.2fs".format(fiveRep.totalTimeSeconds),
        timestamp = fiveRep.timestamp,
        repStats = fiveRep.repetitionStats, // Ensure you updated your model to include this
        accCenter = fiveRep.accCenter,
        accLeft = fiveRep.accLeft,
        accRight = fiveRep.accRight,
        calculatedScore = 0.0
    )

    constructor(thirtySec: ThirtySecResult) : this(
        isFiveRep = false,
        mainScoreLabel = "Total Repetitions",
        mainScoreValue = "${thirtySec.totalRepetitions}",
        timestamp = thirtySec.timestamp,
        repStats = thirtySec.repetitionStats, // Ensure you updated your model to include this
        accCenter = thirtySec.accCenter,
        accLeft = thirtySec.accLeft,
        accRight = thirtySec.accRight,
        calculatedScore = 0.0
    )
}