package com.ripplehealthcare.frst.ui.components.authScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripplehealthcare.frst.R
import com.ripplehealthcare.frst.ui.components.utility.CustomTextField
import com.ripplehealthcare.frst.ui.components.utility.CustomLabel
import com.ripplehealthcare.frst.ui.theme.*
import com.ripplehealthcare.frst.ui.viewmodel.InputType

@Composable
fun LoginSection(
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onSwitchToSignUp: () -> Unit,
    onGoogleSign: () -> Unit,
    isLoading: Boolean
) {
    val inputType = remember(email) {
        val trimmedInput = email.trim()
        when {
            trimmedInput.length == 10 && trimmedInput.all { it.isDigit() } -> InputType.PHONE
            trimmedInput.contains("@") && android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedInput).matches() -> InputType.EMAIL
            else -> InputType.INVALID
        }
    }

    val isFormValid = inputType != InputType.INVALID && password.length >= 6

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Sign in to your account",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextBlack
        )
        Spacer(modifier = Modifier.height(24.dp))

        CustomLabel("Mobile number / Email ID")
        CustomTextField(
            value = email,
            onValueChange = {
                val cleaned = it.trim()
                if (cleaned.all { c -> c.isDigit() }) {
                    if (cleaned.length <= 10) onEmailChange(cleaned)
                } else {
                    onEmailChange(cleaned)
                }
            },
            placeholder = "Enter 10-digit number or Email",
            enabled = !isLoading,
            isError = email.isNotEmpty() && inputType == InputType.INVALID,
            keyboardType = KeyboardType.Email,
        )

        // Optional: Inline error hint
        if (email.isNotEmpty() && inputType == InputType.INVALID) {
            Text(
                "Please enter a valid 10-digit phone or email",
                color = RedColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        CustomLabel("Password")
        CustomTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = "**********",
            isPassword = true,
            enabled = !isLoading,
            keyboardType = KeyboardType.Password
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSignIn,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            // Button only lights up when input is valid
            enabled = !isLoading && isFormValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = SecondaryColor,
                disabledContainerColor = DisabledColor
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Sign in", fontSize = 20.sp, color = WhiteColor, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("or Log in with", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = TextBlack)

        Spacer(modifier = Modifier.height(16.dp))

        // Single Google Icon as per your request
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.google_icon),
                contentDescription = "Google",
                modifier = Modifier
                    .size(54.dp)
                    .clickable(enabled = !isLoading) { onGoogleSign() }
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Don't have an account ?",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = TextBlack
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onSwitchToSignUp,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            border = BorderStroke(1.dp, SecondaryColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Sign up", fontSize = 20.sp, color = SecondaryColor, fontWeight = FontWeight.Bold)
        }
    }
}