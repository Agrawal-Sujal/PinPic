package com.sunflower.pinpic.presentation.navigation

/**
 * Typed route objects for every top-level destination.
 *
 * Rules:
 * - Each object is an immutable singleton (no params yet; add data class entries when screens
 *   need arguments, e.g. `data class PlaceDetail(val id: String) : Screen()`).
 * - NavDisplay back-stack stores instances of these sealed-class entries.
 */
sealed class Screen {
    /** Shown immediately on launch; handles routing decision. */
    data object Splash : Screen()

    /** Auth gate — shown when the user is not signed in. */
    data object Login : Screen()

    /** Onboarding flow — shown on first-ever launch after login. */
    data object Onboarding : Screen()

    /**
     * Main app shell with the bottom navigation bar.
     * Nested nav (Map / Album / Chat) lives inside this destination.
     */
    data object Main : Screen()
    data object SignUp: Screen()
    data object UsernameSetup: Screen()
    data object Camera : Screen()
}
