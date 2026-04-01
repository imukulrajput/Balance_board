package com.ripplehealthcare.frst.domain.usecase

import com.ripplehealthcare.frst.domain.repository.DeviceRepository

class ConnectDeviceUseCase(private val repository: DeviceRepository) {
    suspend operator fun invoke(): Boolean = repository.connect()
}