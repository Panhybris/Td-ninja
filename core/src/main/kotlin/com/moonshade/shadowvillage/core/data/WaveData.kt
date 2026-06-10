package com.moonshade.shadowvillage.core.data

data class SpawnEntry(
    val type: EnemyType,
    val count: Int,
    /** Seconds between spawns of this entry. */
    val interval: Float,
    /** Seconds after the wave starts before this entry begins spawning. */
    val startDelay: Float = 0f,
)

data class WaveDef(val entries: List<SpawnEntry>) {
    val totalEnemies: Int get() = entries.sumOf { it.count }
}

private class WaveListBuilder {
    val waves = mutableListOf<WaveDef>()

    fun wave(block: WaveBuilder.() -> Unit) {
        waves += WaveDef(WaveBuilder().apply(block).entries)
    }
}

private class WaveBuilder {
    val entries = mutableListOf<SpawnEntry>()

    fun send(type: EnemyType, count: Int, every: Float, after: Float = 0f) {
        entries += SpawnEntry(type, count, every, after)
    }
}

object WaveData {

    fun forMap(@Suppress("UNUSED_PARAMETER") mapId: String): List<WaveDef> = WAVES

    private val WAVES: List<WaveDef> = run {
        val b = WaveListBuilder()
        with(b) {
            wave { send(EnemyType.SCOUT, 8, every = 1.0f) }                                          // 1
            wave { send(EnemyType.SCOUT, 12, every = 0.9f) }                                         // 2
            wave { send(EnemyType.NINJA, 8, every = 1.0f) }                                          // 3
            wave {                                                                                   // 4
                send(EnemyType.NINJA, 10, every = 1.3f)
                send(EnemyType.SCOUT, 6, every = 0.9f, after = 5f)
            }
            wave { send(EnemyType.WOLF, 8, every = 1.4f) }                                           // 5
            wave {                                                                                   // 6
                send(EnemyType.NINJA, 10, every = 1.1f)
                send(EnemyType.WOLF, 3, every = 1.6f, after = 4f)
            }
            wave { send(EnemyType.BRUTE, 4, every = 2.2f) }                                          // 7
            wave { send(EnemyType.RAIDER, 10, every = 0.9f) }                                        // 8
            wave {                                                                                   // 9
                send(EnemyType.WOLF, 10, every = 1.0f)
                send(EnemyType.RAIDER, 5, every = 1.2f, after = 3f)
            }
            wave {                                                                                   // 10: mini-boss
                send(EnemyType.NINJA, 8, every = 0.8f)
                send(EnemyType.ONI_VANGUARD, 1, every = 1f, after = 5f)
            }
            wave { send(EnemyType.BRUTE, 6, every = 1.9f) }                                          // 11
            wave {                                                                                   // 12
                send(EnemyType.RAIDER, 12, every = 1.0f)
                send(EnemyType.SCOUT, 10, every = 0.6f, after = 2f)
            }
            wave { send(EnemyType.MENDER, 8, every = 1.1f) }                                         // 13
            wave {                                                                                   // 14
                send(EnemyType.WOLF, 14, every = 0.7f)
                send(EnemyType.MENDER, 4, every = 1.4f, after = 4f)
            }
            wave {                                                                                   // 15
                send(EnemyType.BRUTE, 8, every = 1.4f)
                send(EnemyType.RAIDER, 8, every = 0.9f, after = 3f)
            }
            wave {                                                                                   // 16
                send(EnemyType.MENDER, 10, every = 1.0f)
                send(EnemyType.WOLF, 10, every = 0.8f, after = 4f)
            }
            wave { send(EnemyType.BRUTE, 10, every = 1.2f) }                                         // 17
            wave {                                                                                   // 18
                send(EnemyType.RAIDER, 16, every = 0.7f)
                send(EnemyType.MENDER, 6, every = 1.2f, after = 4f)
            }
            wave {                                                                                   // 19
                send(EnemyType.BRUTE, 8, every = 1.3f)
                send(EnemyType.WOLF, 12, every = 0.7f, after = 3f)
                send(EnemyType.MENDER, 6, every = 1.2f, after = 6f)
            }
            wave {                                                                                   // 20: boss
                send(EnemyType.BRUTE, 4, every = 2.0f)
                send(EnemyType.RAIDER, 8, every = 1.0f, after = 3f)
                send(EnemyType.ONI_WARLORD, 1, every = 1f, after = 8f)
            }
        }
        b.waves
    }
}
