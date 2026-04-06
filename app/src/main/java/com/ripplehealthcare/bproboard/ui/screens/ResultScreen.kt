package com.ripplehealthcare.bproboard.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ripplehealthcare.bproboard.domain.model.TestType
import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit
import com.ripplehealthcare.bproboard.ui.components.TopBar
import com.ripplehealthcare.bproboard.ui.theme.PrimaryColor
import com.ripplehealthcare.bproboard.ui.theme.TextBlack
import com.ripplehealthcare.bproboard.ui.theme.WhiteColor

@Composable
fun ResultScreen(
    navController: NavController,
    testViewModel: TestViewModel // Pass the ViewModel to access results
) {
    // --- Collect state from the ViewModel ---
    val testType by testViewModel.currentTestType.collectAsState()
    val timerValue by testViewModel.timer.collectAsState()
    val thirtySecondReps by testViewModel.thirtySecondsTotalReps.collectAsState()

    // --- Generate the dynamic result text ---
    val resultText = when (testType) {
        TestType.FIVE_REPS -> "You completed 5 Sit-to-Stand repetitions in just $timerValue seconds! Keep up the great work! 💪"
        TestType.THIRTY_SECONDS -> "You completed $thirtySecondReps Sit-to-Stand repetitions in 30 seconds! Keep up the great work! 💪"
        else -> "Test results are not available." // Fallback text
    }

    val party = remember {
        Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
            emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
            position = Position.Relative(0.5, 0.1)
        )
    }

    Scaffold(
        topBar = { TopBar("", onBackClick = {
            navController.popBackStack()}) },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            KonfettiView(
                parties = listOf(party),
                modifier = Modifier.fillMaxSize()
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding()
            ) {
                Text(
                    text = "Well Done !",
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = PrimaryColor
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = resultText, // Use the dynamic text here
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp,
                    color = TextBlack,
                    modifier = Modifier.padding(20.dp)
                )
                Button(
                    onClick = {
                        navController.navigate("reportDetail"){
                            popUpTo("sessionDashboard"){inclusive=false}
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp, 20.dp).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, disabledContainerColor = Color.Gray, contentColor= WhiteColor, disabledContentColor = WhiteColor)
                ) {
                    Text("See Report", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = {
                        navController.navigate("sessionDashboard"){
                            popUpTo("sessionDashboard"){inclusive=true}
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp, 20.dp).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, PrimaryColor),
                    colors = ButtonDefaults.buttonColors(contentColor = PrimaryColor, disabledContentColor = Color.Gray, containerColor = Color.Transparent, disabledContainerColor = Color.Transparent)
                ) {
                    Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}