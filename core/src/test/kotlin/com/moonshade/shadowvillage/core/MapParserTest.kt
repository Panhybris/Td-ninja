package com.moonshade.shadowvillage.core

import com.moonshade.shadowvillage.core.map.MapParser
import com.moonshade.shadowvillage.core.map.Maps
import com.moonshade.shadowvillage.core.map.TileType
import com.moonshade.shadowvillage.core.math.Vec2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MapParserTest {

    @Test
    fun `straight path has waypoints at cell centers`() {
        val map = MapParser.parse("t", "t", listOf("S#G"))
        assertEquals(3, map.waypoints.size)
        assertEquals(Vec2(0.5f, 0.5f), map.waypoints.first())
        assertEquals(Vec2(2.5f, 0.5f), map.waypoints.last())
        assertEquals(2f, map.totalPathLength)
    }

    @Test
    fun `positionAt interpolates and clamps`() {
        val map = MapParser.parse("t", "t", listOf("S#G"))
        assertEquals(Vec2(0.5f, 0.5f), map.positionAt(-1f))
        assertEquals(Vec2(1.5f, 0.5f), map.positionAt(1f))
        assertEquals(Vec2(1.0f, 0.5f), map.positionAt(0.5f))
        assertEquals(Vec2(2.5f, 0.5f), map.positionAt(99f))
    }

    @Test
    fun `branching path is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            MapParser.parse(
                "t", "t",
                listOf(
                    "S##",
                    ".##",
                    "..G",
                ),
            )
        }
    }

    @Test
    fun `both shipped maps parse with sensible paths`() {
        for (map in Maps.all) {
            assertTrue(map.waypoints.size > 20, "${map.id}: path too short")
            assertEquals(map.waypoints.size - 1f, map.totalPathLength, "${map.id}: non-unit segments")
            assertEquals(TileType.SPAWN, map.tileAt(map.waypoints.first().x.toInt(), map.waypoints.first().y.toInt()))
            assertEquals(TileType.GOAL, map.tileAt(map.waypoints.last().x.toInt(), map.waypoints.last().y.toInt()))
        }
    }

    @Test
    fun `shipped maps have enough buildable space`() {
        for (map in Maps.all) {
            var buildable = 0
            for (r in 0 until map.rows) for (c in 0 until map.cols) {
                if (map.isBuildable(c, r)) buildable++
            }
            assertTrue(buildable > 40, "${map.id}: only $buildable buildable cells")
        }
    }
}
