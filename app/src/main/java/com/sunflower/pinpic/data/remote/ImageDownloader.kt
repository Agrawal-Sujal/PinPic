package com.sunflower.pinpic.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageDownloader @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "ImageDownloader"
    }

    suspend fun downloadPhotoToFile(downloadUrl: String, destinationFile: File): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(downloadUrl).get().build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Download failed with HTTP ${response.code} for URL: $downloadUrl")
                        return@withContext false
                    }
                    destinationFile.parentFile?.mkdirs()
                    FileOutputStream(destinationFile).use { fos ->
                        response.body.byteStream().copyTo(fos)
                    }
                    Log.d(TAG, "Successfully downloaded photo to ${destinationFile.absolutePath}")
                    true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading photo from $downloadUrl", e)
                false
            }
        }
}
