package com.sunflower.pinpic.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// --- Brand ---
val CoralLight = Color(0xFFD85A30)
val CoralDark  = Color(0xFFF0997B)

val TealLight = Color(0xFF1D9E75)
val TealDark  = Color(0xFF5DCAA5)

// --- Light neutrals (unchanged) ---
val BgLight = Color(0xFFFDF8F3)
val SurfaceLight = Color(0xFFFFFFFF)
val TextPrimaryLight = Color(0xFF2B2420)
val TextSecondaryLight = Color(0xFF8A7F73)
val BorderLight = Color(0xFFEDE6DC)

// --- Dark neutrals: warm near-black, with real elevation steps ---
val BgDark               = Color(0xFF15120E) // base background (slightly darker, less muddy than before)
val SurfaceDark          = Color(0xFF1F1B16) // cards
val SurfaceContainerDark = Color(0xFF2A241D) // sheets, dialogs, elevated cards
val SurfaceContainerHighDark = Color(0xFF352E25) // top/bottom bars, nav — needs to read as "above" content

val TextPrimaryDark   = Color(0xFFF2EAE0) // pulled slightly off pure white — less eye strain, avoids halation on OLED
val TextSecondaryDark = Color(0xFFB6A996)

val OutlineDark        = Color(0xFF4A4136) // actual dividers/borders — now visibly distinct from surfaces
val OutlineVariantDark = Color(0xFF322B22) // subtle/low-emphasis dividers

val BorderDark = OutlineVariantDark // kept for backward compat if referenced elsewhere

// --- Error (harmonized warm-red, not a stock M3 red) ---
val ErrorDark   = Color(0xFFFFB4A0)
val OnErrorDark = Color(0xFF5C1400)

val LightColorScheme = lightColorScheme(
    primary        = CoralLight,
    onPrimary      = SurfaceLight,
    secondary      = TealLight,
    onSecondary    = SurfaceLight,
    background     = BgLight,
    onBackground   = TextPrimaryLight,
    surface        = SurfaceLight,
    onSurface      = TextPrimaryLight,
    surfaceVariant = BorderLight,
    onSurfaceVariant = TextSecondaryLight,
)

val DarkColorScheme = darkColorScheme(
    primary          = CoralDark,
    onPrimary        = Color(0xFF4A1B0C),
    primaryContainer = Color(0xFF6B2A13),   // for filled buttons/chips that need a coral "container" tone
    onPrimaryContainer = Color(0xFFFFDBCB),

    secondary          = TealDark,
    onSecondary        = Color(0xFF04342C),
    secondaryContainer = Color(0xFF0B4C40),
    onSecondaryContainer = Color(0xFFB9F2DE),

    background   = BgDark,
    onBackground = TextPrimaryDark,

    surface   = SurfaceDark,
    onSurface = TextPrimaryDark,

    surfaceVariant   = SurfaceContainerHighDark, // chips, input fields — now a real step up from surface
    onSurfaceVariant = TextSecondaryDark,

    outline        = OutlineDark,
    outlineVariant = OutlineVariantDark,

    error             = ErrorDark,
    onError           = OnErrorDark,
    errorContainer    = Color(0xFF7C2B14),
    onErrorContainer  = Color(0xFFFFDAD2),

    inverseSurface   = TextPrimaryDark,
    inverseOnSurface = SurfaceDark,
    inversePrimary   = CoralLight,
)