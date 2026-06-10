package com.moonshade.shadowvillage.render.sprites

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.moonshade.shadowvillage.core.data.Element
import com.moonshade.shadowvillage.render.Palette

/** Projectiles are tiny and animated (spin), so they draw live each frame. */
object ProjectileSprites {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    /** Draws centered at (cx, cy). [spin] in radians animates shuriken. */
    fun draw(canvas: Canvas, element: Element, cx: Float, cy: Float, radius: Float, spin: Float) {
        val c = Palette.of(element)
        when (element) {
            Element.WIND, Element.FIRE -> shuriken(canvas, cx, cy, radius, spin, c.base, c.dark)
            Element.WATER -> {
                fill.color = c.light
                canvas.drawCircle(cx, cy, radius, fill)
                fill.color = c.base
                canvas.drawCircle(cx, cy, radius * 0.65f, fill)
                fill.color = Palette.EYE_WHITE
                canvas.drawCircle(cx - radius * 0.3f, cy - radius * 0.3f, radius * 0.22f, fill)
            }
            Element.EARTH -> {
                fill.color = c.base
                canvas.drawCircle(cx, cy, radius, fill)
                stroke.color = c.dark
                stroke.strokeWidth = radius * 0.25f
                canvas.drawCircle(cx - radius * 0.2f, cy + radius * 0.1f, radius * 0.45f, stroke)
            }
            Element.LIGHTNING -> Unit // lightning has no projectile
        }
    }

    private fun shuriken(canvas: Canvas, cx: Float, cy: Float, r: Float, spin: Float, base: Int, dark: Int) {
        canvas.save()
        canvas.rotate(Math.toDegrees(spin.toDouble()).toFloat(), cx, cy)
        fill.color = base
        val blade = Path()
        for (i in 0 until 4) {
            blade.reset()
            blade.moveTo(cx, cy)
            blade.lineTo(cx + r * 0.35f, cy - r * 0.4f)
            blade.lineTo(cx, cy - r * 1.1f)
            blade.lineTo(cx - r * 0.35f, cy - r * 0.4f)
            blade.close()
            canvas.drawPath(blade, fill)
            canvas.rotate(90f, cx, cy)
        }
        fill.color = dark
        canvas.drawCircle(cx, cy, r * 0.28f, fill)
        canvas.restore()
    }
}
