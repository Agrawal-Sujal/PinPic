package com.sunflower.pinpic.presentation.auth.username

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunflower.pinpic.common.Result
import com.sunflower.pinpic.domain.usecase.SetUsernameUseCase
import com.sunflower.pinpic.presentation.common.SnackbarController
import com.sunflower.pinpic.data.local.datastore.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UsernameViewModel @Inject constructor(
    private val setUsernameUseCase: SetUsernameUseCase,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(UsernameUiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = Channel<UsernameSideEffect>()
    val sideEffect = _sideEffect.receiveAsFlow()

    init {
        viewModelScope.launch {
            dataStoreManager.emailFlow.collect { email ->
                _uiState.update { it.copy(email = email) }
            }
        }
    }

    fun onEvent(event: UsernameUiEvent) {
        when (event) {
            is UsernameUiEvent.OnUsernameChanged -> {
                _uiState.update { it.copy(usernameText = event.username) }
            }
            UsernameUiEvent.OnSubmitClicked -> {
                performSubmit()
            }
            UsernameUiEvent.OnChangeAccountClicked -> {
                performChangeAccount()
            }
            UsernameUiEvent.OnClearError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    private fun performChangeAccount() {
        viewModelScope.launch {
            dataStoreManager.clearAuthInfo()
            _sideEffect.send(UsernameSideEffect.NavigateToLogin)
        }
    }

    private fun performSubmit() {
        val username = _uiState.value.usernameText
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        
        viewModelScope.launch {
            val result = setUsernameUseCase(username)
            _uiState.update { it.copy(isLoading = false) }
            
            when (result) {
                is Result.Success -> {
                    _sideEffect.send(UsernameSideEffect.NavigateToHome)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                    SnackbarController.sendEvent(result.message)
                }
            }
        }
    }
}
