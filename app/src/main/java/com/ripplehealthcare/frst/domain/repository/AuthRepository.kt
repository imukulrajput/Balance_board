package com.ripplehealthcare.frst.domain.repository

import android.app.Activity
import com.ripplehealthcare.frst.domain.model.AuthState
import com.ripplehealthcare.frst.domain.model.UserProfile
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.ripplehealthcare.frst.domain.model.CenterProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getCurrentUser(): FirebaseUser?
    fun signInWithCredential(credential: PhoneAuthCredential): Flow<AuthState>
    fun signInWithGoogleCredential(idToken: String): Flow<AuthState>
    suspend fun saveCenterProfile(centerProfile: CenterProfile)
    suspend fun getCenterProfile(uid: String): CenterProfile?
    suspend fun saveUserProfile(userProfile: UserProfile)
    suspend fun getUserProfile(uid: String): UserProfile?
    fun signInWithEmail(email: String, password: String): Flow<AuthState>
    fun signUpWithEmail(email: String, password: String): Flow<AuthState>
    suspend fun getEmailFromPhone(phone: String): String?
    fun sendPasswordResetEmail(email: String): Flow<AuthState>
    suspend fun linkPhoneWithAccount(phoneNumber: String, activity: Activity)
    fun signOut()
    fun sendOtp(phoneNumber: String, activity: Activity, callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks)
}