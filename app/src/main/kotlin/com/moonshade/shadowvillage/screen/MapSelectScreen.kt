package com.moonshade.shadowvillage.screen

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import com.moonshade.shadowvillage.core.map.GameMap
import com.moonshade.shadowvillage.core.map.Maps
import com.moonshade.shadowvillage.core.map.TileType
import com.moonshade.shadowvillage.render.Palette
import com.moonshade.shadowvillage.render.sprites.UiSprites

class MapSelectScreen(private val screens: ScreenManager) : Screen {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    private var width = 0
    private var height = 0
    private val cards = mutableListOf<Pair<RectF, GameMap>>()

    private val subtitles = mapOf(
        "river_crossing" to "Beginner - one winding road",
        "twin_gates" to "Veteran - long switchback",
    )

    override fun onSizeChanged(width: Int, height: Int) {
        this.width = width
        this.height = height
        cards.clear()
        val cardW = width * 0.38f
        val cardH = cardW * 9f / 16f
        val gap = width * 0.08f
        val top = height * 0.30f
        var x = (width - 2 * cardW - gap) / 2f
        for (map in Maps.all) {
            cards += RectF(x, top, x + cardW, top + cardH) to map
            x += cardW + gap
        }
    }

    override fun update(dt: Float) {}

    override fun draw(canvas: Canvas) {
        if (width == 0) return
        canvas.drawColor(0xFF1B2238.toInt())

        text.color = Palette.HUD_TEXT
        text.textSize = height * 0.08f
        canvas.drawText("CHOOSE A BATTLEGROUND", width / 2f, height * 0.16f, text)

        for ((rect, map) in cards) {
            val unlocked = screens.progress.isUnlocked(map.id)

            // mini map preview
            val cell = rect.width() / map.cols
            for (row in 0 until map.rows) {
                for (col in 0 until map.cols) {
                    paint.color = when (map.tileAt(col, row)) {
                        TileType.PATH, TileType.SPAWN, TileType.GOAL -> Palette.PATH_DIRT
                        TileType.BLOCKED -> Palette.ROCK
                        TileType.BUILDABLE -> Palette.GRASS
                    }
                    canvas.drawRect(
                        rect.left + col * cell, rect.top + row * cell,
                        rect.left + (col + 1) * cell, rect.top + (row + 1) * cell, paint,
                    )
                }
            }
            paint.color = Palette.OUTLINE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = height * 0.008f
            canvas.drawRect(rect, paint)
            paint.style = Paint.Style.FILL

            if (!unlocked) {
                paint.color = 0xB0101522.toInt()
                canvas.drawRect(rect, paint)
                // padlock
                paint.color = Palette.HUD_TEXT
                val lw = rect.width() * 0.10f
                canvas.drawRoundRect(
                    RectF(rect.centerX() - lw, rect.centerY() - lw * 0.4f, rect.centerX() + lw, rect.centerY() + lw * 1.1f),
                    lw * 0.2f, lw * 0.2f, paint,
                )
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = lw * 0.35f
                canvas.drawArc(
                    RectF(rect.centerX() - lw * 0.6f, rect.centerY() - lw * 1.4f, rect.centerX() + lw * 0.6f, rect.centerY()),
                    180f, 180f, false, paint,
                )
                paint.style = Paint.Style.FILL
            }

            // star rating row
            val starR = height * 0.022f
            for (i in 0 until 3) {
                UiSprites.star(
                    canvas, rect.centerX() + (i - 1) * starR * 2.6f, rect.bottom + height * 0.035f,
                    starR, i < screens.progress.stars(map.id),
                )
            }

            text.textSize = height * 0.055f
            text.color = Palette.HUD_TEXT
            canvas.drawText(map.name, rect.centerX(), rect.bottom + height * 0.11f, text)
            text.textSize = height * 0.035f
            text.color = 0xFFB9C2D6.toInt()
            val sub = if (unlocked) {
                subtitles[map.id] ?: ""
            } else {
                val prev = Maps.all[Maps.all.indexOfFirst { it.id == map.id } - 1]
                "Earn a star on ${prev.name} to unlock"
            }
            canvas.drawText(sub, rect.centerX(), rect.bottom + height * 0.16f, text)
        }
    }

    override fun onTouch(event: MotionEvent) {
        if (event.action != MotionEvent.ACTION_DOWN) return
        for ((rect, map) in cards) {
            if (rect.contains(event.x, event.y) && screens.progress.isUnlocked(map.id)) {
                screens.navigate(PlayScreen(screens, map))
                return
            }
        }
    }
}
