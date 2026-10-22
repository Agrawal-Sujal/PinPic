package com.sunflower.pinpic.presentation.common

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sunflower.pinpic.domain.model.Photo
import java.io.File

@Composable
fun PhotoInfoDialog(
    photo: Photo,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Photo Info") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val sizeString = photo.photoUrl.let { uri ->
                    val file = File(uri)
                    if (file.exists()) Formatter.formatShortFileSize(context, file.length()) else "N/A"
                } ?: "N/A"
                
                Text("Size: $sizeString")
                Text("Date: ${photo.takenAt}")
//                Text("Time: ${photo.time ?: "N/A"}")
//                Text("Location: ${photo.locationName ?: "N/A"}")
//                val latStr = photo.lat?.let { "%.6f".format(it) } ?: "N/A"
//                val lngStr = photo.lng?.let { "%.6f".format(it) } ?: "N/A"
//                Text("Coordinates: $latStr, $lngStr")
//                Text("App Storage Path: ${photo.uri ?: "N/A"}")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close")
            }
        }
    )
}
