package com.moonshade.shadowvillage.render

import android.graphics.Canvas
import android.graphics.LightingColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.moonshade.shadowvillage.core.data.Element
import com.moonshade.shadowvillage.core.data.EnemyType
import com.moonshade.shadowvillage.core.game.GameSession
import com.moonshade.shadowvillage.core.math.Vec2
import com.moonshade.shadowvillage.render.sprites.EnemySprites
import com.moonshade.shadowvillage.render.sprites.HeroPose
import com.moonshade.shadowvillage.render.sprites.NinjaPose
import com.moonshade.shadowvillage.render.sprites.ProjectileSprites
import com.moonshade.shadowvillage.render.sprites.SpriteCache
import kotlin.math.sin

/** Draws towers, the hero, enemies, and projectiles from session state. */
class GameRenderer(private val sprites: SpriteCache) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val flashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = LightingColorFilter(0xFFFFFFFF.toInt(), 0x606060)
    }
    private var animTime = 0f

    // transient animation state fed by EffectEvents via PlayScreen
    private val attackTimers = HashMap<Int, Float>()   // towerId -> remaining attack-pose time
    private val facing = HashMap<Int, Float>()         // towerId -> +1 right / -1 left
    private val hitFlashes = HashMap<Int, Float>()     // enemyId -> remaining flash time
    private var heroAttackTimer = 0f
    private var heroDashTimer = 0f
    private var lastHeroPos: Vec2? = null

    fun onTowerFired(towerId: Int, towerPos: Vec2, targetPos: Vec2) {
        attackTimers[towerId] = 0.25f
        facing[towerId] = if (targetPos.x < towerPos.x) -1f else 1f
    }

    fun onEnemyDamaged(enemyId: Int) {
        hitFlashes[enemyId] = 0.1f
    }

    fun onHeroAttack() {
        heroAttackTimer = 0.25f
    }

    fun tick(dt: Float) {
        animTime += dt
        decay(attackTimers, dt)
        decay(hitFlashes, dt)
        heroAttackTimer -= dt
        heroDashTimer -= dt
    }

    private fun decay(timers: HashMap<Int, Float>, dt: Float) {
        val it = timers.entries.iterator()
        while (it.hasNext()) {
            val e = it.next()
            e.setValue(e.value - dt)
            if (e.value <= 0f) it.remove()
        }
    }

    fun draw(canvas: Canvas, session: GameSession, cam: Camera) {
        val cs = cam.cellSize

        drawTowers(canvas, session, cam, cs)
        drawHero(canvas, session, cam, cs)
        drawEnemies(canvas, session, cam, cs)

        for (p in session.projectiles) {
            drawProjectileTrail(canvas, p.element, cam.worldX(p.pos), cam.worldY(p.pos), p.aimPoint, p.pos, cs)
            ProjectileSprites.draw(
                canvas, p.element,
                cam.worldX(p.pos), cam.worldY(p.pos),
                cs * 0.14f, animTime * 14f,
            )
        }
    }

    private fun drawTowers(canvas: Canvas, session: GameSession, cam: Camera, cs: Float) {
        val towerSize = (cs * 0.92f).toInt()
        for (tower in session.towers) {
            val cx = cam.worldX(tower.pos)
            val cy = cam.worldY(tower.pos)

            // T3 aura: pulsing ground glow
            if (tower.tier >= 3) {
                val colors = Palette.of(tower.element)
                val r = cs * (0.55f + 0.05f * sin(animTime * 3f + tower.id))
                paint.shader = RadialGradient(
                    cx, cy + cs * 0.25f, r,
                    (colors.base and 0x00FFFFFF) or 0x50000000,
                    colors.base and 0x00FFFFFF,
                    Shader.TileMode.CLAMP,
                )
                canvas.drawCircle(cx, cy + cs * 0.25f, r, paint)
                paint.shader = null
            }

            val attackT = attackTimers[tower.id]
            val pose = if (attackT != null) NinjaPose.ATTACK else NinjaPose.IDLE
            val bmp = sprites.ninja(tower.element, tower.tier, tower.spec, pose, towerSize)

            canvas.save()
            // face the target; recoil-lean while the attack pose is live
            val face = facing[tower.id] ?: 1f
            if (face < 0f) canvas.scale(-1f, 1f, cx, cy)
            if (attackT != null) {
                canvas.rotate(-6f * (attackT / 0.25f), cx, cy + cs * 0.3f)
            }
            // idle sway
            canvas.translate(0f, sin(animTime * 2f + tower.id * 1.7f) * cs * 0.012f)
            canvas.drawBitmap(bmp, cx - towerSize / 2f, cy - towerSize / 2f, null)
            canvas.restore()
        }
    }

    private fun drawHero(canvas: Canvas, session: GameSession, cam: Camera, cs: Float) {
        val hero = session.hero ?: return

        // detect relocation: pos jump triggers the dash pose
        val last = lastHeroPos
        if (last != null && last.distanceTo(hero.pos) > 0.01f) {
            heroDashTimer = 0.2f
        }
        lastHeroPos = hero.pos

        val pose = when {
            heroDashTimer > 0f -> HeroPose.DASH
            heroAttackTimer > 0f -> HeroPose.ATTACK
            else -> HeroPose.IDLE
        }
        val size = (cs * 1.05f).toInt()
        val cx = cam.worldX(hero.pos)
        val cy = cam.worldY(hero.pos)

        // shadow
        paint.color = 0x40000000
        canvas.drawOval(cx - size * 0.28f, cy + size * 0.34f, cx + size * 0.28f, cy + size * 0.46f, paint)

        canvas.save()
        canvas.translate(0f, sin(animTime * 2.2f) * cs * 0.015f)
        canvas.drawBitmap(sprites.hero(pose, size), cx - size / 2f, cy - size / 2f, null)
        canvas.restore()
    }

    private fun drawEnemies(canvas: Canvas, session: GameSession, cam: Camera, cs: Float) {
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

            // walk frame from distance travelled: slowed enemies trudge,
            // stunned ones freeze mid-stride
            val frame = if (enemy.flying) {
                if ((animTime * 2f + enemy.id).toInt() % 2 == 0) 0 else 2
            } else {
                (((enemy.pathDistance * 1.2f) % 1f) * EnemySprites.WALK_FRAMES).toInt()
                    .coerceIn(0, EnemySprites.WALK_FRAMES - 1)
            }

            // shadow
            paint.color = 0x40000000
            canvas.drawOval(
                cam.worldX(enemy.pos) - size * 0.30f,
                cam.worldY(enemy.pos) + size * 0.30f,
                cam.worldX(enemy.pos) + size * 0.30f,
                cam.worldY(enemy.pos) + size * 0.45f,
                paint,
            )

            val bmp = sprites.enemy(enemy.type, frame, size)
            if (hitFlashes.containsKey(enemy.id)) {
                canvas.drawBitmap(bmp, ex, ey, flashPaint)
            } else {
                canvas.drawBitmap(bmp, ex, ey, null)
            }

            // status indicators
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
            if (enemy.stunned) {
                paint.color = 0xFFFFE68A.toInt()
                for (i in 0 until 3) {
                    val ang = animTime * 6f + i * 2.09f
                    canvas.drawCircle(
                        cam.worldX(enemy.pos) + size * 0.4f * sin(ang),
                        ey - cs * 0.05f + size * 0.12f * sin(ang * 2f),
                        size * 0.05f, paint,
                    )
                }
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
    }

    /** Stateless ghost trail: three decaying circles behind the projectile. */
    private fun drawProjectileTrail(
        canvas: Canvas,
        element: Element,
        px: Float,
        py: Float,
        aim: Vec2,
        pos: Vec2,
        cs: Float,
    ) {
        val dir = (pos - aim).normalized()
        val light = Palette.of(element).light
        for (i in 1..3) {
            paint.color = (light and 0x00FFFFFF) or ((0x50 / i) shl 24)
            canvas.drawCircle(
                px + dir.x * cs * 0.16f * i,
                py + dir.y * cs * 0.16f * i,
                cs * 0.10f / i + cs * 0.02f, paint,
            )
        }
    }
}
