package com.ripplehealthcare.frst.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ripplehealthcare.frst.R
import com.ripplehealthcare.frst.domain.model.AuthState
import com.ripplehealthcare.frst.ui.theme.BgPrimary
import com.ripplehealthcare.frst.ui.theme.TextBlack
import com.ripplehealthcare.frst.ui.viewmodel.AuthViewModel

@Composable
fun SplashScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            navController.navigate("main") {
                popUpTo("splash") { inclusive = true }
            }
        }else if(authState is AuthState.Idle || authState is AuthState.FirstLogin){
             navController.navigate("authScreen"){
                 popUpTo("splash") { inclusive = true }
             }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .windowInsetsPadding(WindowInsets.systemBars),
        contentAlignment = Alignment.Center
    ) {

        // --- Center Content (Logo & Slogan) ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.frst_logo),
                contentDescription = "FRST Logo",
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .heightIn(min = 120.dp, max = 200.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Every Step, Seen. Every Risk, Understood.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextBlack,
                    letterSpacing = 0.5.sp
                ),
                textAlign = TextAlign.Center
            )
        }

        // --- Bottom Content (Powered By) ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp), // Adjust padding to match the "Ripple" placement
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Powered by",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = TextBlack,
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Image(
                painter = painterResource(id = R.drawable.ripple_logo),
                contentDescription = "Ripple Healthcare Logo",
                modifier = Modifier
                    .height(50.dp)
                    .wrapContentWidth(),
                contentScale = ContentScale.Fit
            )
        }
    }
}