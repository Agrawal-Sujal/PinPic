package com.sunflower.pinpic.presentation.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraMoveStartedReason
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
import com.sunflower.pinpic.domain.model.Photo
import com.sunflower.pinpic.presentation.gallery.GalleryContent
import com.sunflower.pinpic.presentation.gallery.GalleryUiState
import com.sunflower.pinpic.presentation.map.components.MarkerIconFactory
import com.sunflower.pinpic.presentation.map.components.PhotoMapMarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

private const val MARKER_CHUNK_SIZE = 10
private const val MARKER_CHUNK_DELAY_MS = 32L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    if (uiState.errorMessage != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = uiState.errorMessage ?: "Unknown Error",
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }

    val density = LocalDensity.current.density
    val iconFactory = remember(density) { MarkerIconFactory(density) }

    LaunchedEffect(iconFactory) {
        withContext(Dispatchers.Default) { iconFactory.warmUp() }
    }

    var renderedNodes by remember { mutableStateOf<List<ClusterNode>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    var bottomSheetPhotos by remember { mutableStateOf<List<Photo>>(emptyList()) }
    var expandedDates by remember { mutableStateOf(emptySet<String>()) }
    var expandedLocations by remember { mutableStateOf(emptySet<String>()) }
    var selectedMenuNode by remember { mutableStateOf<ClusterNode?>(null) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    val bottomSheetUiState = remember(bottomSheetPhotos, expandedDates, expandedLocations) {
        val byDate = bottomSheetPhotos.groupBy { it.date ?: "Unknown Date" }
        val byLoc = bottomSheetPhotos.groupBy { it.locationName ?: "Unknown Location" }
        GalleryUiState(
            photosGroupedByDate = byDate,
            photosGroupedByLocation = byLoc,
            expandedDates = expandedDates,
            expandedLocations = expandedLocations,
            isLoading = false
        )
    }

    var longPressedLocation by remember { mutableStateOf<LatLng?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var pendingUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            pendingUris = uris
            showDatePicker = true
        } else {
            longPressedLocation = null
        }
    }

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

//    val themeMode by viewModel.themeModeFlow.collectAsState(initial = "LIGHT")
//    val isDarkTheme = when (themeMode) {
//        "DARK" -> true
//        "SYSTEM" -> isSystemInDarkTheme()
//        else -> false
//    }
    val isDarkTheme = MainActivity.darkTheme

    val mapJson = if (isDarkTheme) {
        R.raw.dark
    } else {
        R.raw.light
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(20.0, 0.0), 2f)
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
                        LatLng(
                            loc.latitude,
                            loc.longitude
                        ), 15f
                    )
                )
            }
        }
    }

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

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            moveToCurrentLocation()
        }
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving && cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
            selectedMenuNode = null
            longPressedLocation = null
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
                selectedMenuNode = null
                longPressedLocation = null
            },
            onMapLongClick = { latLng ->
                coroutineScope.launch {
//                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    longPressedLocation = latLng
                }
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
                                selectedMenuNode = node
                                true
                            }
                        )
                    } else {
                        PhotoMapMarker(
                            node = node,
                            viewModel = viewModel,
                            onClick = {
                                showBottomSheet = true
                                coroutineScope.launch {
                                    val photos = viewModel.getPhotosForNode(node.id)
                                    bottomSheetPhotos = photos
                                }

                            },
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            longPressedLocation?.let { latLng ->
                val markerState = rememberMarkerState(position = latLng)
                markerState.position = latLng
                Marker(
                    state = markerState,
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )
            }
        }
        Surface(
            modifier = Modifier
                .systemBarsPadding()
                .align(Alignment.TopStart)
                .padding(top = 24.dp, start = 16.dp)
                .size(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

//        var dummyCount by remember { mutableStateOf("10") }
//
//        Surface(
//            modifier = Modifier
//                .systemBarsPadding()
//                .align(Alignment.TopEnd)
//                .padding(top = 24.dp, end = 16.dp)
//                .width(160.dp),
//            shape = RoundedCornerShape(8.dp),
//            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
//            shadowElevation = 4.dp
//        ) {
//            Column(modifier = Modifier.padding(8.dp)) {
//                OutlinedTextField(
//                    value = dummyCount,
//                    onValueChange = { dummyCount = it },
//                    label = { Text("Points") },
//                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                    modifier = Modifier.fillMaxWidth()
//                )
//                Spacer(modifier = Modifier.height(8.dp))
//                Button(
//                    onClick = {
//                        val count = dummyCount.toIntOrNull() ?: 0
//                        if (count > 0) {
//                            viewModel.generateDummyPoints(count)
//                        }
//                    },
//                    modifier = Modifier.fillMaxWidth(),
//                    enabled = !uiState.isGeneratingDummies
//                ) {
//                    if (uiState.isGeneratingDummies) {
//                        CircularProgressIndicator(
//                            modifier = Modifier.size(24.dp),
//                            color = MaterialTheme.colorScheme.onPrimary,
//                            strokeWidth = 2.dp
//                        )
//                    } else {
//                        Text("Build")
//                    }
//                }
//            }
//        }

        FloatingActionButton(
            onClick = onCameraClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 104.dp, end = 16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape
        ) {
            Icon(Icons.Outlined.CameraAlt, contentDescription = "Camera")
        }
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
                .padding(bottom = 32.dp, end = 16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape
        ) {
            Icon(Icons.Filled.MyLocation, contentDescription = "Current Location")
        }





        if (selectedMenuNode != null) {
            val node = selectedMenuNode!!
            var popupSize by remember { mutableStateOf(IntSize.Zero) }

            Box(
                modifier = Modifier
                    .offset {
                        val projection = cameraPositionState.projection
                        val point = projection?.toScreenLocation(LatLng(node.lat, node.lon))
                        if (point != null) {
                            IntOffset(
                                x = point.x - (popupSize.width / 2),
                                y = point.y - popupSize.height - 50
                            )
                        } else {
                            IntOffset.Zero
                        }
                    }
                    .onGloballyPositioned { coordinates ->
                        popupSize = coordinates.size
                    }
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .width(IntrinsicSize.Max)
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Zoom",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                        LatLng(node.lat, node.lon),
                                        cameraPositionState.position.zoom + 2f
                                    )
                                    selectedMenuNode = null
                                }
                                .padding(horizontal = 24.dp, vertical = 10.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "View photos",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        bottomSheetPhotos = viewModel.getPhotosForNode(node.id)
                                        showBottomSheet = true
                                    }
                                    selectedMenuNode = null
                                }
                                .padding(horizontal = 24.dp, vertical = 10.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        if (longPressedLocation != null && !showDatePicker) {
            val latLng = longPressedLocation!!
            var popupSize by remember { mutableStateOf(IntSize.Zero) }

            Box(
                modifier = Modifier
                    .offset {
                        val projection = cameraPositionState.projection
                        val point = projection?.toScreenLocation(latLng)
                        if (point != null) {
                            IntOffset(
                                x = point.x - (popupSize.width / 2),
                                y = point.y - popupSize.height - 100
                            )
                        } else {
                            IntOffset.Zero
                        }
                    }
                    .onGloballyPositioned { coordinates ->
                        popupSize = coordinates.size
                    }
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .width(IntrinsicSize.Max)
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Add image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                                .padding(horizontal = 24.dp, vertical = 10.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        val isExpanded = bottomSheetState.targetValue == SheetValue.Expanded
        ModalBottomSheet(
            containerColor = MaterialTheme.colorScheme.background,
            onDismissRequest = { showBottomSheet = false },
            sheetState = bottomSheetState,
            dragHandle = {
                if (isExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch { bottomSheetState.hide() }
                                    .invokeOnCompletion {
                                        if (!bottomSheetState.isVisible) {
                                            showBottomSheet = false
                                        }
                                    }
                            },
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                } else {
                    BottomSheetDefaults.DragHandle()
                }
            }
        ) {
            GalleryContent(
                uiState = bottomSheetUiState,
                onToggleDate = { date ->
                    expandedDates =
                        if (expandedDates.contains(date)) expandedDates - date else expandedDates + date
                },
                onToggleLocation = { loc ->
                    expandedLocations =
                        if (expandedLocations.contains(loc)) expandedLocations - loc else expandedLocations + loc
                },
                onDeletePhotos = { photos ->
                    viewModel.deletePhotos(photos)
                    bottomSheetPhotos = bottomSheetPhotos.filter { it !in photos }
                    if (bottomSheetPhotos.isEmpty()) {
                        showBottomSheet = false
                    }
                },
                allPhotos = bottomSheetPhotos,
                allowSelection = false
            )
        }
    }

    // Old custom popup Box removed in favor of MarkerInfoWindow

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
                longPressedLocation = null
                pendingUris = emptyList()
            },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    datePickerState.selectedDateMillis?.let { millis ->
                        val dateString =
                            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
                        viewModel.importPhotos(
                            context,
                            pendingUris,
                            longPressedLocation!!,
                            dateString
                        )
                    }
                    longPressedLocation = null
                    pendingUris = emptyList()
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    longPressedLocation = null
                    pendingUris = emptyList()
                }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
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
