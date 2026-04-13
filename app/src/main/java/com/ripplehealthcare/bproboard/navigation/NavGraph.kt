package com.ripplehealthcare.bproboard.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.ripplehealthcare.bproboard.data.firebase.GoogleAuthManager
import com.ripplehealthcare.bproboard.ui.viewmodel.AuthViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.BluetoothViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel
import com.ripplehealthcare.bproboard.ui.screens.*
import com.ripplehealthcare.bproboard.ui.viewmodel.ManagementViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.PatientViewModel

@SuppressLint("NewApi", "ViewModelConstructorInComposable")
@Composable
fun AppNavHost(
    bluetoothViewModel: BluetoothViewModel,
    testViewModel: TestViewModel,
    authViewModel: AuthViewModel,
    managementViewModel: ManagementViewModel,
    patientViewModel: PatientViewModel,
    googleAuthManager: GoogleAuthManager
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {

        // The Splash screen is the entry point. It will decide where to go next.
        composable("splash") {
            SplashScreen(navController = navController, authViewModel)
        }

        composable("authScreen"){
            AuthScreen(navController,authViewModel,googleAuthManager)
        }

        // Main Navigation Graph
        navigation(startDestination = "home", route = "main") {

            composable("home") {
                CenterDashboardScreen(navController, authViewModel, managementViewModel)
            }

            composable(
                route = "doctorDashboard/{doctorId}",
                arguments = listOf(
                    navArgument("doctorId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""

                DoctorDashboardScreen(
                    doctorId = doctorId,
                    navController = navController,
                    authViewModel = authViewModel,
                    managementViewModel = managementViewModel,
                )
            }

            composable("doctorProfile") {
                DoctorProfileScreen(
                    navController = navController,
                    managementViewModel = managementViewModel
                )
            }

            composable(
                route = "patientOnboarding?isEdit={isEdit}",
                arguments = listOf(
                    navArgument("isEdit") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val isEdit = backStackEntry.arguments?.getBoolean("isEdit") ?: false

                PatientOnboardingScreen(
                    navController = navController,
                    managementViewModel = managementViewModel,
                    viewModel = patientViewModel,
                    isEditMode = isEdit // Pass the flag to the screen
                )
            }

            composable("profile") {
                ProfileScreen(navController,authViewModel)
            }
            composable("patientList"){
                PatientListScreen(navController,managementViewModel,testViewModel,patientViewModel)
            }

            composable("testing"){
                TestingScreen(navController,bluetoothViewModel)
            }
            composable("gameSelection"){
                GameSelectionScreen(
                    navController = navController,
                    bluetoothViewModel = bluetoothViewModel,
                    testViewModel = testViewModel // Pass the new parameter here
                )
            }
            composable("starship") {
                StarshipDefenderGame(navController, bluetoothViewModel , testViewModel = testViewModel )
            }
            composable("colorsorter") {
                ColorSorterGame(navController, bluetoothViewModel, testViewModel = testViewModel)
            }

            composable("ratPuzzle") {
                RatPuzzleGame(navController, bluetoothViewModel,testViewModel = testViewModel)
            }
            composable("stepGame") {
                HexagonStepGame(navController, bluetoothViewModel,testViewModel = testViewModel)
            }
            composable("holenavigator") {
                HolePuzzleGame(navController, bluetoothViewModel , testViewModel = testViewModel)
            }
            composable("staticBalance") {
                StaticBalanceScreen(navController, bluetoothViewModel,testViewModel = testViewModel)
            }
            composable("shapeTraining") {
                ShapeTrainingScreen(navController, bluetoothViewModel , testViewModel = testViewModel)
            }
            composable("patternDrawing") {
                PatternDrawingScreen(navController, bluetoothViewModel , testViewModel = testViewModel)
            }
            composable("trainingModeSelection") {
                TrainingModeScreen(navController,testViewModel = testViewModel)
            }
            composable("gameModeSelection") {
                GameModeScreen(navController,testViewModel = testViewModel)
            }
            composable("graphViewSelection") {
                GraphViewScreen(navController,testViewModel = testViewModel)
            }
            composable("boardSetup") {
                BoardSetupScreen(navController, bluetoothViewModel)
            }
        }
    }
}