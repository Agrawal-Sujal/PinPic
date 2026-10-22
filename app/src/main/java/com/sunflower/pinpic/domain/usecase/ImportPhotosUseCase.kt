package com.sunflower.pinpic.domain.usecase

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.sunflower.pinpic.data.local.datastore.DataStoreManager
import com.sunflower.pinpic.domain.map.MapClusteringSystem
import com.sunflower.pinpic.domain.model.Cluster
import com.sunflower.pinpic.domain.model.Location
import com.sunflower.pinpic.domain.model.Photo
import com.sunflower.pinpic.domain.repository.PhotoRepository
import com.sunflower.pinpic.presentation.readImageInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject


class ImportPhotosUseCase @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val dataStoreManager: DataStoreManager,
    @ApplicationContext private val context: Context,
    private val mapClusteringSystem: MapClusteringSystem
) {
    suspend operator fun invoke(
        uris: List<Uri>,
        onProgress: suspend (current: Int, total: Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext

        val userId = dataStoreManager.userIdFlow.first() ?: "unknown"
        val resolver = context.contentResolver
        val picturesDir = (context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir).apply {
            mkdirs()
        }
        val defaultDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        uris.forEachIndexed { index, uri ->
            try {
                // 1. Get image metadata using the process from ImageMetadataScreen
                val info = readImageInfo(context, uri)
                Log.d("ImportPhotoUseCase",info.toString())
                // 2. Insert image into app storage
                val safeName = info.name.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                val fileName = if (safeName.isNotBlank() && safeName != "Unknown") {
                    "PinPic-${System.currentTimeMillis()}-$index-$safeName"
                } else {
                    "PinPic-Import-${System.currentTimeMillis()}-$index.jpg"
                }

                val destFile = File(picturesDir, fileName)
                resolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(destFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                // 3. Store location and info
                val location = if (info.latitude != null && info.longitude != null) {
                    Location(
                        latitude = info.latitude,
                        longitude = info.longitude,
                        locationName = "Unknown Location",
                        isEdited = false
                    )
                } else {
                    null
                }

                val currentDateStr = defaultDateFormatter.format(Date())
                val photo = Photo(
                    userId = userId,
                    photoName = info.name.takeIf { it.isNotBlank() && it != "Unknown" } ?: fileName,
                    takenAt = currentDateStr,
                    editedAt = currentDateStr,
                    location = location,
                    photoUrl = destFile.absolutePath
                )

                // 4. Store in repository
                val photoId = photoRepository.savePhoto(photo)
                if (location != null) {
                    val savedPhoto = photo.copy(localPhotoId = photoId)
                    val cluster = Cluster(
                        location = location,
                        photoCount = 1,
                        thumbnail = savedPhoto,
                        photos = listOf(savedPhoto)
                    )
                    val clusterId = photoRepository.saveCluster(cluster)
                    photoRepository.addPhotosInCluster(listOf(savedPhoto), cluster.copy(localClusterId = clusterId))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                onProgress(index + 1, uris.size)
            }
        }
        mapClusteringSystem.rebuild()
    }
}
