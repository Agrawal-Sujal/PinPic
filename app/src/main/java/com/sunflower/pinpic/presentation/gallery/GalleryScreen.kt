package com.sunflower.pinpic.presentation.gallery

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sunflower.pinpic.domain.model.Photo
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = hiltViewModel(),
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Trigger load on compose (already done in init, but keeping it for completeness if needed)
//    LaunchedEffect(Unit) {
//        viewModel.loadPhotos()
//    }

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
            modifier = modifier
        )
    }
}

@Composable
fun GalleryContent(
    uiState: GalleryUiState,
    onToggleDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .padding(vertical = 16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "Gallery",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your moments, beautifully mapped.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = "Filter",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                    items(visiblePhotos) { photo ->
                        GalleryPhotoItem(photo = photo)
                    }
                }
            }
        }
    }
}

@Composable
fun GalleryTabs() {
    val scrollState = rememberScrollState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        Triple("By Time", Icons.Outlined.AccessTime, true),
        Triple("By Location", Icons.Outlined.LocationOn, false),
        Triple("By Faces", Icons.Outlined.Face, false),
        Triple("All Photos", Icons.Outlined.GridView, false)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
                    style = if (isSelected) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold) else MaterialTheme.typography.bodyMedium
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
            .clickable { onToggle() }
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$count Photos",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = "Toggle",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun GalleryPhotoItem(photo: Photo) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
                    .background(Color.Black.copy(alpha = 0.5f)) // Keep this as it's an overlay
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
    }
}
