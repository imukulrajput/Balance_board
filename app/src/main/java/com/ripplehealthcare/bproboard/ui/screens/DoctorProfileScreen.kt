package com.ripplehealthcare.bproboard.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ripplehealthcare.bproboard.ui.components.BottomNavigation
import com.ripplehealthcare.bproboard.ui.theme.PrimaryColor
import com.ripplehealthcare.bproboard.ui.theme.TextBlack
import com.ripplehealthcare.bproboard.ui.viewmodel.ManagementViewModel

@Composable
fun DoctorProfileScreen(
    navController: NavController,
    managementViewModel: ManagementViewModel
) {
    val selectedDoctor by managementViewModel.selectedDoctor.collectAsState()

    Scaffold(
        bottomBar = {
            selectedDoctor?.let { doctor ->
                BottomNavigation(navController = navController, doctorId = doctor.id)
            }
        },
        containerColor = Color(0xFFF0F4F8)
    ) { paddingValues ->
        val doctor = selectedDoctor

        if (doctor == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryColor)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Doctor Profile",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight(500),
                    color = TextBlack
                )

                ProfileImage()

                // Doctor-specific Information Cards
                ProfileInfoCard(text = "Name: Dr. ${doctor.name}")
                ProfileInfoCard(text = "Gender: ${doctor.gender}")
                ProfileInfoCard(text = "Specialty: ${doctor.specialization}")
                ProfileInfoCard(text = "Phone: ${doctor.phone}")
                ProfileInfoCard(text = "Email: ${doctor.email}")
                ProfileInfoCard(text = "Center ID: ${doctor.centerId}")

                Spacer(modifier = Modifier.height(24.dp))

                // Back Button to Dashboard
                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                ) {
                    Text("Back to Dashboard", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            textAlign = TextAlign.Start, // Changed to Start for better readability
            fontSize = 16.sp,
            color = TextBlack
        )
    }
}