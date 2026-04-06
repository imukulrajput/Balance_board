package com.ripplehealthcare.bproboard.ui.components.authScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripplehealthcare.bproboard.R
import com.ripplehealthcare.bproboard.ui.components.utility.CustomTextField
import com.ripplehealthcare.bproboard.ui.components.utility.CustomLabel
import com.ripplehealthcare.bproboard.ui.theme.*

@Composable
fun SignUpSection(
    name: String,
    onNameChange: (String) -> Unit,
    email: String,
    password: String,
    confirmPassword: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSignUp: () -> Unit,
    onSwitchToLogin: () -> Unit,
    onGoogleSign: () -> Unit,
    isLoading: Boolean
) {
    val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isPasswordSecure = password.length >= 6
    val doPasswordsMatch = password == confirmPassword

    Column(modifier = Modifier.fillMaxWidth()) {
        CustomLabel("Center Name")
        CustomTextField(value = name, onValueChange = onNameChange, placeholder = "eg: Physio Center Name", enabled = !isLoading)

        CustomLabel("Center Email ID")
        CustomTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = "eg: center@example.com",
            enabled = !isLoading
        )

        CustomLabel("Password")
        CustomTextField(value = password, onValueChange = onPasswordChange, placeholder = "***********", isPassword = true, enabled = !isLoading)

        CustomLabel("Confirm Password")
        CustomTextField(value = confirmPassword, onValueChange = onConfirmPasswordChange, placeholder = "***********", isPassword = true, enabled = !isLoading)

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSignUp,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isLoading && isEmailValid && isPasswordSecure && doPasswordsMatch,
            colors = ButtonDefaults.buttonColors(containerColor = SecondaryColor, disabledContainerColor = DisabledColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Sign up", fontSize = 20.sp, color = WhiteColor, fontWeight = FontWeight.Bold)
        }
        if (password.isNotEmpty() && !isPasswordSecure) {
            Text("Password must be at least 6 characters", color = RedColor, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("or Sign up with", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = TextBlack)

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
            text = "Already have an account ?",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = TextBlack
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onSwitchToLogin,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            border = BorderStroke(1.dp, SecondaryColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Log in", fontSize = 20.sp, color = SecondaryColor, fontWeight = FontWeight.Bold)
        }
    }
}