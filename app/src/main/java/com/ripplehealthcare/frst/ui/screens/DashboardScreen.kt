//package com.ripplehealthcare.frst.ui.screens
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.AccountCircle
//import androidx.compose.material.icons.filled.Email
//import androidx.compose.material.icons.filled.Groups
//import androidx.compose.material.icons.filled.PersonAdd
//import androidx.compose.material.icons.filled.Phone
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.vector.ImageVector
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.input.KeyboardType
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import com.ripplehealthcare.frst.domain.model.Gender
//import com.ripplehealthcare.frst.domain.model.Patient
//import com.ripplehealthcare.frst.domain.model.UserProfile
//import com.ripplehealthcare.frst.ui.components.BottomNavigation
//import com.ripplehealthcare.frst.ui.components.customTextFieldColors
//import com.ripplehealthcare.frst.ui.theme.CardColor
//import com.ripplehealthcare.frst.ui.theme.PrimaryColor
//import com.ripplehealthcare.frst.ui.theme.WhiteColor
//import com.ripplehealthcare.frst.ui.viewmodel.AuthViewModel
//import com.ripplehealthcare.frst.ui.viewmodel.PatientViewModel
//import com.ripplehealthcare.frst.ui.viewmodel.TestViewModel
//import kotlinx.coroutines.launch
//
//@Composable
//fun DashboardScreen(
//    navController: NavController,
//    authViewModel: AuthViewModel,
//    patientViewModel: PatientViewModel,
//    testViewModel: TestViewModel
//) {
//    val userProfile by authViewModel.userProfile.collectAsState()
//    val error by patientViewModel.error.collectAsState()
//    val snackbarHostState = remember { SnackbarHostState() }
//    // State to control the visibility of the bottom sheet
//    var showAddPatientSheet by remember{ mutableStateOf(false) }
//
//    if (showAddPatientSheet) {
//        AddPatientSheet(
//            onAddPatient = { patient->
//                showAddPatientSheet = false
//                testViewModel.setPatient(patient)
//                testViewModel.resetTestData()
//                navController.navigate("patient_detail/${patient.patientId}")
//            },
//            patientViewModel = patientViewModel,
//            onDismiss = {showAddPatientSheet = false}
//        )
//    }
//
//    LaunchedEffect(error) {
//        error?.let {
//            snackbarHostState.showSnackbar(it)
//            patientViewModel.clearError() // Clear the error after showing
//        }
//    }
//
//    Scaffold(
//        topBar = { TopBar(navController) }, // Reusing the same TopBar
//        bottomBar = { BottomNavigation(navController) },
//        snackbarHost = { SnackbarHost(snackbarHostState) },
//        containerColor = Color(0xFFF0F4F8) // Consistent background color
//    ) { innerPadding ->
//        DashboardContent(
//            modifier = Modifier.padding(innerPadding),
//            userProfile=userProfile,
//            navController = navController,
//            onAddNewPatientClick = { showAddPatientSheet = true }
//        )
//    }
//}
//
//@Composable
//private fun DashboardContent(
//    modifier: Modifier = Modifier,
//    userProfile: UserProfile?,
//    navController: NavController,
//    onAddNewPatientClick: () -> Unit
//) {
//    Column(
//        modifier = modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        verticalArrangement = Arrangement.spacedBy(24.dp)
//    ) {
//        Text(
//            text = "Welcome, Dr. ${userProfile?.name}!",
//            style = MaterialTheme.typography.headlineSmall,
//            fontWeight = FontWeight.Bold
//        )
//        // Card to display doctor's details
//        DoctorDetailsCard(
//            name = "Dr. ${userProfile?.name}",
//            email = userProfile?.email?:"N/A",
//            phone = userProfile?.phone?:"N/A"
//        )
//
//        // Elevated button for adding a new patient
//        DashboardActionButton(
//            title = "Add New Patient",
//            subtitle = "Create a new patient record",
//            icon = Icons.Default.PersonAdd,
//            onClick = onAddNewPatientClick
//        )
//
//        // Elevated button for viewing the patient list
//        DashboardActionButton(
//            title = "View Patient List",
//            subtitle = "Browse existing patient records",
//            icon = Icons.Default.Groups,
//            onClick = {
//                navController.navigate("patientList") // Navigate to your PatientListScreen
//            }
//        )
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun AddPatientSheet(
//    onAddPatient: (patient:Patient) -> Unit,
//    patientViewModel: PatientViewModel,
//    onDismiss: () -> Unit
//) {
//    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
//    val formState by patientViewModel.formState.collectAsState()
//    val isLoading by patientViewModel.isLoading.collectAsState()
//    val scope = rememberCoroutineScope()
//
//    ModalBottomSheet(
//        onDismissRequest = onDismiss,
//        sheetState = sheetState,
//        modifier = Modifier.fillMaxHeight(0.9f),
//        containerColor = Color(0xFFF0F4F8)
//    ) {
//        Column(
//            modifier = Modifier
//                .padding(horizontal = 24.dp)
//                .verticalScroll(rememberScrollState())
//        ) {
//            Text(
//                "Add New Patient",
//                style = MaterialTheme.typography.headlineSmall,
//                fontWeight = FontWeight.Bold,
//                color = Color.Black,
//                modifier = Modifier.padding(bottom = 16.dp)
//            )
//
//            OutlinedTextField(
//                value = formState.name,
//                onValueChange = { patientViewModel.updateName(it) },
//                singleLine = true,
//                label = { Text("Name") },
//                modifier = Modifier.fillMaxWidth(),
//                isError = formState.nameError != null, // Show error state
//                supportingText = { // Display the error message
//                    if (formState.nameError != null) {
//                        Text(formState.nameError!!)
//                    }
//                },
//                colors = customTextFieldColors()
//            )
//            Spacer(Modifier.height(12.dp))
//            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                OutlinedTextField(
//                    value = formState.age,
//                    onValueChange = { patientViewModel.updateAge(it) },
//                    singleLine = true,
//                    label = { Text("Age") },
//                    modifier = Modifier.weight(1f),
//                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                    isError = formState.ageError != null, // Show error state
//                    supportingText = { // Display the error message
//                        if (formState.ageError != null) {
//                            Text(formState.ageError!!)
//                        }
//                    },
//                    colors = customTextFieldColors()
//                )
//                OutlinedTextField(
//                    value = formState.height,
//                    onValueChange = { patientViewModel.updateHeight(it) },
//                    singleLine = true,
//                    label = { Text("Height (cm)") },
//                    modifier = Modifier.weight(1f),
//                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                    isError = formState.heightError != null, // Show error state
//                    supportingText = { // Display the error message
//                        if (formState.heightError != null) {
//                            Text(formState.heightError!!)
//                        }
//                    },
//                    colors = customTextFieldColors()
//                )
//                OutlinedTextField(
//                    value = formState.weight,
//                    onValueChange = { patientViewModel.updateWeight(it) },
//                    singleLine = true,
//                    label = { Text("Weight (kg)") },
//                    modifier = Modifier.weight(1f),
//                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                    isError = formState.weightError != null, // Show error state
//                    supportingText = { // Display the error message
//                        if (formState.weightError != null) {
//                            Text(formState.weightError!!)
//                        }
//                    },
//                    colors = customTextFieldColors()
//                )
//            }
//            Spacer(Modifier.height(12.dp))
//            GenderDropdown(
//                selectedGender = formState.gender,
//                onGenderSelect = { patientViewModel.updateGender(it) }
//            )
//            Spacer(Modifier.height(12.dp))
//            OutlinedTextField(
//                value = formState.phone,
//                onValueChange = { input ->
//                    if (input.length <= 10 && input.all { it.isDigit() }) {
//                        patientViewModel.updatePhone(input)
//                    }
//                },
//                singleLine = true,
//                label = { Text("Phone") },
//                modifier = Modifier.fillMaxWidth(),
//                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
//                isError = formState.phoneError != null,
//                supportingText = {
//                    if (formState.phoneError != null) {
//                        Text(formState.phoneError!!)
//                    }
//                },
//                colors = customTextFieldColors()
//            )
//            Spacer(Modifier.height(12.dp))
//            OutlinedTextField(
//                value = formState.email,
//                onValueChange = { patientViewModel.updateEmail(it) },
//                singleLine = true,
//                label = { Text("Email (Optional)") },
//                modifier = Modifier.fillMaxWidth(),
//                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
//                isError = formState.emailError != null,
//                supportingText = {
//                    if (formState.emailError != null) {
//                        Text(formState.emailError!!)
//                    }
//                },
//                colors = customTextFieldColors()
//            )
//
//            Spacer(Modifier.height(16.dp))
//            PainAreaSelection(
//                selectedAreas = formState.painAreas,
//                onToggleArea = { patientViewModel.togglePainArea(it) }
//            )
//
//            Spacer(Modifier.height(24.dp))
//            Button(
//                enabled = !isLoading,
//                onClick = {
//                    scope.launch {
//                        val newPatient = patientViewModel.addPatient("","")
//                        newPatient?.let {
//                            onAddPatient(it)
//                        }
//                    }
//                },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(56.dp),
//                shape = RoundedCornerShape(16.dp),
//                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, disabledContainerColor = Color.Gray, contentColor= WhiteColor, disabledContentColor = WhiteColor)
//            ) {
//                if(isLoading){
//                    CircularProgressIndicator(color= WhiteColor)
//                }else{
//                    Text("Save and Continue")
//                }
//            }
//            Spacer(Modifier.height(16.dp))
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun GenderDropdown(
//    selectedGender: String, // Changed to String to match NewPatientFormState
//    onGenderSelect: (String) -> Unit // Passes the displayName String
//) {
//    var expanded by remember { mutableStateOf(false) }
//
//    val genders = Gender.entries.filter { it != Gender.ALL }
//
//    ExposedDropdownMenuBox(
//        expanded = expanded,
//        onExpandedChange = { expanded = !expanded },
//    ) {
//        OutlinedTextField(
//            // Display the current String value directly
//            value = selectedGender,
//            onValueChange = {},
//            readOnly = true,
//            label = { Text("Gender") },
//            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
//            modifier = Modifier
//                .menuAnchor()
//                .fillMaxWidth(),
//            colors = customTextFieldColors()
//        )
//        ExposedDropdownMenu(
//            expanded = expanded,
//            onDismissRequest = { expanded = false },
//            modifier = Modifier.background(WhiteColor)
//        ) {
//            genders.forEach { gender ->
//                DropdownMenuItem(
//                    text = {
//                        Text(
//                            text = gender.displayName, // Use the professional display name
//                            color = Color.Black
//                        )
//                    },
//                    onClick = {
//                        onGenderSelect(gender.displayName) // Pass the String displayName
//                        expanded = false
//                    }
//                )
//            }
//        }
//    }
//}
//
//@OptIn(ExperimentalLayoutApi::class)
//@Composable
//fun PainAreaSelection(selectedAreas: List<String>, onToggleArea: (String) -> Unit) {
//    val painAreas = listOf("Neck", "Shoulder", "Back", "Knee", "Elbow", "Hip", "Wrist", "Ankle")
//
//    Column {
//        Text(
//            "Pain Area",
//            style = MaterialTheme.typography.titleMedium,
//            color = Color.Black // Force Black Text
//        )
//        Spacer(Modifier.height(8.dp))
//        FlowRow(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.spacedBy(8.dp),
//            verticalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            painAreas.forEach { area ->
//                val isSelected = selectedAreas.contains(area)
//                FilterChip(
//                    selected = isSelected,
//                    onClick = { onToggleArea(area) },
//                    label = {
//                        Text(
//                            text = area,
//                            color = if(isSelected) WhiteColor else Color.Black
//                        )
//                    },
//                    // Specific colors for Selected vs Unselected state
//                    colors = FilterChipDefaults.filterChipColors(
//                        selectedContainerColor = PrimaryColor,
//                        selectedLabelColor = WhiteColor,
//                        containerColor = WhiteColor,
//                        labelColor = Color.Black,
//                        disabledContainerColor = Color.LightGray,
//                        disabledLabelColor = Color.Gray
//                    ),
//                    border = FilterChipDefaults.filterChipBorder(
//                        enabled = true,
//                        selected = isSelected,
//                        borderColor = if(isSelected) PrimaryColor else Color.Gray
//                    )
//                )
//            }
//        }
//    }
//}
//
//@Composable
//private fun DoctorDetailsCard(name: String, email: String, phone: String) {
//    ElevatedCard( // Changed from Card to ElevatedCard
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(containerColor = CardColor),
//        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Added elevation
//    ) {
//        Row(
//            modifier = Modifier.padding(16.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Icon(
//                imageVector = Icons.Filled.AccountCircle,
//                contentDescription = "Doctor Profile",
//                modifier = Modifier.size(80.dp),
//                tint = PrimaryColor
//            )
//
//            Spacer(modifier = Modifier.width(16.dp))
//
//            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
//                Text(
//                    text = name,
//                    style = MaterialTheme.typography.titleLarge,
//                    fontWeight = FontWeight.Bold
//                )
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    Icon(
//                        imageVector = Icons.Filled.Email,
//                        contentDescription = "Email",
//                        modifier = Modifier.size(16.dp),
//                        tint = Color.Gray
//                    )
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text(text = email, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
//                }
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    Icon(
//                        imageVector = Icons.Filled.Phone,
//                        contentDescription = "Phone",
//                        modifier = Modifier.size(16.dp),
//                        tint = Color.Gray
//                    )
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text(text = phone, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun DashboardActionButton(
//    title: String,
//    subtitle: String,
//    icon: ImageVector,
//    onClick: () -> Unit
//) {
//    ElevatedCard(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable(onClick = onClick),
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = CardColor,
//        ),
//        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//    ) {
//        Row(
//            modifier = Modifier.padding(16.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Icon(
//                imageVector = icon,
//                contentDescription = title,
//                modifier = Modifier.size(48.dp),
//                tint = PrimaryColor
//            )
//            Spacer(modifier = Modifier.width(16.dp))
//            Column {
//                Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
//                Text(text = subtitle, fontSize = 14.sp, color = Color.Gray)
//            }
//        }
//    }
//}