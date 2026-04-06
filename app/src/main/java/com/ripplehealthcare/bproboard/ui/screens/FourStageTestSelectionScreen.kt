package com.ripplehealthcare.bproboard.ui.screens

import FourStageResult
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ripplehealthcare.bproboard.R
import com.ripplehealthcare.bproboard.domain.model.ConnectionState
import com.ripplehealthcare.bproboard.ui.components.SavingDialog
import com.ripplehealthcare.bproboard.ui.components.TopBar
import com.ripplehealthcare.bproboard.ui.theme.PrimaryColor
import com.ripplehealthcare.bproboard.ui.theme.WhiteColor
import com.ripplehealthcare.bproboard.ui.viewmodel.BluetoothViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.PatientViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel

@Composable
fun FourStageTestSelectionScreen(
    navController: NavController,
    testViewModel: TestViewModel,
    bluetoothViewModel: BluetoothViewModel,
    patientViewModel: PatientViewModel
) {
    val context = LocalContext.current
    val connectionState by bluetoothViewModel.connectionState.collectAsState()

    // Observe stage results from TestViewModel
    val stageResults by testViewModel.stageResults.collectAsState()

    val isSaving by testViewModel.isSaving.collectAsState()
    val isSavedToBackend by testViewModel.isSavedToBackend.collectAsState()
    val patient by patientViewModel.selectedPatient.collectAsState()

    // Derive completion status
    val isStage1Completed = stageResults.containsKey(1)
    val isStage2Completed = stageResults.containsKey(2)
    val isStage3Completed = stageResults.containsKey(3)
    val isStage4Completed = stageResults.containsKey(4)

    // Allow saving if AT LEAST one stage is done (e.g. if patient fails stage 2, we still save)
    val isAnyCompleted = stageResults.isNotEmpty()

    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.DISCONNECTED) {
            Toast.makeText(context, "Device Disconnected.", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }

    LaunchedEffect(isSavedToBackend) {
        if (isSavedToBackend) {
            testViewModel.resetTestData(keepTestType = true)
            navController.popBackStack("sessionDashboard", inclusive = false)
        }
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopBar(
                title = "4 Stage Balance Test",
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Stage Cards ---

            // Stage 1: Feet Together
            TestSelectionCard(
                title = "1. Feet Together",
                imageResId = R.drawable.both_feet,
                handleClick = {
                    testViewModel.setStage(1)
                    // Ensure your nav graph points 'fourStageInstruction/...' to the Instruction Screen
                    navController.navigate("fourStageInstruction/stage_1")
                },
                isCompleted = isStage1Completed,
                enabled = true
            )

            // Stage 2: Semi-Tandem
            TestSelectionCard(
                title = "2. Semi-Tandem Stand",
                imageResId = R.drawable.semi_tendem_stand,
                handleClick = {
                    testViewModel.setStage(2)
                    navController.navigate("fourStageInstruction/stage_2")
                },
                isCompleted = isStage2Completed,
                // Optional: Enable only if Stage 1 is done
                enabled = true
            )

            // Stage 3: Tandem
            TestSelectionCard(
                title = "3. Tandem Stand",
                imageResId = R.drawable.tandem_stand,
                handleClick = {
                    testViewModel.setStage(3)
                    navController.navigate("fourStageInstruction/stage_3")
                },
                isCompleted = isStage3Completed,
                enabled = true
            )

            // Stage 4: One Leg Stand
            TestSelectionCard(
                title = "4. One Leg Stand",
                imageResId = R.drawable.one_leg,
                handleClick = {
                    testViewModel.setStage(4)
                    navController.navigate("fourStageInstruction/stage_4")
                },
                isCompleted = isStage4Completed,
                enabled = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- SAVE BUTTON (UPDATED) ---
            Button(
                onClick = {
                    // 1. Create the Result Object from the Map
                    val stagesList = stageResults.values.toList()
                    val test = FourStageResult(stages = stagesList)

                    // 2. Save to the Active Session via TestViewModel
                    patient?.let { testViewModel.saveFourStageResult(it.centerId, it.doctorId, test) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isAnyCompleted, // Enable if at least one stage is done
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryColor,
                    disabledContainerColor = Color.Gray,
                    contentColor = WhiteColor,
                    disabledContentColor = WhiteColor
                )
            ) {
                Text(
                    text = "Save 4-Stage Result",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        SavingDialog(isSaving)
    }
}