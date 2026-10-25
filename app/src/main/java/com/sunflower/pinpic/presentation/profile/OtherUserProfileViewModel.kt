package com.sunflower.pinpic.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunflower.pinpic.common.Result
import com.sunflower.pinpic.data.remote.dto.UserPhotoResponse
import com.sunflower.pinpic.domain.repository.UserRepository
import com.sunflower.pinpic.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OtherUserProfileUiState(
    val userId: String = "",
    val name: String = "",
    val username: String = "",
    val pictureUrl: String = "",
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val isFollowing: Boolean = false,
    val isLoading: Boolean = false,
    val isFollowActionInProgress: Boolean = false,
    val photos: List<UserPhotoResponse> = emptyList(),
    val isPhotosLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class OtherUserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OtherUserProfileUiState())
    val uiState: StateFlow<OtherUserProfileUiState> = _uiState.asStateFlow()

    fun initProfile(profile: Screen.OtherUserProfile) {
        if (_uiState.value.userId != profile.userId) {
            _uiState.update {
                it.copy(
                    userId = profile.userId,
                    name = profile.name ?: profile.username,
                    username = profile.username,
                    pictureUrl = profile.pictureUrl ?: "",
                    followerCount = profile.followerCount,
                    followingCount = profile.followingCount,
                    isFollowing = profile.isFollowing,
                    photos = emptyList()
                )
            }
            refreshProfile(profile.userId)
            loadPhotos(profile.userId)
        }
    }

    fun refreshProfile(userId: String) {
        viewModelScope.launch {
            when (val result = userRepository.getUserProfile(userId)) {
                is Result.Success -> {
                    val user = result.data
                    _uiState.update {
                        it.copy(
                            userId = user.id,
                            name = user.name ?: user.username,
                            username = user.username,
                            pictureUrl = user.pictureUrl ?: "",
                            followerCount = user.followerCount,
                            followingCount = user.followingCount,
                            isFollowing = user.isFollowing
                        )
                    }
                }
                is Result.Error -> {
                    // Keep existing values
                }
            }
        }
    }

    fun loadPhotos(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPhotosLoading = true) }
            when (val result = userRepository.getUserPhotos(userId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            photos = result.data,
                            isPhotosLoading = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isPhotosLoading = false) }
                }
            }
        }
    }

    fun toggleFollow() {
        val currentState = _uiState.value
        if (currentState.userId.isEmpty() || currentState.isFollowActionInProgress) return

        val currentlyFollowing = currentState.isFollowing
        val previousFollowerCount = currentState.followerCount
        val newFollowing = !currentlyFollowing
        val newFollowerCount = if (newFollowing) previousFollowerCount + 1 else maxOf(0, previousFollowerCount - 1)

        // Optimistic update
        _uiState.update {
            it.copy(
                isFollowing = newFollowing,
                followerCount = newFollowerCount,
                isFollowActionInProgress = true
            )
        }

        viewModelScope.launch {
            val result = if (currentlyFollowing) {
                userRepository.unfollowUser(currentState.userId)
            } else {
                userRepository.followUser(currentState.userId)
            }

            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isFollowActionInProgress = false) }
                    // Reload photos as friendship status affects visible photos (FRIENDS vs PUBLIC)
                    loadPhotos(currentState.userId)
                }
                is Result.Error -> {
                    // Revert optimistic update
                    _uiState.update {
                        it.copy(
                            isFollowing = currentlyFollowedState(currentlyFollowing),
                            followerCount = previousFollowerCount,
                            isFollowActionInProgress = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    private fun currentlyFollowedState(following: Boolean) = following
}
