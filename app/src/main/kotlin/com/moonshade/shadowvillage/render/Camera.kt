package com.moonshade.shadowvillage.render

import com.moonshade.shadowvillage.core.map.GameMap
import com.moonshade.shadowvillage.core.math.Vec2

/**
 * Maps the grid world (cells) onto the screen (pixels), centered with
 * letterboxing. World unit = one cell.
 */
class Camera(private val map: GameMap) {

    var cellSize = 1f
        private set
    var offsetX = 0f
        private set
    var offsetY = 0f
        private set

    fun onSizeChanged(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        cellSize = minOf(width.toFloat() / map.cols, height.toFloat() / map.rows)
        offsetX = (width - cellSize * map.cols) / 2f
        offsetY = (height - cellSize * map.rows) / 2f
    }

    fun worldX(x: Float) = offsetX + x * cellSize
    fun worldY(y: Float) = offsetY + y * cellSize
    fun worldX(v: Vec2) = worldX(v.x)
    fun worldY(v: Vec2) = worldY(v.y)

    fun screenToCol(px: Float): Int = ((px - offsetX) / cellSize).toInt().let {
        if (px < offsetX) -1 else it
    }

    fun screenToRow(py: Float): Int = ((py - offsetY) / cellSize).toInt().let {
        if (py < offsetY) -1 else it
    }
}
