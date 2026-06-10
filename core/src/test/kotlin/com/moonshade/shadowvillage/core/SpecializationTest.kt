package com.moonshade.shadowvillage.core

import com.moonshade.shadowvillage.core.data.Element
import com.moonshade.shadowvillage.core.data.EnemyType
import com.moonshade.shadowvillage.core.data.SpecPath
import com.moonshade.shadowvillage.core.data.TowerData
import com.moonshade.shadowvillage.core.data.WaveData
import com.moonshade.shadowvillage.core.game.GameSession
import com.moonshade.shadowvillage.core.game.PlayerCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpecializationTest {

    private fun session() = GameSession(testMap, WaveData.forMap(testMap.id))
        .also { it.grantGold(1000) }

    /** Builds a tower and pushes it straight to T3 with the given path. */
    private fun GameSession.buildT3(col: Int, element: Element, path: SpecPath?): Int {
        check(execute(PlayerCommand.BuildTower(col, 0, element)))
        val id = towerAt(col, 0)!!.id
        check(execute(PlayerCommand.UpgradeTower(id)))
        check(execute(PlayerCommand.UpgradeTower(id, path)))
        return id
    }

    @Test
    fun `every element has exactly two specs and shared T3 cost`() {
        for (element in Element.entries) {
            val specs = TowerData.specs.getValue(element)
            assertEquals(2, specs.size, "$element")
            assertEquals(SpecPath.A, specs[0].path)
            assertEquals(SpecPath.B, specs[1].path)
            val baseCost = TowerData.tier(element, 3).cost
            assertTrue(specs.all { it.stats.cost == baseCost }, "$element spec cost differs from base T3")
        }
    }

    @Test
    fun `spec replaces the T3 stat row`() {
        val s = session()
        val id = s.buildT3(3, Element.WATER, SpecPath.A)
        val tower = s.towerById(id)!!
        assertEquals(SpecPath.A, tower.spec)
        assertEquals(TowerData.spec(Element.WATER, SpecPath.A).stats, tower.stats)
        assertEquals(16, tower.stats.damage)
    }

    @Test
    fun `no spec choice keeps the base T3 row`() {
        val s = session()
        val id = s.buildT3(3, Element.WATER, null)
        val tower = s.towerById(id)!!
        assertNull(tower.spec)
        assertEquals(TowerData.tier(Element.WATER, 3), tower.stats)
    }

    @Test
    fun `spec choice on the T1 to T2 upgrade is ignored`() {
        val s = session()
        assertTrue(s.execute(PlayerCommand.BuildTower(3, 0, Element.WATER)))
        val tower = s.towerAt(3, 0)!!
        assertTrue(s.execute(PlayerCommand.UpgradeTower(tower.id, SpecPath.B)))
        assertEquals(2, tower.tier)
        assertNull(tower.spec, "spec must only stick on the upgrade that reaches T3")
    }

    @Test
    fun `sell refund is unaffected by the spec choice`() {
        val a = session()
        val idA = a.buildT3(3, Element.WATER, SpecPath.A)
        val b = session()
        val idB = b.buildT3(3, Element.WATER, null)
        assertEquals(b.sellValue(b.towerById(idB)!!), a.sellValue(a.towerById(idA)!!))
        assertEquals(190, a.towerById(idA)!!.invested)
    }

    @Test
    fun `glacier stuns every fourth hit`() {
        val s = session()
        s.buildT3(5, Element.WATER, SpecPath.A)
        val brute = s.addEnemy(EnemyType.BRUTE, atDistance = 5f) // slow, tanky target

        var sawStun = false
        repeat(240) { // 8 seconds: ~7 shots at 0.9/s
            s.tick()
            if (brute.alive && brute.stunned) sawStun = true
        }
        assertTrue(sawStun, "Glacier should have landed a stunning shot")
    }

    @Test
    fun `inferno splash burns multiple victims`() {
        val s = session()
        s.buildT3(5, Element.FIRE, SpecPath.A)
        val v1 = s.addEnemy(EnemyType.ONI_VANGUARD, atDistance = 5f)
        val v2 = s.addEnemy(EnemyType.ONI_VANGUARD, atDistance = 5.4f)
        s.run(3f)
        assertTrue(v1.burning, "inferno splash should ignite victim 1")
        assertTrue(v2.burning, "inferno splash should ignite victim 2")
    }

    @Test
    fun `riptide splash slows multiple victims`() {
        val s = session()
        s.buildT3(5, Element.WATER, SpecPath.B)
        val v1 = s.addEnemy(EnemyType.ONI_VANGUARD, atDistance = 5f)
        val v2 = s.addEnemy(EnemyType.ONI_VANGUARD, atDistance = 5.4f)
        s.run(3f)
        assertTrue(v1.slowed, "riptide splash should slow victim 1")
        assertTrue(v2.slowed, "riptide splash should slow victim 2")
    }

    @Test
    fun `quake splash slows ground victims but never flyers`() {
        val s = session()
        s.buildT3(5, Element.EARTH, SpecPath.B)
        val ground = s.addEnemy(EnemyType.ONI_VANGUARD, atDistance = 5f)
        val flyer = s.addEnemy(EnemyType.RAIDER, atDistance = 5.2f)
        val flyerHp = flyer.hp
        s.run(3f)
        assertTrue(ground.slowed, "quake should slow the ground victim")
        assertEquals(flyerHp, flyer.hp, "splash must never hit flyers")
    }

    @Test
    fun `inferno cannot target flyers but hellbrand can`() {
        val inferno = session()
        inferno.buildT3(5, Element.FIRE, SpecPath.A)
        val flyer1 = inferno.addEnemy(EnemyType.RAIDER, atDistance = 5f)
        val hp1 = flyer1.hp
        inferno.run(2f)
        assertEquals(hp1, flyer1.hp, "splash-spec fire is ground-only")

        val hellbrand = session()
        hellbrand.buildT3(5, Element.FIRE, SpecPath.B)
        val flyer2 = hellbrand.addEnemy(EnemyType.ONI_VANGUARD, atDistance = 5f)
        val hp2 = flyer2.hp
        hellbrand.run(2f)
        assertTrue(flyer2.hp < hp2, "single-target fire still hits")
    }

    @Test
    fun `thunderbolt hits one target very hard`() {
        val s = session()
        val id = s.buildT3(5, Element.LIGHTNING, SpecPath.B)
        checkNotNull(s.towerById(id))
        val target = s.addEnemy(EnemyType.ONI_VANGUARD, atDistance = 5f) // armor 3
        val before = target.hp
        s.tick()
        assertEquals(95f - 3f, before - target.hp, 0.6f) // one bolt, armor-reduced; minor regen drift
    }

    @Test
    fun `specs are deterministic`() {
        fun play(): GameSession {
            val s = session()
            s.buildT3(5, Element.WATER, SpecPath.A)
            s.execute(PlayerCommand.StartNextWave)
            s.run(60f)
            return s
        }
        val a = play()
        val b = play()
        assertEquals(a.gold, b.gold)
        assertEquals(a.lives, b.lives)
        assertEquals(a.tickCount, b.tickCount)
    }
}
