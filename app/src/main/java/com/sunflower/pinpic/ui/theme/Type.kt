package com.sunflower.pinpic.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PinPicTypography = Typography(
    titleLarge   = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium),   // Screen titles ("Album", "Map")
    titleMedium  = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),   // Card headers, sender name in chat
    bodyLarge    = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),   // Chat message text
    bodyMedium   = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),   // Secondary content
    labelSmall   = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),   // Timestamps, captions, pin counts
)