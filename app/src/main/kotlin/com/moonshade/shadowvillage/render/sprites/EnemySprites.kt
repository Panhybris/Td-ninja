package com.moonshade.shadowvillage.render.sprites

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.moonshade.shadowvillage.core.data.EnemyType
import com.moonshade.shadowvillage.render.Palette
import kotlin.math.sin

/**
 * Enemy sprites with distinct silhouettes per type and a 4-frame walk
 * cycle. [frame] 0..3 selects the gait keyframe; the renderer derives it
 * from pathDistance so slowed enemies trudge and stunned ones freeze.
 */
object EnemySprites {

    const val WALK_FRAMES = 4

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    fun draw(canvas: Canvas, type: EnemyType, size: Float, frame: Int) {
        stroke.color = Palette.OUTLINE
        stroke.strokeWidth = size * 0.05f
        // cycle in [-1, 1]: limb swing; |cycle| also drives body bob
        val phase = (frame % WALK_FRAMES) / WALK_FRAMES.toFloat()
        val cycle = sin(phase * 2f * Math.PI).toFloat()
        when (type) {
            EnemyType.SCOUT -> humanoid(canvas, size, 0xFF8C6BAE.toInt(), 0.78f, scarf = true, cycle)
            EnemyType.NINJA -> humanoid(canvas, size, 0xFF5E5673.toInt(), 0.95f, scarf = false, cycle)
            EnemyType.WOLF -> wolf(canvas, size, cycle)
            EnemyType.BRUTE -> brute(canvas, size, frame)
            EnemyType.RAIDER -> raider(canvas, size, cycle)
            EnemyType.MENDER -> mender(canvas, size, cycle)
            EnemyType.ONI_VANGUARD -> oni(canvas, size, 0xFF5B8C5A.toInt(), cycle)
            EnemyType.ONI_WARLORD -> oni(canvas, size, 0xFFB8413C.toInt(), cycle)
        }
    }

    /** Hooded rogue with alternating legs and a counter-swinging arm. */
    private fun humanoid(canvas: Canvas, size: Float, suit: Int, scale: Float, scarf: Boolean, cycle: Float) {
        val cx = size / 2f
        val s = size * scale
        val pad = (size - s) / 2f
        val bob = -Math.abs(cycle) * s * 0.025f

        // legs: stubs that scissor with the cycle
        fill.color = suit
        val legTop = pad + s * 0.80f + bob
        val legLen = s * 0.16f
        canvas.drawRoundRect(
            RectF(cx - s * 0.16f + cycle * s * 0.07f, legTop, cx - s * 0.04f + cycle * s * 0.07f, legTop + legLen),
            s * 0.03f, s * 0.03f, fill,
        )
        canvas.drawRoundRect(
            RectF(cx + s * 0.04f - cycle * s * 0.07f, legTop, cx + s * 0.16f - cycle * s * 0.07f, legTop + legLen),
            s * 0.03f, s * 0.03f, fill,
        )

        // body
        val body = RectF(cx - s * 0.20f, pad + s * 0.46f + bob, cx + s * 0.20f, pad + s * 0.84f + bob)
        canvas.drawRoundRect(body, s * 0.10f, s * 0.10f, fill)
        canvas.drawRoundRect(body, s * 0.10f, s * 0.10f, stroke)

        // swinging arm
        fill.color = suit
        canvas.drawCircle(cx - s * 0.24f, pad + s * (0.62f - cycle * 0.05f) + bob, s * 0.07f, fill)

        if (scarf) {
            fill.color = 0xFFD96A6A.toInt()
            val flutter = cycle * s * 0.05f
            val tail = Path().apply {
                moveTo(cx - s * 0.15f, pad + s * 0.50f + bob)
                lineTo(cx - s * 0.48f, pad + s * 0.62f + flutter + bob)
                lineTo(cx - s * 0.40f, pad + s * 0.72f + flutter + bob)
                lineTo(cx - s * 0.12f, pad + s * 0.60f + bob)
                close()
            }
            canvas.drawPath(tail, fill)
        }

        // head: hooded, eyes only
        val headR = s * 0.26f
        val headCy = pad + s * 0.26f + bob
        fill.color = suit
        canvas.drawCircle(cx, headCy, headR, fill)
        canvas.drawCircle(cx, headCy, headR, stroke)
        fill.color = Palette.EYE_WHITE
        val slit = RectF(cx - headR * 0.60f, headCy - headR * 0.18f, cx + headR * 0.60f, headCy + headR * 0.18f)
        canvas.drawRoundRect(slit, headR * 0.2f, headR * 0.2f, fill)
        fill.color = Palette.EYE_DARK
        canvas.drawCircle(cx - headR * 0.28f, headCy, headR * 0.10f, fill)
        canvas.drawCircle(cx + headR * 0.28f, headCy, headR * 0.10f, fill)
    }

    /** Quadruped gallop: legs gather and extend, tail bounces. */
    private fun wolf(canvas: Canvas, size: Float, cycle: Float) {
        val s = size
        val dip = Math.abs(cycle) * s * 0.03f
        fill.color = 0xFF4A4458.toInt()
        // legs first (behind torso): front pair and back pair scissor
        for ((i, x) in listOf(0.20f, 0.34f, 0.56f, 0.68f).withIndex()) {
            val pairSwing = if (i < 2) cycle else -cycle
            val lx = s * x + pairSwing * s * 0.05f
            canvas.drawRoundRect(RectF(lx, s * 0.64f, lx + s * 0.08f, s * 0.88f - Math.abs(pairSwing) * s * 0.06f), s * 0.02f, s * 0.02f, fill)
        }
        // torso
        val torso = RectF(s * 0.12f, s * 0.42f + dip, s * 0.78f, s * 0.72f + dip * 0.5f)
        canvas.drawRoundRect(torso, s * 0.14f, s * 0.14f, fill)
        canvas.drawRoundRect(torso, s * 0.14f, s * 0.14f, stroke)
        // bouncing tail
        val tail = Path().apply {
            moveTo(s * 0.14f, s * 0.48f + dip)
            quadTo(s * 0.00f, s * (0.34f - cycle * 0.04f), s * 0.10f, s * (0.24f - cycle * 0.06f))
            quadTo(s * 0.16f, s * 0.40f, s * 0.24f, s * 0.46f + dip)
            close()
        }
        canvas.drawPath(tail, fill)
        // head
        fill.color = 0xFF564F66.toInt()
        canvas.drawCircle(s * 0.78f, s * 0.40f + dip, s * 0.17f, fill)
        canvas.drawCircle(s * 0.78f, s * 0.40f + dip, s * 0.17f, stroke)
        fill.color = 0xFF4A4458.toInt()
        canvas.drawRoundRect(RectF(s * 0.86f, s * 0.38f + dip, s * 1.00f, s * 0.48f + dip), s * 0.04f, s * 0.04f, fill)
        val ear = Path().apply {
            moveTo(s * 0.70f, s * 0.28f + dip); lineTo(s * 0.66f, s * 0.10f + dip); lineTo(s * 0.80f, s * 0.24f + dip); close()
        }
        canvas.drawPath(ear, fill)
        fill.color = 0xFFFFD23F.toInt()
        canvas.drawCircle(s * 0.82f, s * 0.36f + dip, s * 0.035f, fill)
    }

    /** Heavy two-beat stomp: the whole hulk dips on alternating frames. */
    private fun brute(canvas: Canvas, size: Float, frame: Int) {
        val s = size
        val cx = s / 2f
        val dip = if (frame % 2 == 1) s * 0.035f else 0f
        val lean = (if (frame >= 2) 1 else -1) * s * 0.012f
        // legs
        fill.color = 0xFF565963.toInt()
        canvas.drawRoundRect(RectF(cx - s * 0.28f + lean, s * 0.82f, cx - s * 0.08f + lean, s * 0.96f), s * 0.03f, s * 0.03f, fill)
        canvas.drawRoundRect(RectF(cx + s * 0.08f - lean, s * 0.82f, cx + s * 0.28f - lean, s * 0.96f), s * 0.03f, s * 0.03f, fill)
        // torso
        fill.color = 0xFF6E7079.toInt()
        val torso = RectF(s * 0.14f, s * 0.34f + dip, s * 0.86f, s * 0.88f + dip * 0.4f)
        canvas.drawRoundRect(torso, s * 0.10f, s * 0.10f, fill)
        canvas.drawRoundRect(torso, s * 0.10f, s * 0.10f, stroke)
        fill.color = 0xFF8B8E98.toInt()
        canvas.drawRoundRect(RectF(s * 0.30f, s * 0.48f + dip, s * 0.70f, s * 0.84f + dip * 0.4f), s * 0.08f, s * 0.08f, fill)
        // pauldrons roll with the lean
        fill.color = 0xFF565963.toInt()
        canvas.drawRoundRect(RectF(s * 0.04f, s * 0.30f + dip - lean, s * 0.30f, s * 0.50f + dip - lean), s * 0.06f, s * 0.06f, fill)
        canvas.drawRoundRect(RectF(s * 0.70f, s * 0.30f + dip + lean, s * 0.96f, s * 0.50f + dip + lean), s * 0.06f, s * 0.06f, fill)
        // head
        fill.color = 0xFFB99B72.toInt()
        canvas.drawCircle(cx, s * 0.26f + dip, s * 0.14f, fill)
        canvas.drawCircle(cx, s * 0.26f + dip, s * 0.14f, stroke)
        fill.color = Palette.EYE_DARK
        canvas.drawCircle(cx - s * 0.05f, s * 0.25f + dip, s * 0.025f, fill)
        canvas.drawCircle(cx + s * 0.05f, s * 0.25f + dip, s * 0.025f, fill)
    }

    /** Glider tilts side to side; legs sway with the wind. */
    private fun raider(canvas: Canvas, size: Float, cycle: Float) {
        val s = size
        val cx = s / 2f
        canvas.save()
        canvas.rotate(cycle * 6f, cx, s * 0.4f)
        fill.color = 0xFFCB7B3E.toInt()
        val wing = Path().apply {
            moveTo(cx, s * 0.10f)
            lineTo(s * 0.96f, s * 0.42f)
            lineTo(cx, s * 0.32f)
            lineTo(s * 0.04f, s * 0.42f)
            close()
        }
        canvas.drawPath(wing, fill)
        canvas.drawPath(wing, stroke)
        fill.color = 0xFF5E5673.toInt()
        canvas.drawRoundRect(RectF(cx - s * 0.12f, s * 0.40f, cx + s * 0.12f, s * 0.70f), s * 0.06f, s * 0.06f, fill)
        val headR = s * 0.14f
        canvas.drawCircle(cx, s * 0.38f, headR, fill)
        canvas.drawCircle(cx, s * 0.38f, headR, stroke)
        fill.color = Palette.EYE_WHITE
        canvas.drawRoundRect(RectF(cx - headR * 0.55f, s * 0.36f, cx + headR * 0.55f, s * 0.41f), s * 0.02f, s * 0.02f, fill)
        stroke.strokeWidth = s * 0.05f
        canvas.drawLine(cx - s * 0.05f, s * 0.70f, cx - s * 0.08f - cycle * s * 0.03f, s * 0.82f, stroke)
        canvas.drawLine(cx + s * 0.05f, s * 0.70f, cx + s * 0.08f - cycle * s * 0.03f, s * 0.82f, stroke)
        canvas.restore()
    }

    /** Robe sways; the mist swirl breathes. */
    private fun mender(canvas: Canvas, size: Float, cycle: Float) {
        val s = size
        val cx = s / 2f
        val sway = cycle * s * 0.03f
        fill.color = 0xFF4E7D8C.toInt()
        val robe = Path().apply {
            moveTo(cx, s * 0.18f)
            quadTo(cx + s * 0.34f, s * 0.40f, cx + s * 0.30f + sway, s * 0.90f)
            lineTo(cx - s * 0.30f + sway, s * 0.90f)
            quadTo(cx - s * 0.34f, s * 0.40f, cx, s * 0.18f)
            close()
        }
        canvas.drawPath(robe, fill)
        canvas.drawPath(robe, stroke)
        fill.color = 0xFF2B3540.toInt()
        canvas.drawCircle(cx, s * 0.32f, s * 0.16f, fill)
        fill.color = 0xFF8FE8D0.toInt()
        canvas.drawCircle(cx - s * 0.06f, s * 0.32f, s * 0.030f, fill)
        canvas.drawCircle(cx + s * 0.06f, s * 0.32f, s * 0.030f, fill)
        stroke.strokeWidth = s * 0.04f
        val swirl = Path().apply {
            moveTo(cx - s * 0.36f, s * 0.62f + sway)
            quadTo(cx - s * 0.50f, s * 0.50f - cycle * s * 0.04f, cx - s * 0.36f, s * 0.44f + sway)
        }
        canvas.drawPath(swirl, stroke)
        stroke.strokeWidth = s * 0.05f
    }

    /** Slow menacing stride with a shoulder roll. */
    private fun oni(canvas: Canvas, size: Float, skin: Int, cycle: Float) {
        val s = size
        val cx = s / 2f
        val roll = cycle * 2.5f
        canvas.save()
        canvas.rotate(roll, cx, s * 0.6f)
        // legs
        fill.color = skin
        canvas.drawRoundRect(RectF(cx - s * 0.24f + cycle * s * 0.05f, s * 0.84f, cx - s * 0.06f + cycle * s * 0.05f, s * 0.98f), s * 0.03f, s * 0.03f, fill)
        canvas.drawRoundRect(RectF(cx + s * 0.06f - cycle * s * 0.05f, s * 0.84f, cx + s * 0.24f - cycle * s * 0.05f, s * 0.98f), s * 0.03f, s * 0.03f, fill)
        // body
        val body = RectF(s * 0.18f, s * 0.40f, s * 0.82f, s * 0.90f)
        canvas.drawRoundRect(body, s * 0.12f, s * 0.12f, fill)
        canvas.drawRoundRect(body, s * 0.12f, s * 0.12f, stroke)
        // loincloth sways
        fill.color = 0xFFE2B33C.toInt()
        canvas.drawRect(s * 0.22f + cycle * s * 0.02f, s * 0.72f, s * 0.78f + cycle * s * 0.02f, s * 0.90f, fill)
        fill.color = Palette.OUTLINE
        for (x in listOf(0.30f, 0.46f, 0.62f)) {
            canvas.drawRect(s * x + cycle * s * 0.02f, s * 0.72f, s * (x + 0.05f) + cycle * s * 0.02f, s * 0.90f, fill)
        }
        // head with horns
        fill.color = skin
        canvas.drawCircle(cx, s * 0.30f, s * 0.20f, fill)
        canvas.drawCircle(cx, s * 0.30f, s * 0.20f, stroke)
        fill.color = 0xFFE8E4D8.toInt()
        for (dir in listOf(-1, 1)) {
            val horn = Path().apply {
                moveTo(cx + dir * s * 0.10f, s * 0.14f)
                lineTo(cx + dir * s * 0.20f, s * 0.02f)
                lineTo(cx + dir * s * 0.22f, s * 0.16f)
                close()
            }
            canvas.drawPath(horn, fill)
            canvas.drawPath(horn, stroke)
        }
        fill.color = 0xFFFFE68A.toInt()
        canvas.drawCircle(cx - s * 0.08f, s * 0.28f, s * 0.035f, fill)
        canvas.drawCircle(cx + s * 0.08f, s * 0.28f, s * 0.035f, fill)
        fill.color = Palette.EYE_WHITE
        canvas.drawRect(cx - s * 0.09f, s * 0.38f, cx - s * 0.05f, s * 0.44f, fill)
        canvas.drawRect(cx + s * 0.05f, s * 0.38f, cx + s * 0.09f, s * 0.44f, fill)
        canvas.restore()
    }
}
