package com.sunflower.pinpic.presentation.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunflower.pinpic.common.Result
import com.sunflower.pinpic.data.local.datastore.DataStoreManager
import com.sunflower.pinpic.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class EditProfileUiState(
    val fullName: String = "",
    val mobileNumber: String = "",
    val locale: String = "",
    val pictureUrl: String = "",
    val selectedImageUri: Uri? = null,
    val selectedImageName: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val fullName = dataStoreManager.fullNameFlow.first() ?: ""
            val mobileNumber = dataStoreManager.mobileNumberFlow.first() ?: ""
            val pictureUrl = dataStoreManager.pictureUrlFlow.first() ?: ""
            // Assume locale is not in datastore yet or we just leave it empty
            _uiState.update {
                it.copy(
                    fullName = fullName,
                    mobileNumber = mobileNumber,
                    pictureUrl = pictureUrl
                )
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(fullName = name) }
    }

    fun onMobileNumberChange(mobile: String) {
        _uiState.update { it.copy(mobileNumber = mobile) }
    }

    fun onLocaleChange(locale: String) {
        _uiState.update { it.copy(locale = locale) }
    }

    fun onPictureSelected(uri: Uri, name: String) {
        _uiState.update { it.copy(selectedImageUri = uri, selectedImageName = name) }
    }

    fun updateProfile(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            var imageBytes: ByteArray? = null
            var imageName: String? = null
            
            val selectedUri = _uiState.value.selectedImageUri
            if (selectedUri != null) {
                try {
                    val inputStream = context.contentResolver.openInputStream(selectedUri)
                    if (inputStream != null) {
                        imageBytes = withContext(Dispatchers.IO) { inputStream.readBytes() }
                        imageName = _uiState.value.selectedImageName ?: "photo.jpg"
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, error = "Could not read image file") }
                    return@launch
                }
            }

            val result = userRepository.updateProfile(
                fullName = _uiState.value.fullName,
                mobileNumber = _uiState.value.mobileNumber,
                locale = _uiState.value.locale,
                pictureUrl = _uiState.value.pictureUrl,
                imageBytes = imageBytes,
                imageName = imageName
            )
            
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                    onSuccess()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }
}
