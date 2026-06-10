package com.moonshade.shadowvillage.render.sprites

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.moonshade.shadowvillage.render.Palette

/** Small HUD icons, drawn centered at (cx, cy) with half-extent r. */
object UiSprites {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    fun coin(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        fill.color = Palette.GOLD
        canvas.drawCircle(cx, cy, r, fill)
        fill.color = 0xFFD4A50F.toInt()
        canvas.drawCircle(cx, cy, r * 0.68f, fill)
        fill.color = Palette.GOLD
        canvas.drawCircle(cx, cy, r * 0.40f, fill)
    }

    fun heart(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        fill.color = Palette.LIFE
        val path = Path().apply {
            moveTo(cx, cy + r * 0.9f)
            cubicTo(cx - r * 1.4f, cy - r * 0.1f, cx - r * 0.7f, cy - r * 1.1f, cx, cy - r * 0.3f)
            cubicTo(cx + r * 0.7f, cy - r * 1.1f, cx + r * 1.4f, cy - r * 0.1f, cx, cy + r * 0.9f)
            close()
        }
        canvas.drawPath(path, fill)
    }

    fun pause(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        fill.color = color
        canvas.drawRoundRect(RectF(cx - r * 0.6f, cy - r * 0.7f, cx - r * 0.15f, cy + r * 0.7f), r * 0.1f, r * 0.1f, fill)
        canvas.drawRoundRect(RectF(cx + r * 0.15f, cy - r * 0.7f, cx + r * 0.6f, cy + r * 0.7f), r * 0.1f, r * 0.1f, fill)
    }

    fun play(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        fill.color = color
        val path = Path().apply {
            moveTo(cx - r * 0.45f, cy - r * 0.7f)
            lineTo(cx + r * 0.75f, cy)
            lineTo(cx - r * 0.45f, cy + r * 0.7f)
            close()
        }
        canvas.drawPath(path, fill)
    }

    fun flag(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        fill.color = color
        canvas.drawRect(cx - r * 0.55f, cy - r * 0.8f, cx - r * 0.40f, cy + r * 0.9f, fill)
        val path = Path().apply {
            moveTo(cx - r * 0.40f, cy - r * 0.8f)
            lineTo(cx + r * 0.75f, cy - r * 0.45f)
            lineTo(cx - r * 0.40f, cy - r * 0.1f)
            close()
        }
        canvas.drawPath(path, fill)
    }
}
