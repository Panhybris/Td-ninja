package com.moonshade.shadowvillage.core

import com.moonshade.shadowvillage.core.data.Balance
import com.moonshade.shadowvillage.core.data.Element
import com.moonshade.shadowvillage.core.data.EnemyType
import com.moonshade.shadowvillage.core.data.SpawnEntry
import com.moonshade.shadowvillage.core.data.WaveData
import com.moonshade.shadowvillage.core.data.WaveDef
import com.moonshade.shadowvillage.core.entity.EffectEvent
import com.moonshade.shadowvillage.core.game.GameSession
import com.moonshade.shadowvillage.core.game.GameStatus
import com.moonshade.shadowvillage.core.game.PlayerCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameFlowTest {

    @Test
    fun `leaked enemies cost lives`() {
        val s = GameSession(testMap, WaveData.forMap(testMap.id))
        s.execute(PlayerCommand.StartNextWave) // 8 scouts, no defense
        s.run(20f)
        assertEquals(Balance.STARTING_LIVES - 8, s.lives)
        assertEquals(GameStatus.RUNNING, s.status)
    }

    @Test
    fun `defeat when lives reach zero`() {
        val s = GameSession(testMap, WaveData.forMap(testMap.id))
        val status = autoplay(s, builds = emptyList())
        assertEquals(GameStatus.DEFEAT, status)
        assertEquals(0, s.lives)
        assertTrue(s.waveNumber <= 5, "defenseless game should end early, ended on wave ${s.waveNumber}")
    }

    @Test
    fun `victory after killing the final wave`() {
        val tinyCampaign = listOf(
            WaveDef(listOf(SpawnEntry(EnemyType.SCOUT, count = 2, interval = 0.5f))),
        )
        val s = GameSession(testMap, tinyCampaign)
        // Water slow + fire burn reliably kill scouts crossing the corridor.
        assertTrue(s.execute(PlayerCommand.BuildTower(4, 0, Element.WATER)))
        assertTrue(s.execute(PlayerCommand.BuildTower(6, 0, Element.FIRE)))
        assertTrue(s.execute(PlayerCommand.StartNextWave))
        s.run(30f)
        assertEquals(GameStatus.VICTORY, s.status)
        assertEquals(Balance.STARTING_LIVES, s.lives, "scouts should die, not leak")
    }

    @Test
    fun `wave clear bonus is granted once per wave`() {
        val tinyCampaign = listOf(
            WaveDef(listOf(SpawnEntry(EnemyType.SCOUT, count = 1, interval = 0.5f))),
            WaveDef(listOf(SpawnEntry(EnemyType.SCOUT, count = 1, interval = 0.5f))),
        )
        val s = GameSession(testMap, tinyCampaign)
        val baseline = s.gold
        // No towers: the scout leaks, which still ends the wave and pays the bonus.
        assertTrue(s.execute(PlayerCommand.StartNextWave))
        s.run(15f)
        assertEquals(baseline + Balance.waveClearBonus(1), s.gold)

        assertTrue(s.execute(PlayerCommand.StartNextWave))
        s.run(15f)
        assertEquals(baseline + Balance.waveClearBonus(1) + Balance.waveClearBonus(2), s.gold)
    }

    @Test
    fun `starting a wave emits WaveStarted`() {
        val s = GameSession(testMap, WaveData.forMap(testMap.id))
        s.enqueue(PlayerCommand.StartNextWave)
        s.tick()
        assertTrue(s.effectEvents.any { it == EffectEvent.WaveStarted(1) })
    }

    @Test
    fun `direct damage emits a Damage event with the armor-reduced amount`() {
        val s = GameSession(testMap, WaveData.forMap(testMap.id))
        assertTrue(s.execute(PlayerCommand.BuildTower(5, 0, Element.LIGHTNING)))
        val brute = s.addEnemy(EnemyType.BRUTE, atDistance = 5f) // armor 4
        s.tick()
        val damage = s.effectEvents.filterIsInstance<EffectEvent.Damage>().single()
        assertEquals(brute.id, damage.enemyId)
        assertEquals(14 - 4, damage.amount) // T1 lightning 14, armor 4
    }

    @Test
    fun `commands are rejected after the game ends`() {
        val s = GameSession(testMap, listOf(WaveDef(listOf(SpawnEntry(EnemyType.SCOUT, 1, 1f)))))
        s.execute(PlayerCommand.BuildTower(5, 0, Element.LIGHTNING))
        s.execute(PlayerCommand.StartNextWave)
        s.run(30f)
        assertEquals(GameStatus.VICTORY, s.status)
        assertEquals(false, s.execute(PlayerCommand.BuildTower(6, 0, Element.FIRE)))
    }
}
