package com.sunflower.pinpic.domain.repository

import com.sunflower.pinpic.common.Result

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun register(email: String, password: String): Result<Unit>
    suspend fun googleSignIn(idTokenString: String): Result<Unit>
    suspend fun setUsername(username: String): Result<Unit>
}
