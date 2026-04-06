package com.ripplehealthcare.bproboard.ui.components

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ripplehealthcare.bproboard.ui.theme.PrimaryColor

@Composable
fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.Black,
    unfocusedTextColor = Color.Black,
    disabledTextColor = Color.Gray,
    cursorColor = PrimaryColor,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color.White,
    focusedBorderColor = PrimaryColor,
    unfocusedBorderColor = Color.Gray,
    focusedLabelColor = PrimaryColor,
    unfocusedLabelColor = Color.Gray,
    errorContainerColor = Color.White
)