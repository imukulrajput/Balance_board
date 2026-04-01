package com.ripplehealthcare.frst.data.repository

import FourStageResult
import android.util.Log
import com.ripplehealthcare.frst.data.model.SensorData
import com.ripplehealthcare.frst.domain.model.AccPoint
import com.ripplehealthcare.frst.domain.model.FiveRepResult
import com.ripplehealthcare.frst.domain.model.Patient
import com.ripplehealthcare.frst.domain.model.StageResultData
import com.ripplehealthcare.frst.domain.model.StaticBalanceResult
import com.ripplehealthcare.frst.domain.model.SwayPoint
import com.ripplehealthcare.frst.domain.model.TestResult
import com.ripplehealthcare.frst.domain.model.TestSession
import com.ripplehealthcare.frst.domain.model.TestType
import com.ripplehealthcare.frst.domain.repository.TestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import com.ripplehealthcare.frst.domain.model.TestState
import com.ripplehealthcare.frst.domain.model.ThirtySecResult
import com.ripplehealthcare.frst.domain.model.TugResult
import com.ripplehealthcare.frst.domain.model.TugState
import com.ripplehealthcare.frst.utils.AllTestReport
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ripplehealthcare.frst.domain.model.Gender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import com.google.firebase.firestore.SetOptions
import com.ripplehealthcare.frst.domain.model.ColorSorterResult
import com.ripplehealthcare.frst.domain.model.HolePuzzleResult
import com.ripplehealthcare.frst.domain.model.PatternDrawingResult
import com.ripplehealthcare.frst.domain.model.RatPuzzleResult
import com.ripplehealthcare.frst.domain.model.ShapeTrainingResult
import com.ripplehealthcare.frst.domain.model.StarshipResult
import com.ripplehealthcare.frst.domain.model.StepGameResult

class TestRepositoryImpl : TestRepository {
    private enum class TugInternalStage {
        AWAITING_STAND_UP, // Waiting for user to stand
        AWAITING_WALK,     // Standing, waiting to start walking
        WALKING,           // User is walking
        AWAITING_SIT_DOWN, // User stopped walking, waiting to sit
        DONE               // Test Finished
    }

    private val db = FirebaseFirestore.getInstance()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _testState = MutableStateFlow(TestState.IDLE)
    override val testState: StateFlow<TestState> = _testState

    private val _tugState = MutableStateFlow(TugState.SITTING)
    override val tugState: StateFlow<TugState> = _tugState

    private val _errorEvent = MutableSharedFlow<String>()
    override val errorEvent = _errorEvent.asSharedFlow()

    private var hasStartedSitting = false

    private val _standingCalibration = MutableStateFlow<SensorData?>(null)
    override val standingCalibration: StateFlow<SensorData?> = _standingCalibration.asStateFlow()

    private val _sittingCalibration = MutableStateFlow<SensorData?>(null)
    override val sittingCalibration: StateFlow<SensorData?> = _sittingCalibration.asStateFlow()

    // Default Fallbacks
    private val DEFAULT_STANDING_PITCH = 0f
    private val DEFAULT_SITTING_PITCH = -90f
    private val POSTURE_THRESHOLD = 25f

    private val ACCEL_BUFFER_SIZE = 15
    private val accelHistory = ArrayDeque<Float>(ACCEL_BUFFER_SIZE)

    // Motion Confirmation Counters
    private var consecutiveWalkSamples = 0
    private var consecutiveStillSamples = 0
    private val SAMPLES_TO_CONFIRM_WALK = 6
    private val SAMPLES_TO_CONFIRM_STILL = 25

    private var isConfirmedWalking = false
    private var isConfirmedStill = true

    // Posture Buffering
    private val PITCH_BUFFER_SIZE = 10
    private val pitchBuffer = ArrayDeque<Float>(PITCH_BUFFER_SIZE)

    // Thresholds
    private val WALKING_SD_THRESHOLD = 0.6f

    private var tugInternalStage = TugInternalStage.AWAITING_STAND_UP
    private var tugWalkStartTime: Long = 0

    private val _currentTestType = MutableStateFlow(TestType.NONE)
    override val currentTestType: StateFlow<TestType> = _currentTestType

    private val _fiveRepsTotalReps = MutableStateFlow(0)
    override val fiveRepsTotalReps: StateFlow<Int> = _fiveRepsTotalReps

    private val _fiveRepsTimes = MutableStateFlow<List<Long>>(emptyList())
    override val fiveRepsTimes: StateFlow<List<Long>> = _fiveRepsTimes

    private val _thirtySecondsTotalReps = MutableStateFlow(0)
    override val thirtySecondsTotalReps: StateFlow<Int> = _thirtySecondsTotalReps

    private val _thirtySecondsTimes = MutableStateFlow<List<Long>>(emptyList())
    override val thirtySecondsTimes: StateFlow<List<Long>> = _thirtySecondsTimes

    private val _stageResults = MutableStateFlow<Map<Int, StageResultData>>(emptyMap())
    override val stageResults: StateFlow<Map<Int, StageResultData>> = _stageResults

    private val _timer = MutableStateFlow(0)
    override val timer: StateFlow<Int> = _timer

    private var timerJob: Job? = null

    private var lastCenterRoll: Float? = null
    private var lastRollTimestamp: Long = 0
    private var outboundHeading: Float? = null
    private var turnStartTime: Long = 0
    private var isTurning = false
    private var hasRecordedTurn = false
    private var calculatedTurnSpeed: Float = 0f

    private val _patient = MutableStateFlow(
        Patient(
            patientId = "",
            centerId = "",
            doctorId = "",
            name = "",
            gender = Gender.MALE.displayName
        )
    )
    override val patient: StateFlow<Patient> = _patient

    private var lastState = "UNKNOWN"
    private var repStartTime = 0L

    override fun setPatient(patient: Patient) {
        _patient.value = patient
    }

    override fun setTestType(type: TestType) {
        _currentTestType.value = type
    }

    override fun setStandingCalibration(data: SensorData) {
        _standingCalibration.value = data
        Log.d("Repo", "Standing Calibration Saved")
    }

    override fun setSittingCalibration(data: SensorData) {
        _sittingCalibration.value = data
        Log.d("Repo", "Sitting Calibration Saved")
    }

    override fun getStandingCalibration() = standingCalibration.value
    override fun getSittingCalibration() = sittingCalibration.value

    override fun startTest() {
        if (_currentTestType.value == TestType.NONE) return
        resetTestData(keepTestType = true)
        _testState.value = TestState.RUNNING
        _timer.value = if (_currentTestType.value == TestType.THIRTY_SECONDS) 30 else 0

        startInternalTimer()
    }

    override fun stopTest() {
        if (_testState.value == TestState.RUNNING) {
            _testState.value = TestState.COMPLETED
            stopInternalTimer()
        }
    }

    private fun startInternalTimer() {
        stopInternalTimer()

        timerJob = scope.launch {
            Log.d("TestRepo", "Timer Started")
            while (isActive && _testState.value == TestState.RUNNING) {
                delay(1000)

                if (!isActive || _testState.value != TestState.RUNNING) break

                when (_currentTestType.value) {
                    TestType.THIRTY_SECONDS -> {
                        if (_timer.value > 0) {
                            _timer.value -= 1
                        } else {
                            stopTest()
                        }
                    }

                    TestType.FIVE_REPS, TestType.TUG -> {
                        _timer.value += 1
                    }

                    else -> {}
                }
            }
        }
    }

    private fun stopInternalTimer() {
        timerJob?.cancel()
        timerJob = null
        Log.d("TestRepo", "Timer Stopped")
    }

    override fun updateTimer(time: Int) {
        _timer.value = time
    }

    override fun resetCalibrationData() {
        _standingCalibration.value = null
        _sittingCalibration.value = null
        Log.d("TestRepo", "Calibration data reset for new patient")
    }

    override fun resetTestData(keepTestType: Boolean) {
        stopInternalTimer()
        lastState = "UNKNOWN"
        repStartTime = 0L
        _testState.value = TestState.IDLE

        hasStartedSitting = false
        when (currentTestType.value) {
            TestType.FIVE_REPS -> {
                _fiveRepsTotalReps.value = 0
                _fiveRepsTimes.value = emptyList()
                _timer.value = 0
            }

            TestType.THIRTY_SECONDS -> {
                _thirtySecondsTotalReps.value = 0
                _thirtySecondsTimes.value = emptyList()
                _timer.value = 30
            }

            TestType.TUG -> {
                _tugState.value = TugState.SITTING
                accelHistory.clear()
                pitchBuffer.clear()
                tugInternalStage = TugInternalStage.AWAITING_STAND_UP
                isConfirmedWalking = false
                isConfirmedStill = true
                _timer.value = 0
                lastCenterRoll = null
                lastRollTimestamp = 0
                outboundHeading = null
                turnStartTime = 0
                hasRecordedTurn = false
                isTurning = false
                calculatedTurnSpeed = 0f
            }

            TestType.FOUR_STAGE_BALANCE -> {
                _stageResults.value = emptyMap()
                _timer.value = 10
            }

            else -> {
                _fiveRepsTotalReps.value = 0
                _fiveRepsTimes.value = emptyList()
                _thirtySecondsTotalReps.value = 0
                _thirtySecondsTimes.value = emptyList()
                _stageResults.value = emptyMap()
            }
        }
        if (!keepTestType) _currentTestType.value = TestType.NONE
    }

    override fun onNewAngleReceived(leftPitch: Float, rightPitch: Float) {
        if (_testState.value != TestState.RUNNING) return

        val currentTime = System.currentTimeMillis()

        val targetStandL = standingCalibration.value?.leftPitch ?: DEFAULT_STANDING_PITCH
        val targetStandR = standingCalibration.value?.rightPitch ?: DEFAULT_STANDING_PITCH
        val targetSitL = sittingCalibration.value?.leftPitch ?: DEFAULT_SITTING_PITCH
        val targetSitR = sittingCalibration.value?.rightPitch ?: DEFAULT_SITTING_PITCH

        val isLeftStanding = abs(leftPitch - targetStandL) < POSTURE_THRESHOLD
        val isRightStanding = abs(rightPitch - targetStandR) < POSTURE_THRESHOLD
        val isStanding = isLeftStanding && isRightStanding

        val isLeftSitting = abs(leftPitch - targetSitL) < POSTURE_THRESHOLD
        val isRightSitting = abs(rightPitch - targetSitR) < POSTURE_THRESHOLD
        val isSitting = isLeftSitting && isRightSitting

        when {
            isSitting && (lastState == "UNKNOWN") -> {
                lastState = "SITTING"
                repStartTime = currentTime
            }

            isStanding && lastState == "SITTING" -> {
                lastState = "STANDING"
            }

            isSitting && lastState == "STANDING" -> {
                lastState = "SITTING"
                val now = System.currentTimeMillis()
                val timeTaken = now - repStartTime

                when (_currentTestType.value) {
                    TestType.FIVE_REPS -> {
                        _fiveRepsTimes.value += timeTaken
                        _fiveRepsTotalReps.value += 1
                    }

                    TestType.THIRTY_SECONDS -> {
                        _thirtySecondsTimes.value += timeTaken
                        _thirtySecondsTotalReps.value += 1
                    }

                    else -> {}
                }

                repStartTime = now
            }
        }
    }

    private fun getAngleDiff(target: Float, current: Float): Float {
        var diff = target - current
        while (diff > 180) diff -= 360
        while (diff < -180) diff += 360
        return diff
    }

    override fun analyzeTugData(
        accelLeftX: Float, accelLeftY: Float, accelLeftZ: Float,
        accelRightX: Float, accelRightY: Float, accelRightZ: Float,
        leftPitch: Float, rightPitch: Float,
        centerRoll: Float
    ) {
        if (_testState.value != TestState.RUNNING) return
        if (_currentTestType.value != TestType.TUG) return
        if (tugInternalStage == TugInternalStage.DONE) return

        val currentTime = System.currentTimeMillis()

        val leftMag = sqrt(accelLeftX * accelLeftX + accelLeftY * accelLeftY + accelLeftZ * accelLeftZ)
        val rightMag = sqrt(accelRightX * accelRightX + accelRightY * accelRightY + accelRightZ * accelRightZ)
        val avgMag = (leftMag + rightMag) / 2f

        if (accelHistory.size >= ACCEL_BUFFER_SIZE) accelHistory.removeFirst()
        accelHistory.add(avgMag)
        if (accelHistory.size < 5) return

        val mean = accelHistory.average()
        val variance = accelHistory.map { (it - mean).pow(2) }.average()
        val standardDeviation = sqrt(variance)

        val isRawMotion = standardDeviation > WALKING_SD_THRESHOLD

        if (isRawMotion) {
            consecutiveWalkSamples++
            consecutiveStillSamples = 0
        } else {
            consecutiveStillSamples++
            consecutiveWalkSamples = 0
        }

        if (consecutiveWalkSamples >= SAMPLES_TO_CONFIRM_WALK) {
            isConfirmedWalking = true
            isConfirmedStill = false
            consecutiveWalkSamples = SAMPLES_TO_CONFIRM_WALK
        }

        if (consecutiveStillSamples >= SAMPLES_TO_CONFIRM_STILL) {
            isConfirmedStill = true
            isConfirmedWalking = false
            consecutiveStillSamples = SAMPLES_TO_CONFIRM_STILL
        }

        val targetStandL = standingCalibration.value?.leftPitch ?: DEFAULT_STANDING_PITCH
        val targetStandR = standingCalibration.value?.rightPitch ?: DEFAULT_STANDING_PITCH
        val targetSitL = sittingCalibration.value?.leftPitch ?: DEFAULT_SITTING_PITCH
        val targetSitR = sittingCalibration.value?.rightPitch ?: DEFAULT_SITTING_PITCH

        val isLeftStanding = abs(leftPitch - targetStandL) < POSTURE_THRESHOLD
        val isLeftSitting = abs(leftPitch - targetSitL) < POSTURE_THRESHOLD

        val isRightStanding = abs(rightPitch - targetStandR) < POSTURE_THRESHOLD
        val isRightSitting = abs(rightPitch - targetSitR) < POSTURE_THRESHOLD

        val isUpright = isLeftStanding && isRightStanding
        val isSitting = isLeftSitting && isRightSitting

        if (!hasStartedSitting) {
            if (isSitting) {
                hasStartedSitting = true
            } else if (isUpright || isConfirmedWalking) {
                scope.launch {
                    _errorEvent.emit("Test Cancelled: Please sit down before starting!")
                }
                resetTestData(keepTestType = true)
                return
            } else {
                return
            }
        }
        var currentRotationalSpeed = 0f
        if (lastCenterRoll != null && lastRollTimestamp != 0L) {
            val dt = (currentTime - lastRollTimestamp) / 1000f
            if (dt > 0) {
                val delta = getAngleDiff(centerRoll, lastCenterRoll!!)
                currentRotationalSpeed = abs(delta / dt)
            }
        }
        lastCenterRoll = centerRoll
        lastRollTimestamp = currentTime

        val uiState =
            if (isSitting) TugState.SITTING else if (isConfirmedWalking) TugState.WALKING else TugState.STANDING
        if (_tugState.value != uiState) _tugState.value = uiState

        when (tugInternalStage) {
            TugInternalStage.AWAITING_STAND_UP -> {
                if (isUpright && isConfirmedStill) {
                    tugInternalStage = TugInternalStage.AWAITING_WALK
                    Log.d("TUG", "Stage: AWAITING_WALK")
                } else if (isUpright && isConfirmedWalking) {
                    tugInternalStage = TugInternalStage.WALKING
                    tugWalkStartTime = System.currentTimeMillis()
                    outboundHeading = centerRoll
                    Log.d("TUG", "Stage: WALKING (Fast start)")
                }
            }

            TugInternalStage.AWAITING_WALK -> {
                if (isConfirmedWalking) {
                    tugInternalStage = TugInternalStage.WALKING
                    tugWalkStartTime = System.currentTimeMillis()
                    outboundHeading = centerRoll
                    Log.d("TUG", "Stage: WALKING")
                } else if (isSitting && isConfirmedStill) {
                    tugInternalStage = TugInternalStage.AWAITING_STAND_UP
                    Log.d("TUG", "Stage: Reset to AWAITING_STAND_UP")
                }
            }

            TugInternalStage.WALKING -> {
                val duration = System.currentTimeMillis() - tugWalkStartTime

                if (duration > 3000) {
                    if (!hasRecordedTurn) {
                        outboundHeading?.let { startAngle ->
                            val deviation = abs(getAngleDiff(centerRoll, startAngle))

                            if (!isTurning) {
                                if (deviation > 30) {
                                    isTurning = true
                                    turnStartTime = System.currentTimeMillis()
                                    Log.d("TUG", "Turn STARTED")
                                }
                            } else {
                                if (deviation > 140 && currentRotationalSpeed < 20) {
                                    isTurning = false
                                    val turnEndTime = System.currentTimeMillis()

                                    val durationSec = (turnEndTime - turnStartTime) / 1000f
                                    if (durationSec > 0.1) {
                                        calculatedTurnSpeed = deviation / durationSec
                                        hasRecordedTurn = true
                                        Log.d("TUG", "Turn SPEED: $calculatedTurnSpeed deg/s")
                                    }
                                    outboundHeading = centerRoll
                                }
                            }
                        }
                    }

                    if (isConfirmedStill && isUpright) {
                        tugInternalStage = TugInternalStage.AWAITING_SIT_DOWN
                        Log.d("TUG", "Stage: AWAITING_SIT_DOWN")
                    } else if (isConfirmedStill && isSitting) {
                        finishTugTest()
                    }
                }
            }

            TugInternalStage.AWAITING_SIT_DOWN -> {
                if (isConfirmedStill && isSitting) {
                    finishTugTest()
                } else if (isConfirmedWalking) {
                    tugInternalStage = TugInternalStage.WALKING
                    Log.d("TUG", "Stage: Resumed WALKING")
                }
            }

            else -> {}
        }
    }

    private fun finishTugTest() {
        tugInternalStage = TugInternalStage.DONE
        Log.d("TUG", "Test Finished Automatically")
        stopTest()
    }

    override fun getTestResult(): TestResult {
        return when (_currentTestType.value) {
            TestType.FIVE_REPS -> TestResult(
                "",
                TestType.FIVE_REPS,
                _fiveRepsTotalReps.value,
                _fiveRepsTimes.value,
                _timer.value
            )

            TestType.THIRTY_SECONDS -> TestResult(
                "",
                TestType.THIRTY_SECONDS,
                _thirtySecondsTotalReps.value,
                _thirtySecondsTimes.value,
                _timer.value
            )

            else -> TestResult("", TestType.NONE, 0, emptyList(), 0)
        }
    }

    override fun saveStageResult(
        stage: Int,
        pointsLeft: List<Pair<Float, Float>>,
        accLeft: List<AccPoint>,
        pointsRight: List<Pair<Float, Float>>,
        accRight: List<AccPoint>,
        pointsCenter: List<Pair<Float, Float>>,
        accCenter: List<AccPoint>
    ) {
        val newResult = StageResultData(
            stageNumber = stage,
            pointsLeft = pointsLeft.map { SwayPoint(it.first, it.second) },
            accelerationLeft = accLeft,
            pointsRight = pointsRight.map { SwayPoint(it.first, it.second) },
            accelerationRight = accRight,
            pointsCenter = pointsCenter.map { SwayPoint(it.first, it.second) },
            accelerationCenter = accCenter
        )

        _stageResults.update { currentMap ->
            currentMap + (stage to newResult)
        }
    }

    override fun generateNewSessionId(): String = UUID.randomUUID().toString()

    override suspend fun saveSession(session: TestSession) {
        try {
            db.collection("centers").document(session.centerId)
                .collection("patients").document(session.patientId)
                .collection("sessions").document(session.sessionId)
                .set(session).await()
        } catch (e: Exception) {
            Log.e("TestRepo", "Error saving session", e)
            throw e
        }
    }

    override suspend fun getPatientSessions(
        patientId: String,
        centerId: String,
        doctorId: String
    ): List<TestSession> {
        if (centerId.isEmpty() || patientId.isEmpty()) return emptyList()

        return try {
            val sessionsSnapshot = db.collection("centers").document(centerId)
                .collection("patients").document(patientId)
                .collection("sessions")
                .whereEqualTo("doctorId", doctorId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().await()

            val fullSessions = sessionsSnapshot.documents.map { sessionDoc ->
                val session = sessionDoc.toObject(TestSession::class.java)!!

                val resultsSnapshot = sessionDoc.reference.collection("results").get().await()

                var tug: TugResult? = null
                var fiveRep: FiveRepResult? = null
                var thirtySec: ThirtySecResult? = null
                var fourStage: FourStageResult? = null

                for (result in resultsSnapshot.documents) {
                    when (result.id) {
                        "tug" -> tug = result.toObject(TugResult::class.java)
                        "fiveReps" -> fiveRep = result.toObject(FiveRepResult::class.java)
                        "thirtySec" -> thirtySec = result.toObject(ThirtySecResult::class.java)
                        "fourStages" -> fourStage = result.toObject(FourStageResult::class.java)
                    }
                }

                session.copy(
                    tugResult = tug,
                    fiveRepResult = fiveRep,
                    thirtySecResult = thirtySec,
                    fourStageResult = fourStage
                )
            }
            fullSessions
        } catch (e: Exception) {
            Log.e("TestRepo", "Error fetching combined sessions", e)
            emptyList()
        }
    }

    override suspend fun addTugResult(centerId: String, test: TugResult) {
        try {
            val ref = db.collection("centers").document(centerId)
                .collection("patients").document(test.patientId)
                .collection("sessions").document(test.sessionId)
                .collection("results").document("tug")

            val testWithIds = test.copy(testId = "tug", turnSpeed = calculatedTurnSpeed)
            ref.set(testWithIds).await()
        } catch (e: Exception) {
            Log.e("TestRepo", "Error saving TUG result", e)
        }
    }

    override suspend fun addFiveRepResult(centerId: String, test: FiveRepResult) {
        val ref = db.collection("centers").document(centerId)
            .collection("patients").document(test.patientId)
            .collection("sessions").document(test.sessionId)
            .collection("results").document("fiveReps")
        val testWithIds = test.copy(
            testId = ref.id,
        )
        ref.set(testWithIds).await()
        Log.d("TestRepository", "Test added for patient ${ref.id}")
    }

    override suspend fun addThirtySecResult(centerId: String, test: ThirtySecResult) {
        val ref = db.collection("centers").document(centerId)
            .collection("patients").document(test.patientId)
            .collection("sessions").document(test.sessionId)
            .collection("results").document("thirtySec")
        val testWithIds = test.copy(
            testId = ref.id,
        )
        ref.set(testWithIds).await()
        Log.d("TestRepository", "Test added for patient ${ref.id}")
    }

    override suspend fun addFourStageResult(centerId: String, test: FourStageResult) {
        val ref = db.collection("centers").document(centerId)
            .collection("patients").document(test.patientId)
            .collection("sessions").document(test.sessionId)
            .collection("results").document("fourStages")
        val testWithIds = test.copy(
            testId = ref.id,
        )
        ref.set(testWithIds).await()
        Log.d("TestRepository", "Test added for patient ${ref.id}")
    }

    override suspend fun getTugResults(userId: String, patientId: String): List<TugResult> =
        fetchTests("tug", userId, patientId, TugResult::class.java)

    override suspend fun getFiveRepResults(userId: String, patientId: String): List<FiveRepResult> =
        fetchTests("fiveReps", userId, patientId, FiveRepResult::class.java)

    override suspend fun getThirtySecResults(
        userId: String,
        patientId: String
    ): List<ThirtySecResult> =
        fetchTests("thirtySec", userId, patientId, ThirtySecResult::class.java)

    override suspend fun getFourStageResults(
        userId: String,
        patientId: String
    ): List<FourStageResult> =
        fetchTests("fourStages", userId, patientId, FourStageResult::class.java)

    private suspend fun <T> fetchTests(
        collection: String,
        userId: String,
        patientId: String,
        clazz: Class<T>
    ): List<T> {
        return try {
            db.collection(collection)
                .whereEqualTo("patientId", patientId)
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().await().toObjects(clazz)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getSessionReportData(
        centerId: String,
        sessionId: String,
        patient: Patient,
        doctorId: String
    ): AllTestReport {
        val sessionRef = db.collection("centers").document(centerId)
            .collection("patients").document(patient.patientId)
            .collection("sessions").document(sessionId)

        val sessionDoc = sessionRef.get().await().toObject(TestSession::class.java)
        val resultsRef = sessionRef.collection("results")

        val tug = resultsRef.document("tug").get().await().toObject(TugResult::class.java)
        val fiveRep =
            resultsRef.document("fiveReps").get().await().toObject(FiveRepResult::class.java)
        val thirtySec =
            resultsRef.document("thirtySec").get().await().toObject(ThirtySecResult::class.java)
        val fourStage =
            resultsRef.document("fourStages").get().await().toObject(FourStageResult::class.java)
        val timestamp = sessionDoc?.timestamp ?: Date()
        val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        return AllTestReport(
            sessionId = sessionId,
            centerId = centerId,
            doctorId = doctorId,
            patient = patient,
            date = dateFormat.format(timestamp),
            time = timeFormat.format(timestamp),
            doctorNotes = sessionDoc?.doctorNotes ?: "",
            fiveRepResult = fiveRep,
            thirtySecResult = thirtySec,
            tugResult = tug,
            fourStageResult = fourStage
        )
    }

    override suspend fun updateSessionNotes(
        centerId: String,
        patientId: String,
        sessionId: String,
        notes: String
    ) {
        db.collection("centers").document(centerId)
            .collection("patients").document(patientId)
            .collection("sessions").document(sessionId)
            .update("doctorNotes", notes).await()
    }


    // =========================================================================
    // --- CONCURRENT BACKGROUND FETCHING FOR GAMES & TRAINING MODULES ---
    // =========================================================================

    override suspend fun addStaticBalanceResult(centerId: String, result: StaticBalanceResult) {
        try {
            val sessionRef = db.collection("centers").document(centerId)
                .collection("patients").document(result.patientId)
                .collection("sessions").document(result.sessionId)

            val dummySessionData = mapOf("sessionId" to result.sessionId, "timestamp" to result.timestamp)
            sessionRef.set(dummySessionData, SetOptions.merge()).await()

            val ref = sessionRef.collection("results").document("staticBalance")
            val testWithId = result.copy(testId = ref.id)
            ref.set(testWithId).await()
        } catch (e: Exception) {
            Log.e("GraphFetch", "SAVE FAILED: Error saving Static Balance result", e)
        }
    }

    override suspend fun getStaticBalanceResults(centerId: String, patientId: String): List<StaticBalanceResult> {
        return try {
            val sessionsSnapshot = db.collection("centers").document(centerId)
                .collection("patients").document(patientId)
                .collection("sessions").get().await()

            val resultsList = coroutineScope {
                sessionsSnapshot.documents.map { sessionDoc ->
                    async {
                        val resultDoc = sessionDoc.reference.collection("results").document("staticBalance").get().await()
                        if (resultDoc.exists()) {
                            val result = resultDoc.toObject(StaticBalanceResult::class.java)
                            if (result != null && result.totalTimeMs > 0) result else null
                        } else null
                    }
                }.awaitAll().filterNotNull()
            }

            resultsList.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun addPatternDrawingResult(centerId: String, result: PatternDrawingResult) {
        try {
            val sessionRef = db.collection("centers").document(centerId)
                .collection("patients").document(result.patientId)
                .collection("sessions").document(result.sessionId)

            val dummySessionData = mapOf("sessionId" to result.sessionId, "timestamp" to result.timestamp)
            sessionRef.set(dummySessionData, SetOptions.merge()).await()

            val ref = sessionRef.collection("results").document("patternDrawing")
            val testWithId = result.copy(testId = ref.id)
            ref.set(testWithId).await()
        } catch (e: Exception) {
            Log.e("PatternDebug", "SAVE FAILED: Error saving Pattern Drawing", e)
        }
    }

    override suspend fun getPatternDrawingResults(centerId: String, patientId: String): List<PatternDrawingResult> {
        return try {
            val sessionsSnapshot = db.collection("centers").document(centerId)
                .collection("patients").document(patientId)
                .collection("sessions").get().await()

            val resultsList = coroutineScope {
                sessionsSnapshot.documents.map { sessionDoc ->
                    async {
                        val resultDoc = sessionDoc.reference.collection("results").document("patternDrawing").get().await()
                        if (resultDoc.exists()) resultDoc.toObject(PatternDrawingResult::class.java) else null
                    }
                }.awaitAll().filterNotNull()
            }
            resultsList.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun addShapeTrainingResult(centerId: String, result: ShapeTrainingResult) {
        try {
            val sessionRef = db.collection("centers").document(centerId)
                .collection("patients").document(result.patientId)
                .collection("sessions").document(result.sessionId)

            val dummySessionData = mapOf("sessionId" to result.sessionId, "timestamp" to result.timestamp)
            sessionRef.set(dummySessionData, SetOptions.merge()).await()

            val ref = sessionRef.collection("results").document("shapeTraining")
            val testWithId = result.copy(testId = ref.id)
            ref.set(testWithId).await()
        } catch (e: Exception) {
            Log.e("ShapeDebug", "Error saving Shape Training", e)
        }
    }

    override suspend fun getShapeTrainingResults(centerId: String, patientId: String): List<ShapeTrainingResult> {
        return try {
            val sessionsSnapshot = db.collection("centers").document(centerId)
                .collection("patients").document(patientId)
                .collection("sessions").get().await()

            val resultsList = coroutineScope {
                sessionsSnapshot.documents.map { sessionDoc ->
                    async {
                        val resultDoc = sessionDoc.reference.collection("results").document("shapeTraining").get().await()
                        if (resultDoc.exists()) resultDoc.toObject(ShapeTrainingResult::class.java) else null
                    }
                }.awaitAll().filterNotNull()
            }
            resultsList.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun addColorSorterResult(centerId: String, result: ColorSorterResult) {
        try {
            val sessionRef = db.collection("centers").document(centerId)
                .collection("patients").document(result.patientId)
                .collection("sessions").document(result.sessionId)

            val dummySessionData = mapOf("sessionId" to result.sessionId, "timestamp" to result.timestamp)
            sessionRef.set(dummySessionData, SetOptions.merge()).await()

            val ref = sessionRef.collection("results").document("colorSorter")
            val testWithId = result.copy(testId = ref.id)
            ref.set(testWithId).await()
        } catch (e: Exception) {
            Log.e("ColorSorter", "Error saving data", e)
        }
    }

    override suspend fun getColorSorterResults(centerId: String, patientId: String): List<ColorSorterResult> {
        return try {
            val sessionsSnapshot = db.collection("centers").document(centerId)
                .collection("patients").document(patientId)
                .collection("sessions").get().await()

            val resultsList = coroutineScope {
                sessionsSnapshot.documents.map { sessionDoc ->
                    async {
                        val resultDoc = sessionDoc.reference.collection("results").document("colorSorter").get().await()
                        if (resultDoc.exists()) resultDoc.toObject(ColorSorterResult::class.java) else null
                    }
                }.awaitAll().filterNotNull()
            }
            resultsList.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun addRatPuzzleResult(centerId: String, result: RatPuzzleResult) {
        try {
            val sessionRef = db.collection("centers").document(centerId)
                .collection("patients").document(result.patientId)
                .collection("sessions").document(result.sessionId)

            val dummySessionData = mapOf("sessionId" to result.sessionId, "timestamp" to result.timestamp)
            sessionRef.set(dummySessionData, SetOptions.merge()).await()

            val ref = sessionRef.collection("results").document("ratPuzzle")
            val testWithId = result.copy(testId = ref.id)
            ref.set(testWithId).await()
        } catch (e: Exception) {}
    }

    override suspend fun getRatPuzzleResults(centerId: String, patientId: String): List<RatPuzzleResult> {
        return try {
            val sessionsSnapshot = db.collection("centers").document(centerId)
                .collection("patients").document(patientId)
                .collection("sessions").get().await()

            val resultsList = coroutineScope {
                sessionsSnapshot.documents.map { sessionDoc ->
                    async {
                        val resultDoc = sessionDoc.reference.collection("results").document("ratPuzzle").get().await()
                        if (resultDoc.exists()) resultDoc.toObject(RatPuzzleResult::class.java) else null
                    }
                }.awaitAll().filterNotNull()
            }
            resultsList.sortedByDescending { it.timestamp }
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun addStarshipResult(centerId: String, result: StarshipResult) {
        try {
            val sessionRef = db.collection("centers").document(centerId)
                .collection("patients").document(result.patientId)
                .collection("sessions").document(result.sessionId)

            sessionRef.set(mapOf("sessionId" to result.sessionId, "timestamp" to result.timestamp), SetOptions.merge()).await()
            val ref = sessionRef.collection("results").document("starshipDefender")
            ref.set(result.copy(testId = ref.id)).await()
        } catch (e: Exception) {}
    }

    override suspend fun getStarshipResults(centerId: String, patientId: String): List<StarshipResult> {
        return try {
            val sessionsSnapshot = db.collection("centers").document(centerId)
                .collection("patients").document(patientId)
                .collection("sessions").get().await()

            val resultsList = coroutineScope {
                sessionsSnapshot.documents.map { sessionDoc ->
                    async {
                        val resultDoc = sessionDoc.reference.collection("results").document("starshipDefender").get().await()
                        if (resultDoc.exists()) resultDoc.toObject(StarshipResult::class.java) else null
                    }
                }.awaitAll().filterNotNull()
            }
            resultsList.sortedByDescending { it.timestamp }
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun addHolePuzzleResult(centerId: String, result: HolePuzzleResult) {
        try {
            val sessionRef = db.collection("centers").document(centerId)
                .collection("patients").document(result.patientId)
                .collection("sessions").document(result.sessionId)

            sessionRef.set(mapOf("sessionId" to result.sessionId, "timestamp" to result.timestamp), SetOptions.merge()).await()
            val ref = sessionRef.collection("results").document("holePuzzle")
            ref.set(result.copy(testId = ref.id)).await()
        } catch (e: Exception) {}
    }

    override suspend fun getHolePuzzleResults(centerId: String, patientId: String): List<HolePuzzleResult> {
        return try {
            val sessionsSnapshot = db.collection("centers").document(centerId)
                .collection("patients").document(patientId)
                .collection("sessions").get().await()

            val resultsList = coroutineScope {
                sessionsSnapshot.documents.map { sessionDoc ->
                    async {
                        val resultDoc = sessionDoc.reference.collection("results").document("holePuzzle").get().await()
                        if (resultDoc.exists()) resultDoc.toObject(HolePuzzleResult::class.java) else null
                    }
                }.awaitAll().filterNotNull()
            }
            resultsList.sortedByDescending { it.timestamp }
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun addStepGameResult(centerId: String, result: StepGameResult) {
        try {
            val sessionRef = db.collection("centers").document(centerId)
                .collection("patients").document(result.patientId)
                .collection("sessions").document(result.sessionId)

            sessionRef.set(mapOf("sessionId" to result.sessionId, "timestamp" to result.timestamp), SetOptions.merge()).await()
            val ref = sessionRef.collection("results").document("stepGame")
            ref.set(result.copy(testId = ref.id)).await()
        } catch (e: Exception) {}
    }

    override suspend fun getStepGameResults(centerId: String, patientId: String): List<StepGameResult> {
        return try {
            val sessionsSnapshot = db.collection("centers").document(centerId)
                .collection("patients").document(patientId)
                .collection("sessions").get().await()

            val resultsList = coroutineScope {
                sessionsSnapshot.documents.map { sessionDoc ->
                    async {
                        val resultDoc = sessionDoc.reference.collection("results").document("stepGame").get().await()
                        if (resultDoc.exists()) resultDoc.toObject(StepGameResult::class.java) else null
                    }
                }.awaitAll().filterNotNull()
            }
            resultsList.sortedByDescending { it.timestamp }
        } catch (e: Exception) { emptyList() }
    }
}