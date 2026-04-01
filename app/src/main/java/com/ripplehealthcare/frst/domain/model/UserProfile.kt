package com.ripplehealthcare.frst.domain.model

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val age: Int = 0,
    val gender: String = "",
    val weight: Float = 0f,
    val phone: String? = null,
    val email: String? = null,
    val clinicName: String = "",
    val clinicAddress: String = "",
    val createdAt: Long = System.currentTimeMillis()
)