package com.moonshade.shadowvillage.core.data

object Balance {
    const val STARTING_GOLD = 200
    const val STARTING_LIVES = 25
    const val SELL_REFUND = 0.7f

    /** Logic tick length in seconds (30 ticks/sec). */
    const val TICK = 1f / 30f

    fun hpScale(wave: Int): Float = 1f + 0.10f * (wave - 1)

    fun waveClearBonus(wave: Int): Int = 15 + 3 * wave
}
