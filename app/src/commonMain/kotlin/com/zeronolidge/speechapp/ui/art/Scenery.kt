package com.zeronolidge.speechapp.ui.art

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.logic.Land
import com.zeronolidge.speechapp.ui.components.popUp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Each area has its own few bold paper colours. Tomato red is never used here: it's kept for
 * things he can tap.
 */
data class LandPalette(
    val sky: Color,
    /** The lighter band near the horizon. */
    val glow: Color,
    val far: Color,
    val mid: Color,
    val near: Color,
    val ground: Color,
    /** The area's warm or bright highlight colour. */
    val accent: Color,
    val accent2: Color,
)

val Land.palette: LandPalette
    get() = when (this) {
        // Deep greens and mustard.
        Land.FOREST -> LandPalette(
            Color(0xFFD4ECD9), Color(0xFFF2F6DC), Color(0xFF2C6B4E), Color(0xFF3F8C58), Color(0xFF5BAA5E),
            Color(0xFF79B957), Color(0xFFE2A72E), Color(0xFFF6D57A),
        )
        // Sunflower yellow, leaf green and lilac.
        Land.MEADOW -> LandPalette(
            Color(0xFFFFF1C2), Color(0xFFFFF9E3), Color(0xFF8DBF45), Color(0xFF6AA63B), Color(0xFF9CCB4F),
            Color(0xFFB9D86C), Color(0xFFF7C11E), Color(0xFFB48EDB),
        )
        // Coral and teal.
        Land.LAGOON -> LandPalette(
            Color(0xFFC4F0EE), Color(0xFFEAFBF8), Color(0xFF1C9C9E), Color(0xFF14797F), Color(0xFFF2C893),
            Color(0xFFF7DDB1), Color(0xFFFF9C86), Color(0xFF5FD6CF),
        )
        // Terracotta roofs, wood, plum and mustard.
        Land.MARKET -> LandPalette(
            Color(0xFFFFE6CC), Color(0xFFFFF3E3), Color(0xFFC9773A), Color(0xFF8C5A3A), Color(0xFFB98A5E),
            Color(0xFFE6C38E), Color(0xFF7D4C96), Color(0xFFF2B630),
        )
        // Periwinkle, indigo and snow, with a mustard sun.
        Land.PEAKS -> LandPalette(
            Color(0xFFDDE3F8), Color(0xFFF1F3FC), Color(0xFF7C79C4), Color(0xFF55529A), Color(0xFF8C93C8),
            Color(0xFFA9B1D6), Color(0xFFF4C542), Color(0xFFFFFFFF),
        )
        // Slate blues with teal and yellow.
        Land.FACTORY -> LandPalette(
            Color(0xFFDDE8EC), Color(0xFFF1F6F7), Color(0xFF5F7C8A), Color(0xFF41606E), Color(0xFF7C97A3),
            Color(0xFF9DB2BB), Color(0xFF22A699), Color(0xFFFFC94A),
        )
        // Deep purple rock and glowing cyan crystals.
        Land.CAVES -> LandPalette(
            Color(0xFF3A3463), Color(0xFF4C4580), Color(0xFF5A4E8E), Color(0xFF2E2950), Color(0xFF4A3F72),
            Color(0xFF6B5848), Color(0xFF6FE3F0), Color(0xFFFFD166),
        )
        // Lavender, candy pink, white clouds and gold.
        Land.CLOUDS -> LandPalette(
            Color(0xFFE6DAFA), Color(0xFFF6F0FF), Color(0xFFFFFFFF), Color(0xFFF2B8D8), Color(0xFFFBE3F0),
            Color(0xFFFFFFFF), Color(0xFFFFC94A), Color(0xFF9B7FE0),
        )
    }

/**
 * A land's backdrop, built like a pop-up book from stacked paper layers: sky, drifting clouds,
 * far hills, near hills, then the ground. Each layer is baked once with its own soft shadow
 * (deeper the nearer it is), white cut edges and paper fibres. When the scene changes ([seed])
 * the layers stand up one after another, back to front, in under half a second.
 */
@Composable
fun Scenery(land: Land, modifier: Modifier = Modifier, seed: Int = 0, content: @Composable BoxScope.() -> Unit = {}) {
    val t = rememberInfiniteTransition(label = "sky")
    val drift by t.loop(0f, 1f, 60000, mode = RepeatMode.Restart, easing = LinearEasing, label = "drift")
    val pal = land.palette
    val scene = land to seed
    val decor = remember(land, seed) {
        val r = Random(seed * 31 + land.ordinal)
        List(4) { i -> Triple(land.decor.random(r), 0.04f + i * 0.25f + r.nextFloat() * 0.08f, r.nextFloat()) }
    }
    BoxWithConstraints(modifier) {
        val w = maxWidth
        val h = maxHeight
        BakedLayer(listOf("sky", land), depth = 0.dp, edge = false) { sky(land, pal) }
        if (land != Land.CAVES) {
            // One baked strip of clouds, slid along and wrapped around.
            listOf(0f, -1f).forEach { k ->
                BakedLayer(
                    listOf("clouds", land),
                    Modifier.graphicsLayer { translationX = (drift + k) * size.width },
                    depth = 1.5.dp, grain = false,
                ) { cloudStrip(pal) }
            }
        }
        BakedLayer(listOf("far", land, seed), Modifier.popUp(scene, 0), depth = 2.dp) { far(land, pal, seed) }
        BakedLayer(listOf("near", land, seed), Modifier.popUp(scene, 70), depth = 3.dp) { near(land, pal, seed) }
        val fs = (h.value * 0.11f).coerceIn(18f, 40f)
        Box(Modifier.fillMaxSize().popUp(scene, 110)) {
            decor.forEach { (emoji, x, y) ->
                Text(emoji, fontSize = fs.sp, modifier = Modifier.offset(x = w * x, y = h * (0.55f + y * 0.08f)))
            }
        }
        BakedLayer(listOf("ground", land), Modifier.popUp(scene, 140), depth = 4.dp) { ground(land, pal) }
        // A light wash keeps the scenery behind the raised paper buttons, so they always win.
        Canvas(Modifier.fillMaxSize()) { drawRect(PaperStyle.sceneryWash) }
        Box(Modifier.fillMaxSize(), content = content)
    }
}

// ---------------------------------------------------------------- layers

private fun DrawScope.sky(land: Land, pal: LandPalette) {
    drawRect(pal.sky)
    paperRect(pal.glow, Offset(-20f, size.height * 0.36f), Size(size.width + 40f, size.height * 0.4f))
    val u = size.minDimension
    when (land) {
        Land.CAVES -> {
            // A glowing hole in the cave roof instead of a sun.
            paperCircle(lerp(pal.sky, pal.accent, 0.25f), u * 0.2f, Offset(size.width * 0.8f, size.height * 0.1f))
        }
        Land.CLOUDS -> {
            paperCircle(pal.accent, u * 0.08f, Offset(size.width * 0.84f, size.height * 0.16f))
            listOf(0.12f to 0.1f, 0.3f to 0.22f, 0.62f to 0.08f, 0.72f to 0.3f).forEach { (x, y) ->
                star(Offset(size.width * x, size.height * y), u * 0.025f, pal.accent)
            }
        }
        else -> {
            val sun = Offset(size.width * 0.85f, size.height * 0.15f)
            paperCircle(lerp(pal.accent, Color.White, 0.55f), u * 0.13f, sun)
            paperCircle(lerp(pal.accent, Color.White, 0.2f), u * 0.085f, sun)
        }
    }
}

private fun DrawScope.cloudStrip(pal: LandPalette) {
    val u = size.minDimension
    val white = lerp(Color.White, pal.sky, 0.08f)
    listOf(0.1f to 0.12f, 0.42f to 0.22f, 0.74f to 0.1f).forEach { (x, y) ->
        cloud(Offset(size.width * x, size.height * y), u * 0.055f, white)
    }
}

private fun DrawScope.far(land: Land, pal: LandPalette, seed: Int) {
    val r = Random(seed * 17 + land.ordinal * 5)
    val w = size.width
    val h = size.height
    val u = size.minDimension
    when (land) {
        Land.FOREST -> {
            hill(pal.far, -0.1f, 0.5f, 0.75f, 0.42f)
            hill(pal.far, 0.45f, 0.48f, 0.7f, 0.45f)
            var x = -0.02f
            while (x < 1.04f) {
                val tall = 0.16f + r.nextFloat() * 0.12f
                pineTree(Offset(w * x, h * 0.6f), u * tall, if (r.nextBoolean()) pal.far else pal.mid)
                x += 0.07f + r.nextFloat() * 0.06f
            }
        }
        Land.MEADOW -> {
            hill(pal.far, -0.15f, 0.5f, 0.8f, 0.4f)
            hill(pal.mid, 0.4f, 0.53f, 0.8f, 0.4f)
            repeat(18) {
                val c = Offset(w * r.nextFloat(), h * (0.56f + r.nextFloat() * 0.12f))
                paperCircle(if (it % 2 == 0) pal.accent else pal.accent2, u * 0.012f, c)
            }
        }
        Land.LAGOON -> {
            paperRect(pal.far, Offset(-10f, h * 0.5f), Size(w + 20f, h * 0.3f))
            // A little island far out to sea, with a palm.
            paperOval(pal.near, Offset(w * 0.12f, h * 0.48f), Size(w * 0.22f, h * 0.06f))
            palmTree(Offset(w * 0.21f, h * 0.5f), u * 0.16f, pal.mid)
            repeat(7) { i ->
                val x = w * (0.08f + i * 0.14f + r.nextFloat() * 0.04f)
                paperArc(pal.accent2, 180f, 180f, false, Offset(x, h * (0.56f + (i % 3) * 0.05f)), Size(u * 0.06f, u * 0.025f))
            }
        }
        Land.MARKET -> {
            var x = -0.03f
            var i = 0
            while (x < 1.02f) {
                val bw = 0.09f + r.nextFloat() * 0.05f
                val bh = 0.12f + r.nextFloat() * 0.1f
                house(Offset(w * x, h * 0.62f), w * bw, h * bh, if (i % 2 == 0) pal.far else lerp(pal.far, pal.mid, 0.4f), pal.mid, pal.accent2)
                x += bw + 0.01f
                i++
            }
        }
        Land.PEAKS -> {
            var x = -0.1f
            while (x < 1.1f) {
                val tall = 0.25f + r.nextFloat() * 0.18f
                val wide = 0.14f + r.nextFloat() * 0.08f
                snowPeak(Offset(w * x, h * 0.64f), w * wide, h * tall, if (r.nextBoolean()) pal.far else pal.mid)
                x += wide * 1.1f
            }
        }
        Land.FACTORY -> {
            var x = -0.02f
            while (x < 1.02f) {
                val bw = 0.1f + r.nextFloat() * 0.06f
                val bh = 0.14f + r.nextFloat() * 0.14f
                paperRect(pal.far, Offset(w * x, h * (0.62f - bh)), Size(w * bw, h * bh + 4f))
                if (r.nextFloat() < 0.5f) paperRect(pal.mid, Offset(w * (x + bw * 0.6f), h * (0.62f - bh - 0.1f)), Size(w * bw * 0.22f, h * 0.12f))
                repeat(2) { k -> paperRect(pal.accent2, Offset(w * (x + 0.02f + k * bw * 0.45f), h * (0.62f - bh + 0.04f)), Size(w * 0.025f, h * 0.04f)) }
                x += bw + 0.02f
            }
            gear(Offset(w * 0.3f, h * 0.36f), u * 0.08f, pal.mid)
            gear(Offset(w * 0.68f, h * 0.3f), u * 0.06f, pal.accent)
        }
        Land.CAVES -> {
            // Rock arches far back and stalactites hanging from the roof.
            hill(pal.far, -0.1f, 0.42f, 0.6f, 0.5f)
            hill(pal.far, 0.5f, 0.45f, 0.7f, 0.5f)
            val roof = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                val n = 12
                for (i in n downTo 0) {
                    lineTo(w * i / n, h * (if (i % 2 == 0) 0.05f else 0.14f + r.nextFloat() * 0.12f))
                }
                close()
            }
            paperPath(roof, pal.mid)
            repeat(5) {
                crystal(Offset(w * (0.1f + it * 0.2f + r.nextFloat() * 0.05f), h * (0.5f + r.nextFloat() * 0.08f)), u * 0.05f, pal.accent)
            }
        }
        Land.CLOUDS -> {
            castleSilhouette(Offset(w * 0.7f, h * 0.55f), u * 0.28f, pal.mid, pal.accent2)
            repeat(5) { i -> cloud(Offset(w * (0.05f + i * 0.24f), h * 0.6f), u * 0.09f, pal.far) }
        }
    }
}

private fun DrawScope.near(land: Land, pal: LandPalette, seed: Int) {
    val r = Random(seed * 13 + land.ordinal * 7 + 1)
    val w = size.width
    val h = size.height
    val u = size.minDimension
    when (land) {
        Land.CLOUDS -> repeat(6) { i -> cloud(Offset(w * (i / 5f), h * 0.74f), u * 0.11f, pal.near) }
        Land.LAGOON -> {
            hill(pal.near, -0.2f, 0.66f, 0.9f, 0.3f)
            hill(lerp(pal.near, pal.ground, 0.4f), 0.5f, 0.68f, 0.8f, 0.3f)
            repeat(4) {
                val c = Offset(w * (0.1f + it * 0.25f + r.nextFloat() * 0.05f), h * (0.74f + r.nextFloat() * 0.03f))
                shell(c, u * 0.025f, pal.accent)
            }
        }
        else -> {
            hill(pal.near, -0.2f, 0.62f, 0.85f, 0.34f)
            hill(lerp(pal.near, pal.mid, 0.25f), 0.48f, 0.65f, 0.75f, 0.34f)
            when (land) {
                Land.FOREST -> repeat(5) {
                    val c = Offset(w * (0.05f + it * 0.22f + r.nextFloat() * 0.06f), h * (0.7f + r.nextFloat() * 0.03f))
                    bush(c, u * 0.04f, pal.mid, pal.near)
                    if (it % 2 == 1) mushroom(Offset(c.x + u * 0.05f, c.y + u * 0.02f), u * 0.02f, pal.accent)
                }
                Land.MEADOW -> repeat(14) {
                    val c = Offset(w * r.nextFloat(), h * (0.68f + r.nextFloat() * 0.05f))
                    flower(c, u * 0.016f, if (it % 3 == 0) pal.accent2 else pal.accent)
                }
                Land.MARKET -> {
                    stall(Offset(w * (0.12f + r.nextFloat() * 0.1f), h * 0.7f), u * 0.11f, pal)
                    stall(Offset(w * (0.68f + r.nextFloat() * 0.1f), h * 0.69f), u * 0.1f, pal)
                }
                Land.PEAKS -> repeat(4) {
                    val c = Offset(w * (0.08f + it * 0.28f), h * 0.68f)
                    paperOval(Color.White, Offset(c.x - u * 0.05f, c.y - u * 0.012f), Size(u * 0.1f, u * 0.024f))
                }
                Land.FACTORY -> {
                    // A conveyor belt with parcels.
                    paperRoundRect(pal.far, Offset(w * 0.05f, h * 0.68f), Size(w * 0.9f, h * 0.03f), CornerRadius(h * 0.015f))
                    repeat(5) { i ->
                        paperRect(if (i % 2 == 0) pal.accent2 else pal.accent, Offset(w * (0.12f + i * 0.17f), h * 0.62f), Size(u * 0.06f, u * 0.055f))
                    }
                }
                Land.CAVES -> repeat(4) {
                    crystal(Offset(w * (0.12f + it * 0.25f + r.nextFloat() * 0.05f), h * 0.7f), u * 0.07f, pal.accent)
                }
                else -> {}
            }
        }
    }
}

private fun DrawScope.ground(land: Land, pal: LandPalette) {
    val w = size.width
    val h = size.height
    if (land == Land.CLOUDS) {
        repeat(8) { i -> cloud(Offset(w * (i / 7f), h * 0.88f), size.minDimension * 0.1f, pal.ground) }
    } else {
        paperRect(pal.ground, Offset(-10f, h * 0.76f), Size(w + 20f, h * 0.3f))
    }
    // A path for the buddy to walk along.
    val path = if (land == Land.CAVES) lerp(pal.ground, Color.White, 0.25f) else lerp(pal.ground, Color(0xFFFFF3E0), 0.55f)
    paperOval(path, Offset(-w * 0.1f, h * 0.85f), Size(w * 1.2f, h * 0.11f))
}

// ---------------------------------------------------------------- shapes

/** A rounded hill: an oval starting at [x] (fraction of width), top at [top], [wide] and [tall] as fractions. */
private fun DrawScope.hill(color: Color, x: Float, top: Float, wide: Float, tall: Float) =
    paperOval(color, Offset(size.width * x, size.height * top), Size(size.width * wide, size.height * tall * 2))

private fun DrawScope.cloud(c: Offset, r: Float, color: Color) {
    paperCircle(color, r, c)
    paperCircle(color, r * 0.8f, Offset(c.x - r * 1.0f, c.y + r * 0.25f))
    paperCircle(color, r * 0.85f, Offset(c.x + r * 1.0f, c.y + r * 0.2f))
    paperCircle(color, r * 0.7f, Offset(c.x + r * 0.4f, c.y - r * 0.5f))
}

private fun DrawScope.pineTree(base: Offset, s: Float, color: Color) {
    for (k in 0..2) {
        val top = base.y - s + k * s * 0.28f
        val half = s * (0.18f + k * 0.08f)
        val tri = Path().apply {
            moveTo(base.x, top)
            lineTo(base.x + half, top + s * 0.42f)
            lineTo(base.x - half, top + s * 0.42f)
            close()
        }
        paperPath(tri, color)
    }
}

private fun DrawScope.palmTree(base: Offset, s: Float, color: Color) {
    val trunk = Path().apply {
        moveTo(base.x - s * 0.04f, base.y)
        quadraticTo(base.x + s * 0.1f, base.y - s * 0.5f, base.x + s * 0.06f, base.y - s)
        lineTo(base.x + s * 0.12f, base.y - s)
        quadraticTo(base.x + s * 0.16f, base.y - s * 0.5f, base.x + s * 0.04f, base.y)
        close()
    }
    paperPath(trunk, lerp(color, Color(0xFF8D6E63), 0.6f))
    val top = Offset(base.x + s * 0.09f, base.y - s)
    listOf(-160f, -120f, -60f, -20f).forEach { deg ->
        val a = deg * PI / 180
        val end = Offset(top.x + (s * 0.5f * cos(a)).toFloat(), top.y + (s * 0.5f * sin(a)).toFloat() + s * 0.18f)
        val leaf = Path().apply {
            moveTo(top.x, top.y)
            quadraticTo((top.x + end.x) / 2, top.y - s * 0.22f, end.x, end.y)
            quadraticTo((top.x + end.x) / 2, top.y - s * 0.05f, top.x, top.y)
            close()
        }
        paperPath(leaf, color)
    }
}

private fun DrawScope.house(base: Offset, bw: Float, bh: Float, wall: Color, roof: Color, window: Color) {
    paperRect(wall, Offset(base.x, base.y - bh), Size(bw, bh + 4f))
    val tri = Path().apply {
        moveTo(base.x - bw * 0.08f, base.y - bh)
        lineTo(base.x + bw / 2, base.y - bh - bw * 0.5f)
        lineTo(base.x + bw * 1.08f, base.y - bh)
        close()
    }
    paperPath(tri, roof)
    paperRoundRect(window, Offset(base.x + bw * 0.3f, base.y - bh * 0.7f), Size(bw * 0.4f, bh * 0.3f), CornerRadius(bw * 0.2f))
}

private fun DrawScope.snowPeak(base: Offset, wide: Float, tall: Float, color: Color) {
    val m = Path().apply {
        moveTo(base.x - wide, base.y)
        lineTo(base.x, base.y - tall)
        lineTo(base.x + wide, base.y)
        close()
    }
    paperPath(m, color)
    val cap = Path().apply {
        moveTo(base.x, base.y - tall)
        lineTo(base.x + wide * 0.3f, base.y - tall * 0.7f)
        lineTo(base.x + wide * 0.1f, base.y - tall * 0.76f)
        lineTo(base.x - wide * 0.06f, base.y - tall * 0.66f)
        lineTo(base.x - wide * 0.3f, base.y - tall * 0.7f)
        close()
    }
    paperPath(cap, Color.White)
}

private fun DrawScope.gear(c: Offset, r: Float, color: Color) {
    val teeth = 8
    val p = Path()
    for (i in 0 until teeth * 2) {
        val a = i * PI / teeth
        val rr = if (i % 2 == 0) r else r * 0.8f
        val a2 = a + PI / teeth
        val p1 = Offset(c.x + (rr * cos(a)).toFloat(), c.y + (rr * sin(a)).toFloat())
        val p2 = Offset(c.x + (rr * cos(a2)).toFloat(), c.y + (rr * sin(a2)).toFloat())
        if (i == 0) p.moveTo(p1.x, p1.y) else p.lineTo(p1.x, p1.y)
        p.lineTo(p2.x, p2.y)
    }
    p.close()
    paperPath(p, color)
    paperCircle(lerp(color, Color.White, 0.5f), r * 0.32f, c)
}

private fun DrawScope.crystal(base: Offset, s: Float, color: Color) {
    listOf(-0.35f to 0.7f, 0f to 1f, 0.35f to 0.6f).forEachIndexed { i, (dx, tall) ->
        val x = base.x + dx * s
        val p = Path().apply {
            moveTo(x - s * 0.13f, base.y)
            lineTo(x - s * 0.13f, base.y - s * tall * 0.7f)
            lineTo(x, base.y - s * tall)
            lineTo(x + s * 0.13f, base.y - s * tall * 0.7f)
            lineTo(x + s * 0.13f, base.y)
            close()
        }
        paperPath(p, if (i == 1) color else lerp(color, Color.White, 0.35f))
    }
}

private fun DrawScope.castleSilhouette(base: Offset, s: Float, color: Color, roof: Color) {
    paperRect(color, Offset(base.x - s * 0.4f, base.y - s * 0.5f), Size(s * 0.8f, s * 0.5f))
    listOf(-0.45f, 0f, 0.45f).forEachIndexed { i, dx ->
        val tall = if (i == 1) 0.85f else 0.65f
        val x = base.x + dx * s
        paperRect(color, Offset(x - s * 0.1f, base.y - s * tall), Size(s * 0.2f, s * tall))
        val tri = Path().apply {
            moveTo(x - s * 0.14f, base.y - s * tall)
            lineTo(x, base.y - s * tall - s * 0.25f)
            lineTo(x + s * 0.14f, base.y - s * tall)
            close()
        }
        paperPath(tri, roof)
    }
}

private fun DrawScope.bush(c: Offset, r: Float, dark: Color, light: Color) {
    paperCircle(dark, r, c)
    paperCircle(dark, r * 0.8f, Offset(c.x - r * 0.9f, c.y + r * 0.2f))
    paperCircle(lerp(light, Color.White, 0.15f), r * 0.55f, Offset(c.x + r * 0.2f, c.y - r * 0.3f))
}

private fun DrawScope.mushroom(c: Offset, r: Float, cap: Color) {
    paperRect(Color(0xFFFFF3E0), Offset(c.x - r * 0.3f, c.y - r * 0.2f), Size(r * 0.6f, r))
    paperArc(cap, 180f, 180f, true, Offset(c.x - r, c.y - r * 0.9f), Size(r * 2, r * 1.6f))
    paperCircle(Color.White, r * 0.18f, Offset(c.x - r * 0.35f, c.y - r * 0.4f))
}

private fun DrawScope.flower(c: Offset, r: Float, petal: Color) {
    repeat(5) { k ->
        val a = k * 2 * PI / 5
        paperCircle(petal, r * 0.6f, Offset(c.x + (r * 0.7f * cos(a)).toFloat(), c.y + (r * 0.7f * sin(a)).toFloat()))
    }
    paperCircle(Color(0xFFFFF3C4), r * 0.45f, c)
}

private fun DrawScope.shell(c: Offset, r: Float, color: Color) {
    paperArc(color, 180f, 180f, true, Offset(c.x - r, c.y - r), Size(r * 2, r * 2))
    paperRect(lerp(color, Color.White, 0.3f), Offset(c.x - r * 0.3f, c.y - r * 0.1f), Size(r * 0.6f, r * 0.3f))
}

private fun DrawScope.stall(base: Offset, s: Float, pal: LandPalette) {
    paperRect(pal.mid, Offset(base.x - s * 0.6f, base.y - s * 0.8f), Size(s * 0.08f, s * 0.8f))
    paperRect(pal.mid, Offset(base.x + s * 0.52f, base.y - s * 0.8f), Size(s * 0.08f, s * 0.8f))
    paperRect(lerp(pal.mid, pal.ground, 0.5f), Offset(base.x - s * 0.65f, base.y - s * 0.3f), Size(s * 1.3f, s * 0.3f))
    for (i in 0 until 5) {
        val col = if (i % 2 == 0) pal.accent else pal.accent2
        val left = base.x - s * 0.7f + i * s * 0.28f
        paperRect(col, Offset(left, base.y - s * 1.05f), Size(s * 0.28f, s * 0.25f))
        paperArc(col, 0f, 180f, true, Offset(left, base.y - s * 0.92f), Size(s * 0.28f, s * 0.2f))
    }
}

/** Small round badge of a land, for the world map. */
@Composable
fun LandBadge(land: Land, modifier: Modifier = Modifier) {
    Scenery(land, modifier)
}
