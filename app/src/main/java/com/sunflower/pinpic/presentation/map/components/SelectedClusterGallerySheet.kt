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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.ui.graphics.Color
import com.sunflower.pinpic.domain.model.Cluster

@Composable
fun SelectedClusterGallerySheet(
    photos: List<Photo>,
    onClose: () -> Unit,
    onDeletePhotos: (List<Photo>) -> Unit,
    cluster: Cluster? = null,
    onEditCluster: ((Cluster) -> Unit)? = null,
    showThumbnailAndLocation: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }
    val groupedByDate = remember(photos) { photos.groupBy { it.takenAt ?: "Unknown Date" } }
    val groupedByLocation = remember(photos) { photos.groupBy { it.location?.locationName ?: "Unknown Location" } }
    
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
                showHeader = false,
                onEdit = null
            )
            
            if (cluster != null && onEditCluster != null) {
                var expanded by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = { expanded = true },
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Thumbnail") },
                            onClick = { 
                                showEditDialog = true
                                expanded = false 
                            }
                        )
                    }
                }
            }
        }
    }
    
    if (showEditDialog && cluster != null && onEditCluster != null) {
        EditClusterMetadataDialog(
            cluster = cluster,
            photos = photos,
            onDismiss = { showEditDialog = false },
            onSave = { updatedCluster ->
                onEditCluster(updatedCluster)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun EditClusterMetadataDialog(
    cluster: Cluster,
    photos: List<Photo>,
    onDismiss: () -> Unit,
    onSave: (Cluster) -> Unit
) {
    var selectedThumbnailUri by remember { mutableStateOf(cluster.thumbnail?.photoUrl) }
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
                        val uri = photo.photoUrl
                        val isSelected = uri == selectedThumbnailUri || (selectedThumbnailUri == null && uri == (photos.firstOrNull()?.photoUrl))
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
                    if (selectedThumbnailUri != null && photos.none { it.photoUrl == selectedThumbnailUri }) {
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

            }
        },
        confirmButton = {
            TextButton(onClick = {
                // Here we would ideally want to update the Photo object that is the thumbnail.
                // Since this is a simple dialog we just save a mock Photo with the new URI.
                val updatedCluster = cluster.copy(
                    thumbnail = cluster.thumbnail?.copy(photoUrl = selectedThumbnailUri ?: "")
                )
                onSave(updatedCluster)
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
