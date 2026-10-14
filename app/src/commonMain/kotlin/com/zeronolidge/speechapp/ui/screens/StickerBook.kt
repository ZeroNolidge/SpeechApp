package com.zeronolidge.speechapp.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.logic.WordStat
import com.zeronolidge.speechapp.ui.Nav
import com.zeronolidge.speechapp.ui.art.Critter
import com.zeronolidge.speechapp.ui.art.Mood
import com.zeronolidge.speechapp.ui.art.paperCard
import com.zeronolidge.speechapp.ui.components.KidScreen
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.PagedChoices
import com.zeronolidge.speechapp.ui.components.PaperChip
import com.zeronolidge.speechapp.ui.components.flip
import kotlinx.coroutines.launch

/**
 * A die-cut paper sticker of a practised word. Shiny stickers (the microphone heard the word
 * clearly) get a gold rim and a twinkle. Tapping a sticker flips it and says the word.
 */
@Composable
fun Sticker(w: WordStat, size: Dp, modifier: Modifier = Modifier, onTap: (() -> Unit)? = null) {
    val tilt = remember(w.text) { ((w.text.hashCode() % 13) - 6).toFloat() }
    val twinkle = rememberInfiniteTransition(label = "twinkle")
    val shine by twinkle.animateFloat(0.3f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "shine")
    val flipping = remember(w.text) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        modifier
            .size(size)
            .rotate(tilt)
            .flip(flipping.value)
            .then(
                if (onTap == null) Modifier else Modifier.clickable(remember { MutableInteractionSource() }, indication = null) {
                    onTap()
                    scope.launch {
                        flipping.snapTo(0f)
                        flipping.animateTo(1f, tween(260))
                        flipping.animateTo(0f, tween(260))
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        // White die-cut border, then the coloured sticker.
        Box(Modifier.fillMaxSize().paperCard(Color.White, circle = true))
        Box(
            Modifier
                .fillMaxSize(0.84f)
                .paperCard(if (w.shiny) Color(0xFFFFF3C4) else Color(0xFFE8F4FD), circle = true)
                .then(if (w.shiny) Modifier.border(4.dp, Color(0xFFFFC107), CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(w.emoji, fontSize = (size.value * 0.42f).sp)
        }
        if (w.shiny) {
            Text("✨", fontSize = (size.value * 0.22f).sp, modifier = Modifier.align(Alignment.TopEnd).alpha(shine))
        }
    }
}

/** Every word practised becomes a sticker. Four stickers a page, big paper arrows to turn pages. */
@Composable
fun StickerBookScreen(nav: Nav) {
    val app = LocalApp.current
    val p = app.progress
    val stickers = p.words.stickers
    val spoken = if (stickers.isEmpty()) {
        "This is your sticker book! It's empty right now. Say words in the story, and each word becomes a sticker!"
    } else {
        "Your sticker book! You have ${stickers.size} ${if (stickers.size == 1) "sticker" else "stickers"}. Tap a sticker to hear its word. The sparkly ones are words I heard really clearly!"
    }
    KidScreen("📒", nav.home, spoken) {
        if (stickers.isEmpty()) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                val species = p.hero ?: Species.KID
                Critter(species, Modifier.fillMaxHeight(0.6f), p.heroColor(species), p.outfit, Mood.THINK)
                Spacer(Modifier.height(12.dp))
                PaperChip("🗣️ ▶ ⭐", fontSize = 40.sp)
            }
        } else {
            BoxWithConstraints(Modifier.fillMaxSize().paperCard(Color(0xFFFFFAF0), 30.dp)) {
                val size = minOf(maxWidth / 6.2f, maxHeight / 1.9f, 190.dp)
                PagedChoices(stickers, perPage = 4, modifier = Modifier.fillMaxSize()) { w, m ->
                    Column(m, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Sticker(w, size, onTap = { app.speaker.say(w.text) })
                        Spacer(Modifier.height(14.dp))
                        PaperChip(w.text, fontSize = 22.sp, modifier = Modifier.offset(y = 0.dp))
                    }
                }
            }
        }
    }
}
