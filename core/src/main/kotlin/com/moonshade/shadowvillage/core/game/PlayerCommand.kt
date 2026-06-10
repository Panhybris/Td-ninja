package com.moonshade.shadowvillage.core.game

import com.moonshade.shadowvillage.core.combat.Targeting
import com.moonshade.shadowvillage.core.data.Element
import com.moonshade.shadowvillage.core.data.SpecPath

/**
 * The only way the UI mutates the simulation. Commands are enqueued from
 * any thread and validated + executed at the start of a logic tick.
 */
sealed class PlayerCommand {
    data class BuildTower(val col: Int, val row: Int, val element: Element) : PlayerCommand()
    /** [spec] picks a tier-3 path; only honored on the T2 -> T3 upgrade. */
    data class UpgradeTower(val towerId: Int, val spec: SpecPath? = null) : PlayerCommand()
    data class SellTower(val towerId: Int) : PlayerCommand()
    data class SetTargeting(val towerId: Int, val mode: Targeting) : PlayerCommand()
    data object StartNextWave : PlayerCommand()

    /** Places the hero, or relocates them if already deployed. */
    data class PlaceHero(val col: Int, val row: Int) : PlayerCommand()

    data object HeroAbility : PlayerCommand()
}
