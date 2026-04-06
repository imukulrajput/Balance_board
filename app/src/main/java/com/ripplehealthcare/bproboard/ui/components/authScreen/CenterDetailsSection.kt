package com.ripplehealthcare.bproboard.ui.components.authScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripplehealthcare.bproboard.domain.model.CenterAddress
import com.ripplehealthcare.bproboard.ui.components.utility.CustomTextField
import com.ripplehealthcare.bproboard.ui.components.utility.CustomLabel
import com.ripplehealthcare.bproboard.ui.theme.*

@Composable
fun CenterDetailsSection(
    centerName: String,
    addressObj: CenterAddress,
    phoneNumber: String,
    errors: Map<String, String>,
    onNameChange: (String) -> Unit,
    onAddressClick: () -> Unit,
    onPhoneChange: (String) -> Unit,
    onNext: () -> Unit,
    isLoading: Boolean
) {
    val isPhoneValid = phoneNumber.length == 10 &&
            phoneNumber.firstOrNull() in listOf('6', '7', '8', '9')
    val isNameValid = centerName.trim().length >= 3
    val isAddressSelected = addressObj.fullAddress.isNotEmpty()

    val canProceed = isPhoneValid && isNameValid && isAddressSelected
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Center details",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextBlack
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Center Name Field
        CustomLabel("*Center Name")
        CustomTextField(
            value = centerName,
            onValueChange = onNameChange,
            placeholder = "Name of the clinic",
            enabled = !isLoading,
            isError = errors.containsKey("clinicName"),
            errorText = errors["clinicName"]
        )

        CustomLabel("*Phone number of the center")
        CustomTextField(
            value = phoneNumber,
            onValueChange = { if (it.length <= 10) onPhoneChange(it.filter { char -> char.isDigit() }) },
            placeholder = "10-digit mobile number",
            enabled = !isLoading,
            isError = errors.containsKey("phone"),
            errorText = errors["phone"]
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Address Section with Google Place Picker Trigger
        CustomLabel("*Address")
        Box(modifier = Modifier.clickable(enabled = !isLoading) { onAddressClick() }) {
            CustomTextField(
                value = addressObj.fullAddress,
                onValueChange = {},
                placeholder = "Building name, street,",
                enabled = false,
                isError = errors.containsKey("clinicAddress"),
                errorText = errors["clinicAddress"]
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        CustomTextField(value = addressObj.city, onValueChange = {}, placeholder = "City", enabled = false)

        Spacer(modifier = Modifier.height(8.dp))
        CustomTextField(value = addressObj.state, onValueChange = {}, placeholder = "State", enabled = false)

        Spacer(modifier = Modifier.height(8.dp))
        CustomTextField(value = addressObj.zipCode, onValueChange = {}, placeholder = "Pin Code", enabled = false)
        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = canProceed && !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = SecondaryColor, disabledContainerColor = DisabledColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Next",
                fontSize = 20.sp,
                color = WhiteColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}