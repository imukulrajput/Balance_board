package com.ripplehealthcare.frst.data.firebase

import android.util.Log
import com.ripplehealthcare.frst.domain.model.CompleteTestResult
import com.ripplehealthcare.frst.domain.model.Patient
import com.ripplehealthcare.frst.domain.repository.PatientRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class PatientRepositoryImpl : PatientRepository {

    private val db = FirebaseFirestore.getInstance()

    override suspend fun addPatient(centerId: String, patient: Patient): Patient {
        try {
            val ref = db.collection("centers").document(centerId)
                .collection("patients").document()

            val finalPatient = patient.copy(
                patientId = ref.id,
                centerId = centerId
            )

            ref.set(finalPatient).await()
            return finalPatient
        } catch (e: Exception) {
            Log.e("PatientRepository", "Error adding patient", e)
            throw e
        }
    }

    override suspend fun getPatientsByDoctor(centerId: String, doctorId: String): List<Patient> {
        return try {
            val snapshot = db.collection("centers").document(centerId)
                .collection("patients")
                .whereEqualTo("doctorId", doctorId) // Filter by doctor
                .get()
                .await()

            snapshot.toObjects(Patient::class.java)
        } catch (e: Exception) {
            Log.e("PatientRepository", "Error fetching patients", e)
            emptyList()
        }
    }

    override suspend fun updatePatient(patient: Patient) {
        try {
            val patientRef = db.collection("centers")
                .document(patient.centerId)
                .collection("patients")
                .document(patient.patientId)

            patientRef.set(patient, SetOptions.merge()).await()

            Log.d("PatientRepository", "Patient updated: ${patient.name}")
        } catch (e: Exception) {
            Log.e("PatientRepository", "Error updating patient: ${e.message}")
            throw e
        }
    }

    override suspend fun transferPatient(centerId: String, patientId: String, newDoctorId: String): Result<Unit> = try {
        db.collection("centers").document(centerId)
            .collection("patients").document(patientId)
            .update("doctorId", newDoctorId)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getPatients(userId: String): List<Patient> {
        return try {
            val snapshot = db.collection("patients")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            snapshot.toObjects(Patient::class.java)
        } catch (e: Exception) {
            Log.e("PatientRepository", "Error fetching patients", e)
            emptyList()
        }
    }

    override suspend fun getPatientById(centerId: String, patientId: String): Patient? = try {
        val snapshot = db.collection("centers").document(centerId)
            .collection("patients").document(patientId)
            .get()
            .await()

        snapshot.toObject(Patient::class.java)
    } catch (e: Exception) {
        null
    }

}

