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
import androidx.compose.runtime.LaunchedEffect
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
import com.zeronolidge.speechapp.ui.screens.CountingScreen
import com.zeronolidge.speechapp.ui.screens.GrownUpsScreen
import com.zeronolidge.speechapp.ui.screens.HomeScreen
import com.zeronolidge.speechapp.ui.screens.LetterGameScreen
import com.zeronolidge.speechapp.ui.screens.LettersMenu
import com.zeronolidge.speechapp.ui.screens.MathScreen
import com.zeronolidge.speechapp.ui.screens.MoreOrLessScreen
import com.zeronolidge.speechapp.ui.screens.NumberLineScreen
import com.zeronolidge.speechapp.ui.screens.NumbersMenu
import com.zeronolidge.speechapp.ui.screens.OddOneOutScreen
import com.zeronolidge.speechapp.ui.screens.PatternsMenu
import com.zeronolidge.speechapp.ui.screens.ReadMenu
import com.zeronolidge.speechapp.ui.screens.RobotGameScreen
import com.zeronolidge.speechapp.ui.screens.RobotMenu
import com.zeronolidge.speechapp.ui.screens.SoundItOutScreen
import com.zeronolidge.speechapp.ui.screens.TalkMenu
import com.zeronolidge.speechapp.ui.screens.TalkPracticeScreen
import com.zeronolidge.speechapp.ui.screens.WhatsNextScreen
import com.zeronolidge.speechapp.ui.screens.WhichWordScreen
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
    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val go: (Screen) -> Unit = { stack.add(it) }
    val back: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    BackHandler(enabled = stack.size > 1) { back() }

    val app = LocalApp.current
    LaunchedEffect(Unit) { app.speaker.say("Hi friend! What do you want to play?") }

    // A fresh key per back-stack entry, so every visit to a game starts clean.
    val s = stack.last()
    key(stack.size, s) { ShowScreen(s, go, back) }
}

@Composable
private fun ShowScreen(s: Screen, go: (Screen) -> Unit, back: () -> Unit) {
    when (s) {
        Screen.Home -> HomeScreen(go)
        Screen.GrownUps -> GrownUpsScreen(back)
        Screen.TalkMenu -> TalkMenu(go, back)
        is Screen.TalkPractice -> TalkPracticeScreen(s.sound, back)
        Screen.LettersMenu -> LettersMenu(go, back)
        Screen.Alphabet -> AlphabetScreen(back)
        is Screen.LetterGame -> LetterGameScreen(s.mode, back)
        Screen.ReadMenu -> ReadMenu(go, back)
        Screen.SoundItOut -> SoundItOutScreen(back)
        Screen.WhichWord -> WhichWordScreen(back)
        Screen.NumbersMenu -> NumbersMenu(go, back)
        Screen.NumberLine -> NumberLineScreen(back)
        Screen.Counting -> CountingScreen(back)
        is Screen.MathGame -> MathScreen(s.plus, back)
        Screen.MoreOrLess -> MoreOrLessScreen(back)
        Screen.PatternsMenu -> PatternsMenu(go, back)
        Screen.WhatsNext -> WhatsNextScreen(back)
        Screen.OddOneOutGame -> OddOneOutScreen(back)
        Screen.RobotMenu -> RobotMenu(go, back)
        is Screen.RobotGame -> RobotGameScreen(s.relative, back)
    }
}
