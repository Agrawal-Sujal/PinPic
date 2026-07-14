package com.sunflower.pinpic.domain.map

/**
 * Minimal 2D KD-tree over parallel (x, y) coordinate arrays.
 * Stores original array indices so callers can map results back to their own data.
 *
 * Used twice in this pipeline:
 *  - During index build: radius search in normalized Mercator space to merge nearby points/clusters.
 *  - At query time: bounding-box search in lat/lon space to fetch what's on screen.
 */
class KDTree private constructor(private var root: Node?) {

    // left/right are `var` (not `val`) so insert() can graft a new leaf onto an existing node
    // without rebuilding the tree around it.
    private class Node(
        val index: Int,
        val x: Double,
        val y: Double,
        var left: Node?,
        var right: Node?
    )

    /** All indices whose (x, y) falls inside the closed box [minX,maxX] x [minY,maxY]. */
    fun rangeBBox(minX: Double, minY: Double, maxX: Double, maxY: Double): List<Int> {
        val result = ArrayList<Int>()
        fun search(node: Node?, depth: Int) {
            node ?: return
            if (node.x in minX..maxX && node.y in minY..maxY) result.add(node.index)
            val axisIsX = depth % 2 == 0
            val nodeAxis = if (axisIsX) node.x else node.y
            val minAxis = if (axisIsX) minX else minY
            val maxAxis = if (axisIsX) maxX else maxY
            // Left subtree holds axis values <= nodeAxis, right holds >= nodeAxis (see build()).
            if (minAxis <= nodeAxis) search(node.left, depth + 1)
            if (maxAxis >= nodeAxis) search(node.right, depth + 1)
        }
        search(root, 0)
        return result
    }

    /** All indices within [radius] (same units as tree coords) of (qx, qy), euclidean. */
    fun rangeRadius(qx: Double, qy: Double, radius: Double): List<Int> {
        val r2 = radius * radius
        val result = ArrayList<Int>()
        fun search(node: Node?, depth: Int) {
            node ?: return
            val dx = node.x - qx
            val dy = node.y - qy
            if (dx * dx + dy * dy <= r2) result.add(node.index)

            val axisIsX = depth % 2 == 0
            val nodeAxis = if (axisIsX) node.x else node.y
            val queryAxis = if (axisIsX) qx else qy
            val axisDiff = nodeAxis - queryAxis

            // Descend into the side containing the query point first, then only cross the
            // splitting plane into the far side if the query radius actually reaches it.
            if (queryAxis < nodeAxis) {
                search(node.left, depth + 1)
                if (axisDiff * axisDiff <= r2) search(node.right, depth + 1)
            } else {
                search(node.right, depth + 1)
                if (axisDiff * axisDiff <= r2) search(node.left, depth + 1)
            }
        }
        search(root, 0)
        return result
    }

    /**
     * Inserts a new point in O(depth) time by descending exactly like build()'s median split
     * assigns sides — alternating x/y axis per depth, going left on ties/less-than — and attaching
     * as a new leaf where it falls off the tree.
     *
     * Unlike build(), this does NOT rebalance. A tree that only ever grows via insert() (no
     * occasional rebuild()) can degenerate toward a linked list on adversarial insertion order,
     * pushing rangeBBox/rangeRadius from O(log n) toward O(n). For occasional live inserts — a few
     * new points trickling in between full rebuilds — this is a fine trade-off; if you expect a
     * large number of inserts before the next rebuild, rebuild() periodically from build() instead.
     */
    fun insert(index: Int, x: Double, y: Double) {
        val newNode = Node(index, x, y, null, null)
        val current = root
        if (current == null) {
            root = newNode
            return
        }

        var node = current
        var depth = 0
        while (true) {
            val axisIsX = depth % 2 == 0
            // Matches build()'s split: left holds axis values <= the node, right holds >=.
            val goLeft = if (axisIsX) x <= node!!.x else y <= node!!.y
            if (goLeft) {
                val left = node.left
                if (left == null) {
                    node.left = newNode
                    return
                }
                node = left
            } else {
                val right = node.right
                if (right == null) {
                    node.right = newNode
                    return
                }
                node = right
            }
            depth++
        }
    }

    companion object {
        /** Builds a balanced KD-tree by recursive median split, alternating x/y axis per depth. */
        fun build(xs: DoubleArray, ys: DoubleArray): KDTree {
            val indices = xs.indices.toMutableList()

            fun buildNode(idxList: List<Int>, depth: Int): Node? {
                if (idxList.isEmpty()) return null
                val axisIsX = depth % 2 == 0
                val sorted = idxList.sortedBy { if (axisIsX) xs[it] else ys[it] }
                val mid = sorted.size / 2
                val medianIdx = sorted[mid]
                return Node(
                    index = medianIdx,
                    x = xs[medianIdx],
                    y = ys[medianIdx],
                    left = buildNode(sorted.subList(0, mid), depth + 1),
                    right = buildNode(sorted.subList(mid + 1, sorted.size), depth + 1)
                )
            }
            return KDTree(buildNode(indices, 0))
        }
    }
}