package com.moonshade.shadowvillage.core.map

import com.moonshade.shadowvillage.core.math.Vec2

/**
 * A grid map with a single enemy path from SPAWN to GOAL. World units are
 * grid cells: cell (col, row) has its center at (col + 0.5, row + 0.5).
 */
class GameMap(
    val id: String,
    val name: String,
    val cols: Int,
    val rows: Int,
    private val tiles: List<List<TileType>>,
    /** Cell centers along the path, from spawn to goal inclusive. */
    val waypoints: List<Vec2>,
) {
    /** Cumulative path distance at each waypoint; starts at 0. */
    private val cumulative: FloatArray = FloatArray(waypoints.size).also { acc ->
        for (i in 1 until waypoints.size) {
            acc[i] = acc[i - 1] + waypoints[i].distanceTo(waypoints[i - 1])
        }
    }

    val totalPathLength: Float = cumulative.last()

    fun isInside(col: Int, row: Int) = col in 0 until cols && row in 0 until rows

    fun tileAt(col: Int, row: Int): TileType = tiles[row][col]

    fun isBuildable(col: Int, row: Int) = isInside(col, row) && tileAt(col, row) == TileType.BUILDABLE

    /** World position for a scalar distance along the path, clamped to its ends. */
    fun positionAt(distance: Float): Vec2 {
        if (distance <= 0f) return waypoints.first()
        if (distance >= totalPathLength) return waypoints.last()
        var i = cumulative.indexOfFirst { it >= distance }
        if (i <= 0) i = 1
        val segLen = cumulative[i] - cumulative[i - 1]
        val t = if (segLen < 1e-6f) 0f else (distance - cumulative[i - 1]) / segLen
        return Vec2.lerp(waypoints[i - 1], waypoints[i], t)
    }
}
