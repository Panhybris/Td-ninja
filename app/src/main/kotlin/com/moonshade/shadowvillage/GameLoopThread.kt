package com.moonshade.shadowvillage

import android.graphics.Canvas
import android.os.Build
import android.view.SurfaceHolder
import com.moonshade.shadowvillage.screen.ScreenManager

/**
 * Dedicated game thread: updates the current screen and draws it to the
 * surface. Simulation pacing (fixed timestep, speed, pause) lives inside
 * PlayScreen; this loop just provides frames and wall-clock dt.
 */
class GameLoopThread(
    private val holder: SurfaceHolder,
    private val screens: ScreenManager,
) : Thread("GameLoop") {

    @Volatile
    var running = true

    @Volatile
    private var alive = true

    fun shutdown() {
        alive = false
        join(1000)
    }

    override fun run() {
        var lastNanos = System.nanoTime()
        while (alive) {
            if (!running) {
                sleep(50)
                lastNanos = System.nanoTime()
                continue
            }
            val now = System.nanoTime()
            // Clamp dt so a hitch doesn't fast-forward the simulation.
            val dt = ((now - lastNanos) / 1_000_000_000.0).toFloat().coerceAtMost(0.1f)
            lastNanos = now

            screens.swapIfNeeded()
            val screen = screens.current
            screen.update(dt)

            val canvas = lockCanvas() ?: continue
            try {
                screen.draw(canvas)
            } finally {
                try {
                    holder.unlockCanvasAndPost(canvas)
                } catch (_: Exception) {
                    // Surface died mid-frame; the loop will exit via alive=false.
                }
            }
        }
    }

    private fun lockCanvas(): Canvas? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            holder.lockHardwareCanvas()
        } else {
            holder.lockCanvas()
        }
    } catch (_: Exception) {
        null
    }
}
