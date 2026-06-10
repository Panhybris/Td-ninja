package com.moonshade.shadowvillage.screen

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import com.moonshade.shadowvillage.core.data.Balance
import com.moonshade.shadowvillage.core.data.EnemyType
import com.moonshade.shadowvillage.core.data.WaveData
import com.moonshade.shadowvillage.core.entity.EffectEvent
import com.moonshade.shadowvillage.core.game.GameSession
import com.moonshade.shadowvillage.core.game.GameStatus
import com.moonshade.shadowvillage.core.game.PlayerCommand
import com.moonshade.shadowvillage.core.map.GameMap
import com.moonshade.shadowvillage.input.BuildMenu
import com.moonshade.shadowvillage.input.TowerPanel
import com.moonshade.shadowvillage.render.AtmosphereRenderer
import com.moonshade.shadowvillage.render.Camera
import com.moonshade.shadowvillage.render.GameRenderer
import com.moonshade.shadowvillage.render.HudRenderer
import com.moonshade.shadowvillage.render.MapRenderer
import com.moonshade.shadowvillage.render.MapTheme
import com.moonshade.shadowvillage.render.Palette
import com.moonshade.shadowvillage.core.math.Vec2
import com.moonshade.shadowvillage.render.sprites.ArcFx
import com.moonshade.shadowvillage.render.sprites.BannerFx
import com.moonshade.shadowvillage.render.sprites.EffectSprites
import com.moonshade.shadowvillage.render.sprites.Fx
import com.moonshade.shadowvillage.render.sprites.PoofFx
import com.moonshade.shadowvillage.render.sprites.RingFx
import com.moonshade.shadowvillage.render.sprites.SparkFx
import com.moonshade.shadowvillage.render.sprites.SparkleFx
import com.moonshade.shadowvillage.render.sprites.SpriteCache
import com.moonshade.shadowvillage.render.sprites.TextFx

class PlayScreen(
    private val screens: ScreenManager,
    private val map: GameMap,
) : Screen {

    private val session = GameSession(map, WaveData.forMap(map.id))
    private val camera = Camera(map)
    private val sprites = SpriteCache()
    private val theme = MapTheme.forMap(map.id)
    private val mapRenderer = MapRenderer(map, theme)
    private val atmosphere = AtmosphereRenderer(map, theme)
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
    private val damageSums = HashMap<Int, Pair<Vec2, Int>>() // per-tick damage coalescing
    private var shakeAmp = 0f
    private var shakeTime = 0f

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
        atmosphere.update(dt) // ambient life keeps moving even while paused
        for (f in fx) f.age += dt
        fx.removeAll { it.done }
        shakeTime += dt
        shakeAmp *= (1f - 6f * dt).coerceAtLeast(0f)

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

    private fun addFx(f: Fx) {
        if (fx.size >= 120) fx.removeAt(0)
        fx += f
    }

    private fun shake(amount: Float) {
        if (amount > shakeAmp) shakeAmp = amount
    }

    private fun collectFx() {
        damageSums.clear()
        for (event in session.effectEvents) {
            when (event) {
                is EffectEvent.LightningArc -> addFx(ArcFx(event.points))
                is EffectEvent.Explosion -> {
                    addFx(RingFx(event.pos, event.radius, 0xFFC4A284.toInt()))
                    shake(0.12f)
                }
                is EffectEvent.Impact -> addFx(SparkFx(event.pos, Palette.of(event.element).light))
                is EffectEvent.EnemyDeath -> {
                    addFx(PoofFx(event.pos))
                    addFx(TextFx(event.pos, "+${event.bounty}", Palette.GOLD))
                    addFx(SparkleFx(event.pos))
                    if (event.type == EnemyType.ONI_WARLORD || event.type == EnemyType.ONI_VANGUARD) shake(0.5f)
                }
                is EffectEvent.EnemyLeaked -> {
                    addFx(TextFx(event.pos, "-${event.livesLost}", Palette.LIFE))
                    shake(0.25f)
                }
                is EffectEvent.WaveCleared -> addFx(BannerFx("WAVE CLEAR  +${event.bonus}", Palette.GOLD))
                is EffectEvent.TowerFired -> session.towerById(event.towerId)?.let {
                    gameRenderer.onTowerFired(it.id, it.pos, event.targetPos)
                }
                is EffectEvent.WaveStarted -> addFx(
                    when (event.wave) {
                        10 -> BannerFx("ONI VANGUARD APPROACHES", Palette.LIFE)
                        session.totalWaves -> BannerFx("ONI WARLORD APPROACHES", Palette.LIFE)
                        else -> BannerFx("WAVE ${event.wave}", Palette.HUD_TEXT)
                    },
                )
                is EffectEvent.Damage -> {
                    gameRenderer.onEnemyDamaged(event.enemyId)
                    val prev = damageSums[event.enemyId]
                    damageSums[event.enemyId] = event.pos to ((prev?.second ?: 0) + event.amount)
                }
                is EffectEvent.HeroAttack -> {
                    gameRenderer.onHeroAttack()
                    addFx(SparkFx(event.targetPos, Palette.HUD_TEXT))
                }
                is EffectEvent.HeroAbilityUsed -> {
                    addFx(RingFx(event.pos, event.radius, 0xFF2A2D45.toInt()))
                    shake(0.5f)
                }
            }
        }
        // one summed damage number per enemy per tick keeps the field readable
        for ((pos, sum) in damageSums.values) {
            addFx(TextFx(pos, "$sum", Palette.HUD_TEXT, scale = 0.6f))
        }
        // events are cleared by the session at the start of the next tick
    }

    override fun draw(canvas: Canvas) {
        if (width == 0) return
        canvas.save()
        if (shakeAmp > 0.003f) {
            val px = shakeAmp * camera.cellSize
            canvas.translate(
                kotlin.math.sin(shakeTime * 47f) * px,
                kotlin.math.cos(shakeTime * 31f) * px,
            )
        }
        mapRenderer.draw(canvas, camera, width, height)
        atmosphere.drawBehindEntities(canvas, camera)
        gameRenderer.draw(canvas, session, camera)
        atmosphere.drawOverEntities(canvas, camera, width, height)
        for (f in fx) EffectSprites.draw(canvas, f, camera)
        canvas.restore()

        drawBanners(canvas)

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

    /** Slide-in, hold, slide-out announcement across the top third. */
    private fun drawBanners(canvas: Canvas) {
        for (f in fx) {
            val banner = f as? BannerFx ?: continue
            val t = banner.t
            // ease: slide in 0..0.2, hold, slide out 0.8..1
            val slide = when {
                t < 0.2f -> 1f - t / 0.2f
                t > 0.8f -> (t - 0.8f) / 0.2f
                else -> 0f
            }
            val x = width / 2f + slide * slide * width * 0.6f * (if (t < 0.5f) -1f else 1f)
            val y = height * 0.28f
            text.textSize = height * 0.075f
            text.color = (0x99000000.toInt())
            canvas.drawText(banner.text, x + height * 0.006f, y + height * 0.006f, text)
            text.color = banner.color
            canvas.drawText(banner.text, x, y, text)
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
