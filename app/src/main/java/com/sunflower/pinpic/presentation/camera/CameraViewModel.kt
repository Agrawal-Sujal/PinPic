package com.sunflower.pinpic.presentation.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunflower.pinpic.domain.model.Photo
import com.sunflower.pinpic.domain.usecase.SavePhotoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val savePhotoUseCase: SavePhotoUseCase
) : ViewModel() {

    fun onPhotoCaptured(uri: String, lat: Double, lng: Double) {
        val now = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

        val photo = Photo(
            lat = lat,
            lng = lng,
            date = dateFormat.format(now),
            time = timeFormat.format(now),
            locationName = null,
            uri = uri,
            url = null
        )

        viewModelScope.launch {
            savePhotoUseCase(photo)
        }
    }
}