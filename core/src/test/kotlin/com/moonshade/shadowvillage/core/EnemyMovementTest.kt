package com.moonshade.shadowvillage.core

import com.moonshade.shadowvillage.core.data.Balance
import com.moonshade.shadowvillage.core.data.EnemyType
import com.moonshade.shadowvillage.core.entity.Enemy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnemyMovementTest {

    private fun enemy(type: EnemyType = EnemyType.SCOUT, hpScale: Float = 1f) =
        Enemy(1, type, hpScale, testMap)

    @Test
    fun `enemy advances along path at its speed`() {
        val e = enemy()
        e.update(Balance.TICK)
        assertEquals(e.stats.speed * Balance.TICK, e.pathDistance, 1e-4f)
        assertTrue(e.pos.x > 0.5f)
    }

    @Test
    fun `enemy reaching goal dies and flags reachedGoal`() {
        val e = enemy()
        e.teleportTo(testMap.totalPathLength - 0.01f)
        e.update(Balance.TICK)
        assertFalse(e.alive)
        assertTrue(e.reachedGoal)
    }

    @Test
    fun `slow reduces speed and expires`() {
        val e = enemy()
        e.applySlow(0.5f, duration = 0.5f)
        assertEquals(0.5f, e.speedMul)
        repeat(20) { e.update(Balance.TICK) } // 0.66s > duration
        assertEquals(1f, e.speedMul)
    }

    @Test
    fun `strongest slow wins and weaker is ignored`() {
        val e = enemy()
        e.applySlow(0.3f, 2f)
        e.applySlow(0.5f, 2f)
        assertEquals(0.5f, e.speedMul)
        e.applySlow(0.3f, 99f)
        assertEquals(0.5f, e.speedMul, "weaker slow must not replace stronger")
    }

    @Test
    fun `stun halts movement then releases`() {
        val e = enemy()
        e.teleportTo(2f)
        e.applyStun(0.5f)
        assertEquals(0f, e.speedMul)
        repeat(10) { e.update(Balance.TICK) } // 0.33s, still stunned
        assertEquals(2f, e.pathDistance)
        repeat(20) { e.update(Balance.TICK) } // past 0.5s total
        assertTrue(e.pathDistance > 2f, "movement should resume after stun")
    }

    @Test
    fun `stun durations max-merge and bosses are immune`() {
        val e = enemy()
        e.applyStun(1.0f)
        e.applyStun(0.2f) // shorter must not shorten the active stun
        repeat(15) { e.update(Balance.TICK) } // 0.5s
        assertTrue(e.stunned, "1s stun must survive a 0.2s re-application")

        val boss = enemy(EnemyType.ONI_WARLORD)
        boss.applyStun(5f)
        assertFalse(boss.stunned)
    }

    @Test
    fun `burn still ticks while stunned`() {
        val e = enemy(EnemyType.MENDER)
        e.applyStun(2f)
        e.applyBurn(dps = 30f, duration = 1f)
        repeat(30) { e.update(Balance.TICK) }
        assertTrue(e.hp < e.maxHp, "burn must damage a stunned enemy")
    }

    @Test
    fun `boss slow is capped`() {
        val e = enemy(EnemyType.ONI_WARLORD)
        e.applySlow(0.5f, 2f)
        assertEquals(1f - 0.25f, e.speedMul)
    }

    @Test
    fun `burn ticks damage over time and ignores armor`() {
        val e = enemy(EnemyType.BRUTE) // armor 4
        e.applyBurn(dps = 30f, duration = 1f)
        repeat(30) { e.update(Balance.TICK) } // 1 second
        // ~30 burn damage, unmitigated by armor; movement doesn't heal
        assertTrue(e.hp <= e.maxHp - 29f, "expected ~30 burn damage, hp=${e.hp}/${e.maxHp}")
    }

    @Test
    fun `mender regenerates up to max hp`() {
        val e = enemy(EnemyType.MENDER)
        e.takeDamage(50f)
        val damaged = e.hp
        repeat(30) { e.update(Balance.TICK) }
        assertTrue(e.hp > damaged, "mender should regen")
        repeat(600) { e.update(Balance.TICK) }
        assertTrue(e.hp <= e.maxHp)
    }

    @Test
    fun `armor reduces damage with a minimum of 1`() {
        val e = enemy(EnemyType.BRUTE) // armor 4
        assertEquals(6f, e.takeDamage(10f))
        assertEquals(1f, e.takeDamage(2f))
    }

    @Test
    fun `knockback pushes back but not before spawn, immune boss ignores`() {
        val e = enemy()
        e.teleportTo(2f)
        e.applyKnockback(0.5f)
        assertEquals(1.5f, e.pathDistance)
        e.applyKnockback(99f)
        assertEquals(0f, e.pathDistance)

        val boss = enemy(EnemyType.ONI_WARLORD)
        boss.teleportTo(2f)
        boss.applyKnockback(0.5f)
        assertEquals(2f, boss.pathDistance)
    }

    @Test
    fun `hp scaling rounds to whole points`() {
        val e = enemy(EnemyType.SCOUT, hpScale = 1.24f)
        assertEquals(37f, e.maxHp) // round(30 * 1.24)
    }
}
