package com.zeronolidge.speechapp.ui.art

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.logic.Land
import kotlin.random.Random

/**
 * A painted backdrop for a land: sky, sun, drifting clouds, hills and a few props.
 * [seed] varies the props so every scene in a quest looks a little different.
 */
@Composable
fun Scenery(land: Land, modifier: Modifier = Modifier, seed: Int = 0, content: @Composable BoxScope.() -> Unit = {}) {
    val t = rememberInfiniteTransition(label = "sky")
    val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(30000, easing = LinearEasing), RepeatMode.Restart), label = "drift")
    val sky = Color(land.sky)
    val ground = Color(land.ground)
    val decor = remember(land, seed) {
        val r = Random(seed * 31 + land.ordinal)
        List(5) { i -> Triple(land.decor.random(r), 0.04f + i * 0.2f + r.nextFloat() * 0.08f, r.nextFloat()) }
    }
    BoxWithConstraints(modifier) {
        val w = maxWidth
        val h = maxHeight
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(listOf(lerp(sky, Color.White, 0.1f), lerp(sky, Color.White, 0.6f))))
            when (land) {
                Land.CAVES -> caveTop(this)
                else -> {
                    drawCircle(Color(0xFFFFEE58), size.minDimension * 0.09f, Offset(size.width * 0.85f, size.height * 0.16f))
                    drawCircle(Color(0x55FFEE58), size.minDimension * 0.14f, Offset(size.width * 0.85f, size.height * 0.16f))
                    listOf(0.15f to 0.12f, 0.5f to 0.2f, 0.8f to 0.33f).forEachIndexed { i, (x, y) ->
                        val cx = ((x + drift * (0.3f + i * 0.1f)) % 1.2f - 0.1f) * size.width
                        cloud(this, Offset(cx, size.height * y), size.minDimension * 0.06f)
                    }
                }
            }
            // Rolling hills.
            val back = lerp(ground, Color.White, 0.25f)
            drawOval(back, Offset(-size.width * 0.2f, size.height * 0.55f), Size(size.width * 0.8f, size.height * 0.6f))
            drawOval(back, Offset(size.width * 0.45f, size.height * 0.5f), Size(size.width * 0.9f, size.height * 0.7f))
            drawRect(ground, Offset(0f, size.height * 0.72f), Size(size.width, size.height * 0.28f))
            if (land == Land.LAGOON) {
                drawRect(Color(0xFF4FC3F7), Offset(0f, size.height * 0.62f), Size(size.width, size.height * 0.1f))
            }
            if (land == Land.CLOUDS) {
                (0..6).forEach { i -> cloud(this, Offset(size.width * i / 6f, size.height * 0.78f), size.minDimension * 0.1f) }
            }
            // A little path for the hero to walk along.
            drawOval(lerp(ground, Color(0xFFFFF3E0), 0.5f), Offset(-size.width * 0.1f, size.height * 0.84f), Size(size.width * 1.2f, size.height * 0.12f))
        }
        val fs = (h.value * 0.12f).coerceIn(18f, 44f)
        decor.forEach { (emoji, x, y) ->
            Text(
                emoji,
                fontSize = fs.sp,
                modifier = Modifier.offset(x = w * x, y = h * (0.52f + y * 0.12f)),
            )
        }
        Box(Modifier.fillMaxSize(), content = content)
    }
}

private fun cloud(d: DrawScope, c: Offset, r: Float) = with(d) {
    val white = Color.White.copy(alpha = 0.9f)
    drawCircle(white, r, c)
    drawCircle(white, r * 0.8f, Offset(c.x - r * 1.0f, c.y + r * 0.25f))
    drawCircle(white, r * 0.85f, Offset(c.x + r * 1.0f, c.y + r * 0.2f))
    drawCircle(white, r * 0.7f, Offset(c.x + r * 0.4f, c.y - r * 0.5f))
}

private fun caveTop(d: DrawScope) = with(d) {
    val rock = Color(0xFF4E342E)
    val p = Path().apply {
        moveTo(0f, 0f)
        lineTo(size.width, 0f)
        val n = 9
        for (i in n downTo 0) {
            val x = size.width * i / n
            lineTo(x, size.height * (if (i % 2 == 0) 0.08f else 0.2f + (i % 3) * 0.04f))
        }
        close()
    }
    drawPath(p, rock)
    // Twinkling crystals.
    listOf(0.2f to 0.35f, 0.6f to 0.28f, 0.75f to 0.45f).forEach { (x, y) ->
        star(Offset(size.width * x, size.height * y), size.minDimension * 0.025f, Color(0xFFB3E5FC))
    }
}

/** Small round badge of a land, for the world map. */
@Composable
fun LandBadge(land: Land, modifier: Modifier = Modifier) {
    Scenery(land, modifier)
}
