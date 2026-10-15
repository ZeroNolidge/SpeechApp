package com.zeronolidge.speechapp.ui.art

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf

/**
 * Motion rules: scene changes, pop-ups and peels are short (under half a second), and when the
 * device asks for less motion ("Reduce motion" on iPad, "Remove animations" on Android) nothing
 * moves on its own at all.
 */
val LocalReduceMotion = compositionLocalOf { false }

/** The longest a one-off animation may take. */
const val MaxMotionMs = 450

/** How long a one-off animation should take: [ms] capped at [MaxMotionMs], or 0 with reduce motion. */
@Composable
fun motionMs(ms: Int): Int = if (LocalReduceMotion.current) 0 else minOf(ms, MaxMotionMs)

/**
 * A gentle looping movement (bobbing, drifting, pulsing) that stays still at [from] when the
 * device asks for less motion.
 */
@Composable
fun InfiniteTransition.loop(
    from: Float,
    to: Float,
    ms: Int,
    mode: RepeatMode = RepeatMode.Reverse,
    easing: Easing = FastOutSlowInEasing,
    label: String = "loop",
): State<Float> {
    val still = LocalReduceMotion.current
    return animateFloat(from, if (still) from else to, infiniteRepeatable(tween(ms, easing = easing), mode), label = label)
}
