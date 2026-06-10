package com.moonshade.shadowvillage.core.data

data class EnemyStats(
    val displayName: String,
    val hp: Int,
    /** Cells per second. */
    val speed: Float,
    val armor: Int,
    val bounty: Int,
    val lifeCost: Int = 1,
    val flying: Boolean = false,
    val regenPerSec: Float = 0f,
    /** Strongest slow factor that can apply (1 = no cap). */
    val slowCap: Float = 1f,
    val knockbackImmune: Boolean = false,
)

object EnemyData {

    private val stats: Map<EnemyType, EnemyStats> = mapOf(
        EnemyType.SCOUT to EnemyStats("Rogue Scout", hp = 30, speed = 1.9f, armor = 0, bounty = 5),
        EnemyType.NINJA to EnemyStats("Renegade Ninja", hp = 60, speed = 1.5f, armor = 0, bounty = 8),
        EnemyType.WOLF to EnemyStats("Shadow Wolf", hp = 80, speed = 1.9f, armor = 0, bounty = 11),
        EnemyType.BRUTE to EnemyStats("Iron Brute", hp = 220, speed = 0.8f, armor = 4, bounty = 18),
        EnemyType.RAIDER to EnemyStats("Sky Raider", hp = 70, speed = 1.7f, armor = 0, bounty = 12, flying = true),
        EnemyType.MENDER to EnemyStats("Mistmender", hp = 130, speed = 1.2f, armor = 0, bounty = 14, regenPerSec = 6f),
        EnemyType.ONI_VANGUARD to EnemyStats(
            "Oni Vanguard", hp = 800, speed = 0.65f, armor = 3, bounty = 60,
            lifeCost = 3, slowCap = 0.4f, knockbackImmune = true,
        ),
        EnemyType.ONI_WARLORD to EnemyStats(
            "Oni Warlord Gorath", hp = 2500, speed = 0.55f, armor = 6, bounty = 150,
            lifeCost = 5, slowCap = 0.25f, knockbackImmune = true,
        ),
    )

    fun of(type: EnemyType): EnemyStats = stats.getValue(type)
}
