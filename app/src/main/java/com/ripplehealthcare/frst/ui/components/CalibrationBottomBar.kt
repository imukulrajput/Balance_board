package com.ripplehealthcare.frst.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripplehealthcare.frst.ui.theme.PrimaryColor
import com.ripplehealthcare.frst.ui.theme.WhiteColor

@Composable
fun CalibrationBottomBar(
    isCalibrated: Boolean,
    onStartClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Surface(shadowElevation = 8.dp, color = Color.White) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp, 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Start Calibration Button
            Button(
                onClick = onStartClick,
                enabled = !isCalibrated, // Disabled after calibration
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, disabledContainerColor = Color.Gray, contentColor= WhiteColor, disabledContentColor = WhiteColor)
            ) {
                Text("Start Calibration", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            // Save Calibration Button
            OutlinedButton(
                onClick = onSaveClick,
                enabled = isCalibrated, // Enabled only after calibration
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    if (isCalibrated) PrimaryColor else Color.Gray
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PrimaryColor, disabledContentColor = Color.Gray, containerColor = Color.Transparent, disabledContainerColor = Color.Transparent
                )
            ) {
                Text("Save and Continue", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}