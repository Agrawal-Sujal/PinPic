package com.sunflower.pinpic.presentation.splash

/**
 * One-time side effects for the Splash screen.
 */
sealed interface SplashSideEffect {
    /** Emitted when the user is not authenticated. */
    data object NavigateToLogin : SplashSideEffect
    
    /** Emitted when the user is already authenticated. */
    data object NavigateToMain : SplashSideEffect

    data object NavigationToOnboarding: SplashSideEffect
}
