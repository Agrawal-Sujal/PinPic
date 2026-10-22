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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.sunflower.pinpic.data.remote.api.UserApi

data class ProfileUiState(
    val fullName: String = "",
    val email: String = "",
    val mobileNumber: String = "",
    val pictureUrl: String = "",
    val username: String = "",
    val followerCount: Int = 0,
    val followingCount: Int = 0
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val userApi: UserApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    private val _networkStats = MutableStateFlow(Pair(0, 0))

    init {
        viewModelScope.launch {
            combine(
                combine(
                    dataStoreManager.fullNameFlow,
                    dataStoreManager.emailFlow,
                    dataStoreManager.mobileNumberFlow
                ) { f, e, m -> Triple(f, e, m) },
                dataStoreManager.pictureUrlFlow,
                dataStoreManager.usernameFlow,
                _networkStats
            ) { triple, pictureUrl, username, stats ->
                val (fullName, email, mobileNumber) = triple
                ProfileUiState(
                    fullName = fullName ?: "Name",
                    email = email ?: "email@example.com",
                    mobileNumber = mobileNumber ?: "",
                    pictureUrl = pictureUrl ?: "",
                    username = username ?: "username",
                    followerCount = stats.first,
                    followingCount = stats.second
                )
            }.collect { state ->
                _uiState.update { state }
            }
        }
        fetchProfile()
    }

    private fun fetchProfile() {
        viewModelScope.launch {
            try {
                val token = dataStoreManager.jwtTokenFlow.first()
                if (token != null) {
                    val response = userApi.getProfile("Bearer $token")
                    if (response.isSuccessful) {
                        response.body()?.let { profile ->
                            dataStoreManager.updateProfileInfo(
                                fullName = profile.name ?: fullNameFlowValue(),
                                email = profile.email,
                                mobileNumber = profile.mobileNumber ?: mobileNumberFlowValue(),
                                pictureUrl = profile.pictureUrl ?: pictureUrlFlowValue()
                            )
                            _networkStats.value = Pair(profile.followerCount, profile.followingCount)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun fullNameFlowValue() = dataStoreManager.fullNameFlow.first()
    private suspend fun mobileNumberFlowValue() = dataStoreManager.mobileNumberFlow.first()
    private suspend fun pictureUrlFlowValue() = dataStoreManager.pictureUrlFlow.first()

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
