package com.ripplehealthcare.frst.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ripplehealthcare.frst.R
import com.ripplehealthcare.frst.data.firebase.GoogleAuthManager
import com.ripplehealthcare.frst.domain.model.AuthState
import com.ripplehealthcare.frst.ui.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.ripplehealthcare.frst.domain.model.CenterAddress
import com.ripplehealthcare.frst.domain.model.CenterProfile
import com.ripplehealthcare.frst.ui.components.authScreen.*
import com.ripplehealthcare.frst.ui.theme.*

@Composable
fun AuthScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    googleAuthManager: GoogleAuthManager
) {
    val authState by authViewModel.authState.collectAsState()
    val errors by authViewModel.formErrors.collectAsState()
    val context = LocalContext.current

    // Navigation and UI State
    var isLoginMode by remember { mutableStateOf(true) }
    var showProfileInput by remember { mutableStateOf(false) }

    // Form States
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var mobNumber by remember { mutableStateOf("") }
    var centerName by remember { mutableStateOf("") }
    var centerAddressObj by remember { mutableStateOf(CenterAddress()) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                authViewModel.signInWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(context, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Restored: Google Places Launcher
    val placePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val place = Autocomplete.getPlaceFromIntent(result.data!!)
            val components = place.addressComponents?.asList() ?: emptyList()
            var city = ""
            var state = ""
            var zip = ""
            for (component in components) {
                val types = component.types
                when {
                    types.contains("locality") -> city = component.name
                    city.isEmpty() && types.contains("sublocality_level_1") -> city = component.name
                    types.contains("administrative_area_level_1") -> state = component.name
                    types.contains("postal_code") -> zip = component.name
                }
            }
            centerAddressObj = CenterAddress(
                fullAddress = place.address ?: "Unknown Address",
                city = city.ifEmpty { "Unknown City" },
                state = state.ifEmpty { "Unknown State" },
                zipCode = zip.ifEmpty { "" }, // Keep empty for validation check
                latitude = place.latLng?.latitude ?: 0.0,
                longitude = place.latLng?.longitude ?: 0.0
            )
        }
    }

    // Restored: Auth State Observer
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> navController.navigate("main") { popUpTo("splash") { inclusive = true } }
            is AuthState.FirstLogin -> {
                showProfileInput = true
                mobNumber = state.user.phoneNumber?.replace("+91", "") ?: ""
            }
            is AuthState.Error ->{
                val errorMessage = (authState as AuthState.Error).message
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
            else -> Unit
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = BgPrimary) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // Branding
            Image(
                painter = painterResource(id = R.drawable.frst_logo),
                contentDescription = "Logo",
                modifier = Modifier.height(65.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(32.dp))

            when {
                showProfileInput -> {
                    CenterDetailsSection(
                        centerName = centerName,
                        addressObj = centerAddressObj,
                        onNameChange = { centerName = it },
                        phoneNumber = mobNumber,
                        errors = errors,
                        onPhoneChange = {mobNumber=it},
                        onAddressClick = {
                            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.ADDRESS_COMPONENTS)
                            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).setCountry("IN").build(context)
                            placePickerLauncher.launch(intent)
                        },
                        onNext = {
                                val currentUser = authViewModel.getCurrentUser()
                                val profile = CenterProfile(
                                    uid = currentUser?.uid ?: "",
                                    centerName = centerName,
                                    address = centerAddressObj,
                                    contactPhone = mobNumber, // Verified in ViewModel
                                    adminEmail = currentUser?.email ?: ""
                                )
                                if (profile.uid.isNotEmpty() && profile.adminEmail.isNotEmpty()) {
                                    authViewModel.processCenterOnboarding(profile)
                                } else {
                                    Toast.makeText(context, "Session error. Please try again.", Toast.LENGTH_SHORT).show()
                                }
                        },
                        isLoading = authState is AuthState.Loading
                    )
                }

                authState is AuthState.EmailNotVerified -> {
                    EmailVerificationSection(
                        email = (authState as AuthState.EmailNotVerified).email,
                        onCheckStatus = { authViewModel.checkEmailVerificationStatus() },
                        onResend = { authViewModel.resendVerificationEmail() },
                        isLoading = authState is AuthState.Loading
                    )
                }

                else -> {
                    if (isLoginMode) {
                        LoginSection(
                            email = email,
                            password = password,
                            onEmailChange = { email = it },
                            onPasswordChange = { password = it },
                            onSignIn = {
                                authViewModel.signInWithIdentity(email, password)
                            },
                            onSwitchToSignUp = { isLoginMode = false },
                            onGoogleSign = { googleSignInLauncher.launch(googleAuthManager.getSignInIntent()) },
                            isLoading = authState is AuthState.Loading
                        )
                    } else {
                        SignUpSection(
                            name = centerName,
                            email = email,
                            password = password,
                            confirmPassword = confirmPassword,
                            onEmailChange = { email = it },
                            onNameChange = { centerName = it },
                            onPasswordChange = { password = it },
                            onConfirmPasswordChange = { confirmPassword = it },
                            onSignUp = { authViewModel.registerWithEmail(email, password) },
                            onSwitchToLogin = { isLoginMode = true },
                            onGoogleSign = { googleSignInLauncher.launch(googleAuthManager.getSignInIntent()) },
                            isLoading = authState is AuthState.Loading
                        )
                    }
                }
            }

            if (authState is AuthState.Loading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp), color = SecondaryColor)
            }
        }
    }
}