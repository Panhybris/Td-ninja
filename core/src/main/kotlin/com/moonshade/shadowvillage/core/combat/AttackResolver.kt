package com.moonshade.shadowvillage.core.combat

import com.moonshade.shadowvillage.core.entity.Enemy
import com.moonshade.shadowvillage.core.math.Vec2

object AttackResolver {

    /**
     * Picks chain-lightning victims: the first target plus up to
     * `count - 1` hops, each to the nearest unhit living enemy within
     * [hopRadius] of the previous link.
     */
    fun chainTargets(first: Enemy, enemies: List<Enemy>, count: Int, hopRadius: Float): List<Enemy> {
        val chain = mutableListOf(first)
        var current = first
        while (chain.size < count) {
            val next = enemies
                .filter { it.alive && it !in chain }
                .filter { it.pos.distanceTo(current.pos) <= hopRadius }
                .minByOrNull { it.pos.distanceTo(current.pos) }
                ?: break
            chain += next
            current = next
        }
        return chain
    }

    /** Ground enemies within [radius] of [center] (splash never hits flying). */
    fun splashVictims(center: Vec2, enemies: List<Enemy>, radius: Float): List<Enemy> =
        enemies.filter { it.alive && !it.flying && it.pos.distanceTo(center) <= radius }
}
