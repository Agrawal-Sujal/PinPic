package com.sunflower.pinpic.presentation.map


import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.sunflower.pinpic.data.local.datastore.DataStoreManager
import com.sunflower.pinpic.data.repository.LocalPhotoRepository
import com.sunflower.pinpic.domain.map.BBox
import com.sunflower.pinpic.domain.map.ClusterNode
import com.sunflower.pinpic.domain.map.MapClusteringSystem
import com.sunflower.pinpic.domain.model.Cluster
import com.sunflower.pinpic.domain.model.Location
import com.sunflower.pinpic.domain.model.Photo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private const val REBUILD_INTERVAL_MS = 5_000L

data class CameraState(val bbox: BBox, val zoom: Int)

data class MapUiState(
    val isLoading: Boolean = true,
    val visibleNodes: List<ClusterNode> = emptyList(),
    val errorMessage: String? = null,
    var newNodes: Int = 0,
    val isGeneratingDummies: Boolean = false
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class MapViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localPhotoRepository: LocalPhotoRepository,
    private val dataStoreManager: DataStoreManager,
    private val mapClusteringSystem: MapClusteringSystem
) : ViewModel() {

    private val camera = MutableStateFlow<CameraState?>(null)
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState

    val themeModeFlow = dataStoreManager.themeModeFlow

    init {

        viewModelScope.launch {

            mapClusteringSystem.build()
            _uiState.update { it.copy(isLoading = false) }
        }

        viewModelScope.launch {
            camera
                .filterNotNull()
                .debounce(10.milliseconds)
                .distinctUntilChanged()
                .mapLatest { state -> withContext(Dispatchers.Default) { queryForLocked(state) } }
                .collect { newNodes ->
                    _uiState.update { it.copy(visibleNodes = newNodes) }
                }
        }
    }

    fun onCameraIdle(state: CameraState) {
        camera.value = state
    }


    private fun queryForLocked(state: CameraState): List<ClusterNode> {
        val clusterNodes = mapClusteringSystem.getClusters(state.bbox, state.zoom)
        Log.d("MapViewModel", clusterNodes.toString())
        return clusterNodes
    }

    suspend fun getPhotosForNode(clusterId: Long): List<Photo> {
        return localPhotoRepository.getPhotosOfCluster(clusterId.toInt())
    }


    fun updateCluster(cluster: Cluster) {
        viewModelScope.launch {
            localPhotoRepository.saveCluster(cluster)
        }
    }

    fun deletePhotos(photos: List<Photo>) {
        viewModelScope.launch {
            localPhotoRepository.deletePhotos(photos)
            mapClusteringSystem.rebuild()
            camera.value?.let { state ->
                val newNodes = queryForLocked(state)
                _uiState.update { it.copy(visibleNodes = newNodes) }
            }
        }
    }

    fun importPhotos(
        context: Context,
        uris: List<Uri>,
        latLng: LatLng,
        dateStr: String?,
        timeStr: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val resolver = context.contentResolver
            val defaultTimeFormatter = SimpleDateFormat("HH:mm:ss", Locale.US)
            val defaultDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val filePrefixFormatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            val now = Date()

            val finalDateStr = dateStr ?: defaultDateFormatter.format(now)
            val finalTimeStr = timeStr ?: defaultTimeFormatter.format(now)

            // 1. Create a cluster first
            val emptyCluster = Cluster(
                location = Location(
                    latitude = latLng.latitude,
                    longitude = latLng.longitude,
                    locationName = "Imported Cluster"
                )
            )
            val clusterId = localPhotoRepository.saveCluster(emptyCluster)

            val savedPhotos = mutableListOf<Photo>()
            var sumLat = 0.0
            var sumLng = 0.0

            val userId = dataStoreManager.userIdFlow.first()

            // 2. Iterate through URIs and add photos
            uris.forEach { uri ->
                try {
                    val inputStream = resolver.openInputStream(uri) ?: return@forEach
                    val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

                    val fileName = "PinPic-Import-${filePrefixFormatter.format(Date())}.jpg"
                    val destFile = File(picturesDir, fileName)

                    FileOutputStream(destFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    inputStream.close()

                    val location = Location(
                        latitude = latLng.latitude,
                        longitude = latLng.longitude,
                        locationName = "Unknown Location",
                        isEdited = false
                    )

                    val photo = Photo(
                        userId = userId ?: "unknown",
                        photoName = fileName,
                        takenAt = finalDateStr,
                        editedAt = finalDateStr,
                        location = location,
                        photoUrl = destFile.absolutePath,
                    )

                    val photoId = localPhotoRepository.savePhoto(photo)
                    val savedPhoto = photo.copy(localPhotoId = photoId)
                    savedPhotos.add(savedPhoto)

                    sumLat += latLng.latitude
                    sumLng += latLng.longitude
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 3. Update the cluster to include all photos and average lat/lng
            if (savedPhotos.isNotEmpty()) {
                val avgLat = sumLat / savedPhotos.size
                val avgLng = sumLng / savedPhotos.size

                val updatedCluster = emptyCluster.copy(
                    localClusterId = clusterId,
                    location = Location(
                        latitude = avgLat,
                        longitude = avgLng,
                        locationName = "Imported Cluster"
                    ),
                    photos = savedPhotos,
                    thumbnail = savedPhotos.first(),
                    photoCount = savedPhotos.size
                )

                localPhotoRepository.saveCluster(updatedCluster)
                localPhotoRepository.addPhotosInCluster(savedPhotos, updatedCluster)
                mapClusteringSystem.addClusters(listOf(updatedCluster))

                camera.value?.let { state ->
                    val newNodes = queryForLocked(state)
                    _uiState.update { it.copy(visibleNodes = newNodes) }
                }
            }
        }
    }


    fun generateDummyPoints(count: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isGeneratingDummies = true) }
            val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.US)
            val now = Date()
            val dateStr = "2026-01-01"

            val newClusters = mutableListOf<Cluster>()

            for (i in 0 until count) {
                val lat = kotlin.random.Random.nextDouble(-90.0, 90.0)
                val lng = kotlin.random.Random.nextDouble(-180.0, 180.0)

                val location = Location(
                    latitude = lat,
                    longitude = lng,
                    locationName = "Dummy Location $i"
                )
                val photo = Photo(
                    userId = "dummy_user",
                    photoName = "dummy_$i.jpg",
                    takenAt = dateStr,
                    editedAt = dateStr,
                    location = location,
                    photoUrl = "https://picsum.photos/200"
                )

                val photoId = localPhotoRepository.savePhoto(photo)
                val savedPhoto = photo.copy(localPhotoId = photoId)

                val emptyCluster = Cluster(
                    location = location,
                    photoCount = 1,
                    thumbnail = savedPhoto,
                    photos = listOf(savedPhoto)
                )

                val clusterId = localPhotoRepository.saveCluster(emptyCluster)
                val finalCluster = emptyCluster.copy(localClusterId = clusterId)

                localPhotoRepository.addPhotosInCluster(listOf(savedPhoto), finalCluster)
                newClusters.add(finalCluster)
            }

            mapClusteringSystem.addClusters(newClusters)
            camera.value?.let { state ->
                val newNodes = queryForLocked(state)
                _uiState.update { it.copy(visibleNodes = newNodes) }
            }
            _uiState.update { it.copy(isGeneratingDummies = false) }
        }
    }
}
