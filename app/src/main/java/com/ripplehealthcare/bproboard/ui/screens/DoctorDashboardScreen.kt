package com.ripplehealthcare.bproboard.ui.screens

import androidx.compose.foundation.Image
import com.ripplehealthcare.bproboard.ui.viewmodel.ManagementViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ripplehealthcare.bproboard.R
import com.ripplehealthcare.bproboard.ui.components.BottomNavigation
import com.ripplehealthcare.bproboard.ui.theme.CardColor
import com.ripplehealthcare.bproboard.ui.theme.PrimaryColor
import com.ripplehealthcare.bproboard.ui.viewmodel.AuthViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.ManagementUiState

@Composable
fun DoctorDashboardScreen(
    doctorId: String,
    navController: NavController,
    managementViewModel: ManagementViewModel,
    authViewModel: AuthViewModel
) {
    val selectedDoctor by managementViewModel.selectedDoctor.collectAsState()
    val uiState by managementViewModel.uiState.collectAsState()
    val center by authViewModel.centerProfile.collectAsState()

    LaunchedEffect(doctorId) {
        if (managementViewModel.selectedDoctor.value?.id != doctorId) {
            center?.let { managementViewModel.getDoctorById(it.uid, doctorId) }
        }
    }

    Scaffold(
        topBar = { TopBar(navController) },
        bottomBar = { BottomNavigation(navController, doctorId) },
        containerColor = Color(0xFFF0F4F8)
    ) { innerPadding ->
        // Use a Box to overlay the loading spinner over the content area
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (uiState) {
                is ManagementUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = PrimaryColor
                    )
                }
                is ManagementUiState.Error -> {
                    Text(
                        text = (uiState as ManagementUiState.Error).message,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    selectedDoctor?.let { doctor ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Text(
                                text = "Welcome, Dr. ${doctor.name}!",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )

                            // Profile Information
                            DoctorDetailsCard(
                                name = "Dr. ${doctor.name}",
                                email = doctor.email,
                                phone = doctor.phone
                            )

                            // Actions
                            DashboardActionButton(
                                title = "Add New Patient",
                                subtitle = "Create a new patient record",
                                icon = Icons.Default.PersonAdd,
                                onClick = { navController.navigate("patientOnboarding") }
                            )

                            DashboardActionButton(
                                title = "View Patient List",
                                subtitle = "Browse patients under your care",
                                icon = Icons.Default.Groups,
                                onClick = { navController.navigate("patientList") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorDetailsCard(name: String, email: String, phone: String) {
    ElevatedCard( // Changed from Card to ElevatedCard
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Added elevation
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = "Doctor Profile",
                modifier = Modifier.size(80.dp),
                tint = PrimaryColor
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = "Email",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = email, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Phone,
                        contentDescription = "Phone",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = phone, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun DashboardActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardColor,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(48.dp),
                tint = PrimaryColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = subtitle, fontSize = 14.sp, color = Color.Gray)
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