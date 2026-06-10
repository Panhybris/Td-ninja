package com.moonshade.shadowvillage.core

import com.moonshade.shadowvillage.core.combat.TargetSelector
import com.moonshade.shadowvillage.core.combat.Targeting
import com.moonshade.shadowvillage.core.data.Element
import com.moonshade.shadowvillage.core.data.EnemyType
import com.moonshade.shadowvillage.core.entity.Enemy
import com.moonshade.shadowvillage.core.entity.Tower
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TargetingTest {

    private fun enemyAt(distance: Float, type: EnemyType = EnemyType.SCOUT, id: Int = 1): Enemy =
        Enemy(id, type, 1f, testMap).also { it.teleportTo(distance) }

    // Tower at (5, 0): pos (5.5, 0.5); the path runs along y=1.5 so an enemy
    // at pathDistance d sits at (0.5+d, 1.5) - horizontal dist = |d - 5|.
    private fun tower(element: Element = Element.FIRE) = Tower(99, element, 5, 0)

    @Test
    fun `first picks furthest along path`() {
        val t = tower().also { it.targeting = Targeting.FIRST }
        val a = enemyAt(4f, id = 1)
        val b = enemyAt(6f, id = 2)
        assertEquals(b, TargetSelector.select(t, listOf(a, b)))
    }

    @Test
    fun `last picks least far along path`() {
        val t = tower().also { it.targeting = Targeting.LAST }
        val a = enemyAt(4f, id = 1)
        val b = enemyAt(6f, id = 2)
        assertEquals(a, TargetSelector.select(t, listOf(a, b)))
    }

    @Test
    fun `strong picks highest hp`() {
        val t = tower().also { it.targeting = Targeting.STRONG }
        val weak = enemyAt(4f, EnemyType.SCOUT, id = 1)
        val tough = enemyAt(6f, EnemyType.MENDER, id = 2)
        assertEquals(tough, TargetSelector.select(t, listOf(weak, tough)))
    }

    @Test
    fun `near picks closest to the tower`() {
        val t = tower().also { it.targeting = Targeting.NEAR }
        val close = enemyAt(5f, id = 1)
        val far = enemyAt(6.5f, id = 2)
        assertEquals(close, TargetSelector.select(t, listOf(far, close)))
    }

    @Test
    fun `enemies out of range are ignored`() {
        val t = tower() // fire range 2.5
        val far = enemyAt(10.5f)
        assertNull(TargetSelector.select(t, listOf(far)))
    }

    @Test
    fun `dead enemies are ignored`() {
        val t = tower()
        val e = enemyAt(5f)
        e.takeDamage(9999f)
        assertNull(TargetSelector.select(t, listOf(e)))
    }

    @Test
    fun `earth cannot target flying`() {
        val earth = tower(Element.EARTH)
        val flyer = enemyAt(5f, EnemyType.RAIDER)
        assertNull(TargetSelector.select(earth, listOf(flyer)))

        val fire = tower(Element.FIRE)
        assertEquals(flyer, TargetSelector.select(fire, listOf(flyer)))
    }

    @Test
    fun `targeting mode cycles through all modes`() {
        var mode = Targeting.FIRST
        val seen = mutableSetOf(mode)
        repeat(Targeting.entries.size - 1) {
            mode = mode.next()
            seen += mode
        }
        assertEquals(Targeting.entries.toSet(), seen)
        assertEquals(Targeting.FIRST, mode.next())
    }
}
