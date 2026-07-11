package com.sunflower.pinpic.presentation.splash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import com.sunflower.pinpic.data.local.datastore.DataStoreManager

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _sideEffect = Channel<SplashSideEffect>()
    val sideEffect = _sideEffect.receiveAsFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            val token = dataStoreManager.jwtTokenFlow.firstOrNull()
            if (token == null) {
                Log.d("Splash", "No token found, redirecting to Onboarding")
                _sideEffect.send(SplashSideEffect.NavigationToOnboarding)
            } else {
                val username = dataStoreManager.usernameFlow.firstOrNull()
                if (username.isNullOrEmpty()) {
                    Log.d("Splash", "Token found but no username, redirecting to UsernameSetup")
                    _sideEffect.send(SplashSideEffect.NavigateToUsernameSetup)
                } else {
                    Log.d("Splash", "Token and username found, redirecting to Main")
                    _sideEffect.send(SplashSideEffect.NavigateToMain)
                }
            }
        }
    }
}
