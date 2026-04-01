package com.ripplehealthcare.frst.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ripplehealthcare.frst.domain.model.DoctorProfile
import com.ripplehealthcare.frst.ui.components.centerDashbord.AddDoctorBottomSheet
import com.ripplehealthcare.frst.ui.components.centerDashbord.DashboardBottomNav
import com.ripplehealthcare.frst.ui.components.centerDashbord.DoctorSelectionSheet
import com.ripplehealthcare.frst.ui.theme.PrimaryColor
import com.ripplehealthcare.frst.ui.viewmodel.AuthViewModel
import com.ripplehealthcare.frst.ui.viewmodel.ManagementUiState
import com.ripplehealthcare.frst.ui.viewmodel.ManagementViewModel

enum class DashboardSheet { SELECT_DOCTOR, ADD_DOCTOR }

@Composable
fun CenterDashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    managementViewModel: ManagementViewModel
) {
    val uiState by managementViewModel.uiState.collectAsState()
    val centerProfile by authViewModel.centerProfile.collectAsState()
    val doctors by managementViewModel.doctors.collectAsState()
    val selectedDoctor by managementViewModel.selectedDoctor.collectAsState()

    var activeSheet by remember { mutableStateOf<DashboardSheet?>(null) }

    LaunchedEffect(centerProfile?.uid) {
        centerProfile?.uid?.let { uid ->
            managementViewModel.loadDoctors(uid)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is ManagementUiState.Success) {
            activeSheet = null
        }
    }

    Scaffold(
        bottomBar = { DashboardBottomNav(navController) },
        containerColor = Color(0xFFF0F4F8)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Center Dashboard",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D3142)
            )

            // 1. Center Card with Teal Accents
            CenterInfoCard(
                name = centerProfile?.centerName ?: "Loading...",
                address = centerProfile?.address?.fullAddress ?: "Address not set",
                city = centerProfile?.address?.city ?: "",
                accentColor = PrimaryColor
            )

            // 2. Management Section
            Column {
                Text(
                    text = "Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                DashboardActionButton(
                    title = "Add New Doctor",
                    subtitle = "Onboard a new physician",
                    icon = Icons.Default.PersonAdd,
                    onClick = { activeSheet = DashboardSheet.ADD_DOCTOR }
                )
            }

            // 3. Doctors List
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Our Doctors",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (doctors.size > 5) {
                        TextButton(onClick = { activeSheet = DashboardSheet.SELECT_DOCTOR }) {
                            Text("View All", color = PrimaryColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                doctors.take(5).forEach { doctor ->
                    DoctorCard(
                        doctor = doctor,
                        isSelected = doctor.id == selectedDoctor?.id,
                        accentColor = PrimaryColor,
                        onClick = {
                            managementViewModel.selectDoctor(doctor)
                            navController.navigate("doctorDashboard/${doctor.id}")
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    // Sheet Handling
    when (activeSheet) {
        DashboardSheet.SELECT_DOCTOR -> {
            DoctorSelectionSheet(
                doctors = doctors,
                selectedDoctorId = selectedDoctor?.id ,
                onDoctorSelected = {
                    managementViewModel.selectDoctor(it)
                    activeSheet = null
                    navController.navigate("doctorDashboard/${it.id}")
                },
                onAddNewDoctor = { activeSheet = DashboardSheet.ADD_DOCTOR },
                onDismiss = { activeSheet = null }
            )
        }
        DashboardSheet.ADD_DOCTOR -> {
            AddDoctorBottomSheet(
                onDismiss = { activeSheet = null },
                onAddClick = { n, p, e, g, s ->
                    centerProfile?.uid?.let { uid ->
                        managementViewModel.addNewDoctor(uid, n, p, e, g, s)
                    }
                },
                isLoading = uiState is ManagementUiState.Loading
            )
        }
        else -> Unit
    }
}
@Composable
fun CenterInfoCard(name: String, address: String, city: String, accentColor: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp), // More rounded for a modern feel
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0).copy(alpha = 0.5f)) // Subtle border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Branded Icon Container
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = accentColor.copy(alpha = 0.1f) // Matches teal theme
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2D3142),
                        letterSpacing = (-0.5).sp
                    )

                    Surface(
                        color = accentColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = city.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }
            }

            // Divider to separate identity from location
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                thickness = 0.5.dp,
                color = Color.LightGray.copy(alpha = 0.5f)
            )

            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).padding(top = 2.dp),
                    tint = accentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    lineHeight = 20.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DoctorCard(
    doctor: DoctorProfile,
    isSelected: Boolean = false,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp,
            pressedElevation = 8.dp
        ),
        // The border is the primary selection indicator
        border = if (isSelected) BorderStroke(2.dp, accentColor) else BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        // Use a Box to layer a subtle tint over the white card only when selected
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isSelected) accentColor.copy(alpha = 0.04f) else Color.Transparent)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Icon Container
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (doctor.gender == "Female") "👩‍⚕️" else "👨‍⚕️",
                            fontSize = 26.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = doctor.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF2D3142)
                    )
                    Text(
                        text = doctor.specialization,
                        color = accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Smooth transition icon
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (isSelected) accentColor else Color.LightGray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}