package com.ripplehealthcare.bproboard.data.repository

import android.util.Log
import com.ripplehealthcare.bproboard.domain.model.Patient
import com.ripplehealthcare.bproboard.domain.model.StaticBalanceResult
import com.ripplehealthcare.bproboard.domain.repository.TestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.google.firebase.firestore.FirebaseFirestore
import com.ripplehealthcare.bproboard.domain.model.Gender
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.SetOptions
import com.ripplehealthcare.bproboard.domain.model.ColorSorterResult
import com.ripplehealthcare.bproboard.domain.model.HolePuzzleResult
import com.ripplehealthcare.bproboard.domain.model.PatternDrawingResult
import com.ripplehealthcare.bproboard.domain.model.RatPuzzleResult
import com.ripplehealthcare.bproboard.domain.model.ShapeTrainingResult
import com.ripplehealthcare.bproboard.domain.model.StarshipResult
import com.ripplehealthcare.bproboard.domain.model.StepGameResult

class TestRepositoryImpl : TestRepository {
    private val db = FirebaseFirestore.getInstance()
    private val _errorEvent = MutableSharedFlow<String>()
    override val errorEvent = _errorEvent.asSharedFlow()

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

    override fun setPatient(patient: Patient) {
        _patient.value = patient
    }

    // ==========================================
    // 1. STATIC BALANCE
    // ==========================================
    override suspend fun addStaticBalanceResult(centerId: String, result: StaticBalanceResult) {
        try {
            val sessionRef = db.collection("centers").document(centerId)
                .collection("patients").document(result.patientId)
                .collection("sessions").document(result.sessionId)

            val dummySessionData = mapOf("sessionId" to result.sessionId, "timestamp" to result.timestamp)
            sessionRef.set(dummySessionData, SetOptions.merge()).await()

            // FIXED: Appended unique timestamp to prevent overwriting
            val ref = sessionRef.collection("results").document("staticBalance_${System.currentTimeMillis()}")
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
                        // FIXED: Fetch all results and filter by prefix to handle multiple levels in one session
                        val resultsSnapshot = sessionDoc.reference.collection("results").get().await()
                        resultsSnapshot.documents.mapNotNull { doc ->
                            if (doc.id.startsWith("staticBalance")) {
                                val result = doc.toObject(StaticBalanceResult::class.java)
                                if (result != null && result.totalTimeMs > 0) result else null
                            } else null
                        }
                    }
                }.awaitAll().flatten()
            }

            resultsList.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==========================================
    // 2. PATTERN DRAWING
    // ==========================================
    override suspend fun addPatternDrawingResult(centerId: String, result: PatternDrawingResult) {
        try {
            val sessionRef = db.collection("centers").document(centerId)
                .collection("patients").document(result.patientId)
                .collection("sessions").document(result.sessionId)

            val dummySessionData = mapOf("sessionId" to result.sessionId, "timestamp" to result.timestamp)
            sessionRef.set(dummySessionData, SetOptions.merge()).await()

            val ref = sessionRef.collection("results").document("patternDrawing_${System.currentTimeMillis()}")
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
                        val resultsSnapshot = sessionDoc.reference.collection("results").get().await()
                        resultsSnapshot.documents.mapNotNull { doc ->
                            if (doc.id.startsWith("patternDrawing")) doc.toObject(PatternDrawingResult::class.java) else null
                        }
                    }
                }.awaitAll().flatten()
            }
            resultsList.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==========================================
    // 3. SHAPE TRAINING
    // ==========================================
    override suspend fun addShapeTrainingResult(centerId: String, result: ShapeTrainingResult) {
        try {
            val sessionRef = db.collection("centers").document(centerId)
                .collection("patients").document(result.patientId)
                .collection("sessions").document(result.sessionId)

            val dummySessionData = mapOf("sessionId" to result.sessionId, "timestamp" to result.timestamp)
            sessionRef.set(dummySessionData, SetOptions.merge()).await()

            val ref = sessionRef.collection("results").document("shapeTraining_${System.currentTimeMillis()}")
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
                        val resultsSnapshot = sessionDoc.reference.collection("results").get().await()
                        resultsSnapshot.documents.mapNotNull { doc ->
                            if (doc.id.startsWith("shapeTraining")) doc.toObject(ShapeTrainingResult::class.java) else null
                        }
                    }
                }.awaitAll().flatten()
            }
            resultsList.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==========================================
    // 4. COLOR SORTER
    // ==========================================
    override suspend fun addColorSorterResult(centerId: String, result: ColorSorterResult) {
        try {
            val sessionRef = db.collection("centers").document(centerId)
                .collection("patients").document(result.patientId)
                .collection("sessions").document(result.sessionId)

            val dummySessionData = mapOf("sessionId" to result.sessionId, "timestamp" to result.timestamp)
            sessionRef.set(dummySessionData, SetOptions.merge()).await()

            val ref = sessionRef.collection("results").document("colorSorter_${System.currentTimeMillis()}")
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
                        val resultsSnapshot = sessionDoc.reference.collection("results").get().await()
                        resultsSnapshot.documents.mapNotNull { doc ->
                            if (doc.id.startsWith("colorSorter")) doc.toObject(ColorSorterResult::class.java) else null
                        }
                    }
                }.awaitAll().flatten()
            }
            resultsList.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==========================================
    // 5. RAT PUZZLE (MAZE BALANCE)
    // ==========================================
    override suspend fun addRatPuzzleResult(centerId: String, result: RatPuzzleResult) {
        try {
            val sessionRef = db.collection("centers").document(centerId)
                .collection("patients").document(result.patientId)
                .collection("sessions").document(result.sessionId)

            val dummySessionData = mapOf("sessionId" to result.sessionId, "timestamp" to result.timestamp)
            sessionRef.set(dummySessionData, SetOptions.merge()).await()

            val ref = sessionRef.collection("results").document("ratPuzzle_${System.currentTimeMillis()}")
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
                        val resultsSnapshot = sessionDoc.reference.collection("results").get().await()
                        resultsSnapshot.documents.mapNotNull { doc ->
                            if (doc.id.startsWith("ratPuzzle")) doc.toObject(RatPuzzleResult::class.java) else null
                        }
                    }
                }.awaitAll().flatten()
            }
            resultsList.sortedByDescending { it.timestamp }
        } catch (e: Exception) { emptyList() }
    }

    // ==========================================
    // 6. STARSHIP DEFENDER
    // ==========================================
    override suspend fun addStarshipResult(centerId: String, result: StarshipResult) {
        try {
            val sessionRef = db.collection("centers").document(centerId)
                .collection("patients").document(result.patientId)
                .collection("sessions").document(result.sessionId)

            sessionRef.set(mapOf("sessionId" to result.sessionId, "timestamp" to result.timestamp), SetOptions.merge()).await()
            val ref = sessionRef.collection("results").document("starshipDefender_${System.currentTimeMillis()}")
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
                        val resultsSnapshot = sessionDoc.reference.collection("results").get().await()
                        resultsSnapshot.documents.mapNotNull { doc ->
                            if (doc.id.startsWith("starshipDefender")) doc.toObject(StarshipResult::class.java) else null
                        }
                    }
                }.awaitAll().flatten()
            }
            resultsList.sortedByDescending { it.timestamp }
        } catch (e: Exception) { emptyList() }
    }

    // ==========================================
    // 7. HOLE NAVIGATOR
    // ==========================================
    override suspend fun addHolePuzzleResult(centerId: String, result: HolePuzzleResult) {
        try {
            val sessionRef = db.collection("centers").document(centerId)
                .collection("patients").document(result.patientId)
                .collection("sessions").document(result.sessionId)

            sessionRef.set(mapOf("sessionId" to result.sessionId, "timestamp" to result.timestamp), SetOptions.merge()).await()
            val ref = sessionRef.collection("results").document("holePuzzle_${System.currentTimeMillis()}")
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
                        val resultsSnapshot = sessionDoc.reference.collection("results").get().await()
                        resultsSnapshot.documents.mapNotNull { doc ->
                            if (doc.id.startsWith("holePuzzle")) doc.toObject(HolePuzzleResult::class.java) else null
                        }
                    }
                }.awaitAll().flatten()
            }
            resultsList.sortedByDescending { it.timestamp }
        } catch (e: Exception) { emptyList() }
    }

    // ==========================================
    // 8. STEP GAME
    // ==========================================
    override suspend fun addStepGameResult(centerId: String, result: StepGameResult) {
        try {
            val sessionRef = db.collection("centers").document(centerId)
                .collection("patients").document(result.patientId)
                .collection("sessions").document(result.sessionId)

            sessionRef.set(mapOf("sessionId" to result.sessionId, "timestamp" to result.timestamp), SetOptions.merge()).await()
            val ref = sessionRef.collection("results").document("stepGame_${System.currentTimeMillis()}")
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
                        val resultsSnapshot = sessionDoc.reference.collection("results").get().await()
                        resultsSnapshot.documents.mapNotNull { doc ->
                            if (doc.id.startsWith("stepGame")) doc.toObject(StepGameResult::class.java) else null
                        }
                    }
                }.awaitAll().flatten()
            }
            resultsList.sortedByDescending { it.timestamp }
        } catch (e: Exception) { emptyList() }
    }
}