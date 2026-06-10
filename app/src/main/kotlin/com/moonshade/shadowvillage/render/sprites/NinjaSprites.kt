package com.moonshade.shadowvillage.render.sprites

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.moonshade.shadowvillage.core.data.Element
import com.moonshade.shadowvillage.render.Palette

/**
 * Chibi ninja tower defenders, drawn entirely with Canvas vector ops.
 * All draws fill a square of [size] px with the sprite centered; callers
 * translate the canvas to position it.
 */
object NinjaSprites {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    /**
     * Draws a ninja adept of [element] at upgrade [tier] (1..3) into a
     * size x size box whose top-left is at the canvas origin.
     */
    fun draw(canvas: Canvas, element: Element, tier: Int, size: Float) {
        val c = Palette.of(element)
        val cx = size / 2f
        val outline = size * 0.045f
        stroke.color = Palette.OUTLINE
        stroke.strokeWidth = outline

        // ----- body (small, under the big head) -----
        val bodyTop = size * 0.52f
        val bodyW = size * 0.42f
        val bodyH = size * 0.40f
        val body = RectF(cx - bodyW / 2, bodyTop, cx + bodyW / 2, bodyTop + bodyH)
        fill.shader = null
        fill.color = Palette.NINJA_SUIT
        canvas.drawRoundRect(body, size * 0.12f, size * 0.12f, fill)
        canvas.drawRoundRect(body, size * 0.12f, size * 0.12f, stroke)

        // element-colored sash
        fill.color = c.base
        val sash = Path().apply {
            moveTo(cx - bodyW / 2, bodyTop + bodyH * 0.35f)
            lineTo(cx + bodyW / 2, bodyTop + bodyH * 0.55f)
            lineTo(cx + bodyW / 2, bodyTop + bodyH * 0.75f)
            lineTo(cx - bodyW / 2, bodyTop + bodyH * 0.55f)
            close()
        }
        canvas.drawPath(sash, fill)

        // arms folded: two small fists at the sash
        fill.color = Palette.SKIN
        canvas.drawCircle(cx - bodyW * 0.30f, bodyTop + bodyH * 0.50f, size * 0.055f, fill)
        canvas.drawCircle(cx + bodyW * 0.30f, bodyTop + bodyH * 0.58f, size * 0.055f, fill)

        // ----- head (chibi: ~55% of sprite) -----
        val headR = size * 0.275f
        val headCy = size * 0.30f
        fill.color = Palette.NINJA_SUIT
        canvas.drawCircle(cx, headCy, headR, fill)
        canvas.drawCircle(cx, headCy, headR, stroke)

        // face opening of the hood
        fill.color = Palette.SKIN
        val face = RectF(cx - headR * 0.62f, headCy - headR * 0.30f, cx + headR * 0.62f, headCy + headR * 0.55f)
        canvas.drawRoundRect(face, headR * 0.45f, headR * 0.45f, fill)

        // eyes: determined look
        fill.color = Palette.EYE_DARK
        val eyeY = headCy + headR * 0.05f
        val eyeDx = headR * 0.30f
        canvas.drawCircle(cx - eyeDx, eyeY, headR * 0.10f, fill)
        canvas.drawCircle(cx + eyeDx, eyeY, headR * 0.10f, fill)
        // eyebrows angled inward
        stroke.strokeWidth = outline * 0.8f
        canvas.drawLine(cx - eyeDx - headR * 0.18f, eyeY - headR * 0.28f, cx - eyeDx + headR * 0.12f, eyeY - headR * 0.18f, stroke)
        canvas.drawLine(cx + eyeDx + headR * 0.18f, eyeY - headR * 0.28f, cx + eyeDx - headR * 0.12f, eyeY - headR * 0.18f, stroke)
        stroke.strokeWidth = outline

        // ----- headband in element color with tier stripes -----
        fill.color = c.base
        val bandTop = headCy - headR * 0.55f
        val band = RectF(cx - headR * 0.95f, bandTop, cx + headR * 0.95f, bandTop + headR * 0.42f)
        canvas.drawRoundRect(band, headR * 0.15f, headR * 0.15f, fill)
        canvas.drawRoundRect(band, headR * 0.15f, headR * 0.15f, stroke)
        // headband knot tails
        fill.color = c.dark
        val tail = Path().apply {
            moveTo(cx + headR * 0.80f, bandTop + headR * 0.30f)
            lineTo(cx + headR * 1.25f, bandTop + headR * 0.75f)
            lineTo(cx + headR * 1.05f, bandTop + headR * 0.85f)
            close()
        }
        canvas.drawPath(tail, fill)
        // tier stripes on the band
        fill.color = Palette.HUD_TEXT
        val stripeW = headR * 0.10f
        val stripeH = headR * 0.26f
        val stripeY = bandTop + headR * 0.08f
        for (i in 0 until tier) {
            val sx = cx - stripeW * 1.5f + i * stripeW * 1.5f
            canvas.drawRoundRect(
                RectF(sx, stripeY, sx + stripeW, stripeY + stripeH),
                stripeW / 2, stripeW / 2, fill,
            )
        }

        // ----- tier 3: shoulder scroll -----
        if (tier >= 3) {
            fill.color = 0xFFE8DCC0.toInt()
            val scroll = RectF(cx - bodyW * 0.78f, bodyTop - size * 0.03f, cx - bodyW * 0.30f, bodyTop + size * 0.10f)
            canvas.drawRoundRect(scroll, size * 0.06f, size * 0.06f, fill)
            canvas.drawRoundRect(scroll, size * 0.06f, size * 0.06f, stroke)
            fill.color = c.base
            canvas.drawRect(
                scroll.centerX() - size * 0.02f, scroll.top, scroll.centerX() + size * 0.02f, scroll.bottom, fill,
            )
        }

        // ----- element orb hovering by the free hand -----
        val orbX = cx + bodyW * 0.52f
        val orbY = bodyTop + bodyH * 0.18f
        val orbR = size * (0.07f + 0.015f * tier)
        fill.shader = RadialGradient(orbX, orbY, orbR, c.light, c.dark, Shader.TileMode.CLAMP)
        canvas.drawCircle(orbX, orbY, orbR, fill)
        fill.shader = null
    }
}
