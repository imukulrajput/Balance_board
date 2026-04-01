package com.ripplehealthcare.frst.ui.components.centerDashbord

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripplehealthcare.frst.domain.model.Gender
import com.ripplehealthcare.frst.domain.model.Specialization
import com.ripplehealthcare.frst.ui.components.utility.CustomLabel
import com.ripplehealthcare.frst.ui.components.utility.CustomTextField
import com.ripplehealthcare.frst.ui.theme.InputBg
import com.ripplehealthcare.frst.ui.theme.InputBorder
import com.ripplehealthcare.frst.ui.theme.RedColor
import com.ripplehealthcare.frst.ui.theme.SecondaryColor
import com.ripplehealthcare.frst.ui.theme.TextBlack
import com.ripplehealthcare.frst.ui.theme.WhiteColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDoctorBottomSheet(
    onDismiss: () -> Unit,
    onAddClick: (name: String, phone: String, email: String, gender: String, spec: String) -> Unit,
    isLoading: Boolean
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    // Enum States
    var selectedGender by remember { mutableStateOf(Gender.MALE) }
    var selectedSpec by remember { mutableStateOf(Specialization.NEURO_PHYSIO) }
    var otherSpec by remember { mutableStateOf("") }

    // Dropdown Expanded States
    var genderExpanded by remember { mutableStateOf(false) }
    var specExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp).verticalScroll(rememberScrollState())) {
            Text("Add New Doctor", fontSize = 20.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(20.dp))

            CustomLabel("*Doctor Name")
            CustomTextField(value = name, onValueChange = { name = it }, placeholder = "Dr. Name")

            CustomLabel("*Email Address")
            CustomTextField(value = email, onValueChange = { email = it }, placeholder = "doctor@ripplehealthcare.in")

            CustomLabel("*Gender")
            ExposedDropdownMenuBox(expanded = genderExpanded, onExpandedChange = { genderExpanded = !genderExpanded }) {
                OutlinedTextField(
                    value = selectedGender.displayName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = InputBg,
                        focusedContainerColor = InputBg,
                        disabledContainerColor = InputBg,
                        errorContainerColor = InputBg,

                        unfocusedBorderColor = InputBorder,
                        focusedBorderColor = SecondaryColor,
                        disabledBorderColor = InputBorder,
                        errorBorderColor = RedColor,

                        focusedTextColor = TextBlack,
                        unfocusedTextColor = TextBlack,
                        disabledTextColor = TextBlack,
                        errorTextColor = TextBlack
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = genderExpanded,
                    onDismissRequest = { genderExpanded = false },
                    modifier = Modifier.background(WhiteColor))
                {
                    Gender.entries.filter { it != Gender.ALL }.forEach { gender ->
                        DropdownMenuItem(
                            text = { Text(gender.displayName, color= TextBlack) },
                            onClick = { selectedGender = gender; genderExpanded = false }
                        )
                    }
                }
            }

            CustomLabel("*Specialization")
            ExposedDropdownMenuBox(
                expanded = specExpanded,
                onExpandedChange = { specExpanded = !specExpanded },
                modifier = Modifier.background(WhiteColor)
            ) {
                OutlinedTextField(
                    value = if (selectedSpec == Specialization.OTHER) "Other" else selectedSpec.displayName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = specExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = InputBg,
                        focusedContainerColor = InputBg,
                        disabledContainerColor = InputBg,
                        errorContainerColor = InputBg,

                        unfocusedBorderColor = InputBorder,
                        focusedBorderColor = SecondaryColor,
                        disabledBorderColor = InputBorder,
                        errorBorderColor = RedColor,

                        focusedTextColor = TextBlack,
                        unfocusedTextColor = TextBlack,
                        disabledTextColor = TextBlack,
                        errorTextColor = TextBlack
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = specExpanded, onDismissRequest = { specExpanded = false }) {
                    Specialization.entries.forEach { spec ->
                        DropdownMenuItem(
                            text = { Text(spec.displayName, color= TextBlack) },
                            onClick = { selectedSpec = spec; specExpanded = false }
                        )
                    }
                }
            }

            if (selectedSpec == Specialization.OTHER) {
                Spacer(modifier = Modifier.height(8.dp))
                CustomTextField(value = otherSpec, onValueChange = { otherSpec = it }, placeholder = "Enter Specialization")
            }

            CustomLabel("*Phone Number")
            CustomTextField(value = phone, onValueChange = { if (it.length <= 10) phone = it.filter { c -> c.isDigit() } }, placeholder = "10-digit number")

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val finalSpec = if (selectedSpec == Specialization.OTHER) otherSpec else selectedSpec.displayName
                    onAddClick(name, phone, email, selectedGender.displayName, finalSpec)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading && name.isNotBlank() && phone.length == 10 && email.contains("@"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8BC34A))
            ) {
                Text("Register Doctor", fontWeight = FontWeight.Bold)
            }
        }
    }
}