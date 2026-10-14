package com.zeronolidge.speechapp.ui.screens

import com.zeronolidge.speechapp.ui.art.paperCard
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
import com.zeronolidge.speechapp.ui.art.IslandMap
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
            .background(Color(0xFF3FA9E0)),
    ) {
        IslandMap(
            open = { p.landOpen(it) },
            suggested = suggested,
            onTap = { land ->
                if (p.landOpen(land)) {
                    app.speaker.say("${land.title}!")
                    p.visit(land)
                    go(Screen.Quest(land))
                } else {
                    val need = land.needs!!
                    app.speaker.say("That place is hidden in the mist! You need ${need.label}. Keep playing to get it! $hintLine")
                }
            },
            modifier = Modifier.fillMaxSize().padding(top = 56.dp),
            hero = { m -> HeroWithPet(species, p.heroColor(species), p.outfit, p.pet, m) },
            pointer = { m, size -> PointingHand(m, size = size, pointDown = true) },
        )

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
            .paperCard(color, 999.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 24.sp)
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Kid.ink)
    }
}
