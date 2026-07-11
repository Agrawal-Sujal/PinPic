package com.sunflower.pinpic.presentation.auth.signup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunflower.pinpic.presentation.auth.AuthSideEffect
import com.sunflower.pinpic.presentation.auth.AuthUiEvent
import com.sunflower.pinpic.presentation.auth.AuthUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.sunflower.pinpic.domain.usecase.RegisterUseCase
import com.sunflower.pinpic.domain.usecase.GoogleSignInUseCase
import com.sunflower.pinpic.presentation.common.SnackbarController
import com.sunflower.pinpic.common.Result
import com.sunflower.pinpic.presentation.auth.GoogleAuthManager
import com.sunflower.pinpic.data.local.datastore.DataStoreManager
import kotlinx.coroutines.flow.firstOrNull

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase,
    private val googleSignInUseCase: GoogleSignInUseCase,
    private val googleAuthManager: GoogleAuthManager,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = Channel<AuthSideEffect>()
    val sideEffect = _sideEffect.receiveAsFlow()

    fun onEvent(event: AuthUiEvent) {
        when (event) {
            is AuthUiEvent.OnEmailChanged -> {
                _uiState.update { it.copy(emailText = event.email) }
            }
            is AuthUiEvent.OnPasswordChanged -> {
                _uiState.update { it.copy(passwordText = event.password) }
            }
            is AuthUiEvent.OnConfirmPasswordChanged -> {
                _uiState.update { it.copy(confirmPasswordText = event.password) }
            }
            AuthUiEvent.OnTogglePasswordVisibility -> {
                _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }
            AuthUiEvent.OnToggleConfirmPasswordVisibility -> {
                _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
            }
            AuthUiEvent.OnSignUpClicked -> {
                performSignUp()
            }
            is AuthUiEvent.OnGoogleAuthClicked -> {
                performGoogleAuth(event.context)
            }
            AuthUiEvent.OnLoginClicked -> {
                viewModelScope.launch { _sideEffect.send(AuthSideEffect.NavigateToLogin) }
            }
            AuthUiEvent.OnClearError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
            else -> Unit
        }
    }

    private fun performSignUp() {
        if (_uiState.value.passwordText != _uiState.value.confirmPasswordText) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match") }
            return
        }

        val email = _uiState.value.emailText
        val password = _uiState.value.passwordText

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = registerUseCase(email, password)
            _uiState.update { it.copy(isLoading = false) }
            when (result) {
                is Result.Success -> _sideEffect.send(AuthSideEffect.NavigateToLogin)
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                    SnackbarController.sendEvent(result.message)
                }
            }
        }
    }

    private fun performGoogleAuth(context: Context) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val token = googleAuthManager.getGoogleIdToken(context)
            if (token != null) {
                val result = googleSignInUseCase(token)
                _uiState.update { it.copy(isLoading = false) }
                when (result) {
                    is Result.Success -> {
                        val username = dataStoreManager.usernameFlow.firstOrNull()
                        if (username.isNullOrEmpty()) {
                            _sideEffect.send(AuthSideEffect.NavigateToUsernameSetup)
                        } else {
                            _sideEffect.send(AuthSideEffect.NavigateToHome)
                        }
                    }
                    is Result.Error -> {
                        _uiState.update { it.copy(errorMessage = result.message) }
                        SnackbarController.sendEvent(result.message)
                    }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}