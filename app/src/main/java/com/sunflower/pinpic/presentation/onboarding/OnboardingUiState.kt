package com.sunflower.pinpic.presentation.onboarding

import androidx.compose.runtime.Immutable

@Immutable
data class OnboardingUiState(

    val title: String,

    val description: String,

    val illustration: IllustrationType

)

enum class IllustrationType {
    Camera,
    Map,
    Chat
}