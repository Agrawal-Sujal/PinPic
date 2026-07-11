package com.sunflower.pinpic.data.repository

import com.sunflower.pinpic.common.Result
import com.sunflower.pinpic.data.local.datastore.DataStoreManager
import com.sunflower.pinpic.data.remote.api.AuthApi
import com.sunflower.pinpic.data.remote.dto.GoogleAuthRequest
import com.sunflower.pinpic.data.remote.dto.LoginRequest
import com.sunflower.pinpic.data.remote.dto.RegisterRequest
import com.sunflower.pinpic.data.remote.dto.SetUsernameRequest
import com.sunflower.pinpic.data.remote.parseResponse
import com.sunflower.pinpic.domain.repository.AuthRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val dataStoreManager: DataStoreManager
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<Unit> {
        val result = parseResponse {
            api.login(LoginRequest(email, password))
        }

       return when (result) {
            is Result.Success -> {
                dataStoreManager.saveAuthInfo(
                    jwt = result.data.jwt,
                    username = result.data.username,
                    email = email,
                    pictureUrl = null // Backend doesn't return this yet for simple login
                )
                Result.Success(Unit)
            }
            is Result.Error -> Result.Error(result.message)
        }
    }

    override suspend fun register(email: String, password: String): Result<Unit> {
        return parseResponse {
            api.register(RegisterRequest(email, password))
        }
    }

    override suspend fun googleSignIn(idTokenString: String): Result<Unit> {
        val result = parseResponse {
            api.googleAuth(GoogleAuthRequest(idTokenString))
        }

        return when (result) {
            is Result.Success -> {
                dataStoreManager.saveAuthInfo(
                    jwt = result.data.jwt,
                    username = result.data.username,
                    email = null, // Backend doesn't return this in Google output in the description
                    pictureUrl = null
                )
                Result.Success(Unit)
            }
            is Result.Error -> Result.Error(result.message)
        }
    }

    override suspend fun setUsername(username: String): Result<Unit> {
        val jwt = dataStoreManager.jwtTokenFlow.firstOrNull()
            ?: return Result.Error("Authentication required")

        val result = parseResponse {
            api.setUsername("Bearer $jwt", SetUsernameRequest(username))
        }

        return when (result) {
            is Result.Success -> {
                // Update username in DataStore
                dataStoreManager.saveAuthInfo(
                    jwt = jwt,
                    username = username,
                    email = null, 
                    pictureUrl = null 
                )
                Result.Success(Unit)
            }
            is Result.Error -> Result.Error(result.message)
        }
    }
}
