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
fun PartyBunting(
    filled: Set<Land>,
    modifier: Modifier = Modifier,
    flagHeight: Dp = 26.dp,
    /** The flag that just filled (the fix moment), popping in for [popT] 0 to 1. */
    pop: Land? = null,
    popT: Float = 1f,
) {
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
                    .graphicsLayer {
                        alpha = if (on) 1f else 0.38f
                        if (pl.land == pop && popT < 1f) { val k = 1f + 0.5f * kotlin.math.sin(popT * kotlin.math.PI).toFloat(); scaleX = k; scaleY = k }
                    },
                contentAlignment = Alignment.Center,
            ) {
                PlaceIcon(pl.land, flagHeight.value * (if (on) 0.58f else 0.5f))
            }
        }
    }
}

/**
 * A place's picture for its map label and its party flag, at about [sizeSp] (as an emoji of that
 * font size would be). Market Town's is drawn: a little stall with a striped awning, like the
 * stalls on the map (Ruby: not the 🏪 shop emoji); the others are their emoji.
 */
@Composable
fun PlaceIcon(land: Land, sizeSp: Float, modifier: Modifier = Modifier) {
    if (land == Land.MARKET) {
        Canvas(modifier.size((sizeSp * 1.2f).dp)) { marketStallIcon() }
    } else {
        Text(land.emoji, fontSize = sizeSp.sp, textAlign = TextAlign.Center, modifier = modifier)
    }
}

/** The market stall icon, filling the draw area: striped awning with a scalloped edge, posts, counter, fruit. */
fun androidx.compose.ui.graphics.drawscope.DrawScope.marketStallIcon() {
    val w = size.width
    val h = size.height
    val wood = Color(0xFF8D6E63)
    val stripe = Color(0xFF1E88E5)
    // posts
    drawRect(wood, Offset(w * 0.16f, h * 0.38f), androidx.compose.ui.geometry.Size(w * 0.08f, h * 0.5f))
    drawRect(wood, Offset(w * 0.76f, h * 0.38f), androidx.compose.ui.geometry.Size(w * 0.08f, h * 0.5f))
    // counter with fruit
    drawRect(Color(0xFFD7A26B), Offset(w * 0.1f, h * 0.62f), androidx.compose.ui.geometry.Size(w * 0.8f, h * 0.26f))
    drawRect(Color(0xFFB07C4F), Offset(w * 0.1f, h * 0.62f), androidx.compose.ui.geometry.Size(w * 0.8f, h * 0.05f))
    listOf(0xFFE84A7F, 0xFFFFCA28, 0xFF66BB6A).forEachIndexed { i, c -> drawCircle(Color(c), w * 0.075f, Offset(w * (0.3f + i * 0.2f), h * 0.57f)) }
    // striped awning, scalloped
    val n = 5
    val sw = w * 0.92f / n
    for (i in 0 until n) {
        val col = if (i % 2 == 0) stripe else Color.White
        val left = w * 0.04f + i * sw
        drawRect(col, Offset(left, h * 0.1f), androidx.compose.ui.geometry.Size(sw, h * 0.24f))
        drawArc(col, 0f, 180f, true, Offset(left, h * 0.24f), androidx.compose.ui.geometry.Size(sw, h * 0.18f))
    }
    drawRoundRect(Color(0xFF1565C0), Offset(w * 0.02f, h * 0.06f), androidx.compose.ui.geometry.Size(w * 0.96f, h * 0.07f), androidx.compose.ui.geometry.CornerRadius(h * 0.03f))
}

/** An island costume as what Rory wears. */
fun com.zeronolidge.speechapp.logic.Cosmetic.asOutfit(): Outfit = when (slot) {
    com.zeronolidge.speechapp.logic.Slot.HAT -> Outfit(hat = this)
    com.zeronolidge.speechapp.logic.Slot.CAPE -> Outfit(cape = this)
    com.zeronolidge.speechapp.logic.Slot.GLASSES -> Outfit(glasses = this)
    else -> Outfit()
}
