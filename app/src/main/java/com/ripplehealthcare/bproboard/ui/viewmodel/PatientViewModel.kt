package com.ripplehealthcare.bproboard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ripplehealthcare.bproboard.domain.model.Patient
import com.ripplehealthcare.bproboard.domain.usecase.PatientUseCase
import com.google.firebase.auth.FirebaseAuth
import com.ripplehealthcare.bproboard.domain.model.Gender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewPatientFormState(
    val currentStep: Int = 0, // Tracks which screen the user is on (0 to 4)
    val name: String = "",
    val nameError: String? = null,
    val age: String = "",
    val ageError: String? = null,
    val gender: String = Gender.MALE.displayName,
    val height: String = "",
    val heightError: String? = null,
    val phone: String = "",
    val phoneError: String? = null,
    val email: String = "",
    val emailError: String? = null,
    val weight: String = "",
    val weightError: String? = null,

    // Step 2 & 3: Clinical Questionnaire
    val hasFallenPastYear: Boolean? = null,
    val feelsUnsteady: Boolean? = null,
    val historyOfDizziness: Boolean? = null,
    val needsSupportToStand: Boolean? = null,
    val takesDizzyMedication: Boolean? = null,
    val worriedAboutFalling: Boolean? = null,

    // Step 4: Pain Areas
    val painAreas: List<String> = emptyList()
)

class PatientViewModel(private val useCase: PatientUseCase) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _patients = MutableStateFlow<List<Patient>>(emptyList())

    // Simple state for showing a loading indicator
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Simple state for showing an error message
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _formState = MutableStateFlow(NewPatientFormState())
    val formState = _formState.asStateFlow()

    private val _selectedPatient = MutableStateFlow<Patient?>(null)
    val selectedPatient = _selectedPatient.asStateFlow()

    private var lastLoadedDoctorId: String? = null

    // --- ADD THIS: State for search and filtering ---
    val searchQuery = MutableStateFlow("")
    val selectedGender = MutableStateFlow(Gender.ALL)

    fun updateName(name: String) = _formState.update { it.copy(name = name) }
    fun updateAge(age: String) = _formState.update { it.copy(age = age) }
    fun updateGender(gender: String) = _formState.update { it.copy(gender = gender) }
    fun updateHeight(height: String) = _formState.update { it.copy(height = height) }
    fun updatePhone(phone: String) = _formState.update { it.copy(phone = phone) }
    fun updateEmail(email: String) = _formState.update { it.copy(email = email) }
    fun updateWeight(weight: String) = _formState.update { it.copy(weight = weight) }

    val filteredPatients: StateFlow<List<Patient>> = combine(
        _patients,
        searchQuery,
        selectedGender
    ) { patients, query, genderFilter ->
        patients.filter { patient ->
            // Use Gender.ALL to bypass filtering, otherwise compare display names
            val matchesGender = genderFilter == Gender.ALL ||
                    patient.gender == genderFilter.displayName

            val matchesQuery = query.isBlank() ||
                    patient.phone.contains(query) ||
                    patient.name.contains(query, ignoreCase = true)

            matchesGender && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Updated inside PatientViewModel.kt
    fun getPatientById(centerId: String, patientId: String) {
        if (_selectedPatient.value?.patientId == patientId) {
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null // Clear previous errors

            try {
                // 1. Check local list first for performance
                val localPatient = _patients.value.find { it.patientId == patientId }

                if (localPatient != null) {
                    _selectedPatient.value = localPatient
                } else {
                    // 2. Fallback: Direct remote fetch from usecase/repository
                    // Note: Ensure your useCase has getPatientById(centerId, patientId)
                    val remotePatient = useCase.getPatientById(centerId, patientId)
                    if (remotePatient != null) {
                        _selectedPatient.value = remotePatient
                    } else {
                        _error.value = "Patient profile not found."
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to fetch patient details."
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun validateForm(): Boolean {
        val formData = _formState.value
        val nameError = if (formData.name.isBlank()) "Name cannot be empty" else null
        val ageError = when {
            formData.age.isBlank() -> "Age cannot be empty"
            formData.age.toIntOrNull() == null -> "Enter a valid number"
            formData.age.toInt() <= 10 -> "Age must be greater than 10"
            else -> null
        }
        val heightError = if (formData.height.isNotEmpty() && (formData.height.toFloatOrNull() == null || formData.height.toFloat() <= 0f)) {
            "Enter a valid positive number for height"
        } else {
            null
        }

        val weightError = if (formData.weight.isNotEmpty() && (formData.weight.toFloatOrNull() == null || formData.weight.toFloat() <= 0f)) {
            "Enter a valid positive number for weight"
        } else {
            null
        }
        val phoneError = if (formData.phone.length < 10) "Enter a valid 10-digit phone number" else null

        // Basic email validation (optional field)
        val emailError = if (formData.email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(formData.email).matches()) {
            "Enter a valid email address"
        } else {
            null
        }

        _formState.update {
            it.copy(
                nameError = nameError,
                ageError = ageError,
                heightError = heightError,
                weightError = weightError,
                phoneError = phoneError,
                emailError = emailError
            )
        }

        // Return true if all error fields are null
        return listOfNotNull(nameError, ageError, heightError, weightError, phoneError, emailError).isEmpty()
    }

    fun togglePainArea(area: String) {
        _formState.update { currentState ->
            // Convert the list to a mutable list for easier manipulation
            val newPainAreas = currentState.painAreas.toMutableList()
            if (newPainAreas.contains(area)) {
                newPainAreas.remove(area)
            } else {
                newPainAreas.add(area)
            }
            currentState.copy(painAreas = newPainAreas) // Save the updated list
        }
    }

    fun loadPatientsForDoctor(centerId: String, doctorId: String, forceRefresh: Boolean = false) {

        if (!forceRefresh && lastLoadedDoctorId == doctorId && _patients.value.isNotEmpty()) {
            return
        }
        viewModelScope.launch {
            _patients.value = emptyList()
            _isLoading.value = true
            try {
                _patients.value = useCase.getPatientsByDoctor(centerId, doctorId)
                lastLoadedDoctorId = doctorId
            } catch (e: Exception) {
                _error.value = "Failed to load patients for this doctor"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadPatients() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _patients.value = useCase.getPatients(userId)
            } catch (e: Exception) {
                _error.value = "Failed to load patients"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadPatientIntoForm(patient: Patient) {
        _formState.value = NewPatientFormState(
            currentStep = 0, // Start at the beginning
            name = patient.name,
            age = patient.age,
            gender = patient.gender,
            height = patient.height,
            phone = patient.phone.replace("+91", ""), // Clean for editing
            email = patient.email,
            weight = patient.weight,
            painAreas = patient.painAreas,
            // Pre-fill clinical questionnaire
            hasFallenPastYear = patient.fallHistory,
            feelsUnsteady = patient.unsteadiness,
            historyOfDizziness = patient.dizziness,
            needsSupportToStand = patient.supportNeeded,
            takesDizzyMedication = patient.dizzyMeds,
            worriedAboutFalling = patient.fearOfFalling
        )
    }

    fun updatePatientDetails(patientId: String, centerId: String, doctorId: String, onComplete: () -> Unit) {
        if (!validateForm()) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val formData = _formState.value
                val updatedPatient = Patient(
                    patientId = patientId,
                    centerId = centerId,
                    doctorId = doctorId,
                    name = formData.name,
                    age = formData.age,
                    gender = formData.gender,
                    phone = "+91${formData.phone}",
                    email = formData.email,
                    height = formData.height,
                    weight = formData.weight,
                    painAreas = formData.painAreas,
                    fallHistory = formData.hasFallenPastYear ?: false,
                    unsteadiness = formData.feelsUnsteady ?: false,
                    dizziness = formData.historyOfDizziness ?: false,
                    supportNeeded = formData.needsSupportToStand ?: false,
                    dizzyMeds = formData.takesDizzyMedication ?: false,
                    fearOfFalling = formData.worriedAboutFalling ?: false
                )

                useCase.updatePatient(updatedPatient)
                _selectedPatient.value = updatedPatient
                _patients.update { currentList ->
                    currentList.map { existingPatient ->
                        if (existingPatient.patientId == patientId) updatedPatient else existingPatient
                    }
                }
                clearForm()
                onComplete()
            } catch (e: Exception) {
                _error.value = "Failed to update details"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun clearForm() {
        _formState.value = NewPatientFormState()
    }

    fun clearError() {
        _error.value = null
    }

    fun nextStep() {
        val currentState = _formState.value.currentStep
        // Validate if the current step is complete before moving forward
        if (currentState == 0 && !canProceedFromStep0()) return

        if (currentState < 3) { // Clamped to Step 3 (Pain Mapping)
            _formState.update { it.copy(currentStep = it.currentStep + 1) }
        }
    }

    fun previousStep() {
        if (_formState.value.currentStep > 0) {
            _formState.update { it.copy(currentStep = it.currentStep - 1) }
        }
    }

    // Questionnaire Updates
    fun updateQuestion(questionIndex: Int, answer: Boolean) {
        _formState.update { state ->
            when (questionIndex) {
                0 -> state.copy(hasFallenPastYear = answer)
                1 -> state.copy(feelsUnsteady = answer)
                2 -> state.copy(historyOfDizziness = answer)
                3 -> state.copy(needsSupportToStand = answer)
                4 -> state.copy(takesDizzyMedication = answer)
                5 -> state.copy(worriedAboutFalling = answer)
                else -> state
            }
        }
    }
    fun canProceedFromStep0(): Boolean {
        val isValid = validateForm()
        val state = _formState.value

        // 2. Strict check for Step 0 (Identity)
        return state.name.isNotBlank() &&
                state.age.isNotBlank() &&
                state.phone.isNotBlank() &&
                state.nameError == null &&
                state.ageError == null &&
                state.phoneError == null &&
                state.emailError == null
    }

    suspend fun saveOnboarding(centerId: String, doctorId: String): Patient? {
        _isLoading.value = true
        return try {
            val formData = _formState.value
            val patientToAdd = Patient(
                centerId = centerId,
                doctorId = doctorId,
                name = formData.name,
                age = formData.age,
                gender = formData.gender,
                phone = formData.phone,
                email = formData.email,
                height = formData.height,
                weight = formData.weight,
                painAreas = formData.painAreas,
                // Include clinical assessment
                fallHistory = formData.hasFallenPastYear ?: false,
                unsteadiness = formData.feelsUnsteady ?: false,
                dizziness = formData.historyOfDizziness ?: false,
                supportNeeded = formData.needsSupportToStand ?: false,
                dizzyMeds = formData.takesDizzyMedication ?: false,
                fearOfFalling = formData.worriedAboutFalling ?: false
            )

            val newPatient = useCase.addPatient(centerId, patientToAdd)
            loadPatientsForDoctor(centerId, doctorId, true)
            clearForm()
            newPatient
        } catch (e: Exception) {
            _error.value = "Failed to complete onboarding"
            null
        } finally {
            _isLoading.value = false
        }
    }

    fun transferPatient(patientId: String, centerId: String, newDoctorId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = useCase.transferPatient(centerId, patientId, newDoctorId)

                if (result.isSuccess) {
                    _patients.update { currentList ->
                        currentList.filter { it.patientId != patientId }
                    }

                    _selectedPatient.value = null

                    onComplete()
                } else {
                    _error.value = "Transfer failed"
                }
            } catch (e: Exception) {
                _error.value = "An error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
