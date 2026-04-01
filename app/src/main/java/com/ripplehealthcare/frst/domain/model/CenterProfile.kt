package com.ripplehealthcare.frst.domain.model

data class CenterProfile(
    val uid: String = "",           // Admin's Auth UID
    val centerName: String = "",
    val contactPhone: String = "",
    val adminEmail: String = "",

    val address: CenterAddress = CenterAddress(),

    // Billing Security: Keep these as numbers, not strings
    val unpaidSessions: Int = 0,
    val ratePerSession: Double = 100.0,

    val createdAt: Long = System.currentTimeMillis(),
    val role: String = "CENTER_ADMIN" // Security claim
)

data class CenterAddress(
    val fullAddress: String = "", // The complete string for display
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)