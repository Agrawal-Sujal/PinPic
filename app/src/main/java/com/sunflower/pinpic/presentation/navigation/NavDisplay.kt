package com.sunflower.pinpic.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.sunflower.pinpic.presentation.auth.login.LoginScreen
import com.sunflower.pinpic.presentation.auth.signup.SignUpScreen
import com.sunflower.pinpic.presentation.common.SnackbarController
import com.sunflower.pinpic.presentation.onboarding.OnboardingScreen
import com.sunflower.pinpic.presentation.splash.SplashScreen
import com.sunflower.pinpic.presentation.auth.username.UsernameScreen

@Composable
fun AppNavDisplay() {
    val backStack = remember { mutableStateListOf<Screen>() }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        SnackbarController.messages.collect { message ->
            snackbarHostState.showSnackbar(message = message)
        }
    }

    backStack.add(Screen.Splash)

    // The one Scaffold for the whole app.
    // bottomBar is hidden on Splash and Login — those screens have no bottom nav.
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                        },
                        onNavigateToUsernameSetup = {
                            backStack.removeLastOrNull()
                            backStack.add(Screen.UsernameSetup)
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
                    com.sunflower.pinpic.presentation.main.MainScreen()
                }

                entry<Screen.Login> {
                    LoginScreen(
                        onNavigateToHome = {
                            backStack.removeLastOrNull()
                            backStack.add(Screen.Main)
                        }, 
                        onNavigateToSignUp = {
                            backStack.add(Screen.SignUp)
                        },
                        onNavigateToUsernameSetup = {
                            backStack.removeLastOrNull()
                            backStack.add(Screen.UsernameSetup)
                        }
                    )
                }
                entry<Screen.SignUp> {
                    SignUpScreen(
                        onNavigateToHome = {
                            backStack.removeLastOrNull()
                            backStack.add(Screen.Main)
                        }, 
                        onNavigateToLogin = {
                            backStack.removeLastOrNull()
                            backStack.add(Screen.Login)
                        },
                        onNavigateToUsernameSetup = {
                            backStack.removeLastOrNull()
                            backStack.add(Screen.UsernameSetup)
                        }
                    )
                }
                entry<Screen.UsernameSetup> {
                    UsernameScreen(
                        onNavigateToHome = {
                            backStack.removeLastOrNull()
                            backStack.add(Screen.Main)
                        },
                        onNavigateToLogin = {
                            backStack.removeLastOrNull()
                            backStack.add(Screen.Login)
                        }
                    )
                }
            }
        )
    }
}
