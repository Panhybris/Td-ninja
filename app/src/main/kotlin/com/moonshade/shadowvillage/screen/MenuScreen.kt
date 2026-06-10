package com.moonshade.shadowvillage.screen

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.MotionEvent
import com.moonshade.shadowvillage.core.data.Element
import com.moonshade.shadowvillage.render.Palette
import com.moonshade.shadowvillage.render.sprites.NinjaPose
import com.moonshade.shadowvillage.render.sprites.NinjaSprites
import kotlin.math.sin

class MenuScreen(private val screens: ScreenManager) : Screen {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    private var time = 0f
    private var width = 0
    private var height = 0
    private var sky: Shader? = null

    override fun onSizeChanged(width: Int, height: Int) {
        this.width = width
        this.height = height
        sky = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            0xFF1B2238.toInt(), 0xFF3A4A6B.toInt(), Shader.TileMode.CLAMP,
        )
    }

    override fun update(dt: Float) {
        time += dt
    }

    override fun draw(canvas: Canvas) {
        if (width == 0) return
        paint.shader = sky
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // moon
        paint.color = 0xFFE8E4D8.toInt()
        canvas.drawCircle(width * 0.82f, height * 0.18f, height * 0.09f, paint)
        paint.color = 0xFF1B2238.toInt()
        canvas.drawCircle(width * 0.85f, height * 0.16f, height * 0.08f, paint)

        // village rooftops silhouette
        paint.color = 0xFF141A2C.toInt()
        val base = height * 0.86f
        for (i in 0 until 6) {
            val x = width * (0.05f + i * 0.16f)
            val w = width * 0.13f
            val h = height * (0.10f + (i % 3) * 0.05f)
            canvas.drawRect(x, base - h, x + w, base + height, paint)
            // pagoda roof
            val roof = android.graphics.Path().apply {
                moveTo(x - w * 0.18f, base - h)
                lineTo(x + w / 2, base - h - height * 0.07f)
                lineTo(x + w * 1.18f, base - h)
                close()
            }
            canvas.drawPath(roof, paint)
        }

        // title
        text.color = Palette.HUD_TEXT
        text.textSize = height * 0.13f
        canvas.drawText("SHADOW VILLAGE", width / 2f, height * 0.30f, text)
        text.color = Palette.GOLD
        canvas.drawText("DEFENSE", width / 2f, height * 0.44f, text)

        // the five elemental defenders, lined up
        val size = height * 0.22f
        val totalW = size * 5.4f
        var x = (width - totalW) / 2f
        for (element in Element.entries) {
            canvas.save()
            val bob = sin(time * 2.5f + element.ordinal) * height * 0.012f
            canvas.translate(x, height * 0.52f + bob)
            NinjaSprites.draw(canvas, element, tier = 3, spec = null, pose = NinjaPose.IDLE, size = size)
            canvas.restore()
            x += size * 1.1f
        }

        // pulsing prompt
        text.color = Palette.HUD_TEXT
        text.alpha = (155 + 100 * sin(time * 4f)).toInt().coerceIn(0, 255)
        text.textSize = height * 0.05f
        canvas.drawText("TAP TO DEFEND THE VILLAGE", width / 2f, height * 0.90f, text)
        text.alpha = 255
    }

    override fun onTouch(event: MotionEvent) {
        if (event.action == MotionEvent.ACTION_DOWN) {
            screens.navigate(MapSelectScreen(screens))
        }
    }
}
