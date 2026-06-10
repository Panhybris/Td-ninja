package com.moonshade.shadowvillage.render.sprites

import android.graphics.Bitmap
import android.graphics.Canvas
import com.moonshade.shadowvillage.core.data.Element
import com.moonshade.shadowvillage.core.data.EnemyType

/**
 * Pre-renders the vector sprites into bitmaps once per size so per-frame
 * work is a cheap blit. Rebuilt when the surface size changes.
 */
class SpriteCache {

    private val cache = HashMap<String, Bitmap>()

    fun clear() {
        cache.values.forEach { it.recycle() }
        cache.clear()
    }

    private fun bitmap(key: String, sizePx: Int, render: (Canvas, Float) -> Unit): Bitmap =
        cache.getOrPut("$key@$sizePx") {
            val size = sizePx.coerceAtLeast(4)
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            render(Canvas(bmp), size.toFloat())
            bmp
        }

    fun ninja(element: Element, tier: Int, sizePx: Int): Bitmap =
        bitmap("ninja/$element/$tier", sizePx) { canvas, size ->
            NinjaSprites.draw(canvas, element, tier, size)
        }

    fun enemy(type: EnemyType, sizePx: Int): Bitmap =
        bitmap("enemy/$type", sizePx) { canvas, size ->
            EnemySprites.draw(canvas, type, size)
        }
}
