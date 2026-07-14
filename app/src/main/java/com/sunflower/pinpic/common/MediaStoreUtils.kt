package com.sunflower.pinpic.common

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.sunflower.pinpic.domain.model.Photo
import java.io.File
import java.io.FileInputStream

object MediaStoreUtils {

    fun savePhotosToDevice(context: Context, photos: List<Photo>) {
        photos.forEach { photo ->
            photo.uri?.let { uriString ->
                try {
                    val file = File(uriString)
                    if (file.exists()) {
                        saveImageToMediaStore(context, file)
                    }
                } catch (e: Exception) {
                    Log.e("MediaStoreUtils", "Failed to save photo: ${e.message}", e)
                }
            }
        }
    }

    private fun saveImageToMediaStore(context: Context, file: File) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/PinPic")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            } else {
                // For older versions, we could use Environment.getExternalStoragePublicDirectory
                // but PinPic is targeting modern Android versions mostly. 
                // We'll leave the older implementation simple or unsupported if not needed.
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val uri = resolver.insert(collection, contentValues)

        uri?.let {
            try {
                resolver.openOutputStream(it)?.use { outputStream ->
                    FileInputStream(file).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                }
            } catch (e: Exception) {
                resolver.delete(it, null, null)
                throw e
            }
        }
    }
}
