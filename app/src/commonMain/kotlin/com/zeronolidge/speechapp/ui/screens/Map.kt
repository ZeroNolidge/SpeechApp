package com.zeronolidge.speechapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import com.zeronolidge.speechapp.ui.components.popIn
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
        // The present waits in the closet (a 🎁 on the buddy badge); nothing pops up by itself.
        present?.let { app.newPiece = it }
        val welcome = present?.let { "Welcome back, ${p.heroName}! Your buddy found a present for coming to play: the ${it.name}! It's in the closet. " } ?: ""
        // Back after a few days away: no streaks, nothing lost, just a happy hello.
        val missed = if (p.daysAway >= 1) "I missed you, ${p.heroName}! I'm so happy you're back! " else ""
        val cave = if (p.echoReady) "A secret crystal cave! Tap the glowing purple cave by the trees to play. " else ""
        val owl = if (owlOpen(p) && "owl" !in p.voiceStoriesDone && !p.owlAnnounced) {
            p.markOwlAnnounced()
            "Look! Sleepy Owl has a story for you, by the trees! "
        } else ""
        val line = missed + cave + owl + when {
            p.chapter == 0 -> "This is the map! Let's start the adventure. $hintLine"
            unfoldNew && Trail.opensLand(p.chapter) -> "Look, the map is opening up! A new place to explore. $hintLine"
            unfoldNew -> "The trail goes on! $hintLine"
            else -> "Where shall we go, ${p.heroName}? $hintLine"
        }
        app.rememberInstruction(line)
        if (present != null) app.celebrate(welcome + line) else app.instruct(line)
    }
    // No spoken nagging and never a jump to another screen when nobody taps: after about 20
    // seconds of quiet, Rory just waves from the corner (see MapWave), and that's all.
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
        // For checking: every area and the scene a tap on it opens, from the same rule as onElsewhere.
        LaunchedEffect(p.chapter) {
            val peek = if (!Trail.opensLand(p.chapter)) Trail.lands.getOrNull(Trail.lands.indexOf(land) + 1) else null
            Land.entries.forEach { l ->
                val opens = if (l in Trail.reached(p.chapter) || l == peek) {
                    "${l.title} games: " + landTiles(l).joinToString(", ") { it.label }
                } else {
                    "misty: wiggles, Rory shows the way to ${land.title}"
                }
                println("MAPSCENE ${l.name} -> $opens")
            }
            println("MAPSCENE STOP -> the next story chapter (${land.title}, stop ${Trail.stopOf(p.chapter) + 1}; its visitor is picked fresh each time)")
        }
        IslandMap(
            chapter = p.chapter,
            reached = { it in Trail.reached(p.chapter) },
            unfoldNew = unfoldNew,
            onUnfolded = { p.markUnfoldSeen() },
            onStop = startChapter,
            // Once Pip is found: the crystal cave, a stop just past the Forest, there for good
            // (glowing while it has new tries waiting). It lives only here, on the map.
            cave = p.echoReady || p.pipTags > 0,
            caveFresh = p.echoReady,
            onCave = {
                app.speaker.say("The crystal cave!")
                nav.go(Screen.EchoCave())
            },
            // The voice stories, each a stop by its land (always in the same order, so they stay put).
            // Sleepy Owl's Morning is a reward stop, like the cave: it appears once the first
            // Forest chapter is done (the other stories are there from the start).
            stories = com.zeronolidge.speechapp.logic.VoiceStories.all.filter { it.id != "owl" || owlOpen(p) }.map {
                com.zeronolidge.speechapp.ui.art.MapStoryStop(it.id, it.emoji, it.land, it.id in p.voiceStoriesDone)
            },
            // A finished stop's ⭐: play a story there again (a fresh one; the trail stays put).
            onDone = { stop ->
                app.speaker.say("Let's play it again!")
                nav.go(Screen.Chapter(Trail.landAt(stop), replay = true))
            },
            onStory = { id ->
                com.zeronolidge.speechapp.logic.VoiceStories.byId(id)?.let { app.speaker.say(it.title + "!") }
                nav.go(Screen.VoiceStory(id))
            },
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
                        app.speaker.say("Beep boop! Not yet! Follow me to ${land.shortName}!")
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
        MapWave()
    }
}

/** The map's own buttons, kept to the corners so the trail stays the main thing to tap. */
@Composable
private fun MapCorners(nav: Nav, onAdventure: () -> Unit) {
    val app = LocalApp.current
    val p = app.progress
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
                // (The crystal cave lives only on the map, as a glowing stop on the path.)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    adventure(Modifier.weight(1f))
                }
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

/**
 * After about 20 seconds with no taps, Rory peeks in at the left edge and waves (no words, no
 * moving to another screen). Any tap tucks him away and starts the wait again. At most 3 times.
 */
@Composable
private fun MapWave() {
    val app = LocalApp.current
    var waving by remember { mutableStateOf(false) }
    var waves by remember { mutableStateOf(0) }
    // Every touch anywhere (counted at the app's root, so no layer here ever takes a tap).
    LaunchedEffect(app.touches) {
        waving = false
        if (waves >= 3) return@LaunchedEffect
        kotlinx.coroutines.delay(20_000)
        waving = true
        waves++
        kotlinx.coroutines.delay(4_000)
        waving = false
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
        if (waving) {
            Critter(
                Species.ROBOT, Modifier.padding(start = 6.dp).size(width = 90.dp, height = 110.dp).popIn("wave$waves"),
                // A real wave: hand up, side to side twice, then down.
                Color(0xFFFFCC80), mood = Mood.HAPPY, waving = true,
            )
        }
    }
}

/** Sleepy Owl's Morning opens once the first Forest chapter is done. */
private fun owlOpen(p: com.zeronolidge.speechapp.state.Progress) = p.chaptersDone >= 1
