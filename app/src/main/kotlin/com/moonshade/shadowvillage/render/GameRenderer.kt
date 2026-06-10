package com.moonshade.shadowvillage.render

import android.graphics.Canvas
import android.graphics.Paint
import com.moonshade.shadowvillage.core.data.Element
import com.moonshade.shadowvillage.core.data.EnemyType
import com.moonshade.shadowvillage.core.game.GameSession
import com.moonshade.shadowvillage.render.sprites.ProjectileSprites
import com.moonshade.shadowvillage.render.sprites.SpriteCache
import kotlin.math.sin

/** Draws towers, enemies, and projectiles from the session state. */
class GameRenderer(private val sprites: SpriteCache) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var animTime = 0f

    fun tick(dt: Float) {
        animTime += dt
    }

    fun draw(canvas: Canvas, session: GameSession, cam: Camera) {
        val cs = cam.cellSize

        // towers
        val towerSize = (cs * 0.92f).toInt()
        for (tower in session.towers) {
            val bmp = sprites.ninja(tower.element, tower.tier, towerSize)
            canvas.drawBitmap(
                bmp,
                cam.worldX(tower.col.toFloat()) + (cs - towerSize) / 2f,
                cam.worldY(tower.row.toFloat()) + (cs - towerSize) / 2f,
                null,
            )
        }

        // enemies (flying drawn last, on top, with a shadow offset)
        for (enemy in session.enemies.sortedBy { it.flying }) {
            val scale = when (enemy.type) {
                EnemyType.ONI_WARLORD -> 1.5f
                EnemyType.ONI_VANGUARD -> 1.2f
                else -> 0.78f
            }
            val size = (cs * scale).toInt()
            val bob = sin((animTime * 6f + enemy.id)) * cs * 0.03f
            val ex = cam.worldX(enemy.pos) - size / 2f
            val ey = cam.worldY(enemy.pos) - size / 2f + bob - (if (enemy.flying) cs * 0.25f else 0f)

            // shadow
            paint.color = 0x40000000
            canvas.drawOval(
                cam.worldX(enemy.pos) - size * 0.30f,
                cam.worldY(enemy.pos) + size * 0.30f,
                cam.worldX(enemy.pos) + size * 0.30f,
                cam.worldY(enemy.pos) + size * 0.45f,
                paint,
            )

            canvas.drawBitmap(sprites.enemy(enemy.type, size), ex, ey, null)

            // status tints
            if (enemy.slowed) {
                paint.color = 0x553FA7D6
                canvas.drawCircle(cam.worldX(enemy.pos), ey + size / 2f, size * 0.55f, paint)
            }
            if (enemy.burning) {
                paint.color = Palette.of(Element.FIRE).base
                val fr = size * 0.10f
                canvas.drawCircle(ex + size * 0.2f, ey + fr + sin(animTime * 20f) * fr * 0.4f, fr, paint)
                canvas.drawCircle(ex + size * 0.8f, ey + fr * 1.6f + sin(animTime * 17f + 2) * fr * 0.4f, fr * 0.8f, paint)
            }

            // hp bar when damaged
            if (enemy.hp < enemy.maxHp) {
                val w = size * 0.9f
                val x = cam.worldX(enemy.pos) - w / 2f
                val y = ey - cs * 0.10f
                paint.color = Palette.HP_BAR_BG
                canvas.drawRect(x, y, x + w, y + cs * 0.07f, paint)
                paint.color = Palette.HP_BAR
                canvas.drawRect(x, y, x + w * (enemy.hp / enemy.maxHp), y + cs * 0.07f, paint)
            }
        }

        // projectiles
        for (p in session.projectiles) {
            ProjectileSprites.draw(
                canvas, p.element,
                cam.worldX(p.pos), cam.worldY(p.pos),
                cs * 0.14f, animTime * 14f,
            )
        }
    }
}
