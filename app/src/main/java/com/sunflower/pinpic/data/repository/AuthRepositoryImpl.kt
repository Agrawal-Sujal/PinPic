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
import com.sunflower.pinpic.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val dataStoreManager: DataStoreManager,
    private val deviceRepository: DeviceRepository
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
                    email = result.data.emailId,
                    pictureUrl = result.data.profileUrl,
                    userId = result.data.userId
                )
                // If we don't have the device ID, trigger the request to get it and store it in DataStore
                deviceRepository.getOrRegisterDeviceId(
                    jwtToken = result.data.jwt,
                    userId = result.data.userId
                )
                Result.Success(Unit)
            }
            is Result.Error -> Result.Error(result.message)
        }
    }

    override suspend fun register(email: String, password: String): Result<Unit> {
        // If we don't have the device ID, trigger the request to get it and store it in DataStore
        deviceRepository.getOrRegisterDeviceId()

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
                    email = result.data.emailId,
                    pictureUrl = result.data.profileUrl,
                    userId = result.data.userId
                )
                // If we don't have the device ID, trigger the request to get it and store it in DataStore
                deviceRepository.getOrRegisterDeviceId(
                    jwtToken = result.data.jwt,
                    userId = result.data.userId
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
                    username = username
                )
                Result.Success(Unit)
            }
            is Result.Error -> Result.Error(result.message)
        }
    }
}
