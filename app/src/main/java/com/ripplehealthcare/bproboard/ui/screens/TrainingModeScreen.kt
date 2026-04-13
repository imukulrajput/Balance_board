package com.ripplehealthcare.bproboard.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ripplehealthcare.bproboard.ui.components.TopBar
import com.ripplehealthcare.bproboard.R
import com.ripplehealthcare.bproboard.ui.components.TestSelectionCard
import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel

@Composable
fun TrainingModeScreen(
    navController: NavController,
    testViewModel: TestViewModel
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopBar(
                title = "Training Mode",
                onBackClick = {
                    navController.popBackStack()
                }
            )
        },
        containerColor = Color(0xFFF8F9FA),
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                OutlinedButton(
                    onClick = {
                        testViewModel.endSession()
                        // Pop all the way back to the Doctor's Dashboard
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    border = BorderStroke(2.dp, Color(0xFFE53935)), // Red border
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "END PATIENT SESSION",
                        color = Color(0xFFE53935),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            TestSelectionCard(
                title = "Static Balance",
                imageResId = R.drawable.space_debris,
                isCompleted = false,
                handleClick = {
                    navController.navigate("staticBalance") {
                        popUpTo("trainingModeSelection") { inclusive = false }
                    }
                },
                enabled = true
            )

            TestSelectionCard(
                title = "Shape Training",
                imageResId = R.drawable.space_debris,
                isCompleted = false,
                handleClick = {
                    navController.navigate("shapeTraining") {
                        popUpTo("trainingModeSelection") { inclusive = false }
                    }
                },
                enabled = true
            )

            TestSelectionCard(
                title = "Pattern Drawing",
                imageResId = R.drawable.space_debris,
                isCompleted = false,
                handleClick = {
                    navController.navigate("patternDrawing") {
                        popUpTo("trainingModeSelection") { inclusive = false }
                    }
                },
                enabled = true
            )
        }
    }
}