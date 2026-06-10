package com.moonshade.shadowvillage.input

import android.graphics.Canvas
import android.graphics.Paint
import com.moonshade.shadowvillage.core.data.Element
import com.moonshade.shadowvillage.core.data.TowerData
import com.moonshade.shadowvillage.render.Camera
import com.moonshade.shadowvillage.render.Palette
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Radial element picker shown around a tapped buildable cell.
 * Stateless geometry: pass the cell; it lays the five icons in an arc.
 */
class BuildMenu(val col: Int, val row: Int) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private fun iconRadius(cam: Camera) = cam.cellSize * 0.52f
    private fun orbitRadius(cam: Camera) = cam.cellSize * 1.35f

    private fun iconCenter(cam: Camera, index: Int): Pair<Float, Float> {
        // arc over the top of the cell: -150deg .. -30deg
        val angle = Math.toRadians((-150.0 + 30.0 * index))
        val cx = cam.worldX(col + 0.5f) + (orbitRadius(cam) * cos(angle)).toFloat()
        val cy = cam.worldY(row + 0.5f) + (orbitRadius(cam) * sin(angle)).toFloat()
        return cx to cy
    }

    /** Returns the tapped element, or null if the tap missed every icon. */
    fun hitTest(cam: Camera, x: Float, y: Float): Element? {
        for ((i, element) in Element.entries.withIndex()) {
            val (cx, cy) = iconCenter(cam, i)
            if (hypot(x - cx, y - cy) <= iconRadius(cam) * 1.15f) return element
        }
        return null
    }

    fun draw(canvas: Canvas, cam: Camera, gold: Int) {
        val r = iconRadius(cam)

        // highlight the target cell
        paint.color = Palette.VALID_CELL
        canvas.drawRect(
            cam.worldX(col.toFloat()), cam.worldY(row.toFloat()),
            cam.worldX(col + 1f), cam.worldY(row + 1f), paint,
        )

        for ((i, element) in Element.entries.withIndex()) {
            val (cx, cy) = iconCenter(cam, i)
            val cost = TowerData.buildCost(element)
            val affordable = gold >= cost
            val colors = Palette.of(element)

            paint.color = if (affordable) colors.base else Palette.BUTTON_DISABLED
            canvas.drawCircle(cx, cy, r, paint)
            paint.color = Palette.OUTLINE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = r * 0.12f
            canvas.drawCircle(cx, cy, r, paint)
            paint.style = Paint.Style.FILL

            text.color = if (affordable) Palette.OUTLINE else 0xFF77808C.toInt()
            text.textSize = r * 0.52f
            canvas.drawText(element.displayName.take(4), cx, cy - r * 0.05f, text)
            text.textSize = r * 0.46f
            canvas.drawText("$cost", cx, cy + r * 0.5f, text)
        }
    }
}
