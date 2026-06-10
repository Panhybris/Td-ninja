package com.moonshade.shadowvillage.render.sprites

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.moonshade.shadowvillage.core.data.EnemyType
import com.moonshade.shadowvillage.render.Palette

/**
 * Enemy sprites with distinct silhouettes per type, each drawn into a
 * size x size box at the canvas origin.
 */
object EnemySprites {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    fun draw(canvas: Canvas, type: EnemyType, size: Float) {
        stroke.color = Palette.OUTLINE
        stroke.strokeWidth = size * 0.05f
        when (type) {
            EnemyType.SCOUT -> humanoid(canvas, size, suit = 0xFF8C6BAE.toInt(), scale = 0.78f, scarf = true)
            EnemyType.NINJA -> humanoid(canvas, size, suit = 0xFF5E5673.toInt(), scale = 0.95f, scarf = false)
            EnemyType.WOLF -> wolf(canvas, size)
            EnemyType.BRUTE -> brute(canvas, size, mini = false)
            EnemyType.RAIDER -> raider(canvas, size)
            EnemyType.MENDER -> mender(canvas, size)
            EnemyType.ONI_VANGUARD -> oni(canvas, size, skin = 0xFF5B8C5A.toInt())
            EnemyType.ONI_WARLORD -> oni(canvas, size, skin = 0xFFB8413C.toInt())
        }
    }

    /** Hooded rogue: same chibi proportions as defenders, hostile colors. */
    private fun humanoid(canvas: Canvas, size: Float, suit: Int, scale: Float, scarf: Boolean) {
        val cx = size / 2f
        val s = size * scale
        val pad = (size - s) / 2f

        // body
        fill.color = suit
        val body = RectF(cx - s * 0.20f, pad + s * 0.50f, cx + s * 0.20f, pad + s * 0.92f)
        canvas.drawRoundRect(body, s * 0.10f, s * 0.10f, fill)
        canvas.drawRoundRect(body, s * 0.10f, s * 0.10f, stroke)

        if (scarf) {
            fill.color = 0xFFD96A6A.toInt()
            val tail = Path().apply {
                moveTo(cx - s * 0.15f, pad + s * 0.52f)
                lineTo(cx - s * 0.48f, pad + s * 0.66f)
                lineTo(cx - s * 0.40f, pad + s * 0.76f)
                lineTo(cx - s * 0.12f, pad + s * 0.62f)
                close()
            }
            canvas.drawPath(tail, fill)
        }

        // head: hooded, eyes only
        val headR = s * 0.26f
        val headCy = pad + s * 0.28f
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

    /** Quadruped silhouette with pointed ears and a bushy tail. */
    private fun wolf(canvas: Canvas, size: Float) {
        val s = size
        fill.color = 0xFF4A4458.toInt()
        // torso
        val torso = RectF(s * 0.12f, s * 0.42f, s * 0.78f, s * 0.72f)
        canvas.drawRoundRect(torso, s * 0.14f, s * 0.14f, fill)
        canvas.drawRoundRect(torso, s * 0.14f, s * 0.14f, stroke)
        // legs
        for (x in listOf(0.20f, 0.34f, 0.56f, 0.68f)) {
            canvas.drawRect(s * x, s * 0.68f, s * (x + 0.08f), s * 0.88f, fill)
        }
        // tail
        val tail = Path().apply {
            moveTo(s * 0.14f, s * 0.48f)
            quadTo(s * 0.00f, s * 0.34f, s * 0.10f, s * 0.24f)
            quadTo(s * 0.16f, s * 0.40f, s * 0.24f, s * 0.46f)
            close()
        }
        canvas.drawPath(tail, fill)
        // head
        fill.color = 0xFF564F66.toInt()
        canvas.drawCircle(s * 0.78f, s * 0.40f, s * 0.17f, fill)
        canvas.drawCircle(s * 0.78f, s * 0.40f, s * 0.17f, stroke)
        // snout + ears
        fill.color = 0xFF4A4458.toInt()
        canvas.drawRoundRect(RectF(s * 0.86f, s * 0.38f, s * 1.00f, s * 0.48f), s * 0.04f, s * 0.04f, fill)
        val ear = Path().apply {
            moveTo(s * 0.70f, s * 0.28f); lineTo(s * 0.66f, s * 0.10f); lineTo(s * 0.80f, s * 0.24f); close()
        }
        canvas.drawPath(ear, fill)
        // eye
        fill.color = 0xFFFFD23F.toInt()
        canvas.drawCircle(s * 0.82f, s * 0.36f, s * 0.035f, fill)
    }

    /** Wide armored hulk with pauldrons. */
    private fun brute(canvas: Canvas, size: Float, mini: Boolean) {
        val s = size
        val cx = s / 2f
        // torso: wide slab
        fill.color = 0xFF6E7079.toInt()
        val torso = RectF(s * 0.14f, s * 0.34f, s * 0.86f, s * 0.90f)
        canvas.drawRoundRect(torso, s * 0.10f, s * 0.10f, fill)
        canvas.drawRoundRect(torso, s * 0.10f, s * 0.10f, stroke)
        // belly plate
        fill.color = 0xFF8B8E98.toInt()
        canvas.drawRoundRect(RectF(s * 0.30f, s * 0.48f, s * 0.70f, s * 0.86f), s * 0.08f, s * 0.08f, fill)
        // pauldrons
        fill.color = 0xFF565963.toInt()
        canvas.drawRoundRect(RectF(s * 0.04f, s * 0.30f, s * 0.30f, s * 0.50f), s * 0.06f, s * 0.06f, fill)
        canvas.drawRoundRect(RectF(s * 0.70f, s * 0.30f, s * 0.96f, s * 0.50f), s * 0.06f, s * 0.06f, fill)
        // small head peeking over the armor
        fill.color = 0xFFB99B72.toInt()
        canvas.drawCircle(cx, s * 0.26f, s * 0.14f, fill)
        canvas.drawCircle(cx, s * 0.26f, s * 0.14f, stroke)
        fill.color = Palette.EYE_DARK
        canvas.drawCircle(cx - s * 0.05f, s * 0.25f, s * 0.025f, fill)
        canvas.drawCircle(cx + s * 0.05f, s * 0.25f, s * 0.025f, fill)
    }

    /** Small flyer on a kite-glider. */
    private fun raider(canvas: Canvas, size: Float) {
        val s = size
        val cx = s / 2f
        // glider wing
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
        // hanging ninja (small)
        fill.color = 0xFF5E5673.toInt()
        canvas.drawRoundRect(RectF(cx - s * 0.12f, s * 0.40f, cx + s * 0.12f, s * 0.70f), s * 0.06f, s * 0.06f, fill)
        val headR = s * 0.14f
        canvas.drawCircle(cx, s * 0.38f, headR, fill)
        canvas.drawCircle(cx, s * 0.38f, headR, stroke)
        fill.color = Palette.EYE_WHITE
        canvas.drawRoundRect(
            RectF(cx - headR * 0.55f, s * 0.36f, cx + headR * 0.55f, s * 0.41f), s * 0.02f, s * 0.02f, fill,
        )
        // dangling legs
        stroke.strokeWidth = s * 0.05f
        canvas.drawLine(cx - s * 0.05f, s * 0.70f, cx - s * 0.08f, s * 0.82f, stroke)
        canvas.drawLine(cx + s * 0.05f, s * 0.70f, cx + s * 0.08f, s * 0.82f, stroke)
    }

    /** Hooded healer with a wisp of mist. */
    private fun mender(canvas: Canvas, size: Float) {
        val s = size
        val cx = s / 2f
        // robe: bell shape
        fill.color = 0xFF4E7D8C.toInt()
        val robe = Path().apply {
            moveTo(cx, s * 0.18f)
            quadTo(cx + s * 0.34f, s * 0.40f, cx + s * 0.30f, s * 0.90f)
            lineTo(cx - s * 0.30f, s * 0.90f)
            quadTo(cx - s * 0.34f, s * 0.40f, cx, s * 0.18f)
            close()
        }
        canvas.drawPath(robe, fill)
        canvas.drawPath(robe, stroke)
        // shadowed hood with glowing eyes
        fill.color = 0xFF2B3540.toInt()
        canvas.drawCircle(cx, s * 0.32f, s * 0.16f, fill)
        fill.color = 0xFF8FE8D0.toInt()
        canvas.drawCircle(cx - s * 0.06f, s * 0.32f, s * 0.030f, fill)
        canvas.drawCircle(cx + s * 0.06f, s * 0.32f, s * 0.030f, fill)
        // mist swirl
        stroke.strokeWidth = s * 0.04f
        val swirl = Path().apply {
            moveTo(cx - s * 0.36f, s * 0.62f)
            quadTo(cx - s * 0.50f, s * 0.50f, cx - s * 0.36f, s * 0.44f)
        }
        canvas.drawPath(swirl, stroke)
        stroke.strokeWidth = s * 0.05f
    }

    /** Horned ogre; the warlord is the same silhouette in red, drawn bigger by the renderer. */
    private fun oni(canvas: Canvas, size: Float, skin: Int) {
        val s = size
        val cx = s / 2f
        // body
        fill.color = skin
        val body = RectF(s * 0.18f, s * 0.40f, s * 0.82f, s * 0.92f)
        canvas.drawRoundRect(body, s * 0.12f, s * 0.12f, fill)
        canvas.drawRoundRect(body, s * 0.12f, s * 0.12f, stroke)
        // tiger-stripe loincloth
        fill.color = 0xFFE2B33C.toInt()
        canvas.drawRect(s * 0.22f, s * 0.74f, s * 0.78f, s * 0.92f, fill)
        fill.color = Palette.OUTLINE
        for (x in listOf(0.30f, 0.46f, 0.62f)) {
            canvas.drawRect(s * x, s * 0.74f, s * (x + 0.05f), s * 0.92f, fill)
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
        // angry eyes + tusks
        fill.color = 0xFFFFE68A.toInt()
        canvas.drawCircle(cx - s * 0.08f, s * 0.28f, s * 0.035f, fill)
        canvas.drawCircle(cx + s * 0.08f, s * 0.28f, s * 0.035f, fill)
        fill.color = Palette.EYE_WHITE
        canvas.drawRect(cx - s * 0.09f, s * 0.38f, cx - s * 0.05f, s * 0.44f, fill)
        canvas.drawRect(cx + s * 0.05f, s * 0.38f, cx + s * 0.09f, s * 0.44f, fill)
    }
}
