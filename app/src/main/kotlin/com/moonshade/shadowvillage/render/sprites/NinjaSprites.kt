package com.moonshade.shadowvillage.render.sprites

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.moonshade.shadowvillage.core.data.Element
import com.moonshade.shadowvillage.core.data.SpecPath
import com.moonshade.shadowvillage.render.Palette

enum class NinjaPose { IDLE, ATTACK }

/**
 * Chibi ninja defenders with a visible rank progression:
 * T1 apprentice (plain suit), T2 adept (shoulder guards + signature
 * weapon), T3 master (element haori, headgear, scroll, twin orbs).
 * Spec path B recolors the haori dark with light trim.
 */
object NinjaSprites {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    fun draw(
        canvas: Canvas,
        element: Element,
        tier: Int,
        spec: SpecPath?,
        pose: NinjaPose,
        size: Float,
    ) {
        val c = Palette.of(element)
        val cx = size / 2f
        val outline = size * 0.045f
        stroke.color = Palette.OUTLINE
        stroke.strokeWidth = outline

        val attacking = pose == NinjaPose.ATTACK
        val bodyTop = size * 0.52f
        val bodyW = size * 0.42f
        val bodyH = size * 0.40f
        val headR = size * 0.275f
        val headCy = size * 0.30f

        // ---- T3 haori cloak, behind everything ----
        if (tier >= 3) {
            val cloakMain = if (spec == SpecPath.B) c.dark else c.base
            val cloakTrim = if (spec == SpecPath.B) c.light else c.dark
            fill.color = cloakMain
            val cloak = Path().apply {
                moveTo(cx - bodyW * 0.75f, bodyTop - size * 0.02f)
                quadTo(cx - bodyW * 1.05f, bodyTop + bodyH * 0.7f, cx - bodyW * 0.55f, bodyTop + bodyH * 1.05f)
                lineTo(cx + bodyW * 0.55f, bodyTop + bodyH * 1.05f)
                quadTo(cx + bodyW * 1.05f, bodyTop + bodyH * 0.7f, cx + bodyW * 0.75f, bodyTop - size * 0.02f)
                close()
            }
            canvas.drawPath(cloak, fill)
            canvas.drawPath(cloak, stroke)
            stroke.color = cloakTrim
            stroke.strokeWidth = outline * 0.7f
            canvas.drawLine(cx - bodyW * 0.70f, bodyTop + size * 0.02f, cx - bodyW * 0.48f, bodyTop + bodyH, stroke)
            canvas.drawLine(cx + bodyW * 0.70f, bodyTop + size * 0.02f, cx + bodyW * 0.48f, bodyTop + bodyH, stroke)
            stroke.color = Palette.OUTLINE
            stroke.strokeWidth = outline
        }

        // ---- body ----
        val body = RectF(cx - bodyW / 2, bodyTop, cx + bodyW / 2, bodyTop + bodyH)
        fill.shader = null
        fill.color = Palette.NINJA_SUIT
        canvas.drawRoundRect(body, size * 0.12f, size * 0.12f, fill)
        canvas.drawRoundRect(body, size * 0.12f, size * 0.12f, stroke)

        // sash: single wrap T1, double wrap T2+
        fill.color = c.base
        val sash = Path().apply {
            moveTo(cx - bodyW / 2, bodyTop + bodyH * 0.35f)
            lineTo(cx + bodyW / 2, bodyTop + bodyH * 0.55f)
            lineTo(cx + bodyW / 2, bodyTop + bodyH * 0.72f)
            lineTo(cx - bodyW / 2, bodyTop + bodyH * 0.52f)
            close()
        }
        canvas.drawPath(sash, fill)
        if (tier >= 2) {
            fill.color = c.dark
            val sash2 = Path().apply {
                moveTo(cx - bodyW / 2, bodyTop + bodyH * 0.55f)
                lineTo(cx + bodyW / 2, bodyTop + bodyH * 0.75f)
                lineTo(cx + bodyW / 2, bodyTop + bodyH * 0.85f)
                lineTo(cx - bodyW / 2, bodyTop + bodyH * 0.65f)
                close()
            }
            canvas.drawPath(sash2, fill)
        }

        // ---- T2+ shoulder guards ----
        if (tier >= 2) {
            fill.color = c.base
            canvas.drawRoundRect(
                RectF(cx - bodyW * 0.72f, bodyTop - size * 0.015f, cx - bodyW * 0.28f, bodyTop + size * 0.085f),
                size * 0.04f, size * 0.04f, fill,
            )
            canvas.drawRoundRect(
                RectF(cx + bodyW * 0.28f, bodyTop - size * 0.015f, cx + bodyW * 0.72f, bodyTop + size * 0.085f),
                size * 0.04f, size * 0.04f, fill,
            )
        }

        // ---- arms / weapon hand ----
        val handY = bodyTop + bodyH * (if (attacking) 0.30f else 0.50f)
        val handX = cx + bodyW * (if (attacking) 0.62f else 0.42f)
        fill.color = Palette.SKIN
        canvas.drawCircle(cx - bodyW * 0.30f, bodyTop + bodyH * 0.52f, size * 0.055f, fill)
        canvas.drawCircle(handX, handY, size * 0.055f, fill)

        if (tier >= 2) {
            drawWeapon(canvas, element, handX, handY, size, attacking, c.base, c.light)
        }

        // ---- head ----
        fill.color = Palette.NINJA_SUIT
        canvas.drawCircle(cx, headCy, headR, fill)
        canvas.drawCircle(cx, headCy, headR, stroke)

        fill.color = Palette.SKIN
        val face = RectF(cx - headR * 0.62f, headCy - headR * 0.30f, cx + headR * 0.62f, headCy + headR * 0.55f)
        canvas.drawRoundRect(face, headR * 0.45f, headR * 0.45f, fill)

        fill.color = Palette.EYE_DARK
        val eyeY = headCy + headR * 0.05f
        val eyeDx = headR * 0.30f
        canvas.drawCircle(cx - eyeDx, eyeY, headR * 0.10f, fill)
        canvas.drawCircle(cx + eyeDx, eyeY, headR * 0.10f, fill)
        stroke.strokeWidth = outline * 0.8f
        canvas.drawLine(cx - eyeDx - headR * 0.18f, eyeY - headR * 0.28f, cx - eyeDx + headR * 0.12f, eyeY - headR * 0.18f, stroke)
        canvas.drawLine(cx + eyeDx + headR * 0.18f, eyeY - headR * 0.28f, cx + eyeDx - headR * 0.12f, eyeY - headR * 0.18f, stroke)
        stroke.strokeWidth = outline

        // ---- headband ----
        fill.color = c.base
        val bandTop = headCy - headR * 0.55f
        val band = RectF(cx - headR * 0.95f, bandTop, cx + headR * 0.95f, bandTop + headR * 0.42f)
        canvas.drawRoundRect(band, headR * 0.15f, headR * 0.15f, fill)
        canvas.drawRoundRect(band, headR * 0.15f, headR * 0.15f, stroke)
        fill.color = c.dark
        val tail = Path().apply {
            moveTo(cx + headR * 0.80f, bandTop + headR * 0.30f)
            lineTo(cx + headR * 1.25f, bandTop + headR * 0.75f)
            lineTo(cx + headR * 1.05f, bandTop + headR * 0.85f)
            close()
        }
        canvas.drawPath(tail, fill)

        // T2+: metal plate; spec'd T3 gets an A/B glyph on it
        if (tier >= 2) {
            fill.color = 0xFFB9C2D6.toInt()
            val plate = RectF(cx - headR * 0.34f, bandTop + headR * 0.04f, cx + headR * 0.34f, bandTop + headR * 0.38f)
            canvas.drawRoundRect(plate, headR * 0.08f, headR * 0.08f, fill)
            if (tier >= 3 && spec != null) {
                text.color = Palette.OUTLINE
                text.textSize = headR * 0.34f
                canvas.drawText(spec.name, cx, bandTop + headR * 0.33f, text)
            }
        }

        // ---- T3 headgear per element ----
        if (tier >= 3) {
            drawHeadgear(canvas, element, cx, headCy, headR, c.base, c.dark)
        }

        // ---- T3 shoulder scroll ----
        if (tier >= 3) {
            fill.color = 0xFFE8DCC0.toInt()
            val scroll = RectF(cx - bodyW * 0.82f, bodyTop - size * 0.03f, cx - bodyW * 0.34f, bodyTop + size * 0.10f)
            canvas.drawRoundRect(scroll, size * 0.06f, size * 0.06f, fill)
            canvas.drawRoundRect(scroll, size * 0.06f, size * 0.06f, stroke)
            fill.color = c.base
            canvas.drawRect(scroll.centerX() - size * 0.02f, scroll.top, scroll.centerX() + size * 0.02f, scroll.bottom, fill)
        }

        // ---- element orb(s) ----
        val orbX = handX + size * (if (attacking) 0.10f else 0.10f)
        val orbY = handY - size * 0.10f
        val orbR = size * (0.06f + 0.015f * tier) * (if (attacking) 1.35f else 1f)
        fill.shader = RadialGradient(orbX, orbY, orbR, c.light, c.dark, Shader.TileMode.CLAMP)
        canvas.drawCircle(orbX, orbY, orbR, fill)
        fill.shader = null
        if (attacking) {
            stroke.color = c.light
            stroke.strokeWidth = outline * 0.6f
            canvas.drawCircle(orbX, orbY, orbR * 1.5f, stroke)
            stroke.color = Palette.OUTLINE
            stroke.strokeWidth = outline
        }
        if (tier >= 3) {
            // second small orbiting orb, mirrored side
            fill.shader = RadialGradient(cx - bodyW * 0.55f, bodyTop + bodyH * 0.15f, orbR * 0.6f, c.light, c.dark, Shader.TileMode.CLAMP)
            canvas.drawCircle(cx - bodyW * 0.55f, bodyTop + bodyH * 0.15f, orbR * 0.6f, fill)
            fill.shader = null
        }
    }

    private fun drawWeapon(
        canvas: Canvas,
        element: Element,
        handX: Float,
        handY: Float,
        size: Float,
        attacking: Boolean,
        base: Int,
        light: Int,
    ) {
        val reach = size * (if (attacking) 0.30f else 0.22f)
        val tilt = if (attacking) -0.5f else -0.9f // radians-ish slope of the weapon
        stroke.strokeWidth = size * 0.035f
        when (element) {
            Element.FIRE -> { // flame-tipped tanto
                fill.color = 0xFFB9C2D6.toInt()
                val blade = Path().apply {
                    moveTo(handX, handY)
                    lineTo(handX + reach * 0.9f, handY + tilt * reach * 0.9f)
                    lineTo(handX + reach * 0.78f, handY + tilt * reach * 0.9f - size * 0.05f)
                    close()
                }
                canvas.drawPath(blade, fill)
                fill.color = base
                canvas.drawCircle(handX + reach * 0.95f, handY + tilt * reach * 0.95f, size * 0.05f, fill)
            }
            Element.WATER -> { // flowing curved blade with a drip
                fill.color = light
                val wave = Path().apply {
                    moveTo(handX, handY)
                    quadTo(handX + reach * 0.6f, handY + tilt * reach - size * 0.06f, handX + reach, handY + tilt * reach)
                    quadTo(handX + reach * 0.55f, handY + tilt * reach + size * 0.03f, handX, handY + size * 0.03f)
                    close()
                }
                canvas.drawPath(wave, fill)
                fill.color = base
                canvas.drawCircle(handX + reach, handY + tilt * reach + size * 0.07f, size * 0.025f, fill)
            }
            Element.LIGHTNING -> { // zigzag bolt wand
                fill.color = base
                val bolt = Path().apply {
                    moveTo(handX, handY)
                    lineTo(handX + reach * 0.45f, handY + tilt * reach * 0.5f)
                    lineTo(handX + reach * 0.30f, handY + tilt * reach * 0.55f)
                    lineTo(handX + reach * 0.95f, handY + tilt * reach)
                    lineTo(handX + reach * 0.62f, handY + tilt * reach * 0.62f)
                    lineTo(handX + reach * 0.75f, handY + tilt * reach * 0.55f)
                    close()
                }
                canvas.drawPath(bolt, fill)
            }
            Element.WIND -> { // open tessen war fan
                fill.color = light
                val fan = Path()
                val baseAng = if (attacking) -100.0 else -130.0
                for (i in 0..4) {
                    val ang = Math.toRadians(baseAng + i * 22.0)
                    val tipX = handX + (reach * 1.1f * Math.cos(ang)).toFloat()
                    val tipY = handY + (reach * 1.1f * Math.sin(ang)).toFloat()
                    if (i == 0) fan.moveTo(handX, handY)
                    fan.lineTo(tipX, tipY)
                }
                fan.close()
                canvas.drawPath(fan, fill)
                stroke.color = base
                canvas.drawPath(fan, stroke)
                stroke.color = Palette.OUTLINE
            }
            Element.EARTH -> { // stone-headed hammer
                stroke.color = 0xFF8B6F4E.toInt()
                canvas.drawLine(handX, handY, handX + reach * 0.9f, handY + tilt * reach * 0.9f, stroke)
                stroke.color = Palette.OUTLINE
                fill.color = base
                canvas.drawRoundRect(
                    RectF(
                        handX + reach * 0.72f, handY + tilt * reach * 0.9f - size * 0.075f,
                        handX + reach * 1.12f, handY + tilt * reach * 0.9f + size * 0.045f,
                    ),
                    size * 0.025f, size * 0.025f, fill,
                )
            }
        }
        stroke.strokeWidth = size * 0.045f
    }

    private fun drawHeadgear(
        canvas: Canvas,
        element: Element,
        cx: Float,
        headCy: Float,
        headR: Float,
        base: Int,
        dark: Int,
    ) {
        when (element) {
            Element.FIRE -> { // flame plume crest
                fill.color = base
                val flame = Path().apply {
                    moveTo(cx - headR * 0.18f, headCy - headR * 0.85f)
                    quadTo(cx - headR * 0.30f, headCy - headR * 1.45f, cx, headCy - headR * 1.55f)
                    quadTo(cx + headR * 0.10f, headCy - headR * 1.15f, cx + headR * 0.22f, headCy - headR * 0.85f)
                    close()
                }
                canvas.drawPath(flame, fill)
                fill.color = Palette.of(Element.FIRE).light
                canvas.drawCircle(cx - headR * 0.02f, headCy - headR * 1.18f, headR * 0.12f, fill)
            }
            Element.WATER -> { // wide straw kasa with wave mark
                fill.color = 0xFFD9C68A.toInt()
                val kasa = Path().apply {
                    moveTo(cx - headR * 1.25f, headCy - headR * 0.55f)
                    lineTo(cx, headCy - headR * 1.35f)
                    lineTo(cx + headR * 1.25f, headCy - headR * 0.55f)
                    close()
                }
                canvas.drawPath(kasa, fill)
                canvas.drawPath(kasa, stroke)
                stroke.color = base
                canvas.drawLine(cx - headR * 0.4f, headCy - headR * 0.82f, cx + headR * 0.4f, headCy - headR * 0.82f, stroke)
                stroke.color = Palette.OUTLINE
            }
            Element.LIGHTNING -> { // storm-swept spiked hair
                fill.color = 0xFFE8E4D8.toInt()
                for (i in 0 until 4) {
                    val sx = cx - headR * 0.55f + i * headR * 0.36f
                    val spike = Path().apply {
                        moveTo(sx, headCy - headR * 0.75f)
                        lineTo(sx + headR * 0.30f, headCy - headR * (1.25f + (i % 2) * 0.18f))
                        lineTo(sx + headR * 0.42f, headCy - headR * 0.70f)
                        close()
                    }
                    canvas.drawPath(spike, fill)
                }
            }
            Element.WIND -> { // streaming twin scarf tails
                fill.color = base
                val scarf = Path().apply {
                    moveTo(cx - headR * 0.85f, headCy - headR * 0.45f)
                    quadTo(cx - headR * 1.85f, headCy - headR * 0.75f, cx - headR * 1.95f, headCy - headR * 0.15f)
                    quadTo(cx - headR * 1.45f, headCy - headR * 0.35f, cx - headR * 0.85f, headCy - headR * 0.12f)
                    close()
                }
                canvas.drawPath(scarf, fill)
                fill.color = dark
                val scarf2 = Path().apply {
                    moveTo(cx - headR * 0.85f, headCy - headR * 0.30f)
                    quadTo(cx - headR * 1.55f, headCy + headR * 0.05f, cx - headR * 1.70f, headCy + headR * 0.45f)
                    quadTo(cx - headR * 1.25f, headCy + headR * 0.18f, cx - headR * 0.85f, headCy + headR * 0.02f)
                    close()
                }
                canvas.drawPath(scarf2, fill)
            }
            Element.EARTH -> { // horned stone kabuto
                fill.color = 0xFF8B8E98.toInt()
                canvas.drawArc(
                    RectF(cx - headR * 0.95f, headCy - headR * 1.25f, cx + headR * 0.95f, headCy + headR * 0.1f),
                    180f, 180f, true, fill,
                )
                fill.color = 0xFFE8E4D8.toInt()
                for (dir in listOf(-1, 1)) {
                    val horn = Path().apply {
                        moveTo(cx + dir * headR * 0.55f, headCy - headR * 0.95f)
                        lineTo(cx + dir * headR * 1.05f, headCy - headR * 1.55f)
                        lineTo(cx + dir * headR * 0.80f, headCy - headR * 0.80f)
                        close()
                    }
                    canvas.drawPath(horn, fill)
                    canvas.drawPath(horn, stroke)
                }
            }
        }
    }
}
