package com.sunflower.pinpic.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import coil.compose.AsyncImage
import com.sunflower.pinpic.domain.model.Photo
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import java.io.File

@Composable
fun FullScreenPhotoViewer(
    photos: List<Photo>,
    initialPageIndex: Int,
    onDismissRequest: () -> Unit,
    onDeletePhoto: (Photo) -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { photos.size }
    )
    
    var showInfoDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val photo = photos[page]
                val model = if (photo.uri != null) {
                    File(photo.uri)
                } else {
                    photo.url
                }
                
                AsyncImage(
                    model = model,
                    contentDescription = "Full Screen Photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { /* No-op as requested */ }) {
                        Icon(imageVector = Icons.Outlined.Share, contentDescription = "Share", tint = Color.White)
                    }
                    IconButton(onClick = {
                        val currentPhoto = photos[pagerState.currentPage]
                        com.sunflower.pinpic.common.MediaStoreUtils.savePhotosToDevice(
                            context,
                            listOf(currentPhoto)
                        )
                        Toast.makeText(context, "Saved to device", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.Outlined.Save, contentDescription = "Save", tint = Color.White)
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(imageVector = Icons.Outlined.Info, contentDescription = "Info", tint = Color.White)
                    }
                }
            }
        }
    }
    
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Photo") },
            text = { Text("Are you sure you want to delete this photo?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeletePhoto(photos[pagerState.currentPage])
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
    
    if (showInfoDialog) {
        PhotoInfoDialog(
            photo = photos[pagerState.currentPage],
            onDismissRequest = { showInfoDialog = false }
        )
    }
}
