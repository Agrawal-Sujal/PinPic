package com.sunflower.pinpic.presentation.auth.username

sealed interface UsernameUiEvent {
    data class OnUsernameChanged(val username: String) : UsernameUiEvent
    object OnSubmitClicked : UsernameUiEvent
    object OnChangeAccountClicked : UsernameUiEvent
    object OnClearError : UsernameUiEvent
}
