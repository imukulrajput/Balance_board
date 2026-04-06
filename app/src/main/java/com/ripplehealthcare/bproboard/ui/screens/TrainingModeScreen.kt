package com.ripplehealthcare.bproboard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ripplehealthcare.bproboard.ui.components.TopBar
import com.ripplehealthcare.bproboard.R

@Composable
fun TrainingModeScreen(
    navController: NavController
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