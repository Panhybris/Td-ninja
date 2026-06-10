package com.moonshade.shadowvillage.core.map

object Maps {

    /** Beginner map: one gentle S-curve with build space everywhere. */
    val riverCrossing = MapParser.parse(
        id = "river_crossing",
        name = "River Crossing",
        ascii = listOf(
            "..X.........X...",
            "S#####..........",
            ".....#..........",
            ".....#...######.",
            ".....#...#....#.",
            ".....#####....#.",
            "..............#.",
            "..............#G",
            "......X........X",
        ),
    )

    /** Harder map: long switchback with a tight center corridor. */
    val twinGates = MapParser.parse(
        id = "twin_gates",
        name = "Twin Gates",
        ascii = listOf(
            ".X..........X...",
            "............###G",
            "............#...",
            "...##########...",
            "...#....XX......",
            "...##########...",
            "............#...",
            "S############...",
            "...X............",
        ),
    )

    val all = listOf(riverCrossing, twinGates)

    fun byId(id: String): GameMap = all.first { it.id == id }
}
