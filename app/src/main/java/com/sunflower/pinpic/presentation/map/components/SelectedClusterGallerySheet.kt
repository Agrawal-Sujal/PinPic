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

@Composable
fun SelectedClusterGallerySheet(
    photos: List<Photo>,
    locationInfo: LocationInfo?,
    isLoadingLocation: Boolean,
    onClose: () -> Unit,
    onDeletePhotos: (List<Photo>) -> Unit,
    showThumbnailAndLocation: Boolean = true,
    modifier: Modifier = Modifier
) {
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
                val featuredPhoto = photos.firstOrNull()
                if (featuredPhoto != null) {
                    AsyncImage(
                        model = featuredPhoto.uri ?: featuredPhoto.url,
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
                        Text(
                            text = locationInfo?.name ?: locationInfo?.city ?: "Unknown Location",
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
}
