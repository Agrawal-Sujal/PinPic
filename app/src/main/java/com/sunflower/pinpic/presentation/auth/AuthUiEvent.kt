package com.sunflower.pinpic.presentation.auth

import android.content.Context

sealed interface AuthUiEvent {
    data class OnEmailChanged(val email: String) : AuthUiEvent
    data class OnPasswordChanged(val password: String) : AuthUiEvent
    data class OnConfirmPasswordChanged(val password: String) : AuthUiEvent
    object OnTogglePasswordVisibility : AuthUiEvent
    object OnToggleConfirmPasswordVisibility : AuthUiEvent
    object OnLoginClicked : AuthUiEvent
    object OnSignUpClicked : AuthUiEvent
    data class OnGoogleAuthClicked(val context: Context) : AuthUiEvent
    object OnForgotPasswordClicked : AuthUiEvent
    object OnClearError : AuthUiEvent
}