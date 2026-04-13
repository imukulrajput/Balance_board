package com.ripplehealthcare.bproboard.domain.repository

import com.ripplehealthcare.bproboard.domain.model.Patient
import com.ripplehealthcare.bproboard.domain.model.PatternDrawingResult
import com.ripplehealthcare.bproboard.domain.model.ShapeTrainingResult
import com.ripplehealthcare.bproboard.domain.model.StaticBalanceResult
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface TestRepository {
    val errorEvent: SharedFlow<String>
    val patient: StateFlow<Patient>

    fun setPatient(patient: Patient)

    suspend fun addStaticBalanceResult(centerId: String, result: StaticBalanceResult)
    suspend fun getStaticBalanceResults(centerId: String, patientId: String): List<StaticBalanceResult>

    suspend fun addPatternDrawingResult(centerId: String, result: PatternDrawingResult)
    suspend fun getPatternDrawingResults(centerId: String, patientId: String): List<PatternDrawingResult>

    suspend fun addShapeTrainingResult(centerId: String, result: ShapeTrainingResult)
    suspend fun getShapeTrainingResults(centerId: String, patientId: String): List<ShapeTrainingResult>

    suspend fun addColorSorterResult(centerId: String, result: com.ripplehealthcare.bproboard.domain.model.ColorSorterResult)

    suspend fun getColorSorterResults(centerId: String, patientId: String): List<com.ripplehealthcare.bproboard.domain.model.ColorSorterResult>

    suspend fun addRatPuzzleResult(centerId: String, result: com.ripplehealthcare.bproboard.domain.model.RatPuzzleResult)
    suspend fun getRatPuzzleResults(centerId: String, patientId: String): List<com.ripplehealthcare.bproboard.domain.model.RatPuzzleResult>

    suspend fun addStarshipResult(centerId: String, result: com.ripplehealthcare.bproboard.domain.model.StarshipResult)
    suspend fun getStarshipResults(centerId: String, patientId: String): List<com.ripplehealthcare.bproboard.domain.model.StarshipResult>

    suspend fun addHolePuzzleResult(centerId: String, result: com.ripplehealthcare.bproboard.domain.model.HolePuzzleResult)
    suspend fun getHolePuzzleResults(centerId: String, patientId: String): List<com.ripplehealthcare.bproboard.domain.model.HolePuzzleResult>

    suspend fun addStepGameResult(centerId: String, result: com.ripplehealthcare.bproboard.domain.model.StepGameResult)
    suspend fun getStepGameResults(centerId: String, patientId: String): List<com.ripplehealthcare.bproboard.domain.model.StepGameResult>
}