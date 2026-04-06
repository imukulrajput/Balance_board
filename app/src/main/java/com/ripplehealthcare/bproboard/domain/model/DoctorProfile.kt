package com.ripplehealthcare.bproboard.domain.model

data class DoctorProfile(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val specialization: String = "",
    val gender: String = "",
    val status: String = "Active",
    val profileImage: Int? = null,
    val centerId: String = ""
)