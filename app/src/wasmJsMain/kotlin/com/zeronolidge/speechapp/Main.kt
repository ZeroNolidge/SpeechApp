package com.zeronolidge.speechapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ComposeViewport
import com.zeronolidge.speechapp.audio.VoiceDetector
import com.zeronolidge.speechapp.audio.WebMicLevels
import com.zeronolidge.speechapp.audio.WebParrot
import com.zeronolidge.speechapp.audio.QueueSpeaker
import com.zeronolidge.speechapp.audio.WebVoiceEngine
import com.zeronolidge.speechapp.audio.ClipVoiceEngine
import com.zeronolidge.speechapp.audio.WebClips
import com.zeronolidge.speechapp.res.Res
import com.zeronolidge.speechapp.res.noto_color_emoji
import com.zeronolidge.speechapp.state.Progress
import com.zeronolidge.speechapp.state.WebPrefs
import com.zeronolidge.speechapp.ui.TalkAndLearnApp
import com.zeronolidge.speechapp.ui.components.AppServices
import com.zeronolidge.speechapp.ui.components.GoButton
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.theme.Kid
import com.zeronolidge.speechapp.ui.platform.MicStatus
import com.zeronolidge.speechapp.ui.Screen
import com.zeronolidge.speechapp.ui.screens.PracticeKind
import com.zeronolidge.speechapp.logic.Land
import com.zeronolidge.speechapp.data.SpeechContent
import com.zeronolidge.speechapp.ui.platform.WebMic
import com.zeronolidge.speechapp.ui.theme.SpeechAppTheme
import kotlinx.browser.document
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont

/** The web (PWA) version: the same app as Android, with the browser's speech and storage. */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val progress = Progress(WebPrefs())
    val services = AppServices(
        // Rory's pre-made voice clips where they exist, the browser's voice for anything else.
        speaker = QueueSpeaker(ClipVoiceEngine(WebClips(), WebVoiceEngine())).also { it.rate = progress.speechRate },
        voice = VoiceDetector(WebMicLevels()),
        parrot = WebParrot(),
        progress = progress,
        sfx = com.zeronolidge.speechapp.audio.WebSfx(),
    )
    hideLoading()
    ComposeViewport(document.body!!) { WebRoot(services) }
}

private fun locationHash(): String = js("(() => { try { return window.location.hash || ''; } catch (e) { return ''; } })()")

/**
 * For the automatic screenshot check only: "#test=ADD" opens that practice game and
 * "#test=land-MEADOW" an area's games, so layouts can be checked at different screen sizes.
 */
private fun testStart(): Screen? {
    val v = locationHash().removePrefix("#test=").takeIf { locationHash().startsWith("#test=") } ?: return null
    return runCatching {
        when {
            v.startsWith("land-") -> Screen.LandGames(Land.valueOf(v.removePrefix("land-")))
            v == "talk" -> Screen.TalkPractice(SpeechContent.sounds.first())
            v == "pip" -> Screen.Pip
            v == "say" -> Screen.SayCheck
            v == "cave" -> Screen.EchoCave()
            v == "rest" -> Screen.Rest
            else -> Screen.PracticeGame(PracticeKind.valueOf(v))
        }
    }.getOrNull()
}

private fun hideLoading(): Unit = js("(() => { const l = document.getElementById('loading'); if (l) l.remove(); })()")

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun WebRoot(services: AppServices) {
    // Browsers draw text with their own fonts, which have no emoji. Load an emoji font
    // (trimmed to just the emoji this app uses) and register it as a fallback.
    val emoji by preloadFont(Res.font.noto_color_emoji)
    var fontsReady by remember { mutableStateOf(false) }
    val resolver = LocalFontFamilyResolver.current
    LaunchedEffect(emoji) {
        emoji?.let {
            resolver.preload(FontFamily(it))
            fontsReady = true
        }
    }
    // Browsers only let a page talk after the first tap, so start with a big "play" button.
    var started by remember { mutableStateOf(false) }
    when {
        // The start screen's ▶ is a paper button, which reads the app's services (its crinkle).
        !started || !fontsReady -> CompositionLocalProvider(LocalApp provides services) { StartScreen(ready = fontsReady) {
            // First launch: the next screen ("Can Rory hear you?") asks for the microphone. After
            // that, if the browser has forgotten the answer, ask again here at the start, so the
            // question never pops up in the middle of a story.
            if (services.progress.micAsked) {
                services.speaker.say("Hi friend! Let's play!")
                if (services.progress.useMic && WebMic.status == MicStatus.NOT_ASKED) WebMic.request { }
            }
            started = true
        } }
        else -> TalkAndLearnApp(services, start = remember { testStart() })
    }
}

@Composable
private fun StartScreen(ready: Boolean, onStart: () -> Unit) {
    SpeechAppTheme {
        Box(Modifier.fillMaxSize().background(Kid.background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Talk & Learn", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = Kid.ink)
                Spacer(Modifier.height(24.dp))
                if (ready) GoButton(size = 130.dp, onClick = onStart)
                else Text("Loading…", fontSize = 24.sp, color = Kid.ink)
            }
        }
    }
}
