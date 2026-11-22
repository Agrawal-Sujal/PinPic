package com.sunflower.pinpic.presentation.map.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.sunflower.pinpic.domain.model.Photo

/**
 * A dialog that shows photos from a cluster or single node.
 *
 * - By default it shows a compact grid of small thumbnails.
 * - A "Maximize" / "Exit fullscreen" toggle button in the header switches it
 *   to a full-screen dialog with larger images.
 * - No selection is supported — this is a view-only dialog.
 */
@Composable
fun ClusterPhotoDialog(
    photos: List<Photo>,
    photoCount: Int = photos.size,
    locationName: String? = null,
    onDismiss: () -> Unit,
) {
    var isFullscreen by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = isFullscreen,
        transitionSpec = {
            fadeIn(tween(220)) togetherWith fadeOut(tween(180))
        },
        label = "dialogSizeTransition"
    ) { fullscreen ->
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = !fullscreen,
                dismissOnBackPress = true,
                dismissOnClickOutside = !fullscreen,
                decorFitsSystemWindows = false
            )
        ) {
            Surface(
                modifier = if (fullscreen) {
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                },
                shape = if (fullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // ── Header ──────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = locationName ?: "Photos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "$photoCount photo${if (photoCount != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Maximize / restore toggle
                        IconButton(onClick = { isFullscreen = !fullscreen }) {
                            Icon(
                                imageVector = if (fullscreen) Icons.Filled.FullscreenExit
                                              else Icons.Filled.Fullscreen,
                                contentDescription = if (fullscreen) "Exit fullscreen"
                                                     else "Maximize",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Close button
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // ── Divider ─────────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    // ── Photo grid ──────────────────────────────────────────
                    val columns = if (fullscreen) 3 else 2
                    val imageSize = if (fullscreen) 180.dp else 140.dp

                    if (photos.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No photos available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (fullscreen) Modifier.fillMaxSize()
                                    else Modifier.height(
                                        // Compact: show up to 2 rows + padding
                                        (imageSize * 2) + 40.dp
                                    )
                                )
                        ) {
                            items(photos) { photo ->
                                PhotoThumbnailCard(photo = photo, imageSize = imageSize)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoThumbnailCard(
    photo: Photo,
    imageSize: androidx.compose.ui.unit.Dp
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        AsyncImage(
            model = photo.photoUrl,
            contentDescription = photo.photoName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
