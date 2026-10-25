package com.sunflower.pinpic.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.sunflower.pinpic.common.Result
import com.sunflower.pinpic.data.local.entity.Visibility
import com.sunflower.pinpic.data.remote.dto.UserClusterResponse
import com.sunflower.pinpic.domain.map.ClusterNode
import com.sunflower.pinpic.domain.map.GeoClusterIndex
import com.sunflower.pinpic.domain.model.Cluster
import com.sunflower.pinpic.domain.model.Location
import com.sunflower.pinpic.domain.model.Photo
import com.sunflower.pinpic.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

data class OtherUserMapUiState(
    val isLoading: Boolean = true,
    val visibleNodes: List<ClusterNode> = emptyList(),
    val errorMessage: String? = null,
    val initialCameraPosition: LatLng? = null,
    val totalClustersCount: Int = 0,
    val totalPhotosCount: Int = 0
)

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class OtherUserMapViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val camera = MutableStateFlow<CameraState?>(null)
    private val _uiState = MutableStateFlow(OtherUserMapUiState())
    val uiState: StateFlow<OtherUserMapUiState> = _uiState.asStateFlow()

    private var geoClusterIndex: GeoClusterIndex? = null
    private var allClusters: List<Cluster> = emptyList()

    init {
        viewModelScope.launch {
            camera
                .filterNotNull()
                .debounce(16.milliseconds)
                .distinctUntilChanged()
                .mapLatest { state ->
                    withContext(Dispatchers.Default) {
                        geoClusterIndex?.getClusters(state.bbox, state.zoom) ?: emptyList()
                    }
                }
                .collect { nodes ->
                    _uiState.update { it.copy(visibleNodes = nodes) }
                }
        }
    }

    fun loadUserClusters(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = userRepository.getUserClusters(userId)) {
                is Result.Success -> {
                    withContext(Dispatchers.Default) {
                        setupClusters(result.data)
                    }
                }

                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

    private fun setupClusters(clusterResponses: List<UserClusterResponse>) {
        val domainClusters = clusterResponses.mapNotNull { dto ->
            val lat = dto.location?.latitude ?: return@mapNotNull null
            val lon = dto.location.longitude ?: return@mapNotNull null

            val domainPhotos = dto.photos.map { p ->
                Photo(
                    localPhotoId = abs(p.photoId.hashCode()),
                    globalPhotoId = p.photoId,
                    userId = p.userId,
                    photoName = p.name ?: "Photo",
                    takenAt = p.takenAt ?: "",
                    editedAt = "",
                    photoUrl = p.downloadUrl ?: "",
                    visibility = if (p.visibility == "FRIENDS") Visibility.FRIENDS else Visibility.PUBLIC,
                    location = Location(
                        latitude = lat,
                        longitude = lon,
                        locationName = dto.location.locationName ?: "Unknown Location",
                        isEdited = false
                    )
                )
            }

            val clusterIdKey = dto.clusterId
            val intId = abs(clusterIdKey.hashCode())
            Cluster(
                localClusterId = intId,
                globalClusterId = clusterIdKey,
                location = Location(
                    latitude = lat,
                    longitude = lon,
                    locationName = dto.location.locationName ?: "Unknown Location",
                    isEdited = false
                ),
                thumbnail = Photo(
                    localPhotoId = 0,
                    userId = dto.userId,
                    photoName = "Thumbnail",
                    takenAt = "",
                    editedAt = "",
                    photoUrl = dto.thumbnailUrl ?: domainPhotos.firstOrNull()?.photoUrl ?: ""
                ),
                photoCount = if (dto.photoCount > 0) dto.photoCount else domainPhotos.size,
                photos = domainPhotos,
                visibility = if (dto.visibility == "FRIENDS") Visibility.FRIENDS else Visibility.PUBLIC
            )
        }

        allClusters = domainClusters

        if (domainClusters.isNotEmpty()) {
            geoClusterIndex = GeoClusterIndex.build(
                domainClusters,
                minZoom = 0,
                maxZoom = 21,
                radiusPixels = 40.0
            )

            val firstLoc = domainClusters.first().location
            val centerLatLng =
                if (firstLoc != null) LatLng(firstLoc.latitude, firstLoc.longitude) else null

            val totalPhotos = domainClusters.sumOf { it.photoCount }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    initialCameraPosition = centerLatLng,
                    totalClustersCount = domainClusters.size,
                    totalPhotosCount = totalPhotos
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    visibleNodes = emptyList(),
                    totalClustersCount = 0,
                    totalPhotosCount = 0
                )
            }
        }
    }

    fun onCameraIdle(state: CameraState) {
        camera.value = state
    }

    fun getPhotosForNode(nodeId: Long): List<Photo> {
        val index = geoClusterIndex ?: return emptyList()
        val leaves = index.getLeaves(nodeId)
        val photos = mutableListOf<Photo>()
        for (leaf in leaves) {
            val pointId = leaf.pointId
            if (pointId != null) {
                val cluster = allClusters.find { it.localClusterId.toString() == pointId }
                if (cluster != null) {
                    photos.addAll(cluster.photos ?: emptyList())
                }
            }
        }
        return photos.distinctBy { it.globalPhotoId ?: it.localPhotoId.toString() }
    }

    fun getClusterForNode(nodeId: Long): Cluster? {
        val index = geoClusterIndex ?: return null
        val leaves = index.getLeaves(nodeId)
        val firstLeaf = leaves.firstOrNull() ?: return null
        val pointId = firstLeaf.pointId ?: return null
        return allClusters.find { it.localClusterId.toString() == pointId }
    }

    fun getPhotosAndClusterForNode(nodeId: Long): Pair<List<Photo>, Cluster?> {
        return Pair(getPhotosForNode(nodeId), getClusterForNode(nodeId))
    }
}
