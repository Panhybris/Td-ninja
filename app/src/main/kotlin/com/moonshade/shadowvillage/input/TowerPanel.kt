package com.moonshade.shadowvillage.input

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.moonshade.shadowvillage.core.entity.Tower
import com.moonshade.shadowvillage.core.game.GameSession
import com.moonshade.shadowvillage.render.Camera
import com.moonshade.shadowvillage.render.Palette
import com.moonshade.shadowvillage.render.sprites.NinjaPose
import com.moonshade.shadowvillage.render.sprites.SpriteCache

/** Bottom strip with stats and Upgrade / Sell / Targeting for a selected tower. */
class TowerPanel(val towerId: Int) {

    enum class Action { UPGRADE, SELL, TARGETING }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFakeBoldText = true }

    private val panel = RectF()
    private val upgradeBtn = RectF()
    private val sellBtn = RectF()
    private val targetBtn = RectF()

    fun layout(width: Int, height: Int) {
        val h = height * 0.16f
        panel.set(0f, height - h, width.toFloat(), height.toFloat())
        val btnW = width * 0.17f
        val pad = h * 0.15f
        val btnTop = panel.top + pad
        val btnBottom = panel.bottom - pad
        targetBtn.set(width - btnW - pad, btnTop, width - pad, btnBottom)
        sellBtn.set(width - 2 * (btnW + pad), btnTop, width - btnW - 2 * pad, btnBottom)
        upgradeBtn.set(width - 3 * (btnW + pad), btnTop, width - 2 * btnW - 3 * pad, btnBottom)
    }

    fun contains(x: Float, y: Float) = panel.contains(x, y)

    fun hitTest(x: Float, y: Float): Action? = when {
        upgradeBtn.contains(x, y) -> Action.UPGRADE
        sellBtn.contains(x, y) -> Action.SELL
        targetBtn.contains(x, y) -> Action.TARGETING
        else -> null
    }

    fun draw(canvas: Canvas, session: GameSession, cam: Camera, sprites: SpriteCache) {
        val tower = session.towerById(towerId) ?: return
        val h = panel.height()

        // range circle on the field
        paint.color = Palette.RANGE_FILL
        val cx = cam.worldX(tower.pos)
        val cy = cam.worldY(tower.pos)
        canvas.drawCircle(cx, cy, tower.stats.range * cam.cellSize, paint)
        paint.color = Palette.RANGE_STROKE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = cam.cellSize * 0.05f
        canvas.drawCircle(cx, cy, tower.stats.range * cam.cellSize, paint)
        paint.style = Paint.Style.FILL

        // panel
        paint.color = Palette.PANEL_BG
        canvas.drawRect(panel, paint)

        // portrait
        val portrait = (h * 0.8f).toInt()
        canvas.drawBitmap(
            sprites.ninja(tower.element, tower.tier, tower.spec, NinjaPose.IDLE, portrait),
            h * 0.1f, panel.top + h * 0.1f, null,
        )

        // name + stats
        text.color = Palette.HUD_TEXT
        text.textAlign = Paint.Align.LEFT
        text.textSize = h * 0.30f
        val infoX = h * 1.05f
        canvas.drawText("${tower.element.towerName}  T${tower.tier}", infoX, panel.top + h * 0.38f, text)
        text.textSize = h * 0.24f
        text.color = 0xFFB9C2D6.toInt()
        val s = tower.stats
        canvas.drawText(
            "DMG ${s.damage}   RNG ${s.range}   RATE ${s.fireRate}/s",
            infoX, panel.top + h * 0.72f, text,
        )

        // buttons
        val upgradeCost = tower.upgradeCost
        drawButton(
            canvas, upgradeBtn,
            label = if (upgradeCost != null) "UPGRADE" else "MAX",
            sub = upgradeCost?.let { "$it g" },
            color = when {
                upgradeCost == null -> Palette.BUTTON_DISABLED
                session.gold >= upgradeCost -> 0xFF3E7D46.toInt()
                else -> Palette.BUTTON_DISABLED
            },
        )
        drawButton(canvas, sellBtn, "SELL", "+${session.sellValue(tower)} g", Palette.SELL)
        drawButton(canvas, targetBtn, "AIM", tower.targeting.displayName.uppercase(), Palette.BUTTON_ACTIVE)
    }

    private fun drawButton(canvas: Canvas, rect: RectF, label: String, sub: String?, color: Int) {
        paint.color = color
        canvas.drawRoundRect(rect, rect.height() * 0.2f, rect.height() * 0.2f, paint)
        text.color = Palette.HUD_TEXT
        text.textAlign = Paint.Align.CENTER
        text.textSize = rect.height() * 0.32f
        if (sub == null) {
            canvas.drawText(label, rect.centerX(), rect.centerY() + text.textSize * 0.35f, text)
        } else {
            canvas.drawText(label, rect.centerX(), rect.centerY() - text.textSize * 0.15f, text)
            text.textSize = rect.height() * 0.26f
            canvas.drawText(sub, rect.centerX(), rect.centerY() + text.textSize * 1.05f, text)
        }
    }
}
