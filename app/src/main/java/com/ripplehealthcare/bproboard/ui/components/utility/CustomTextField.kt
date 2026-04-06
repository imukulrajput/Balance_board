package com.ripplehealthcare.bproboard.ui.components.utility

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripplehealthcare.bproboard.ui.theme.*

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        placeholder = {
            Text(
                text = placeholder,
                color = TextGray,
                fontSize = 16.sp
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = false,
        isError = isError,
        colors = OutlinedTextFieldDefaults.colors(
            // Matching the light grey background from the images
            unfocusedContainerColor = InputBg,
            focusedContainerColor = InputBg,
            disabledContainerColor = InputBg,
            errorContainerColor = InputBg,

            // Border styling
            unfocusedBorderColor = InputBorder,
            focusedBorderColor = SecondaryColor,
            disabledBorderColor = InputBorder,
            errorBorderColor = RedColor,

            // Text colors
            focusedTextColor = TextBlack,
            unfocusedTextColor = TextBlack,
            disabledTextColor = TextBlack,
            errorTextColor = TextBlack
        )
    )
    if (isError && errorText != null) {
        Text(
            text = errorText,
            color = RedColor,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
        )
    }
}