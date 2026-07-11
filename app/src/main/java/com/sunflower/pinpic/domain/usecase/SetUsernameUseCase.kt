package com.sunflower.pinpic.domain.usecase

import com.sunflower.pinpic.common.Result
import com.sunflower.pinpic.domain.repository.AuthRepository
import javax.inject.Inject

class SetUsernameUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(username: String): Result<Unit> {
        if (username.isBlank()) {
            return Result.Error("Username cannot be blank")
        }
        if (username.length < 3) {
            return Result.Error("Username must be at least 3 characters")
        }
        return authRepository.setUsername(username)
    }
}
