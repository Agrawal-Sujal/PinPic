package com.sunflower.pinpic.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.sunflower.pinpic.presentation.auth.login.LoginScreen
import com.sunflower.pinpic.presentation.onboarding.OnboardingScreen
import com.sunflower.pinpic.presentation.splash.SplashScreen

@Composable
fun AppNavDisplay() {
    val backStack = remember { mutableStateListOf<Screen>() }

    backStack.add(Screen.Splash)

    // The one Scaffold for the whole app.
    // bottomBar is hidden on Splash and Login — those screens have no bottom nav.
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // bottomBar will be added here once Main shell is implemented.
    ) { innerPadding ->

        NavDisplay(
            modifier = Modifier.padding(innerPadding),
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<Screen.Splash> {
                    SplashScreen(
                        onNavigateToLogin = {
                            backStack.removeLastOrNull()
                            backStack.add(Screen.Login)
                        },
                        onNavigateToMain = {
                            backStack.removeLastOrNull()
                            backStack.add(Screen.Main)
                        },
                        onNavigateToOnBoarding = {
                            backStack.removeLastOrNull()
                            backStack.add(Screen.Onboarding)
                        }
                    )
                }

                entry<Screen.Onboarding> {
                    OnboardingScreen(onFinish = {
                        backStack.removeLastOrNull()
                        backStack.add(Screen.Login)
                    })
                }
                entry<Screen.Main> {
                    // Placeholder — Main shell with bottom nav to be implemented in a future task.
                }

                entry<Screen.Login> {
                    LoginScreen()
                }
            }
        )
    }
}
