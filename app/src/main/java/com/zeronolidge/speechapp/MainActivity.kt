package com.zeronolidge.speechapp

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.zeronolidge.speechapp.audio.Listener
import com.zeronolidge.speechapp.audio.Parrot
import com.zeronolidge.speechapp.audio.Speaker
import com.zeronolidge.speechapp.state.Progress
import com.zeronolidge.speechapp.ui.Screen
import com.zeronolidge.speechapp.ui.components.AppServices
import com.zeronolidge.speechapp.ui.components.CelebrationOverlay
import com.zeronolidge.speechapp.ui.components.LocalApp
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
import com.zeronolidge.speechapp.ui.theme.SpeechAppTheme

class MainActivity : ComponentActivity() {
    private lateinit var services: AppServices

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val progress = Progress(this)
        services = AppServices(
            speaker = Speaker(this).also { it.rate = progress.speechRate },
            listener = Listener(this),
            parrot = Parrot(this),
            progress = progress,
        )
        setContent {
            SpeechAppTheme {
                CompositionLocalProvider(LocalApp provides services) {
                    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                        App()
                        CelebrationOverlay()
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        services.speaker.stop()
        services.listener.cancel()
        services.parrot.stop()
    }

    override fun onDestroy() {
        services.speaker.shutdown()
        services.listener.cancel()
        services.parrot.stop()
        super.onDestroy()
    }
}

@Composable
private fun App() {
    val app = LocalApp.current
    val stack = remember { mutableStateListOf<Screen>(Screen.Map) }
    val go: (Screen) -> Unit = { stack.add(it) }
    val back: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    BackHandler(enabled = stack.size > 1) { back() }

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
