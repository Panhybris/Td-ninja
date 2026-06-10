package com.moonshade.shadowvillage.input

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.moonshade.shadowvillage.core.data.SpecPath
import com.moonshade.shadowvillage.core.data.TowerData
import com.moonshade.shadowvillage.core.game.GameSession
import com.moonshade.shadowvillage.render.Camera
import com.moonshade.shadowvillage.render.Palette
import com.moonshade.shadowvillage.render.sprites.NinjaPose
import com.moonshade.shadowvillage.render.sprites.SpriteCache

/**
 * Bottom strip with stats and Upgrade / Sell / Targeting for a selected
 * tower. Upgrading at tier 2 opens a chooser between the two tier-3
 * specialization paths.
 */
class TowerPanel(val towerId: Int) {

    enum class Action { UPGRADE, SELL, TARGETING, SPEC_A, SPEC_B, BACK }

    /** Set while the T2 -> T3 path chooser is showing. UI-thread written, render-thread read. */
    @Volatile
    var choosingSpec = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFakeBoldText = true }

    private val panel = RectF()
    private val upgradeBtn = RectF()
    private val sellBtn = RectF()
    private val targetBtn = RectF()
    private val specABtn = RectF()
    private val specBBtn = RectF()
    private val backBtn = RectF()

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

        // chooser layout: back chevron on the left, two wide path buttons
        backBtn.set(pad, btnTop, pad + h * 0.7f, btnBottom)
        val specW = (width - backBtn.right - 3 * pad) / 2f
        specABtn.set(backBtn.right + pad, btnTop, backBtn.right + pad + specW, btnBottom)
        specBBtn.set(specABtn.right + pad, btnTop, specABtn.right + pad + specW, btnBottom)
    }

    fun contains(x: Float, y: Float) = panel.contains(x, y)

    fun hitTest(x: Float, y: Float): Action? = if (choosingSpec) {
        when {
            specABtn.contains(x, y) -> Action.SPEC_A
            specBBtn.contains(x, y) -> Action.SPEC_B
            backBtn.contains(x, y) -> Action.BACK
            else -> null
        }
    } else {
        when {
            upgradeBtn.contains(x, y) -> Action.UPGRADE
            sellBtn.contains(x, y) -> Action.SELL
            targetBtn.contains(x, y) -> Action.TARGETING
            else -> null
        }
    }

    fun draw(canvas: Canvas, session: GameSession, cam: Camera, sprites: SpriteCache) {
        val tower = session.towerById(towerId) ?: return
        if (tower.tier != 2) choosingSpec = false // upgrade landed or state changed
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

        if (choosingSpec) {
            drawSpecChooser(canvas, session, tower.element, tower.upgradeCost, sprites, h)
            return
        }

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
        val title = tower.spec?.let { TowerData.spec(tower.element, it).name } ?: tower.element.towerName
        canvas.drawText("$title  T${tower.tier}", infoX, panel.top + h * 0.38f, text)
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

    private fun drawSpecChooser(
        canvas: Canvas,
        session: GameSession,
        element: com.moonshade.shadowvillage.core.data.Element,
        cost: Int?,
        sprites: SpriteCache,
        h: Float,
    ) {
        // back chevron
        paint.color = Palette.BUTTON
        canvas.drawRoundRect(backBtn, h * 0.1f, h * 0.1f, paint)
        text.color = Palette.HUD_TEXT
        text.textAlign = Paint.Align.CENTER
        text.textSize = h * 0.4f
        canvas.drawText("<", backBtn.centerX(), backBtn.centerY() + text.textSize * 0.35f, text)

        val affordable = cost != null && session.gold >= cost
        for ((rect, path) in listOf(specABtn to SpecPath.A, specBBtn to SpecPath.B)) {
            val def = TowerData.spec(element, path)
            paint.color = if (affordable) Palette.BUTTON_ACTIVE else Palette.BUTTON_DISABLED
            canvas.drawRoundRect(rect, h * 0.12f, h * 0.12f, paint)

            // recolored mini portrait so the two paths read differently
            val mini = (rect.height() * 0.85f).toInt()
            canvas.drawBitmap(
                sprites.ninja(element, 3, path, NinjaPose.IDLE, mini),
                rect.left + h * 0.06f, rect.centerY() - mini / 2f, null,
            )

            text.textAlign = Paint.Align.LEFT
            text.color = Palette.HUD_TEXT
            text.textSize = h * 0.27f
            val tx = rect.left + mini + h * 0.14f
            canvas.drawText("${def.name}  ${cost ?: 0} g", tx, rect.centerY() - h * 0.06f, text)
            text.textSize = h * 0.20f
            text.color = 0xFFB9C2D6.toInt()
            canvas.drawText(def.blurb, tx, rect.centerY() + h * 0.22f, text)
        }
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
