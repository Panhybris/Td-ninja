package com.moonshade.shadowvillage.core

import com.moonshade.shadowvillage.core.data.Element
import com.moonshade.shadowvillage.core.data.WaveData
import com.moonshade.shadowvillage.core.game.GameSession
import com.moonshade.shadowvillage.core.map.Maps
import kotlin.test.Test
import kotlin.test.assertEquals

class DeterminismTest {

    @Test
    fun `same script produces an identical game`() {
        val builds = listOf(
            BuildStep(4, 2, Element.WATER),
            BuildStep(6, 4, Element.FIRE),
            BuildStep(13, 4, Element.LIGHTNING),
        )

        fun play(): GameSession {
            val s = GameSession(Maps.riverCrossing, WaveData.forMap(Maps.riverCrossing.id), seed = 7L)
            autoplay(s, builds)
            return s
        }

        val a = play()
        val b = play()

        assertEquals(a.status, b.status)
        assertEquals(a.tickCount, b.tickCount)
        assertEquals(a.gold, b.gold)
        assertEquals(a.lives, b.lives)
        assertEquals(a.waveNumber, b.waveNumber)
    }
}
