package com.ripplehealthcare.frst.domain.usecase

import com.ripplehealthcare.frst.domain.repository.TestRepository

class StopTestUseCase(private val repository: TestRepository) {
    operator fun invoke() = repository.stopTest()
}