package com.sunflower.pinpic.data.remote

import android.content.Context
import android.net.Uri
import com.sunflower.pinpic.data.remote.api.S3Api
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream
import javax.inject.Inject

class ImageUploader @Inject constructor(
    private val s3Api: S3Api,
    @ApplicationContext private val context: Context
) {

    suspend fun uploadPhotoToPresignedUrl(
        presignedUrl: String,
        imageUri: Uri
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Resolve MIME type (must match what was signed on the server)
                val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
                val mediaType = mimeType.toMediaTypeOrNull()

                // 2. Read bytes from Uri
                val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
                val bytes = inputStream?.use { it.readBytes() }
                    ?: throw IllegalArgumentException("Could not read file from URI")

                // 3. Create raw RequestBody (no MultipartBody.Part)
                val requestBody = bytes.toRequestBody(mediaType)

                val response = s3Api.uploadToPresignedUrl(
                    uploadUrl = presignedUrl,
                    contentType = mimeType,
                    imageBody = requestBody
                )


                // 4. Send directly to the pre-signed destination
//                val request = Request.Builder()
//                    .url(presignedUrl)
//                    .put(requestBody)
//                    .build()
//
//                val response = okHttpClient.newCall(request).execute()

                response.isSuccessful
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}