package com.sunflower.pinpic.presentation.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sunflower.pinpic.domain.model.LocationInfo
import com.sunflower.pinpic.domain.model.Photo
import com.sunflower.pinpic.presentation.common.shimmerEffect
import com.sunflower.pinpic.presentation.gallery.GalleryContent
import com.sunflower.pinpic.presentation.gallery.GalleryUiState
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.os.Environment

@Composable
fun SelectedClusterGallerySheet(
    photos: List<Photo>,
    locationInfo: LocationInfo?,
    isLoadingLocation: Boolean,
    onClose: () -> Unit,
    onDeletePhotos: (List<Photo>) -> Unit,
    clusterPoint: com.sunflower.pinpic.domain.model.ClusterPoint? = null,
    onEditClusterPoint: ((com.sunflower.pinpic.domain.model.ClusterPoint) -> Unit)? = null,
    showThumbnailAndLocation: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }
    val groupedByDate = remember(photos) { photos.groupBy { it.date ?: "Unknown Date" } }
    val groupedByLocation = remember(photos) { photos.groupBy { it.locationName ?: "Unknown Location" } }
    
    var uiState by remember(photos) {
        mutableStateOf(
            GalleryUiState(
                photosGroupedByDate = groupedByDate,
                photosGroupedByLocation = groupedByLocation
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
            if (showThumbnailAndLocation) {
                Spacer(modifier = Modifier.width(8.dp))
                
                // Thumbnail
                val thumbnailUriToDisplay = clusterPoint?.thumbnailUri ?: photos.firstOrNull()?.let { it.uri ?: it.url }
                
                if (thumbnailUriToDisplay != null) {
                    AsyncImage(
                        model = thumbnailUriToDisplay,
                        contentDescription = "Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // Location Info
                Column(modifier = Modifier.weight(1f)) {
                    if (isLoadingLocation) {
                        Box(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp).shimmerEffect())
                    } else {
                        val displayName = clusterPoint?.locationName ?: clusterPoint?.city ?: locationInfo?.name ?: locationInfo?.city ?: "Unknown Location"
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${photos.size} photos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (clusterPoint != null && onEditClusterPoint != null) {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

        // Gallery Content
        Box(modifier = Modifier.weight(1f)) {
            GalleryContent(
                uiState = uiState,
                onToggleDate = { dateStr ->
                    val currentExpanded = uiState.expandedDates
                    val newExpanded = if (currentExpanded.contains(dateStr)) {
                        currentExpanded - dateStr
                    } else {
                        currentExpanded + dateStr
                    }
                    uiState = uiState.copy(expandedDates = newExpanded)
                },
                onToggleLocation = { locStr ->
                    val currentExpanded = uiState.expandedLocations
                    val newExpanded = if (currentExpanded.contains(locStr)) {
                        currentExpanded - locStr
                    } else {
                        currentExpanded + locStr
                    }
                    uiState = uiState.copy(expandedLocations = newExpanded)
                },
                onDeletePhotos = onDeletePhotos,
                allPhotos = photos,
                showHeader = false
            )
        }
    }
    
    if (showEditDialog && clusterPoint != null && onEditClusterPoint != null) {
        EditClusterMetadataDialog(
            clusterPoint = clusterPoint,
            photos = photos,
            onDismiss = { showEditDialog = false },
            onSave = { updatedPoint ->
                onEditClusterPoint(updatedPoint)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun EditClusterMetadataDialog(
    clusterPoint: com.sunflower.pinpic.domain.model.ClusterPoint,
    photos: List<Photo>,
    onDismiss: () -> Unit,
    onSave: (com.sunflower.pinpic.domain.model.ClusterPoint) -> Unit
) {
    var locationName by remember { mutableStateOf(clusterPoint.locationName ?: "") }
    var city by remember { mutableStateOf(clusterPoint.city ?: "") }
    var selectedThumbnailUri by remember { mutableStateOf(clusterPoint.thumbnailUri) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val absolutePath = withContext(Dispatchers.IO) {
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                        val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                        val fileName = "PinPic-CustomThumb-${System.currentTimeMillis()}.jpg"
                        val destFile = java.io.File(picturesDir, fileName)
                        java.io.FileOutputStream(destFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        inputStream.close()
                        destFile.absolutePath
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                if (absolutePath != null) {
                    selectedThumbnailUri = absolutePath
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Cluster Details") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Thumbnail:", style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Text("Import from files")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(photos) { photo ->
                        val uri = photo.uri ?: photo.url
                        val isSelected = uri == selectedThumbnailUri || (selectedThumbnailUri == null && uri == (photos.firstOrNull()?.uri ?: photos.firstOrNull()?.url))
                        AsyncImage(
                            model = uri,
                            contentDescription = "Thumbnail option",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedThumbnailUri = uri
                                }
                        )
                    }
                    if (selectedThumbnailUri != null && photos.none { it.uri == selectedThumbnailUri || it.url == selectedThumbnailUri }) {
                        item {
                            AsyncImage(
                                model = selectedThumbnailUri,
                                contentDescription = "Custom Thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        width = 3.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        // already selected
                                    }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    label = { Text("Location Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val updatedPoint = clusterPoint.copy(
                    locationName = locationName.takeIf { it.isNotBlank() },
                    city = city.takeIf { it.isNotBlank() },
                    thumbnailUri = selectedThumbnailUri
                )
                onSave(updatedPoint)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
