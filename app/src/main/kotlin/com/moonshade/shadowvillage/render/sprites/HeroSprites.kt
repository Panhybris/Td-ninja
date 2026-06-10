package com.moonshade.shadowvillage.render.sprites

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.moonshade.shadowvillage.render.Palette

enum class HeroPose { IDLE, ATTACK, DASH }

/**
 * The Kage: taller than the chibi tower ninjas (45% head), black/indigo
 * cloak with moon-silver accents, katana. No element colors - the hero
 * stands apart from the five adepts.
 */
object HeroSprites {

    private const val CLOAK = 0xFF2A2D45.toInt()
    private const val CLOAK_DARK = 0xFF1D1F33.toInt()
    private const val SILVER = 0xFFD8DCE8.toInt()

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    fun draw(canvas: Canvas, pose: HeroPose, size: Float) {
        val cx = size / 2f
        val outline = size * 0.04f
        stroke.color = Palette.OUTLINE
        stroke.strokeWidth = outline

        val lunge = if (pose == HeroPose.DASH) size * 0.06f else 0f
        val headR = size * 0.225f // 45% head height vs the towers' 55%
        val headCy = size * 0.26f
        val bodyTop = size * 0.44f
        val bodyW = size * 0.36f
        val bodyH = size * 0.46f

        // flowing cloak behind the body
        fill.color = CLOAK_DARK
        val cloakSway = when (pose) {
            HeroPose.DASH -> size * 0.16f
            HeroPose.ATTACK -> size * 0.08f
            HeroPose.IDLE -> size * 0.03f
        }
        val cloak = Path().apply {
            moveTo(cx - bodyW * 0.55f + lunge, bodyTop)
            quadTo(cx - bodyW * 1.1f - cloakSway, bodyTop + bodyH * 0.6f, cx - bodyW * 0.7f - cloakSway, bodyTop + bodyH * 1.05f)
            lineTo(cx + bodyW * 0.5f + lunge, bodyTop + bodyH * 1.0f)
            quadTo(cx + bodyW * 0.7f + lunge, bodyTop + bodyH * 0.5f, cx + bodyW * 0.55f + lunge, bodyTop)
            close()
        }
        canvas.drawPath(cloak, fill)
        canvas.drawPath(cloak, stroke)

        // legs in stride when dashing
        fill.color = CLOAK
        val legTop = bodyTop + bodyH * 0.78f
        val stride = if (pose == HeroPose.DASH) size * 0.10f else size * 0.03f
        canvas.drawRoundRect(
            RectF(cx - size * 0.10f - stride + lunge, legTop, cx - size * 0.01f - stride + lunge, legTop + size * 0.16f),
            size * 0.03f, size * 0.03f, fill,
        )
        canvas.drawRoundRect(
            RectF(cx + size * 0.01f + stride + lunge, legTop, cx + size * 0.10f + stride + lunge, legTop + size * 0.16f),
            size * 0.03f, size * 0.03f, fill,
        )

        // body
        fill.color = CLOAK
        val body = RectF(cx - bodyW / 2 + lunge, bodyTop, cx + bodyW / 2 + lunge, bodyTop + bodyH * 0.82f)
        canvas.drawRoundRect(body, size * 0.10f, size * 0.10f, fill)
        canvas.drawRoundRect(body, size * 0.10f, size * 0.10f, stroke)

        // silver chest cord
        stroke.color = SILVER
        stroke.strokeWidth = outline * 0.6f
        canvas.drawLine(cx - bodyW * 0.4f + lunge, bodyTop + bodyH * 0.18f, cx + bodyW * 0.4f + lunge, bodyTop + bodyH * 0.38f, stroke)
        stroke.color = Palette.OUTLINE
        stroke.strokeWidth = outline

        // katana
        val handX: Float
        val handY: Float
        when (pose) {
            HeroPose.ATTACK -> {
                // horizontal slash: blade swept out to the side
                handX = cx + bodyW * 0.75f + lunge
                handY = bodyTop + bodyH * 0.25f
                stroke.color = SILVER
                stroke.strokeWidth = size * 0.045f
                canvas.drawLine(handX, handY, handX + size * 0.34f, handY - size * 0.04f, stroke)
                // slash arc
                stroke.strokeWidth = size * 0.02f
                canvas.drawArc(
                    RectF(handX - size * 0.1f, handY - size * 0.22f, handX + size * 0.42f, handY + size * 0.22f),
                    -60f, 120f, false, stroke,
                )
                stroke.color = Palette.OUTLINE
                stroke.strokeWidth = outline
            }
            else -> {
                // hand resting on the hilt, blade on the back
                handX = cx + bodyW * 0.45f + lunge
                handY = bodyTop + bodyH * 0.30f
                stroke.color = SILVER
                stroke.strokeWidth = size * 0.04f
                canvas.drawLine(
                    cx - bodyW * 0.55f + lunge, bodyTop - size * 0.06f,
                    cx + bodyW * 0.55f + lunge, bodyTop + bodyH * 0.30f, stroke,
                )
                stroke.color = Palette.OUTLINE
                stroke.strokeWidth = outline
            }
        }
        fill.color = Palette.SKIN
        canvas.drawCircle(handX, handY, size * 0.05f, fill)

        // head: hooded with a silver crescent
        fill.color = CLOAK
        canvas.drawCircle(cx + lunge, headCy, headR, fill)
        canvas.drawCircle(cx + lunge, headCy, headR, stroke)
        fill.color = Palette.SKIN
        val face = RectF(
            cx - headR * 0.58f + lunge, headCy - headR * 0.22f,
            cx + headR * 0.58f + lunge, headCy + headR * 0.52f,
        )
        canvas.drawRoundRect(face, headR * 0.4f, headR * 0.4f, fill)
        fill.color = Palette.EYE_DARK
        canvas.drawCircle(cx - headR * 0.28f + lunge, headCy + headR * 0.08f, headR * 0.09f, fill)
        canvas.drawCircle(cx + headR * 0.28f + lunge, headCy + headR * 0.08f, headR * 0.09f, fill)
        // crescent moon mark on the hood
        fill.color = SILVER
        canvas.drawCircle(cx + lunge, headCy - headR * 0.55f, headR * 0.16f, fill)
        fill.color = CLOAK
        canvas.drawCircle(cx + headR * 0.08f + lunge, headCy - headR * 0.60f, headR * 0.13f, fill)

        // dash smoke
        if (pose == HeroPose.DASH) {
            fill.shader = RadialGradient(
                cx - bodyW + lunge, bodyTop + bodyH * 0.8f, size * 0.2f,
                0x80D9D4C8.toInt(), 0x00D9D4C8, Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx - bodyW + lunge, bodyTop + bodyH * 0.8f, size * 0.2f, fill)
            fill.shader = null
        }
    }
}
