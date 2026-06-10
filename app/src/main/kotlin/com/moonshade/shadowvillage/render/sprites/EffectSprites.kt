package com.moonshade.shadowvillage.render.sprites

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.moonshade.shadowvillage.core.math.Vec2
import com.moonshade.shadowvillage.render.Camera
import com.moonshade.shadowvillage.render.Palette
import kotlin.math.sin
import kotlin.random.Random

/** Short-lived animations spawned from core EffectEvents. */
sealed class Fx(val ttl: Float) {
    var age = 0f
    val done get() = age >= ttl
    val t get() = (age / ttl).coerceIn(0f, 1f)
}

class ArcFx(val points: List<Vec2>) : Fx(0.20f)
class RingFx(val pos: Vec2, val radius: Float, val color: Int) : Fx(0.35f)
class SparkFx(val pos: Vec2, val color: Int) : Fx(0.18f)
class PoofFx(val pos: Vec2) : Fx(0.4f)
class TextFx(val pos: Vec2, val text: String, val color: Int) : Fx(0.9f)

object EffectSprites {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val rng = Random(42)

    fun draw(canvas: Canvas, fx: Fx, cam: Camera) {
        when (fx) {
            is ArcFx -> arc(canvas, fx, cam)
            is RingFx -> ring(canvas, fx, cam)
            is SparkFx -> spark(canvas, fx, cam)
            is PoofFx -> poof(canvas, fx, cam)
            is TextFx -> text(canvas, fx, cam)
        }
    }

    private fun alpha(base: Int, a: Float): Int =
        (base and 0x00FFFFFF) or (((255 * a).toInt().coerceIn(0, 255)) shl 24)

    private fun arc(canvas: Canvas, fx: ArcFx, cam: Camera) {
        if (fx.points.size < 2) return
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = cam.cellSize * 0.10f * (1f - fx.t * 0.6f)
        paint.color = alpha(0xFFFFE68A.toInt(), 1f - fx.t)
        val path = Path()
        for (i in 0 until fx.points.size - 1) {
            val a = fx.points[i]
            val b = fx.points[i + 1]
            path.moveTo(cam.worldX(a), cam.worldY(a))
            // jittered midpoint for the zigzag look
            val mx = (a.x + b.x) / 2f + (rng.nextFloat() - 0.5f) * 0.3f
            val my = (a.y + b.y) / 2f + (rng.nextFloat() - 0.5f) * 0.3f
            path.quadTo(cam.worldX(mx), cam.worldY(my), cam.worldX(b), cam.worldY(b))
        }
        canvas.drawPath(path, paint)
        paint.strokeWidth = cam.cellSize * 0.04f
        paint.color = alpha(Palette.EYE_WHITE, 1f - fx.t)
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
    }

    private fun ring(canvas: Canvas, fx: RingFx, cam: Camera) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = cam.cellSize * 0.12f * (1f - fx.t)
        paint.color = alpha(fx.color, 1f - fx.t)
        canvas.drawCircle(
            cam.worldX(fx.pos), cam.worldY(fx.pos),
            cam.cellSize * fx.radius * (0.3f + 0.7f * fx.t), paint,
        )
        paint.style = Paint.Style.FILL
    }

    private fun spark(canvas: Canvas, fx: SparkFx, cam: Camera) {
        paint.style = Paint.Style.FILL
        paint.color = alpha(fx.color, 1f - fx.t)
        val cx = cam.worldX(fx.pos)
        val cy = cam.worldY(fx.pos)
        val r = cam.cellSize * 0.10f * (1f + fx.t)
        canvas.drawCircle(cx, cy, r, paint)
    }

    private fun poof(canvas: Canvas, fx: PoofFx, cam: Camera) {
        paint.style = Paint.Style.FILL
        paint.color = alpha(0xFFD9D4C8.toInt(), (1f - fx.t) * 0.8f)
        val cx = cam.worldX(fx.pos)
        val cy = cam.worldY(fx.pos)
        val r = cam.cellSize * (0.15f + 0.25f * fx.t)
        for (i in 0 until 4) {
            val ang = i * 1.5708f + fx.t * 2f
            canvas.drawCircle(
                cx + r * 0.9f * sin(ang), cy + r * 0.9f * sin(ang + 2.1f), r * 0.55f, paint,
            )
        }
        canvas.drawCircle(cx, cy, r * 0.7f, paint)
    }

    private fun text(canvas: Canvas, fx: TextFx, cam: Camera) {
        textPaint.textSize = cam.cellSize * 0.42f
        textPaint.color = alpha(fx.color, 1f - fx.t * fx.t)
        val cy = cam.worldY(fx.pos) - cam.cellSize * 0.7f * fx.t
        canvas.drawText(fx.text, cam.worldX(fx.pos), cy, textPaint)
    }
}
