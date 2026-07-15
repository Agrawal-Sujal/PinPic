package com.sunflower.pinpic.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunflower.pinpic.common.Result
import com.sunflower.pinpic.domain.map.BBox
import com.sunflower.pinpic.domain.map.ClusterNode
import com.sunflower.pinpic.domain.map.GeoClusterIndex
import com.sunflower.pinpic.domain.map.GeoPoint
import com.sunflower.pinpic.domain.model.Photo
import com.sunflower.pinpic.domain.repository.PhotosRepository
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.google.android.gms.maps.model.LatLng
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/** How often the index gets fully recomputed from scratch — see the rebuild loop in init{}. */
private const val REBUILD_INTERVAL_MS = 5_000L

data class CameraState(val bbox: BBox, val zoom: Int)

data class MapUiState(
    val isLoading: Boolean = true,
    val visibleNodes: List<ClusterNode> = emptyList(),
    val errorMessage: String? = null,
    var newNodes: Int = 0
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class MapViewModel @Inject constructor(
    private val photosRepository: PhotosRepository
) : ViewModel() {

    private var index: GeoClusterIndex? = null

    // KDTree.insert() mutates the tree in place, so both the initial build and every later
    // addPoint() go through this lock to keep them from touching the tree concurrently.
    private val indexMutex = Mutex()

    // Points that arrive via addPoint() before the initial build finishes get queued here, then
    // flushed into the fresh index once it exists.
    private val pendingInserts = ArrayDeque<GeoPoint>()

    // Every point the index has ever seen — the original batch plus everything added later via
    // addPoint(). This is what rebuildIndexNow() rebuilds from, since GeoClusterIndex.build()
    // needs the full point set, not just what changed since the last build.
    private val livePoints = mutableListOf<GeoPoint>()

    private val camera = MutableStateFlow<CameraState?>(null)

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState

    init {
        var initialBuildStarted = false

        viewModelScope.launch {
            photosRepository.getAllPhotosFlow().collect { result ->
                when (result) {
                    is Result.Success -> {
                        val newPhotos = result.data
                        
                        val newPoints = indexMutex.withLock {
                            val existingIds = livePoints.map { it.id }.toSet()
                            val pointsToAdd = newPhotos.filter { it.id.toString() !in existingIds }.mapNotNull { it.toGeoPoint() }
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
                                        GeoClusterIndex.build(snapshot, minZoom = 0, maxZoom = 16, radiusPixels = 40.0)
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
                             // Zero photos from DB initially
                             initialBuildStarted = true
                             viewModelScope.launch {
                                 val built = withContext(Dispatchers.Default) {
                                        GeoClusterIndex.build(emptyList(), minZoom = 0, maxZoom = 16, radiusPixels = 40.0)
                                 }
                                 indexMutex.withLock { index = built }
                                 _uiState.update { it.copy(isLoading = false) }
                                 camera.value?.let { requery(it) }
                             }
                        } else {
                            // no new points, but we've already started building
                            if (index != null) {
                                _uiState.update { it.copy(isLoading = false) }
                            }
                        }
                    }
                    is Result.Error -> {
                        _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                    }
                }
            }
        }

        viewModelScope.launch {
            camera
                .filterNotNull()
                .debounce(10.milliseconds) // swallow intermediate frames during a pan/zoom gesture
                .distinctUntilChanged()
                .mapLatest { state -> withContext(Dispatchers.Default) { queryForLocked(state) } }
                .collect { newNodes ->
                    _uiState.update { it.copy(visibleNodes = newNodes) }
                }
        }

        // Periodic full rebuild: addPoint()/KDTree.insert() is cheap but leaves every new point
        // as its own unmerged marker (see GeoClusterIndex.addPoint's doc). Every few seconds this
        // recomputes the real clustering from every point seen so far, folding those loose points
        // back into proper clusters — the same "insert cheaply, rebuild periodically" trade-off
        // discussed for KDTree.insert() itself.
        viewModelScope.launch {
            while (true) {
                delay(REBUILD_INTERVAL_MS.milliseconds)
                rebuildIndexNow()
            }
        }
    }

    /** Call this from your camera-idle listener (MapLibre's onCameraIdle / GMaps' setOnCameraIdleListener). */
    fun onCameraIdle(state: CameraState) {
        camera.value = state
    }

    /**
     * Recomputes the clustering from scratch using every point seen so far ([livePoints]) and
     * atomically swaps it in for the live index. The expensive [GeoClusterIndex.build] call itself
     * runs outside the lock (on [Dispatchers.Default]) so it doesn't block concurrent inserts or
     * queries while it's crunching — only the final pointer swap is guarded.
     */
    private suspend fun rebuildIndexNow() {
        val snapshot = indexMutex.withLock { livePoints.toList() }
        if (snapshot.isEmpty()) return
        
        val rebuilt = withContext(Dispatchers.Default) {
            GeoClusterIndex.build(snapshot, minZoom = 0, maxZoom = 16, radiusPixels = 40.0)
        }
        indexMutex.withLock { 
            if (index != null) { // Only swap if the initial build finished
                index = rebuilt 
            }
        }
        camera.value?.let { requery(it) }
    }

    // Suspend + lock-guarded because a query walks the same KD-tree nodes that addPoint()'s
    // insert() mutates — without the lock a query could read a half-linked node mid-insert.
    private suspend fun queryForLocked(state: CameraState): List<ClusterNode> {
        return indexMutex.withLock {
            index?.getClusters(state.bbox, state.zoom) ?: emptyList()
        }
    }

    /** Tap handler for a cluster bubble: zoom in, or fetch its immediate children to fan out. */
    fun childrenOf(clusterId: Long): List<ClusterNode> {
        return index?.getChildren(clusterId) ?: emptyList()
    }

    /** Returns all actual Photos under a given node (cluster or point). */
    suspend fun getPhotosForNode(clusterId: Long): List<Photo> {
        return indexMutex.withLock {
            val idx = index ?: return@withLock emptyList()
            val leaves = idx.getLeaves(clusterId)
            val pointIds = leaves.mapNotNull { it.pointId }.toSet()
            livePoints.filter { it.id in pointIds }.mapNotNull { it.payload as? Photo }
        }
    }

    fun deletePhotos(photos: List<Photo>) {
        viewModelScope.launch {
            photosRepository.deletePhotos(photos)
        }
    }

    fun importPhotos(context: Context, uris: List<Uri>, latLng: LatLng, dateStr: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val resolver = context.contentResolver
            val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.US)
            val filePrefixFormatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)

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

                    val now = Date()
                    val photo = Photo(
                        lat = latLng.latitude,
                        lng = latLng.longitude,
                        date = dateStr,
                        time = timeFormatter.format(now),
                        locationName = null,
                        uri = destFile.absolutePath,
                        url = null
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
            id = id.toString(),
            lat = lat,
            lon = lng,
            payload = this
        )
    }
}
