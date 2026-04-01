package com.ripplehealthcare.frst.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ripplehealthcare.frst.ui.components.TopBar
import com.ripplehealthcare.frst.ui.theme.PrimaryColor
import com.ripplehealthcare.frst.ui.viewmodel.BluetoothViewModel
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TestingScreen(
    navController: NavController,
    viewModel: BluetoothViewModel
) {
    // 1. Observe Sensor Data
    val sensorData by viewModel.sensorData.collectAsState()

    // 2. Tab State
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Waist (Center)", "Left Leg", "Right Leg")

    // 3. Determine which value to show based on tab
    // IMPORTANT: Ensure your SensorData model has these fields (or map X/Y/Z to Roll here)
    val currentRoll = when(selectedTab) {
        0 -> sensorData?.centerRoll ?: 0f
        1 -> sensorData?.leftRoll ?: 0f
        2 -> sensorData?.rightRoll ?: 0f
        else -> 0f
    }

    Scaffold(
        topBar = {
            TopBar(
                title = "Orientation Test",
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- TABS ---
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = PrimaryColor,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PrimaryColor
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // --- GAUGE CARD ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Live Orientation",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )

                    RollSpeedometer(
                        currentRoll = currentRoll,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}



@Composable
fun RollSpeedometer(
    currentRoll: Float, // Value between -180 and 180
    modifier: Modifier = Modifier
) {
    // Smooth animation for needle movement
    val animatedRoll by animateFloatAsState(
        targetValue = currentRoll,
        animationSpec = tween(durationMillis = 100),
        label = "NeedleAnim"
    )

    Box(modifier = modifier.aspectRatio(1f).padding(16.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2, height / 2)
            val radius = size.minDimension / 2 - 20.dp.toPx()

            // --- Configuration ---
            val startAngle = 135f
            val sweepAngle = 270f
            val minVal = -180f
            val maxVal = 180f

            // Helper to map Roll value to Angle degrees
            fun valueToAngle(value: Float): Float {
                val fraction = (value - minVal) / (maxVal - minVal)
                return startAngle + (fraction * sweepAngle)
            }

            // 1. Draw Background Arc
            drawArc(
                color = Color.LightGray.copy(alpha = 0.3f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
            )

            // 2. Draw Active Arc (Optional - color based on angle)
            // For roll, maybe Green is near 0 (flat), Yellow/Red at extremes
            // Here we just draw a solid primary color arc
            drawArc(
                color = Color(0xFF2196F3).copy(alpha = 0.2f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
            )

            // 3. Draw Ticks & Labels
            val step = 45 // Tick every 45 degrees
            for (i in -180..180 step step) {
                val angle = valueToAngle(i.toFloat())
                val rad = Math.toRadians(angle.toDouble())

                // Tick Line
                val start = center + Offset(
                    (radius - 30f) * cos(rad).toFloat(),
                    (radius - 30f) * sin(rad).toFloat()
                )
                val end = center + Offset(
                    radius * cos(rad).toFloat(),
                    radius * sin(rad).toFloat()
                )

                drawLine(
                    color = if (i == 0) Color.Black else Color.Gray,
                    start = start,
                    end = end,
                    strokeWidth = if (i == 0) 4.dp.toPx() else 2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Text Label
                val textRadius = radius - 60f
                val textPos = center + Offset(
                    textRadius * cos(rad).toFloat(),
                    textRadius * sin(rad).toFloat()
                )

                drawContext.canvas.nativeCanvas.drawText(
                    i.toString(),
                    textPos.x,
                    textPos.y + 10f, // approximate vertical center
                    Paint().apply {
                        color = android.graphics.Color.DKGRAY
                        textSize = 32f
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.DEFAULT_BOLD
                    }
                )
            }

            // 4. Draw Needle
            val needleAngle = valueToAngle(animatedRoll)

            // We rotate the canvas context to draw the needle pointing "Right" (0 deg) usually,
            // but here we map it to our arc.
            // Canvas 0 degrees is 3 o'clock. Our calculation aligns with that.

            val needleLength = radius - 40f
            val needleRad = Math.toRadians(needleAngle.toDouble())

            val needleEnd = center + Offset(
                needleLength * cos(needleRad).toFloat(),
                needleLength * sin(needleRad).toFloat()
            )

            drawLine(
                color = Color.Red,
                start = center,
                end = needleEnd,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Needle Pivot Cap
            drawCircle(Color.Red, 8.dp.toPx(), center)
            drawCircle(Color.White, 4.dp.toPx(), center)

            // 5. Draw Digital Value Text
            drawContext.canvas.nativeCanvas.drawText(
                "%.1f°".format(animatedRoll),
                center.x,
                center.y + 120f,
                Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 60f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
            )

            drawContext.canvas.nativeCanvas.drawText(
                "Roll Angle",
                center.x,
                center.y + 160f,
                Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 30f
                    textAlign = Paint.Align.CENTER
                }
            )
        }
    }
}