package com.sunflower.pinpic.presentation.camera

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.sunflower.pinpic.domain.model.Photo
import com.sunflower.pinpic.domain.usecase.SavePhotosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val savePhotosUseCase: SavePhotosUseCase,
    private val fusedLocationClient: FusedLocationProviderClient
) : ViewModel() {

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { _currentLocation.value = it }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            request, locationCallback!!, Looper.getMainLooper()
        )

        // Prime it immediately too, don't wait for the first update
        fusedLocationClient.lastLocation.addOnSuccessListener {
            if (_currentLocation.value == null) _currentLocation.value = it
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
    }

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _latestPhotoUri = MutableStateFlow<Any?>(null)
    val latestPhotoUri: StateFlow<Any?> = _latestPhotoUri.asStateFlow()

    fun startSaving(): Boolean {
        return _isSaving.compareAndSet(expect = false, update = true)
    }
    
    fun stopSaving() {
        _isSaving.value = false
    }
    

    fun onPhotoCaptured(uri: String) {
        Log.d("CameraViewModel","Photo clicked")
        _latestPhotoUri.value = uri
        val loc = _currentLocation.value
        val now = Date()
        val photo = Photo(
            lat = loc?.latitude ?: 0.0,
            lng = loc?.longitude ?: 0.0,
            date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now),
            time = SimpleDateFormat("HH:mm:ss", Locale.US).format(now),
            locationName = null,
            uri = uri,
            url = null
        )

        viewModelScope.launch {
            try {
                savePhotosUseCase(listOf(photo), photo.lat ?: 0.0, photo.lng ?: 0.0)
            } finally {
                _isSaving.value = false
            }
        }
    }

    @SuppressLint("EmptySuperCall")
    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}