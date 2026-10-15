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
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.zeronolidge.speechapp.ui.components.PaperColorsUi
import com.zeronolidge.speechapp.ui.components.PaperIconButton
import com.zeronolidge.speechapp.ui.components.PointingHand
import com.zeronolidge.speechapp.ui.components.popIn
import androidx.compose.runtime.Composable
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
import com.zeronolidge.speechapp.ui.screens.PracticeGameScreen
import com.zeronolidge.speechapp.ui.screens.PracticeMenu
import com.zeronolidge.speechapp.ui.screens.StickerBookScreen
import com.zeronolidge.speechapp.ui.screens.TalkMenu
import com.zeronolidge.speechapp.ui.screens.TalkPracticeScreen
import com.zeronolidge.speechapp.ui.theme.Kid
import com.zeronolidge.speechapp.ui.theme.SpeechAppTheme

/** The whole app, shared by Android and the web. Each platform passes in its own [services]. */
@Composable
fun TalkAndLearnApp(services: AppServices) {
    SpeechAppTheme {
        CompositionLocalProvider(LocalApp provides services, LocalReduceMotion provides rememberReduceMotion()) {
            Box(Modifier.fillMaxSize().background(Kid.background).safeDrawingPadding()) {
                Screens()
                CelebrationOverlay()
                GiftOverlay()
            }
        }
    }
}

/** Moves between screens. Every screen gets [Nav], so 🏠 and "back" work the same everywhere. */
class Nav(val go: (Screen) -> Unit, val back: () -> Unit, val home: () -> Unit, val replace: (Screen) -> Unit)

@Composable
private fun Screens() {
    val app = LocalApp.current
    val stack = remember { mutableStateListOf<Screen>(Screen.Map) }
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
        HeroPickScreen(onPicked = { nav.home() })
        return
    }

    // A fresh key per back-stack entry, so every visit to a game starts clean.
    val s = stack.last()
    key(stack.size, s) {
        Box(Modifier.fillMaxSize().paperEnter(s)) { ShowScreen(s, nav) }
    }
}

@Composable
private fun ShowScreen(s: Screen, nav: Nav) {
    when (s) {
        Screen.Map -> MapScreen(nav)
        is Screen.Chapter -> ChapterScreen(nav, s.visit)
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
