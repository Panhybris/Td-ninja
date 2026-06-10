package com.moonshade.shadowvillage.screen

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import com.moonshade.shadowvillage.core.data.Balance
import com.moonshade.shadowvillage.core.data.WaveData
import com.moonshade.shadowvillage.core.entity.EffectEvent
import com.moonshade.shadowvillage.core.game.GameSession
import com.moonshade.shadowvillage.core.game.GameStatus
import com.moonshade.shadowvillage.core.game.PlayerCommand
import com.moonshade.shadowvillage.core.map.GameMap
import com.moonshade.shadowvillage.input.BuildMenu
import com.moonshade.shadowvillage.input.TowerPanel
import com.moonshade.shadowvillage.render.Camera
import com.moonshade.shadowvillage.render.GameRenderer
import com.moonshade.shadowvillage.render.HudRenderer
import com.moonshade.shadowvillage.render.MapRenderer
import com.moonshade.shadowvillage.render.Palette
import com.moonshade.shadowvillage.render.sprites.ArcFx
import com.moonshade.shadowvillage.render.sprites.EffectSprites
import com.moonshade.shadowvillage.render.sprites.Fx
import com.moonshade.shadowvillage.render.sprites.PoofFx
import com.moonshade.shadowvillage.render.sprites.RingFx
import com.moonshade.shadowvillage.render.sprites.SparkFx
import com.moonshade.shadowvillage.render.sprites.SpriteCache
import com.moonshade.shadowvillage.render.sprites.TextFx

class PlayScreen(
    private val screens: ScreenManager,
    private val map: GameMap,
) : Screen {

    private val session = GameSession(map, WaveData.forMap(map.id))
    private val camera = Camera(map)
    private val sprites = SpriteCache()
    private val mapRenderer = MapRenderer(map)
    private val gameRenderer = GameRenderer(sprites)
    private val hud = HudRenderer()

    private var width = 0
    private var height = 0
    private var accumulator = 0f

    @Volatile
    private var paused = false

    @Volatile
    private var speed = 1

    @Volatile
    private var buildMenu: BuildMenu? = null

    @Volatile
    private var towerPanel: TowerPanel? = null

    private val fx = mutableListOf<Fx>()

    // overlay buttons (pause menu and end screens share the layout)
    private val overlayPrimary = RectF()
    private val overlaySecondary = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    override fun onSizeChanged(width: Int, height: Int) {
        this.width = width
        this.height = height
        camera.onSizeChanged(width, height)
        mapRenderer.invalidate()
        sprites.clear()
        hud.onSizeChanged(width, height)
        towerPanel?.layout(width, height)
        val btnW = width * 0.22f
        val btnH = height * 0.10f
        overlayPrimary.set(width / 2f - btnW - width * 0.02f, height * 0.62f, width / 2f - width * 0.02f, height * 0.62f + btnH)
        overlaySecondary.set(width / 2f + width * 0.02f, height * 0.62f, width / 2f + btnW + width * 0.02f, height * 0.62f + btnH)
    }

    override fun update(dt: Float) {
        gameRenderer.tick(dt)
        for (f in fx) f.age += dt
        fx.removeAll { it.done }

        if (paused || session.status != GameStatus.RUNNING) return

        accumulator += dt
        while (accumulator >= Balance.TICK) {
            accumulator -= Balance.TICK
            repeat(speed) {
                session.update(Balance.TICK)
                collectFx()
            }
        }
    }

    private fun collectFx() {
        for (event in session.effectEvents) {
            when (event) {
                is EffectEvent.LightningArc -> fx += ArcFx(event.points)
                is EffectEvent.Explosion -> fx += RingFx(event.pos, event.radius, 0xFFC4A284.toInt())
                is EffectEvent.Impact -> fx += SparkFx(event.pos, Palette.of(event.element).light)
                is EffectEvent.EnemyDeath -> {
                    fx += PoofFx(event.pos)
                    fx += TextFx(event.pos, "+${event.bounty}", Palette.GOLD)
                }
                is EffectEvent.EnemyLeaked -> fx += TextFx(event.pos, "-${event.livesLost}", Palette.LIFE)
                is EffectEvent.WaveCleared -> fx += TextFx(
                    map.waypoints[map.waypoints.size / 2], "WAVE CLEAR +${event.bonus}", Palette.GOLD,
                )
                is EffectEvent.TowerFired -> Unit
            }
        }
        // events are cleared by the session at the start of the next tick
    }

    override fun draw(canvas: Canvas) {
        if (width == 0) return
        mapRenderer.draw(canvas, camera, width, height)
        gameRenderer.draw(canvas, session, camera)
        for (f in fx) EffectSprites.draw(canvas, f, camera)

        towerPanel?.let {
            if (session.towerById(it.towerId) == null) towerPanel = null else it.draw(canvas, session, camera, sprites)
        }
        buildMenu?.draw(canvas, camera, session.gold)
        hud.draw(canvas, session, speed, paused)

        when {
            session.status == GameStatus.VICTORY -> overlay(canvas, "VICTORY!", Palette.GOLD, "REPLAY", "MAPS")
            session.status == GameStatus.DEFEAT -> overlay(canvas, "THE VILLAGE FELL", Palette.LIFE, "RETRY", "MAPS")
            paused -> overlay(canvas, "PAUSED", Palette.HUD_TEXT, "RESUME", "QUIT")
        }
    }

    private fun overlay(canvas: Canvas, title: String, titleColor: Int, primary: String, secondary: String) {
        paint.color = 0xB0101522.toInt()
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        text.color = titleColor
        text.textSize = height * 0.13f
        canvas.drawText(title, width / 2f, height * 0.38f, text)

        if (session.status != GameStatus.RUNNING) {
            text.textSize = height * 0.045f
            text.color = Palette.HUD_TEXT
            canvas.drawText(
                "Wave ${session.waveNumber}/${session.totalWaves}   Lives ${session.lives}",
                width / 2f, height * 0.50f, text,
            )
        }

        for ((rect, label) in listOf(overlayPrimary to primary, overlaySecondary to secondary)) {
            paint.color = Palette.BUTTON_ACTIVE
            canvas.drawRoundRect(rect, height * 0.02f, height * 0.02f, paint)
            text.color = Palette.HUD_TEXT
            text.textSize = height * 0.045f
            canvas.drawText(label, rect.centerX(), rect.centerY() + text.textSize * 0.35f, text)
        }
    }

    override fun onTouch(event: MotionEvent) {
        if (event.action != MotionEvent.ACTION_DOWN) return
        val x = event.x
        val y = event.y

        // 1) overlays capture everything
        if (session.status == GameStatus.VICTORY || session.status == GameStatus.DEFEAT) {
            when {
                overlayPrimary.contains(x, y) -> screens.navigate(PlayScreen(screens, map))
                overlaySecondary.contains(x, y) -> screens.navigate(MapSelectScreen(screens))
            }
            return
        }
        if (paused) {
            when {
                overlayPrimary.contains(x, y) -> paused = false
                overlaySecondary.contains(x, y) -> screens.navigate(MapSelectScreen(screens))
                hud.pauseBtn.contains(x, y) -> paused = false
            }
            return
        }

        // 2) tower panel buttons
        towerPanel?.let { panel ->
            if (panel.contains(x, y)) {
                val tower = session.towerById(panel.towerId) ?: return
                when (panel.hitTest(x, y)) {
                    TowerPanel.Action.UPGRADE -> session.enqueue(PlayerCommand.UpgradeTower(tower.id))
                    TowerPanel.Action.SELL -> {
                        session.enqueue(PlayerCommand.SellTower(tower.id))
                        towerPanel = null
                    }
                    TowerPanel.Action.TARGETING ->
                        session.enqueue(PlayerCommand.SetTargeting(tower.id, tower.targeting.next()))
                    null -> Unit
                }
                return
            }
        }

        // 3) build menu icons
        buildMenu?.let { menu ->
            val element = menu.hitTest(camera, x, y)
            buildMenu = null
            if (element != null) {
                session.enqueue(PlayerCommand.BuildTower(menu.col, menu.row, element))
                return
            }
        }

        // 4) HUD buttons
        when {
            hud.pauseBtn.contains(x, y) -> {
                paused = true
                return
            }
            hud.speedBtn.contains(x, y) -> {
                speed = if (speed == 1) 2 else 1
                return
            }
            hud.sendWaveBtn.contains(x, y) -> {
                session.enqueue(PlayerCommand.StartNextWave)
                return
            }
        }

        // 5) world taps
        val col = camera.screenToCol(x)
        val row = camera.screenToRow(y)
        val tappedTower = session.towerAt(col, row)
        when {
            tappedTower != null -> {
                towerPanel = TowerPanel(tappedTower.id).also { it.layout(width, height) }
            }
            session.canBuildAt(col, row) -> {
                towerPanel = null
                buildMenu = BuildMenu(col, row)
            }
            else -> {
                towerPanel = null
            }
        }
    }
}
