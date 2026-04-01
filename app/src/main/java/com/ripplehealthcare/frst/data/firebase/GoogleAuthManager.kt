package com.ripplehealthcare.frst.data.firebase

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class GoogleAuthManager(private val activity: Activity) {
    private val gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("761659689353-iqjnvsbav32o96dccjqf9mbq2p2k0dd4.apps.googleusercontent.com") // Replace with your Web Client ID from Firebase Console
        .requestEmail()
        .build()

    val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(activity, gso)

    fun getSignInIntent(): Intent = googleSignInClient.signInIntent
}