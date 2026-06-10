package com.moonshade.shadowvillage.core.entity

import com.moonshade.shadowvillage.core.combat.Targeting
import com.moonshade.shadowvillage.core.data.Element
import com.moonshade.shadowvillage.core.data.SpecPath
import com.moonshade.shadowvillage.core.data.TierStats
import com.moonshade.shadowvillage.core.data.TowerData
import com.moonshade.shadowvillage.core.math.Vec2

class Tower(
    val id: Int,
    val element: Element,
    val col: Int,
    val row: Int,
) {
    val pos = Vec2(col + 0.5f, row + 0.5f)
    var tier = 1
        private set
    /** Chosen tier-3 path; null below tier 3 or on the base T3 path. */
    var spec: SpecPath? = null
        private set
    var cooldown = 0f
    var targeting = Targeting.FIRST
    /** Total gold spent on this tower (build + upgrades), for sell refunds. */
    var invested = TowerData.buildCost(element)
        private set
    /** Shots fired, used for every-Nth-shot effects. */
    var shotCounter = 0

    val stats: TierStats
        get() = spec?.let { TowerData.spec(element, it).stats } ?: TowerData.tier(element, tier)

    val upgradeCost: Int? get() = TowerData.upgradeCost(element, tier)

    fun upgrade(specChoice: SpecPath? = null) {
        val cost = checkNotNull(upgradeCost) { "tower $id already at max tier" }
        tier++
        invested += cost
        if (tier == TowerData.MAX_TIER) spec = specChoice
    }
}
