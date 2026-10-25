package com.sunflower.pinpic.presentation.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.sunflower.pinpic.MainActivity
import com.sunflower.pinpic.R
import com.sunflower.pinpic.domain.map.BBox
import com.sunflower.pinpic.domain.map.ClusterNode
import com.sunflower.pinpic.domain.model.Cluster
import com.sunflower.pinpic.domain.model.Photo
import com.sunflower.pinpic.presentation.map.components.MarkerIconFactory
import com.sunflower.pinpic.presentation.map.components.PhotoMapMarker
import com.sunflower.pinpic.presentation.map.components.SelectedClusterGallerySheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

private const val MARKER_CHUNK_SIZE = 10
private const val MARKER_CHUNK_DELAY_MS = 32L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherUserMapScreen(
    userId: String,
    username: String = "",
    onBack: () -> Unit,
    viewModel: OtherUserMapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    val iconFactory = remember(density) { MarkerIconFactory(density) }

    LaunchedEffect(userId) {
        viewModel.loadUserClusters(userId)
    }

    LaunchedEffect(iconFactory) {
        withContext(Dispatchers.IO) {
            try {
                com.google.android.gms.maps.MapsInitializer.initialize(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        withContext(Dispatchers.Default) {
            iconFactory.warmUp()
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    var renderedNodes by remember { mutableStateOf<List<ClusterNode>>(emptyList()) }
    var selectedClusterPhotos by remember { mutableStateOf<List<Photo>?>(null) }
    var selectedCluster by remember { mutableStateOf<Cluster?>(null) }
    var isClusterClick by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.visibleNodes) {
        val newIds = uiState.visibleNodes.map { it.id }.toSet()
        renderedNodes = renderedNodes.filter { it.id in newIds }

        val alreadyRenderedIds = renderedNodes.map { it.id }.toSet()
        val toAdd = uiState.visibleNodes.filter { it.id !in alreadyRenderedIds }

        toAdd.chunked(MARKER_CHUNK_SIZE).forEach { chunk ->
            renderedNodes = renderedNodes + chunk
            delay(MARKER_CHUNK_DELAY_MS.milliseconds)
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(20.0, 0.0), 2f)
    }

    var hasCenteredOnClusters by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.initialCameraPosition) {
        if (!hasCenteredOnClusters && uiState.initialCameraPosition != null) {
            hasCenteredOnClusters = true
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(uiState.initialCameraPosition!!, 10f)
            )
        }
    }

    val isDarkTheme = MainActivity.darkTheme
    val mapJson = if (isDarkTheme) R.raw.dark else R.raw.light

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val moveToCurrentLocation: () -> Unit = {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (_: SecurityException) {
            null
        }

        location?.let { loc ->
            coroutineScope.launch {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(loc.latitude, loc.longitude),
                        15f
                    )
                )
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            moveToCurrentLocation()
        }
    }

    val mapProperties = remember(hasLocationPermission) {
        MapProperties(
            mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, mapJson),
            isMyLocationEnabled = hasLocationPermission
        )
    }

    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings,
            onMapLoaded = { reportCamera(cameraPositionState, viewModel) },
            onMapClick = {
                selectedClusterPhotos = null
                selectedCluster = null
                isClusterClick = false
            }
        ) {
            reportCamera(cameraPositionState, viewModel)

            renderedNodes.forEach { node ->
                key(node.id) {
                    if (node.isCluster) {
                        Marker(
                            state = rememberMarkerState(position = LatLng(node.lat, node.lon)),
                            icon = iconFactory.cluster(node.count),
                            anchor = Offset(0.5f, 0.5f),
                            flat = true,
                            onClick = {
                                coroutineScope.launch {
                                    val (photos, cluster) = viewModel.getPhotosAndClusterForNode(
                                        node.id
                                    )
                                    if (photos.isNotEmpty()) {
                                        selectedClusterPhotos = photos
                                        selectedCluster =
                                            cluster ?: Cluster(localClusterId = node.id.toInt())
                                        isClusterClick = true
                                    }
                                }
                                true
                            }
                        )
                    } else {
                        PhotoMapMarker(
                            node = node,
                            viewModel = null,
                            onClick = {
                                coroutineScope.launch {
                                    val (photos, cluster) = viewModel.getPhotosAndClusterForNode(
                                        node.id
                                    )
                                    if (photos.isNotEmpty()) {
                                        selectedClusterPhotos = photos
                                        selectedCluster =
                                            cluster ?: Cluster(localClusterId = node.id.toInt())
                                        isClusterClick = false
                                    }
                                }
                            },
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        // Top Floating Header with Back Button and Profile info
        Surface(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (username.isNotEmpty()) "@$username" else "User Photos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${uiState.totalPhotosCount} photos • ${uiState.totalClustersCount} locations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Bottom My-Location Floating Action Button
        FloatingActionButton(
            onClick = {
                val hasPerm = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                hasLocationPermission = hasPerm

                if (hasPerm) {
                    moveToCurrentLocation()
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 32.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape
        ) {
            Icon(
                Icons.Filled.MyLocation,
                contentDescription = "Current Location"
            )
        }

        // Cluster Gallery Modal Bottom Sheet
        if (!selectedClusterPhotos.isNullOrEmpty()) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = {
                    selectedClusterPhotos = null
                    selectedCluster = null
                    isClusterClick = false
                },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.background,
                dragHandle = { BottomSheetDefaults.DragHandle() },
                modifier = Modifier.fillMaxSize()
            ) {
                SelectedClusterGallerySheet(
                    photos = selectedClusterPhotos!!,
                    cluster = selectedCluster,
                    showThumbnailAndLocation = !isClusterClick,
                    onClose = {
                        coroutineScope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                selectedClusterPhotos = null
                                selectedCluster = null
                                isClusterClick = false
                            }
                        }
                    },
                    onEditCluster = null, // Read-only: cluster editing disabled
                    onDeletePhotos = {}    // Read-only: photo deletion disabled
                )
            }
        }
    }
}

private fun reportCamera(
    cameraPositionState: CameraPositionState,
    viewModel: OtherUserMapViewModel
) {
    val bounds = cameraPositionState.projection?.visibleRegion?.latLngBounds ?: return
    viewModel.onCameraIdle(
        CameraState(
            bbox = BBox(
                minLon = bounds.southwest.longitude,
                minLat = bounds.southwest.latitude,
                maxLon = bounds.northeast.longitude,
                maxLat = bounds.northeast.latitude
            ),
            zoom = cameraPositionState.position.zoom.toInt()
        )
    )
}

