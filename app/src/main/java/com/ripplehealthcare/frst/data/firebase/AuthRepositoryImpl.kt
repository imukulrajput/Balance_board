package com.ripplehealthcare.frst.data.firebase

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.ripplehealthcare.frst.domain.model.AuthState
import com.ripplehealthcare.frst.domain.model.UserProfile
import com.ripplehealthcare.frst.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.ripplehealthcare.frst.domain.model.CenterProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class AuthRepositoryImpl : AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun getCurrentUser(): FirebaseUser? = auth.currentUser

    override fun signInWithCredential(credential: PhoneAuthCredential): Flow<AuthState> = flow {
        emit(AuthState.Loading)
        try {
            val result = auth.signInWithCredential(credential).await()
            emit(AuthState.Authenticated(result.user!!))
        } catch (e: Exception) {
            emit(AuthState.Error(e.localizedMessage ?: "Authentication failed"))
        }
    }

    override fun signInWithGoogleCredential(idToken: String): Flow<AuthState> = flow {
        emit(AuthState.Loading)
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            emit(AuthState.Authenticated(result.user!!))
        } catch (e: Exception) {
            emit(AuthState.Error(e.localizedMessage ?: "Google sign-in failed"))
        }
    }

    override suspend fun saveCenterProfile(centerProfile: CenterProfile) {
        val db = FirebaseFirestore.getInstance()
        // SAVE TO CENTERS COLLECTION
        db.collection("centers").document(centerProfile.uid)
            .set(centerProfile)
            .await() // Use .await() for cleaner code in coroutines
    }

    override suspend fun getCenterProfile(uid: String): CenterProfile? {
        val db = FirebaseFirestore.getInstance()
        return try {
            // Always check the 'centers' collection first
            val snapshot = db.collection("centers").document(uid).get().await()
            snapshot.toObject(CenterProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveUserProfile(userProfile: UserProfile) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userProfile.uid)
            .set(userProfile)
            .addOnSuccessListener { Log.d("AuthRepository", "Profile saved for ${userProfile.uid}") }
            .addOnFailureListener { e -> Log.e("AuthRepository", "Profile save failed", e) }
    }

    override suspend fun getUserProfile(uid: String): UserProfile? {
        val db = FirebaseFirestore.getInstance()
        return try {
            val snapshot = db.collection("users").document(uid).get().await()
            snapshot.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Profile fetch failed", e)
            null
        }
    }

    override fun signInWithEmail(email: String, password: String): Flow<AuthState> = flow {
        emit(AuthState.Loading)
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            emit(AuthState.Authenticated(result.user!!))
        } catch (e: Exception) {
            emit(AuthState.Error(e.localizedMessage ?: "Login failed"))
        }
    }

    override fun signUpWithEmail(email: String, password: String): Flow<AuthState> = flow {
        emit(AuthState.Loading)
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user

            // Send verification email
            user?.sendEmailVerification()?.await()

            // We emit Authenticated, but with a note that verification is pending
            emit(AuthState.Authenticated(user!!))
        } catch (e: Exception) {
            emit(AuthState.Error(e.localizedMessage ?: "Registration failed"))
        }
    }

    override suspend fun getEmailFromPhone(phone: String): String? {
        val db = FirebaseFirestore.getInstance()
        return try {
            val snapshot = db.collection("centers")
                .whereEqualTo("contactPhone", phone.trim())
                .limit(1)
                .get()
                .await()

            // If a center is found, return the associated email
            if (!snapshot.isEmpty) {
                snapshot.documents.first().getString("adminEmail")
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error finding email by phone: ${e.message}")
            null
        }
    }

    override fun sendPasswordResetEmail(email: String): Flow<AuthState> = flow {
        emit(AuthState.Loading)
        try {
            auth.sendPasswordResetEmail(email).await()
            // We can emit a special state or use Error with a custom message
            emit(AuthState.Error("Password reset link sent to your email."))
        } catch (e: Exception) {
            emit(AuthState.Error(e.localizedMessage ?: "Failed to send reset email"))
        }
    }

    override suspend fun linkPhoneWithAccount(phoneNumber: String, activity: Activity) {
        val user = auth.currentUser ?: return

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+91$phoneNumber")       // Ensure country code is included
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Automatically link if Google Play Services detects the SMS
                    user.updatePhoneNumber(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e("AuthRepo", "Phone linking failed", e)
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    // Normally you'd ask for OTP here, but if you want it
                    // "saved" silently, Firebase requires this verification step.
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    override fun signOut() {
        auth.signOut()
    }

    override fun sendOtp(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
}