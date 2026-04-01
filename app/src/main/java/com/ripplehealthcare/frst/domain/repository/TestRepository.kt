// domain/repository/TestRepository.kt
package com.ripplehealthcare.frst.domain.repository

import FourStageResult
import com.ripplehealthcare.frst.data.model.SensorData
import com.ripplehealthcare.frst.domain.model.AccPoint
import com.ripplehealthcare.frst.domain.model.FiveRepResult
import com.ripplehealthcare.frst.domain.model.Patient
import com.ripplehealthcare.frst.domain.model.PatternDrawingResult
import com.ripplehealthcare.frst.domain.model.ShapeTrainingResult
import com.ripplehealthcare.frst.domain.model.StageResultData
import com.ripplehealthcare.frst.domain.model.StaticBalanceResult
import com.ripplehealthcare.frst.domain.model.TestResult
import com.ripplehealthcare.frst.domain.model.TestSession
import com.ripplehealthcare.frst.domain.model.TestState
import com.ripplehealthcare.frst.domain.model.TestType
import com.ripplehealthcare.frst.domain.model.ThirtySecResult
import com.ripplehealthcare.frst.domain.model.TugResult
import com.ripplehealthcare.frst.domain.model.TugState
import com.ripplehealthcare.frst.utils.AllTestReport
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface TestRepository {
    val standingCalibration: StateFlow<SensorData?>
    val sittingCalibration: StateFlow<SensorData?>
    val testState: StateFlow<TestState>
    val tugState: StateFlow<TugState>
    val currentTestType: StateFlow<TestType>
    val fiveRepsTotalReps: StateFlow<Int>
    val fiveRepsTimes: StateFlow<List<Long>>
    val thirtySecondsTotalReps: StateFlow<Int>
    val thirtySecondsTimes: StateFlow<List<Long>>
    val stageResults: StateFlow<Map<Int, StageResultData>>
    val errorEvent: SharedFlow<String>
    val timer: StateFlow<Int>
    val patient: StateFlow<Patient>

    fun setPatient(patient: Patient)
    fun setTestType(type: TestType)
    fun setStandingCalibration(data: SensorData)
    fun setSittingCalibration(data: SensorData)

    fun getStandingCalibration(): SensorData?
    fun getSittingCalibration(): SensorData?
    fun updateTimer(time: Int)
    fun startTest()
    fun stopTest()
    fun resetCalibrationData()
    fun resetTestData(keepTestType: Boolean = false)
    fun onNewAngleReceived(leftPitch: Float, rightPitch: Float)
    fun analyzeTugData(
        accelLeftX: Float, accelLeftY: Float, accelLeftZ: Float,
        accelRightX: Float, accelRightY: Float, accelRightZ: Float,
        leftPitch: Float,
        rightPitch: Float,
        centerRoll: Float
    )
    fun getTestResult(): TestResult
    fun saveStageResult(stage:Int, pointsLeft: List<Pair<Float, Float>>,accLeft:List<AccPoint>, pointsRight: List<Pair<Float, Float>>,accRight:List<AccPoint>, pointsCenter: List<Pair<Float, Float>>,accCenter:List<AccPoint>)

    fun generateNewSessionId(): String
    suspend fun saveSession(session: TestSession)
    suspend fun getPatientSessions(patientId: String, centerId: String, doctorId: String): List<TestSession>

    suspend fun getSessionReportData(centerId: String, sessionId: String, patient: Patient, doctorId: String): AllTestReport

    suspend fun addTugResult(centerId: String, test: TugResult)
    suspend fun addFiveRepResult(centerId: String, test: FiveRepResult)
    suspend fun addThirtySecResult(centerId: String, test: ThirtySecResult)
    suspend fun addFourStageResult(centerId: String, test: FourStageResult)

    suspend fun getTugResults(userId: String, patientId: String): List<TugResult>
    suspend fun getFiveRepResults(userId: String, patientId: String): List<FiveRepResult>
    suspend fun getThirtySecResults(userId: String, patientId: String): List<ThirtySecResult>
    suspend fun getFourStageResults(userId: String, patientId: String): List<FourStageResult>

    suspend fun updateSessionNotes(centerId: String, patientId: String, sessionId: String, notes: String)

    suspend fun addStaticBalanceResult(centerId: String, result: StaticBalanceResult)
    suspend fun getStaticBalanceResults(centerId: String, patientId: String): List<StaticBalanceResult>

    suspend fun addPatternDrawingResult(centerId: String, result: PatternDrawingResult)
    suspend fun getPatternDrawingResults(centerId: String, patientId: String): List<PatternDrawingResult>

    suspend fun addShapeTrainingResult(centerId: String, result: ShapeTrainingResult)
    suspend fun getShapeTrainingResults(centerId: String, patientId: String): List<ShapeTrainingResult>

    suspend fun addColorSorterResult(centerId: String, result: com.ripplehealthcare.frst.domain.model.ColorSorterResult)

    suspend fun getColorSorterResults(centerId: String, patientId: String): List<com.ripplehealthcare.frst.domain.model.ColorSorterResult>

    suspend fun addRatPuzzleResult(centerId: String, result: com.ripplehealthcare.frst.domain.model.RatPuzzleResult)
    suspend fun getRatPuzzleResults(centerId: String, patientId: String): List<com.ripplehealthcare.frst.domain.model.RatPuzzleResult>

    suspend fun addStarshipResult(centerId: String, result: com.ripplehealthcare.frst.domain.model.StarshipResult)
    suspend fun getStarshipResults(centerId: String, patientId: String): List<com.ripplehealthcare.frst.domain.model.StarshipResult>

    suspend fun addHolePuzzleResult(centerId: String, result: com.ripplehealthcare.frst.domain.model.HolePuzzleResult)
    suspend fun getHolePuzzleResults(centerId: String, patientId: String): List<com.ripplehealthcare.frst.domain.model.HolePuzzleResult>

    suspend fun addStepGameResult(centerId: String, result: com.ripplehealthcare.frst.domain.model.StepGameResult)
    suspend fun getStepGameResults(centerId: String, patientId: String): List<com.ripplehealthcare.frst.domain.model.StepGameResult>
}