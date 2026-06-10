package com.moonshade.shadowvillage.core

import com.moonshade.shadowvillage.core.data.Element
import com.moonshade.shadowvillage.core.data.WaveData
import com.moonshade.shadowvillage.core.game.GameSession
import com.moonshade.shadowvillage.core.game.GameStatus
import com.moonshade.shadowvillage.core.map.Maps
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Locks the difficulty curve: a sensible all-element build must beat all 20
 * waves, while no defense at all must collapse early. If a balance tweak
 * breaks either side, this test catches it.
 */
class AutoplayBalanceTest {

    /** A reasonable but not optimized build around River Crossing's corners. */
    private val decentBuild = listOf(
        BuildStep(4, 2, Element.WATER),      // first corner
        BuildStep(6, 4, Element.FIRE),       // middle S-bend
        BuildStep(13, 4, Element.LIGHTNING), // late double coverage, built early
        BuildStep(8, 4, Element.EARTH),      // corner cluster
        BuildStep(10, 4, Element.WIND),      // anti-air + chip
        BuildStep(10, 2, Element.LIGHTNING), // second-half reinforcement
        BuildStep(13, 6, Element.FIRE),      // last stretch
        BuildStep(6, 2, Element.WATER),      // extra slow on the first half
        BuildStep(13, 2, Element.EARTH),     // extra splash at the late corner
    )

    @Test
    fun `decent build wins river crossing`() {
        val s = GameSession(Maps.riverCrossing, WaveData.forMap(Maps.riverCrossing.id))
        val status = autoplay(s, decentBuild)
        assertEquals(GameStatus.VICTORY, status, "lost on wave ${s.waveNumber} with ${s.lives} lives")
        assertTrue(s.lives > 0)
    }

    @Test
    fun `no defense loses quickly`() {
        val s = GameSession(Maps.riverCrossing, WaveData.forMap(Maps.riverCrossing.id))
        assertEquals(GameStatus.DEFEAT, autoplay(s, emptyList()))
    }

    @Test
    fun `decent build also wins twin gates`() {
        // Twin Gates: center corridor cells cover both switchback lanes.
        val build = listOf(
            BuildStep(5, 4, Element.WATER),
            BuildStep(6, 4, Element.FIRE),
            BuildStep(7, 4, Element.EARTH),
            BuildStep(11, 4, Element.LIGHTNING),
            BuildStep(10, 4, Element.WIND),
            BuildStep(13, 2, Element.LIGHTNING),
            BuildStep(13, 6, Element.FIRE),
        )
        val s = GameSession(Maps.twinGates, WaveData.forMap(Maps.twinGates.id))
        val status = autoplay(s, build)
        assertEquals(GameStatus.VICTORY, status, "lost on wave ${s.waveNumber} with ${s.lives} lives")
    }
}
