package com.ripplehealthcare.frst

import android.app.Application
import android.util.Log
import com.ripplehealthcare.frst.core.di.AppContainer
import com.google.firebase.FirebaseApp

class BalanceApp : Application() {
    lateinit var appContainer: AppContainer

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        Log.d("BalanceApp", "Firebase initialized successfully")

        appContainer = AppContainer() // Initialize empty AppContainer
        // Initialization of deviceRepository and testRepository will be handled later
    }
}