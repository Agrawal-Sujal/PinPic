package com.sunflower.pinpic.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val CoralLight = Color(0xFFD85A30)
val CoralDark  = Color(0xFFF0997B)

val TealLight = Color(0xFF1D9E75)
val TealDark  = Color(0xFF5DCAA5)

val BgLight = Color(0xFFFDF8F3)
val BgDark  = Color(0xFF191510)

val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark  = Color(0xFF241F1A)

val TextPrimaryLight = Color(0xFF2B2420)
val TextPrimaryDark  = Color(0xFFF5EFE7)

val TextSecondaryLight = Color(0xFF8A7F73)
val TextSecondaryDark  = Color(0xFFA89A8C)

val BorderLight = Color(0xFFEDE6DC)
val BorderDark  = Color(0xFF362E27)

val LightColorScheme = lightColorScheme(
    primary        = CoralLight,      // pins, FAB, primary CTA
    onPrimary      = SurfaceLight,
    secondary      = TealLight,       // map paths, chat accents, links
    onSecondary    = SurfaceLight,
    background     = BgLight,
    onBackground   = TextPrimaryLight,
    surface        = SurfaceLight,    // cards, sheets, top/bottom bars
    onSurface      = TextPrimaryLight,
    surfaceVariant = BorderLight,     // hairline borders, dividers
    onSurfaceVariant = TextSecondaryLight,
)

val DarkColorScheme = darkColorScheme(
    primary        = CoralDark,
    onPrimary      = Color(0xFF4A1B0C),
    secondary      = TealDark,
    onSecondary    = Color(0xFF04342C),
    background     = BgDark,
    onBackground   = TextPrimaryDark,
    surface        = SurfaceDark,
    onSurface      = TextPrimaryDark,
    surfaceVariant = BorderDark,
    onSurfaceVariant = TextSecondaryDark,
)