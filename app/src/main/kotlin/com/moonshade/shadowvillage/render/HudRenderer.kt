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
    }

    fun draw(canvas: Canvas, session: GameSession, speed: Int, paused: Boolean) {
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
    }

    private fun button(canvas: Canvas, rect: RectF, active: Boolean) {
        paint.color = if (active) Palette.BUTTON_ACTIVE else Palette.BUTTON
        canvas.drawRoundRect(rect, unit * 0.4f, unit * 0.4f, paint)
    }
}
