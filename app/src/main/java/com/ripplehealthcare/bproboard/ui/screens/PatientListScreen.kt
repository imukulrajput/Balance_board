package com.ripplehealthcare.bproboard.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ripplehealthcare.bproboard.domain.model.Gender
import com.ripplehealthcare.bproboard.domain.model.Patient
import com.ripplehealthcare.bproboard.domain.model.TestType
import com.ripplehealthcare.bproboard.ui.theme.PrimaryColor
import com.ripplehealthcare.bproboard.ui.viewmodel.PatientViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel
import com.ripplehealthcare.bproboard.ui.components.TopBar
import com.ripplehealthcare.bproboard.ui.components.customTextFieldColors
import com.ripplehealthcare.bproboard.ui.theme.WhiteColor
import com.ripplehealthcare.bproboard.ui.viewmodel.ManagementViewModel
import kotlinx.coroutines.launch

@Composable
fun PatientListScreen(
    navController: NavController,
    managementViewModel: ManagementViewModel,
    testViewModel: TestViewModel,
    patientViewModel: PatientViewModel
) {
    val isLoading by patientViewModel.isLoading.collectAsState()
    val error by patientViewModel.error.collectAsState()
    val searchQuery by patientViewModel.searchQuery.collectAsState()
    val selectedGender by patientViewModel.selectedGender.collectAsState()
    val filteredPatients by patientViewModel.filteredPatients.collectAsState()
    val doctor by managementViewModel.selectedDoctor.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(error) {
        error?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                patientViewModel.clearError()
            }
        }
    }

    LaunchedEffect(doctor?.id) {
        doctor?.let {
            patientViewModel.loadPatientsForDoctor(it.centerId, it.id)
        }
    }

    Scaffold(
        topBar = {
            TopBar("Patients", onBackClick = { navController.popBackStack() })
        },
        containerColor = Color(0xFFF0F4F8),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { patientViewModel.searchQuery.value = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                placeholder = { Text("Search name or phone...") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = PrimaryColor
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = customTextFieldColors()
            )

            Spacer(modifier = Modifier.height(16.dp))

            FilterSection(
                selectedGender = selectedGender,
                onGenderSelect = { patientViewModel.selectedGender.value = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredPatients.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No patients found.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredPatients, key = { it.patientId }) { patient ->
                        PatientCard(
                            patient = patient,
                            onClick = {
                                testViewModel.setPatient(patient)
                                testViewModel.setTestType(TestType.NONE)
                                testViewModel.resetTestData()
                                // Navigate directly to game selection
                                navController.navigate("gameSelection")
                            },
                            onEditClick = {
                                patientViewModel.loadPatientIntoForm(patient)
                                navController.navigate("patientOnboarding?isEdit=true")
                            }
                        )
                    }
                }
            }
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryColor)
                }
            }
        }
    }
}

@Composable
fun FilterSection(
    selectedGender: Gender,
    onGenderSelect: (Gender) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Gender.entries.forEach { gender ->
            val isSelected = selectedGender == gender
            FilterChip(
                selected = isSelected,
                onClick = { onGenderSelect(gender) },
                label = {
                    Text(
                        text = gender.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = if (isSelected) WhiteColor else Color.Black
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryColor,
                    containerColor = WhiteColor
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) PrimaryColor else Color.Gray,
                    borderWidth = 1.dp
                )
            )
        }
    }
}

@Composable
fun PatientCard(
    patient: Patient,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryColor.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (patient.gender == "Female") "👩" else "👨",
                            fontSize = 32.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = patient.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${patient.gender} • Age: ${patient.age}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // Replacing Chevron with 3-dot Menu
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.Gray)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Information") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = {
                                showMenu = false
                                onEditClick()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (patient.fallHistory) RiskBadge("Fall History", Color.Red)
                if (patient.unsteadiness) RiskBadge("Unsteady", Color(0xFFFFA000))
                if (patient.fearOfFalling) RiskBadge("Fear of Falling", Color.Red)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(patient.phone, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun RiskBadge(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}