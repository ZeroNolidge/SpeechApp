package com.zeronolidge.speechapp.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.logic.IslandStory
import com.zeronolidge.speechapp.logic.Land

/**
 * The island party's paper bunting: eight flags on a string, one per place, across the top of
 * the map under the corner buttons. Only to look at (never tappable, clear of every label). An
 * empty flag is a cream outline with its place's picture faint; a fixed place's flag is filled
 * in that place's own colour with its picture bold. No green and no checks (those mean "right"
 * and "done" elsewhere). [flagHeight] is the flags' drop (24-28 dp on a phone).
 */
@Composable
fun PartyBunting(filled: Set<Land>, modifier: Modifier = Modifier, flagHeight: Dp = 26.dp) {
    val places = IslandStory.places
    val sag = flagHeight * 0.22f
    BoxWithConstraints(modifier.fillMaxWidth().height(flagHeight + sag + 4.dp)) {
        val n = places.size
        val slot = maxWidth / n
        val fw = minOf(slot * 0.78f, flagHeight * 1.25f)
        val sagPx = sag
        Canvas(Modifier.fillMaxSize()) {
            val sg = sagPx.toPx()
            val top = 2.dp.toPx()
            fun cordY(x: Float): Float { val f = x / size.width; return top + 4f * sg * f * (1f - f) }
            // The string, sagging gently between its two ends.
            val cord = Path().apply { moveTo(0f, top); quadraticTo(size.width / 2, top + 2f * sg, size.width, top) }
            drawPath(cord, Color(0xFFFFF3DC), style = Stroke(2.5.dp.toPx()))
            drawPath(cord, Color(0xFFB8A27A).copy(alpha = 0.5f), style = Stroke(1.dp.toPx()))
            val fwp = fw.toPx(); val fh = flagHeight.toPx()
            places.forEachIndexed { i, pl ->
                val cx = slot.toPx() * (i + 0.5f)
                val y0 = cordY(cx)
                val on = pl.land in filled
                // A pennant: straight sides, then a point (room for the picture in its wide part).
                val flag = Path().apply {
                    moveTo(cx - fwp / 2, y0); lineTo(cx + fwp / 2, y0)
                    lineTo(cx + fwp / 2, y0 + fh * 0.55f); lineTo(cx, y0 + fh); lineTo(cx - fwp / 2, y0 + fh * 0.55f); close()
                }
                if (on) {
                    paperPath(flag, Color(pl.flag))
                    drawPath(flag, Color.White.copy(alpha = 0.85f), style = Stroke(1.6.dp.toPx()))
                } else {
                    drawPath(flag, Color(0xFFFFF8E7).copy(alpha = 0.28f))
                    drawPath(flag, Color(0xFFFFF3DC), style = Stroke(2.dp.toPx()))
                }
            }
        }
        places.forEachIndexed { i, pl ->
            val on = pl.land in filled
            val cx = slot * (i + 0.5f)
            val f = (cx / maxWidth)
            val y0 = 2.dp + sag * 4f * f * (1f - f)
            Box(
                Modifier.offset(x = cx - fw / 2, y = y0 + flagHeight * 0.06f).size(fw, flagHeight * 0.55f)
                    .graphicsLayer { alpha = if (on) 1f else 0.38f },
                contentAlignment = Alignment.Center,
            ) {
                Text(pl.land.emoji, fontSize = (flagHeight.value * (if (on) 0.58f else 0.5f)).sp, textAlign = TextAlign.Center)
            }
        }
    }
}
