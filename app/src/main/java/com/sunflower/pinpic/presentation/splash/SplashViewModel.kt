package com.sunflower.pinpic.presentation.splash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import com.sunflower.pinpic.data.local.datastore.DataStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _sideEffect = MutableStateFlow<SplashSideEffect?>(null)
    val sideEffect = _sideEffect.asStateFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            try {
                val token = dataStoreManager.jwtTokenFlow.firstOrNull()
                if (token == null) {
                    Log.d("Splash", "No token found, redirecting to Onboarding")
                    _sideEffect.value = SplashSideEffect.NavigationToOnboarding
                } else {
                    val username = dataStoreManager.usernameFlow.firstOrNull()
                    if (username.isNullOrEmpty()) {
                        Log.d("Splash", "Token found but no username, redirecting to UsernameSetup")
                        _sideEffect.value = SplashSideEffect.NavigateToUsernameSetup
                    } else {
                        Log.d("Splash", "Token and username found, redirecting to Main")
                        _sideEffect.value = SplashSideEffect.NavigateToMain
                    }
                }
            } catch (e: Exception) {
                Log.e("Splash", "Error checking auth status", e)
                _sideEffect.value = SplashSideEffect.NavigationToOnboarding
            }
        }
    }
}
