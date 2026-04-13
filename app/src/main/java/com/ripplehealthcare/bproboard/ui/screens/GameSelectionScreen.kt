package com.ripplehealthcare.bproboard.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ripplehealthcare.bproboard.domain.model.ConnectionState
import com.ripplehealthcare.bproboard.ui.viewmodel.BluetoothViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel

@Composable
fun GameSelectionScreen(
    navController: NavController,
    bluetoothViewModel: BluetoothViewModel,
    testViewModel: TestViewModel
) {
    val context = LocalContext.current
    val connectionState by bluetoothViewModel.connectionState.collectAsState()
    val patient = testViewModel.patient.collectAsState().value

    // NEW: State to show the Sitting/Standing popup
    var showPostureDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        bluetoothViewModel.startTest()

        testViewModel.startNewSession()
    }

    val primaryTextColor = Color(0xFF4A44D4)
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFE0F7FA), Color(0xFFB2EBF2))
    )

    // Helper function to handle strict navigation logic AND trigger the popup
    val navigateIfConnected = { route: String ->
        if (connectionState == ConnectionState.CONNECTED) {
            if (route == "trainingModeSelection") {
                showPostureDialog = true // Trigger popup instead of navigating immediately!
            } else {
                navController.navigate(route)
            }
        } else {
            Toast.makeText(context, "First do Board Setup to connect the device.", Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CustomTopBar(
                navController = navController,
                textColor = primaryTextColor
            )

            // Dynamic Greeting
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Patient Greeting
                Text(
                    text = "Hi, ${patient.name}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor
                )
                Text(
                    text = "Age: ${patient.age}  |  Phone: ${patient.phone}",
                    fontSize = 18.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                // Sleek Board Setup Button
                BoardSetupButton(
                    connectionState = connectionState,
                    onClick = { navController.navigate("boardSetup") }
                )
            }

            // Center the cards in the remaining screen space
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryCard(
                        title = "Training\nMode",
                        icon = Icons.Default.FitnessCenter,
                        backgroundColor = Color(0xFF2196F3),
                        onClick = { navigateIfConnected("trainingModeSelection") }
                    )

                    CategoryCard(
                        title = "Game\nmode",
                        icon = Icons.Default.VideogameAsset,
                        backgroundColor = Color(0xFF5C6BC0),
                        onClick = { navigateIfConnected("gameModeSelection") }
                    )

                    CategoryCard(
                        title = "Graph\nView",
                        icon = Icons.Default.ShowChart,
                        backgroundColor = Color(0xFFAB47BC),
                        // FIX: Navigates directly, bypassing the connection check!
                        onClick = { navController.navigate("graphViewSelection") }
                    )
                }
            }
        }
    }

    // --- NEW: THE POSTURE POPUP DIALOG ---
    if (showPostureDialog) {
        AlertDialog(
            onDismissRequest = { showPostureDialog = false },
            title = { Text("Select Posture 🪑🧍") },
            text = { Text("How will the patient perform these training exercises?") },
            confirmButton = {
                Button(
                    onClick = {
                        testViewModel.trainingPosture = "STANDING"
                        showPostureDialog = false
                        navController.navigate("trainingModeSelection")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)) // Green
                ) { Text("Standing") }
            },
            dismissButton = {
                Button(
                    onClick = {
                        testViewModel.trainingPosture = "SITTING"
                        showPostureDialog = false
                        navController.navigate("trainingModeSelection")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)) // Blue
                ) { Text("Sitting") }
            }
        )
    }
}

@Composable
fun CustomTopBar(
    navController: NavController,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clickable { navController.popBackStack() }
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = textColor,
                modifier = Modifier.size(32.dp)
            )
        }

        TextButton(onClick = {
            navController.navigate("home") { popUpTo(0) }
        }) {
            Text("Home", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CategoryCard(
    title: String,
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(240.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .size(96.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 10.dp)
            )

            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

@Composable
private fun BoardSetupButton(
    connectionState: ConnectionState,
    onClick: () -> Unit
) {
    val isConnected = connectionState == ConnectionState.CONNECTED

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.5f)
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isConnected) Color(0xFF4CAF50) else Color(0xFFE53935)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Bluetooth,
            contentDescription = "Bluetooth Icon",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = if (isConnected) "Board Connected" else "Board Setup",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}