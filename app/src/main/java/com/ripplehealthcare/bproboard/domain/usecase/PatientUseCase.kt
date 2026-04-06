package com.ripplehealthcare.bproboard.domain.usecase

import com.ripplehealthcare.bproboard.domain.model.Patient

import com.ripplehealthcare.bproboard.domain.repository.PatientRepository

class PatientUseCase(private val repository: PatientRepository) {

    suspend fun addPatient(centerId: String, patient: Patient): Patient =
        repository.addPatient(centerId, patient)

    suspend fun getPatientsByDoctor(centerId: String, doctorId: String): List<Patient> =
        repository.getPatientsByDoctor(centerId, doctorId)

    suspend fun updatePatient(patient: Patient) {
        repository.updatePatient(patient)
    }

    suspend fun transferPatient(centerId: String, patientId: String, newDoctorId: String): Result<Unit> {
        return repository.transferPatient(centerId, patientId, newDoctorId)
    }

    suspend fun getPatients(userId: String): List<Patient> =
        repository.getPatients(userId)

    suspend fun getPatientById(centerId: String, patientId: String): Patient? =
        repository.getPatientById(centerId,patientId)

}
