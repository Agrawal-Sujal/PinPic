package com.sunflower.pinpic.presentation.map.components

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.LruCache
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.core.graphics.createBitmap

/**
 * Pre-renders marker icons as plain Android Bitmaps instead of going through Compose's
 * MarkerComposable, which snapshots a full composable subtree (layout + draw) per marker. That's
 * cheap for a few markers but expensive enough, done hundreds of times in one frame, to visibly
 * freeze the UI — which is exactly the "many individual points visible at once" case.
 *
 * The point icon is built once and reused everywhere. Cluster icons are cached by
 * (size bucket, label) since many clusters share the same rounded count, so most cluster markers
 * hit the cache after the first few are drawn.
 */
class MarkerIconFactory(private val density: Float) {

    private val clusterCache = LruCache<String, BitmapDescriptor>(64)
    private val pointIcon: BitmapDescriptor by lazy { buildPointIcon() }

    fun point(): BitmapDescriptor = pointIcon

    fun cluster(count: Int): BitmapDescriptor {
        val bucket = bucketFor(count)
        val label = formatCount(count)
        val key = "$bucket:$label"
        clusterCache.get(key)?.let { return it }
        val icon = buildClusterIcon(bucket, label)
        clusterCache.put(key, icon)
        return icon
    }

    /**
     * Pre-generates every icon this factory can produce, given [formatCount] only emits a small
     * fixed vocabulary of labels. Call once off the main thread (e.g. Dispatchers.Default) before
     * any markers render, so a first-time-in-this-area pan never has to draw a bitmap synchronously
     * on the main thread.
     */
    fun warmUp() {
        point()
        val representativeCounts = (1..9) +
                (10..90 step 10) +
                (100..900 step 100) +
                (1_000..9_000 step 1_000) +
                listOf(15_000, 50_000)
        representativeCounts.forEach { cluster(it) }
    }

    private fun bucketFor(count: Int) = when {
        count < 10 -> 0
        count < 100 -> 1
        count < 1_000 -> 2
        else -> 3
    }

    private fun buildPointIcon(): BitmapDescriptor {
        val size = dp(16)
        val bmp = createBitmap(size, size)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0x34, 0xA8, 0x53) } // Green
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        return BitmapDescriptorFactory.fromBitmap(bmp)
    }

    private fun buildClusterIcon(bucket: Int, label: String): BitmapDescriptor {
        val diameterDp = when (bucket) {
            0 -> 34
            1 -> 40
            2 -> 46
            else -> 52
        }
        val size = dp(diameterDp)
        val bmp = createBitmap(size, size)
        val canvas = Canvas(bmp)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0xCC, 0x55, 0x00) }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bg)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = size * 0.32f
            isFakeBoldText = true
        }
        val textY = size / 2f - (text.descent() + text.ascent()) / 2f
        canvas.drawText(label, size / 2f, textY, text)
        return BitmapDescriptorFactory.fromBitmap(bmp)
    }

    private fun dp(value: Int): Int = (value * density).toInt().coerceAtLeast(1)

    private fun formatCount(count: Int): String = when {
        count < 10 -> count.toString()
        count < 100 -> "${(count / 10) * 10}+"
        count < 1_000 -> "${(count / 100) * 100}+"
        count < 1_000_000 -> "${count / 1_000}K+"
        else -> "${count / 1_000_000}M+"
    }
}