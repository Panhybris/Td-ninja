package com.moonshade.shadowvillage.core.data

/**
 * Stats for one tower tier. For tier 1 [cost] is the build cost; for tiers
 * 2 and 3 it is the incremental upgrade cost from the previous tier.
 */
data class TierStats(
    val cost: Int,
    val damage: Int,
    val range: Float,
    /** Shots per second. */
    val fireRate: Float,
    val burnDps: Float = 0f,
    val burnDuration: Float = 0f,
    /** 0.3 = enemies move 30% slower. */
    val slowFactor: Float = 0f,
    val slowDuration: Float = 0f,
    /** Total enemies hit by a chain, including the first. */
    val chainCount: Int = 0,
    /** Damage multiplier per chain hop. */
    val chainFalloff: Float = 0f,
    /** Max hop distance between chained enemies, in cells. */
    val chainRadius: Float = 0f,
    /** Cells pushed back along the path. */
    val knockback: Float = 0f,
    /** Every Nth shot knocks back; 0 = never. */
    val knockbackEvery: Int = 0,
    val splashRadius: Float = 0f,
    /** Cells per second. */
    val projectileSpeed: Float = 8f,
)

object TowerData {

    val stats: Map<Element, List<TierStats>> = mapOf(
        Element.FIRE to listOf(
            TierStats(cost = 70, damage = 10, range = 2.5f, fireRate = 1.0f, burnDps = 5f, burnDuration = 3f),
            TierStats(cost = 55, damage = 18, range = 2.5f, fireRate = 1.0f, burnDps = 8f, burnDuration = 3f),
            TierStats(cost = 90, damage = 28, range = 3.0f, fireRate = 1.0f, burnDps = 12f, burnDuration = 3f),
        ),
        Element.WATER to listOf(
            TierStats(cost = 60, damage = 6, range = 2.5f, fireRate = 0.9f, slowFactor = 0.30f, slowDuration = 2f),
            TierStats(cost = 50, damage = 11, range = 2.5f, fireRate = 0.9f, slowFactor = 0.40f, slowDuration = 2f),
            TierStats(cost = 80, damage = 16, range = 2.5f, fireRate = 0.9f, slowFactor = 0.50f, slowDuration = 2f),
        ),
        Element.LIGHTNING to listOf(
            TierStats(cost = 100, damage = 14, range = 3.0f, fireRate = 0.7f, chainCount = 3, chainFalloff = 0.7f, chainRadius = 2.0f),
            TierStats(cost = 80, damage = 24, range = 3.0f, fireRate = 0.7f, chainCount = 4, chainFalloff = 0.7f, chainRadius = 2.0f),
            TierStats(cost = 130, damage = 36, range = 3.0f, fireRate = 0.7f, chainCount = 6, chainFalloff = 0.75f, chainRadius = 2.0f),
        ),
        Element.WIND to listOf(
            TierStats(cost = 80, damage = 5, range = 3.0f, fireRate = 3.0f, knockback = 0.3f, knockbackEvery = 4, projectileSpeed = 12f),
            TierStats(cost = 65, damage = 8, range = 3.0f, fireRate = 3.0f, knockback = 0.4f, knockbackEvery = 4, projectileSpeed = 12f),
            TierStats(cost = 100, damage = 12, range = 3.0f, fireRate = 4.0f, knockback = 0.6f, knockbackEvery = 4, projectileSpeed = 12f),
        ),
        Element.EARTH to listOf(
            TierStats(cost = 90, damage = 18, range = 2.2f, fireRate = 0.5f, splashRadius = 1.0f, projectileSpeed = 6f),
            TierStats(cost = 75, damage = 30, range = 2.2f, fireRate = 0.5f, splashRadius = 1.2f, projectileSpeed = 6f),
            TierStats(cost = 120, damage = 50, range = 2.4f, fireRate = 0.5f, splashRadius = 1.5f, projectileSpeed = 6f),
        ),
    )

    const val MAX_TIER = 3

    fun tier(element: Element, tier: Int): TierStats = stats.getValue(element)[tier - 1]

    fun buildCost(element: Element): Int = stats.getValue(element)[0].cost

    /** Cost to upgrade from [currentTier], or null if already at max. */
    fun upgradeCost(element: Element, currentTier: Int): Int? =
        if (currentTier >= MAX_TIER) null else stats.getValue(element)[currentTier].cost
}
