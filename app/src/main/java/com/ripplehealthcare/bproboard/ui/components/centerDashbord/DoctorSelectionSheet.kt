package com.ripplehealthcare.bproboard.ui.components.centerDashbord

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ripplehealthcare.bproboard.domain.model.DoctorProfile
import com.ripplehealthcare.bproboard.domain.model.Gender
import com.ripplehealthcare.bproboard.ui.components.customTextFieldColors
import com.ripplehealthcare.bproboard.ui.screens.DoctorCard
import com.ripplehealthcare.bproboard.ui.theme.PrimaryColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DoctorSelectionSheet(
    doctors: List<DoctorProfile>,
    selectedDoctorId: String?,
    onDoctorSelected: (DoctorProfile) -> Unit,
    onAddNewDoctor: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterGender by remember { mutableStateOf(Gender.ALL) }

    // Logic for filtering
    val filteredDoctors = doctors.filter { doctor ->
        val matchesName = doctor.name.contains(searchQuery, ignoreCase = true)
        val matchesGender = filterGender == Gender.ALL || doctor.gender == filterGender.displayName
        matchesName && matchesGender
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        modifier = Modifier.fillMaxHeight(0.95f) // Near full height
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Select Doctor", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onAddNewDoctor) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, tint = PrimaryColor)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                colors = customTextFieldColors()
            )

            Spacer(modifier = Modifier.height(12.dp))
            // Gender Filter Chips
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Gender.entries.forEach { gender ->
                    FilterChip(
                        selected = filterGender == gender,
                        onClick = { filterGender = gender },
                        label = { Text(gender.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryColor.copy(alpha = 0.1f),
                            selectedLabelColor = PrimaryColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Doctor List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filteredDoctors.isEmpty()) {
                    item {
                        Text(
                            "No doctors found matching filters.",
                            modifier = Modifier.padding(top = 20.dp),
                            color = Color.Gray
                        )
                    }
                }
                items(filteredDoctors) { doctor ->
                    DoctorCard(
                        doctor = doctor,
                        isSelected = doctor.id == selectedDoctorId,
                        onClick = { onDoctorSelected(doctor) },
                        accentColor = PrimaryColor
                    )
                }
            }
        }
    }
}