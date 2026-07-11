package com.sunflower.pinpic.presentation.splash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SplashViewModel : ViewModel() {

    private val _sideEffect = Channel<SplashSideEffect>()
    val sideEffect = _sideEffect.receiveAsFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
//            delay(15000L.milliseconds)

            Log.d("TAG","viewmodel emitted Navigation to Login")
            
            // TODO: Replace with real auth state check.
            _sideEffect.send(SplashSideEffect.NavigationToOnboarding)
        }
    }
}
