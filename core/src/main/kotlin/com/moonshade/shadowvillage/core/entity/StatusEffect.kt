package com.moonshade.shadowvillage.core.entity

sealed class StatusEffect {
    abstract var remaining: Float

    class Burn(val dps: Float, override var remaining: Float) : StatusEffect()

    /** Non-stacking; the strongest factor wins. 0.3 = 30% slower. */
    class Slow(val factor: Float, override var remaining: Float) : StatusEffect()

    /** Halts movement entirely. Durations max-merge; bosses are immune. */
    class Stun(override var remaining: Float) : StatusEffect()
}
