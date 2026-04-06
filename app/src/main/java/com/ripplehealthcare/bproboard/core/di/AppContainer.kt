// core/di/AppContainer.kt
package com.ripplehealthcare.bproboard.core.di

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.ripplehealthcare.bproboard.data.bluetooth.BluetoothDataSourceImpl
import com.ripplehealthcare.bproboard.data.firebase.AuthRepositoryImpl
import com.ripplehealthcare.bproboard.data.firebase.GoogleAuthManager
import com.ripplehealthcare.bproboard.data.firebase.ManagementRepositoryImpl
import com.ripplehealthcare.bproboard.data.firebase.PatientRepositoryImpl
import com.ripplehealthcare.bproboard.data.repository.DeviceRepositoryImpl
import com.ripplehealthcare.bproboard.data.repository.TestRepositoryImpl
import com.ripplehealthcare.bproboard.domain.repository.DeviceRepository
import com.ripplehealthcare.bproboard.domain.repository.ManagementRepository
import com.ripplehealthcare.bproboard.domain.repository.TestRepository
import com.ripplehealthcare.bproboard.domain.usecase.AuthUseCase
import com.ripplehealthcare.bproboard.domain.usecase.PatientUseCase
import com.ripplehealthcare.bproboard.ui.viewmodel.AuthViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.BluetoothViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.ManagementViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.PatientViewModel
import com.ripplehealthcare.bproboard.ui.viewmodel.TestViewModel

class AppContainer{
    lateinit var deviceRepository: DeviceRepository
    private var testRepository: TestRepository = TestRepositoryImpl()
    private val managementRepository: ManagementRepository = ManagementRepositoryImpl()

    private val authUseCase = AuthUseCase(AuthRepositoryImpl())

    private val patientUseCase = PatientUseCase(PatientRepositoryImpl())

    fun initialize(context: Context, device: BluetoothDevice) {
        val bluetoothDataSource = BluetoothDataSourceImpl(device)
        deviceRepository = DeviceRepositoryImpl(bluetoothDataSource)
    }

    fun getBluetoothViewModel(context: Context): BluetoothViewModel = BluetoothViewModel(context)
    fun getTestViewModel(): TestViewModel = TestViewModel(testRepository) // Use initialized testRepository

    fun getAuthViewModel(): AuthViewModel {
        return AuthViewModel(authUseCase)
    }

    fun getManagementViewModel(): ManagementViewModel {
        return ManagementViewModel(managementRepository)
    }

    fun getGoogleAuthManager(activity: Activity): GoogleAuthManager {
        return GoogleAuthManager(activity)
    }

    fun getPatientViewModel(): PatientViewModel = PatientViewModel(patientUseCase)

}