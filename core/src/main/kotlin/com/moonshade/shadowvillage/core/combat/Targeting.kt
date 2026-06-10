package com.moonshade.shadowvillage.core.combat

import com.moonshade.shadowvillage.core.entity.Enemy
import com.moonshade.shadowvillage.core.entity.Tower

enum class Targeting(val displayName: String) {
    FIRST("First"),
    LAST("Last"),
    STRONG("Strong"),
    NEAR("Near");

    fun next(): Targeting = entries[(ordinal + 1) % entries.size]
}

object TargetSelector {

    /** Splash attacks are ground-only, so splash towers never target flyers. */
    fun canHit(tower: Tower, enemy: Enemy): Boolean =
        enemy.alive && !(tower.stats.splashRadius > 0f && enemy.flying)

    fun inRange(tower: Tower, enemy: Enemy): Boolean =
        tower.pos.distanceTo(enemy.pos) <= tower.stats.range + 1e-4f

    fun select(tower: Tower, enemies: List<Enemy>): Enemy? {
        val candidates = enemies.filter { canHit(tower, it) && inRange(tower, it) }
        if (candidates.isEmpty()) return null
        return when (tower.targeting) {
            Targeting.FIRST -> candidates.maxBy { it.pathDistance }
            Targeting.LAST -> candidates.minBy { it.pathDistance }
            Targeting.STRONG -> candidates.maxBy { it.hp }
            Targeting.NEAR -> candidates.minBy { tower.pos.distanceTo(it.pos) }
        }
    }
}
