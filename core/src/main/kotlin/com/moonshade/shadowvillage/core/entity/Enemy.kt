package com.moonshade.shadowvillage.core.entity

import com.moonshade.shadowvillage.core.data.EnemyData
import com.moonshade.shadowvillage.core.data.EnemyType
import com.moonshade.shadowvillage.core.map.GameMap
import com.moonshade.shadowvillage.core.math.Vec2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class Enemy(
    val id: Int,
    val type: EnemyType,
    hpScale: Float,
    private val map: GameMap,
) {
    val stats = EnemyData.of(type)
    val maxHp: Float = (stats.hp * hpScale).roundToInt().toFloat()
    var hp: Float = maxHp
        private set
    var pathDistance: Float = 0f
        private set
    var pos: Vec2 = map.positionAt(0f)
        private set
    var alive = true
    var reachedGoal = false
        private set
    val flying get() = stats.flying

    private var burn: StatusEffect.Burn? = null
    private var slow: StatusEffect.Slow? = null
    private var stun: StatusEffect.Stun? = null

    val burning get() = burn != null
    val slowed get() = slow != null
    val stunned get() = stun != null

    /** Current speed multiplier after slows and stuns. */
    val speedMul: Float get() = if (stunned) 0f else 1f - (slow?.factor ?: 0f)

    fun applyBurn(dps: Float, duration: Float) {
        val current = burn
        if (current == null || dps > current.dps) {
            burn = StatusEffect.Burn(dps, duration)
        } else if (dps == current.dps) {
            current.remaining = max(current.remaining, duration)
        }
    }

    fun applySlow(factor: Float, duration: Float) {
        val capped = min(factor, stats.slowCap)
        if (capped <= 0f) return
        val current = slow
        if (current == null || capped > current.factor) {
            slow = StatusEffect.Slow(capped, duration)
        } else if (capped == current.factor) {
            current.remaining = max(current.remaining, duration)
        }
    }

    fun applyStun(duration: Float) {
        // Control-immune enemies (bosses) shrug stuns off like knockbacks.
        if (stats.knockbackImmune) return
        val current = stun
        if (current == null) {
            stun = StatusEffect.Stun(duration)
        } else {
            current.remaining = max(current.remaining, duration)
        }
    }

    fun applyKnockback(cells: Float) {
        if (stats.knockbackImmune) return
        pathDistance = max(0f, pathDistance - cells)
        pos = map.positionAt(pathDistance)
    }

    /** Applies armor-reduced damage; returns the amount actually dealt. */
    fun takeDamage(raw: Float): Float {
        if (!alive) return 0f
        val applied = max(1f, raw - stats.armor)
        hp -= applied
        if (hp <= 0f) alive = false
        return applied
    }

    /** Burn damage ignores armor. */
    private fun takeBurnDamage(amount: Float) {
        hp -= amount
        if (hp <= 0f) alive = false
    }

    /** Test helper: place the enemy at an exact path distance. */
    internal fun teleportTo(distance: Float) {
        pathDistance = distance
        pos = map.positionAt(distance)
    }

    fun update(dt: Float) {
        if (!alive) return

        burn?.let {
            takeBurnDamage(it.dps * dt)
            it.remaining -= dt
            if (it.remaining <= 0f) burn = null
        }
        if (!alive) return

        slow?.let {
            it.remaining -= dt
            if (it.remaining <= 0f) slow = null
        }

        stun?.let {
            it.remaining -= dt
            if (it.remaining <= 0f) stun = null
        }

        if (stats.regenPerSec > 0f) {
            hp = min(maxHp, hp + stats.regenPerSec * dt)
        }

        pathDistance += stats.speed * speedMul * dt
        if (pathDistance >= map.totalPathLength) {
            reachedGoal = true
            alive = false
        }
        pos = map.positionAt(pathDistance)
    }
}
