package com.ripplehealthcare.frst.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ripplehealthcare.frst.R
import com.ripplehealthcare.frst.domain.model.ConnectionState
import com.ripplehealthcare.frst.ui.components.TopBar
import com.ripplehealthcare.frst.ui.theme.CardColor
import com.ripplehealthcare.frst.ui.theme.Green
import com.ripplehealthcare.frst.ui.theme.PrimaryColor
import com.ripplehealthcare.frst.ui.theme.WhiteColor
import com.ripplehealthcare.frst.ui.viewmodel.BluetoothViewModel
import com.ripplehealthcare.frst.ui.viewmodel.TestViewModel

@Composable
fun CalibrationSelectionScreen(
    navController: NavController,
    viewModel: BluetoothViewModel,
    testViewModel: TestViewModel
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val standingData by testViewModel.standingCalibration.collectAsState()
    val sittingData by testViewModel.sittingCalibration.collectAsState()

    val anyDataAvailable = standingData != null || sittingData != null
    val bothDataAvailable = standingData != null && sittingData != null

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
        topBar = { TopBar("Calibration", onBackClick = {navController.popBackStack()}) },
        bottomBar = {
            SelectionBottomBar(
                isResetEnabled = anyDataAvailable,
                isTestEnabled = bothDataAvailable,
                onResetClick = {
                    testViewModel.resetCalibrationData()
                },
                onTestNowClick = {
                    navController.navigate("sessionDashboard") {
                        popUpTo("calibration"){ inclusive = true }
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CalibrationSelectionCard(
                title = "Standing Calibration",
                imageResId = R.drawable.standing_pose,
                isCompleted = standingData != null,
                onCalibrateClick = {
                    navController.navigate("standingCalibration") {
                        popUpTo("calibration")
                    }
                }
            )
            CalibrationSelectionCard(
                title = "Sitting Calibration",
                imageResId = R.drawable.sitting_pose,
                isCompleted = sittingData != null,
                onCalibrateClick = {
                    navController.navigate("sittingCalibration") {
                        popUpTo("calibration")
                    }
                }
            )
//            CalibrationSelectionCard(
//                title = "Walk Calibration",
//                imageResId = R.drawable.sitting_pose,
//                isCompleted = sittingData != null,
//                onCalibrateClick = {
//                    navController.navigate("walkCalibration") {
//                        popUpTo("calibration")
//                    }
//                }
//            )
        }
    }
}

@Composable
fun CalibrationSelectionCard(
    title: String,
    imageResId: Int,
    isCompleted: Boolean,
    onCalibrateClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor, disabledContainerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        // Add a border when completed
        border = if (isCompleted) BorderStroke(1.dp, Green) else null,
        onClick = { if (!isCompleted) onCalibrateClick() },
        enabled = !isCompleted
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = title,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SelectionBottomBar(
    isResetEnabled: Boolean,
    isTestEnabled: Boolean,
    onResetClick: () -> Unit,
    onTestNowClick: () -> Unit
) {
    val context = LocalContext.current

    Surface(shadowElevation = 8.dp, color = Color.White) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp, 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Test Now Button
            Button(
                enabled = isTestEnabled,
                onClick = onTestNowClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, disabledContainerColor = Color.Gray, contentColor= WhiteColor, disabledContentColor = WhiteColor)
            ) {
                Text("Go To Test", fontWeight = FontWeight.Bold)
            }

            // Re-Calibrate Button
            OutlinedButton(
                enabled = isResetEnabled,
                onClick = {
                    onResetClick()
                    Toast.makeText(context, "Calibration data reset", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    if (isResetEnabled) PrimaryColor else Color.Gray
                ),
                colors = ButtonDefaults.buttonColors(contentColor = PrimaryColor, disabledContentColor = Color.Gray, containerColor = Color.Transparent, disabledContainerColor = Color.Transparent)
            ) {
                Text("Re-Calibrate", fontWeight = FontWeight.Bold)
            }
        }
    }
}