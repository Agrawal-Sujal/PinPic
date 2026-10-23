package com.sunflower.pinpic.presentation.gallery

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.text.format.Formatter
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sunflower.pinpic.common.MediaStoreUtils.savePhotosToDevice
import com.sunflower.pinpic.data.local.entity.Visibility
import com.sunflower.pinpic.domain.model.Photo
import com.sunflower.pinpic.presentation.common.FullScreenPhotoViewer
import com.sunflower.pinpic.presentation.gallery.components.ChangeVisibilityBottomSheet
import java.io.File

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = hiltViewModel(),
    onSelectionModeChange: ((Boolean) -> Unit)? = null,
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
            onToggleLocation = { viewModel.toggleLocationExpansion(it) },
            onDeletePhotos = { viewModel.deletePhotos(it) },
            onUpdateVisibility = { photos, visibility ->
                viewModel.updatePhotosVisibility(photos, visibility)
            },
            onSelectionModeChange = onSelectionModeChange,
            onImportPhotos = { viewModel.importPhotos(it) },
            onSync = { viewModel.syncPhotos() },
            modifier = modifier,
            allPhotos = allPhotos,
            showFloatingImportButton = true
        )
    }

    if (uiState.isImporting) {
        ImportProgressDialog(uiState = uiState)
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun GalleryContent(
    uiState: GalleryUiState,
    onToggleDate: (String) -> Unit,
    onToggleLocation: (String) -> Unit,
    onDeletePhotos: (List<Photo>) -> Unit,
    modifier: Modifier = Modifier,
    allPhotos: List<Photo>,
    allowSelection: Boolean = true,
    showHeader: Boolean = true,
    onEdit: (() -> Unit)? = null,
    showFloatingImportButton: Boolean = showHeader,
    onImportPhotos: ((List<Uri>) -> Unit)? = null,
    onSync: (() -> Unit)? = null,
    onUpdateVisibility: ((List<Photo>, Visibility) -> Unit)? = null,
    onSelectionModeChange: ((Boolean) -> Unit)? = null
) {
    var selectedPhotoIndex by remember { mutableStateOf<Int?>(null) }
    val selectedPhotos = remember { mutableStateListOf<Photo>() }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showGalleryInfoDialog by remember { mutableStateOf(false) }
    var showVisibilityBottomSheet by remember { mutableStateOf(false) }

    val inSelectionMode = selectedPhotos.isNotEmpty()
    val context = LocalContext.current

    LaunchedEffect(inSelectionMode) {
        onSelectionModeChange?.invoke(inSelectionMode)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportPhotos?.invoke(uris)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        photoPickerLauncher.launch(
            arrayOf("image/*")
        )
    }

    val launchPhotoPicker = {
        val perms = mutableListOf(Manifest.permission.ACCESS_MEDIA_LOCATION)
        perms += if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    if (inSelectionMode) {
        BackHandler {
            selectedPhotos.clear()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (inSelectionMode) {
                SelectionActionBar(
                    selectedCount = selectedPhotos.size,
                    totalCount = allPhotos.size,
                    onSelectAll = {
                        selectedPhotos.clear()
                        selectedPhotos.addAll(allPhotos)
                    },
                    onClearSelection = { selectedPhotos.clear() },
                    onChangeVisibility = { showVisibilityBottomSheet = true },
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
                    onDelete = { showDeleteConfirmDialog = true },
                    onEdit = onEdit
                )
            } else if (showHeader) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gallery",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(
                        onClick = { onSync?.invoke() },
                        enabled = !uiState.isSyncing
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                contentDescription = "Sync",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (uiState.isSyncing) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(2.5.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
            ) {

                Spacer(modifier = Modifier.height(12.dp))

                Spacer(modifier = Modifier.height(8.dp))

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
                        items(allPhotos, key = { it.localPhotoId }) { photo ->
                            val isSelected = selectedPhotos.contains(photo)
                            GalleryPhotoItem(
                                photo = photo,
                                isSelected = isSelected,
                                modifier = Modifier.animateItem(),
                                onClick = {
                                    if (inSelectionMode) {
                                        if (isSelected) selectedPhotos.remove(photo) else selectedPhotos.add(
                                            photo
                                        )
                                    } else {
                                        selectedPhotoIndex =
                                            allPhotos.indexOf(photo).takeIf { it >= 0 }
                                    }
                                },
                                onLongClick = {
                                    if (allowSelection && !inSelectionMode) {
                                        selectedPhotos.add(photo)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Floating button at bottom right
        if (showFloatingImportButton && !inSelectionMode) {
            FloatingActionButton(
                onClick = { launchPhotoPicker() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 104.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Import Photos"
                )
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

        if (showVisibilityBottomSheet && selectedPhotos.isNotEmpty()) {
            val commonVisibility = if (selectedPhotos.map { it.visibility }.distinct().size == 1) {
                selectedPhotos.first().visibility
            } else null
            ChangeVisibilityBottomSheet(
                selectedCount = selectedPhotos.size,
                initialVisibility = commonVisibility,
                onDismissRequest = { showVisibilityBottomSheet = false },
                onApply = { chosenVisibility ->
                    onUpdateVisibility?.invoke(selectedPhotos.toList(), chosenVisibility)
                    val visibilityName = chosenVisibility.name.lowercase().replaceFirstChar { it.uppercase() }
                    Toast.makeText(
                        context,
                        "Visibility changed to $visibilityName",
                        Toast.LENGTH_SHORT
                    ).show()
                    selectedPhotos.clear()
                    showVisibilityBottomSheet = false
                }
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
}

@Composable
fun ImportProgressDialog(uiState: GalleryUiState) {
    Dialog(
        onDismissRequest = { /* Block dismissal while import is processing */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = "Importing Photos",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                if (uiState.importTotal > 0) {
                    LinearProgressIndicator(
                        progress = { uiState.importProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Importing ${uiState.importCurrent} of ${uiState.importTotal}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(uiState.importProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "Reading metadata and saving to app storage...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


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
        val model = File(photo.photoUrl)

        AsyncImage(
            model = model,
            contentDescription = "Photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

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
    totalCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onShare: () -> Unit,
    onInfo: () -> Unit,
    onSaveToDevice: () -> Unit,
    onDelete: () -> Unit,
    onChangeVisibility: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
            Text("$selectedCount selected", style = MaterialTheme.typography.titleMedium)
        }
        Row {
            TriStateCheckbox(
                state = when (selectedCount) {
                    0 -> ToggleableState.Off
                    totalCount -> ToggleableState.On
                    else -> ToggleableState.Indeterminate
                },
                onClick = if (selectedCount == totalCount) onClearSelection else onSelectAll
            )
            if (onChangeVisibility != null) {
                IconButton(onClick = onChangeVisibility) {
                    Icon(
                        Icons.Outlined.Visibility,
                        contentDescription = "Change Visibility"
                    )
                }
            }
            if (selectedCount == 1) {
                IconButton(onClick = onInfo) {
                    Icon(Icons.Outlined.Info, contentDescription = "Info")
                }
                if (onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                    }
                }
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Outlined.Share, contentDescription = "Share")
            }
            IconButton(onClick = onSaveToDevice) {
                Icon(Icons.Outlined.Save, contentDescription = "Save to Device")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

//private fun logAllImageMetadata(context: android.content.Context, uri: Uri) {
//    val tag = "GalleryScreen"
//    Log.d(tag, "==================== IMAGE METADATA START ====================")
//    Log.d(tag, "Image selected: $uri")
//
//    // 1. ContentResolver / MediaStore / OpenableColumns metadata
//    var displayName: String? = null
//    var fileSize: Long? = null
//    val mimeType = context.contentResolver.getType(uri)
//
//    try {
//        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
//            if (cursor.moveToFirst()) {
//                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
//                if (nameIndex != -1 && !cursor.isNull(nameIndex)) {
//                    displayName = cursor.getString(nameIndex)
//                }
//                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
//                if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
//                    fileSize = cursor.getLong(sizeIndex)
//                }
//
//                Log.d(tag, "--- ContentResolver Columns ---")
//                for (i in 0 until cursor.columnCount) {
//                    val colName = cursor.getColumnName(i)
//                    val colVal = try {
//                        cursor.getString(i)
//                    } catch (_: Exception) {
//                        null
//                    }
//                    if (colVal != null) {
//                        Log.d(tag, "Column [$colName] = $colVal")
//                    }
//                }
//            }
//        }
//    } catch (e: Exception) {
//        Log.e(tag, "Error querying ContentResolver: ${e.message}", e)
//    }
//
//    Log.d(tag, "--- Basic File Details ---")
//    Log.d(tag, "File Name: ${displayName ?: "Unknown"}")
//    Log.d(
//        tag,
//        "File Size: ${
//            fileSize?.let {
//                Formatter.formatShortFileSize(
//                    context,
//                    it
//                )
//            } ?: "Unknown"
//        } (${fileSize ?: 0} bytes)")
//    Log.d(tag, "MIME Type: ${mimeType ?: "Unknown"}")
//
//    // 2. Decode Image Dimensions using BitmapFactory
//    var decodedWidth = -1
//    var decodedHeight = -1
//    try {
//        context.contentResolver.openInputStream(uri)?.use { stream ->
//            val options = BitmapFactory.Options().apply {
//                inJustDecodeBounds = true
//            }
//            BitmapFactory.decodeStream(stream, null, options)
//            decodedWidth = options.outWidth
//            decodedHeight = options.outHeight
//        }
//    } catch (e: Exception) {
//        Log.e(tag, "Error decoding image bounds: ${e.message}", e)
//    }
//
//    // 3. EXIF Metadata using androidx.exifinterface
//    try {
//        val exif: ExifInterface? = try {
//            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
//                ExifInterface(pfd.fileDescriptor)
//            }
//        } catch (_: Exception) {
//            null
//        } ?: try {
//            context.contentResolver.openInputStream(uri)?.use { stream ->
//                ExifInterface(stream)
//            }
//        } catch (_: Exception) {
//            null
//        }
//
//        if (exif != null) {
//            // Location
//            val latLong = exif.latLong
//            val hasLocation = latLong != null
//            Log.d(tag, "--- Location Metadata ---")
//            Log.d(tag, "Has Location: $hasLocation")
//            if (latLong != null) {
//                Log.d(tag, "Location: Lat ${latLong[0]}, Lng ${latLong[1]}")
//                Log.d(tag, "Latitude: ${latLong[0]}")
//                Log.d(tag, "Longitude: ${latLong[1]}")
//            }
//            val altitude = exif.getAltitude(Double.NaN)
//            if (!altitude.isNaN()) {
//                Log.d(tag, "Altitude: $altitude meters")
//            }
//
//            // Date / Time
//            val date = exif.getAttribute(ExifInterface.TAG_DATETIME)
//            val dateOriginal = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
//            val dateDigitized = exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
//            val offsetTime = exif.getAttribute(ExifInterface.TAG_OFFSET_TIME)
//                ?: exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL)
//            Log.d(tag, "--- Date & Time Metadata ---")
//            Log.d(tag, "Date: ${date ?: dateOriginal ?: "N/A"}")
//            if (dateOriginal != null) Log.d(tag, "Date (Original): $dateOriginal")
//            if (dateDigitized != null) Log.d(tag, "Date (Digitized): $dateDigitized")
//            if (offsetTime != null) Log.d(tag, "Timezone Offset: $offsetTime")
//
//            // Make / Model / Lens / Software
//            val make = exif.getAttribute(ExifInterface.TAG_MAKE)
//            val model = exif.getAttribute(ExifInterface.TAG_MODEL)
//            val lensMake = exif.getAttribute(ExifInterface.TAG_LENS_MAKE)
//            val lensModel = exif.getAttribute(ExifInterface.TAG_LENS_MODEL)
//            val software = exif.getAttribute(ExifInterface.TAG_SOFTWARE)
//            Log.d(tag, "--- Device & Camera ---")
//            Log.d(tag, "Make: ${make ?: "N/A"}, Model: ${model ?: "N/A"}")
//            if (lensMake != null || lensModel != null) {
//                Log.d(tag, "Lens: ${listOfNotNull(lensMake, lensModel).joinToString(" ")}")
//            }
//            if (software != null) Log.d(tag, "Software: $software")
//
//            // Dimensions / Resolution
//            val exifWidth = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)
//                ?: exif.getAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION)
//                ?: if (decodedWidth > 0) decodedWidth.toString() else null
//            val exifLength = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)
//                ?: exif.getAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION)
//                ?: if (decodedHeight > 0) decodedHeight.toString() else null
//            Log.d(tag, "--- Dimensions & Orientation ---")
//            Log.d(tag, "Size: ${exifWidth ?: "N/A"} x ${exifLength ?: "N/A"}")
//            if (decodedWidth > 0 && decodedHeight > 0) {
//                Log.d(tag, "Decoded Dimensions: ${decodedWidth}x${decodedHeight}")
//            }
//
//            // Orientation
//            val orientation = exif.getAttributeInt(
//                ExifInterface.TAG_ORIENTATION,
//                ExifInterface.ORIENTATION_UNDEFINED
//            )
//            val orientationDesc = when (orientation) {
//                ExifInterface.ORIENTATION_NORMAL -> "Normal (0°)"
//                ExifInterface.ORIENTATION_ROTATE_90 -> "Rotate 90° CW"
//                ExifInterface.ORIENTATION_ROTATE_180 -> "Rotate 180°"
//                ExifInterface.ORIENTATION_ROTATE_270 -> "Rotate 270° CW"
//                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> "Flip Horizontal"
//                ExifInterface.ORIENTATION_FLIP_VERTICAL -> "Flip Vertical"
//                ExifInterface.ORIENTATION_TRANSPOSE -> "Transpose"
//                ExifInterface.ORIENTATION_TRANSVERSE -> "Transverse"
//                else -> "Undefined ($orientation)"
//            }
//            Log.d(tag, "Orientation: $orientationDesc")
//
//            // Camera Settings
//            val exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
//            val fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
//            val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
//            val focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
//            val focalLength35mm = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM)
//            val flash = exif.getAttribute(ExifInterface.TAG_FLASH)
//            val whiteBalance = exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE)
//
//            Log.d(tag, "--- Camera Settings ---")
//            if (exposureTime != null) Log.d(tag, "Exposure Time: $exposureTime s")
//            if (fNumber != null) Log.d(tag, "F-Number: f/$fNumber")
//            if (iso != null) Log.d(tag, "ISO: $iso")
//            if (focalLength != null) Log.d(tag, "Focal Length: $focalLength mm")
//            if (focalLength35mm != null) Log.d(tag, "Focal Length (35mm): $focalLength35mm mm")
//            if (flash != null) Log.d(tag, "Flash: $flash")
//            if (whiteBalance != null) Log.d(tag, "White Balance: $whiteBalance")
//
//            // All EXIF Tags present in the file
//            Log.d(tag, "--- All Present EXIF Attributes ---")
//            val allExifTags = mutableMapOf<String, String>()
//            val fields = ExifInterface::class.java.fields
//            for (field in fields) {
//                if (java.lang.reflect.Modifier.isStatic(field.modifiers) &&
//                    field.name.startsWith("TAG_") &&
//                    field.type == String::class.java
//                ) {
//                    try {
//                        val tagConstValue = field.get(null) as? String ?: continue
//                        val tagVal = exif.getAttribute(tagConstValue)
//                        if (!tagVal.isNullOrBlank()) {
//                            allExifTags[field.name] = tagVal
//                        }
//                    } catch (_: Exception) {
//                    }
//                }
//            }
//
//            allExifTags.toSortedMap().forEach { (tagName, tagValue) ->
//                Log.d(tag, "$tagName: $tagValue")
//            }
//            Log.d(tag, "Total EXIF tags found: ${allExifTags.size}")
//        } else {
//            Log.w(tag, "Could not open ExifInterface for URI: $uri")
//        }
//    } catch (e: Exception) {
//        Log.e(tag, "Error reading EXIF metadata", e)
//    }
//
//    Log.d(tag, "==================== IMAGE METADATA END ====================")
//}
