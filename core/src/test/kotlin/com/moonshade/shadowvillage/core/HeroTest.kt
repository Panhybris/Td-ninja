package com.moonshade.shadowvillage.core

import com.moonshade.shadowvillage.core.data.Balance
import com.moonshade.shadowvillage.core.data.Element
import com.moonshade.shadowvillage.core.data.EnemyType
import com.moonshade.shadowvillage.core.data.HeroData
import com.moonshade.shadowvillage.core.data.WaveData
import com.moonshade.shadowvillage.core.game.GameSession
import com.moonshade.shadowvillage.core.game.PlayerCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HeroTest {

    private fun session() = GameSession(testMap, WaveData.forMap(testMap.id))

    @Test
    fun `hero places on grass and on the path but not on blockers`() {
        val s = session()
        assertTrue(s.execute(PlayerCommand.PlaceHero(4, 0)), "grass")
        assertNotNull(s.hero)

        val s2 = session()
        assertTrue(s2.execute(PlayerCommand.PlaceHero(4, 1)), "hero may stand in the road")

        val s3 = session()
        assertTrue(s3.execute(PlayerCommand.BuildTower(4, 0, Element.WATER)))
        assertFalse(s3.execute(PlayerCommand.PlaceHero(4, 0)), "tower cell occupied")
        assertFalse(s3.execute(PlayerCommand.PlaceHero(-1, 0)), "out of bounds")
        assertNull(s3.hero)
    }

    @Test
    fun `relocation is gated by its cooldown`() {
        val s = session()
        assertTrue(s.execute(PlayerCommand.PlaceHero(4, 0)))
        assertFalse(s.execute(PlayerCommand.PlaceHero(5, 0)), "relocate during cooldown")
        s.run(HeroData.RELOCATE_COOLDOWN + 0.5f)
        assertTrue(s.execute(PlayerCommand.PlaceHero(5, 0)), "relocate after cooldown")
        assertEquals(5, s.hero!!.col)
    }

    @Test
    fun `melee kills nearby ground enemies and pays bounty`() {
        val s = session()
        assertTrue(s.execute(PlayerCommand.PlaceHero(5, 0)))
        val goldBefore = s.gold
        s.addEnemy(EnemyType.SCOUT, atDistance = 5f) // 30 hp, one swing
        s.run(2f)
        assertTrue(s.enemies.isEmpty(), "scout should die to the hero")
        assertEquals(goldBefore + 5, s.gold)
    }

    @Test
    fun `melee cleaves at most two targets and ignores flyers`() {
        val s = session()
        assertTrue(s.execute(PlayerCommand.PlaceHero(5, 0)))
        val a = s.addEnemy(EnemyType.BRUTE, atDistance = 5f)
        val b = s.addEnemy(EnemyType.BRUTE, atDistance = 5.2f)
        val c = s.addEnemy(EnemyType.BRUTE, atDistance = 5.4f)
        val flyer = s.addEnemy(EnemyType.RAIDER, atDistance = 5.1f)
        val flyerHp = flyer.hp
        s.tick() // one swing
        val hit = listOf(a, b, c).count { it.hp < it.maxHp }
        assertEquals(2, hit, "cleave must hit exactly 2 of the 3 brutes")
        assertEquals(flyerHp, flyer.hp, "hero melee never hits flyers")
    }

    @Test
    fun `hero idles when nothing is in range`() {
        val s = session()
        assertTrue(s.execute(PlayerCommand.PlaceHero(1, 0)))
        val far = s.addEnemy(EnemyType.BRUTE, atDistance = 9f)
        s.run(1f)
        assertEquals(far.maxHp, far.hp)
    }

    @Test
    fun `ability damages and stuns everything in radius including flyers`() {
        val s = session()
        assertTrue(s.execute(PlayerCommand.PlaceHero(5, 0)))
        val ground = s.addEnemy(EnemyType.BRUTE, atDistance = 5f)
        val flyer = s.addEnemy(EnemyType.RAIDER, atDistance = 5.3f)
        val out = s.addEnemy(EnemyType.BRUTE, atDistance = 10f)

        assertTrue(s.execute(PlayerCommand.HeroAbility))
        assertTrue(ground.hp < ground.maxHp)
        assertTrue(ground.stunned)
        assertTrue(flyer.hp < flyer.maxHp, "ability hits flyers")
        assertEquals(out.maxHp, out.hp, "out of radius untouched")
    }

    @Test
    fun `ability respects cooldown and boss immunities`() {
        val s = session()
        assertTrue(s.execute(PlayerCommand.PlaceHero(5, 0)))
        val boss = s.addEnemy(EnemyType.ONI_WARLORD, atDistance = 5f)
        boss.teleportTo(5f)
        val before = boss.hp
        val distBefore = boss.pathDistance

        assertTrue(s.execute(PlayerCommand.HeroAbility))
        assertTrue(boss.hp < before, "boss takes ability damage")
        assertFalse(boss.stunned, "boss cannot be stunned")
        assertEquals(distBefore, boss.pathDistance, "boss cannot be knocked back")

        assertFalse(s.execute(PlayerCommand.HeroAbility), "cooldown gate")
        s.run(HeroData.ABILITY_COOLDOWN + 0.5f)
        assertTrue(s.execute(PlayerCommand.HeroAbility), "ready again after cooldown")
    }

    @Test
    fun `ability without a deployed hero is rejected`() {
        val s = session()
        assertFalse(s.execute(PlayerCommand.HeroAbility))
    }

    @Test
    fun `hero sessions are deterministic`() {
        fun play(): GameSession {
            val s = session()
            s.execute(PlayerCommand.PlaceHero(4, 0))
            s.execute(PlayerCommand.StartNextWave)
            repeat((30f / Balance.TICK).toInt()) {
                s.update(Balance.TICK)
                if (s.tickCount == 90L) s.execute(PlayerCommand.HeroAbility)
                if (s.tickCount == 300L) s.execute(PlayerCommand.PlaceHero(6, 0))
            }
            return s
        }
        val a = play()
        val b = play()
        assertEquals(a.gold, b.gold)
        assertEquals(a.lives, b.lives)
        assertEquals(a.enemies.size, b.enemies.size)
    }
}
