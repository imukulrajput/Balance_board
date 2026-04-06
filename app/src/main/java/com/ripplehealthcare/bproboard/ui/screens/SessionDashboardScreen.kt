package com.ripplehealthcare.bproboard.ui.screens

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ripplehealthcare.bproboard.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import com.ripplehealthcare.bproboard.domain.model.ConnectionState
import com.ripplehealthcare.bproboard.domain.model.TestType
import com.ripplehealthcare.bproboard.ui.components.TopBar
import com.ripplehealthcare.bproboard.ui.theme.CardColor
import com.ripplehealthcare.bproboard.ui.theme.Green
import com.ripplehealthcare.bproboard.ui.theme.PrimaryColor
import com.ripplehealthcare.bproboard.ui.theme.WhiteColor
import com.ripplehealthcare.bproboard.ui.viewmodel.BluetoothViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.PatientViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel

@Composable
fun SessionDashboardScreen(
    navController: NavController,
    testViewModel: TestViewModel,
    patientViewModel: PatientViewModel, // Added to get selected patient ID
    bluetoothViewModel: BluetoothViewModel
) {
    val context = LocalContext.current

    // 1. Observe Session State
    val sessionProgress by testViewModel.sessionProgress.collectAsState()
    val currentSessionId by testViewModel.currentSessionId.collectAsState()
    val selectedPatient by patientViewModel.selectedPatient.collectAsState()
    val connectionState by bluetoothViewModel.connectionState.collectAsState()
    val scrollState = rememberScrollState()

    // 2. Start Session Logic
    LaunchedEffect(Unit) {
        // Ensure Bluetooth is ready
        bluetoothViewModel.startTest()

        // Start a new session if one isn't active
        if (currentSessionId == null) {
            testViewModel.startNewSession()
        }
    }

    // 3. Handle Disconnection
    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.DISCONNECTED) {
            Toast.makeText(context, "Device Disconnected.", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopBar("Current Session",
                onBackClick = {
                    navController.popBackStack()
                })
        },
        bottomBar = {
            // 4. "Finish Session" Button
            FinishSessionBottomBar(
                isEnabled = sessionProgress.isNotEmpty(), // Enable only if at least 1 test is done
                onFinishClick = {
                    selectedPatient?.let { patient ->
                        // Finish the session in DB
                        testViewModel.finishSession(patient.centerId,patient.doctorId,patient.patientId)
                        Toast.makeText(context, "Session Saved Successfully", Toast.LENGTH_SHORT).show()

                        // Navigate away (e.g., back to Patient Details or History)
                        navController.popBackStack()
                    }
                }
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 5. Test Cards (Now connected to sessionProgress)

            TestSelectionCard(
                title = "5 Time Sit to Stand",
                imageResId = R.drawable.standing_image,
                isCompleted = sessionProgress[TestType.FIVE_REPS] == true,
                handleClick = {
                    testViewModel.setTestType(TestType.FIVE_REPS)
                    navController.navigate("testInstruction/5_time_sit_stand") {
                        // Ensure we come back to THIS dashboard, not restart it
                        popUpTo("sessionDashboard") { inclusive = false } // Changed ID
                    }
                },
                enabled = true
            )

            TestSelectionCard(
                title = "30 Second Sit to Stand",
                imageResId = R.drawable.sitting_image,
                isCompleted = sessionProgress[TestType.THIRTY_SECONDS] == true,
                handleClick = {
                    testViewModel.setTestType(TestType.THIRTY_SECONDS)
                    navController.navigate("testInstruction/30_sec_sit_to_stand") {
                        popUpTo("sessionDashboard") { inclusive = false }
                    }
                },
                enabled = true
            )

            TestSelectionCard(
                title = "4 Stage Test",
                imageResId = R.drawable.sts_5,
                isCompleted = sessionProgress[TestType.FOUR_STAGE_BALANCE] == true,
                handleClick = {
                    testViewModel.setTestType(TestType.FOUR_STAGE_BALANCE) // Ensure Enum matches
                    navController.navigate("fourStage") {
                        popUpTo("sessionDashboard") { inclusive = false }
                    }
                },
                enabled = true
            )

            TestSelectionCard(
                title = "Timed Up and Go (TUG)",
                imageResId = R.drawable.sts_30s,
                isCompleted = sessionProgress[TestType.TUG] == true,
                handleClick = {
                    testViewModel.setTestType(TestType.TUG)
                    navController.navigate("testInstruction/timed_up_and_go") {
                        popUpTo("sessionDashboard") { inclusive = false }
                    }
                },
                enabled = true
            )

            // Dynamic Gait Index (DGI) / 4-item DGI
            TestSelectionCard(
                title = "Dynamic Gait Index (DGI)",
                imageResId = R.drawable.dgi, // Image empty for now
                isCompleted = false,
                handleClick = {
                    Toast.makeText(context, "We are working on it", Toast.LENGTH_SHORT).show()
                },
                enabled = true
            )

            // Functional Gait Assessment (FGA)
            TestSelectionCard(
                title = "Functional Gait Assessment (FGA)",
                imageResId = R.drawable.fga, // Image empty for now
                isCompleted = false,
                handleClick = {
                    Toast.makeText(context, "We are working on it", Toast.LENGTH_SHORT).show()
                },
                enabled = true
            )

            // Berg Balance Scale (BBS)
            TestSelectionCard(
                title = "Berg Balance Scale (BBS)",
                imageResId = R.drawable.bbs, // Image empty for now
                isCompleted = false,
                handleClick = {
                    Toast.makeText(context, "We are working on it", Toast.LENGTH_SHORT).show()
                },
                enabled = true
            )

            // Functional Reach Test
            TestSelectionCard(
                title = "Functional Reach Test",
                imageResId = R.drawable.frt, // Image empty for now
                isCompleted = false,
                handleClick = {
                    Toast.makeText(context, "We are working on it", Toast.LENGTH_SHORT).show()
                },
                enabled = true
            )

            // Y-Balance Test
            TestSelectionCard(
                title = "Y-Balance Test",
                imageResId = R.drawable.y, // Image empty for now
                isCompleted = false,
                handleClick = {
                    Toast.makeText(context, "We are working on it", Toast.LENGTH_SHORT).show()
                },
                enabled = true
            )

            // Star Excursion Balance Test
            TestSelectionCard(
                title = "Star Excursion Balance Test",
                imageResId = R.drawable.sebt, // Image empty for now
                isCompleted = false,
                handleClick = {
                    Toast.makeText(context, "We are working on it", Toast.LENGTH_SHORT).show()
                },
                enabled = true
            )

            // Balance Evaluation Systems Test (BESTest)
            TestSelectionCard(
                title = "Balance Evaluation Systems Test",
                imageResId = R.drawable.best, // Image empty for now
                isCompleted = false,
                handleClick = {
                    Toast.makeText(context, "We are working on it", Toast.LENGTH_SHORT).show()
                },
                enabled = true
            )

            // Tinetti Test
            TestSelectionCard(
                title = "Tinetti Test",
                imageResId = R.drawable.test_image, // Image empty for now
                isCompleted = false,
                handleClick = {
                    Toast.makeText(context, "We are working on it", Toast.LENGTH_SHORT).show()
                },
                enabled = true
            )
//            TestSelectionCard(
//                title = "Testing",
//                imageResId = R.drawable.sts_30s, // Update icon if needed
//                isCompleted = sessionProgress[TestType.TUG] == true,
//                handleClick = {
//                    navController.navigate("testing") {
//                        popUpTo("sessionDashboard") { inclusive = false }
//                    }
//                },
//                enabled = true
//            )
        }
    }
}

@SuppressLint("NewApi")
@Composable
fun TestSelectionCard(title: String, imageResId: Int, handleClick: ()-> Unit, isCompleted:Boolean,enabled:Boolean) {
    // For GIFs, we use Coil's AsyncImage
    val imageLoader = ImageLoader.Builder(LocalContext.current)
        .components {
            add(ImageDecoderDecoder.Factory())
        }
        .build()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor, disabledContainerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isCompleted) BorderStroke(1.dp, Green) else null,
        onClick = handleClick,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))

            // Use AsyncImage from Coil to load the GIF
            AsyncImage(
                model = imageResId, // Your R.drawable.your_gif_file
                contentDescription = title,
                imageLoader = imageLoader, // Use the ImageLoader with GIF support
                modifier = Modifier
                    .size(width = 100.dp, height = 100.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

// Updated Bottom Bar for Session Flow
@Composable
fun FinishSessionBottomBar(
    isEnabled: Boolean,
    onFinishClick: () -> Unit
) {
    Surface(shadowElevation = 8.dp, color = Color.White) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp, 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                enabled = isEnabled,
                onClick = onFinishClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryColor,
                    disabledContainerColor = Color.Gray,
                    contentColor = WhiteColor,
                    disabledContentColor = WhiteColor
                )
            ) {
                Text("Finish Session & Save Report", fontWeight = FontWeight.Bold)
            }
        }
    }
}
