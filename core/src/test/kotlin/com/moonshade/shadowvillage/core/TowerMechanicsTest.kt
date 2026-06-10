package com.moonshade.shadowvillage.core

import com.moonshade.shadowvillage.core.combat.AttackResolver
import com.moonshade.shadowvillage.core.data.Balance
import com.moonshade.shadowvillage.core.data.Element
import com.moonshade.shadowvillage.core.data.EnemyType
import com.moonshade.shadowvillage.core.data.WaveData
import com.moonshade.shadowvillage.core.game.GameSession
import com.moonshade.shadowvillage.core.game.PlayerCommand
import com.moonshade.shadowvillage.core.math.Vec2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TowerMechanicsTest {

    private fun session() = GameSession(testMap, WaveData.forMap(testMap.id))

    @Test
    fun `chain selects up to count within hop radius`() {
        val s = session()
        val a = s.addEnemy(EnemyType.MENDER, atDistance = 5f)
        val b = s.addEnemy(EnemyType.MENDER, atDistance = 5.5f)
        val c = s.addEnemy(EnemyType.MENDER, atDistance = 6f)
        val faraway = s.addEnemy(EnemyType.MENDER, atDistance = 10f)

        val chain = AttackResolver.chainTargets(a, s.enemies, count = 3, hopRadius = 2f)
        assertEquals(listOf(a, b, c), chain)
        assertTrue(faraway !in chain)
    }

    @Test
    fun `lightning tower chains with damage falloff`() {
        val s = session()
        assertTrue(s.execute(PlayerCommand.BuildTower(5, 0, Element.LIGHTNING)))
        // Default targeting FIRST picks the enemy furthest along the path,
        // so the leader takes the full hit and the trailer the falloff hop.
        val trailer = s.addEnemy(EnemyType.MENDER, atDistance = 5f)
        val leader = s.addEnemy(EnemyType.MENDER, atDistance = 5.6f)
        val leaderHp = leader.hp
        val trailerHp = trailer.hp

        s.tick() // lightning is instant on the first eligible tick

        // T1 lightning: 14 dmg, falloff 0.7 -> trailer takes ~9.8
        assertTrue(leaderHp - leader.hp >= 13f, "full chain hit missing: ${leaderHp - leader.hp}")
        assertTrue(trailerHp - trailer.hp in 8.5f..11f, "falloff hit wrong: ${trailerHp - trailer.hp}")
    }

    @Test
    fun `fire projectile applies burn on hit`() {
        val s = session()
        assertTrue(s.execute(PlayerCommand.BuildTower(5, 0, Element.FIRE)))
        val e = s.addEnemy(EnemyType.MENDER, atDistance = 5f)
        s.run(1.0f)
        assertTrue(e.burning, "fire hit should ignite the target")
    }

    @Test
    fun `water projectile slows on hit`() {
        val s = session()
        assertTrue(s.execute(PlayerCommand.BuildTower(5, 0, Element.WATER)))
        val e = s.addEnemy(EnemyType.MENDER, atDistance = 5f)
        s.run(1.0f)
        assertTrue(e.slowed, "water hit should slow the target")
        assertEquals(0.7f, e.speedMul, 1e-4f)
    }

    @Test
    fun `earth splash damages grouped ground enemies but not flyers`() {
        val s = session()
        assertTrue(s.execute(PlayerCommand.BuildTower(5, 0, Element.EARTH)))
        val ground1 = s.addEnemy(EnemyType.ONI_VANGUARD, atDistance = 5f) // slow mover stays grouped
        val ground2 = s.addEnemy(EnemyType.ONI_VANGUARD, atDistance = 5.4f)
        val flyer = s.addEnemy(EnemyType.RAIDER, atDistance = 5.2f)
        val flyerHp = flyer.hp

        s.run(1.5f) // fire + projectile flight

        assertTrue(ground1.hp < ground1.maxHp, "splash should hit first ground enemy")
        assertTrue(ground2.hp < ground2.maxHp, "splash should hit second ground enemy")
        assertEquals(flyerHp, flyer.hp, "splash must never hit flying enemies")
    }

    @Test
    fun `splash victims respects radius`() {
        val s = session()
        val near = s.addEnemy(EnemyType.NINJA, atDistance = 5f)
        val far = s.addEnemy(EnemyType.NINJA, atDistance = 8f)
        val victims = AttackResolver.splashVictims(Vec2(5.5f, 1.5f), s.enemies, radius = 1f)
        assertEquals(listOf(near), victims)
        assertTrue(far !in victims)
    }

    @Test
    fun `wind knocks back on every fourth shot`() {
        val s = session()
        assertTrue(s.execute(PlayerCommand.BuildTower(5, 0, Element.WIND)))
        // Knockback-eligible tank that won't die quickly and barely moves.
        val e = s.addEnemy(EnemyType.BRUTE, atDistance = 5f)

        var sawKnockback = false
        var last = e.pathDistance
        repeat(150) {
            s.tick()
            if (e.alive && e.pathDistance < last) sawKnockback = true
            last = e.pathDistance
        }
        assertTrue(sawKnockback, "wind tower should knock the brute backwards on 4th hits")
    }

    @Test
    fun `burn kills award bounty too`() {
        val s = session()
        val goldBefore = s.gold
        val e = s.addEnemy(EnemyType.BRUTE, atDistance = 5f) // bounty 18
        e.applyBurn(dps = 100_000f, duration = 1f)
        s.tick()
        assertTrue(s.enemies.isEmpty())
        assertEquals(goldBefore + 18, s.gold)
        assertEquals(Balance.STARTING_LIVES, s.lives, "burn death is a kill, not a leak")
    }

    @Test
    fun `tower kills award bounty`() {
        val s = session()
        assertTrue(s.execute(PlayerCommand.BuildTower(5, 0, Element.LIGHTNING)))
        val goldBefore = s.gold
        val e = s.addEnemy(EnemyType.BRUTE, atDistance = 5f) // slow enough to stay in range
        e.takeDamage(e.hp - 1f) // one zap from death
        s.run(2f)
        assertTrue(s.enemies.isEmpty())
        assertEquals(goldBefore + 18, s.gold)
    }
}
