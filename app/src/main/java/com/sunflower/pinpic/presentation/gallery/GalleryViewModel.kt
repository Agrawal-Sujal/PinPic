package com.sunflower.pinpic.presentation.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunflower.pinpic.domain.model.Photo
import com.sunflower.pinpic.domain.repository.PhotosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.sunflower.pinpic.common.Result

data class GalleryUiState(
    val photosGroupedByDate: Map<String, List<Photo>> = emptyMap(),
    val photosGroupedByLocation: Map<String, List<Photo>> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val expandedDates: Set<String> = emptySet(),
    val expandedLocations: Set<String> = emptySet()
)

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val photosRepository: PhotosRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        loadPhotos()
    }

    fun loadPhotos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            photosRepository.getAllPhotosFlow().collect { result ->
                when (result) {
                    is Result.Success -> {
                        val groupedByDate = result.data.groupBy { it.date ?: "Unknown Date" }
                        val groupedByLocation = result.data.groupBy { it.locationName ?: "Unknown Location" }
                        _uiState.value = _uiState.value.copy(
                            photosGroupedByDate = groupedByDate,
                            photosGroupedByLocation = groupedByLocation,
                            isLoading = false
                        )
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = result.message,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun toggleDateExpansion(dateStr: String) {
        val currentExpanded = _uiState.value.expandedDates
        val newExpanded = if (currentExpanded.contains(dateStr)) {
            currentExpanded - dateStr
        } else {
            currentExpanded + dateStr
        }
        _uiState.value = _uiState.value.copy(expandedDates = newExpanded)
    }

    fun toggleLocationExpansion(locationStr: String) {
        val currentExpanded = _uiState.value.expandedLocations
        val newExpanded = if (currentExpanded.contains(locationStr)) {
            currentExpanded - locationStr
        } else {
            currentExpanded + locationStr
        }
        _uiState.value = _uiState.value.copy(expandedLocations = newExpanded)
    }

    fun deletePhotos(photos: List<Photo>) {
        viewModelScope.launch {
            photosRepository.deletePhotos(photos)
        }
    }
}
