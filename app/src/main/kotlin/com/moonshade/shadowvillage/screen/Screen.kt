package com.moonshade.shadowvillage.screen

import android.graphics.Canvas
import android.view.MotionEvent

/**
 * One full-screen game state (menu, map select, gameplay). [update] and
 * [draw] run on the game-loop thread; [onTouch] runs on the UI thread and
 * must only touch thread-safe state.
 */
interface Screen {
    fun update(dt: Float)
    fun draw(canvas: Canvas)
    fun onTouch(event: MotionEvent)
    fun onSizeChanged(width: Int, height: Int) {}
}

/** Swaps screens at a safe point in the frame. */
class ScreenManager(
    val progress: com.moonshade.shadowvillage.progress.ProgressStore,
    initial: (ScreenManager) -> Screen,
) {
    @Volatile
    private var pending: Screen? = null

    @Volatile
    var current: Screen = initial(this)
        private set

    @Volatile
    var width: Int = 0
        private set

    @Volatile
    var height: Int = 0
        private set

    fun navigate(screen: Screen) {
        pending = screen
    }

    fun onSizeChanged(width: Int, height: Int) {
        this.width = width
        this.height = height
        current.onSizeChanged(width, height)
    }

    /** Called by the loop thread at the start of every frame. */
    fun swapIfNeeded() {
        val next = pending ?: return
        pending = null
        current = next
        if (width > 0) next.onSizeChanged(width, height)
    }
}
