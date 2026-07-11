package com.sunflower.pinpic.domain.usecase

import com.sunflower.pinpic.common.Result
import com.sunflower.pinpic.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        if (email.isBlank() || password.isBlank()) {
            return Result.Error("Email and password cannot be empty")
        }
        return repository.login(email, password)
    }
}
