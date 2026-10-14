package com.zeronolidge.speechapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.logic.Cosmetic
import com.zeronolidge.speechapp.logic.Land
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.logic.Trail
import com.zeronolidge.speechapp.logic.heroSpecies
import com.zeronolidge.speechapp.state.today
import com.zeronolidge.speechapp.ui.Nav
import com.zeronolidge.speechapp.ui.Screen
import com.zeronolidge.speechapp.ui.art.Critter
import com.zeronolidge.speechapp.ui.art.HeroWithPet
import com.zeronolidge.speechapp.ui.art.IslandMap
import com.zeronolidge.speechapp.ui.art.Mood
import com.zeronolidge.speechapp.ui.art.PaperStyle
import com.zeronolidge.speechapp.ui.art.Scenery
import com.zeronolidge.speechapp.ui.components.ChoiceTile
import com.zeronolidge.speechapp.ui.components.GoButton
import com.zeronolidge.speechapp.ui.components.HearAgainButton
import com.zeronolidge.speechapp.ui.components.HeroBadge
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.Nudge
import com.zeronolidge.speechapp.ui.components.PagedChoices
import com.zeronolidge.speechapp.ui.components.PaperButton
import com.zeronolidge.speechapp.ui.components.PaperChip
import com.zeronolidge.speechapp.ui.components.PaperIconButton
import com.zeronolidge.speechapp.ui.components.PointingHand
import com.zeronolidge.speechapp.ui.components.ScreenFrame
import com.zeronolidge.speechapp.ui.components.SpeakOnOpen
import com.zeronolidge.speechapp.ui.components.popIn

/** Pick a buddy: three at a time, then the big green arrow. Shown on first launch (and from the closet). */
@Composable
fun HeroPickScreen(onPicked: () -> Unit, onHome: (() -> Unit)? = null) {
    val app = LocalApp.current
    var picked by remember { mutableStateOf<Species?>(null) }
    SpeakOnOpen("Hi friend! Every adventure needs a buddy. Tap a friend to pick your buddy!")
    ScreenFrame(
        onHome = onHome,
        background = { Scenery(Land.MEADOW, Modifier.fillMaxSize(), seed = 3) },
    ) {
        PagedChoices(heroSpecies, perPage = 3, modifier = Modifier.weight(1f).fillMaxWidth()) { sp, m ->
            ChoiceTile(
                emoji = "", label = null, color = PaperStyle.chip,
                modifier = m.padding(vertical = 6.dp),
                selected = sp == picked,
                onClick = {
                    picked = sp
                    app.instruct("${sp.label}! Tap the big green arrow to start!")
                },
            ) {
                Critter(
                    sp, Modifier.fillMaxHeight(),
                    color = Color(if (sp == Species.KID) 0xFFFF7043 else sp.defaultColor),
                    mood = if (sp == picked) Mood.CHEER else Mood.HAPPY,
                )
            }
        }
        Box(Modifier.height(150.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val sp = picked
            if (sp != null) {
                GoButton(size = 96.dp) {
                    app.progress.chooseHero(sp)
                    app.speaker.say("Hooray! Let's go on an adventure!")
                    onPicked()
                }
            }
        }
    }
    if (picked == null) Nudge("pick", "Tap a friend to pick your buddy!")
    else Nudge(picked!!, "Tap the big green arrow to start!")
}

/**
 * The world map: the paper trail with the next stop to play, plus the buddy's closet (top-left),
 * the sticker book and practice games (bottom-right) and the grown-ups' gear (bottom-left).
 */
@Composable
fun MapScreen(nav: Nav) {
    val app = LocalApp.current
    val p = app.progress
    val land = p.currentLand
    val unfoldNew = p.unfoldedSeen < p.chapter
    val gift = remember { mutableStateOf<Cosmetic?>(null) }
    val hintLine = "Tap the big yellow button by ${land.hint}, where the hand is pointing!"
    LaunchedEffect(Unit) {
        gift.value = p.checkIn(today())
        val welcome = gift.value?.let { "Welcome back, ${p.heroName}! Your buddy found a present for coming to play: the ${it.name}! " } ?: ""
        val line = when {
            p.chapter == 0 -> "This is the map! Let's start the adventure. $hintLine"
            unfoldNew && Trail.opensLand(p.chapter) -> "Look, the map is opening up! A new place to explore. $hintLine"
            unfoldNew -> "The trail goes on! $hintLine"
            else -> "Where shall we go, ${p.heroName}? $hintLine"
        }
        app.rememberInstruction(line)
        if (gift.value != null) app.celebrate(welcome + line) else app.instruct(line)
    }
    Nudge("map", hintLine, everyMs = 12000)
    val species = p.hero ?: Species.KID
    // The map fills the screen; the corner buttons sit on top of it, in the same spots as on every screen.
    Box(Modifier.fillMaxSize()) {
        IslandMap(
            chapter = p.chapter,
            reached = { it in Trail.reached(p.chapter) },
            unfoldNew = unfoldNew,
            onUnfolded = { p.markUnfoldSeen() },
            onStop = {
                app.speaker.say("${land.title}! Let's go!")
                p.markUnfoldSeen()
                nav.go(Screen.Chapter)
            },
            onElsewhere = { l ->
                app.speaker.say(
                    when {
                        l == land -> "Let's go! $hintLine"
                        l in Trail.reached(p.chapter) -> "We've been there! The next adventure is where the hand is pointing."
                        else -> "That place is still hidden in the mist. Follow the trail to get there!"
                    },
                )
            },
            modifier = Modifier.fillMaxSize().padding(top = 60.dp),
            hero = { m -> HeroWithPet(species, p.heroColor(species), p.outfit, p.pet, m) },
            pointer = { m, size -> PointingHand(m, size = size, pointDown = true) },
        )
        MapCorners(nav)
    }
    gift.value?.let { g -> GiftCard(g) { gift.value = null } }
}

/** The map's own buttons, kept to the corners so the trail stays the main thing to tap. */
@Composable
private fun MapCorners(nav: Nav) {
    val app = LocalApp.current
    Box(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(Modifier.align(Alignment.TopStart).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HeroBadge { app.speaker.say("Your buddy's closet!"); nav.go(Screen.Closet) }
            Spacer(Modifier.weight(1f))
            HearAgainButton()
        }
        Row(
            Modifier.align(Alignment.BottomEnd),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            PaperIconButton("📒", Color(0xFFB2EBF2)) { app.speaker.say("Your sticker book!"); nav.go(Screen.Stickers) }
            PaperIconButton("🎯", Color(0xFFF8BBD0)) { app.speaker.say("Practice games!"); nav.go(Screen.Practice) }
        }
        // Grown-ups only: a small gear that opens a grown-up check.
        PaperIconButton("⚙️", Color(0xFFCFD8DC), size = 56.dp, modifier = Modifier.align(Alignment.BottomStart)) {
            app.speaker.say("This part is for grown-ups.")
            nav.go(Screen.Gate)
        }
    }
}

/** A "welcome back" present: the new paper piece pops up on a card. Tap anywhere to close. */
@Composable
private fun GiftCard(piece: Cosmetic, onClose: () -> Unit) {
    val p = LocalApp.current.progress
    val species = p.hero ?: Species.KID
    Box(
        Modifier.fillMaxSize().padding(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        PaperButton(
            PaperStyle.chip,
            Modifier.fillMaxWidth(0.55f).fillMaxHeight(0.8f).popIn(piece),
            onClick = onClose,
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎁", fontSize = 44.sp)
                HeroWithPet(species, p.heroColor(species), p.outfit, p.pet, Modifier.weight(1f).fillMaxWidth(), Mood.CHEER)
                PaperChip(piece.name)
            }
        }
    }
}
