package com.ripplehealthcare.frst.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ripplehealthcare.frst.R
import com.ripplehealthcare.frst.ui.components.TopBar

@Composable
fun GameModeScreen(
    navController: NavController
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopBar(
                title = "Game Mode",
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
                title = "Starship defender",
                imageResId = R.drawable.space_debris,
                isCompleted = false,
                handleClick = {
                    navController.navigate("starship") {
                        popUpTo("gameModeSelection") { inclusive = false }
                    }
                },
                enabled = true
            )

            TestSelectionCard(
                title = "Color Sorter",
                imageResId = R.drawable.space_debris,
                isCompleted = false,
                handleClick = {
                    navController.navigate("colorsorter") {
                        popUpTo("gameModeSelection") { inclusive = false }
                    }
                },
                enabled = true
            )



            TestSelectionCard(
                title = "Rat Puzzle",
                imageResId = R.drawable.space_debris,
                isCompleted = false,
                handleClick = {
                    navController.navigate("ratPuzzle") {
                        popUpTo("gameModeSelection") { inclusive = false }
                    }
                },
                enabled = true
            )

            TestSelectionCard(
                title = "Step Game",
                imageResId = R.drawable.space_debris,
                isCompleted = false,
                handleClick = {
                    navController.navigate("stepgame") {
                        popUpTo("gameModeSelection") { inclusive = false }
                    }
                },
                enabled = true
            )

            TestSelectionCard(
                title = "Hole Navigator",
                imageResId = R.drawable.space_debris,
                isCompleted = false,
                handleClick = {
                    navController.navigate("holenavigator") {
                        popUpTo("gameModeSelection") { inclusive = false }
                    }
                },
                enabled = true
            )
        }
    }
}