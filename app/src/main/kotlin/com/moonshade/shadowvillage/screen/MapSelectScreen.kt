package com.moonshade.shadowvillage.screen

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import com.moonshade.shadowvillage.core.map.GameMap
import com.moonshade.shadowvillage.core.map.Maps
import com.moonshade.shadowvillage.core.map.TileType
import com.moonshade.shadowvillage.render.Palette

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

            text.textSize = height * 0.055f
            text.color = Palette.HUD_TEXT
            canvas.drawText(map.name, rect.centerX(), rect.bottom + height * 0.08f, text)
            text.textSize = height * 0.035f
            text.color = 0xFFB9C2D6.toInt()
            canvas.drawText(subtitles[map.id] ?: "", rect.centerX(), rect.bottom + height * 0.13f, text)
        }
    }

    override fun onTouch(event: MotionEvent) {
        if (event.action != MotionEvent.ACTION_DOWN) return
        for ((rect, map) in cards) {
            if (rect.contains(event.x, event.y)) {
                screens.navigate(PlayScreen(screens, map))
                return
            }
        }
    }
}
