package com.sunflower.pinpic.domain.map

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin

/** A raw input point before clustering. [payload] carries whatever your caller needs (Wikidata id, etc). */
data class GeoPoint(
    val id: String,
    val lat: Double,
    val lon: Double,
    val count: Int = 1,
    val payload: Any? = null,
    val thumbnailUri:String?
)

/**
 * A node in the precomputed cluster hierarchy — either a real point (count == 1, [pointId] set)
 * or an aggregate cluster (count > 1, [childIds] set, pointing at the layer one zoom higher).
 *
 * [zoom] is the zoom level at which this node was formed (i.e. the coarsest zoom it's visible at
 * as exactly this cluster — at lower zoom it will have been merged into something bigger).
 */
data class ClusterNode(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val count: Int,
    val zoom: Int,
    val pointId: String? = null,
    val childIds: LongArray? = null,
    val thumbnailUri: String?
) {
    val isCluster: Boolean get() = pointId == null

    override fun equals(other: Any?) = other is ClusterNode && id == other.id
    override fun hashCode() = id.hashCode()
}

data class BBox(val minLon: Double, val minLat: Double, val maxLon: Double, val maxLat: Double)

/**
 * Precomputed hierarchical clustering (Approach 3).
 *
 * Build once, off the UI thread, from the full point set. After that, every camera-idle event is
 * just a bounding-box lookup into the layer for the current zoom — no live clustering math, so it
 * can safely run even on a slower dispatcher without risking a frame drop.
 */
class GeoClusterIndex private constructor(
    private val layers: Map<Int, Layer>,
    private val nodesById: MutableMap<Long, ClusterNode>,
    val minZoom: Int,
    val maxZoom: Int,
    private var nextIdCounter: Long
) {
    // nodes is a MutableList (not List) so addPoint() can append a new leaf and reference it by
    // its list position as the KD-tree's stored index.
    private class Layer(val nodes: MutableList<ClusterNode>, val tree: KDTree)

    /** Everything visible in [bbox] at [zoom]. Clamp zoom yourself upstream if you want hysteresis. */
    fun getClusters(bbox: BBox, zoom: Int): List<ClusterNode> {
        val z = zoom.coerceIn(minZoom, maxZoom)
        val layer = layers[z] ?: return emptyList()
        return layer.tree.rangeBBox(bbox.minLon, bbox.minLat, bbox.maxLon, bbox.maxLat)
            .map { layer.nodes[it] }
    }

    /** Direct children of a cluster (one zoom level finer). Use for tap-to-expand UX. */
    fun getChildren(clusterId: Long): List<ClusterNode> {
        val node = nodesById[clusterId] ?: return emptyList()
        val ids = node.childIds ?: return emptyList()
        return ids.map { nodesById[it]!! }
    }

    /** Fully expands a cluster down to its leaf points (e.g. for "show all N here" list view). */
    fun getLeaves(clusterId: Long): List<ClusterNode> {
        val result = ArrayList<ClusterNode>()
        fun expand(id: Long) {
            val node = nodesById[id] ?: return
            val kids = node.childIds
            if (kids == null) {
                result.add(node)
            } else {
                kids.forEach { expand(it) }
            }
        }
        expand(clusterId)
        return result
    }

    /**
     * Adds one new point to the live index without recomputing the clustering, using
     * [KDTree.insert] under the hood. The point is inserted as its own standalone (unmerged) leaf
     * into every zoom layer, so it shows up immediately at every zoom — including layers where it
     * would "really" belong inside a nearby cluster. It'll get folded into the correct cluster the
     * next time you call [build] from scratch; until then it just renders as an extra individual
     * point. That trade-off is what makes this O(zoom levels × log n) instead of a full rebuild.
     *
     * Safe to call repeatedly as new points stream in (e.g. from dummy/live data on a timer).
     */
    fun addPoint(point: GeoPoint) {
        val id = nextIdCounter++
        val leaf = ClusterNode(
            id = id,
            lat = point.lat,
            lon = point.lon,
            count = 1,
            zoom = maxZoom + 1,
            pointId = point.id,
            thumbnailUri = point.thumbnailUri
        )
        nodesById[id] = leaf

        for (z in minZoom..maxZoom) {
            val layer = layers[z] ?: continue
            val newIndex = layer.nodes.size
            layer.nodes.add(leaf)
            // Layers are built from (lon, lat) arrays — see build() below — so insert() must be
            // called with the same axis order the tree was built with.
            layer.tree.insert(newIndex, point.lon, point.lat)
        }
    }

    companion object {

        /**
         * @param radiusPixels cluster radius in screen pixels at any given zoom — tune this, not a
         *   geographic distance, so cluster density looks consistent across zoom levels.
         * @param extent assumed tile size in pixels (matches standard 512px vector tiles).
         */
        fun build(
            points: List<GeoPoint>,
            minZoom: Int = 0,
            maxZoom: Int = 16,
            radiusPixels: Double = 10.0,
            extent: Int = 512
        ): GeoClusterIndex {
            var nextIdCounter = 0L
            fun nextId() = nextIdCounter++

            // Internal working node kept in normalized Mercator (x,y in [0,1)) for radius merging.
            data class Working(
                val id: Long,
                val x: Double,
                val y: Double,
                val count: Int,
                val pointId: String?,
                val childIds: LongArray?,
                val zoom: Int,
                val thumbnailUri:String?
            )

            var current: List<Working> = points.map { p ->
                Working(
                    id = nextId(),
                    x = lngX(p.lon),
                    y = latY(p.lat),
                    count = p.count,
                    pointId = p.id,
                    childIds = null,
                    zoom = maxZoom + 1, // leaves are always "visible" as themselves at max zoom
                    thumbnailUri = p.thumbnailUri
                )
            }

            val layers = HashMap<Int, Layer>()
            val nodesById = HashMap<Long, ClusterNode>()

            // Seed nodesById with leaves so getChildren/getLeaves can resolve them regardless of layer.
            current.forEach { w ->
                nodesById[w.id] = ClusterNode(w.id, yLat(w.y), xLng(w.x), w.count, w.zoom, w.pointId, w.childIds,w.thumbnailUri)
            }

            for (z in maxZoom downTo minZoom) {
                val xs = DoubleArray(current.size) { current[it].x }
                val ys = DoubleArray(current.size) { current[it].y }
                val tree = KDTree.build(xs, ys)
                val visited = BooleanArray(current.size)
                val radius = radiusPixels / (extent * 2.0.pow(z))

                val next = ArrayList<Working>(current.size)
                for (i in current.indices) {
                    if (visited[i]) continue
                    val neighbors = tree.rangeRadius(current[i].x, current[i].y, radius)
                        .filter { !visited[it] }
                    neighbors.forEach { visited[it] = true }

                    if (neighbors.size == 1) {
                        // No merge at this zoom — carry the same node forward unchanged so its id
                        // (and therefore any UI/animation keyed on it) stays stable across zooms.
                        next.add(current[i])
                    } else {
                        var sumX = 0.0; var sumY = 0.0; var sumCount = 0
                        neighbors.forEach { idx ->
                            val n = current[idx]
                            sumX += n.x * n.count
                            sumY += n.y * n.count
                            sumCount += n.count
                        }
                        val cx = sumX / sumCount
                        val cy = sumY / sumCount
                        val merged = Working(
                            id = nextId(),
                            x = cx,
                            y = cy,
                            count = sumCount,
                            pointId = null,
                            childIds = neighbors.map { current[it].id }.toLongArray(),
                            zoom = z,
                            thumbnailUri = null
                        )
                        next.add(merged)
                        nodesById[merged.id] = ClusterNode(
                            merged.id, yLat(cy), xLng(cx), merged.count, merged.zoom,
                            null, merged.childIds,merged.thumbnailUri
                        )
                    }
                }

                // Store this zoom's layer using lat/lon coords so runtime queries take a plain
                // geographic bbox with no projection step on the hot path.
                val layerNodes = next.map { w ->
                    nodesById[w.id] ?: ClusterNode(w.id, yLat(w.y), xLng(w.x), w.count, w.zoom, w.pointId, w.childIds,w.thumbnailUri)
                }.toMutableList()
                val lonArr = DoubleArray(layerNodes.size) { layerNodes[it].lon }
                val latArr = DoubleArray(layerNodes.size) { layerNodes[it].lat }
                layers[z] = Layer(layerNodes, KDTree.build(lonArr, latArr))

                current = next
            }

            return GeoClusterIndex(layers, nodesById, minZoom, maxZoom, nextIdCounter)
        }

        // --- Spherical Mercator projection, matches the standard slippy-map tile scheme ---
        private fun lngX(lng: Double): Double = lng / 360.0 + 0.5
        private fun latY(lat: Double): Double {
            val s = sin(lat * PI / 180.0)
            val y = 0.5 - 0.25 * ln((1 + s) / (1 - s)) / PI
            return y.coerceIn(0.0, 1.0)
        }
        private fun xLng(x: Double): Double = (x - 0.5) * 360.0
        private fun yLat(y: Double): Double {
            val y2 = (180 - y * 360) * PI / 180
            return 360.0 * atan(exp(y2)) / PI - 90.0
        }
    }
}