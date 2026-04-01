// core/di/AppContainer.kt
package com.ripplehealthcare.frst.core.di

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.ripplehealthcare.frst.data.bluetooth.BluetoothDataSourceImpl
import com.ripplehealthcare.frst.data.firebase.AuthRepositoryImpl
import com.ripplehealthcare.frst.data.firebase.GoogleAuthManager
import com.ripplehealthcare.frst.data.firebase.ManagementRepositoryImpl
import com.ripplehealthcare.frst.data.firebase.PatientRepositoryImpl
import com.ripplehealthcare.frst.data.repository.DeviceRepositoryImpl
import com.ripplehealthcare.frst.data.repository.TestRepositoryImpl
import com.ripplehealthcare.frst.domain.repository.DeviceRepository
import com.ripplehealthcare.frst.domain.repository.ManagementRepository
import com.ripplehealthcare.frst.domain.repository.TestRepository
import com.ripplehealthcare.frst.domain.usecase.AuthUseCase
import com.ripplehealthcare.frst.domain.usecase.PatientUseCase
import com.ripplehealthcare.frst.ui.viewmodel.AuthViewModel
import com.ripplehealthcare.frst.ui.viewmodel.BluetoothViewModel
import com.ripplehealthcare.frst.ui.viewmodel.ManagementViewModel
import com.ripplehealthcare.frst.ui.viewmodel.PatientViewModel
import com.ripplehealthcare.frst.ui.viewmodel.TestViewModel

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