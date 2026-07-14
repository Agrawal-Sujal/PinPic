package com.sunflower.pinpic.presentation.gallery

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.animation.core.animateFloatAsState
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sunflower.pinpic.common.MediaStoreUtils.savePhotosToDevice
import com.sunflower.pinpic.domain.model.Photo
import com.sunflower.pinpic.presentation.common.FullScreenPhotoViewer
import com.sunflower.pinpic.presentation.common.noRippleClickable
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = hiltViewModel(),
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val allPhotos = remember(uiState.photosGroupedByDate) {
        uiState.photosGroupedByDate.values.flatten()
    }

    if (uiState.isLoading && uiState.photosGroupedByDate.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (uiState.errorMessage != null && uiState.photosGroupedByDate.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = uiState.errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        GalleryContent(
            uiState = uiState,
            onToggleDate = { viewModel.toggleDateExpansion(it) },
            onDeletePhotos = { viewModel.deletePhotos(it) },
            modifier = modifier,
            allPhotos = allPhotos
        )
    }
}

@Composable
fun GalleryContent(
    uiState: GalleryUiState,
    onToggleDate: (String) -> Unit,
    onDeletePhotos: (List<Photo>) -> Unit,
    modifier: Modifier = Modifier,
    allPhotos: List<Photo>
) {
    var selectedPhotoIndex by remember { mutableStateOf<Int?>(null) }
    val selectedPhotos = remember { mutableStateListOf<Photo>() }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showGalleryInfoDialog by remember { mutableStateOf(false) }
    val inSelectionMode = selectedPhotos.isNotEmpty()
    val context = LocalContext.current

    if (inSelectionMode) {
        BackHandler {
            selectedPhotos.clear()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (inSelectionMode) {
            SelectionActionBar(
                selectedCount = selectedPhotos.size,
                onClearSelection = { selectedPhotos.clear() },
                onShare = { /* No-op as requested */ },
                onInfo = { showGalleryInfoDialog = true },
                onSaveToDevice = {
                    savePhotosToDevice(
                        context,
                        selectedPhotos.toList()
                    )
                    Toast.makeText(context, "Saved to device", Toast.LENGTH_SHORT).show()
                    selectedPhotos.clear()
                },
                onDelete = { showDeleteConfirmDialog = true }
            )
        } else {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Gallery",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs
        GalleryTabs()

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.photosGroupedByDate.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No photos yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            // Photos Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 120.dp) // Leave space for bottom nav
            ) {
                // To sort dates descending, we need to sort the keys
                val sortedDates = uiState.photosGroupedByDate.keys.sortedDescending()

                sortedDates.forEach { dateStr ->
                    val photos = uiState.photosGroupedByDate[dateStr] ?: emptyList()
                    val isExpanded = uiState.expandedDates.contains(dateStr)
                    val visiblePhotos = if (isExpanded) photos else photos.take(3)

                    // Header
                    item(span = { GridItemSpan(3) }) {
                        GalleryDateHeader(
                            dateStr = dateStr,
                            count = photos.size,
                            isExpanded = isExpanded,
                            onToggle = { onToggleDate(dateStr) }
                        )
                    }

                    // Photos
                    items(visiblePhotos, key = { it.id }) { photo ->
                        val isSelected = selectedPhotos.contains(photo)
                        GalleryPhotoItem(
                            photo = photo,
                            isSelected = isSelected,
                            modifier = Modifier.animateItem(),
                            onClick = {
                                if (inSelectionMode) {
                                    if (isSelected) selectedPhotos.remove(photo) else selectedPhotos.add(photo)
                                } else {
                                    selectedPhotoIndex = allPhotos.indexOf(photo).takeIf { it >= 0 }
                                }
                            },
                            onLongClick = {
                                if (!inSelectionMode) {
                                    selectedPhotos.add(photo)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Photos") },
            text = { Text("Are you sure you want to delete ${selectedPhotos.size} photo(s)?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeletePhotos(selectedPhotos.toList())
                    selectedPhotos.clear()
                    showDeleteConfirmDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showGalleryInfoDialog && selectedPhotos.size == 1) {
        com.sunflower.pinpic.presentation.common.PhotoInfoDialog(
            photo = selectedPhotos.first(),
            onDismissRequest = { showGalleryInfoDialog = false }
        )
    }

    if (selectedPhotoIndex != null && allPhotos.isNotEmpty() && selectedPhotoIndex!! < allPhotos.size) {
        FullScreenPhotoViewer(
            photos = allPhotos,
            initialPageIndex = selectedPhotoIndex!!,
            onDismissRequest = { selectedPhotoIndex = null },
            onDeletePhoto = { photo ->
                onDeletePhotos(listOf(photo))
                selectedPhotoIndex = null // Dismiss viewer after delete
            }
        )
    }
}

@Composable
fun GalleryTabs() {
    val scrollState = rememberScrollState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        Triple("By Time", Icons.Outlined.AccessTime, true),
        Triple("All Photos", Icons.Outlined.GridView, false),
        Triple("By Location", Icons.Outlined.LocationOn, false)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = selectedTabIndex == index
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { selectedTabIndex = index }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = tab.second,
                    contentDescription = tab.first,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = tab.first,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = if (isSelected) MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold) else MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun GalleryDateHeader(
    dateStr: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    // Basic date parsing and formatting logic to match "Today • May 24, 2025" or similar
    // We expect dateStr in "yyyy-MM-dd"
    val displayDate = try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = parser.parse(dateStr)
        val formatter = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
        val formatted = date?.let { formatter.format(it) } ?: dateStr
        formatted
    } catch (_: Exception) {
        dateStr
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = "Date",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = displayDate,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Row(
            modifier = Modifier
                .noRippleClickable { onToggle() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$count Photos",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Toggle",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryPhotoItem(
    photo: Photo,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        val model = if (photo.uri != null) {
            File(photo.uri)
        } else {
            photo.url // if url is present
        }

        AsyncImage(
            model = model,
            contentDescription = "Photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Location Chip Overlay
        if (photo.locationName != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = "Location",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = photo.locationName,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Selection overlay
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            )
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun SelectionActionBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onShare: () -> Unit,
    onInfo: () -> Unit,
    onSaveToDevice: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
            Text("$selectedCount selected", style = MaterialTheme.typography.titleMedium)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (selectedCount == 1) {
                IconButton(onClick = onInfo) {
                    Icon(Icons.Outlined.Info, contentDescription = "Info")
                }
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Outlined.Share, contentDescription = "Share")
            }
            IconButton(onClick = onSaveToDevice) {
                Icon(Icons.Outlined.Save, contentDescription = "Save to Device")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
