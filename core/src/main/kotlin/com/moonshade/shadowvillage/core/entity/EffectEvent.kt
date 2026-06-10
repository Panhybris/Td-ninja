package com.moonshade.shadowvillage.core.entity

import com.moonshade.shadowvillage.core.data.Element
import com.moonshade.shadowvillage.core.data.EnemyType
import com.moonshade.shadowvillage.core.math.Vec2

/**
 * Transient render hints emitted by the simulation each tick. The renderer
 * consumes them to start animations (arcs, explosions, floating text); the
 * core stays render-agnostic.
 */
sealed class EffectEvent {
    /** Chain lightning arc: tower position followed by each struck enemy. */
    data class LightningArc(val points: List<Vec2>) : EffectEvent()

    data class Explosion(val pos: Vec2, val radius: Float) : EffectEvent()

    data class Impact(val pos: Vec2, val element: Element) : EffectEvent()

    data class EnemyDeath(val pos: Vec2, val type: EnemyType, val bounty: Int) : EffectEvent()

    data class EnemyLeaked(val pos: Vec2, val livesLost: Int) : EffectEvent()

    data class WaveCleared(val wave: Int, val bonus: Int) : EffectEvent()

    data class WaveStarted(val wave: Int) : EffectEvent()

    data class TowerFired(val towerId: Int, val targetPos: Vec2) : EffectEvent()

    /** Direct damage dealt to an enemy (armor-reduced). Burn ticks are not reported. */
    data class Damage(val enemyId: Int, val pos: Vec2, val amount: Int) : EffectEvent()

    data class HeroAttack(val pos: Vec2, val targetPos: Vec2) : EffectEvent()

    data class HeroAbilityUsed(val pos: Vec2, val radius: Float) : EffectEvent()
}
