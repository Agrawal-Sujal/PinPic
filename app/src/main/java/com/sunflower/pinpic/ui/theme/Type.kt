package com.sunflower.pinpic.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PinPicTypography = Typography(
    // Display — big, rare, hero moments only (onboarding, empty-state illustrations)
    displayLarge  = TextStyle(fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.SemiBold),  // Onboarding hero line
    displayMedium = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold),  // "No pins yet" empty state
    displaySmall  = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Medium),    // Splash tagline

    // Headline — section-level headers inside a screen, one notch below the top app bar title
    headlineLarge  = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium),   // Rarely used; big standalone header
    headlineMedium = TextStyle(fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),   // Album grid month/section divider ("July 2026")
    headlineSmall  = TextStyle(fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium),   // Bottom-sheet header (pin detail sheet title)

    // Title — screen/app-bar titles and card headers
    titleLarge  = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),      // Screen titles ("Album", "Map")
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),      // Card headers, sender name in chat
    titleSmall  = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),      // Dialog titles, list item primary text

    // Body — actual content the user reads
    bodyLarge  = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal),       // Chat message text
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),       // Secondary content, pin detail description
    bodySmall  = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal),       // Helper text under inputs, list secondary line

    // Label — small UI text on interactive elements
    labelLarge  = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),      // Button text ("Send", "Save")
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),      // Nav bar item labels, chips
    labelSmall  = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),      // Timestamps, captions, pin counts
)