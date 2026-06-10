package com.moonshade.shadowvillage

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.moonshade.shadowvillage.screen.MenuScreen
import com.moonshade.shadowvillage.screen.ScreenManager

class GameSurfaceView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private val screens = ScreenManager { manager -> MenuScreen(manager) }
    private var loop: GameLoopThread? = null

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        loop = GameLoopThread(holder, screens).also { it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        screens.onSizeChanged(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        loop?.shutdown()
        loop = null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        screens.current.onTouch(event)
        return true
    }

    fun resumeGame() {
        loop?.running = true
    }

    fun pauseGame() {
        loop?.running = false
    }
}
