package com.sunflower.pinpic.presentation.auth.username

sealed interface UsernameSideEffect {
    object NavigateToHome : UsernameSideEffect
    object NavigateToLogin : UsernameSideEffect
}
