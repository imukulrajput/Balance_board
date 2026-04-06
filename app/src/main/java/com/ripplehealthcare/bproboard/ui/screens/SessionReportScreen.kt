package com.ripplehealthcare.bproboard.ui.screens

import FourStageResult
import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.ripplehealthcare.bproboard.domain.model.*
import com.ripplehealthcare.bproboard.ui.components.AccelerationGraph
import com.ripplehealthcare.bproboard.ui.components.TopBar
import com.ripplehealthcare.bproboard.ui.components.customTextFieldColors
import com.ripplehealthcare.bproboard.ui.theme.PrimaryColor
import com.ripplehealthcare.bproboard.ui.theme.WhiteColor
import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel
import com.ripplehealthcare.bproboard.utils.AllTestReport
import com.ripplehealthcare.bproboard.utils.BalanceScoreCalculator
import com.ripplehealthcare.bproboard.utils.NormativeRange
import com.ripplehealthcare.bproboard.utils.generateAllTestsReportPdf
import com.ripplehealthcare.bproboard.utils.getFiveSTSNorm
import com.ripplehealthcare.bproboard.utils.getThirtySTSNorm
import com.ripplehealthcare.bproboard.utils.getTugNorm

@Composable
fun SessionReportScreen(
    navController: NavController,
    testViewModel: TestViewModel
) {
    val context = LocalContext.current
    val report by testViewModel.currentSessionReport.collectAsState()
    val scoreData = remember(report) {
        report?.let { data ->
            val age = data.patient.age.toIntOrNull() ?: 65
            val gender = Gender.entries.find { it.displayName == data.patient.gender } ?: Gender.OTHER

            // Individual Scores using your BalanceScoreCalculator
            val sTug = BalanceScoreCalculator.calculateTimedScore(
                data.tugResult?.totalTimeSeconds?.toDouble() ?: 0.0,
                getTugNorm(age, gender)?.let { NormativeRange(it.meanValue, it.failureValue) } ?: NormativeRange(10.0, 20.0)
            )
            val s4S = BalanceScoreCalculator.calculateFourStageScore(
                data.fourStageResult?.stages?.associateBy { it.stageNumber } ?: emptyMap()
            )
            val s30 = BalanceScoreCalculator.calculateRepetitionScore(
                data.thirtySecResult?.totalRepetitions ?: 0,
                getThirtySTSNorm(age, gender)?.let { NormativeRange(it.meanValue, it.failureValue) } ?: NormativeRange(12.0, 5.0)
            )
            val s5 = BalanceScoreCalculator.calculateTimedScore(
                data.fiveRepResult?.totalTimeSeconds?.toDouble() ?: 0.0,
                getFiveSTSNorm(age, gender)?.let { NormativeRange(it.meanValue, it.failureValue) } ?: NormativeRange(12.0, 25.0)
            )

            val total = (sTug * 0.35) + (s4S * 0.30) + (s30 * 0.20) + (s5 * 0.15)

            mapOf("total" to total, "tug" to sTug, "fourStage" to s4S, "sts30" to s30, "sts5" to s5)
        }
    }
    var showNotesDialog by remember { mutableStateOf(false) }

    // If report is null (loading or error), show spinner or empty state
    if (report == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryColor)
        }
        return
    }

    val data = report!!

    Scaffold(
        topBar = {
            TopBar("Session Report", onBackClick = { navController.popBackStack() })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNotesDialog = true },
                icon = { Icon(Icons.Default.Edit, "Notes") },
                text = { Text("Doctor's Notes") },
                containerColor = PrimaryColor,
                contentColor = WhiteColor
            )
        },
        bottomBar = {
            ReportActionsBar(
                onShare = {
                    val pdfUri = generateAllTestsReportPdf(context, data, toCache = true)
                    if (pdfUri != null) {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, pdfUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Report"))
                    }
                },
                onDownload = {
                    val pdfUri = generateAllTestsReportPdf(context, data, toCache = false)
                    if (pdfUri != null){
                        Toast.makeText(context, "Report Generated", Toast.LENGTH_LONG).show()
                        val openPdfIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(pdfUri, "application/pdf")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        try {
                            context.startActivity(openPdfIntent)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, "No PDF viewer found", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Patient Header
            item {
                PatientSummaryCard(data)
            }

            scoreData?.get("total")?.let { totalScore ->
                item {
                    OverallScoreCard(score = totalScore.toInt())
                    Spacer(Modifier.height(16.dp))
                }
            }

            if (report!!.fiveRepResult != null) {
                item { StsReportCard(report!!.fiveRepResult, isFiveRep = true, score = scoreData?.get("sts5")) }
            }

            if (report!!.thirtySecResult != null) {
                item { StsReportCard(thirtySecResult = report!!.thirtySecResult, isFiveRep = false, score = scoreData?.get("sts30")) }
            }

            if (report!!.tugResult != null) {
                item { TugReportCard(report!!.tugResult!!, score = scoreData?.get("tug")) }
            }

            if (report!!.fourStageResult != null) {
                item { FourStageReportCard(report!!.fourStageResult!!, score = scoreData?.get("fourStage")) }
            }

            // Spacing for FAB
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // Notes Dialog
    if (showNotesDialog) {
        DoctorNotesDialog(
            initialNotes = report!!.doctorNotes, // data.doctorNotes
            onDismiss = { showNotesDialog = false },
            onSave = { notes ->
                testViewModel.saveDoctorNotes(data.centerId,data.sessionId,data.patient.patientId,notes)
                showNotesDialog = false
            }
        )
    }
}

// --- COMPONENT: PATIENT HEADER ---
@Composable
fun PatientSummaryCard(data: AllTestReport) {
    Card(
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(data.patient.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${data.patient.age} yrs • ${data.patient.gender}", color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Date: ${data.date}", fontWeight = FontWeight.Medium)
                Text("Time: ${data.time}", fontWeight = FontWeight.Medium)
            }
        }
    }
}

// --- COMPONENT: TUG REPORT ---
@Composable
fun TugReportCard(result: TugResult, score: Double?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            ReportHeader("Timed Up and Go (TUG)", score)

            // Metrics
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("Total Time", "${"%.2f".format(result.totalTimeSeconds.toFloat())} s")
                MetricItem("Status", "Completed", Color(0xFF4CAF50))
            }

            Spacer(Modifier.height(16.dp))
            Text("Waist Acceleration", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            Spacer(Modifier.height(8.dp))

            // Graph
            Box(Modifier.height(150.dp).fillMaxWidth()) {
                AccelerationGraph(
                    modifier = Modifier.fillMaxSize(),
                    dataPoints = result.accCenter,
                    range = 6f
                )
            }
        }
    }
}

// --- COMPONENT: STS REPORT (5-Rep & 30-Sec) ---
@Composable
fun StsReportCard(
    fiveRepResult: FiveRepResult? = null,
    thirtySecResult: ThirtySecResult? = null,
    isFiveRep: Boolean,
    score: Double?
) {
    val accCenter = if (isFiveRep) fiveRepResult!!.accCenter else thirtySecResult!!.accCenter
    val title = if (isFiveRep) "5-Rep Sit to Stand" else "30-Sec Sit to Stand"

    // Get stats from either result type
    val repStats = if (isFiveRep) fiveRepResult!!.repetitionStats else thirtySecResult!!.repetitionStats

    Card(
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            ReportHeader(title, score)

            // --- Metrics Row ---
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (isFiveRep) {
                    MetricItem("Total Time", "${"%.2f".format(fiveRepResult!!.totalTimeSeconds)} s")
                    MetricItem("Reps", "5")
                } else {
                    MetricItem("Total Reps", "${thirtySecResult!!.totalRepetitions}")
                    MetricItem("Time Limit", "30 s")
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- NEW: Bar Chart Section ---
            if (repStats.isNotEmpty()) {
                RepetitionDurationChart(repStats = repStats)
                Spacer(Modifier.height(24.dp))
            } else {
                Text("No repetition details available", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(Modifier.height(16.dp))
            }

            Divider()
            Spacer(Modifier.height(16.dp))

            // --- Acceleration Graph ---
            Text("Waist Acceleration", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            Spacer(Modifier.height(8.dp))

            Box(Modifier.height(150.dp).fillMaxWidth()) {
                AccelerationGraph(Modifier.fillMaxSize(), accCenter, 4f)
            }
        }
    }
}

// --- COMPONENT: 4-STAGE REPORT ---
@Composable
fun FourStageReportCard(result: FourStageResult, score: Double?) {
    // 1. Convert List to Map for easy access
    val stageMap = remember(result) { result.stages.associateBy { it.stageNumber } }

    // 2. Local State for the Card
    var selectedStageIndex by remember { mutableIntStateOf(0) } // 0 = Stage 1

    val stageTabs = listOf("Stage 1", "Stage 2", "Stage 3", "Stage 4")

    Card(
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            ReportHeader("4-Stage Balance Test", score)

            // --- STAGE SELECTION TABS ---
            ScrollableTabRow(
                selectedTabIndex = selectedStageIndex,
                containerColor = WhiteColor,
                contentColor = PrimaryColor,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedStageIndex]),
                        color = PrimaryColor
                    )
                }
            ) {
                stageTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedStageIndex == index,
                        onClick = { selectedStageIndex = index },
                        text = { Text(title, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- DATA DISPLAY ---
            val currentStageId = selectedStageIndex + 1
            val data = stageMap[currentStageId]

            if (data == null) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No data for Stage $currentStageId", color = Color.Gray)
                }
            } else {
                // 1. TILT / SWAY VISUALIZATION
                Text("Waist Sway & Tilt", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f) // Keep it square
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                ) {
                    // Embed the Canvas Logic here
                    ReportCanvas(dataPoints = data.pointsCenter)
                }

                // Tilt Metrics
                MetricsSection(dataPoints = data.pointsCenter)

                Divider(Modifier.padding(vertical = 16.dp))
            }
        }
    }
}

@Composable
fun OverallScoreCard(title: String = "Ripple Balance Score", score: Int) {
    val statusColor = when {
        score >= 80 -> Color(0xFF2E7D32) // Deeper Green
        score >= 50 -> Color(0xFFEF6C00) // Deeper Orange
        else -> Color(0xFFD32F2F)        // Deeper Red
    }

    val riskLevel = when {
        score >= 80 -> "Low Risk"
        score >= 50 -> "Moderate Risk"
        else -> "High Risk"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$score/100",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Status Chip
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = riskLevel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Enhanced Circular Progress
            Box(contentAlignment = Alignment.Center) {
                // Background Track
                CircularProgressIndicator(
                    progress = 1f,
                    modifier = Modifier.size(85.dp),
                    color = Color.LightGray.copy(alpha = 0.3f),
                    strokeWidth = 10.dp,
                    strokeCap = StrokeCap.Round
                )
                // Active Score
                CircularProgressIndicator(
                    progress = score / 100f,
                    modifier = Modifier.size(85.dp),
                    color = statusColor,
                    strokeWidth = 10.dp,
                    strokeCap = StrokeCap.Round
                )
                // Percentage Text
                Text(
                    text = "$score%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}


// --- SHARED UI HELPERS ---
@Composable
fun ReportHeader(title: String, score: Double?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryColor)
        if (score != null) {
            Surface(
                color = PrimaryColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Score: ${score.toInt()}/100",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = PrimaryColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    Divider(Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))
}

@Composable
fun MetricItem(label: String, value: String, valueColor: Color = Color.Black) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
fun ReportActionsBar(onShare: () -> Unit, onDownload: () -> Unit) {
    Surface(shadowElevation = 16.dp, color = WhiteColor) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp, 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onShare,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(
                    1.dp,
                   PrimaryColor
                ),
                colors = ButtonDefaults.buttonColors(contentColor = PrimaryColor, disabledContentColor = Color.Gray, containerColor = Color.Transparent, disabledContainerColor = Color.Transparent)
            ) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Share")
            }
            Button(
                onClick = onDownload,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, contentColor = WhiteColor)
            ) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Download")
            }
        }
    }
}

@Composable
fun DoctorNotesDialog(
    initialNotes: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.9f),
        containerColor = Color.White, // Force White Background
        title = {
            Text(
                "Doctor's Notes",
                fontWeight = FontWeight.Bold,
                color = Color.Black // Force Black Text
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                placeholder = {
                    Text(
                        "Enter observations, diagnosis, or recommendations...",
                        color = Color.Gray // Force Gray Placeholder
                    )
                },
                colors = customTextFieldColors(), // Apply consistent text field colors
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(text) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Notes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray) // Force Gray text
            }
        }
    )
}