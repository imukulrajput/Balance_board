package com.ripplehealthcare.bproboard.ui.components.utility

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripplehealthcare.bproboard.ui.theme.TextGray

@Composable
fun CustomLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp, top = 16.dp),
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        color = TextGray
    )
}