package com.moonshade.shadowvillage.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.moonshade.shadowvillage.core.map.GameMap
import com.moonshade.shadowvillage.core.map.TileType

/**
 * Pre-bakes the whole map (grass, dirt path, blocked scenery, gates) into
 * one bitmap so per-frame map drawing is a single blit. The theme decides
 * what BLOCKED cells look like (ponds at midday, stone lanterns at dusk).
 */
class MapRenderer(private val map: GameMap, private val theme: MapTheme) {

    private var background: Bitmap? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun invalidate() {
        background?.recycle()
        background = null
    }

    fun draw(canvas: Canvas, cam: Camera, width: Int, height: Int) {
        val bg = background ?: bake(cam, width, height).also { background = it }
        canvas.drawBitmap(bg, 0f, 0f, null)
    }

    private fun bake(cam: Camera, width: Int, height: Int): Bitmap {
        val bmp = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val cs = cam.cellSize

        // letterbox backdrop
        c.drawColor(0xFF141A2C.toInt())

        for (row in 0 until map.rows) {
            for (col in 0 until map.cols) {
                val left = cam.worldX(col.toFloat())
                val top = cam.worldY(row.toFloat())
                val rect = RectF(left, top, left + cs, top + cs)
                when (map.tileAt(col, row)) {
                    TileType.BUILDABLE -> grass(c, rect, col, row)
                    TileType.PATH, TileType.SPAWN, TileType.GOAL -> dirt(c, rect)
                    TileType.BLOCKED -> {
                        grass(c, rect, col, row)
                        when (theme.blockedStyle) {
                            BlockedStyle.POND -> pond(c, rect)
                            BlockedStyle.STONE_LANTERN -> lantern(c, rect)
                        }
                    }
                }
            }
        }

        // gates over the path ends
        gate(c, cam, map.waypoints.first().x, map.waypoints.first().y, Palette.SPAWN_GATE)
        gate(c, cam, map.waypoints.last().x, map.waypoints.last().y, Palette.GOAL_GATE)
        return bmp
    }

    private fun grass(c: Canvas, r: RectF, col: Int, row: Int) {
        paint.style = Paint.Style.FILL
        paint.color = if ((col + row) % 2 == 0) Palette.GRASS else Palette.GRASS_LIGHT
        c.drawRect(r, paint)
        // deterministic tufts
        paint.color = Palette.GRASS_TUFT
        val seed = col * 31 + row * 17
        val n = seed % 3
        for (i in 0 until n) {
            val fx = r.left + ((seed * (i + 7)) % 80 + 10) / 100f * r.width()
            val fy = r.top + ((seed * (i + 13)) % 80 + 10) / 100f * r.height()
            c.drawCircle(fx, fy, r.width() * 0.04f, paint)
        }
    }

    private fun dirt(c: Canvas, r: RectF) {
        paint.style = Paint.Style.FILL
        paint.color = Palette.PATH_DIRT
        c.drawRect(r, paint)
        paint.color = Palette.PATH_DIRT_DARK
        c.drawCircle(r.centerX() - r.width() * 0.2f, r.centerY() + r.height() * 0.15f, r.width() * 0.08f, paint)
        c.drawCircle(r.centerX() + r.width() * 0.25f, r.centerY() - r.height() * 0.2f, r.width() * 0.06f, paint)
    }

    /** Still water with a rocky rim; ripples are animated live on top. */
    private fun pond(c: Canvas, r: RectF) {
        paint.style = Paint.Style.FILL
        paint.color = Palette.ROCK_DARK
        c.drawRoundRect(
            RectF(r.left + r.width() * 0.04f, r.top + r.height() * 0.06f, r.right - r.width() * 0.04f, r.bottom - r.height() * 0.02f),
            r.width() * 0.30f, r.height() * 0.30f, paint,
        )
        paint.color = 0xFF2F6F94.toInt()
        val water = RectF(r.left + r.width() * 0.12f, r.top + r.height() * 0.14f, r.right - r.width() * 0.12f, r.bottom - r.height() * 0.10f)
        c.drawRoundRect(water, r.width() * 0.26f, r.height() * 0.26f, paint)
        paint.color = 0xFF3FA7D6.toInt()
        c.drawRoundRect(
            RectF(water.left + r.width() * 0.06f, water.top + r.height() * 0.06f, water.right - r.width() * 0.06f, water.bottom - r.height() * 0.10f),
            r.width() * 0.22f, r.height() * 0.22f, paint,
        )
        // a couple of rim stones
        paint.color = Palette.ROCK
        c.drawCircle(r.left + r.width() * 0.16f, r.top + r.height() * 0.22f, r.width() * 0.08f, paint)
        c.drawCircle(r.right - r.width() * 0.18f, r.bottom - r.height() * 0.18f, r.width() * 0.07f, paint)
    }

    /** Stone lantern; its warm glow is animated live by the atmosphere. */
    private fun lantern(c: Canvas, r: RectF) {
        paint.style = Paint.Style.FILL
        val w = r.width()
        // pedestal, post, light box, cap
        paint.color = Palette.ROCK_DARK
        c.drawRect(r.centerX() - w * 0.18f, r.bottom - w * 0.22f, r.centerX() + w * 0.18f, r.bottom - w * 0.12f, paint)
        paint.color = Palette.ROCK
        c.drawRect(r.centerX() - w * 0.07f, r.centerY(), r.centerX() + w * 0.07f, r.bottom - w * 0.20f, paint)
        c.drawRoundRect(
            RectF(r.centerX() - w * 0.16f, r.top + w * 0.30f, r.centerX() + w * 0.16f, r.centerY() + w * 0.05f),
            w * 0.04f, w * 0.04f, paint,
        )
        paint.color = 0xFFFFC93C.toInt()
        c.drawRect(r.centerX() - w * 0.08f, r.top + w * 0.36f, r.centerX() + w * 0.08f, r.centerY() - w * 0.02f, paint)
        paint.color = Palette.ROCK_DARK
        val cap = Path().apply {
            moveTo(r.centerX() - w * 0.22f, r.top + w * 0.30f)
            lineTo(r.centerX(), r.top + w * 0.12f)
            lineTo(r.centerX() + w * 0.22f, r.top + w * 0.30f)
            close()
        }
        c.drawPath(cap, paint)
        paint.color = Palette.OUTLINE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.03f
        c.drawPath(cap, paint)
        paint.style = Paint.Style.FILL
    }

    private fun gate(c: Canvas, cam: Camera, x: Float, y: Float, color: Int) {
        val cs = cam.cellSize
        val cx = cam.worldX(x)
        val cy = cam.worldY(y)
        paint.style = Paint.Style.FILL
        paint.color = color
        // torii-like gate: two posts and a beam
        val w = cs * 0.38f
        val h = cs * 0.42f
        c.drawRect(cx - w, cy - h, cx - w + cs * 0.10f, cy + h, paint)
        c.drawRect(cx + w - cs * 0.10f, cy - h, cx + w, cy + h, paint)
        c.drawRoundRect(RectF(cx - w * 1.25f, cy - h, cx + w * 1.25f, cy - h + cs * 0.12f), cs * 0.04f, cs * 0.04f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = cs * 0.03f
        paint.color = Palette.OUTLINE
        c.drawRect(cx - w, cy - h, cx + w, cy + h, paint)
        paint.style = Paint.Style.FILL
    }
}
