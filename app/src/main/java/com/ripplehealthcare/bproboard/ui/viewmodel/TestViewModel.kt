package com.ripplehealthcare.bproboard.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ripplehealthcare.bproboard.domain.model.Patient
import com.ripplehealthcare.bproboard.domain.model.StaticBalanceResult
import com.ripplehealthcare.bproboard.domain.repository.TestRepository
import com.ripplehealthcare.bproboard.domain.model.ColorSorterResult
import com.ripplehealthcare.bproboard.domain.model.HolePuzzleResult
import com.ripplehealthcare.bproboard.domain.model.PatternDrawingResult
import com.ripplehealthcare.bproboard.domain.model.RatPuzzleResult
import com.ripplehealthcare.bproboard.domain.model.ShapeTrainingResult
import com.ripplehealthcare.bproboard.domain.model.StarshipResult
import com.ripplehealthcare.bproboard.domain.model.StepGameResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TestViewModel(private val testRepository: TestRepository) : ViewModel() {

    var activeSessionId: String? = null
        private set

    fun startNewSession() {
        if (activeSessionId == null) {
            activeSessionId = java.util.UUID.randomUUID().toString()
        }
    }
    fun endSession() {
        activeSessionId = null

    }
    var trainingPosture: String = "STANDING"

    val patient: StateFlow<Patient> = testRepository.patient

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

    fun setPatient(patient: Patient) = testRepository.setPatient(patient)

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