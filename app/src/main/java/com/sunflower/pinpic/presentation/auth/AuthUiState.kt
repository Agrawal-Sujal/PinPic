package com.sunflower.pinpic.presentation.auth

data class AuthUiState(
    val emailText: String = "",
    val passwordText: String = "",
    val confirmPasswordText: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)