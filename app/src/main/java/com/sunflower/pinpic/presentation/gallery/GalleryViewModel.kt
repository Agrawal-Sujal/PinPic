package com.sunflower.pinpic.presentation.gallery

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunflower.pinpic.common.Result
import com.sunflower.pinpic.domain.map.MapClusteringSystem
import com.sunflower.pinpic.domain.model.Photo
import com.sunflower.pinpic.domain.repository.PhotoRepository
import com.sunflower.pinpic.domain.usecase.ImportPhotosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class GalleryUiState(
    val photosGroupedByDate: Map<String, List<Photo>> = emptyMap(),
    val photosGroupedByLocation: Map<String, List<Photo>> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val expandedDates: Set<String> = emptySet(),
    val expandedLocations: Set<String> = emptySet(),
    val isImporting: Boolean = false,
    val importProgress: Float = 0f,
    val importCurrent: Int = 0,
    val importTotal: Int = 0
)

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val importPhotosUseCase: ImportPhotosUseCase,
    private val mapClusteringSystem: MapClusteringSystem
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        loadPhotos()
    }

    fun loadPhotos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            photoRepository.getAllPhotosFlow().collect { result ->
                when (result) {
                    is Result.Success -> {
                        val groupedByDate = result.data.groupBy { photo ->
                            val rawDate = photo.takenAt ?: "Unknown Date"
                            try {
                                val parserLong = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.US)
                                val parsed = parserLong.parse(rawDate)
                                if (parsed != null) {
                                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(parsed)
                                } else {
                                    rawDate
                                }
                            } catch(e: Exception) {
                                rawDate
                            }
                        }
                        val groupedByLocation = result.data.groupBy { it.location?.locationName ?: "Unknown Location" }
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
            photoRepository.deletePhotos(photos)
            mapClusteringSystem.rebuild()
        }
    }

    fun importPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isImporting = true,
                    importProgress = 0f,
                    importCurrent = 0,
                    importTotal = uris.size
                )
            }

            importPhotosUseCase(uris) { current, total ->
                _uiState.update {
                    it.copy(
                        importCurrent = current,
                        importTotal = total,
                        importProgress = if (total > 0) current.toFloat() / total.toFloat() else 0f
                    )
                }
            }

            delay(400.milliseconds)

            _uiState.update {
                it.copy(
                    isImporting = false,
                    importProgress = 0f,
                    importCurrent = 0,
                    importTotal = 0
                )
            }
        }
    }
}
