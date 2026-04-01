package com.ripplehealthcare.frst.ui.components.authScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripplehealthcare.frst.ui.theme.SecondaryColor
import com.ripplehealthcare.frst.ui.theme.TextBlack
import com.ripplehealthcare.frst.ui.theme.TextGray
import com.ripplehealthcare.frst.ui.theme.WhiteColor

@Composable
fun EmailVerificationSection(
    email: String,
    onCheckStatus: () -> Unit,
    onResend: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Verification Icon
        Icon(
            imageVector = Icons.Default.Email,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = SecondaryColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Verify your Email",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextBlack
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "We've sent a verification link to:\n$email",
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            color = TextGray,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Main Action Button
        Button(
            onClick = onCheckStatus,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = SecondaryColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("I've Verified My Email", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WhiteColor)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Resend Option
        TextButton(
            onClick = onResend,
            enabled = !isLoading
        ) {
            Text(
                text = "Resend Verification Email",
                color = SecondaryColor,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = "Check your spam folder if you don't see the email.",
            fontSize = 12.sp,
            color = TextGray,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}