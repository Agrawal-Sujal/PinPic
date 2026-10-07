package com.sunflower.pinpic.presentation.map


import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.sunflower.pinpic.common.ImageUtils
import com.sunflower.pinpic.common.Result
import com.sunflower.pinpic.data.local.datastore.DataStoreManager
import com.sunflower.pinpic.domain.map.BBox
import com.sunflower.pinpic.domain.map.ClusterNode
import com.sunflower.pinpic.domain.map.GeoClusterIndex
import com.sunflower.pinpic.domain.map.GeoPoint
import com.sunflower.pinpic.domain.model.ClusterPoint
import com.sunflower.pinpic.domain.model.Photo
import com.sunflower.pinpic.domain.repository.PhotosRepository
import com.sunflower.pinpic.domain.usecase.SavePhotosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val photosRepository: PhotosRepository,
    private val savePhotosUseCase: SavePhotosUseCase,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private var index: GeoClusterIndex? = null
    private val indexMutex = Mutex()
    private val pendingInserts = ArrayDeque<GeoPoint>()
    private val livePoints = mutableListOf<GeoPoint>()
    private val camera = MutableStateFlow<CameraState?>(null)
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState
    
    val themeModeFlow = dataStoreManager.themeModeFlow

    init {
        var initialBuildStarted = false

        viewModelScope.launch {
            combine(
                photosRepository.getUnclusteredPhotosFlow(),
                photosRepository.getAllClusterPointsFlow()
            ) { unclusteredResult, clustersResult ->
                if (clustersResult is Result.Success) {
                    val clusters = clustersResult.data
                    val updatedClusters = mutableListOf<ClusterPoint>()
                    for (cluster in clusters) {
                        if (cluster.thumbnailUri == null) {
                            val photosRes = photosRepository.getPhotosByClusterIds(listOf(cluster.id))
                            if (photosRes is Result.Success && photosRes.data.isNotEmpty()) {
                                val firstPhoto = photosRes.data.first()
                                val photoUri = firstPhoto.uri ?: firstPhoto.url
                                if (photoUri != null) {
                                    val thumbUri = ImageUtils.generateAndSaveThumbnail(context, photoUri)
                                    if (thumbUri != null) {
                                        val updated = cluster.copy(thumbnailUri = thumbUri)
                                        photosRepository.saveClusterPoint(updated)
                                        updatedClusters.add(updated)
                                        continue
                                    }
                                }
                            }
                        }
                        updatedClusters.add(cluster)
                    }
                    updatedClusters.map { it.toGeoPoint() }
                } else {
                    null
                }
            }.collect { newPointsList ->
                if (newPointsList == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Error loading map points"
                        )
                    }
                    return@collect
                }

                val newPoints = indexMutex.withLock {
                    val existingIds = livePoints.map { it.id }.toSet()
                    val pointsToAdd = newPointsList.filter { it.id !in existingIds }
                    livePoints.addAll(pointsToAdd)
                    pointsToAdd
                }

                if (newPoints.isNotEmpty()) {
                    indexMutex.withLock {
                        val idx = index
                        if (idx == null) {
                            pendingInserts.addAll(newPoints)
                        } else {
                            newPoints.forEach { idx.addPoint(it) }
                        }
                    }

                    if (!initialBuildStarted) {
                        initialBuildStarted = true
                        viewModelScope.launch {
                            val snapshot = indexMutex.withLock { pendingInserts.toList() }
                            val built = withContext(Dispatchers.Default) {
                                GeoClusterIndex.build(snapshot, minZoom = 0, maxZoom = 21, radiusPixels = 40.0)
                            }
                            indexMutex.withLock {
                                index = built
                                repeat(snapshot.size) { pendingInserts.removeFirst() }
                                while (pendingInserts.isNotEmpty()) {
                                    built.addPoint(pendingInserts.removeFirst())
                                }
                            }
                            _uiState.update { it.copy(isLoading = false) }
                            camera.value?.let { requery(it) }
                        }
                    } else if (index != null) {
                        camera.value?.let { requery(it) }
                    }
                } else if (!initialBuildStarted) {
                    initialBuildStarted = true
                    viewModelScope.launch {
                        val built = withContext(Dispatchers.Default) {
                            GeoClusterIndex.build(
                                emptyList(),
                                minZoom = 0,
                                maxZoom = 21,
                                radiusPixels = 40.0
                            )
                        }
                        indexMutex.withLock { index = built }
                        _uiState.update { it.copy(isLoading = false) }
                        camera.value?.let { requery(it) }
                    }
                } else {
                    if (index != null) {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
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

        viewModelScope.launch {
            while (true) {
                delay(REBUILD_INTERVAL_MS.milliseconds)
                rebuildIndexNow()
            }
        }
    }

    fun onCameraIdle(state: CameraState) {
        camera.value = state
    }

    private suspend fun rebuildIndexNow() {
        val snapshot = indexMutex.withLock { livePoints.toList() }
        if (snapshot.isEmpty()) return

        val rebuilt = withContext(Dispatchers.Default) {
            GeoClusterIndex.build(snapshot, minZoom = 0, maxZoom = 21, radiusPixels = 40.0)
        }
        indexMutex.withLock {
            if (index != null) {
                index = rebuilt
            }
        }
        camera.value?.let { requery(it) }
    }

    private suspend fun queryForLocked(state: CameraState): List<ClusterNode> {
        return indexMutex.withLock {
            index?.getClusters(state.bbox, state.zoom) ?: emptyList()
        }
    }

    fun childrenOf(clusterId: Long): List<ClusterNode> {
        return index?.getChildren(clusterId) ?: emptyList()
    }

    suspend fun getThumbnailForNode(clusterId: Long): String? {
        return indexMutex.withLock {
            val idx = index ?: return@withLock null
            val leaves = idx.getLeaves(clusterId)
            val firstPointId = leaves.firstOrNull()?.pointId ?: return@withLock null
            val clusterPoint = livePoints.find { it.id == firstPointId }?.payload as? ClusterPoint
            clusterPoint?.thumbnailUri
        }
    }

    suspend fun getPhotosForNode(clusterId: Long): List<Photo> {
        val (directPhotos, clusterPoints) = indexMutex.withLock {
            val idx = index ?: return@withLock Pair(emptyList<Photo>(), emptyList<ClusterPoint>())
            val leaves = idx.getLeaves(clusterId)
            val pointIds = leaves.mapNotNull { it.pointId }.toSet()

            val liveLeaves = livePoints.filter { it.id in pointIds }

            val direct = liveLeaves.mapNotNull { it.payload as? Photo }
            val clusters = liveLeaves.mapNotNull { it.payload as? ClusterPoint }
            Pair(direct, clusters)
        }

        val clusteredPhotos = if (clusterPoints.isNotEmpty()) {
            val clusterIds = clusterPoints.map { it.id }
            val result = photosRepository.getPhotosByClusterIds(clusterIds)
            if (result is Result.Success) result.data else emptyList()
        } else {
            emptyList()
        }

        return directPhotos + clusteredPhotos
    }

    suspend fun getPhotosAndClusterForNode(clusterId: Long): Pair<List<Photo>, ClusterPoint?> {
        val (directPhotos, clusterPoints) = indexMutex.withLock {
            val idx = index ?: return@withLock Pair(emptyList<Photo>(), emptyList<ClusterPoint>())
            val leaves = idx.getLeaves(clusterId)
            val pointIds = leaves.mapNotNull { it.pointId }.toSet()

            val liveLeaves = livePoints.filter { it.id in pointIds }

            val direct = liveLeaves.mapNotNull { it.payload as? Photo }
            val clusters = liveLeaves.mapNotNull { it.payload as? ClusterPoint }
            Pair(direct, clusters)
        }

        val clusteredPhotos = if (clusterPoints.isNotEmpty()) {
            val clusterIds = clusterPoints.map { it.id }
            val result = photosRepository.getPhotosByClusterIds(clusterIds)
            if (result is Result.Success) result.data else emptyList()
        } else {
            emptyList()
        }
        
        val clusterPoint = if (clusterPoints.size == 1) clusterPoints.first() else null
        return Pair(directPhotos + clusteredPhotos, clusterPoint)
    }

    fun updateClusterPoint(clusterPoint: ClusterPoint) {
        viewModelScope.launch {
            photosRepository.saveClusterPoint(clusterPoint)
        }
    }

    fun deletePhotos(photos: List<Photo>) {
        viewModelScope.launch {
            photosRepository.deletePhotos(photos)
        }
    }

    fun importPhotos(context: Context, uris: List<Uri>, latLng: LatLng, dateStr: String?, timeStr: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val resolver = context.contentResolver
            val defaultTimeFormatter = SimpleDateFormat("HH:mm:ss", Locale.US)
            val defaultDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val filePrefixFormatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            val now = Date()
            
            val finalDateStr = dateStr ?: defaultDateFormatter.format(now)
            val finalTimeStr = timeStr ?: defaultTimeFormatter.format(now)

            val cityName = "Unknown Location"

            var createdClusterId: Long? = null
            val cluster = ClusterPoint(
                lat = latLng.latitude,
                lng = latLng.longitude,
                count = uris.size
            )
            val res = photosRepository.saveClusterPoint(cluster)
            if (res is Result.Success) {
                createdClusterId = res.data
            }

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

                    val photo = Photo(
                        lat = latLng.latitude,
                        lng = latLng.longitude,
                        date = finalDateStr,
                        time = finalTimeStr,
                        locationName = cityName,
                        uri = destFile.absolutePath,
                        url = null,
                        clusterId = createdClusterId
                    )
                    photosRepository.savePhoto(photo)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun requery(state: CameraState) {
        viewModelScope.launch(Dispatchers.Default) {
            val newNodes = queryForLocked(state)
            _uiState.update { it.copy(visibleNodes = newNodes) }
        }
    }

    private fun Photo.toGeoPoint(): GeoPoint? {
        if (lat == null || lng == null) return null
        return GeoPoint(
            id = "p_$id",
            lat = lat,
            lon = lng,
            payload = this,
            thumbnailUri = uri
        )
    }

    private fun ClusterPoint.toGeoPoint(): GeoPoint {
        return GeoPoint(
            id = "c_$id",
            lat = lat,
            lon = lng,
            count = count,
            payload = this,
            thumbnailUri = thumbnailUri
        )
    }

    fun generateDummyPoints(count: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isGeneratingDummies = true) }
            val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.US)
            val now = Date()
            val dateStr = "2026-01-01"

            for (i in 0 until count) {
                val lat = kotlin.random.Random.nextDouble(-90.0, 90.0)
                val lng = kotlin.random.Random.nextDouble(-180.0, 180.0)

                val cluster = ClusterPoint(
                    lat = lat,
                    lng = lng,
                    count = 1,
                    thumbnailUri = "https://picsum.photos/200"
                )
                val res = photosRepository.saveClusterPoint(cluster)
                if (res is Result.Success) {
                    val photo = Photo(
                        lat = lat,
                        lng = lng,
                        date = dateStr,
                        time = timeFormatter.format(now),
                        locationName = "Dummy Location $i",
                        uri = null,
                        url = "https://picsum.photos/200",
                        clusterId = res.data
                    )
                    photosRepository.savePhoto(photo)
                }
            }
            _uiState.update { it.copy(isGeneratingDummies = false) }
        }
    }
}
