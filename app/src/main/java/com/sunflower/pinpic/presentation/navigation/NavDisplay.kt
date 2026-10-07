package com.sunflower.pinpic.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.sunflower.pinpic.presentation.auth.login.LoginScreen
import com.sunflower.pinpic.presentation.auth.signup.SignUpScreen
import com.sunflower.pinpic.presentation.auth.username.UsernameScreen

import com.sunflower.pinpic.presentation.common.SnackbarController
import com.sunflower.pinpic.presentation.onboarding.OnboardingScreen
import com.sunflower.pinpic.presentation.settings.SettingsScreen
import com.sunflower.pinpic.presentation.splash.SplashScreen

@Composable
fun AppNavDisplay() {
    val backStack = remember { mutableStateListOf<Screen>(Screen.Splash) }
    val snackbarHostState = remember { SnackbarHostState() }
    val currentMainScreen: MutableState<MainTab?> = remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        SnackbarController.messages.collect { message ->
            snackbarHostState.showSnackbar(message = message)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        NavDisplay(
            modifier = if (currentMainScreen.value != null && currentMainScreen.value == MainTab.Map) Modifier.padding(
                bottom = innerPadding.calculateBottomPadding()
            ) else Modifier.padding(innerPadding),
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
                    MainScreen(
                        onNavigateToSettings = {
                            backStack.add(Screen.Settings)
                        },
                        currentMainScreen
                    )
                }

                entry<Screen.Settings> {
                    SettingsScreen(
                        onBack = {
                            backStack.removeLastOrNull()
                        }
                    )
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
