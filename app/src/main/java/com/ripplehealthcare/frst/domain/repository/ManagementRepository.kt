package com.ripplehealthcare.frst.domain.repository

import com.ripplehealthcare.frst.domain.model.DoctorProfile
import kotlinx.coroutines.flow.Flow

interface ManagementRepository {
    suspend fun addDoctor(centerId: String, doctor: DoctorProfile): Result<Unit>
    fun getDoctors(centerId: String): Flow<List<DoctorProfile>>
    suspend fun updateDoctorStatus(centerId: String, doctorId: String, status: String): Result<Unit>
    suspend fun getDoctorById(centerId: String, doctorId: String): DoctorProfile?
}