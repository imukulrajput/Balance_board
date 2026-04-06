package com.ripplehealthcare.bproboard.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ripplehealthcare.bproboard.domain.model.ConnectionState
import com.ripplehealthcare.bproboard.domain.model.TestDataSource
import com.ripplehealthcare.bproboard.ui.components.TopBar
import com.ripplehealthcare.bproboard.ui.theme.PrimaryColor
import com.ripplehealthcare.bproboard.ui.viewmodel.BluetoothViewModel
import kotlinx.coroutines.delay


@Composable
fun FourStageInstructionScreen(
    navController: NavController,
    stageId: String?,
    viewModel: BluetoothViewModel,
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    // 1. Fetch data for the specific stage
    val stageData = TestDataSource.getTestById(stageId)

    var countdown by remember { mutableIntStateOf(5) }
    val isButtonEnabled = countdown == 0

    // --- NEW: Countdown Timer Logic ---
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000L) // Wait 1 second
            countdown--
        }
    }

    LaunchedEffect(connectionState) {
        when (connectionState) {
            ConnectionState.DISCONNECTED -> {
                Toast.makeText(context, "Device Disconnected.", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
            else -> Unit // No action for other states
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = "Instructions",
                onBackClick = { navController.popBackStack() }
            )
        },
        bottomBar = {
            // Reusing the updated BottomBar logic
            StartButtonBottomBar(
                enabled = isButtonEnabled,
                timerValue = countdown,
                onStartClick = {
                    navController.navigate("game") {
                        popUpTo("fourStage")
                    }
                }
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { innerPadding ->
        if (stageData == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Error: Stage instructions not found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                // --- Title and Countdown Badge ---
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stageData.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        // The Countdown Badge
                        AnimatedVisibility(
                            visible = countdown > 0,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Surface(
                                color = PrimaryColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Read: ${countdown}s",
                                        color = PrimaryColor,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                // Image/GIF
                item {
                    GifDisplay(stageData.gifResId)
                    Spacer(Modifier.height(32.dp))
                }

                // Steps
                itemsIndexed(stageData.steps) { index, step ->
                    InstructionStep(number = "${index + 1}", text = step)
                    Spacer(Modifier.height(24.dp))
                }

                // Extra Warning for Balance Tests
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️ Safety First: Have a wall or sturdy chair nearby in case you lose your balance.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }

                // Extra spacer for bottom bar
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
