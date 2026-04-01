package com.ripplehealthcare.frst.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ripplehealthcare.frst.ui.screens.GameState
import kotlin.math.min
import kotlin.math.sqrt

private const val MAX_TILT_ANGLE = 15f

@Composable
fun SensorCircle(
    modifier: Modifier = Modifier,
    yaw: Float,
    pitch: Float,
    baseYaw: Float,
    basePitch: Float,
    gameState: GameState,
    dotColor: Color
) {
    // 1. Math Logic
    val xValue = yaw - baseYaw
    val yValue = pitch - basePitch
    val dist = sqrt(xValue * xValue + yValue * yValue)

    // 2. State for Trail
    // Stores offsets relative to the Canvas center (0,0 is center)
    val trailPoints = remember { mutableStateListOf<Offset>() }
    val animatedOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    // 3. Clear trail on reset
    LaunchedEffect(gameState) {
        if (gameState == GameState.READY || gameState == GameState.CALIBRATING) {
            trailPoints.clear()
        }
    }

    // 4. Animation & Clamping

    var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    LaunchedEffect(xValue, yValue, gameState, canvasSize) {
        if (gameState == GameState.RUNNING && canvasSize.width > 0) {
            val maxRadius = min(canvasSize.width, canvasSize.height) / 2
            val scale = maxRadius / MAX_TILT_ANGLE

            var fX = xValue; var fY = yValue
            if (dist > MAX_TILT_ANGLE && dist > 0) {
                val factor = MAX_TILT_ANGLE / dist
                fX *= factor; fY *= factor
            }

            // Calculate Target Offset relative to CENTER
            // Note: In Compose Y is Down. Positive Pitch (up tilt) usually means negative Y in pixels.
            // Let's assume standard graph: +Y is Up.
            val targetX = fX * scale
            val targetY = -(fY * scale) // Invert Y for screen coords
            val targetOffset = Offset(targetX, targetY)

            animatedOffset.animateTo(targetOffset, tween(5))

            // Add to trail (Relative to center)
            trailPoints.add(animatedOffset.value)
        } else if (gameState == GameState.READY) {
            animatedOffset.snapTo(Offset.Zero)
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            canvasSize = size
            val centerX = size.width / 2
            val centerY = size.height / 2
            val center = Offset(centerX, centerY)
            val maxRadius = min(centerX, centerY)

            // A. Draw Zones
            drawCircle(Color(0xFFFFCDD2), maxRadius, center) // Danger
            drawCircle(Color(0xFFFFF9C4), maxRadius * (12f / MAX_TILT_ANGLE), center) // Warning
            drawCircle(Color(0xFFC8E6C9), maxRadius * (5f / MAX_TILT_ANGLE), center) // Safe

            // B. Draw Crosshairs
            drawLine(Color.Gray.copy(0.5f), Offset(centerX, centerY - maxRadius), Offset(centerX, centerY + maxRadius), 2f)
            drawLine(Color.Gray.copy(0.5f), Offset(centerX - maxRadius, centerY), Offset(centerX + maxRadius, centerY), 2f)

            // C. Draw Trail
            if (gameState == GameState.RUNNING || gameState == GameState.STAGE_FINISHED) {
                if (trailPoints.isNotEmpty()) {
                    // Convert relative offsets to absolute pixel coordinates
                    val absolutePoints = trailPoints.map { relativeOffset ->
                        Offset(centerX + relativeOffset.x, centerY + relativeOffset.y)
                    }
                    drawPoints(
                        points = absolutePoints,
                        pointMode = PointMode.Polygon,
                        color = dotColor.copy(alpha = 0.5f),
                        strokeWidth = 4f
                    )
                }
            }

            // D. Draw Current Dot
            if (gameState == GameState.RUNNING || gameState == GameState.READY) {
                // Current pos is relative to center, so add center
                val currentPos = Offset(
                    centerX + animatedOffset.value.x,
                    centerY + animatedOffset.value.y
                )

                val actualColor = if (gameState == GameState.READY) Color.Gray else dotColor
                drawCircle(actualColor, 10f, currentPos)
                drawCircle(Color.White, 10f, currentPos, style = Stroke(3f))
            }
        }
    }
}