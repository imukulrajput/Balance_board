package com.ripplehealthcare.frst.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ripplehealthcare.frst.R
import com.ripplehealthcare.frst.domain.model.Gender
import com.ripplehealthcare.frst.ui.components.customTextFieldColors
import com.ripplehealthcare.frst.ui.theme.PrimaryColor
import com.ripplehealthcare.frst.ui.theme.WhiteColor
import com.ripplehealthcare.frst.ui.viewmodel.ManagementViewModel
import com.ripplehealthcare.frst.ui.viewmodel.PatientViewModel
import kotlinx.coroutines.launch

@Composable
fun PatientOnboardingScreen(
    navController: NavController,
    managementViewModel: ManagementViewModel,
    viewModel: PatientViewModel,
    isEditMode: Boolean = false
) {
    val formState by viewModel.formState.collectAsState()
    val doctor by managementViewModel.selectedDoctor.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedPatient by viewModel.selectedPatient.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            OnboardingTopBar(
                title = if (isEditMode) "Edit Patient" else "New Patient",
                onBack = { if(formState.currentStep==0){ navController.popBackStack() } else { viewModel.previousStep() } }
            )
        },
        containerColor = Color(0xFFF0F4F8)
    ) { padding ->
        // Use a Box to allow overlaying the loader
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                when (formState.currentStep) {
                    0 -> IdentityStep(viewModel)
                    1 -> QuestionnaireStepOne(viewModel)
                    2 -> QuestionnaireStepTwo(viewModel)
                    3 -> PainMappingStep(viewModel)
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        when (formState.currentStep) {
                            0 -> {
                                if (viewModel.canProceedFromStep0()) viewModel.nextStep()
                            }
                            1 -> {
                                if (formState.hasFallenPastYear != null && formState.feelsUnsteady != null && formState.historyOfDizziness != null) {
                                    viewModel.nextStep()
                                }
                            }
                            2 -> {
                                if (formState.needsSupportToStand != null && formState.takesDizzyMedication != null && formState.worriedAboutFalling != null) {
                                    viewModel.nextStep()
                                }
                            }
                            3 -> {
                                scope.launch {
                                    if (isEditMode) {
                                        selectedPatient?.let { p ->
                                            doctor?.let { d ->
                                                viewModel.updatePatientDetails(p.patientId, d.centerId, p.doctorId) {
                                                    navController.popBackStack()
                                                }
                                            }
                                        }
                                    } else {
                                        val newPatient = doctor?.let { d ->
                                            viewModel.saveOnboarding(d.centerId, d.id)
                                        }
                                        if (newPatient != null) navController.popBackStack()
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                    enabled = !isLoading
                ) {
                    Text(
                        text = if (formState.currentStep < 3) "Next"
                        else if (isEditMode) "Save Changes"
                        else "Finish Onboarding",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Global Loader Overlay
            if (isLoading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.3f) // Dim the background
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryColor)
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionnaireStepOne(viewModel: PatientViewModel) {
    val formState by viewModel.formState.collectAsState()

    Column {
        if (formState.hasFallenPastYear == null || formState.feelsUnsteady == null || formState.historyOfDizziness == null) {
            Text(
                "Please answer all questions to proceed",
                color = Color.Red,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        QuestionnaireItem(
            question = "Have you fallen or slipped to the ground in the past 12 months?",
            selectedAnswer = formState.hasFallenPastYear,
            onAnswerSelected = { viewModel.updateQuestion(0, it) },
            accentColor = PrimaryColor
        )
        QuestionnaireItem(
            question = "Do you ever feel unsteady when standing or walking?",
            selectedAnswer = formState.feelsUnsteady,
            onAnswerSelected = { viewModel.updateQuestion(1, it) },
            accentColor = PrimaryColor
        )
        QuestionnaireItem(
            question = "Do you have any history of dizziness, vertigo, or episodes of lightheadedness?",
            selectedAnswer = formState.historyOfDizziness,
            onAnswerSelected = { viewModel.updateQuestion(2, it) },
            accentColor = PrimaryColor
        )
    }
}

@Composable
fun QuestionnaireStepTwo(viewModel: PatientViewModel) {
    val formState by viewModel.formState.collectAsState()

    Column {
        if (formState.needsSupportToStand == null && formState.takesDizzyMedication !== null && formState.worriedAboutFalling == null) {
            Text(
                "Please answer all questions to proceed",
                color = Color.Red,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        QuestionnaireItem(
            question = "Do you need to push with your hands to stand up from a chair?",
            selectedAnswer = formState.needsSupportToStand,
            onAnswerSelected = { viewModel.updateQuestion(3, it) },
            accentColor = PrimaryColor
        )
        QuestionnaireItem(
            question = "Do you take any medicine that can make you feel dizzy or more tired than usual?",
            selectedAnswer = formState.takesDizzyMedication,
            onAnswerSelected = { viewModel.updateQuestion(4, it) },
            accentColor = PrimaryColor
        )
        QuestionnaireItem(
            question = "Are you worried about falling?",
            selectedAnswer = formState.worriedAboutFalling,
            onAnswerSelected = { viewModel.updateQuestion(5, it) },
            accentColor = PrimaryColor
        )
    }
}

@Composable
fun QuestionnaireItem(
    question: String,
    selectedAnswer: Boolean?,
    onAnswerSelected: (Boolean) -> Unit,
    accentColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(
            text = question,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2D3142)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Yes Button
            OptionButton(
                text = "Yes",
                isSelected = selectedAnswer == true,
                onClick = { onAnswerSelected(true) },
                accentColor = accentColor,
                modifier = Modifier.weight(1f)
            )
            // No Button
            OptionButton(
                text = "No",
                isSelected = selectedAnswer == false,
                onClick = { onAnswerSelected(false) },
                accentColor = Color.Red,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun IdentityStep(viewModel: PatientViewModel) {
    val formState by viewModel.formState.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Patient details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = formState.name,
            onValueChange = { viewModel.updateName(it) },
            label = { Text("* Enter your full name") },
            modifier = Modifier.fillMaxWidth(),
            isError = formState.nameError != null,
            supportingText = { formState.nameError?.let { Text(it) } },
            colors = customTextFieldColors()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = formState.age,
                onValueChange = { viewModel.updateAge(it) },
                label = { Text("* Age") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { formState.ageError?.let { Text(it) } },
                colors = customTextFieldColors()
            )
            // Reusing your GenderDropdown component here
            Box(modifier = Modifier.weight(1f)) {
                GenderDropdown(
                    selectedGender = formState.gender,
                    onGenderSelect = { viewModel.updateGender(it) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = formState.weight,
                onValueChange = { viewModel.updateWeight(it) },
                label = { Text("Weight") },
                modifier = Modifier.weight(1f),
                supportingText = { formState.weightError?.let { Text(it) } },
                colors = customTextFieldColors()
            )
            OutlinedTextField(
                value = formState.height,
                onValueChange = { viewModel.updateHeight(it) },
                label = { Text("Height in CM") },
                modifier = Modifier.weight(1f),
                supportingText = { formState.heightError?.let { Text(it) } },
                colors = customTextFieldColors()
            )
        }

        OutlinedTextField(
            value = formState.phone,
            onValueChange = { if (it.length <= 10) viewModel.updatePhone(it) },
            label = { Text("* Mob Number") },
            modifier = Modifier.fillMaxWidth(),
            supportingText = { formState.phoneError?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = customTextFieldColors()
        )

        OutlinedTextField(
            value = formState.email,
            onValueChange = { viewModel.updateEmail(it) },
            label = { Text("Email ID") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = customTextFieldColors()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenderDropdown(
    selectedGender: String, // Changed to String to match NewPatientFormState
    onGenderSelect: (String) -> Unit // Passes the displayName String
) {
    var expanded by remember { mutableStateOf(false) }

    val genders = Gender.entries.filter { it != Gender.ALL }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            // Display the current String value directly
            value = selectedGender,
            onValueChange = {},
            readOnly = true,
            label = { Text("Gender") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = customTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(WhiteColor)
        ) {
            genders.forEach { gender ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = gender.displayName, // Use the professional display name
                            color = Color.Black
                        )
                    },
                    onClick = {
                        onGenderSelect(gender.displayName) // Pass the String displayName
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun PainMappingStep(viewModel: PatientViewModel) {
    val formState by viewModel.formState.collectAsState()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Please tap on any body parts where you experiencing pain.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        Box(modifier = Modifier.size(350.dp, 450.dp)) {
            Image(
                painter = painterResource(id = R.drawable.human_body),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )

            // UPPER BODY
            PainNode("Head", formState.painAreas.contains("Head"), { viewModel.togglePainArea("Head") },
                Modifier.align(Alignment.TopCenter).padding(top = 10.dp))

            PainNode("Chest", formState.painAreas.contains("Chest"), { viewModel.togglePainArea("Chest") },
                Modifier.align(Alignment.TopStart).padding(top = 80.dp, start = 80.dp))

            PainNode("Stomach", formState.painAreas.contains("Stomach"), { viewModel.togglePainArea("Stomach") },
                Modifier.align(Alignment.CenterStart).padding(top = 20.dp, start = 80.dp))

            // ARMS
            PainNode("Shoulder", formState.painAreas.contains("Shoulder"), { viewModel.togglePainArea("Shoulder") },
                Modifier.align(Alignment.TopEnd).padding(top = 70.dp, end = 60.dp))

            PainNode("Elbow", formState.painAreas.contains("Elbow"), { viewModel.togglePainArea("Elbow") },
                Modifier.align(Alignment.CenterEnd).padding(bottom = 50.dp, end = 40.dp))

            PainNode("Hand", formState.painAreas.contains("Hand"), { viewModel.togglePainArea("Hand") },
                Modifier.align(Alignment.CenterEnd).padding(top = 110.dp, end = 20.dp))

            // LOWER BODY
            PainNode("Hip", formState.painAreas.contains("Hip"), { viewModel.togglePainArea("Hip") },
                Modifier.align(Alignment.CenterStart).padding(top = 100.dp, start = 70.dp))

            PainNode("Thigh", formState.painAreas.contains("Thigh"), { viewModel.togglePainArea("Thigh") },
                Modifier.align(Alignment.BottomEnd).padding(bottom = 140.dp, end = 90.dp))

            PainNode("Knee", formState.painAreas.contains("Knee"), { viewModel.togglePainArea("Knee") },
                Modifier.align(Alignment.BottomStart).padding(bottom = 100.dp, start = 90.dp))

            PainNode("Shin", formState.painAreas.contains("Shin"), { viewModel.togglePainArea("Shin") },
                Modifier.align(Alignment.BottomEnd).padding(bottom = 60.dp, end = 90.dp))

            PainNode("Ankle", formState.painAreas.contains("Ankle"), { viewModel.togglePainArea("Ankle") },
                Modifier.align(Alignment.BottomEnd).padding(bottom = 30.dp, end = 80.dp))

            PainNode("Foot", formState.painAreas.contains("Foot"), { viewModel.togglePainArea("Foot") },
                Modifier.align(Alignment.BottomStart).padding(bottom = 10.dp, start = 90.dp))
        }
    }
}

@Composable
fun PainNode(
    label: String,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.clickable { onToggle() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color.Red, CircleShape)
            )
            Spacer(Modifier.width(4.dp))
        }
        Surface(
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, Color.LightGray),
            color = if (isSelected) Color(0xFFE0F2F1) else Color.White
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun OptionButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isSelected) accentColor else Color.LightGray),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) Color.White else Color.Transparent,
            contentColor = if (isSelected) accentColor else Color.Gray
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingTopBar(title: String, onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(title, color = Color(0xFF1D8F9B), fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }
    )
}