package com.moonshade.shadowvillage.progress

import android.content.Context
import com.moonshade.shadowvillage.core.map.Maps

/**
 * On-device progression: best star rating per map (by lives kept) and
 * sequential map unlocking. Results only ever improve (max-merge).
 */
class ProgressStore(context: Context) {

    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)

    fun stars(mapId: String): Int = prefs.getInt("stars_$mapId", 0)

    fun recordResult(mapId: String, lives: Int): Int {
        val earned = starsFor(lives)
        if (earned > stars(mapId)) {
            prefs.edit().putInt("stars_$mapId", earned).apply()
        }
        return earned
    }

    fun isUnlocked(mapId: String): Boolean {
        val idx = Maps.all.indexOfFirst { it.id == mapId }
        return idx <= 0 || stars(Maps.all[idx - 1].id) >= 1
    }

    companion object {
        /** Of 25 starting lives: keep 20+ for 3 stars, 10+ for 2, any for 1. */
        fun starsFor(lives: Int): Int = when {
            lives >= 20 -> 3
            lives >= 10 -> 2
            lives >= 1 -> 1
            else -> 0
        }
    }
}
