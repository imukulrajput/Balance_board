package com.ripplehealthcare.frst.ui.viewmodel

import FourStageResult
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ripplehealthcare.frst.data.model.SensorData
import com.ripplehealthcare.frst.domain.model.AccPoint
import com.ripplehealthcare.frst.domain.model.FiveRepResult
import com.ripplehealthcare.frst.domain.model.Patient
import com.ripplehealthcare.frst.domain.model.RepetitionStats
import com.ripplehealthcare.frst.domain.model.SensorAxisStats
import com.ripplehealthcare.frst.domain.model.StageResultData
import com.ripplehealthcare.frst.domain.model.StaticBalanceResult
import com.ripplehealthcare.frst.domain.model.TestSession
import com.ripplehealthcare.frst.domain.model.TestState
import com.ripplehealthcare.frst.domain.model.TestType
import com.ripplehealthcare.frst.domain.model.ThirtySecResult
import com.ripplehealthcare.frst.domain.model.TugResult
import com.ripplehealthcare.frst.domain.model.TugState
import com.ripplehealthcare.frst.domain.repository.TestRepository
import com.ripplehealthcare.frst.domain.usecase.StartTestUseCase
import com.ripplehealthcare.frst.domain.usecase.StopTestUseCase
import com.ripplehealthcare.frst.utils.AllTestReport
import com.google.firebase.auth.FirebaseAuth
import com.ripplehealthcare.frst.domain.model.ColorSorterResult
import com.ripplehealthcare.frst.domain.model.HolePuzzleResult
import com.ripplehealthcare.frst.domain.model.PatternDrawingResult
import com.ripplehealthcare.frst.domain.model.RatPuzzleResult
import com.ripplehealthcare.frst.domain.model.ShapeTrainingResult
import com.ripplehealthcare.frst.domain.model.StarshipResult
import com.ripplehealthcare.frst.domain.model.StepGameResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

class TestViewModel(private val testRepository: TestRepository) : ViewModel() {

    var trainingPosture: String = "STANDING"

    private val auth = FirebaseAuth.getInstance()
    private val currentUserId get() = auth.currentUser?.uid ?: ""

    val standingCalibration: StateFlow<SensorData?> = testRepository.standingCalibration
    val sittingCalibration: StateFlow<SensorData?> = testRepository.sittingCalibration

    private val _navigateToResultEvent = MutableSharedFlow<Unit>()
    val navigateToResultEvent = _navigateToResultEvent.asSharedFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId = _currentSessionId.asStateFlow()

    private val _sessionProgress = MutableStateFlow<Map<TestType, Boolean>>(emptyMap())
    val sessionProgress = _sessionProgress.asStateFlow()

    private val _isFiveRepCompleted = MutableStateFlow(false)

    private val _isThirtySecCompleted = MutableStateFlow(false)

    private val _currentStage = MutableStateFlow(1)
    val currentStage: StateFlow<Int> = _currentStage.asStateFlow()

    private val _isSavedToBackend = MutableStateFlow(false)
    val isSavedToBackend: StateFlow<Boolean> = _isSavedToBackend.asStateFlow()

    val testState: StateFlow<TestState> = testRepository.testState
    val errorEvent: SharedFlow<String> = testRepository.errorEvent
    val stageResults: StateFlow<Map<Int, StageResultData>> = testRepository.stageResults
    val currentTestType: StateFlow<TestType> = testRepository.currentTestType
    val fiveRepsTotalReps: StateFlow<Int> = testRepository.fiveRepsTotalReps
    val fiveRepsTimes: StateFlow<List<Long>> = testRepository.fiveRepsTimes
    val thirtySecondsTotalReps: StateFlow<Int> = testRepository.thirtySecondsTotalReps
    val thirtySecondsTimes: StateFlow<List<Long>> = testRepository.thirtySecondsTimes
    val timer: StateFlow<Int> = testRepository.timer
    val tugState: StateFlow<TugState> = testRepository.tugState

    private val _sessions = MutableStateFlow<List<TestSession>>(emptyList())
    val sessions: StateFlow<List<TestSession>> = _sessions.asStateFlow()

    val tugHistory: StateFlow<List<TugResult>> = _sessions.map { list ->
        list.mapNotNull { it.tugResult }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val fiveRepHistory: StateFlow<List<FiveRepResult>> = _sessions.map { list ->
        list.mapNotNull { it.fiveRepResult }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val thirtySecHistory: StateFlow<List<ThirtySecResult>> = _sessions.map { list ->
        list.mapNotNull { it.thirtySecResult }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val fourStageHistory: StateFlow<List<FourStageResult>> = _sessions.map { list ->
        list.mapNotNull { it.fourStageResult }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _currentSessionReport = MutableStateFlow<AllTestReport?>(null)
    val currentSessionReport: StateFlow<AllTestReport?> = _currentSessionReport.asStateFlow()

    private val _selectedReport = MutableStateFlow<FourStageResult?>(null)
    val selectedReport = _selectedReport.asStateFlow()

    private val _selectedTugReport = MutableStateFlow<TugResult?>(null)
    val selectedTugReport = _selectedTugReport.asStateFlow()

    private val _selectedFiveRepReport = MutableStateFlow<FiveRepResult?>(null)
    val selectedFiveRepReport = _selectedFiveRepReport.asStateFlow()

    private val _selectedThirtySecReport = MutableStateFlow<ThirtySecResult?>(null)
    val selectedThirtySecReport = _selectedThirtySecReport.asStateFlow()

    val patient: StateFlow<Patient> = testRepository.patient

    init {
        viewModelScope.launch {
            testRepository.testState.collect { state ->
                when (state) {
                    TestState.COMPLETED -> {
                        if(testRepository.currentTestType.value==TestType.FIVE_REPS){
                            _isFiveRepCompleted.value=true
                        }else if(testRepository.currentTestType.value==TestType.THIRTY_SECONDS){
                            _isThirtySecCompleted.value=true
                        }
                        _navigateToResultEvent.emit(Unit)
                    }
                    else -> {}
                }
            }
        }
    }

    fun resetForNewPatient(newPatient: Patient) {
        if (testRepository.patient.value.patientId == newPatient.patientId) return

        testRepository.setPatient(newPatient)
        testRepository.resetTestData(keepTestType = false)
        testRepository.resetCalibrationData()

        _currentSessionId.value = null
        _sessionProgress.value = emptyMap()
        _isFiveRepCompleted.value = false
        _isThirtySecCompleted.value = false
        _currentStage.value = 1
        _isSavedToBackend.value = false
        _sessions.value = emptyList()
        _currentSessionReport.value = null
    }

    fun startNewSession() {
        _currentSessionId.value = testRepository.generateNewSessionId()
        _sessionProgress.value = emptyMap()
    }

    fun finishSession(centerId: String, doctorId: String, patientId: String) {
        val sessionId = _currentSessionId.value ?: return
        val completedTests = _sessionProgress.value.filterValues { it }.keys
        val requiredTests = setOf(TestType.TUG, TestType.FIVE_REPS, TestType.THIRTY_SECONDS, TestType.FOUR_STAGE_BALANCE)

        val session = TestSession(
            sessionId = sessionId,
            centerId = centerId,
            doctorId = doctorId,
            patientId = patientId,
            timestamp = Date(),
            isCompleted = completedTests.containsAll(requiredTests)
        )

        viewModelScope.launch {
            try {
                testRepository.saveSession(session)
                _currentSessionId.value = null
                _sessionProgress.value = emptyMap()
            } catch (e: Exception) {
                Log.e("TestVM", "Failed to save session", e)
            }
        }
    }

    fun loadPatientHistory(centerId: String, patientId: String, doctorId: String) {
        viewModelScope.launch {
            try {
                _sessions.value = testRepository.getPatientSessions(patientId, centerId, doctorId)
            } catch (e: Exception) {
                Log.e("TestVM", "Error loading history", e)
            }
        }
    }

    private fun getActivePatientId(): String = patient.value.patientId

    fun saveTugResult(centerId: String, doctorId: String, data: TugResult) {
        _isSaving.value = true
        _isSavedToBackend.value = false

        val activeSession = _currentSessionId.value ?: testRepository.generateNewSessionId().also { _currentSessionId.value = it }

        val finalResult = data.copy(
            sessionId = activeSession,
            centerId = centerId,
            doctorId = doctorId,
            patientId = getActivePatientId(),
            timestamp = Date()
        )

        viewModelScope.launch {
            try {
                testRepository.addTugResult(centerId, finalResult)
                _sessionProgress.update { it + (TestType.TUG to true) }
                _isSavedToBackend.value = true
            } catch (e: Exception) {
                Log.e("TestVM", "Failed to save TUG", e)
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun saveFiveRepResult(centerId: String, doctorId: String, data: FiveRepResult) {
        _isSaving.value = true
        _isSavedToBackend.value = false

        val activeSession = _currentSessionId.value ?: testRepository.generateNewSessionId().also { _currentSessionId.value = it }
        val finalResult = data.copy(
            sessionId = activeSession,
            centerId = centerId,
            doctorId = doctorId,
            patientId = getActivePatientId(),
            timestamp = Date()
        )

        viewModelScope.launch {
            try {
                testRepository.addFiveRepResult(centerId, finalResult)
                _sessionProgress.update { it + (TestType.FIVE_REPS to true) }
                _isSavedToBackend.value = true
            } catch (e: Exception) {
                Log.e("TestVM", "Failed to save 5-Rep", e)
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun saveThirtySecResult(centerId: String, doctorId: String, data: ThirtySecResult) {
        if (currentUserId.isEmpty()) return

        _isSaving.value = true
        _isSavedToBackend.value = false

        val activeSession = _currentSessionId.value ?: testRepository.generateNewSessionId().also { _currentSessionId.value = it }
        val finalResult = data.copy(
            sessionId = activeSession,
            centerId = centerId,
            doctorId = doctorId,
            patientId = getActivePatientId(),
            timestamp = Date()
        )

        viewModelScope.launch {
            try {
                testRepository.addThirtySecResult(centerId, finalResult)
                _sessionProgress.update { it + (TestType.THIRTY_SECONDS to true) }
                _isSavedToBackend.value = true
            } catch (e: Exception) {
                Log.e("TestVM", "Failed to save 30-Sec", e)
            } finally {
                _isSaving.value = false
            }
        }
    }

    // --- STATIC BALANCE REPO EXPOSURE ---
    private val _staticBalanceResults = MutableStateFlow<List<StaticBalanceResult>>(emptyList())
    val staticBalanceResults = _staticBalanceResults.asStateFlow()

    fun fetchStaticBalanceResults(centerId: String, patientId: String) {
        viewModelScope.launch {
            try {
                Log.d("GraphFetch", "ViewModel asking Repository to fetch data...")
                val results = testRepository.getStaticBalanceResults(centerId, patientId)
                _staticBalanceResults.value = results
                Log.d("GraphFetch", "ViewModel successfully updated UI state with ${results.size} items.")
            } catch (e: Exception) {
                _staticBalanceResults.value = emptyList()
                Log.e("GraphFetch", "ViewModel FETCH FAILED", e)
            }
        }
    }


    fun saveStaticBalanceResult(result: StaticBalanceResult) {
        viewModelScope.launch {
            val centerId = patient.value.centerId
            testRepository.addStaticBalanceResult(centerId, result)
        }
    }

    fun saveFourStageResult(centerId: String, doctorId: String, data: FourStageResult) {
        _isSaving.value = true
        _isSavedToBackend.value = false

        val activeSession = _currentSessionId.value ?: testRepository.generateNewSessionId().also { _currentSessionId.value = it }
        val finalResult = data.copy(
            sessionId = activeSession,
            centerId = centerId,
            doctorId = doctorId,
            patientId = getActivePatientId(),
            timestamp = Date()
        )

        viewModelScope.launch {
            try {
                testRepository.addFourStageResult(centerId, finalResult)
                _sessionProgress.update { it + (TestType.FOUR_STAGE_BALANCE to true) }
                _isSavedToBackend.value = true
            } catch (e: Exception) {
                Log.e("TestVM", "Failed to save 4-Stage", e)
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun loadSessionReportData(centerId: String, sessionId: String, patient: Patient, doctorId: String) {
        viewModelScope.launch {
            try {
                val report = testRepository.getSessionReportData(centerId, sessionId, patient, doctorId)
                _currentSessionReport.value = report
            } catch (e: Exception) {
                Log.e("TestVM", "Error loading session report", e)
            }
        }
    }

    fun setPatient(patient: Patient) = testRepository.setPatient(patient)
    fun setTestType(type: TestType) = testRepository.setTestType(type)

    fun setStage(stageNumber: Int) {
        if (stageNumber in 1..4) {
            _currentStage.value = stageNumber
            testRepository.updateTimer(10)
        }
    }

    fun startTest() {
        if (testRepository.currentTestType.value == TestType.NONE) return
        StartTestUseCase(testRepository).invoke()
    }

    fun startTugTest() {
        setTestType(TestType.TUG)
        startTest()
    }

    fun onNewAngleReceived(leftPitch: Float, rightPitch: Float) =
        testRepository.onNewAngleReceived(leftPitch, rightPitch)

    fun analyzeTugData(
        accelLeftX: Float, accelLeftY: Float, accelLeftZ: Float,
        accelRightX: Float, accelRightY: Float, accelRightZ: Float,
        leftPitch: Float, rightPitch: Float, centerRoll: Float
    ) {
        testRepository.analyzeTugData(
            accelLeftX, accelLeftY, accelLeftZ,
            accelRightX, accelRightY, accelRightZ,
            leftPitch, rightPitch, centerRoll
        )
    }

    fun cancelTest() {
        resetTestData(keepTestType = true)
    }

    fun stopTest() = StopTestUseCase(testRepository).invoke()

    fun resetTestData(keepTestType: Boolean = false) {
        _isSavedToBackend.value = false
        testRepository.resetTestData(keepTestType)
    }

    fun selectSessionForReport(sessionId: String, patient: Patient) {
        viewModelScope.launch {
            val session = _sessions.value.find { it.sessionId == sessionId } ?: return@launch
            val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())

            val report = AllTestReport(
                sessionId = session.sessionId,
                centerId = session.centerId,
                doctorId = session.doctorId,
                patient = patient,
                date = dateFormat.format(session.timestamp),
                time = timeFormat.format(session.timestamp),
                doctorNotes = session.doctorNotes,
                fiveRepResult = session.fiveRepResult,
                thirtySecResult = session.thirtySecResult,
                tugResult = session.tugResult,
                fourStageResult = session.fourStageResult
            )

            _currentSessionReport.value = report
        }
    }

    fun setSelectedReport(report: FourStageResult) { _selectedReport.value = report }
    fun setSelectedTugReport(report: TugResult) { _selectedTugReport.value = report }
    fun setSelectedFiveRepReport(report: FiveRepResult) { _selectedFiveRepReport.value = report }
    fun setSelectedThirtySecReport(report: ThirtySecResult) { _selectedThirtySecReport.value = report }

    fun saveStageData(
        pointsLeft: List<Pair<Float, Float>>, accLeft: List<AccPoint>,
        pointsRight: List<Pair<Float, Float>>, accRight: List<AccPoint>,
        pointsCenter: List<Pair<Float, Float>>, accCenter: List<AccPoint>
    ) {
        testRepository.saveStageResult(
            stage = _currentStage.value,
            pointsLeft = pointsLeft, accLeft = accLeft,
            pointsRight = pointsRight, accRight = accRight,
            pointsCenter = pointsCenter, accCenter = accCenter
        )
    }

    fun saveStandingCalibration(calculatedCalibration: SensorData) { testRepository.setStandingCalibration(calculatedCalibration) }
    fun saveSittingCalibration(calculatedCalibration: SensorData) { testRepository.setSittingCalibration(calculatedCalibration) }
    fun resetCalibrationData() { testRepository.resetCalibrationData() }

    fun generateRepetitionStats(
        repDurations: List<Long>,
        accCenter: List<AccPoint>, accLeft: List<AccPoint>, accRight: List<AccPoint>
    ): List<RepetitionStats> {
        if (repDurations.isEmpty()) return emptyList()

        val startTime = accCenter.firstOrNull()?.timestamp ?: accLeft.firstOrNull()?.timestamp ?: return emptyList()
        val statsList = mutableListOf<RepetitionStats>()
        var windowStart = startTime

        repDurations.forEachIndexed { index, duration ->
            val windowEnd = windowStart + duration

            val cData = accCenter.filter { it.timestamp in windowStart..windowEnd }
            val lData = accLeft.filter { it.timestamp in windowStart..windowEnd }
            val rData = accRight.filter { it.timestamp in windowStart..windowEnd }

            val repStat = RepetitionStats(
                repIndex = index + 1,
                duration = duration,
                centerX = calculateAxisStats(cData.map { it.x }), centerY = calculateAxisStats(cData.map { it.y }), centerZ = calculateAxisStats(cData.map { it.z }),
                leftX = calculateAxisStats(lData.map { it.x }), leftY = calculateAxisStats(lData.map { it.y }), leftZ = calculateAxisStats(lData.map { it.z }),
                rightX = calculateAxisStats(rData.map { it.x }), rightY = calculateAxisStats(rData.map { it.y }), rightZ = calculateAxisStats(rData.map { it.z })
            )
            statsList.add(repStat)
            windowStart = windowEnd
        }
        return statsList
    }

    private fun calculateAxisStats(values: List<Float>): SensorAxisStats {
        if (values.isEmpty()) return SensorAxisStats()
        return SensorAxisStats(avg = values.average().toFloat(), min = values.minOrNull() ?: 0f, max = values.maxOrNull() ?: 0f)
    }

    fun saveDoctorNotes(centerId: String, sessionId: String, patientId: String, notes: String) {
        viewModelScope.launch {
            try {
                testRepository.updateSessionNotes(centerId, patientId, sessionId, notes)
                _currentSessionReport.update { current -> current?.copy(doctorNotes = notes) }
            } catch (e: Exception) {
                Log.e("TestVM", "Failed to save notes", e)
            }
        }
    }

    private val _patternDrawingResults = MutableStateFlow<List<PatternDrawingResult>>(emptyList())
    val patternDrawingResults = _patternDrawingResults.asStateFlow()

    fun fetchPatternDrawingResults(centerId: String, patientId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("PatternDebug", "ViewModel asking Repository to fetch Pattern Drawing data...")
                val results = testRepository.getPatternDrawingResults(centerId, patientId)
                _patternDrawingResults.value = results
                android.util.Log.d("PatternDebug", "ViewModel successfully updated UI state with ${results.size} items.")
            } catch (e: Exception) {
                _patternDrawingResults.value = emptyList()
                android.util.Log.e("PatternDebug", "ViewModel FETCH FAILED", e)
            }
        }
    }

    fun savePatternDrawingResult(result: PatternDrawingResult) {
        android.util.Log.d("PatternDebug", "ViewModel received save request! Sending to Repo...")
        viewModelScope.launch {
            testRepository.addPatternDrawingResult(patient.value.centerId, result)
        }
    }


    private val _shapeTrainingResults = MutableStateFlow<List<ShapeTrainingResult>>(emptyList())
    val shapeTrainingResults = _shapeTrainingResults.asStateFlow()

    fun fetchShapeTrainingResults(centerId: String, patientId: String) {
        viewModelScope.launch {
            try {
                _shapeTrainingResults.value = testRepository.getShapeTrainingResults(centerId, patientId)
            } catch (e: Exception) {
                _shapeTrainingResults.value = emptyList()
            }
        }
    }

    fun saveShapeTrainingResult(result: ShapeTrainingResult) {
        viewModelScope.launch {
            testRepository.addShapeTrainingResult(patient.value.centerId, result)
        }
    }

    private val _colorSorterResults = MutableStateFlow<List<ColorSorterResult>>(emptyList())
    val colorSorterResults = _colorSorterResults.asStateFlow()

    fun fetchColorSorterResults(centerId: String, patientId: String) {
        viewModelScope.launch {
            try {
                _colorSorterResults.value = testRepository.getColorSorterResults(centerId, patientId)
            } catch (e: Exception) {
                _colorSorterResults.value = emptyList()
            }
        }
    }

    fun saveColorSorterResult(result: ColorSorterResult) {
        viewModelScope.launch {
            testRepository.addColorSorterResult(patient.value.centerId, result)
        }
    }

    private val _ratPuzzleResults = MutableStateFlow<List<RatPuzzleResult>>(emptyList())
    val ratPuzzleResults = _ratPuzzleResults.asStateFlow()

    fun fetchRatPuzzleResults(centerId: String, patientId: String) {
        viewModelScope.launch {
            try { _ratPuzzleResults.value = testRepository.getRatPuzzleResults(centerId, patientId) }
            catch (e: Exception) { _ratPuzzleResults.value = emptyList() }
        }
    }

    fun saveRatPuzzleResult(result: RatPuzzleResult) {
        viewModelScope.launch { testRepository.addRatPuzzleResult(patient.value.centerId, result) }
    }


    private val _starshipResults = MutableStateFlow<List<StarshipResult>>(emptyList())
    val starshipResults = _starshipResults.asStateFlow()

    fun fetchStarshipResults(centerId: String, patientId: String) {
        viewModelScope.launch {
            try { _starshipResults.value = testRepository.getStarshipResults(centerId, patientId) }
            catch (e: Exception) { _starshipResults.value = emptyList() }
        }
    }

    fun saveStarshipResult(result: StarshipResult) {
        viewModelScope.launch { testRepository.addStarshipResult(patient.value.centerId, result) }
    }

    private val _holePuzzleResults = MutableStateFlow<List<HolePuzzleResult>>(emptyList())
    val holePuzzleResults = _holePuzzleResults.asStateFlow()

    fun fetchHolePuzzleResults(centerId: String, patientId: String) {
        viewModelScope.launch {
            try { _holePuzzleResults.value = testRepository.getHolePuzzleResults(centerId, patientId) }
            catch (e: Exception) { _holePuzzleResults.value = emptyList() }
        }
    }

    fun saveHolePuzzleResult(result: HolePuzzleResult) {
        viewModelScope.launch { testRepository.addHolePuzzleResult(patient.value.centerId, result) }
    }

    private val _stepGameResults = MutableStateFlow<List<StepGameResult>>(emptyList())
    val stepGameResults = _stepGameResults.asStateFlow()

    fun fetchStepGameResults(centerId: String, patientId: String) {
        viewModelScope.launch {
            try { _stepGameResults.value = testRepository.getStepGameResults(centerId, patientId) }
            catch (e: Exception) { _stepGameResults.value = emptyList() }
        }
    }

    fun saveStepGameResult(result: StepGameResult) {
        viewModelScope.launch { testRepository.addStepGameResult(patient.value.centerId, result) }
    }
}