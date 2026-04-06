package com.ripplehealthcare.bproboard.ui.screens

import FourStageResult
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ripplehealthcare.bproboard.domain.model.*
import com.ripplehealthcare.bproboard.ui.components.TopBar
import com.ripplehealthcare.bproboard.ui.theme.PrimaryColor
import com.ripplehealthcare.bproboard.ui.viewmodel.PatientViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel
import com.ripplehealthcare.bproboard.utils.AllTestReport
import com.ripplehealthcare.bproboard.utils.generateAllTestsReportPdf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TestsHistoryScreen(
    navController: NavController,
    patientViewModel: PatientViewModel,
    testViewModel: TestViewModel
) {
    val context = LocalContext.current

    // 1. Observe State from TEST ViewModel
    val sessions by testViewModel.sessions.collectAsState()
    val fourStageHistory by testViewModel.fourStageHistory.collectAsState()
    val tugHistory by testViewModel.tugHistory.collectAsState()
    val fiveRepHistory by testViewModel.fiveRepHistory.collectAsState()
    val thirtySecHistory by testViewModel.thirtySecHistory.collectAsState()

    val selectedPatient by patientViewModel.selectedPatient.collectAsState()

    // 2. Fetch Data on Screen Entry
    LaunchedEffect(key1 = selectedPatient) {
        selectedPatient?.let { patient ->
            testViewModel.loadPatientHistory(patient.centerId,patient.patientId,patient.doctorId)
        }
    }

    // --- Tab States ---
    // Level 1: Main Tabs
    var mainTabIndex by remember { mutableIntStateOf(0) }
    val mainTabs = listOf("Comprehensive", "Detailed")

    // Level 2: Detailed Sub-Tabs
    var detailedTabIndex by remember { mutableIntStateOf(0) }
    val detailedTabs = listOf("5-Rep STS", "30-Sec STS", "TUG", "4-Stage")

    // --- Shared Helper for PDF Generation ---
    fun generateSingleReport(
        fiveRep: FiveRepResult? = null,
        thirtySec: ThirtySecResult? = null,
        tug: TugResult? = null,
        fourStage: FourStageResult? = null,
        action: String
    ) {
        val patient = selectedPatient ?: return
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val timestamp = fiveRep?.timestamp ?: thirtySec?.timestamp ?: tug?.timestamp ?: fourStage?.timestamp ?: Date()

        val report = AllTestReport(
            patient = patient,
            date = dateFormat.format(timestamp),
            time = timeFormat.format(timestamp),
            fiveRepResult = fiveRep,
            thirtySecResult = thirtySec,
            tugResult = tug,
            fourStageResult = fourStage
        )
        generatePdfAction(context, report, action)
    }

    Scaffold(
        topBar = {
            TopBar(title = "Test History", onBackClick = { navController.popBackStack() })
        },
        containerColor = Color(0xFFF8F9FA)
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            // --- LEVEL 1: MAIN TABS ---
            TabRow(
                selectedTabIndex = mainTabIndex,
                containerColor = Color.White,
                contentColor = PrimaryColor,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[mainTabIndex]),
                        color = PrimaryColor,
                        height = 3.dp
                    )
                }
            ) {
                mainTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = mainTabIndex == index,
                        onClick = { mainTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (mainTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 16.sp
                            )
                        },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            // --- CONTENT AREA ---
            Box(modifier = Modifier.fillMaxSize()) {
                selectedPatient?.let { patient ->
                    if (mainTabIndex == 0) {
                        // --- TAB 1: COMPREHENSIVE (Sessions List) ---
                        SessionsList(
                            history = sessions,
                            onSessionClick = { session ->
                                testViewModel.selectSessionForReport(session.sessionId,patient)
                                navController.navigate("sessionReport")
                            }
                        )
                    } else {
                        // --- TAB 2: DETAILED (Sub-tabs for Individual Tests) ---
                        Column(Modifier.fillMaxSize()) {
                            // Sub-Tabs Row
                            ScrollableTabRow(
                                selectedTabIndex = detailedTabIndex,
                                containerColor = Color(0xFFF8F9FA), // Slightly different bg
                                contentColor = PrimaryColor,
                                edgePadding = 0.dp,
                                indicator = { tabPositions ->
                                    TabRowDefaults.Indicator(
                                        Modifier.tabIndicatorOffset(tabPositions[detailedTabIndex]),
                                        color = PrimaryColor
                                    )
                                }
                            ) {
                                detailedTabs.forEachIndexed { index, title ->
                                    Tab(
                                        selected = detailedTabIndex == index,
                                        onClick = { detailedTabIndex = index },
                                        text = { Text(title) }
                                    )
                                }
                            }

                            // Sub-Tab Content
                            when (detailedTabIndex) {
                                0 -> FiveRepHistoryList(
                                    history = fiveRepHistory,
                                    onItemClick = { testViewModel.setSelectedFiveRepReport(it); navController.navigate("stsReport/FIVE_REPS") },
                                    onShare = { generateSingleReport(fiveRep = it, action = "SHARE") },
                                    onDownload = { generateSingleReport(fiveRep = it, action = "DOWNLOAD") }
                                )
                                1 -> ThirtySecHistoryList(
                                    history = thirtySecHistory,
                                    onItemClick = { testViewModel.setSelectedThirtySecReport(it); navController.navigate("stsReport/THIRTY_SECONDS") },
                                    onShare = { generateSingleReport(thirtySec = it, action = "SHARE") },
                                    onDownload = { generateSingleReport(thirtySec = it, action = "DOWNLOAD") }
                                )
                                2 -> TugHistoryList(
                                    history = tugHistory,
                                    onItemClick = { testViewModel.setSelectedTugReport(it); navController.navigate("tugReport") },
                                    onShare = { generateSingleReport(tug = it, action = "SHARE") },
                                    onDownload = { generateSingleReport(tug = it, action = "DOWNLOAD") }
                                )
                                3 -> FourStageHistoryList(
                                    history = fourStageHistory,
                                    onItemClick = { testViewModel.setSelectedReport(it); navController.navigate("fourStageReport") },
                                    onShare = { generateSingleReport(fourStage = it, action = "SHARE") },
                                    onDownload = { generateSingleReport(fourStage = it, action = "DOWNLOAD") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- COMPONENT: SESSIONS LIST ---
@Composable
fun SessionsList(
    history: List<TestSession>,
    onSessionClick: (TestSession) -> Unit
) {
    if (history.isEmpty()) EmptyHistoryMessage("No visits recorded yet.")
    else LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(history) { session ->
            SessionHistoryCard(
                session = session,
                onClick = { onSessionClick(session) }
            )
        }
    }
}

@Composable
fun SessionHistoryCard(
    session: TestSession,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Clinic Visit", style = MaterialTheme.typography.labelMedium, color = PrimaryColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(dateFormat.format(session.timestamp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(timeFormat.format(session.timestamp), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
            Surface(
//                color = if (session.isCompleted) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
//                    text = if (session.isCompleted) "Completed" else "In Progress",
                    text = "Completed",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
//                    color = if (session.isCompleted) Color(0xFF2E7D32) else Color(0xFFEF6C00)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = "View", tint = Color.Gray)
        }
    }
}

// --- INDIVIDUAL HISTORY LISTS ---

@Composable
fun FiveRepHistoryList(
    history: List<FiveRepResult>,
    onItemClick: (FiveRepResult) -> Unit,
    onShare: (FiveRepResult) -> Unit,
    onDownload: (FiveRepResult) -> Unit
) {
    if (history.isEmpty()) EmptyHistoryMessage("No 5-Rep STS tests found.")
    else LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(history) { result ->
            HistoryItemCard(
                title = "5-Rep Sit to Stand",
                date = result.timestamp,
                valueText = "Time: ${"%.1f".format(result.totalTimeSeconds)}s",
                valueColor = Color(0xFFFFF3E0),
                valueTextColor = Color(0xFFEF6C00),
                onView = { onItemClick(result) },
                onShare = { onShare(result) },
                onDownload = { onDownload(result) }
            )
        }
    }
}

@Composable
fun ThirtySecHistoryList(
    history: List<ThirtySecResult>,
    onItemClick: (ThirtySecResult) -> Unit,
    onShare: (ThirtySecResult) -> Unit,
    onDownload: (ThirtySecResult) -> Unit
) {
    if (history.isEmpty()) EmptyHistoryMessage("No 30-Sec STS tests found.")
    else LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(history) { result ->
            HistoryItemCard(
                title = "30-Sec Sit to Stand",
                date = result.timestamp,
                valueText = "${result.totalRepetitions} Reps",
                valueColor = Color(0xFFE8F5E9),
                valueTextColor = Color(0xFF2E7D32),
                onView = { onItemClick(result) },
                onShare = { onShare(result) },
                onDownload = { onDownload(result) }
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun TugHistoryList(
    history: List<TugResult>,
    onItemClick: (TugResult) -> Unit,
    onShare: (TugResult) -> Unit,
    onDownload: (TugResult) -> Unit
) {
    if (history.isEmpty()) EmptyHistoryMessage("No TUG tests found.")
    else LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(history) { result ->
            val durationText = String.format("%02d:%02d", result.totalTimeSeconds / 60, result.totalTimeSeconds % 60)
            HistoryItemCard(
                title = "Timed Up and Go",
                date = result.timestamp,
                valueText = "Time: $durationText",
                valueColor = Color(0xFFE3F2FD),
                valueTextColor = Color(0xFF1565C0),
                onView = { onItemClick(result) },
                onShare = { onShare(result) },
                onDownload = { onDownload(result) }
            )
        }
    }
}

@Composable
fun FourStageHistoryList(
    history: List<FourStageResult>,
    onItemClick: (FourStageResult) -> Unit,
    onShare: (FourStageResult) -> Unit,
    onDownload: (FourStageResult) -> Unit
) {
    if (history.isEmpty()) EmptyHistoryMessage("No 4-Stage tests found.")
    else LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(history) { result ->
            val stages = result.stages.size
            HistoryItemCard(
                title = "Four Stage Balance",
                date = result.timestamp,
                valueText = "$stages/4 Stages",
                valueColor = if (stages == 4) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                valueTextColor = if (stages == 4) Color(0xFF2E7D32) else Color(0xFFEF6C00),
                onView = { onItemClick(result) },
                onShare = { onShare(result) },
                onDownload = { onDownload(result) }
            )
        }
    }
}

// --- SHARED UTILS ---

fun generatePdfAction(context: android.content.Context, report: AllTestReport, action: String) {
    val toCache = action == "SHARE"
    val pdfUri = generateAllTestsReportPdf(context, report, toCache = toCache)

    if (pdfUri != null) {
        if (action == "SHARE") {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Report"))
        } else {
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
    } else {
        Toast.makeText(context, "Failed to generate report", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun HistoryItemCard(
    title: String,
    date: Date,
    valueText: String,
    valueColor: Color,
    valueTextColor: Color,
    onView: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onView() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("${dateFormat.format(date)} • ${timeFormat.format(date)}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = valueColor, shape = RoundedCornerShape(16.dp)) {
                    Text(
                        text = valueText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = valueTextColor
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.Gray)
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text="View Report",
                                    color=Color.Black
                                )
                            },
                            onClick = { showMenu = false; onView() },
                            leadingIcon = { Icon(Icons.Default.Visibility, null, tint = Color.Black) }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text="Share PDF",
                                    color=Color.Black
                                )
                            },
                            onClick = { showMenu = false; onShare() },
                            leadingIcon = { Icon(Icons.Default.Share, null, tint = Color.Black) }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text="Download PDF",
                                    color=Color.Black
                                )
                            },
                            onClick = { showMenu = false; onDownload() },
                            leadingIcon = { Icon(Icons.Default.Download, null, tint = Color.Black) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyHistoryMessage(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.History, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.titleMedium, color = Color.Gray)
    }
}