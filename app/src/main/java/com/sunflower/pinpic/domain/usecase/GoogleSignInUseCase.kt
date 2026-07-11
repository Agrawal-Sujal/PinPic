package com.sunflower.pinpic.domain.usecase

import com.sunflower.pinpic.common.Result
import com.sunflower.pinpic.domain.repository.AuthRepository
import javax.inject.Inject

class GoogleSignInUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(token: String): Result<Unit> {
        if (token.isBlank()) {
            return Result.Error("Google ID Token cannot be empty")
        }
        return repository.googleSignIn(token)
    }
}
