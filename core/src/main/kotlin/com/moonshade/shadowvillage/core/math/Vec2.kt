package com.moonshade.shadowvillage.core.math

import kotlin.math.sqrt

data class Vec2(val x: Float, val y: Float) {
    operator fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)
    operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)
    operator fun times(s: Float) = Vec2(x * s, y * s)

    fun length(): Float = sqrt(x * x + y * y)

    fun distanceTo(o: Vec2): Float {
        val dx = x - o.x
        val dy = y - o.y
        return sqrt(dx * dx + dy * dy)
    }

    fun normalized(): Vec2 {
        val len = length()
        return if (len < 1e-6f) Vec2(0f, 0f) else Vec2(x / len, y / len)
    }

    companion object {
        val ZERO = Vec2(0f, 0f)
        fun lerp(a: Vec2, b: Vec2, t: Float) = Vec2(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
    }
}
