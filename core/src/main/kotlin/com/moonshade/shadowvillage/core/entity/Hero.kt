package com.moonshade.shadowvillage.core.entity

import com.moonshade.shadowvillage.core.data.HeroData
import com.moonshade.shadowvillage.core.math.Vec2

/**
 * The player's hero. Has no HP (nothing in the simulation attacks
 * friendlies); relocation is an instant teleport that the renderer sells
 * as a smoke-dash.
 */
class Hero(col: Int, row: Int) {
    var col = col
        private set
    var row = row
        private set
    var pos = Vec2(col + 0.5f, row + 0.5f)
        private set

    var attackCooldown = 0f
    var abilityCooldown = 0f
    /** Starts hot so deployment can't chain into an instant teleport. */
    var relocateCooldown = HeroData.RELOCATE_COOLDOWN

    val canRelocate get() = relocateCooldown <= 0f
    val canUseAbility get() = abilityCooldown <= 0f

    fun moveTo(col: Int, row: Int) {
        this.col = col
        this.row = row
        pos = Vec2(col + 0.5f, row + 0.5f)
        relocateCooldown = HeroData.RELOCATE_COOLDOWN
    }

    fun tickCooldowns(dt: Float) {
        attackCooldown -= dt
        abilityCooldown -= dt
        relocateCooldown -= dt
    }
}
