package com.sunflower.pinpic.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunflower.pinpic.data.local.datastore.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val fullName: String = "",
    val email: String = "",
    val mobileNumber: String = "",
    val pictureUrl: String = "",
    val username: String = ""
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                dataStoreManager.fullNameFlow,
                dataStoreManager.emailFlow,
                dataStoreManager.mobileNumberFlow,
                dataStoreManager.pictureUrlFlow,
                dataStoreManager.usernameFlow
            ) { fullName, email, mobileNumber, pictureUrl, username ->
                ProfileUiState(
                    fullName = fullName ?: "Aisha Khan",
                    email = email ?: "wanderwithme@gmail.com",
                    mobileNumber = mobileNumber ?: "+91 98765 43210",
                    pictureUrl = pictureUrl ?: "",
                    username = username ?: "WanderWithMe"
                )
            }.collect { state ->
                _uiState.update { state }
            }
        }
    }

    fun updateProfile(fullName: String, email: String, mobileNumber: String, pictureUrl: String) {
        viewModelScope.launch {
            dataStoreManager.updateProfileInfo(
                fullName = fullName.takeIf { it.isNotBlank() },
                email = email.takeIf { it.isNotBlank() },
                mobileNumber = mobileNumber.takeIf { it.isNotBlank() },
                pictureUrl = pictureUrl.takeIf { it.isNotBlank() }
            )
        }
    }

    fun updateProfilePicture(uri: String) {
        viewModelScope.launch {
            dataStoreManager.updateProfileInfo(
                fullName = null,
                email = null,
                mobileNumber = null,
                pictureUrl = uri
            )
        }
    }
}
