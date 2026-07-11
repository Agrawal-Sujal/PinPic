package com.sunflower.pinpic.presentation.onboarding


fun onboardingPages() = listOf(

    OnboardingUiState(
        title = "Welcome to PinPic",
        description = "Every photo you take is automatically pinned to the place where it happened.\nTurn your memories into an interactive map.",
        illustration = IllustrationType.Camera
    ),

    OnboardingUiState(
        title = "Rediscover\nEvery Moment",
        description = "Browse your memories by location.\nTap any pin on the map to instantly revisit the photo you captured there.",
        illustration = IllustrationType.Map
    ),

    OnboardingUiState(
        title = "Share Without\nSwitching Apps",
        description = "Send photos directly to your friends through built-in chat while keeping every memory linked to its location.",
        illustration = IllustrationType.Chat
    )

)