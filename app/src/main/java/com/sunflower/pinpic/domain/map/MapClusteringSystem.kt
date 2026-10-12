package com.sunflower.pinpic.domain.map

import android.util.Log
import com.sunflower.pinpic.data.repository.LocalPhotoRepository
import com.sunflower.pinpic.domain.model.Cluster
import kotlinx.coroutines.Delay
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class MapClusteringSystem @Inject constructor(private val localPhotoRepository: LocalPhotoRepository) {
    private var index: GeoClusterIndex? = null
    private var extraPhotos = 0


    companion object {
        var EXTRA_PHOTO_LIMIT = 20
    }

    suspend fun build() {
        Log.d("MapClusteringSystem", "Build started")
//        delay(10000.milliseconds)
        val clusters = localPhotoRepository.getUnMergedCluster()

        index = GeoClusterIndex.build(clusters, minZoom = 0, maxZoom = 21, radiusPixels = 40.0)
        Log.d("MapClusteringSystem", "Build ended")

    }

    suspend fun rebuild() {
        build()
    }

    suspend fun addClusters(clusters: List<Cluster>) {
        rebuild()
//        extraPhotos += clusters.size
//        if (extraPhotos > EXTRA_PHOTO_LIMIT) {
//            rebuild()
//            extraPhotos = 0
//        }
//        else{
//            for(cluster in clusters){
//                index?.addPoint(cluster)
//            }
//        }
    }

    fun getClusters(bbox: BBox, zoom: Int): List<ClusterNode> {
        return index!!.getClusters(bbox, zoom)
    }

}