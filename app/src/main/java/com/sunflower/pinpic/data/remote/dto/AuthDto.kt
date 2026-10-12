package com.sunflower.pinpic.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String
)

@Serializable
data class GoogleAuthRequest(
    val idTokenString: String
)

@Serializable
data class SetUsernameRequest(
    val username: String
)

@Serializable
data class AuthResponse(
    val jwt: String = "",
    val username: String? = null,
    val userId: String = "",
    val profileUrl: String? = null,
    val emailId: String = ""
)

@Serializable
data class ErrorResponse(
    val error: String
)
