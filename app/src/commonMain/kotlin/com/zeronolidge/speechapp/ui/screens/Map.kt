package com.zeronolidge.speechapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
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
import com.zeronolidge.speechapp.ui.components.ArrowIcon
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
import com.zeronolidge.speechapp.ui.components.PaperColorsUi
import com.zeronolidge.speechapp.ui.components.ScreenFrame
import com.zeronolidge.speechapp.ui.components.SpeakOnOpen
import com.zeronolidge.speechapp.ui.components.popIn

/** Pick a buddy: three at a time, then the big green arrow. Shown on first launch (and from the closet). */
@Composable
fun HeroPickScreen(onPicked: () -> Unit, onHome: (() -> Unit)? = null, onBack: (() -> Unit)? = null) {
    val app = LocalApp.current
    var picked by remember { mutableStateOf<Species?>(null) }
    SpeakOnOpen("Hi friend! Every adventure needs a buddy. Tap a friend to pick your buddy!")
    ScreenFrame(
        onHome = onHome,
        onBack = onBack,
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
                    color = Color(if (sp == Species.KID) 0xFF5B7FDB else sp.defaultColor),
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
    val hintLine = "Tap the big yellow button by ${land.hint}, where the hand is pointing!"
    LaunchedEffect(Unit) {
        val present = p.checkIn(today())
        present?.let { app.showGift(it) }
        val welcome = present?.let { "Welcome back, ${p.heroName}! Your buddy found a present for coming to play: the ${it.name}! " } ?: ""
        val line = when {
            p.chapter == 0 -> "This is the map! Let's start the adventure. $hintLine"
            unfoldNew && Trail.opensLand(p.chapter) -> "Look, the map is opening up! A new place to explore. $hintLine"
            unfoldNew -> "The trail goes on! $hintLine"
            else -> "Where shall we go, ${p.heroName}? $hintLine"
        }
        app.rememberInstruction(line)
        if (present != null) app.celebrate(welcome + line) else app.instruct(line)
    }
    Nudge("map", hintLine, everyMs = 12000)
    val species = p.hero ?: Species.KID
    var hintFrom by remember { mutableStateOf<Land?>(null) }
    var hintKey by remember { mutableStateOf(0) }
    // The pointing hand hides as soon as anything is tapped, and comes back after a quiet while.
    var taps by remember { mutableStateOf(0) }
    var showHand by remember { mutableStateOf(true) }
    LaunchedEffect(taps) {
        if (taps > 0) {
            showHand = false
            kotlinx.coroutines.delay(8000)
            showHand = true
        }
    }
    val startChapter = {
        app.speaker.say("${land.title}! Let's go!")
        p.markUnfoldSeen()
        nav.go(Screen.Chapter())
    }
    // The island keeps its shape and fits the screen; the sea fills any space around it.
    // The corner buttons sit on top, in the same spots as on every screen.
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF3FA9E0))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent(PointerEventPass.Initial)
                        if (e.changes.any { it.pressed && !it.previousPressed }) taps++
                    }
                }
            },
    ) {
        val top = 60.dp
        val bottom = if (mapNarrow(maxWidth)) 176.dp else 90.dp
        val availH = maxHeight - top - bottom
        // Wide screens keep the island's usual shape; upright screens let it grow taller
        // instead of leaving big empty bands of sea.
        val aspect = (maxWidth / availH).coerceIn(1.15f, 1.75f)
        val mapW = minOf(maxWidth, availH * aspect)
        val mapH = mapW / aspect
        IslandMap(
            chapter = p.chapter,
            reached = { it in Trail.reached(p.chapter) },
            unfoldNew = unfoldNew,
            onUnfolded = { p.markUnfoldSeen() },
            onStop = startChapter,
            onElsewhere = { l ->
                // The next area along the trail is already peeking out of its mist: it opens too.
                val peek = if (!Trail.opensLand(p.chapter)) Trail.lands.getOrNull(Trail.lands.indexOf(land) + 1) else null
                when {
                    // An open area (its label, picture or landmark): open that same area.
                    // The story itself starts from the yellow stop button or the Adventure banner.
                    l in Trail.reached(p.chapter) || l == peek -> {
                        app.speaker.say("${l.title}!")
                        nav.go(Screen.LandGames(l))
                    }
                    else -> {
                        // A misty place: it wiggles, and Rory walks along a sparkly trail to the next stop.
                        hintFrom = l
                        hintKey++
                        app.speaker.say("Beep boop! Not yet! Follow me to ${land.title}!")
                    }
                }
            },
            hintFrom = hintFrom,
            hintKey = hintKey,
            modifier = Modifier.size(mapW, mapH).align(Alignment.Center).offset(y = (top - bottom) / 2),
            hero = { m -> HeroWithPet(species, p.heroColor(species), p.outfit, p.pet, m) },
            pointer = { m, size, fromLeft -> if (showHand) PointingHand(m, size = size, pointRight = fromLeft, pointLeft = !fromLeft) },
        )
        MapCorners(nav, startChapter)
    }
}

/** The map's own buttons, kept to the corners so the trail stays the main thing to tap. */
@Composable
private fun MapCorners(nav: Nav, onAdventure: () -> Unit) {
    val app = LocalApp.current
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp)) {
        val narrow = mapNarrow(maxWidth)
        Row(Modifier.align(Alignment.TopStart).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HeroBadge { app.speaker.say("Your buddy's closet!"); nav.go(Screen.Closet) }
            Spacer(Modifier.weight(1f))
            HearAgainButton()
        }
        // The bottom row: grown-ups' gear, the big Adventure button, then stickers and practice.
        // Every button has its own space. On a narrow phone, Adventure gets a row of its own above.
        // One wide banner, tappable all over (arrow, word and the paper around them), that presses
        // down under a finger. Tomato red: the one warm accent, kept for things he can tap.
        val adventure = @Composable { m: Modifier ->
            PaperButton(PaperColorsUi.home, m.height(84.dp), corner = 999.dp, onClick = onAdventure) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    ArrowIcon(30.dp, color = Color.White)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Adventure!", fontSize = 26.sp, color = Color.White, maxLines = 1, softWrap = false,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                    )
                }
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (narrow) {
                adventure(Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Grown-ups only: a small gear that opens a grown-up check.
                PaperIconButton("⚙️", Color(0xFFCFD8DC), size = 64.dp) {
                    app.speaker.say("This part is for grown-ups.")
                    nav.go(Screen.Gate)
                }
                Spacer(Modifier.width(24.dp))
                if (!narrow) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        adventure(Modifier.widthIn(max = 460.dp).fillMaxWidth())
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.width(24.dp))
                PaperIconButton("📒", Color(0xFFB2EBF2)) { app.speaker.say("Your sticker book!"); nav.go(Screen.Stickers) }
                Spacer(Modifier.width(18.dp))
                PaperIconButton("🎯", Color(0xFFF8BBD0)) { app.speaker.say("Practice games!"); nav.go(Screen.Practice) }
            }
        }
    }
}

/** Below this width, the Adventure button gets its own row above the other bottom buttons. */
private fun mapNarrow(width: Dp) = width < 600.dp

