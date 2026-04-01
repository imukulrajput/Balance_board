package com.ripplehealthcare.frst.domain.repository

import com.ripplehealthcare.frst.domain.model.CompleteTestResult
import com.ripplehealthcare.frst.domain.model.Patient

interface PatientRepository {
    suspend fun addPatient(centerId: String, patient: Patient):Patient
    suspend fun getPatientsByDoctor(centerId: String, doctorId: String): List<Patient>
    suspend fun updatePatient(patient: Patient)
    suspend fun transferPatient(centerId: String, patientId: String, newDoctorId: String): Result<Unit>
    suspend fun getPatients(userId: String): List<Patient>
    suspend fun getPatientById(centerId: String, patientId: String): Patient?
}
