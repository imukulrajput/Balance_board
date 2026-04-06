package com.ripplehealthcare.bproboard.domain.usecase

import com.ripplehealthcare.bproboard.domain.repository.TestRepository

class StartTestUseCase(private val repository: TestRepository) {
    operator fun invoke() = repository.startTest()
}