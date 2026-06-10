package com.moonshade.shadowvillage.core.map

import com.moonshade.shadowvillage.core.math.Vec2

/**
 * Parses ASCII map definitions.
 *
 * Legend: `S` spawn, `G` goal, `#` path, `.` buildable, `X` blocked scenery.
 * The path must be a single non-branching 4-connected chain from S to G.
 */
object MapParser {

    fun parse(id: String, name: String, ascii: List<String>): GameMap {
        require(ascii.isNotEmpty()) { "map $id: empty grid" }
        val cols = ascii[0].length
        require(ascii.all { it.length == cols }) { "map $id: ragged rows" }
        val rows = ascii.size

        val tiles = ascii.map { line ->
            line.map { ch ->
                when (ch) {
                    'S' -> TileType.SPAWN
                    'G' -> TileType.GOAL
                    '#' -> TileType.PATH
                    '.' -> TileType.BUILDABLE
                    'X' -> TileType.BLOCKED
                    else -> error("map $id: unknown tile '$ch'")
                }
            }
        }

        val spawns = allOf(tiles, TileType.SPAWN)
        val goals = allOf(tiles, TileType.GOAL)
        require(spawns.size == 1) { "map $id: expected 1 spawn, found ${spawns.size}" }
        require(goals.size == 1) { "map $id: expected 1 goal, found ${goals.size}" }

        val waypoints = walkPath(id, tiles, spawns.first(), goals.first())
        return GameMap(id, name, cols, rows, tiles, waypoints)
    }

    private fun allOf(tiles: List<List<TileType>>, type: TileType): List<Pair<Int, Int>> =
        tiles.flatMapIndexed { row, line ->
            line.mapIndexedNotNull { col, t -> if (t == type) col to row else null }
        }

    private fun walkPath(
        id: String,
        tiles: List<List<TileType>>,
        spawn: Pair<Int, Int>,
        goal: Pair<Int, Int>,
    ): List<Vec2> {
        val rows = tiles.size
        val cols = tiles[0].size
        val visited = mutableSetOf(spawn)
        val cells = mutableListOf(spawn)
        var current = spawn
        while (current != goal) {
            val (c, r) = current
            val nexts = listOf(c + 1 to r, c - 1 to r, c to r + 1, c to r - 1)
                .filter { (nc, nr) -> nc in 0 until cols && nr in 0 until rows }
                .filter { (nc, nr) -> tiles[nr][nc] == TileType.PATH || tiles[nr][nc] == TileType.GOAL }
                .filter { it !in visited }
            require(nexts.size == 1) {
                "map $id: path at $current has ${nexts.size} continuations (must be exactly 1)"
            }
            current = nexts.single()
            visited += current
            cells += current
        }
        return cells.map { (c, r) -> Vec2(c + 0.5f, r + 0.5f) }
    }
}
