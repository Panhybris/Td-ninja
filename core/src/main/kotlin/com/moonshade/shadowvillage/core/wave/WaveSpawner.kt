package com.moonshade.shadowvillage.core.wave

import com.moonshade.shadowvillage.core.data.EnemyType
import com.moonshade.shadowvillage.core.data.WaveDef

/**
 * Tracks which wave is active and emits spawns on schedule. Waves are
 * started explicitly (player taps "Send Wave"); several may overlap.
 */
class WaveSpawner(private val waves: List<WaveDef>) {

    private class ActiveEntry(
        val type: EnemyType,
        var remaining: Int,
        val interval: Float,
        var timer: Float,
        val hpScale: Float,
    )

    private val active = mutableListOf<ActiveEntry>()

    /** Highest wave number started so far, 0 before the first. */
    var currentWave = 0
        private set

    val totalWaves: Int get() = waves.size

    val hasPendingSpawns: Boolean get() = active.isNotEmpty()

    val canStartNext: Boolean get() = currentWave < totalWaves

    fun startNext(hpScale: Float): Boolean {
        if (!canStartNext) return false
        val def = waves[currentWave]
        currentWave++
        for (entry in def.entries) {
            active += ActiveEntry(entry.type, entry.count, entry.interval, entry.startDelay, hpScale)
        }
        return true
    }

    fun update(dt: Float, spawn: (EnemyType, Float) -> Unit) {
        val it = active.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            entry.timer -= dt
            while (entry.timer <= 0f && entry.remaining > 0) {
                spawn(entry.type, entry.hpScale)
                entry.remaining--
                entry.timer += entry.interval
            }
            if (entry.remaining <= 0) it.remove()
        }
    }
}
