package com.zeronolidge.speechapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.ui.art.HeroWithPet
import com.zeronolidge.speechapp.ui.art.Mood
import com.zeronolidge.speechapp.ui.art.PaperStyle
import com.zeronolidge.speechapp.ui.components.PaperButton
import com.zeronolidge.speechapp.ui.components.PaperChip
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import com.zeronolidge.speechapp.ui.art.paperCard
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.zeronolidge.speechapp.ui.components.PaperColorsUi
import com.zeronolidge.speechapp.ui.components.PaperIconButton
import com.zeronolidge.speechapp.ui.components.PointingHand
import com.zeronolidge.speechapp.ui.components.popIn
import androidx.compose.runtime.Composable
import com.zeronolidge.speechapp.ui.components.LocalScreenWidth
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.zeronolidge.speechapp.ui.components.AppServices
import com.zeronolidge.speechapp.ui.components.CelebrationOverlay
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.paperEnter
import com.zeronolidge.speechapp.ui.platform.PlatformBackHandler
import com.zeronolidge.speechapp.ui.screens.AlphabetScreen
import com.zeronolidge.speechapp.ui.screens.ChapterScreen
import com.zeronolidge.speechapp.ui.screens.VoiceStoryScreen
import com.zeronolidge.speechapp.ui.screens.ClosetScreen
import com.zeronolidge.speechapp.ui.screens.GrownUpsScreen
import com.zeronolidge.speechapp.ui.screens.HeroPickScreen
import com.zeronolidge.speechapp.ui.screens.MapScreen
import com.zeronolidge.speechapp.ui.screens.MicHelloScreen
import com.zeronolidge.speechapp.ui.platform.MicStatus
import com.zeronolidge.speechapp.ui.platform.rememberMicPermission
import com.zeronolidge.speechapp.ui.platform.rememberReduceMotion
import com.zeronolidge.speechapp.ui.art.LocalReduceMotion
import com.zeronolidge.speechapp.ui.screens.NumberLineScreen
import com.zeronolidge.speechapp.ui.screens.ParentGateScreen
import com.zeronolidge.speechapp.ui.screens.PipStoryScreen
import com.zeronolidge.speechapp.ui.screens.EchoCaveScreen
import com.zeronolidge.speechapp.ui.screens.RestScreen
import com.zeronolidge.speechapp.ui.screens.SayStepCheck
import com.zeronolidge.speechapp.ui.screens.SayBeatCheck
import com.zeronolidge.speechapp.ui.screens.PracticeGameScreen
import com.zeronolidge.speechapp.ui.screens.PracticeMenu
import com.zeronolidge.speechapp.ui.screens.LandGamesScreen
import com.zeronolidge.speechapp.ui.screens.StickerBookScreen
import com.zeronolidge.speechapp.ui.screens.TalkMenu
import com.zeronolidge.speechapp.ui.screens.TalkPracticeScreen
import com.zeronolidge.speechapp.ui.theme.Kid
import com.zeronolidge.speechapp.ui.theme.SpeechAppTheme

/** The whole app, shared by Android and the web. Each platform passes in its own [services]. */
@Composable
fun TalkAndLearnApp(
    services: AppServices,
    /** Opens straight onto this screen (above the map), for automatic layout checks. */
    start: Screen? = null,
) {
    SpeechAppTheme {
        CompositionLocalProvider(LocalApp provides services, LocalReduceMotion provides rememberReduceMotion()) {
            var origin by remember { mutableStateOf(Offset.Zero) }
            var screenW by remember { mutableStateOf(0.dp) }
            val density = LocalDensity.current
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Kid.background)
                    .safeDrawingPadding()
                    .onGloballyPositioned {
                        origin = it.positionInWindow()
                        screenW = with(density) { it.size.width.toDp() }
                    }
                    // A hand holding the tablet: touches that start within EdgeIgnore of the screen's
                    // edge are noted first (Initial pass) and ignored by every tap (detectKidTaps).
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val e = awaitPointerEvent(PointerEventPass.Initial)
                                val m = com.zeronolidge.speechapp.ui.components.EdgeIgnore.toPx()
                                e.changes.forEach { c ->
                                    val p = c.position
                                    if (c.changedToDownIgnoreConsumed()) {
                                        if (p.x < m || p.y < m || p.x > size.width - m || p.y > size.height - m) {
                                            com.zeronolidge.speechapp.ui.components.EdgeTouches.ids += c.id.value
                                        }
                                    } else if (!c.pressed) {
                                        com.zeronolidge.speechapp.ui.components.EdgeTouches.ids -= c.id.value
                                    }
                                }
                            }
                        }
                    }
                    // No tap ever does nothing: a tap that nothing tappable took (on scenery, a
                    // picture, empty paper) gets a little paper puff and a soft pop. Watched last
                    // (the Final pass), so buttons, tiles and the map handle their own taps first.
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val e = awaitPointerEvent(PointerEventPass.Final)
                                if (e.changes.any { it.changedToDownIgnoreConsumed() }) services.touches++
                                e.changes.forEach { c ->
                                    if (c.changedToUp()) services.puff(origin.x + c.position.x, origin.y + c.position.y)
                                }
                            }
                        }
                    },
            ) {
                // Word listening: never downloads by itself. Once a grown-up turns it on, the
                // recogniser loads quietly in the background (41 MB the first time, only on Wi-Fi
                // unless they chose any connection; after that from the device), when the
                // microphone is allowed. While it waits for Wi-Fi it checks again every minute.
                val mic = com.zeronolidge.speechapp.ui.platform.rememberMicPermission()
                val pr = services.progress
                LaunchedEffect(pr.wordListening, pr.wordModelWifiOnly, pr.useMic, mic.status) {
                    kotlinx.coroutines.delay(5000)
                    while (pr.wordListening && pr.useMic && mic.granted()) {
                        if (pr.wordModelSaved || !pr.wordModelWifiOnly || !com.zeronolidge.speechapp.ui.platform.AppHome.onMobileData()) {
                            services.voice.words.prepare()
                            // Once it's ready it's saved on the device: later visits load it even on mobile data.
                            var waited = 0
                            while (!services.voice.words.ready && waited < 600) {
                                kotlinx.coroutines.delay(2000)
                                waited++
                            }
                            if (services.voice.words.ready) pr.markWordModelSaved()
                            break
                        }
                        kotlinx.coroutines.delay(60_000)
                    }
                }
                // Rory's voice for offline play: only when a grown-up turned it on.
                LaunchedEffect(pr.saveVoiceOffline) {
                    if (pr.saveVoiceOffline) com.zeronolidge.speechapp.ui.platform.AppHome.saveVoice()
                }
                // Keeps this sitting of play going while he plays (saved, so a reload carries on).
                LaunchedEffect(Unit) {
                    while (true) {
                        kotlinx.coroutines.delay(60_000)
                        services.touchSession()
                    }
                }
                CompositionLocalProvider(LocalScreenWidth provides screenW) { Screens(start) }
                CelebrationOverlay()
                GiftOverlay()
                WordRateBar()
                PuffOverlay(origin)
            }
        }
    }
}

/** The little paper puffs where taps landed on nothing tappable: a burst of paper bits, gone in 0.35 s. */
@Composable
private fun PuffOverlay(origin: Offset) {
    val app = LocalApp.current
    val still = LocalReduceMotion.current
    app.puffs.toList().forEach { (x, y, id) ->
        key(id) {
            val t = remember { Animatable(if (still) 1f else 0f) }
            LaunchedEffect(Unit) {
                if (!still) t.animateTo(1f, tween(350))
                app.puffs.removeAll { it.third == id }
            }
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(x - origin.x, y - origin.y)
                val v = t.value
                val colors = listOf(Color(0xFFFFD54F), Color(0xFF81D4FA), Color(0xFFA5D6A7), Color(0xFFF8BBD0), Color(0xFFFFFFFF))
                for (i in 0 until 7) {
                    val a = i * 0.8976f + id
                    val d = 18.dp.toPx() + 42.dp.toPx() * v
                    drawCircle(
                        colors[i % colors.size].copy(alpha = (1f - v).coerceIn(0f, 1f)),
                        radius = 8.dp.toPx() * (1f - 0.4f * v),
                        center = Offset(c.x + d * kotlin.math.cos(a), c.y + d * kotlin.math.sin(a)),
                    )
                }
            }
        }
    }
}

/** Moves between screens. Every screen gets [Nav], so 🏠 and "back" work the same everywhere. */
class Nav(val go: (Screen) -> Unit, val back: () -> Unit, val home: () -> Unit, val replace: (Screen) -> Unit)

@Composable
private fun Screens(start: Screen? = null) {
    val app = LocalApp.current
    val stack = remember { mutableStateListOf<Screen>(Screen.Map).apply { if (start != null && start != Screen.Map) add(start) } }
    val nav = remember {
        Nav(
            go = { stack.add(it) },
            back = { if (stack.size > 1) stack.removeAt(stack.lastIndex) },
            home = { stack.clear(); stack.add(Screen.Map) },
            replace = { stack.removeAt(stack.lastIndex); stack.add(it) },
        )
    }
    PlatformBackHandler(enabled = stack.size > 1) { nav.back() }

    // First launch, after the first tap: "Can Rory hear you?" asks for the microphone up front,
    // once, so the question never pops up in the middle of a story.
    val mic = rememberMicPermission()
    if (!app.progress.micAsked) {
        if (!app.progress.useMic || mic.status != MicStatus.NOT_ASKED) {
            LaunchedEffect(Unit) { app.progress.markMicAsked() }
        } else {
            MicHelloScreen(onDone = {})
            return
        }
    }

    // First launch: pick a buddy before anything else.
    if (app.progress.hero == null) {
        // A test link on a new device keeps its screen after the buddy is picked.
        HeroPickScreen(onPicked = { nav.home(); if (start != null && start != Screen.Map) stack.add(start) })
        return
    }

    // A fresh key per back-stack entry, so every visit to a game starts clean.
    val s = stack.last()
    // Remembered, so a page that reloads by itself opens back here (see Screen.placeKey).
    LaunchedEffect(s) { app.notePlace(s.placeKey()) }
    // Leaving a screen (🏠 included) always ends its listening turn: the microphone closes and
    // the listening look goes, so nothing from the old screen can start listening on the next.
    androidx.compose.runtime.DisposableEffect(s) {
        onDispose {
            app.voice.cancel()
            app.listening = false
            app.listenLevel = 0f
        }
    }
    key(stack.size, s) {
        Box(Modifier.fillMaxSize().paperEnter(s)) { ShowScreen(s, nav) }
    }
}

@Composable
private fun ShowScreen(s: Screen, nav: Nav) {
    when (s) {
        Screen.Map -> MapScreen(nav)
        is Screen.Chapter -> ChapterScreen(nav, s.visit)
        is Screen.LandGames -> LandGamesScreen(s.land, nav)
        Screen.Closet -> ClosetScreen(nav)
        Screen.Stickers -> StickerBookScreen(nav)
        Screen.Gate -> ParentGateScreen(nav)
        Screen.GrownUps -> GrownUpsScreen(nav)
        Screen.HeroPick -> HeroPickScreen(onPicked = nav.home, onHome = nav.home, onBack = nav.back)
        Screen.Practice -> PracticeMenu(nav)
        is Screen.PracticeGame -> PracticeGameScreen(s.kind, nav)
        Screen.TalkMenu -> TalkMenu(nav)
        is Screen.TalkPractice -> TalkPracticeScreen(s.sound, nav)
        Screen.Alphabet -> AlphabetScreen(nav)
        Screen.NumberLine -> NumberLineScreen(nav)
        Screen.Pip -> PipStoryScreen(nav)
        is Screen.VoiceStory -> VoiceStoryScreen(s.id, nav, s.startBeat)
        is Screen.EchoCave -> EchoCaveScreen(nav, s.rest)
        Screen.Rest -> RestScreen(nav)
        Screen.SayCheck -> SayStepCheck(nav)
        is Screen.SayBeatCheck -> SayBeatCheck(nav, s.word)
    }
}

/**
 * A new paper piece pops up on a card over whatever is on screen. It closes by itself after
 * 4 seconds, or straight away on a tap anywhere.
 */
@Composable
private fun GiftOverlay() {
    val app = LocalApp.current
    val piece = app.gift ?: return
    val p = app.progress
    val species = p.hero ?: Species.KID
    LaunchedEffect(piece) {
        delay(4000)
        if (app.gift == piece) app.closeGift()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x55000000))
            .clickable(remember { MutableInteractionSource() }, indication = null) { app.closeGift() }
            .padding(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        PaperButton(
            PaperStyle.chip,
            Modifier.fillMaxWidth(0.5f).fillMaxHeight(0.8f).widthIn(min = 260.dp).popIn(piece),
            onClick = { app.closeGift() },
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎁", fontSize = 44.sp)
                HeroWithPet(species, p.heroColor(species), p.outfit, p.pet, Modifier.weight(1f).fillMaxWidth(), Mood.CHEER)
                PaperChip(piece.name)
            }
        }
    }
}

/**
 * Grown-up test mode (turned on behind the parent lock): after each try, a small bar asks the
 * grown-up "right" or "off". Only that rating is saved, next to what the recogniser heard (which
 * isn't shown, so it can't sway the rating). It closes by itself after 12 seconds.
 */
@Composable
private fun WordRateBar() {
    val app = LocalApp.current
    if (!app.progress.wordTestMode || !app.rateOpen) return
    LaunchedEffect(app.rateTry) {
        kotlinx.coroutines.delay(12_000)
        app.rateOpen = false
    }
    Box(Modifier.fillMaxSize().padding(top = 70.dp), contentAlignment = Alignment.TopCenter) {
        androidx.compose.foundation.layout.Row(
            Modifier.paperCard(Color(0xFFECEFF1), 18.dp).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Grown-up: that try was", fontSize = 16.sp, color = Kid.ink)
            androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
            com.zeronolidge.speechapp.ui.components.PaperButton(Color(0xFFC8E6C9), Modifier.height(48.dp), onClick = {
                app.progress.rateLastWordTry(true)
                app.rateOpen = false
            }) { Text("👍 Right", fontSize = 18.sp, color = Kid.ink, modifier = Modifier.padding(horizontal = 14.dp)) }
            androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
            com.zeronolidge.speechapp.ui.components.PaperButton(Color(0xFFFFE0B2), Modifier.height(48.dp), onClick = {
                app.progress.rateLastWordTry(false)
                app.rateOpen = false
            }) { Text("✋ Off", fontSize = 18.sp, color = Kid.ink, modifier = Modifier.padding(horizontal = 14.dp)) }
        }
    }
}
