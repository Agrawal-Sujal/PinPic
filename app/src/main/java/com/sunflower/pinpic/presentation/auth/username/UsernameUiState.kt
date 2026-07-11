package com.sunflower.pinpic.presentation.auth.username

data class UsernameUiState(
    val usernameText: String = "",
    val email: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
