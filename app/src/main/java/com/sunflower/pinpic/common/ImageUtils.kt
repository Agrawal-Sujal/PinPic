package com.sunflower.pinpic.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max

object ImageUtils {

    suspend fun generateAndSaveThumbnail(
        context: Context,
        originalUriString: String,
        photoId: Long? = null,
        photoUrl: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            val thumbnailDir = File(context.filesDir, "thumbnails")
            if (!thumbnailDir.exists()) {
                thumbnailDir.mkdirs()
            }
            
            // Create a deterministic filename based on photo properties
            val fileName = "thumb_${photoId ?: System.currentTimeMillis()}_${originalUriString.hashCode()}.webp"
            val outputFile = File(thumbnailDir, fileName)
            
            if (outputFile.exists()) {
                return@withContext outputFile.absolutePath
            }

            var inputStream: InputStream? = null
            
            // Try resolving local URI or File path
            if (originalUriString.startsWith("content://") || originalUriString.startsWith("file://")) {
                inputStream = context.contentResolver.openInputStream(Uri.parse(originalUriString))
            } else if (originalUriString.startsWith("/")) {
                val file = File(originalUriString)
                if (file.exists()) {
                    inputStream = file.inputStream()
                }
            }
            
            if (inputStream == null) return@withContext null

            // First, decode bounds to calculate inSampleSize
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            // Need to open stream again for actual decoding if we consumed it
            var boundsStream = inputStream
            BitmapFactory.decodeStream(boundsStream, null, options)
            boundsStream?.close()
            
            // Re-open for actual decoding
            if (originalUriString.startsWith("content://") || originalUriString.startsWith("file://")) {
                inputStream = context.contentResolver.openInputStream(Uri.parse(originalUriString))
            } else if (originalUriString.startsWith("/")) {
                inputStream = File(originalUriString).inputStream()
            }

            // Target max dimension 200px
            val targetSize = 200
            var inSampleSize = 1
            if (options.outHeight > targetSize || options.outWidth > targetSize) {
                val halfHeight: Int = options.outHeight / 2
                val halfWidth: Int = options.outWidth / 2
                while (halfHeight / inSampleSize >= targetSize && halfWidth / inSampleSize >= targetSize) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }

            val bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            if (bitmap != null) {
                // Check EXIF orientation
                var orientation = ExifInterface.ORIENTATION_NORMAL
                if (originalUriString.startsWith("content://") || originalUriString.startsWith("file://")) {
                    context.contentResolver.openInputStream(Uri.parse(originalUriString))?.use {
                        val exif = ExifInterface(it)
                        orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    }
                } else if (originalUriString.startsWith("/")) {
                    val exif = ExifInterface(originalUriString)
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                }

                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                }

                val finalBitmap = if (matrix.isIdentity) {
                    bitmap
                } else {
                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                }

                FileOutputStream(outputFile).use { out ->
                    finalBitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
                }
                
                if (finalBitmap != bitmap) {
                    finalBitmap.recycle()
                }
                bitmap.recycle()
                
                return@withContext outputFile.absolutePath
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
