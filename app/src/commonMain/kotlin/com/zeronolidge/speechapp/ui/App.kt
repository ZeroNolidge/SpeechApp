package com.zeronolidge.speechapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Column
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
        CompositionLocalProvider(LocalApp provides services) {
            Box(Modifier.fillMaxSize().background(Kid.background).safeDrawingPadding()) {
                Screens()
                CelebrationOverlay()
                AudioTroubleButton()
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
        Screen.Chapter -> ChapterScreen(nav)
        Screen.Closet -> ClosetScreen(nav)
        Screen.Stickers -> StickerBookScreen(nav)
        Screen.Gate -> ParentGateScreen(nav)
        Screen.GrownUps -> GrownUpsScreen(nav)
        Screen.HeroPick -> HeroPickScreen(onPicked = nav.home, onHome = nav.home)
        Screen.Practice -> PracticeMenu(nav)
        is Screen.PracticeGame -> PracticeGameScreen(s.kind, nav)
        Screen.TalkMenu -> TalkMenu(nav)
        is Screen.TalkPractice -> TalkPracticeScreen(s.sound, nav)
        Screen.Alphabet -> AlphabetScreen(nav)
        Screen.NumberLine -> NumberLineScreen(nav)
    }
}

/**
 * If speech didn't start or finish in time, a big pulsing speaker appears at the bottom. Tapping
 * it says the instructions again and hides it. The story never waits for it.
 */
@Composable
private fun AudioTroubleButton() {
    val app = LocalApp.current
    if (!app.audioTrouble) return
    Box(Modifier.fillMaxSize().padding(bottom = 24.dp), contentAlignment = Alignment.BottomCenter) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PaperIconButton("🔊", PaperColorsUi.hear, size = 120.dp, modifier = Modifier.popIn("trouble")) {
                app.audioTrouble = false
                app.repeatInstruction()
            }
            PointingHand(size = 56.dp)
        }
    }
}
