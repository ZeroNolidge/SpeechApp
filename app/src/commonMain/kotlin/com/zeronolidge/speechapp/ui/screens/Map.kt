package com.zeronolidge.speechapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import com.zeronolidge.speechapp.ui.components.popIn
import com.zeronolidge.speechapp.ui.art.paperCard
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
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.graphics.graphicsLayer
import com.zeronolidge.speechapp.ui.art.asOutfit
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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
    // Island story. (Review and deep links, "#test=island&fixed=N" / "island-fix:LAND" /
    // "island-party": the places fixed come from the test hook, every place open.)
    var testFixed by remember { mutableStateOf(com.zeronolidge.speechapp.logic.IslandTest.fixed) }
    val fixedLands = testFixed ?: p.fixedLands
    // A place just fixed (its story finished): its fix moment plays as the map opens.
    val pendingFix = remember { com.zeronolidge.speechapp.logic.IslandTest.fix ?: p.pendingFix }
    var fixT by remember { mutableStateOf(if (pendingFix != null) 0f else -1f) }
    var partyT by remember { mutableStateOf(-1f) }
    // The pointing hand waits until the fix moment (and any party) is over and the stops have
    // faded back in (Ruby, test53).
    var handReady by remember { mutableStateOf(pendingFix == null) }
    // The replay card: the place whose ⭐ was tapped (with two or more finished stops there).
    var replayAt by remember { mutableStateOf<Land?>(null) }
    // The next stop on the map (dp in the map, and its ring), so the replay card keeps clear of it.
    var stopAt by remember { mutableStateOf<FloatArray?>(null) }
    val still = com.zeronolidge.speechapp.ui.art.LocalReduceMotion.current
    val sweepDone = fixT < 0f || fixT * com.zeronolidge.speechapp.logic.IslandStory.FIX_MS >= com.zeronolidge.speechapp.logic.IslandStory.SWEEP_MS
    // Until the sweep has reached it, the new place's flag stays empty.
    val flagsFilled = if (pendingFix != null && !sweepDone) fixedLands - pendingFix else fixedLands
    val roryOutfit = p.roryCostume?.let { com.zeronolidge.speechapp.logic.IslandStory.place(it).costume.asOutfit() } ?: com.zeronolidge.speechapp.ui.art.Outfit()
    LaunchedEffect(Unit) {
        // The fix moment first, as soon as he's back from the story: Rory's after line, the colour
        // sweep, a soft sparkle, his new costume, the flag; then, if that was the eighth flag,
        // the island party. Nothing to tap, no text, and then the map carries on as always.
        if (pendingFix != null) {
            val story = com.zeronolidge.speechapp.logic.IslandStory
            val before = fixedLands - pendingFix
            println("ISLAND moment=${pendingFix.name} fixed=${fixedLands.size}")
            val said = kotlinx.coroutines.CompletableDeferred<Unit>()
            app.speaker.say(story.place(pendingFix).after) { said.complete(Unit) }
            kotlinx.coroutines.delay(250)
            if (still) fixT = 0.99f else {
                val start = withFrameMillis { it }
                while (true) {
                    val now = withFrameMillis { it }
                    fixT = ((now - start).toFloat() / story.FIX_MS).coerceAtMost(1f)
                    if (fixT >= 1f) break
                }
            }
            app.sfx.pop()
            fixT = -1f
            if (testFixed == null) { p.savePendingFix(null); p.wearCostume(pendingFix) } else p.wearCostume(pendingFix)
            kotlinx.coroutines.withTimeoutOrNull(2500) { said.await() }
            if (story.partyNow(before, fixedLands)) {
                println("ISLAND party start")
                app.confetti()
                val start = withFrameMillis { it }
                var lastBurst = 0L
                while (true) {
                    val now = withFrameMillis { it }
                    partyT = ((now - start).toFloat() / story.PARTY_MS).coerceAtMost(1f)
                    if (now - lastBurst > 1200) { lastBurst = now; app.confetti() }
                    if (partyT >= 1f || still) break
                }
                partyT = -1f
                println("ISLAND party end")
            }
            println("ISLAND moment done")
            kotlinx.coroutines.delay(600)
            handReady = true
            println("ISLAND hand back")
            // After about 12 minutes of play, Rory's "let's rest" comes after the moment.
            if (testFixed == null && app.sessionLong()) { nav.go(Screen.Rest); return@LaunchedEffect }
        }
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
    // Island story: which places are fixed. (Review stills, "#test=island&fixed=N": the first N
    // trail places fixed, every place open, and the trail just past them.)
    // (A fix-moment preview stands on the fixed place's second stop, as just after its first.)
    val mapChapter = testFixed?.let { tf -> tf.size * Trail.STOPS_PER_LAND - if (pendingFix != null) 1 else 0 } ?: p.chapter
    val isReached: (Land) -> Boolean = { testFixed != null || it in Trail.reached(p.chapter) }
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
    // Where the buddy badge ends (dp from the left), so a tablet's bunting hangs clear of it.
    var badgeRight by remember { mutableStateOf(300.dp) }
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
        // test54 (Ruby): the island is one picture filling the screen, from the corner buttons down
        // to the Adventure bar, 16 dp in from the sides (MapAreas.mapFrame, also used by the tests).
        // The party bunting no longer takes a row of its own: on a tablet it hangs in the corner
        // buttons' row, on a phone over the top of the picture (nothing on the map goes under it).
        val frame = com.zeronolidge.speechapp.logic.MapAreas.mapFrame(maxWidth.value, maxHeight.value)
        val narrowMap = frame.narrow
        val buntingH = if (narrowMap) com.zeronolidge.speechapp.logic.MapAreas.PHONE_BUNTING_H.dp else 32.dp
        val buntingY = if (narrowMap) com.zeronolidge.speechapp.logic.MapAreas.PHONE_BUNTING_Y.dp
        else 10.dp + (80.dp - com.zeronolidge.speechapp.logic.MapAreas.buntingDrop(32f).dp) / 2
        val mapW = frame.w.dp
        val mapH = frame.h.dp
        val mapLeft = frame.left.dp
        val mapTop = frame.top.dp
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
            chapter = mapChapter,
            onStopAt = { x, y, r -> stopAt = floatArrayOf(x, y, r) },
            reached = isReached,
            fixed = if (pendingFix != null && fixT >= 0f) fixedLands - pendingFix else fixedLands,
            fixLand = pendingFix?.takeIf { fixT >= 0f },
            fixT = fixT.coerceAtLeast(0f),
            party = partyT,
            roryOutfit = roryOutfit,
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
            // A place's ⭐ (one per place): with one finished stop there, play it again straight
            // away; with more, the replay card beside the place, one picture per stop (test53).
            // The trail stays put either way.
            onDone = { stop ->
                val at = Trail.landAt(stop)
                val done = com.zeronolidge.speechapp.logic.ReplayPicker.finishedAt(at, mapChapter)
                if (done.size <= 1) {
                    app.speaker.say("Let's play it again!")
                    nav.go(Screen.Chapter(at, replay = true, stop = done.firstOrNull() ?: stop))
                } else replayAt = at
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
            modifier = Modifier.offset(x = mapLeft, y = mapTop).size(mapW, mapH),
            keepTop = frame.keepTop.dp,
            hero = { m -> HeroWithPet(species, p.heroColor(species), p.outfit, p.pet, m) },
            pointer = { m, size, dir ->
                if (showHand && handReady && replayAt == null) PointingHand(
                    m, size = size,
                    pointRight = dir == com.zeronolidge.speechapp.logic.MapAreas.HandDir.RIGHT,
                    pointLeft = dir == com.zeronolidge.speechapp.logic.MapAreas.HandDir.LEFT,
                    pointDown = dir == com.zeronolidge.speechapp.logic.MapAreas.HandDir.DOWN,
                )
            },
        )
        // The party bunting. At the island party it swings down to hang just above the castle's
        // towers (both show: Ruby, test52), sways there, and goes back up. About twice the
        // castle's width, kept on screen.
        val castleX = mapLeft + mapW * Land.CLOUDS.mapX
        val castleY = mapTop + mapH * Land.CLOUDS.mapY
        val castleBox = minOf(mapW, mapH) * 0.3f
        // At rest: a phone's across the screen under the corner buttons; a tablet's between the
        // buddy badge and the 🔊 button (12 dp clear of each).
        val slotL = if (narrowMap) 18.dp else badgeRight + 12.dp
        val slotR = if (narrowMap) maxWidth - 18.dp else maxWidth - 14.dp - 80.dp - 12.dp
        val buntW = if (narrowMap) minOf(maxWidth - 36.dp, 640.dp) else (slotR - slotL).coerceIn(120.dp, 640.dp)
        val restX = if (narrowMap) maxWidth / 2 else slotL + (slotR - slotL) / 2
        val partyK = (castleBox * 1.9f / buntW).coerceAtMost(1f)
        val partyX = castleX.coerceIn(buntW * partyK / 2 + 12.dp, maxWidth - buntW * partyK / 2 - 12.dp)

        // The flagpoles' tips are about half the castle box above its middle.
        val partyY = castleY - castleBox * 0.5f - buntingH * partyK * 0.5f - 4.dp
        val sweptIn = com.zeronolidge.speechapp.logic.IslandStory.SWEEP_MS.toFloat() / com.zeronolidge.speechapp.logic.IslandStory.FIX_MS
        com.zeronolidge.speechapp.ui.art.PartyBunting(
            flagsFilled,
            Modifier.offset(x = restX - buntW / 2, y = buntingY).width(buntW)
                .graphicsLayer {
                    if (partyT < 0f) return@graphicsLayer
                    val go = when {
                        partyT < 0.15f -> partyT / 0.15f
                        partyT > 0.85f -> (1f - partyT) / 0.15f
                        else -> 1f
                    }.let { 1f - (1f - it) * (1f - it) }
                    translationX = (partyX - restX).toPx() * go
                    translationY = (partyY - buntingY - buntingH / 2).toPx() * go
                    val k = 1f - go * (1f - partyK)
                    scaleX = k
                    scaleY = k
                    rotationZ = go * 5f * kotlin.math.sin(partyT * 2f * kotlin.math.PI.toFloat() * 4f)
                },
            flagHeight = buntingH,
            pop = pendingFix,
            popT = if (fixT < 0f) 1f else ((fixT - sweptIn) / 0.2f).coerceIn(0f, 1f),
        )
        MapCorners(nav, startChapter) { badgeRight = it }
        MapWave(roryOutfit)
        replayAt?.let { at ->
            val picker = com.zeronolidge.speechapp.logic.ReplayPicker
            val stops = picker.finishedAt(at, mapChapter)
            val plots = stops.map { picker.plotFor(it, p.stopPlot(it)) }
            val area = com.zeronolidge.speechapp.logic.MapAreas.of(at)
            val card = picker.card(
                stops.size, (mapLeft + mapW * area.markX).value, (mapTop + mapH * area.markY).value,
                minOf(mapW, mapH).value * com.zeronolidge.speechapp.logic.MapAreas.LANDMARK_RADIUS,
                maxWidth.value, maxHeight.value, mapNarrow(maxWidth),
                // ...clear of the next stop's ▶ and its pulsing ring (Ruby, test53b).
                ring = stopAt?.let { floatArrayOf(mapLeft.value + it[0], mapTop.value + it[1], it[2]) },
            )
            LaunchedEffect(at) {
                println("REPLAYCARD ${at.name} stops=${stops.joinToString(",")} plots=${plots.joinToString(",") { it.id }} card=${card.box.joinToString(",") { it.toInt().toString() }} tiles=${card.tiles.joinToString(" ") { t -> "${((t[0] + t[2]) / 2).toInt()},${((t[1] + t[3]) / 2).toInt()}" }}")
                app.speaker.say(picker.line(plots))
            }
            ReplayCard(card, plots.map { it.goal }, onPick = { i ->
                replayAt = null
                nav.go(Screen.Chapter(at, replay = true, stop = stops[i]))
            }, onClose = { replayAt = null })
        }
    }
}

/** The map's own buttons, kept to the corners so the trail stays the main thing to tap. */
@Composable
private fun MapCorners(nav: Nav, onAdventure: () -> Unit, onBadgeRight: (Dp) -> Unit = {}) {
    val app = LocalApp.current
    val p = app.progress
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp)) {
        val narrow = mapNarrow(maxWidth)
        Row(Modifier.align(Alignment.TopStart).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val dens = androidx.compose.ui.platform.LocalDensity.current
            HeroBadge(Modifier.onGloballyPositioned { c -> onBadgeRight(with(dens) { (c.positionInRoot().x + c.size.width).toDp() }) }) {
                app.speaker.say("Your buddy's closet!"); nav.go(Screen.Closet)
            }
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
private fun MapWave(roryOutfit: com.zeronolidge.speechapp.ui.art.Outfit = com.zeronolidge.speechapp.ui.art.Outfit()) {
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
                Color(0xFFFFCC80), outfit = roryOutfit, mood = Mood.HAPPY, waving = true,
            )
        }
    }
}

/** Sleepy Owl's Morning opens once the first Forest chapter is done. */
private fun owlOpen(p: com.zeronolidge.speechapp.state.Progress) = p.chaptersDone >= 1

/**
 * The replay card (test53): the map dimmed to about 60% behind it, a small paper card beside the
 * place with one big picture tile per finished stop (the story each one played; no words, never
 * green), and a big ✕. A tap outside the card closes it too. Taps only.
 */
@Composable
private fun ReplayCard(card: com.zeronolidge.speechapp.logic.ReplayPicker.Card, pictures: List<String>, onPick: (Int) -> Unit, onClose: () -> Unit) {
    fun FloatArray.at() = Modifier.offset(x = this[0].dp, y = this[1].dp).size((this[2] - this[0]).dp, (this[3] - this[1]).dp)
    Box(
        Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.4f))
            .pointerInput(Unit) { detectTapGestures { onClose() } },
    ) {
        Box(
            card.box.at().paperCard(Color(0xFFFFF8E1), 28.dp)
                .pointerInput(Unit) { detectTapGestures { } },
        )
        card.tiles.forEachIndexed { i, t ->
            com.zeronolidge.speechapp.ui.components.PaperButton(Color(0xFFFFFDF7), t.at(), corner = 22.dp, onClick = { onPick(i) }) {
                Text(pictures[i], fontSize = ((t[3] - t[1]) * 0.5f).sp, modifier = Modifier.align(Alignment.Center))
            }
        }
        com.zeronolidge.speechapp.ui.components.PaperButton(com.zeronolidge.speechapp.ui.art.PaperColors.tomato, card.close.at(), circle = true, onClick = onClose) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize(0.42f).align(Alignment.Center)) {
                val st = size.minDimension * 0.2f
                drawLine(Color.White, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, size.height), st, androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(Color.White, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(0f, size.height), st, androidx.compose.ui.graphics.StrokeCap.Round)
            }
        }
    }
}
