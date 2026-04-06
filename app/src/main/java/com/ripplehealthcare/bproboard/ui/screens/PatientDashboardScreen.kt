package com.ripplehealthcare.bproboard.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ripplehealthcare.bproboard.R
import com.ripplehealthcare.bproboard.domain.model.ConnectionState
import com.ripplehealthcare.bproboard.domain.model.DoctorProfile
import com.ripplehealthcare.bproboard.domain.model.Patient
import com.ripplehealthcare.bproboard.ui.theme.*
import com.ripplehealthcare.bproboard.ui.viewmodel.BluetoothViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.ManagementViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.PatientViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDashboardScreen(
    navController: NavController,
    patientId: String,
    patientViewModel: PatientViewModel,
    managementViewModel: ManagementViewModel,
    bluetoothViewModel: BluetoothViewModel,
    testViewModel: TestViewModel
) {
    val context = LocalContext.current
    val selectedPatient by patientViewModel.selectedPatient.collectAsState()
    val connectionState by bluetoothViewModel.connectionState.collectAsState()
    val doctor by managementViewModel.selectedDoctor.collectAsState()
    val allDoctors by managementViewModel.doctors.collectAsState()

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    var showTransferDialog by remember { mutableStateOf(false) }

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


    LaunchedEffect(patientId, doctor?.centerId) {
        doctor?.centerId?.let { centerId ->
            if (selectedPatient?.patientId != patientId) {
                patientViewModel.getPatientById(centerId, patientId)
            }
        }
    }

    LaunchedEffect(selectedPatient?.patientId) {
        if (testViewModel.patient.value.patientId != selectedPatient?.patientId) {
            selectedPatient?.let {
                testViewModel.resetForNewPatient(it)
            }
        }
    }

    LaunchedEffect(connectionState) {
        when (connectionState) {
            ConnectionState.CONNECTED -> {
                showBottomSheet = false
            }
            ConnectionState.FAILED -> Toast.makeText(context, "Connection Failed", Toast.LENGTH_SHORT).show()
            else -> Unit // No action for other states
        }
    }

    LaunchedEffect(Unit) {
        if (connectionState == ConnectionState.CONNECTED) {
            bluetoothViewModel.stopTest()
        }
    }

    if (showTransferDialog && doctor != null) {
        TransferPatientDialog(
            currentDoctorId = doctor!!.id,
            doctors = allDoctors,
            onDismiss = { showTransferDialog = false },
            onConfirm = { targetDoctor ->
                patientViewModel.transferPatient(
                    patientId = patientId,
                    centerId = doctor!!.centerId,
                    newDoctorId = targetDoctor.id,
                    onComplete = {
                        // Success callback: Exit to patient list
                        navController.navigate("patientList") {
                            popUpTo("patientList") { inclusive = true }
                        }
                    }
                )
                showTransferDialog = false
            }
        )
    }


    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            DeviceSelectionSheetContent(
                bluetoothViewModel = bluetoothViewModel,
                onDeviceSelected = { device ->
                    bluetoothViewModel.connectToDevice(device)
                }
            )
        }
    }

    Scaffold(
        topBar = { TopBar(navController) },
        containerColor = Color(0xFFF0F4F8)
    ) { innerPadding ->
        if (selectedPatient == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryColor)
            }
        } else {
            PatientDashboardContent(
                modifier = Modifier.padding(innerPadding),
                navController = navController,
                bluetoothViewModel = bluetoothViewModel,
                testViewModel = testViewModel,
                patient = selectedPatient!!,
                connectionState = connectionState,
                onConnectClick = {
                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
                    } else {
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    permissionLauncher.launch(permissions)
                },
                onEditClick = {
                    patientViewModel.loadPatientIntoForm(selectedPatient!!)
                    navController.navigate("patientOnboarding?isEdit=true")
                },
                onTransferClick = {
                    managementViewModel.loadDoctors(doctor!!.centerId)
                    showTransferDialog = true
                }
            )
        }
    }
}

@Composable
private fun PatientDashboardContent(
    modifier: Modifier = Modifier,
    navController: NavController,
    bluetoothViewModel: BluetoothViewModel,
    testViewModel: TestViewModel,
    patient: Patient,
    connectionState: ConnectionState,
    onConnectClick: () -> Unit,
    onEditClick: () -> Unit,
    onTransferClick: () -> Unit
) {
    val standingCalibration by testViewModel.standingCalibration.collectAsState()
    val sittingCalibration by testViewModel.sittingCalibration.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Re-used from HomeScreen
        DeviceConnectionCard(bluetoothViewModel, onConnectClick)

        // Read-only patient details
        PatientDetailCard(patient = patient, onEditClick, onTransferClick)

        // Re-used from HomeScreen
        ActionCard(
            title = "Calibration",
            subtitle = "Perform initial calibration",
            enabled = connectionState == ConnectionState.CONNECTED,
            imageSrc = R.drawable.sitting_image,
            imageDescription = "Calibration Image",
            handleClick = { navController.navigate("calibration") }
        )

        ActionCard(
            title = "Balance Test",
            subtitle = "Check patient's balance level",
            enabled = connectionState == ConnectionState.CONNECTED && standingCalibration!=null && sittingCalibration!=null ,
            imageSrc = R.drawable.standing_image,
            imageDescription = "Balance Test Image",
            handleClick = { navController.navigate("sessionDashboard") }
        )
        ActionCard(
            title = "Rehabilitation",
            subtitle = "Play some games to increase your strength",
            enabled = connectionState == ConnectionState.CONNECTED,
            imageSrc = R.drawable.exercise_image,
            imageDescription = "Exercise Image",
            handleClick = { navController.navigate("gameSelection") }
        )
        ActionCard(
            title = "View Previous Test Reports",
            subtitle = "See all previous test results",
            enabled = true, // Always enabled
            imageSrc = R.drawable.reports_history, // Use an appropriate icon for reports
            imageDescription = "Test Reports Icon",
            handleClick = {
//                navController.navigate("testHistory")
                navController.navigate("reportSelection")
            }
        )
    }
}

@Composable
private fun PatientDetailCard(patient: Patient, onEditClick: () -> Unit, onTransferClick: () -> Unit){
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Patient Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = PrimaryColor)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Information") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = {
                                showMenu = false
                                onEditClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Transfer Patient", color = Color.Red) },
                            leadingIcon = { Icon(Icons.Default.SwapHoriz, null, tint = Color.Red) },
                            onClick = {
                                showMenu = false
                                onTransferClick()
                            }
                        )
                    }
                }
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            InfoRow(label = "Name", value = patient.name)
            InfoRow(label = "Age", value = patient.age)
            InfoRow(label = "Sex", value = patient.gender)
            InfoRow(label = "Phone", value = patient.phone)
            InfoRow(label = "Email", value = patient.email.ifEmpty { "N/A" })
            InfoRow(label = "Height", value = "${patient.height} cm")
            InfoRow(label = "Weight", value = "${patient.weight} kg")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(80.dp)
        )
        Text(text = value, color = Color.Gray)
    }
}

@Composable
fun DeviceConnectionCard(bluetoothViewModel: BluetoothViewModel, onConnectClick: () -> Unit) {
    val connectionState by bluetoothViewModel.connectionState.collectAsState()

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
                    if (connectionState == ConnectionState.CONNECTED) {
                        bluetoothViewModel.disconnect()
                    } else {
                        onConnectClick()
                    }
                },
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(28.dp))
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
fun DeviceSelectionSheetContent(
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
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pairedDevices) { device ->
                        Card(
                            onClick = { onDeviceSelected(device) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)) // Light Gray for items
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = device.name ?: "Unknown Device",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.Black
                                )
                                Text(
                                    text = device.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar( navController: NavController) {
    TopAppBar(
        title = {
            Image(
                painter = painterResource(id = R.drawable.frst_logo),
                contentDescription = "FRST Logo",
                modifier = Modifier.height(24.dp)
            )
        },
        actions = {
            IconButton(onClick = { navController.navigate("main"){
                popUpTo("main")
            } }) {
                Icon(
                    modifier = Modifier.size(32.dp),
                    imageVector = Icons.Default.Dashboard,
                    contentDescription = "Notifications",
                    tint = Color.Gray
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    imageSrc: Int,
    imageDescription: String,
    handleClick: () -> Unit,
    enabled: Boolean = true,
    completed: Boolean = false
) {
    var modifier = Modifier.fillMaxWidth()

    if (completed) {
        modifier = modifier.border(
            width = 1.dp,
            color = Color(0xFF34C759),
            shape = RoundedCornerShape(16.dp)
        )
    }

    Card(
        onClick = handleClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardColor,
            disabledContainerColor = CardColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        enabled = enabled
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(imageSrc),
                contentDescription = imageDescription,
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = subtitle, fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun TransferPatientDialog(
    currentDoctorId: String,
    doctors: List<DoctorProfile>,
    onDismiss: () -> Unit,
    onConfirm: (DoctorProfile) -> Unit
) {
    var selectedDoctor by remember { mutableStateOf<DoctorProfile?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer Patient") },
        text = {
            Column {
                Text("Select the doctor you wish to transfer this patient to:")
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    // Filter out the current doctor from the list
                    items(doctors.filter { it.id != currentDoctorId }) { doctor ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDoctor = doctor }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedDoctor?.id == doctor.id,
                                onClick = { selectedDoctor = doctor },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryColor)
                            )
                            Text(text = "Dr. ${doctor.name}", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedDoctor?.let { onConfirm(it) } },
                enabled = selectedDoctor != null,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) {
                Text("Confirm Transfer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}