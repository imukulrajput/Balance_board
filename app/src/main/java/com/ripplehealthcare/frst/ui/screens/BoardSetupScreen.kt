package com.ripplehealthcare.frst.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ripplehealthcare.frst.R
import com.ripplehealthcare.frst.data.model.SensorData
import com.ripplehealthcare.frst.domain.model.ConnectionState
import com.ripplehealthcare.frst.ui.theme.*
import com.ripplehealthcare.frst.ui.viewmodel.BluetoothViewModel
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import androidx.compose.foundation.shape.CircleShape

private const val TARGET_PITCH = -89.42f
private const val TARGET_YAW = 0.76f

// 2. Deadzone tolerance
private const val TOLERANCE = 1.5f
private const val MAX_DISPLAY_TILT = 15.0f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardSetupScreen(
    navController: NavController,
    bluetoothViewModel: BluetoothViewModel
) {
    val context = LocalContext.current
    val connectionState by bluetoothViewModel.connectionState.collectAsState()
    val sensorData by bluetoothViewModel.sensorData.collectAsState()

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        if (permissionsMap.values.all { it }) {
            bluetoothViewModel.getPairedDevices()
            showBottomSheet = true
        } else {
            Toast.makeText(context, "Bluetooth permissions are required to connect.", Toast.LENGTH_LONG).show()
        }
    }

    // FIX: Start the data stream immediately upon successful connection
    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.CONNECTED) {
            showBottomSheet = false
            bluetoothViewModel.startTest() // <-- THIS FIXES THE GRAPH NOT SHOWING
        } else if (connectionState == ConnectionState.FAILED) {
            Toast.makeText(context, "Connection Failed", Toast.LENGTH_SHORT).show()
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            SetupDeviceSelectionSheetContent(
                bluetoothViewModel = bluetoothViewModel,
                onDeviceSelected = { device ->
                    bluetoothViewModel.connectToDevice(device)
                }
            )
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFE0F7FA), Color(0xFFB2EBF2))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
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
                        tint = Color(0xFF4A44D4),
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Board Setup",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A44D4)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SetupDeviceConnectionCard(
                    connectionState = connectionState,
                    onConnectClick = {
                        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
                        } else {
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                        permissionLauncher.launch(permissions)
                    },
                    onDisconnectClick = {
                        bluetoothViewModel.disconnect()
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                var isLevel = false

                // The graph will now show instantly because we triggered startTest() above
                if (connectionState == ConnectionState.CONNECTED && sensorData != null) {
                    val rawDeltaPitch = sensorData!!.centerPitch - TARGET_PITCH
                    val rawDeltaYaw = sensorData!!.centerYaw - TARGET_YAW

                    val effectivePitch = if (abs(rawDeltaPitch) <= TOLERANCE) 0f else rawDeltaPitch
                    val effectiveYaw = if (abs(rawDeltaYaw) <= TOLERANCE) 0f else rawDeltaYaw

                    isLevel = effectivePitch == 0f && effectiveYaw == 0f

                    MinimalAlignmentCard(effectivePitch, effectiveYaw, isLevel)
                }

                Spacer(modifier = Modifier.weight(1f))

                // Start Training Button (Only enabled when connected AND level)
                Button(
                    onClick = {
                        // Save baselines globally
                        bluetoothViewModel.centerPitch = TARGET_PITCH
                        bluetoothViewModel.centerYaw = TARGET_YAW

                        // FIX: Navigates to the correct route based on your NavGraph
                        navController.navigate("gameSelection") {
                            popUpTo("boardSetup") { inclusive = true }
                        }
                    },
                    enabled = isLevel && connectionState == ConnectionState.CONNECTED,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryColor,
                        disabledContainerColor = Color.LightGray
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = if (isLevel) "Start Training" else "Center Board to Start",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun MinimalAlignmentCard(effectivePitch: Float, effectiveYaw: Float, isLevel: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isLevel) "Board is Level" else "Please level the board",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isLevel) Color(0xFF2E7D32) else Color.DarkGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                LevelingCanvas(pitch = effectivePitch, yaw = effectiveYaw, isLevel = isLevel)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricColumn(label = "F/B Tilt", value = effectivePitch)
                MetricColumn(label = "L/R Tilt", value = effectiveYaw)
            }
        }
    }
}

@Composable
private fun LevelingCanvas(pitch: Float, yaw: Float, isLevel: Boolean) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = size.width / 2f
        val innerTargetRadius = maxRadius * (TOLERANCE / MAX_DISPLAY_TILT)

        drawCircle(color = Color.LightGray, radius = maxRadius, center = Offset(centerX, centerY), style = Stroke(width = 2.dp.toPx()))
        drawCircle(color = Color.LightGray.copy(alpha = 0.5f), radius = innerTargetRadius, center = Offset(centerX, centerY), style = Stroke(width = 1.dp.toPx()))

        drawLine(color = Color.LightGray, start = Offset(centerX - maxRadius, centerY), end = Offset(centerX + maxRadius, centerY), strokeWidth = 1.dp.toPx())
        drawLine(color = Color.LightGray, start = Offset(centerX, centerY - maxRadius), end = Offset(centerX, centerY + maxRadius), strokeWidth = 1.dp.toPx())

        val xOffset = (yaw / MAX_DISPLAY_TILT).coerceIn(-1f, 1f) * maxRadius
        val yOffset = -(pitch / MAX_DISPLAY_TILT).coerceIn(-1f, 1f) * maxRadius

        val distance = sqrt(xOffset.pow(2) + yOffset.pow(2))
        val scale = if (distance > maxRadius) maxRadius / distance else 1f

        val finalX = centerX + (xOffset * scale)
        val finalY = centerY + (yOffset * scale)

        drawCircle(
            color = if (isLevel) Color(0xFF4CAF50) else Color(0xFF616161),
            radius = 12.dp.toPx(),
            center = Offset(finalX, finalY)
        )
    }
}

@Composable
private fun MetricColumn(label: String, value: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Text(text = String.format("%.1f°", value), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable
private fun SetupDeviceConnectionCard(
    connectionState: ConnectionState,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(64.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (connectionState == ConnectionState.CONNECTED) CardColor else RedColor
        ),
        border = BorderStroke(
            1.dp,
            if (connectionState == ConnectionState.CONNECTED) Green else CardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.frst_image),
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .border(width = 0.5.dp, color = BlackBorder, shape = RoundedCornerShape(size = 492.dp)),
                contentDescription = "FRST Logo",
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = if (connectionState == ConnectionState.CONNECTED) "Connected" else "Device not connected",
                modifier = Modifier.weight(1f),
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight(500),
                    color = if (connectionState == ConnectionState.CONNECTED) Green else WhiteColor,
                    letterSpacing = 0.72.sp
                )
            )

            IconButton(
                onClick = {
                    if (connectionState == ConnectionState.CONNECTED) onDisconnectClick() else onConnectClick()
                },
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(28.dp))
            ) {
                Image(
                    painter = painterResource(
                        id = if (connectionState == ConnectionState.CONNECTED) R.drawable.remove_sign else R.drawable.add_sign
                    ),
                    contentDescription = "Connection Button",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun SetupDeviceSelectionSheetContent(
    bluetoothViewModel: BluetoothViewModel,
    onDeviceSelected: (BluetoothDevice) -> Unit
) {
    val pairedDevices by bluetoothViewModel.pairedDevices.collectAsState()
    val connectionState by bluetoothViewModel.connectionState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Select a Device to Connect", style = MaterialTheme.typography.titleMedium, color = Color.Black)
        Spacer(modifier = Modifier.height(16.dp))

        if (connectionState == ConnectionState.CONNECTING) {
            CircularProgressIndicator(color = PrimaryColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Connecting...", color = Color.Gray)
        } else {
            if (pairedDevices.isEmpty()) {
                Text("No paired devices found. Please pair your device in system settings first.", color = Color.Gray)
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(pairedDevices) { device ->
                        Card(
                            onClick = { onDeviceSelected(device) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = device.name ?: "Unknown Device", style = MaterialTheme.typography.titleMedium, color = Color.Black)
                                Text(text = device.address, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}