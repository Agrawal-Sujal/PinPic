package com.sunflower.pinpic.presentation.map.components

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sunflower.pinpic.domain.model.LocationInfo
import com.sunflower.pinpic.presentation.map.AddPhotoMode
import com.sunflower.pinpic.presentation.common.shimmerEffect

@Composable
fun AddPhotoBottomSheetContent(
    addPhotoMode: AddPhotoMode,
    locationInfo: LocationInfo?,
    isLoadingLocation: Boolean,
    pendingUris: List<Uri>,
    onSelectPhotosClick: () -> Unit,
    onNextClick: () -> Unit,
    dateText: String = "",
    onDateTextChange: (String) -> Unit = {},
    timeText: String = "",
    onTimeTextChange: (String) -> Unit = {},
    onDatePickerClick: () -> Unit = {},
    onTimePickerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
//            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (addPhotoMode) {
            AddPhotoMode.SELECT_LOCATION -> {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp).padding(bottom = 16.dp)
                )
                Text(
                    text = "Select a Location",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Tap anywhere on the map to drop a pin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
            AddPhotoMode.LOCATION_SELECTED -> {
                val titleModifier = if (isLoadingLocation) Modifier.fillMaxWidth(0.6f).height(24.dp).shimmerEffect() else Modifier
                val subtitleModifier = if (isLoadingLocation) Modifier.fillMaxWidth(0.9f).height(16.dp).shimmerEffect() else Modifier
                val title = locationInfo?.name ?: locationInfo?.city ?: "Selected Location"
                val subtitle = locationInfo?.formatted ?: "Unknown Location"
                
                Text(
                    text = if (isLoadingLocation) "" else title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = titleModifier.padding(top = 16.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isLoadingLocation) "" else subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = subtitleModifier,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onSelectPhotosClick,
                    enabled = !isLoadingLocation,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select Photos")
                }
            }
            AddPhotoMode.PHOTOS_SELECTED -> {
                val title = locationInfo?.name ?: locationInfo?.city ?: "Selected Location"
                val subtitle = locationInfo?.formatted ?: "Unknown Location"
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selected ${pendingUris.size} photos",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = onSelectPhotosClick) {
                        Text("Add more")
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(pendingUris) { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = dateText,
                    onValueChange = onDateTextChange,
                    label = { Text("Date (DD/MM/YYYY)") },
                    trailingIcon = {
                        IconButton(onClick = onDatePickerClick) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = timeText,
                    onValueChange = onTimeTextChange,
                    label = { Text("Time (12 hr format)") },
                    trailingIcon = {
                        IconButton(onClick = onTimePickerClick) {
                            Icon(Icons.Default.AccessTime, contentDescription = "Select Time")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                Button(
                    onClick = onNextClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Photos")
                }
            }
            else -> {}
        }
    }
}
