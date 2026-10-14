package com.zeronolidge.speechapp.ui.components

import com.zeronolidge.speechapp.ui.art.paperCard
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Children who can't read yet need to *see* what to tap. These helpers make the
 * next thing to press pulse, point at it with a bouncing hand, and say so out loud.
 */

/** A big green "go" arrow on raised paper that gently pulses, with a hand pointing at it. */
@Composable
fun GoButton(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    enabled: Boolean = true,
    /** Pulse and point at it. Off when it's only a "skip", so it doesn't pull attention. */
    hint: Boolean = true,
    onClick: () -> Unit,
) {
    val t = rememberInfiniteTransition(label = "go")
    val pulse by t.animateFloat(1f, 1.1f, infiniteRepeatable(tween(550), RepeatMode.Reverse), label = "pulse")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        PaperButton(
            PaperColorsUi.go,
            Modifier.size(size).scale(if (enabled && hint) pulse else 1f),
            circle = true,
            enabled = enabled,
            onClick = onClick,
        ) {
            Text("▶", fontSize = (size.value * 0.45f).sp, color = Color.White, modifier = Modifier.offset(x = size * 0.04f))
        }
        if (enabled && hint) PointingHand(Modifier.offset(y = 4.dp), size = size * 0.5f)
    }
}

/** A hand that bobs up and down, pointing at whatever is above it. */
@Composable
fun PointingHand(modifier: Modifier = Modifier, size: Dp = 48.dp, pointDown: Boolean = false) {
    val t = rememberInfiniteTransition(label = "hand")
    val bob by t.animateFloat(0f, 1f, infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "bob")
    val shift = size * 0.25f * bob
    Text(
        if (pointDown) "👇" else "👆",
        fontSize = size.value.sp,
        modifier = modifier.offset(y = if (pointDown) shift else -shift),
    )
}

/**
 * Says [text] again every [everyMs] while the screen is showing, if nothing else is being said,
 * up to [times] times. Gives a gentle reminder when a child isn't sure what to do.
 */
@Composable
fun Nudge(key: Any, text: String, everyMs: Long = 9000, times: Int = 3) {
    val app = LocalApp.current
    LaunchedEffect(key) {
        repeat(times) {
            delay(everyMs)
            if (!app.speaker.speaking) app.speaker.say(text)
        }
    }
}
