package com.ripplehealthcare.bproboard.domain.model

data class Patient(
    // 1. Core Identity & Metrics
    val patientId: String = "",
    val centerId: String = "",
    val doctorId: String = "",
    val name: String = "",
    val age: String = "",
    val gender: String = "Male",
    val phone: String = "",
    val email: String = "",
    val height: String = "",
    val weight: String = "",

    // 2. Clinical Questionnaire Results
    val fallHistory: Boolean = false,       // Past 12 months falls
    val unsteadiness: Boolean = false,      // Feeling unsteady walking/standing
    val dizziness: Boolean = false,         // History of vertigo/lightheadedness
    val supportNeeded: Boolean = false,     // Needs hands to stand from chair
    val dizzyMeds: Boolean = false,         // On medication causing dizziness
    val fearOfFalling: Boolean = false,     // Worried about falling

    // 3. Physical Assessment
    val painAreas: List<String> = emptyList(),

    // 4. Metadata
    val createdAt: Long = System.currentTimeMillis()
)