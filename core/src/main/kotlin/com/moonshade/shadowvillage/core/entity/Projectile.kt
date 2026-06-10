package com.moonshade.shadowvillage.core.entity

import com.moonshade.shadowvillage.core.data.Element
import com.moonshade.shadowvillage.core.data.TierStats
import com.moonshade.shadowvillage.core.math.Vec2

/**
 * Homing for Fire/Water/Wind (tracks [target]); ballistic for Earth
 * (flies to a fixed point and splashes). Lightning never spawns projectiles.
 */
class Projectile(
    val id: Int,
    val element: Element,
    val stats: TierStats,
    var pos: Vec2,
    val target: Enemy?,
    targetPoint: Vec2?,
    /** True when this Wind shot is the every-Nth knockback shot. */
    val knockback: Boolean = false,
) {
    var aimPoint: Vec2 = targetPoint ?: target?.pos ?: pos
        private set
    var done = false

    /** Returns true on impact. */
    fun update(dt: Float): Boolean {
        target?.let { if (it.alive) aimPoint = it.pos }
        val toAim = aimPoint - pos
        val dist = toAim.length()
        val step = stats.projectileSpeed * dt
        if (dist <= step) {
            pos = aimPoint
            done = true
            return true
        }
        pos = pos + toAim.normalized() * step
        return false
    }
}
