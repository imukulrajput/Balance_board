package com.ripplehealthcare.bproboard.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ripplehealthcare.bproboard.data.firebase.AuthRepositoryImpl
import com.ripplehealthcare.bproboard.domain.model.AuthState
import com.ripplehealthcare.bproboard.domain.usecase.AuthUseCase
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.functions
import com.ripplehealthcare.bproboard.domain.model.CenterProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class InputType { PHONE, EMAIL, INVALID }

class AuthViewModel(
    private val authUseCase: AuthUseCase = AuthUseCase(AuthRepositoryImpl())
) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val functions = Firebase.functions
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    private val _formErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val formErrors: StateFlow<Map<String, String>> = _formErrors
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _centerProfile = MutableStateFlow<CenterProfile?>(null)
    val centerProfile: StateFlow<CenterProfile?> = _centerProfile

    private val _isCheckingPhone = MutableStateFlow(false)
    val isCheckingPhone = _isCheckingPhone.asStateFlow()

    // AuthStateListener to keep ViewModel state in sync
    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            // Point to the Center-specific check
            handleFirstCenterLogin(user)
        } else {
            _authState.value = AuthState.Idle
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
    }

    // Google: token -> firebase sign in
    fun signInWithGoogle(idToken: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            authUseCase.signInWithGoogle(idToken).collect { state ->
                when (state) {
                    is AuthState.Authenticated -> handleFirstCenterLogin(state.user)
                    else -> _authState.value = state
                }
            }
        }
    }

    private fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            try {
                authUseCase.signInEmail(email, password).collect { state ->
                    if (state is AuthState.Authenticated) {
                        handleFirstCenterLogin(state.user)
                    } else if (state is AuthState.Error) {
                        _authState.value = state
                    }
                }
            } catch (e: Exception) {
                _authState.value = handleFirebaseError(e)
            }
        }
    }

    fun registerWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                authUseCase.signUpEmail(email, password).collect { state ->
                    if (state is AuthState.Authenticated) {
                        handleFirstCenterLogin(state.user)
                    } else {
                        _authState.value = state
                    }
                }
            } catch (e: Exception) {
                _authState.value = handleFirebaseError(e)
            }
        }
    }

    fun checkEmailVerificationStatus() {
        val user = auth.currentUser
        user?.reload()?.addOnCompleteListener {
            if (user.isEmailVerified) {
                handleFirstCenterLogin(user) // Proceed to profile check or main screen
            } else {
                _authState.value = AuthState.Error("Please verify your email first. Check your inbox.")
            }
        }
    }

    fun resendVerificationEmail() {
        auth.currentUser?.sendEmailVerification()
            ?.addOnSuccessListener { _authState.value = AuthState.Error("Verification email sent!") }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _authState.value = AuthState.Error("Please enter your email address.")
            return
        }
        viewModelScope.launch {
            authUseCase.resetPassword(email).collect { state ->
                _authState.value = state
            }
        }
    }

    // Sign out
    fun signOut() {
        authUseCase.signOut()
        _authState.value = AuthState.Idle
    }

    // AuthViewModel.kt

    private fun validateCenterProfile(profile: CenterProfile): Boolean {
        val errors = mutableMapOf<String, String>()

        // 1. Center Name: Minimum 3 chars, letters/numbers only
        if (profile.centerName.trim().length < 3) {
            errors["clinicName"] = "Center name must be at least 3 characters."
        }

        // 2. Phone: Must be exactly 10 digits (for India)
        val phoneRegex = "^[6-9]\\d{9}$".toRegex()
        if (!profile.contactPhone.matches(phoneRegex)) {
            errors["phone"] = "Enter a valid 10-digit mobile number."
        }

        // 3. Address: Ensure Google Places actually returned a city and zip
        if (profile.address.fullAddress.isBlank()) {
            errors["clinicAddress"] = "Please select a verified location."
        } else if (profile.address.zipCode.isBlank()) {
            errors["zipCode"] = "Pincode is missing for this location."
        }

        _formErrors.value = errors
        return errors.isEmpty()
    }

    // Update the login handler to look in the 'centers' collection
    private fun handleFirstCenterLogin(user: FirebaseUser) {
        val isEmailUser = user.providerData.any { it.providerId == "password" }

        if (isEmailUser && !user.isEmailVerified) {
            _authState.value = AuthState.EmailNotVerified(user.email ?: "")
            return
        }
        viewModelScope.launch {
            try {
                val existingCenter = authUseCase.getCenterProfile(user.uid)
                if (existingCenter == null) {
                    _authState.value = AuthState.FirstLogin(user)
                } else {
                    _centerProfile.value = existingCenter
                    _authState.value = AuthState.Success(user)
                }
            } catch (e: Exception) {
                // Catches Firestore-specific fetch errors
                _authState.value = handleFirebaseError(e)
            }
        }
    }

    fun processCenterOnboarding(profile: CenterProfile) {
        if (!validateCenterProfile(profile)) return

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _isCheckingPhone.value = true

            try {
                val db = FirebaseFirestore.getInstance()
                // Check for Unique Phone Number
                val query = db.collection("centers")
                    .whereEqualTo("contactPhone", profile.contactPhone)
                    .get()
                    .await()

                if (!query.isEmpty) {
                    _authState.value = AuthState.Error("This phone number is already registered.")
                    return@launch
                }

                // Save profile
                authUseCase.saveCenterProfile(profile)

                // FIX: Manually update the centerProfile state after successful save
                _centerProfile.value = profile

                _authState.value = AuthState.Success(auth.currentUser!!)
            } catch (e: Exception) {
                _authState.value = handleFirebaseError(e)
            } finally {
                _isCheckingPhone.value = false
            }
        }
    }

    private fun identifyInputType(input: String): InputType {
        val trimmed = input.trim()
        return when {
            trimmed.contains("@") -> {
                if (android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches())
                    InputType.EMAIL
                else
                    InputType.INVALID
            }
            trimmed.all { it.isDigit() } -> {
                if (trimmed.length == 10 && trimmed.first() in '6'..'9')
                    InputType.PHONE
                else
                    InputType.INVALID
            }
            else -> InputType.INVALID
        }
    }

    fun signInWithIdentity(identity: String, pass: String) {
        val type = identifyInputType(identity)

        viewModelScope.launch {
            _authState.value = AuthState.Loading

            val emailToUse = when (type) {
                InputType.EMAIL -> identity
                InputType.PHONE -> {
                    // Call the repository to find the email
                    val resolvedEmail = authUseCase.getEmailFromPhone(identity)
                    if (resolvedEmail == null) {
                        _authState.value = AuthState.Error("No account found with this phone number.")
                        return@launch
                    }
                    resolvedEmail
                }
                InputType.INVALID -> {
                    _authState.value = AuthState.Error("Invalid phone or email format.")
                    return@launch
                }
            }

            // Perform the actual Firebase Email/Password login
            loginWithEmail(emailToUse, pass)
        }
    }

    private fun handleFirebaseError(e: Exception): AuthState {
        Log.e("AuthVM", "Firebase Operation Failed", e)

        val message = when (e) {
            // Authentication Exceptions
            is com.google.firebase.auth.FirebaseAuthInvalidUserException ->
                "No account found with this email."
            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
                "Incorrect password. Please try again."
            is com.google.firebase.auth.FirebaseAuthUserCollisionException ->
                "This email is already in use by another account."
            is com.google.firebase.auth.FirebaseAuthWeakPasswordException ->
                "The password is too weak. Use at least 6 characters."

            // Network/Server Exceptions
            is com.google.firebase.FirebaseNetworkException ->
                "No internet connection. Please check your network."
            is com.google.firebase.firestore.FirebaseFirestoreException ->
                "Database error. Please try again later."

            else -> e.localizedMessage ?: "An unexpected error occurred."
        }

        return AuthState.Error(message)
    }

}