package com.ripplehealthcare.frst.domain.usecase

import com.ripplehealthcare.frst.domain.repository.TestRepository

class StartTestUseCase(private val repository: TestRepository) {
    operator fun invoke() = repository.startTest()
}