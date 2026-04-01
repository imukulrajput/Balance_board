package com.ripplehealthcare.frst.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ripplehealthcare.frst.ui.components.centerDashbord.DashboardBottomNav
import com.ripplehealthcare.frst.ui.theme.PrimaryColor
import com.ripplehealthcare.frst.ui.theme.RedColor
import com.ripplehealthcare.frst.ui.theme.TextBlack
import com.ripplehealthcare.frst.ui.viewmodel.AuthViewModel

@Composable
fun ProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val centerProfile by authViewModel.centerProfile.collectAsState()
    var showSignOutDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    if (showSignOutDialog) {
        SignOutConfirmationDialog(
            onConfirm = {
                authViewModel.signOut()
                showSignOutDialog = false
                navController.navigate("splash") {
                    popUpTo("main") { inclusive = true }
                }
            },
            onDismiss = { showSignOutDialog = false }
        )
    }

    Scaffold(
        bottomBar = {
                DashboardBottomNav(navController = navController)
        },
        containerColor = Color(0xFFF0F4F8)
    ) { paddingValues ->
        val profile = centerProfile

        if (profile == null) {
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
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Center Profile",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight(500),
                    color = TextBlack
                )

                ProfileImage()

                // Center Information Cards
                ProfileCard(text = "Center Name: ${profile.centerName}") { }

                ProfileCard(text = "Contact No.: ${profile.contactPhone}") { }

                ProfileCard(text = "Email: ${profile.adminEmail}") { }

                ProfileCard(text = "Center ID: ${profile.uid}") {
                }

                ProfileCard(text = "Privacy Policies") {
                    uriHandler.openUri("https://www.ripplehealthcare.in/privacy")
                }

                ProfileCard(text = "Help and Feedback") { /* Navigate to Help */ }

                ProfileCard(text = "Log Out", isDestructive = true) {
                    showSignOutDialog = true
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * A reusable composable for the rounded rectangular cards seen in the design.
 */
@Composable
private fun ProfileCard(
    text: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth() // Ensure the text container fills the card
                .padding(16.dp),
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            color = if (isDestructive) RedColor else TextBlack
        )
    }
}

/**
 * Composable for the circular profile image with an overlaid edit button.
 */
@Composable
fun ProfileImage() {
    Box(
        modifier = Modifier.padding(vertical = 8.dp),
    ) {
        // Main profile icon
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Profile Picture",
            modifier = Modifier.size(120.dp),
            tint = PrimaryColor
        )
    }
}

/**
 * Composable for the sign-out confirmation dialog.
 */
@Composable
private fun SignOutConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Log Out") },
        text = { Text("Are you sure you want to log out?") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Log Out")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}