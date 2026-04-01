package com.ripplehealthcare.frst.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.ripplehealthcare.frst.domain.model.DoctorProfile
import com.ripplehealthcare.frst.domain.repository.ManagementRepository
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ManagementRepositoryImpl : ManagementRepository {
    private val db = FirebaseFirestore.getInstance()

    override suspend fun addDoctor(centerId: String, doctor: DoctorProfile): Result<Unit> = try {
        val doctorRef = db.collection("centers").document(centerId)
            .collection("doctors").document() // Auto-generate ID

        val doctorWithId = doctor.copy(id = doctorRef.id)
        doctorRef.set(doctorWithId).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun getDoctors(centerId: String): Flow<List<DoctorProfile>> = callbackFlow {
        val subscription = db.collection("centers").document(centerId)
            .collection("doctors")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val doctors = snapshot?.toObjects(DoctorProfile::class.java) ?: emptyList()
                trySend(doctors)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun updateDoctorStatus(centerId: String, doctorId: String, status: String): Result<Unit> = try {
        db.collection("centers").document(centerId)
            .collection("doctors").document(doctorId)
            .update("status", status).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getDoctorById(centerId: String, doctorId: String): DoctorProfile? = try {
        val snapshot = db.collection("centers").document(centerId)
            .collection("doctors").document(doctorId)
            .get()
            .await()

        snapshot.toObject(DoctorProfile::class.java)
    } catch (e: Exception) {
        null // Return null on error to let the ViewModel handle the failure state
    }

}