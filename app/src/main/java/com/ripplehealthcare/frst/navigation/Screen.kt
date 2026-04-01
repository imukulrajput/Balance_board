//package com.example.frst.navigation
//
//sealed class Screen(val route: String) {
//    object Welcome : Screen("welcome")
//    object SignUp : Screen("signup")
//    object Login : Screen("login")
//    object ForgotPassword : Screen("forgot_password")
//    object OTP : Screen("otp")
//    object Home : Screen("home")
//    object CalibrationSelection : Screen("calibration")
//    object StandingCalibration : Screen("standingCalibration")
//    object SittingCalibration : Screen("sittingCalibration")
//    object TestSelection : Screen("testSelection")
//
//    object TestInstruction : Screen("testInstruction/{testId}") {
//        fun createRoute(testId: String) = "testInstruction/$testId"
//    }
//
//    object Result : Screen("result")
//    object ReportSelection : Screen("reportSelection")
//    object ReportDetail : Screen("reportDetail")
//    object DotGame : Screen("game")
//}
