package com.zeronolidge.speechapp.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale
import androidx.compose.ui.zIndex
import com.zeronolidge.speechapp.ui.components.GoButton
import com.zeronolidge.speechapp.ui.components.Nudge
import com.zeronolidge.speechapp.ui.components.PointingHand
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.logic.Land
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.logic.heroSpecies
import com.zeronolidge.speechapp.ui.Screen
import com.zeronolidge.speechapp.ui.art.Critter
import com.zeronolidge.speechapp.ui.art.HeroWithPet
import com.zeronolidge.speechapp.ui.art.Mood
import com.zeronolidge.speechapp.ui.art.Scenery
import com.zeronolidge.speechapp.ui.components.HeroBadge
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.theme.Kid

/** First launch: pick a hero. */
@Composable
fun HeroPickScreen(onPicked: () -> Unit) {
    val app = LocalApp.current
    var picked by remember { mutableStateOf<Species?>(null) }
    LaunchedEffect(Unit) { app.speaker.say("Hi friend! Every adventure needs a hero. Who do you want to be?") }
    Scenery(Land.MEADOW, Modifier.fillMaxSize(), seed = 3) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Choose your hero!", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Kid.ink)
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                heroSpecies.forEach { sp ->
                    val selected = sp == picked
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (selected) Color(0xCCFFF59D) else Color(0x88FFFFFF))
                            .then(if (selected) Modifier.border(4.dp, Color(0xFFFFB300), RoundedCornerShape(24.dp)) else Modifier)
                            .clickable {
                                picked = sp
                                app.speaker.say("${sp.label}! Tap the big green arrow to start!")
                            }
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Critter(
                            sp, Modifier.weight(1f),
                            color = Color(if (sp == Species.KID) 0xFFFF7043 else sp.defaultColor),
                            mood = if (selected) Mood.CHEER else Mood.HAPPY,
                        )
                        Text(sp.label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Kid.ink)
                    }
                }
            }
            Box(Modifier.height(130.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (picked != null) {
                    GoButton(size = 84.dp) {
                        val sp = picked ?: return@GoButton
                        app.progress.chooseHero(sp)
                        app.speaker.say("Hooray! Let's go on an adventure!")
                        onPicked()
                    }
                } else {
                    PointingHand(size = 56.dp)
                }
            }
        }
    }
    if (picked == null) Nudge("pick", "Tap a friend to be your hero!")
    else Nudge(picked!!, "Tap the big green arrow to start!")
}

/** The world map: lands to visit, the hero's level, and buttons for dress-up and practice. */
@Composable
fun MapScreen(go: (Screen) -> Unit) {
    val app = LocalApp.current
    val p = app.progress
    val suggested = p.suggestedLand()
    val hintLine = "Tap ${suggested.hint}, where the hand is pointing!"
    LaunchedEffect(Unit) {
        app.speaker.say(
            if (p.questsDone == 0) "This is the map! Let's start the adventure. $hintLine"
            else "Where shall we go, ${p.heroName}? $hintLine",
        )
    }
    Nudge("map", hintLine, everyMs = 10000)
    val species = p.hero ?: Species.KID
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF81D4FA), Color(0xFFB3E5FC)))),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(top = 70.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)) {
            val w = maxWidth
            val h = maxHeight
            // The island and the dotted trail between lands.
            Canvas(Modifier.fillMaxSize()) {
                drawOval(Color(0xFFC5E1A5), Offset(-size.width * 0.04f, size.height * 0.05f), androidx.compose.ui.geometry.Size(size.width * 1.08f, size.height * 0.98f))
                drawOval(Color(0xFFAED581), Offset(size.width * 0.02f, size.height * 0.12f), androidx.compose.ui.geometry.Size(size.width * 0.96f, size.height * 0.82f))
                val path = Path()
                Land.entries.forEachIndexed { i, land ->
                    val x = land.mapX * size.width
                    val y = land.mapY * size.height
                    if (i == 0) path.moveTo(x, y) else {
                        val prev = Land.entries[i - 1]
                        val px = prev.mapX * size.width
                        val py = prev.mapY * size.height
                        path.quadraticTo((px + x) / 2, (py + y) / 2 - size.height * 0.15f, x, y)
                    }
                }
                drawPath(
                    path, Color(0xFFFFF3E0),
                    style = Stroke(10.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 22f))),
                )
            }
            val node = minOf(w / 7.5f, h / 3.2f, 120.dp)
            val bounce = rememberInfiniteTransition(label = "suggest")
            val pulse by bounce.animateFloat(1f, 1.15f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "pulse")
            Land.entries.forEach { land ->
                val open = p.landOpen(land)
                val isSuggested = land == suggested
                Column(
                    Modifier
                        .offset(x = w * land.mapX - node / 2, y = h * land.mapY - node / 2)
                        .width(node)
                        .zIndex(if (isSuggested) 1f else 0f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(node)
                            .scale(if (isSuggested) pulse else 1f)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .border(
                                if (isSuggested) 7.dp else 4.dp,
                                when {
                                    isSuggested -> Color(0xFFFFD600)
                                    open -> Color.White
                                    else -> Color(0xFF90A4AE)
                                },
                                CircleShape,
                            )
                            .clickable {
                                if (open) {
                                    app.speaker.say("${land.title}!")
                                    p.visit(land)
                                    go(Screen.Quest(land))
                                } else {
                                    val need = land.needs!!
                                    app.speaker.say("That place is locked! You need ${need.label}. Keep playing to get it! $hintLine")
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Scenery(land, Modifier.fillMaxSize(), seed = 1)
                        Text(land.emoji, fontSize = (node.value * 0.4f).sp)
                        if (!open) {
                            Box(Modifier.fillMaxSize().background(Color(0x99455A64)), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🔒", fontSize = (node.value * 0.25f).sp)
                                    Text("${land.needs?.emoji} Lv ${land.needs?.level}", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Text(
                        land.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Kid.ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCCFFFFFF))
                            .padding(horizontal = 6.dp),
                    )
                }
            }
            // A big hand points down at the land to try next.
            val hand = node * 0.6f
            PointingHand(
                Modifier
                    .offset(x = w * suggested.mapX - hand / 2, y = h * suggested.mapY - node / 2 - hand * 1.25f)
                    .zIndex(2f),
                size = hand,
                pointDown = true,
            )
            // The hero waves from the bottom corner.
            HeroWithPet(
                species, p.heroColor(species), p.outfit, p.pet,
                Modifier
                    .align(Alignment.BottomStart)
                    .height(h * 0.34f)
                    .width(h * 0.36f)
                    .offset(x = w * 0.49f),
            )
        }

        // Top bar.
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HeroBadge { go(Screen.Wardrobe) }
            Spacer(Modifier.weight(1f))
            MapButton("⭐", "Adventure!", Color(0xFFFFD54F)) {
                val open = Land.entries.filter { p.landOpen(it) && it != Land.CLOUDS }
                app.speaker.say("A surprise adventure!")
                go(Screen.Quest(open.random(), mixed = true))
            }
            MapButton("🎒", "Dress up", Color(0xFFF8BBD0)) {
                app.speaker.say("Let's dress up!")
                go(Screen.Wardrobe)
            }
            MapButton("🎯", "Practice", Color(0xFFB2EBF2)) {
                app.speaker.say("Practice games!")
                go(Screen.Practice)
            }
            // Grown-ups only: press and hold so little fingers don't wander in.
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x33000000))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { app.speaker.say("Ask a grown-up to press and hold.") },
                            onLongPress = { go(Screen.GrownUps) },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) { Text("⚙️", fontSize = 24.sp) }
        }
    }
}

@Composable
private fun MapButton(emoji: String, label: String, color: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .shadow(4.dp, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(color)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 24.sp)
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Kid.ink)
    }
}
