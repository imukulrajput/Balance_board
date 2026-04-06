package com.ripplehealthcare.bproboard.data.firebase

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class GoogleAuthManager(private val activity: Activity) {
    private val gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("34377190513-i0jubp2iqk93pjd4p7bp19hdj2quhh3j.apps.googleusercontent.com") // Replace with your Web Client ID from Firebase Console
        .requestEmail()
        .build()

    val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(activity, gso)

    fun getSignInIntent(): Intent = googleSignInClient.signInIntent
}