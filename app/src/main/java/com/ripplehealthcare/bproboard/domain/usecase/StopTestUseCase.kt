package com.ripplehealthcare.bproboard.domain.usecase

import com.ripplehealthcare.bproboard.domain.repository.TestRepository

class StopTestUseCase(private val repository: TestRepository) {
    operator fun invoke() = repository.stopTest()
}