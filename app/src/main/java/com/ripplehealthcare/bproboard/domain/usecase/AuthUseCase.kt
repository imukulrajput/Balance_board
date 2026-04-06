package com.ripplehealthcare.bproboard.domain.usecase

import com.ripplehealthcare.bproboard.domain.model.AuthState
import com.ripplehealthcare.bproboard.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import com.ripplehealthcare.bproboard.domain.model.CenterProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class AuthUseCase(private val repository: AuthRepository) {
    fun signInWithGoogle(idToken: String): Flow<AuthState> = flow {
        emitAll(repository.signInWithGoogleCredential(idToken))
    }

    fun signInEmail(email: String, password: String) = repository.signInWithEmail(email, password)
    fun signUpEmail(email: String, password: String) = repository.signUpWithEmail(email, password)
    suspend fun getEmailFromPhone(identity: String): String? = repository.getEmailFromPhone(identity)

    fun resetPassword(email: String) = repository.sendPasswordResetEmail(email)

    fun getCurrentUser(): FirebaseUser? = repository.getCurrentUser()

    fun signOut() {
        repository.signOut()
    }

    suspend fun saveCenterProfile(profile: CenterProfile) = repository.saveCenterProfile(profile)
    suspend fun getCenterProfile(uid: String) = repository.getCenterProfile(uid)

}