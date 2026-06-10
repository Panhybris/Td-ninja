package com.moonshade.shadowvillage.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.moonshade.shadowvillage.core.game.GameSession
import com.moonshade.shadowvillage.render.sprites.UiSprites

/**
 * Top status bar and the corner buttons. Owns the button rectangles so
 * input hit-testing and drawing always agree.
 */
class HudRenderer {

    val pauseBtn = RectF()
    val speedBtn = RectF()
    val sendWaveBtn = RectF()
    val heroBtn = RectF()
    val abilityBtn = RectF()

    private var width = 0
    private var height = 0
    private var unit = 0f // base ui unit

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFakeBoldText = true }

    fun onSizeChanged(w: Int, h: Int) {
        width = w
        height = h
        unit = h * 0.055f
        val btn = unit * 1.6f
        val pad = unit * 0.5f
        pauseBtn.set(w - btn - pad, pad, w - pad, pad + btn)
        speedBtn.set(w - 2 * btn - 2 * pad, pad, w - btn - 2 * pad, pad + btn)
        sendWaveBtn.set(w - unit * 5.4f - pad, h - btn * 1.15f - pad, w - pad, h - pad)
        heroBtn.set(pad, h - btn * 1.15f - pad, pad + unit * 4.4f, h - pad)
        abilityBtn.set(heroBtn.right + pad, h - btn * 1.15f - pad, heroBtn.right + pad + btn * 1.15f, h - pad)
    }

    fun draw(canvas: Canvas, session: GameSession, speed: Int, paused: Boolean, placingHero: Boolean, time: Float) {
        if (width == 0) return
        val pad = unit * 0.5f

        // status pill: gold, lives, wave
        paint.color = Palette.HUD_BG
        val pillW = unit * 11.5f
        val pill = RectF(pad, pad, pad + pillW, pad + unit * 1.6f)
        canvas.drawRoundRect(pill, unit * 0.8f, unit * 0.8f, paint)

        text.color = Palette.HUD_TEXT
        text.textSize = unit * 0.95f
        text.textAlign = Paint.Align.LEFT
        val cy = pill.centerY()
        val textY = cy + text.textSize * 0.35f

        UiSprites.coin(canvas, pad + unit, cy, unit * 0.55f)
        canvas.drawText("${session.gold}", pad + unit * 1.8f, textY, text)

        UiSprites.heart(canvas, pad + unit * 4.6f, cy, unit * 0.55f)
        canvas.drawText("${session.lives}", pad + unit * 5.4f, textY, text)

        UiSprites.flag(canvas, pad + unit * 7.6f, cy, unit * 0.55f, Palette.HUD_TEXT)
        canvas.drawText("${session.waveNumber}/${session.totalWaves}", pad + unit * 8.4f, textY, text)

        // pause + speed buttons
        button(canvas, pauseBtn, active = paused)
        if (paused) {
            UiSprites.play(canvas, pauseBtn.centerX(), pauseBtn.centerY(), unit * 0.7f, Palette.HUD_TEXT)
        } else {
            UiSprites.pause(canvas, pauseBtn.centerX(), pauseBtn.centerY(), unit * 0.7f, Palette.HUD_TEXT)
        }

        button(canvas, speedBtn, active = speed > 1)
        text.textAlign = Paint.Align.CENTER
        canvas.drawText("${speed}x", speedBtn.centerX(), speedBtn.centerY() + text.textSize * 0.35f, text)

        // send wave button
        val canSend = session.canStartNextWave
        paint.color = when {
            !canSend -> Palette.BUTTON_DISABLED
            session.waveInProgress -> Palette.BUTTON
            else -> 0xFF3E7D46.toInt()
        }
        canvas.drawRoundRect(sendWaveBtn, unit * 0.5f, unit * 0.5f, paint)
        text.color = if (canSend) Palette.HUD_TEXT else 0xFF77808C.toInt()
        text.textSize = unit * 0.9f
        val label = if (canSend) "SEND WAVE ${session.waveNumber + 1}" else "LAST WAVE"
        canvas.drawText(label, sendWaveBtn.centerX(), sendWaveBtn.centerY() + text.textSize * 0.35f, text)

        drawHeroButtons(canvas, session, placingHero, time)
    }

    private fun drawHeroButtons(canvas: Canvas, session: GameSession, placingHero: Boolean, time: Float) {
        val hero = session.hero
        text.textAlign = Paint.Align.CENTER
        text.textSize = unit * 0.78f

        // deploy / move button
        paint.color = when {
            placingHero -> Palette.BUTTON_ACTIVE
            hero == null -> {
                // pulse to invite the first deployment
                val pulse = (0.5f + 0.5f * kotlin.math.sin(time * 4f))
                blend(0xFF3E7D46.toInt(), Palette.BUTTON_ACTIVE, pulse)
            }
            hero.canRelocate -> Palette.BUTTON
            else -> Palette.BUTTON_DISABLED
        }
        canvas.drawRoundRect(heroBtn, unit * 0.5f, unit * 0.5f, paint)
        // crescent mark
        val mcx = heroBtn.left + unit * 0.9f
        val mcy = heroBtn.centerY()
        paint.color = 0xFFD8DCE8.toInt()
        canvas.drawCircle(mcx, mcy, unit * 0.42f, paint)
        paint.color = when {
            placingHero -> Palette.BUTTON_ACTIVE
            hero == null -> 0xFF3E7D46.toInt()
            hero.canRelocate -> Palette.BUTTON
            else -> Palette.BUTTON_DISABLED
        }
        canvas.drawCircle(mcx + unit * 0.16f, mcy - unit * 0.10f, unit * 0.34f, paint)
        text.color = Palette.HUD_TEXT
        val heroLabel = when {
            placingHero -> "TAP A TILE"
            hero == null -> "DEPLOY KAGE"
            else -> "MOVE"
        }
        canvas.drawText(heroLabel, heroBtn.centerX() + unit * 0.5f, heroBtn.centerY() + text.textSize * 0.35f, text)

        // ability button with radial cooldown sweep
        if (hero != null) {
            val ready = hero.canUseAbility
            paint.color = if (ready) 0xFF5A3D7A.toInt() else Palette.BUTTON_DISABLED
            canvas.drawRoundRect(abilityBtn, unit * 0.5f, unit * 0.5f, paint)
            if (!ready) {
                val frac = (hero.abilityCooldown / com.moonshade.shadowvillage.core.data.HeroData.ABILITY_COOLDOWN)
                    .coerceIn(0f, 1f)
                paint.color = 0x66000000
                canvas.drawArc(
                    abilityBtn.left, abilityBtn.top, abilityBtn.right, abilityBtn.bottom,
                    -90f, 360f * frac, true, paint,
                )
            }
            text.color = if (ready) Palette.HUD_TEXT else 0xFF77808C.toInt()
            text.textSize = unit * 0.62f
            canvas.drawText("STRIKE", abilityBtn.centerX(), abilityBtn.centerY() + text.textSize * 0.35f, text)
        }
    }

    private fun blend(a: Int, b: Int, t: Float): Int {
        fun ch(sa: Int, sb: Int) = (sa + ((sb - sa) * t)).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or
            (ch((a shr 16) and 0xFF, (b shr 16) and 0xFF) shl 16) or
            (ch((a shr 8) and 0xFF, (b shr 8) and 0xFF) shl 8) or
            ch(a and 0xFF, b and 0xFF)
    }

    private fun button(canvas: Canvas, rect: RectF, active: Boolean) {
        paint.color = if (active) Palette.BUTTON_ACTIVE else Palette.BUTTON
        canvas.drawRoundRect(rect, unit * 0.4f, unit * 0.4f, paint)
    }
}
