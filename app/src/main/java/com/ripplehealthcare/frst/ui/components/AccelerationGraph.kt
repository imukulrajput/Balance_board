package com.ripplehealthcare.frst.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripplehealthcare.frst.domain.model.AccPoint

@Composable
fun AccelerationGraph(
    modifier: Modifier = Modifier,
    dataPoints: List<AccPoint>,
    range: Float = 6.0f,
    showLegend: Boolean = true
) {
    Column(modifier = modifier) {
        // Legend
        if (showLegend) {
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GraphLegendItem("X (Left-Right)", Color.Red)
                GraphLegendItem("Y (Up-Down)", Color.Green)
                GraphLegendItem("Z (Forward-Backward)", Color.Blue)
            }
        }

        // The Graph Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White)
        ) {
            val density = LocalDensity.current

            // Define Paint for text labels
            val textPaint = remember(density) {
                Paint().apply {
                    color = android.graphics.Color.GRAY
                    textAlign = Paint.Align.RIGHT
                    textSize = density.run { 10.sp.toPx() }
                }
            }

            Canvas(modifier = Modifier.fillMaxSize().padding(end = 8.dp)) {
                val w = size.width
                val h = size.height
                val midH = h / 2f

                // --- PADDING CONFIGURATION ---
                val labelPadding = 35.dp.toPx() // Left space for text
                val verticalPadding = 20.dp.toPx() // Top/Bottom space inside the box

                // Calculate the "drawable" height so points don't hit the absolute edge
                val drawingHeight = midH - verticalPadding

                // 1. Draw Grid & Labels
                val step = 2
                val start = range.toInt()
                val end = -range.toInt()

                for (value in start downTo end step step) {
                    // Adjusted Y Position Formula:
                    // If value is max (range), yPos = verticalPadding
                    // If value is 0, yPos = midH
                    // If value is min (-range), yPos = h - verticalPadding
                    val yPos = midH - (value / range) * drawingHeight

                    // Draw Label
                    drawContext.canvas.nativeCanvas.drawText(
                        value.toString(),
                        labelPadding - 10f,
                        yPos + 10f, // Center text vertically on line
                        textPaint
                    )

                    // Draw Grid Line
                    val lineColor = if (value == 0) Color.Gray else Color.LightGray.copy(alpha = 0.5f)
                    val strokeWidth = if (value == 0) 2f else 1f

                    val pathEffect = if (value != 0) {
                        PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    } else null

                    drawLine(
                        color = lineColor,
                        start = Offset(labelPadding, yPos),
                        end = Offset(w, yPos),
                        strokeWidth = strokeWidth,
                        pathEffect = pathEffect
                    )
                }

                // 2. Draw Graph Paths
                if (dataPoints.size > 1) {
                    val graphWidth = w - labelPadding
                    val maxPoints = 50
                    val xStep = graphWidth / (maxPoints - 1)

                    val pathX = Path()
                    val pathY = Path()
                    val pathZ = Path()

                    dataPoints.forEachIndexed { i, point ->
                        val x = labelPadding + (i * xStep)

                        // Apply the same vertical scaling logic to data points
                        val yX = midH - (point.x / range) * drawingHeight
                        val yY = midH - (point.y / range) * drawingHeight
                        val yZ = midH - (point.z / range) * drawingHeight

                        if (i == 0) {
                            pathX.moveTo(x, yX)
                            pathY.moveTo(x, yY)
                            pathZ.moveTo(x, yZ)
                        } else {
                            pathX.lineTo(x, yX)
                            pathY.lineTo(x, yY)
                            pathZ.lineTo(x, yZ)
                        }
                    }

                    drawPath(pathX, Color.Red, style = Stroke(width = 3f))
                    drawPath(pathY, Color.Green, style = Stroke(width = 3f))
                    drawPath(pathZ, Color.Blue, style = Stroke(width = 3f))
                }
            }
        }
    }
}

@Composable
fun GraphLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, androidx.compose.foundation.shape.CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}