package com.sunflower.pinpic.presentation

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface

data class ImageInfo(
    val name: String,
    val path: String,
    val uri: String,
    val latitude: Double?,
    val longitude: Double?
)

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun ImageMetadataScreen() {
    val context = LocalContext.current
    var info by remember { mutableStateOf<ImageInfo?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { info = readImageInfo(context, it) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Launch the picker regardless; location will just be null if denied
        pickerLauncher.launch(arrayOf("image/*"))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(onClick = {
            val perms = mutableListOf(Manifest.permission.ACCESS_MEDIA_LOCATION)
            if (Build.VERSION.SDK_INT >= 33) {
                perms += Manifest.permission.READ_MEDIA_IMAGES
            } else {
                perms += Manifest.permission.READ_EXTERNAL_STORAGE
            }
            permissionLauncher.launch(perms.toTypedArray())
        }) {
            Text("Select Image")
        }

        info?.let {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Name", style = MaterialTheme.typography.labelMedium)
                    Text(it.name)

                    Text("Path", style = MaterialTheme.typography.labelMedium)
                    Text(it.path)

                    Text("URI", style = MaterialTheme.typography.labelMedium)
                    Text(it.uri)

                    Text("Location", style = MaterialTheme.typography.labelMedium)
                    if (it.latitude != null && it.longitude != null) {
                        Text("Latitude: ${it.latitude}")
                        Text("Longitude: ${it.longitude}")
                    } else {
                        Text("No location data found")
                    }
                }
            }
        }
    }
}

/** Converts a document URI ("image:1234") into a real MediaStore URI. */
private fun resolveMediaUri(context: Context, uri: Uri): Uri {
    if (uri.authority == "media") return uri
    if (DocumentsContract.isDocumentUri(context, uri) &&
        uri.authority == "com.android.providers.media.documents"
    ) {
        val id = DocumentsContract.getDocumentId(uri).substringAfter(":").toLongOrNull()
        if (id != null) {
            return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
        }
    }
    return uri
}

fun readImageInfo(context: Context, pickedUri: Uri): ImageInfo {
    val resolver = context.contentResolver
    val mediaUri = resolveMediaUri(context, pickedUri)

    var name = "Unknown"
    var path = "Not available"

    // Name + path from MediaStore
    try {
        @Suppress("DEPRECATION")
        val projection = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.RELATIVE_PATH
        )
        resolver.query(mediaUri, projection, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                name = c.getString(0) ?: name
                val data = c.getString(1)
                val relative = c.getString(2)
                path = when {
                    !data.isNullOrBlank() -> data
                    !relative.isNullOrBlank() -> relative + name
                    else -> path
                }
            }
        }
    } catch (_: Exception) { }

    // Fallback for name if MediaStore didn't give it
    if (name == "Unknown") {
        try {
            resolver.query(pickedUri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst() && idx >= 0) name = c.getString(idx) ?: name
            }
        } catch (_: Exception) { }
    }

    // Location from EXIF (needs the *original* file, not the redacted copy)
    var lat: Double? = null
    var lon: Double? = null
    try {
        val exifUri = if (Build.VERSION.SDK_INT >= 29 && mediaUri.authority == "media") {
            MediaStore.setRequireOriginal(mediaUri)
        } else mediaUri

        resolver.openInputStream(exifUri)?.use { stream ->
            val latLong = ExifInterface(stream).latLong
            if (latLong != null) {
                lat = latLong[0]
                lon = latLong[1]
            }
        }
    } catch (_: Exception) {
        // Permission denied or no EXIF; leave as null
    }

    return ImageInfo(name, path, pickedUri.toString(), lat, lon)
}