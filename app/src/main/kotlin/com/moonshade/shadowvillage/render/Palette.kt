package com.moonshade.shadowvillage.render

import com.moonshade.shadowvillage.core.data.Element

/** The game's cohesive cartoon look: every sprite pulls from these. */
object Palette {

    const val OUTLINE = 0xFF2B2B3A.toInt()

    // Element triples: base / light / dark
    data class ElementColors(val base: Int, val light: Int, val dark: Int)

    val elements: Map<Element, ElementColors> = mapOf(
        Element.FIRE to ElementColors(0xFFFF6B35.toInt(), 0xFFFF9E6B.toInt(), 0xFFC94A1D.toInt()),
        Element.WATER to ElementColors(0xFF3FA7D6.toInt(), 0xFF7CC6E8.toInt(), 0xFF2A7BA3.toInt()),
        Element.LIGHTNING to ElementColors(0xFFFFD23F.toInt(), 0xFFFFE68A.toInt(), 0xFFD4A50F.toInt()),
        Element.WIND to ElementColors(0xFF9BE564.toInt(), 0xFFC3F0A0.toInt(), 0xFF6FB23E.toInt()),
        Element.EARTH to ElementColors(0xFFA0785A.toInt(), 0xFFC4A284.toInt(), 0xFF77543B.toInt()),
    )

    fun of(element: Element): ElementColors = elements.getValue(element)

    // Characters
    const val SKIN = 0xFFF2C9A0.toInt()
    const val NINJA_SUIT = 0xFF3D4460.toInt()
    const val NINJA_SUIT_DARK = 0xFF2E3349.toInt()
    const val EYE_WHITE = 0xFFFFFFFF.toInt()
    const val EYE_DARK = 0xFF22232E.toInt()

    // Map
    const val GRASS = 0xFF6FA35C.toInt()
    const val GRASS_LIGHT = 0xFF7FB36B.toInt()
    const val GRASS_TUFT = 0xFF5C8F4A.toInt()
    const val PATH_DIRT = 0xFFC9A86A.toInt()
    const val PATH_DIRT_DARK = 0xFFB09053.toInt()
    const val PATH_EDGE = 0xFF9A7B45.toInt()
    const val ROCK = 0xFF8B8E98.toInt()
    const val ROCK_DARK = 0xFF6E7079.toInt()
    const val SPAWN_GATE = 0xFF7A4E8C.toInt()
    const val GOAL_GATE = 0xFFB8413C.toInt()

    // UI
    const val HUD_BG = 0xCC1B2238.toInt()
    const val HUD_TEXT = 0xFFF4EFE6.toInt()
    const val GOLD = 0xFFFFC93C.toInt()
    const val LIFE = 0xFFE85D5D.toInt()
    const val BUTTON = 0xFF3D4460.toInt()
    const val BUTTON_ACTIVE = 0xFF5A6390.toInt()
    const val BUTTON_DISABLED = 0xFF2A2E40.toInt()
    const val PANEL_BG = 0xE61B2238.toInt()
    const val RANGE_FILL = 0x303FA7D6
    const val RANGE_STROKE = 0x903FA7D6.toInt()
    const val VALID_CELL = 0x5064DD64
    const val SELL = 0xFFE85D5D.toInt()
    const val OK = 0xFF7ED957.toInt()

    const val HP_BAR_BG = 0xFF522222.toInt()
    const val HP_BAR = 0xFF6FE26B.toInt()
}
