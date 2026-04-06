package com.ripplehealthcare.bproboard.domain.usecase

import com.ripplehealthcare.bproboard.domain.repository.DeviceRepository

class ConnectDeviceUseCase(private val repository: DeviceRepository) {
    suspend operator fun invoke(): Boolean = repository.connect()
}