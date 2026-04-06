package com.ripplehealthcare.bproboard

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.ripplehealthcare.bproboard.core.di.AppContainer
import com.ripplehealthcare.bproboard.navigation.AppNavHost
import com.ripplehealthcare.bproboard.ui.theme.FRSTTheme
import com.ripplehealthcare.bproboard.ui.viewmodel.AuthViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.BluetoothViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel
import android.Manifest
import com.google.android.libraries.places.api.Places
import com.ripplehealthcare.bproboard.ui.viewmodel.ManagementViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.PatientViewModel

class MainActivity : ComponentActivity() {
    private lateinit var appContainer: AppContainer
    private lateinit var authViewModel: AuthViewModel

    private lateinit var bluetoothViewModel: BluetoothViewModel
    private lateinit var testViewModel: TestViewModel
    private lateinit var patientViewModel: PatientViewModel
    private lateinit var managementViewModel: ManagementViewModel

    // Request multiple Bluetooth permissions
    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.all { it.value }
        if (granted) {
            Log.d("MainActivity", "Bluetooth permissions granted")
            appContainer.getBluetoothViewModel(this).getPairedDevices()
        } else {
            Log.e("MainActivity", "Bluetooth permissions denied")
        }
    }

    @SuppressLint("NewApi")
    @Suppress("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext,BuildConfig.MAPS_API_KEY)
        }
        // Initialize appContainer after activity is created
        appContainer = (application as BalanceApp).appContainer
        authViewModel = appContainer.getAuthViewModel()
        bluetoothViewModel = appContainer.getBluetoothViewModel(this)
        testViewModel = appContainer.getTestViewModel()
        patientViewModel = appContainer.getPatientViewModel()
        managementViewModel = appContainer.getManagementViewModel()

        val googleAuthManager = appContainer.getGoogleAuthManager(this@MainActivity)
        // Request Bluetooth permissions on startup
        bluetoothPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
        enableEdgeToEdge()

        setContent {
            FRSTTheme {
               AppNavHost(bluetoothViewModel,testViewModel,authViewModel,managementViewModel,patientViewModel,googleAuthManager)
            }
        }
    }
}
