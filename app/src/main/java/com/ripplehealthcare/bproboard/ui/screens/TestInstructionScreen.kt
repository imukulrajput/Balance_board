package com.ripplehealthcare.bproboard.ui.screens

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import com.ripplehealthcare.bproboard.domain.model.ConnectionState
import com.ripplehealthcare.bproboard.domain.model.TestDataSource
import com.ripplehealthcare.bproboard.domain.model.TestType
import com.ripplehealthcare.bproboard.ui.viewmodel.BluetoothViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel
import com.ripplehealthcare.bproboard.ui.components.TopBar
import com.ripplehealthcare.bproboard.ui.theme.PrimaryColor
import com.ripplehealthcare.bproboard.ui.theme.WhiteColor
import kotlinx.coroutines.delay

@Composable
fun TestInstructionScreen(
    navController: NavController,
    testId: String?,
    viewModel: BluetoothViewModel,
    testViewModel: TestViewModel,
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()

    val testData = TestDataSource.getTestById(testId)

    val currentTestType by testViewModel.currentTestType.collectAsState()

    var countdown by remember { mutableIntStateOf(5) }
    val isButtonEnabled = countdown == 0

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000L) // Wait for 1 second
            countdown--
        }
    }

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
        topBar = { TopBar("Test Instructions", onBackClick = { navController.popBackStack() }) },
        bottomBar = {
            StartButtonBottomBar(
                enabled = isButtonEnabled, // Pass the enabled state
                timerValue = countdown,
                onStartClick = {
                    testViewModel.resetTestData(true)
                    if (currentTestType == TestType.TUG) {
                        navController.navigate("tug") { popUpTo("sessionDashboard") }
                    } else {
                        navController.navigate("sitToStandTest") { popUpTo("sessionDashboard") }
                    }
                }
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { innerPadding ->
        if (testData == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Error: Test instructions not found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                // --- Title and Countdown Badge ---
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = testData.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        // The Countdown Badge
                        AnimatedVisibility(
                            visible = countdown > 0,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Surface(
                                color = PrimaryColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Read: ${countdown}s",
                                        color = PrimaryColor,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    GifDisplay(testData.gifResId)
                    Spacer(Modifier.height(32.dp))
                }
                itemsIndexed(testData.steps) { index, step ->
                    InstructionStep(number = "${index + 1}", text = step)
                    Spacer(Modifier.height(24.dp))
                }
                // Extra spacer at bottom so content isn't hidden by bottom bar
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}


// ... (GifDisplay, InstructionStep, StartButtonBottomBar same as before)
@SuppressLint("NewApi")
@Composable
fun GifDisplay(@DrawableRes gifResId: Int) {
    val imageLoader = ImageLoader.Builder(LocalContext.current)
        .components { add(ImageDecoderDecoder.Factory()) }
        .build()

    AsyncImage(
        model = gifResId,
        contentDescription = "Test Animation",
        imageLoader = imageLoader,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.25f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
    )
}

@Composable
fun InstructionStep(number: String, text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            Text(number, fontWeight = FontWeight.Bold, color = Color.Black)
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 24.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun StartButtonBottomBar(
    enabled: Boolean,
    timerValue: Int,
    onStartClick: () -> Unit
) {
    Surface(shadowElevation = 8.dp, color = Color.White) {
        Button(
            onClick = onStartClick,
            enabled = enabled, // Control enablement here
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp, 20.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryColor,
                disabledContainerColor = Color.Gray.copy(alpha = 0.5f), // Dimmed when disabled
                contentColor = WhiteColor,
                disabledContentColor = WhiteColor
            )
        ) {
            if (enabled) {
                Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            } else {
                Text("Read Instructions (${timerValue})", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}