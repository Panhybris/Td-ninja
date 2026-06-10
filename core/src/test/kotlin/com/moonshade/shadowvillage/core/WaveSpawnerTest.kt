package com.moonshade.shadowvillage.core

import com.moonshade.shadowvillage.core.data.Balance
import com.moonshade.shadowvillage.core.data.EnemyType
import com.moonshade.shadowvillage.core.data.SpawnEntry
import com.moonshade.shadowvillage.core.data.WaveData
import com.moonshade.shadowvillage.core.data.WaveDef
import com.moonshade.shadowvillage.core.wave.WaveSpawner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WaveSpawnerTest {

    private val twoWaves = listOf(
        WaveDef(listOf(SpawnEntry(EnemyType.SCOUT, count = 3, interval = 1f))),
        WaveDef(listOf(SpawnEntry(EnemyType.NINJA, count = 2, interval = 0.5f, startDelay = 1f))),
    )

    private fun simulate(spawner: WaveSpawner, seconds: Float, spawned: MutableList<Pair<EnemyType, Float>>) {
        val ticks = (seconds / Balance.TICK).toInt()
        repeat(ticks) {
            spawner.update(Balance.TICK) { type, scale -> spawned += type to scale }
        }
    }

    @Test
    fun `spawns the full count at the configured interval`() {
        val spawner = WaveSpawner(twoWaves)
        val spawned = mutableListOf<Pair<EnemyType, Float>>()
        assertTrue(spawner.startNext(hpScale = 1f))
        simulate(spawner, seconds = 4f, spawned)
        assertEquals(3, spawned.size)
        assertTrue(spawned.all { it.first == EnemyType.SCOUT })
        assertFalse(spawner.hasPendingSpawns)
    }

    @Test
    fun `start delay holds back an entry`() {
        val spawner = WaveSpawner(twoWaves)
        val spawned = mutableListOf<Pair<EnemyType, Float>>()
        spawner.startNext(1f) // wave 1
        simulate(spawner, 4f, spawned)
        spawned.clear()
        spawner.startNext(1.12f) // wave 2: delayed 1s
        simulate(spawner, 0.9f, spawned)
        assertEquals(0, spawned.size, "nothing before startDelay")
        simulate(spawner, 2f, spawned)
        assertEquals(2, spawned.size)
        assertEquals(1.12f, spawned[0].second, "hpScale must flow through")
    }

    @Test
    fun `waves can overlap`() {
        val spawner = WaveSpawner(twoWaves)
        val spawned = mutableListOf<Pair<EnemyType, Float>>()
        spawner.startNext(1f)
        spawner.startNext(1.12f)
        assertFalse(spawner.canStartNext)
        simulate(spawner, 5f, spawned)
        assertEquals(5, spawned.size)
        assertEquals(2, spawner.currentWave)
    }

    @Test
    fun `cannot start beyond the last wave`() {
        val spawner = WaveSpawner(twoWaves)
        assertTrue(spawner.startNext(1f))
        assertTrue(spawner.startNext(1f))
        assertFalse(spawner.startNext(1f))
        assertEquals(2, spawner.currentWave)
    }

    @Test
    fun `shipped campaign has 20 waves with a mini-boss and final boss`() {
        val waves = WaveData.forMap("river_crossing")
        assertEquals(20, waves.size)
        assertTrue(waves[9].entries.any { it.type == EnemyType.ONI_VANGUARD }, "wave 10 mini-boss")
        assertTrue(waves[19].entries.any { it.type == EnemyType.ONI_WARLORD }, "wave 20 boss")
        assertTrue(waves.all { it.totalEnemies in 1..40 })
    }
}
