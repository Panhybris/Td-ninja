package com.moonshade.shadowvillage.core.game

import com.moonshade.shadowvillage.core.combat.Targeting
import com.moonshade.shadowvillage.core.data.Element

/**
 * The only way the UI mutates the simulation. Commands are enqueued from
 * any thread and validated + executed at the start of a logic tick.
 */
sealed class PlayerCommand {
    data class BuildTower(val col: Int, val row: Int, val element: Element) : PlayerCommand()
    data class UpgradeTower(val towerId: Int) : PlayerCommand()
    data class SellTower(val towerId: Int) : PlayerCommand()
    data class SetTargeting(val towerId: Int, val mode: Targeting) : PlayerCommand()
    data object StartNextWave : PlayerCommand()
}
