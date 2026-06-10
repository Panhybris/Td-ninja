package com.moonshade.shadowvillage.core

import com.moonshade.shadowvillage.core.combat.Targeting
import com.moonshade.shadowvillage.core.data.Balance
import com.moonshade.shadowvillage.core.data.Element
import com.moonshade.shadowvillage.core.data.WaveData
import com.moonshade.shadowvillage.core.game.GameSession
import com.moonshade.shadowvillage.core.game.PlayerCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EconomyTest {

    private fun session() = GameSession(testMap, WaveData.forMap(testMap.id))

    @Test
    fun `building deducts gold and occupies the cell`() {
        val s = session()
        assertTrue(s.execute(PlayerCommand.BuildTower(3, 0, Element.WATER)))
        assertEquals(Balance.STARTING_GOLD - 60, s.gold)
        assertNotNull(s.towerAt(3, 0))
        assertFalse(s.canBuildAt(3, 0))
    }

    @Test
    fun `cannot build on path, blocked, occupied, or out of bounds`() {
        val s = session()
        assertFalse(s.execute(PlayerCommand.BuildTower(3, 1, Element.WATER)), "path cell")
        assertTrue(s.execute(PlayerCommand.BuildTower(3, 0, Element.WATER)))
        assertFalse(s.execute(PlayerCommand.BuildTower(3, 0, Element.FIRE)), "occupied cell")
        assertFalse(s.execute(PlayerCommand.BuildTower(-1, 0, Element.WATER)), "out of bounds")
    }

    @Test
    fun `cannot build without gold`() {
        val s = session() // 200 gold, water costs 60
        assertTrue(s.execute(PlayerCommand.BuildTower(1, 0, Element.WATER)))
        assertTrue(s.execute(PlayerCommand.BuildTower(2, 0, Element.WATER)))
        assertTrue(s.execute(PlayerCommand.BuildTower(3, 0, Element.WATER)))
        assertFalse(s.execute(PlayerCommand.BuildTower(4, 0, Element.WATER)), "broke")
        assertEquals(20, s.gold)
    }

    @Test
    fun `upgrade raises tier, costs gold, and stops at max`() {
        val s = session() // 200 gold
        assertTrue(s.execute(PlayerCommand.BuildTower(3, 0, Element.WATER))) // 60 -> 140 left
        val tower = s.towerAt(3, 0)!!
        assertTrue(s.execute(PlayerCommand.UpgradeTower(tower.id)))          // 50 -> 90 left
        assertEquals(2, tower.tier)
        assertEquals(90, s.gold)
        assertTrue(s.execute(PlayerCommand.UpgradeTower(tower.id)))          // 80 -> 10 left
        assertEquals(3, tower.tier)
        assertEquals(10, s.gold)
        assertNull(tower.upgradeCost)
        assertFalse(s.execute(PlayerCommand.UpgradeTower(tower.id)), "already max tier")
    }

    @Test
    fun `sell refunds 70 percent of invested and frees the cell`() {
        val s = session()
        assertTrue(s.execute(PlayerCommand.BuildTower(3, 0, Element.WATER))) // invested 60
        assertTrue(s.execute(PlayerCommand.UpgradeTower(s.towerAt(3, 0)!!.id))) // invested 110
        val tower = s.towerAt(3, 0)!!
        assertEquals(110, tower.invested)
        assertEquals(77, s.sellValue(tower))

        val goldBefore = s.gold
        assertTrue(s.execute(PlayerCommand.SellTower(tower.id)))
        assertEquals(goldBefore + 77, s.gold)
        assertTrue(s.canBuildAt(3, 0))
    }

    @Test
    fun `set targeting command applies`() {
        val s = session()
        assertTrue(s.execute(PlayerCommand.BuildTower(3, 0, Element.WATER)))
        val tower = s.towerAt(3, 0)!!
        assertEquals(Targeting.FIRST, tower.targeting)
        assertTrue(s.execute(PlayerCommand.SetTargeting(tower.id, Targeting.STRONG)))
        assertEquals(Targeting.STRONG, tower.targeting)
    }

    @Test
    fun `commands on unknown towers are rejected`() {
        val s = session()
        assertFalse(s.execute(PlayerCommand.UpgradeTower(424242)))
        assertFalse(s.execute(PlayerCommand.SellTower(424242)))
        assertFalse(s.execute(PlayerCommand.SetTargeting(424242, Targeting.LAST)))
        assertNull(s.towerById(424242))
    }

    @Test
    fun `enqueued commands run on the next tick`() {
        val s = session()
        s.enqueue(PlayerCommand.BuildTower(3, 0, Element.WATER))
        assertNull(s.towerAt(3, 0))
        s.tick()
        assertNotNull(s.towerAt(3, 0))
    }
}
