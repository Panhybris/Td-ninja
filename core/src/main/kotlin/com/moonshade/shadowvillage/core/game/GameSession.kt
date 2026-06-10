package com.moonshade.shadowvillage.core.game

import com.moonshade.shadowvillage.core.combat.AttackResolver
import com.moonshade.shadowvillage.core.combat.TargetSelector
import com.moonshade.shadowvillage.core.data.Balance
import com.moonshade.shadowvillage.core.data.Element
import com.moonshade.shadowvillage.core.data.EnemyType
import com.moonshade.shadowvillage.core.data.HeroData
import com.moonshade.shadowvillage.core.data.TowerData
import com.moonshade.shadowvillage.core.data.WaveDef
import com.moonshade.shadowvillage.core.entity.EffectEvent
import com.moonshade.shadowvillage.core.entity.Enemy
import com.moonshade.shadowvillage.core.entity.Hero
import com.moonshade.shadowvillage.core.entity.Projectile
import com.moonshade.shadowvillage.core.entity.Tower
import com.moonshade.shadowvillage.core.map.GameMap
import com.moonshade.shadowvillage.core.map.TileType
import com.moonshade.shadowvillage.core.wave.WaveSpawner
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.max
import kotlin.random.Random

/**
 * The full game simulation for one play-through of one map. Pure JVM —
 * deterministic given the same seed and command sequence.
 *
 * Thread model: [enqueue] may be called from any thread; everything else
 * (including all reads by the renderer) happens on the game-loop thread.
 */
class GameSession(
    val map: GameMap,
    private val waves: List<WaveDef>,
    seed: Long = 0L,
) {
    @Suppress("unused")
    private val rng = Random(seed)

    var gold = Balance.STARTING_GOLD
        private set
    var lives = Balance.STARTING_LIVES
        private set
    var status = GameStatus.RUNNING
        private set
    var tickCount = 0L
        private set

    private val spawner = WaveSpawner(waves)
    val waveNumber: Int get() = spawner.currentWave
    val totalWaves: Int get() = spawner.totalWaves
    val canStartNextWave: Boolean get() = status == GameStatus.RUNNING && spawner.canStartNext

    /** True while the current wave is still spawning or enemies remain. */
    val waveInProgress: Boolean get() = spawner.hasPendingSpawns || enemies.isNotEmpty()

    val enemies = mutableListOf<Enemy>()
    /** Copy-on-write so UI-thread hit-testing can read it safely. */
    val towers = java.util.concurrent.CopyOnWriteArrayList<Tower>()
    val projectiles = mutableListOf<Projectile>()

    @Volatile
    var hero: Hero? = null
        private set

    /** Render hints emitted during the most recent tick. */
    val effectEvents = mutableListOf<EffectEvent>()

    private val commands = ConcurrentLinkedQueue<PlayerCommand>()
    private var nextEntityId = 1
    private var lastClearedWave = 0

    /** Thread-safe; commands run at the start of the next tick. */
    fun enqueue(command: PlayerCommand) {
        commands.add(command)
    }

    /** Test hook: grants gold directly so tests can skip economy grinding. */
    internal fun grantGold(amount: Int) {
        gold += amount
    }

    fun towerAt(col: Int, row: Int): Tower? = towers.find { it.col == col && it.row == row }

    fun towerById(id: Int): Tower? = towers.find { it.id == id }

    fun canBuildAt(col: Int, row: Int): Boolean =
        map.isBuildable(col, row) && towerAt(col, row) == null

    fun sellValue(tower: Tower): Int = (tower.invested * Balance.SELL_REFUND).toInt()

    fun update(dt: Float) {
        if (status != GameStatus.RUNNING) return
        tickCount++
        effectEvents.clear()

        drainCommands()
        spawner.update(dt) { type, hpScale -> spawnEnemy(type, hpScale) }
        updateEnemies(dt)
        updateTowers(dt)
        updateHero(dt)
        updateProjectiles(dt)
        cleanup()
        checkWaveCleared()
        checkEndConditions()
    }

    private fun drainCommands() {
        while (true) {
            val cmd = commands.poll() ?: break
            execute(cmd)
        }
    }

    /**
     * Validates and applies a command immediately. Returns false if the
     * command is rejected (not enough gold, invalid cell, max tier...).
     * Exposed for tests; gameplay code should use [enqueue].
     */
    fun execute(command: PlayerCommand): Boolean {
        if (status != GameStatus.RUNNING) return false
        return when (command) {
            is PlayerCommand.BuildTower -> {
                val cost = TowerData.buildCost(command.element)
                if (gold < cost || !canBuildAt(command.col, command.row)) return false
                gold -= cost
                towers += Tower(nextEntityId++, command.element, command.col, command.row)
                true
            }
            is PlayerCommand.UpgradeTower -> {
                val tower = towerById(command.towerId) ?: return false
                val cost = tower.upgradeCost ?: return false
                if (gold < cost) return false
                gold -= cost
                tower.upgrade(command.spec)
                true
            }
            is PlayerCommand.SellTower -> {
                val tower = towerById(command.towerId) ?: return false
                gold += sellValue(tower)
                towers.remove(tower)
                true
            }
            is PlayerCommand.SetTargeting -> {
                val tower = towerById(command.towerId) ?: return false
                tower.targeting = command.mode
                true
            }
            PlayerCommand.StartNextWave -> {
                val started = spawner.startNext(Balance.hpScale(spawner.currentWave + 1))
                if (started) effectEvents += EffectEvent.WaveStarted(spawner.currentWave)
                started
            }
            is PlayerCommand.PlaceHero -> {
                if (!canPlaceHeroAt(command.col, command.row)) return false
                val h = hero
                when {
                    h == null -> {
                        hero = Hero(command.col, command.row)
                        true
                    }
                    h.canRelocate -> {
                        h.moveTo(command.col, command.row)
                        true
                    }
                    else -> false
                }
            }
            PlayerCommand.HeroAbility -> {
                val h = hero ?: return false
                if (!h.canUseAbility) return false
                h.abilityCooldown = HeroData.ABILITY_COOLDOWN
                effectEvents += EffectEvent.HeroAbilityUsed(h.pos, HeroData.ABILITY_RADIUS)
                // Strikes everything in radius, flyers included; bosses resist
                // the control effects via their own immunities but take damage.
                for (enemy in enemies.filter { it.alive && it.pos.distanceTo(h.pos) <= HeroData.ABILITY_RADIUS }) {
                    enemy.applyKnockback(HeroData.ABILITY_KNOCKBACK)
                    enemy.applyStun(HeroData.ABILITY_STUN)
                    damageEnemy(enemy, HeroData.ABILITY_DAMAGE)
                }
                true
            }
        }
    }

    private fun spawnEnemy(type: EnemyType, hpScale: Float) {
        enemies += Enemy(nextEntityId++, type, hpScale, map)
    }

    private fun updateEnemies(dt: Float) {
        for (enemy in enemies) {
            val wasAlive = enemy.alive
            enemy.update(dt)
            if (wasAlive && !enemy.alive) {
                if (enemy.reachedGoal) {
                    lives = max(0, lives - enemy.stats.lifeCost)
                    effectEvents += EffectEvent.EnemyLeaked(enemy.pos, enemy.stats.lifeCost)
                } else {
                    awardKill(enemy)
                }
            }
        }
    }

    private fun awardKill(enemy: Enemy) {
        gold += enemy.stats.bounty
        effectEvents += EffectEvent.EnemyDeath(enemy.pos, enemy.type, enemy.stats.bounty)
    }

    private fun updateTowers(dt: Float) {
        for (tower in towers) {
            tower.cooldown -= dt
            if (tower.cooldown > 0f) continue
            val target = TargetSelector.select(tower, enemies) ?: continue
            tower.cooldown = 1f / tower.stats.fireRate
            tower.shotCounter++
            fire(tower, target)
        }
    }

    /** Attack style follows capabilities, not elements, so specs can mix them. */
    private fun fire(tower: Tower, target: Enemy) {
        effectEvents += EffectEvent.TowerFired(tower.id, target.pos)
        val stats = tower.stats
        when {
            stats.chainCount > 0 -> {
                val chain = AttackResolver.chainTargets(target, enemies, stats.chainCount, stats.chainRadius)
                var dmg = stats.damage.toFloat()
                for (enemy in chain) {
                    damageEnemy(enemy, dmg)
                    dmg *= stats.chainFalloff
                }
                effectEvents += EffectEvent.LightningArc(listOf(tower.pos) + chain.map { it.pos })
            }
            stats.splashRadius > 0f -> {
                projectiles += Projectile(
                    nextEntityId++, tower.element, stats,
                    pos = tower.pos, target = null, targetPoint = target.pos,
                )
            }
            else -> {
                val knock = stats.knockbackEvery > 0 && tower.shotCounter % stats.knockbackEvery == 0
                val stun = stats.stunEvery > 0 && tower.shotCounter % stats.stunEvery == 0
                projectiles += Projectile(
                    nextEntityId++, tower.element, stats,
                    pos = tower.pos, target = target, targetPoint = null,
                    knockback = knock, stun = stun,
                )
            }
        }
    }

    fun canPlaceHeroAt(col: Int, row: Int): Boolean =
        map.isInside(col, row) &&
            map.tileAt(col, row) != TileType.BLOCKED &&
            towerAt(col, row) == null

    private fun updateHero(dt: Float) {
        val h = hero ?: return
        h.tickCooldowns(dt)
        if (h.attackCooldown > 0f) return
        // Deterministic cleave: nearest ground enemies first, ids break ties.
        val victims = enemies
            .filter { it.alive && !it.flying && it.pos.distanceTo(h.pos) <= HeroData.MELEE_RANGE }
            .sortedWith(compareBy({ it.pos.distanceTo(h.pos) }, { it.id }))
            .take(HeroData.CLEAVE)
        if (victims.isEmpty()) return
        h.attackCooldown = 1f / HeroData.MELEE_RATE
        for (enemy in victims) {
            effectEvents += EffectEvent.HeroAttack(h.pos, enemy.pos)
            damageEnemy(enemy, HeroData.MELEE_DAMAGE)
        }
    }

    private fun updateProjectiles(dt: Float) {
        for (projectile in projectiles) {
            if (projectile.update(dt)) {
                resolveImpact(projectile)
            }
        }
    }

    private fun resolveImpact(projectile: Projectile) {
        val stats = projectile.stats
        if (stats.splashRadius > 0f) {
            effectEvents += EffectEvent.Explosion(projectile.aimPoint, stats.splashRadius)
            for (enemy in AttackResolver.splashVictims(projectile.aimPoint, enemies, stats.splashRadius)) {
                if (stats.burnDps > 0f) enemy.applyBurn(stats.burnDps, stats.burnDuration)
                if (stats.slowFactor > 0f) enemy.applySlow(stats.slowFactor, stats.slowDuration)
                damageEnemy(enemy, stats.damage.toFloat())
            }
        } else {
            val target = projectile.target ?: return
            if (!target.alive) return
            effectEvents += EffectEvent.Impact(target.pos, projectile.element)
            if (stats.burnDps > 0f) target.applyBurn(stats.burnDps, stats.burnDuration)
            if (stats.slowFactor > 0f) target.applySlow(stats.slowFactor, stats.slowDuration)
            if (projectile.knockback) target.applyKnockback(stats.knockback)
            if (projectile.stun) target.applyStun(stats.stunDuration)
            damageEnemy(target, stats.damage.toFloat())
        }
    }

    private fun damageEnemy(enemy: Enemy, raw: Float) {
        if (!enemy.alive) return
        val applied = enemy.takeDamage(raw)
        effectEvents += EffectEvent.Damage(enemy.id, enemy.pos, applied.toInt())
        if (!enemy.alive) awardKill(enemy)
    }

    private fun cleanup() {
        enemies.removeAll { !it.alive }
        projectiles.removeAll { it.done }
    }

    private fun checkWaveCleared() {
        if (spawner.currentWave > lastClearedWave &&
            !spawner.hasPendingSpawns &&
            enemies.isEmpty()
        ) {
            lastClearedWave = spawner.currentWave
            val bonus = Balance.waveClearBonus(lastClearedWave)
            gold += bonus
            effectEvents += EffectEvent.WaveCleared(lastClearedWave, bonus)
        }
    }

    private fun checkEndConditions() {
        if (lives <= 0) {
            status = GameStatus.DEFEAT
        } else if (!spawner.canStartNext && !spawner.hasPendingSpawns && enemies.isEmpty()) {
            status = GameStatus.VICTORY
        }
    }
}
