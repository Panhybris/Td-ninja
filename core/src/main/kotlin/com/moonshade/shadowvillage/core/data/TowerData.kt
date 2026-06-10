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
    /** Every Nth shot stuns; 0 = never. */
    val stunEvery: Int = 0,
    val stunDuration: Float = 0f,
    /** Cells per second. */
    val projectileSpeed: Float = 8f,
)

/** The two tier-3 specialization paths every element offers. */
enum class SpecPath { A, B }

data class SpecDef(
    val path: SpecPath,
    val name: String,
    /** One-liner shown in the chooser UI. */
    val blurb: String,
    val stats: TierStats,
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

    /**
     * Tier-3 specializations. Choosing one replaces the base T3 stat row;
     * the upgrade cost is the same either way. Splash (`splashRadius > 0`)
     * always means ground-only, like Stonefist.
     */
    val specs: Map<Element, List<SpecDef>> = mapOf(
        Element.FIRE to listOf(
            SpecDef(
                SpecPath.A, "Inferno", "Splash that sets packs ablaze (ground only)",
                TierStats(cost = 90, damage = 24, range = 3.0f, fireRate = 1.0f, burnDps = 12f, burnDuration = 3f, splashRadius = 0.9f),
            ),
            SpecDef(
                SpecPath.B, "Hellbrand", "Single-target mega-burn, melts bosses",
                TierStats(cost = 90, damage = 42, range = 3.0f, fireRate = 1.0f, burnDps = 22f, burnDuration = 4f),
            ),
        ),
        Element.WATER to listOf(
            SpecDef(
                SpecPath.A, "Glacier", "Every 4th hit freezes the target solid",
                TierStats(cost = 80, damage = 16, range = 2.5f, fireRate = 0.9f, slowFactor = 0.50f, slowDuration = 2f, stunEvery = 4, stunDuration = 0.8f),
            ),
            SpecDef(
                SpecPath.B, "Riptide", "Slowing splash drenches whole packs (ground only)",
                TierStats(cost = 80, damage = 14, range = 2.5f, fireRate = 0.9f, slowFactor = 0.45f, slowDuration = 2f, splashRadius = 1.0f),
            ),
        ),
        Element.LIGHTNING to listOf(
            SpecDef(
                SpecPath.A, "Stormlord", "Chains through up to 8 enemies",
                TierStats(cost = 130, damage = 30, range = 3.0f, fireRate = 0.7f, chainCount = 8, chainFalloff = 0.85f, chainRadius = 2.0f),
            ),
            SpecDef(
                SpecPath.B, "Thunderbolt", "One devastating bolt, long range",
                TierStats(cost = 130, damage = 95, range = 3.5f, fireRate = 0.45f, chainCount = 1, chainFalloff = 1f, chainRadius = 0f),
            ),
        ),
        Element.WIND to listOf(
            SpecDef(
                SpecPath.A, "Tempest", "A relentless hail of shuriken",
                TierStats(cost = 100, damage = 9, range = 3.0f, fireRate = 6.0f, projectileSpeed = 14f),
            ),
            SpecDef(
                SpecPath.B, "Cyclone", "Every 2nd hit hurls enemies backwards",
                TierStats(cost = 100, damage = 16, range = 3.0f, fireRate = 3.0f, knockback = 0.9f, knockbackEvery = 2, projectileSpeed = 12f),
            ),
        ),
        Element.EARTH to listOf(
            SpecDef(
                SpecPath.A, "Mountain", "Colossal boulders, huge splash",
                TierStats(cost = 120, damage = 85, range = 2.4f, fireRate = 0.4f, splashRadius = 1.7f, projectileSpeed = 6f),
            ),
            SpecDef(
                SpecPath.B, "Quake", "Tremor splash that staggers and slows",
                TierStats(cost = 120, damage = 45, range = 2.4f, fireRate = 0.5f, splashRadius = 1.5f, slowFactor = 0.30f, slowDuration = 1.2f, projectileSpeed = 6f),
            ),
        ),
    )

    fun spec(element: Element, path: SpecPath): SpecDef =
        specs.getValue(element).first { it.path == path }

    fun tier(element: Element, tier: Int): TierStats = stats.getValue(element)[tier - 1]

    fun buildCost(element: Element): Int = stats.getValue(element)[0].cost

    /** Cost to upgrade from [currentTier], or null if already at max. */
    fun upgradeCost(element: Element, currentTier: Int): Int? =
        if (currentTier >= MAX_TIER) null else stats.getValue(element)[currentTier].cost
}
