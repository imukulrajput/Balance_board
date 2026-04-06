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

//            composable("home") {
//                DashboardScreen(navController,authViewModel,patientViewModel,testViewModel)
//            }
            composable(
                route = "patient_detail/{patientId}",
                arguments = listOf(navArgument("patientId") { type = NavType.StringType })
            ) { backStackEntry ->
                // Extract the ID from the arguments
                val patientId = backStackEntry.arguments?.getString("patientId")
                requireNotNull(patientId) { "Patient ID is required" }

                // Pass the extracted ID to your screen
                PatientDashboardScreen(
                    navController = navController,
                    patientId = patientId,
                    patientViewModel = patientViewModel,
                    managementViewModel = managementViewModel,
                    bluetoothViewModel = bluetoothViewModel,
                    testViewModel = testViewModel
                )
            }
            composable("profile") {
                ProfileScreen(navController,authViewModel)
            }
            composable("patientList"){
                PatientListScreen(navController,managementViewModel,testViewModel,patientViewModel)
            }
            composable("calibration") {
                CalibrationSelectionScreen(navController, bluetoothViewModel, testViewModel)
            }
            composable("standingCalibration") {
                StandingCalibrationScreen(navController, bluetoothViewModel, testViewModel)
            }
            composable("sittingCalibration") {
                SittingCalibrationScreen(navController, bluetoothViewModel, testViewModel)
            }

            composable("walkCalibration"){
                WalkCalibrationScreen(navController,bluetoothViewModel)
            }
            composable("sessionDashboard") {
                SessionDashboardScreen(navController, testViewModel, patientViewModel, bluetoothViewModel)
            }
            composable("testing"){
                TestingScreen(navController,bluetoothViewModel)
            }
            composable(
                route = "testInstruction/{testId}",
                arguments = listOf(navArgument("testId") { type = NavType.StringType })
            ) { backStackEntry ->
                val testId = backStackEntry.arguments?.getString("testId")
                TestInstructionScreen(navController, testId, bluetoothViewModel, testViewModel)
            }
            composable("fourStage"){
                FourStageTestSelectionScreen(navController,testViewModel,bluetoothViewModel, patientViewModel)
            }
            composable("tug"){
                TUGTestScreen(navController,bluetoothViewModel,testViewModel, patientViewModel)
            }
            composable("sitToStandTest"){
                SitToStandTestScreen(navController,bluetoothViewModel,testViewModel, patientViewModel)
            }
            composable("gameSelection"){
                GameSelectionScreen(
                    navController = navController,
                    bluetoothViewModel = bluetoothViewModel,
                    testViewModel = testViewModel // Pass the new parameter here
                )
            }
            composable("endlessRunner"){
                EndlessRunnerGame(navController, bluetoothViewModel)
            }
            composable("spaceDebris") {
                SpaceDebrisGame(navController, bluetoothViewModel)
            }
            composable("starship") {
                StarshipDefenderGame(navController, bluetoothViewModel , testViewModel = testViewModel )
            }
            composable("colorsorter") {
                ColorSorterGame(navController, bluetoothViewModel, testViewModel = testViewModel)
            }
            composable("rockClimbing") {
                RockClimbingGame(navController, bluetoothViewModel)
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
                TrainingModeScreen(navController)
            }
            composable("gameModeSelection") {
                // You can duplicate the TrainingModeScreen code to create GameModeScreen
                GameModeScreen(navController)
            }
            composable("graphViewSelection") {
                GraphViewScreen(navController,testViewModel = testViewModel)
            }
            composable("boardSetup") {
                BoardSetupScreen(navController, bluetoothViewModel)
            }






            composable("sessionReport"){
                SessionReportScreen(navController,testViewModel)
            }
            composable("fourStageInstruction/{stageId}") { backStackEntry ->
                val stageId = backStackEntry.arguments?.getString("stageId")
                // Show instruction screen for specific stage
                FourStageInstructionScreen(navController, stageId, bluetoothViewModel)

            }
            composable("result") {
                ResultScreen(navController, testViewModel)
            }
            composable("reportSelection") {
                TestsHistoryScreen(navController,patientViewModel,testViewModel)
            }
            composable("fourStageReport") {
                FourStageReportScreen(navController, testViewModel)
            }
            composable(
                route = "stsReport/{testType}",
                arguments = listOf(navArgument("testType") { type = NavType.StringType })
            ) { backStackEntry ->
                val testTypeStr = backStackEntry.arguments?.getString("testType") ?: "FIVE_REPS"
                SitToStandReportScreen(
                    navController = navController,
                    testViewModel = testViewModel, // Shared ViewModel
                    testTypeStr = testTypeStr
                )
            }
            composable("tugReport") {
                TugReportScreen(navController, testViewModel)
            }
            composable("game") {
                FourStageTestScreen(navController, bluetoothViewModel,testViewModel, patientViewModel)
            }
        }
    }
}