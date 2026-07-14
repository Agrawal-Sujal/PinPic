package com.sunflower.pinpic.presentation.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.sunflower.pinpic.R
import com.sunflower.pinpic.domain.map.BBox
import com.sunflower.pinpic.domain.map.ClusterNode
import com.sunflower.pinpic.presentation.map.components.MarkerIconFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

private const val MARKER_CHUNK_SIZE = 10
private const val MARKER_CHUNK_DELAY_MS = 32L

@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }
    
    if (uiState.errorMessage != null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = uiState.errorMessage ?: "Unknown Error", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    val density = LocalDensity.current.density
    val iconFactory = remember(density) { MarkerIconFactory(density) }

    LaunchedEffect(iconFactory) {
        withContext(Dispatchers.Default) { iconFactory.warmUp() }
    }

    var renderedNodes by remember { mutableStateOf<List<ClusterNode>>(emptyList()) }

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

    val mapJson = if (isSystemInDarkTheme()) {
        R.raw.dark
    } else {
        R.raw.light
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(20.0, 0.0), 2f)
    }

    val mapProperties = remember {
        MapProperties(
            mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, mapJson)
        )
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        onMapLoaded = { reportCamera(cameraPositionState, viewModel) }
    ) {
        reportCamera(cameraPositionState, viewModel)

        renderedNodes.forEach { node ->
            key(node.id) {
                Marker(
                    state = rememberMarkerState(position = LatLng(node.lat, node.lon)),
                    icon = if (node.count > 1) iconFactory.cluster(node.count) else iconFactory.point(),
                    anchor = Offset(0.5f, 0.5f),
                    flat = true,
                    onClick = {
                        if (node.count > 1) {
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                LatLng(node.lat, node.lon),
                                cameraPositionState.position.zoom + 2f
                            )
                        }
                        true
                    }
                )
            }
        }
    }
}

private fun reportCamera(cameraPositionState: CameraPositionState, viewModel: MapViewModel) {
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
