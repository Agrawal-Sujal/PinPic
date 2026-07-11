package com.sunflower.pinpic.presentation.auth

sealed interface AuthSideEffect {
    object NavigateToHome : AuthSideEffect
    object NavigateToLogin : AuthSideEffect
    object NavigateToSignUp : AuthSideEffect
    object NavigateToUsernameSetup : AuthSideEffect
}