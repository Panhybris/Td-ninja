package com.moonshade.shadowvillage.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.moonshade.shadowvillage.core.map.GameMap
import com.moonshade.shadowvillage.core.map.TileType
import kotlin.math.sin

enum class BlockedStyle { POND, STONE_LANTERN }
enum class ParticleStyle { PETALS, FIREFLIES }

data class MapTheme(
    val id: String,
    /** ARGB tint drawn over the world, under the HUD. */
    val tint: Int,
    val blockedStyle: BlockedStyle,
    val particle: ParticleStyle,
) {
    companion object {
        fun forMap(mapId: String): MapTheme = when (mapId) {
            "twin_gates" -> MapTheme("dusk", 0x301A2240, BlockedStyle.STONE_LANTERN, ParticleStyle.FIREFLIES)
            else -> MapTheme("midday", 0x14FFD9A0, BlockedStyle.POND, ParticleStyle.PETALS)
        }
    }
}

/**
 * Ambient life layered around the baked map: pond ripples or lantern
 * glow on BLOCKED cells, drifting particles, and a scene tint. Pools are
 * fixed-size; nothing allocates per frame.
 */
class AtmosphereRenderer(private val map: GameMap, val theme: MapTheme) {

    private class Particle {
        var x = 0f
        var y = 0f
        var seed = 0f
        var age = 0f
        var ttl = 1f
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var time = 0f

    private val blockedCells: List<Pair<Int, Int>> = buildList {
        for (r in 0 until map.rows) for (c in 0 until map.cols) {
            if (map.tileAt(c, r) == TileType.BLOCKED) add(c to r)
        }
    }

    private val particles = Array(if (theme.particle == ParticleStyle.PETALS) 10 else 12) {
        Particle().also { p -> reset(p, it * 0.83f, randomize = true) }
    }

    private fun reset(p: Particle, seed: Float, randomize: Boolean) {
        p.seed = seed
        p.x = (seed * 7.13f) % map.cols
        p.y = if (randomize) (seed * 3.71f) % map.rows else -0.5f
        p.age = 0f
        p.ttl = 6f + (seed * 13f) % 5f
    }

    fun update(dt: Float) {
        time += dt
        for (p in particles) {
            p.age += dt
            when (theme.particle) {
                ParticleStyle.PETALS -> {
                    p.x += dt * (0.35f + 0.1f * sin(p.seed * 9f))
                    p.y += dt * (0.22f + 0.08f * sin(time * 1.3f + p.seed * 11f))
                    if (p.age > p.ttl || p.x > map.cols + 0.5f || p.y > map.rows + 0.5f) {
                        reset(p, p.seed + 1.37f, randomize = false)
                        p.x = (p.seed * 5.91f) % map.cols
                    }
                }
                ParticleStyle.FIREFLIES -> {
                    p.x += dt * 0.25f * sin(time * 0.7f + p.seed * 8f)
                    p.y += dt * 0.20f * sin(time * 0.9f + p.seed * 5f)
                    if (p.age > p.ttl) reset(p, p.seed + 2.11f, randomize = true)
                }
            }
        }
    }

    /** Pond ripples / lantern bases; drawn after the map, before entities. */
    fun drawBehindEntities(canvas: Canvas, cam: Camera) {
        val cs = cam.cellSize
        when (theme.blockedStyle) {
            BlockedStyle.POND -> for ((c, r) in blockedCells) {
                val cx = cam.worldX(c + 0.5f)
                val cy = cam.worldY(r + 0.5f)
                // expanding ripple rings
                paint.style = Paint.Style.STROKE
                for (i in 0 until 2) {
                    val t = ((time * 0.45f + i * 0.5f + c * 0.13f) % 1f)
                    paint.strokeWidth = cs * 0.03f * (1f - t)
                    paint.color = (0xFFFFFFFF.toInt() and 0x00FFFFFF) or (((90 * (1f - t)).toInt()) shl 24)
                    canvas.drawCircle(cx, cy, cs * 0.12f + cs * 0.28f * t, paint)
                }
                paint.style = Paint.Style.FILL
                // moving specular glint
                paint.color = 0x66FFFFFF
                canvas.drawCircle(
                    cx + cs * 0.18f * sin(time * 0.8f + r),
                    cy - cs * 0.10f + cs * 0.05f * sin(time * 1.1f + c),
                    cs * 0.05f, paint,
                )
            }
            BlockedStyle.STONE_LANTERN -> Unit // glow is drawn over entities
        }
    }

    /** Tint, particles, lantern glow; drawn after entities, before HUD. */
    fun drawOverEntities(canvas: Canvas, cam: Camera, width: Int, height: Int) {
        val cs = cam.cellSize

        if (theme.blockedStyle == BlockedStyle.STONE_LANTERN) {
            for ((c, r) in blockedCells) {
                val cx = cam.worldX(c + 0.5f)
                val cy = cam.worldY(r + 0.35f)
                val pulse = 0.8f + 0.2f * sin(time * 2.2f + c * 1.7f)
                val gr = cs * 0.65f * pulse
                paint.shader = RadialGradient(
                    cx, cy, gr,
                    0x70FFC93C, 0x00FFC93C, Shader.TileMode.CLAMP,
                )
                canvas.drawCircle(cx, cy, gr, paint)
                paint.shader = null
            }
        }

        // scene tint
        paint.color = theme.tint
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // particles on top of the tint so they read as foreground
        when (theme.particle) {
            ParticleStyle.PETALS -> {
                for (p in particles) {
                    paint.color = 0xCCF2B8C6.toInt()
                    canvas.save()
                    canvas.rotate(time * 90f + p.seed * 100f, cam.worldX(p.x), cam.worldY(p.y))
                    canvas.drawOval(
                        cam.worldX(p.x) - cs * 0.06f, cam.worldY(p.y) - cs * 0.035f,
                        cam.worldX(p.x) + cs * 0.06f, cam.worldY(p.y) + cs * 0.035f, paint,
                    )
                    canvas.restore()
                }
            }
            ParticleStyle.FIREFLIES -> {
                for (p in particles) {
                    val twinkle = 0.5f + 0.5f * sin(time * 4f + p.seed * 20f)
                    paint.color = (0xFFE68A and 0xFFFFFF) or (((200 * twinkle).toInt()) shl 24)
                    canvas.drawCircle(cam.worldX(p.x), cam.worldY(p.y), cs * 0.035f, paint)
                }
            }
        }
    }
}
