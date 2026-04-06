package com.ripplehealthcare.bproboard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ripplehealthcare.bproboard.data.firebase.ManagementRepositoryImpl
import com.ripplehealthcare.bproboard.domain.model.DoctorProfile
import com.ripplehealthcare.bproboard.domain.repository.ManagementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ManagementViewModel(
    private val repository: ManagementRepository = ManagementRepositoryImpl()
) : ViewModel() {

    private val _doctors = MutableStateFlow<List<DoctorProfile>>(emptyList())
    val doctors: StateFlow<List<DoctorProfile>> = _doctors.asStateFlow()

    private val _uiState = MutableStateFlow<ManagementUiState>(ManagementUiState.Idle)
    val uiState: StateFlow<ManagementUiState> = _uiState.asStateFlow()

    private val _selectedDoctor = MutableStateFlow<DoctorProfile?>(null)
    val selectedDoctor = _selectedDoctor.asStateFlow()

    fun selectDoctor(doctor: DoctorProfile) {
        _selectedDoctor.value = doctor
    }

    // Load doctors automatically when centerId is known
    fun loadDoctors(centerId: String) {
        viewModelScope.launch {
            repository.getDoctors(centerId).collect {
                _doctors.value = it
            }
        }
    }

    fun addNewDoctor(
        centerId: String,
        name: String,
        phone: String,
        email: String,
        gender: String,
        specialization: String
    ) {
        viewModelScope.launch {
            _uiState.value = ManagementUiState.Loading
            val newDoctor = DoctorProfile(
                centerId = centerId,
                name = name,
                phone = phone,
                email = email,
                gender = gender,
                specialization = specialization
            )
            val result = repository.addDoctor(centerId, newDoctor)

            if (result.isSuccess) {
                _uiState.value = ManagementUiState.Success("Doctor added successfully")
            } else {
                _uiState.value = ManagementUiState.Error(result.exceptionOrNull()?.message ?: "Failed to add doctor")
            }
        }
    }

    fun getDoctorById(centerId: String, doctorId: String) {
        viewModelScope.launch {
            _uiState.value = ManagementUiState.Loading

            try {
                val localDoctor = _doctors.value.find { it.id == doctorId }

                if (localDoctor != null) {
                    _selectedDoctor.value = localDoctor
                    _uiState.value = ManagementUiState.Idle
                } else {
                    val result = repository.getDoctorById(centerId, doctorId)
                    if (result != null) {
                        _selectedDoctor.value = result
                        _uiState.value = ManagementUiState.Idle
                    } else {
                        _uiState.value = ManagementUiState.Error("Doctor profile not found.")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = ManagementUiState.Error(e.message ?: "An unexpected error occurred.")
            }
        }
    }
}

sealed class ManagementUiState {
    object Idle : ManagementUiState()
    object Loading : ManagementUiState()
    data class Success(val message: String) : ManagementUiState()
    data class Error(val message: String) : ManagementUiState()
}