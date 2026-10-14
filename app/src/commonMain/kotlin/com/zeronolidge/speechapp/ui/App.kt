package com.zeronolidge.speechapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.zeronolidge.speechapp.ui.art.PaperGrain
import com.zeronolidge.speechapp.ui.components.AppServices
import com.zeronolidge.speechapp.ui.components.CelebrationOverlay
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.platform.PlatformBackHandler
import com.zeronolidge.speechapp.ui.screens.AlphabetScreen
import com.zeronolidge.speechapp.ui.screens.GrownUpsScreen
import com.zeronolidge.speechapp.ui.screens.HeroPickScreen
import com.zeronolidge.speechapp.ui.screens.MapScreen
import com.zeronolidge.speechapp.ui.screens.NumberLineScreen
import com.zeronolidge.speechapp.ui.screens.PracticeGameScreen
import com.zeronolidge.speechapp.ui.screens.PracticeMenu
import com.zeronolidge.speechapp.ui.screens.QuestScreen
import com.zeronolidge.speechapp.ui.screens.TalkMenu
import com.zeronolidge.speechapp.ui.screens.TalkPracticeScreen
import com.zeronolidge.speechapp.ui.screens.WardrobeScreen
import com.zeronolidge.speechapp.ui.theme.Kid
import com.zeronolidge.speechapp.ui.theme.SpeechAppTheme

/** The whole app, shared by Android and the web. Each platform passes in its own [services]. */
@Composable
fun TalkAndLearnApp(services: AppServices) {
    SpeechAppTheme {
        CompositionLocalProvider(LocalApp provides services) {
            Box(Modifier.fillMaxSize().background(Kid.background).safeDrawingPadding()) {
                Screens()
                // Paper grain over everything, so the whole app feels like paper.
                PaperGrain()
                CelebrationOverlay()
            }
        }
    }
}

@Composable
private fun Screens() {
    val app = LocalApp.current
    val stack = remember { mutableStateListOf<Screen>(Screen.Map) }
    val go: (Screen) -> Unit = { stack.add(it) }
    val back: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    PlatformBackHandler(enabled = stack.size > 1) { back() }

    // First launch: pick a hero before anything else.
    if (app.progress.hero == null) {
        HeroPickScreen(onPicked = { stack.clear(); stack.add(Screen.Map) })
        return
    }

    // A fresh key per back-stack entry, so every visit to a game starts clean.
    val s = stack.last()
    key(stack.size, s) { ShowScreen(s, go, back) }
}

@Composable
private fun ShowScreen(s: Screen, go: (Screen) -> Unit, back: () -> Unit) {
    when (s) {
        Screen.Map -> MapScreen(go)
        is Screen.Quest -> QuestScreen(s.land, s.mixed, back, go)
        Screen.Wardrobe -> WardrobeScreen(back)
        Screen.GrownUps -> GrownUpsScreen(back)
        Screen.Practice -> PracticeMenu(go, back)
        is Screen.PracticeGame -> PracticeGameScreen(s.kind, back)
        Screen.TalkMenu -> TalkMenu(go, back)
        is Screen.TalkPractice -> TalkPracticeScreen(s.sound, back)
        Screen.Alphabet -> AlphabetScreen(back)
        Screen.NumberLine -> NumberLineScreen(back)
    }
}
