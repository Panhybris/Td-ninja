package com.moonshade.shadowvillage.core

import com.moonshade.shadowvillage.core.data.Balance
import com.moonshade.shadowvillage.core.data.Element
import com.moonshade.shadowvillage.core.data.EnemyType
import com.moonshade.shadowvillage.core.data.TowerData
import com.moonshade.shadowvillage.core.entity.Enemy
import com.moonshade.shadowvillage.core.game.GameSession
import com.moonshade.shadowvillage.core.game.GameStatus
import com.moonshade.shadowvillage.core.game.PlayerCommand
import com.moonshade.shadowvillage.core.map.GameMap
import com.moonshade.shadowvillage.core.map.MapParser

/** A 12-cell straight path with buildable rows above and below. */
val testMap: GameMap = MapParser.parse(
    id = "test_straight",
    name = "Test Straight",
    ascii = listOf(
        "............",
        "S##########G",
        "............",
    ),
)

fun GameSession.tick(times: Int = 1) {
    repeat(times) { update(Balance.TICK) }
}

/** Runs roughly [seconds] of simulation. */
fun GameSession.run(seconds: Float) {
    tick((seconds / Balance.TICK).toInt())
}

fun GameSession.addEnemy(type: EnemyType, atDistance: Float = 0f, hpScale: Float = 1f): Enemy {
    val enemy = Enemy(id = 100_000 + enemies.size, type = type, hpScale = hpScale, map = map)
    enemy.teleportTo(atDistance)
    enemies += enemy
    return enemy
}

data class BuildStep(val col: Int, val row: Int, val element: Element)

/**
 * Plays a full game like a sensible player: builds the first few towers,
 * then pushes the existing core to tier 2 before expanding, then maxes
 * everything out. Sends the next wave whenever the field is clear.
 */
fun autoplay(session: GameSession, builds: List<BuildStep>, maxTicks: Int = 400_000): GameStatus {
    var buildIdx = 0
    var ticks = 0

    fun tryBuildNext(): Boolean {
        if (buildIdx >= builds.size) return false
        val b = builds[buildIdx]
        if (session.gold < TowerData.buildCost(b.element)) return false
        check(session.execute(PlayerCommand.BuildTower(b.col, b.row, b.element))) {
            "autoplay build failed at $b - bad cell in script?"
        }
        buildIdx++
        return true
    }

    fun tryUpgradeBelow(tier: Int): Boolean {
        val up = session.towers
            .filter { it.tier < tier && it.upgradeCost != null }
            .minByOrNull { it.upgradeCost!! }
            ?: return false
        if (session.gold < up.upgradeCost!!) return false
        return session.execute(PlayerCommand.UpgradeTower(up.id))
    }

    while (session.status == GameStatus.RUNNING && ticks < maxTicks) {
        when {
            buildIdx < 4 -> tryBuildNext()
            tryUpgradeBelow(2) -> {}
            tryBuildNext() -> {}
            else -> tryUpgradeBelow(TowerData.MAX_TIER)
        }
        if (session.canStartNextWave && !session.waveInProgress) {
            session.execute(PlayerCommand.StartNextWave)
        }
        session.update(Balance.TICK)
        ticks++
    }
    return session.status
}
