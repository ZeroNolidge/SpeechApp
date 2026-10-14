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
import com.zeronolidge.speechapp.audio.WebListener
import com.zeronolidge.speechapp.audio.WebParrot
import com.zeronolidge.speechapp.audio.QueueSpeaker
import com.zeronolidge.speechapp.audio.WebVoiceEngine
import com.zeronolidge.speechapp.res.Res
import com.zeronolidge.speechapp.res.noto_color_emoji
import com.zeronolidge.speechapp.state.Progress
import com.zeronolidge.speechapp.state.WebPrefs
import com.zeronolidge.speechapp.ui.TalkAndLearnApp
import com.zeronolidge.speechapp.ui.components.AppServices
import com.zeronolidge.speechapp.ui.components.GoButton
import com.zeronolidge.speechapp.ui.theme.Kid
import com.zeronolidge.speechapp.ui.theme.SpeechAppTheme
import kotlinx.browser.document
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont

/** The web (PWA) version: the same app as Android, with the browser's speech and storage. */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val progress = Progress(WebPrefs())
    val services = AppServices(
        speaker = QueueSpeaker(WebVoiceEngine()).also { it.rate = progress.speechRate },
        listener = WebListener(),
        parrot = WebParrot(),
        progress = progress,
    )
    hideLoading()
    ComposeViewport(document.body!!) { WebRoot(services) }
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
        !started || !fontsReady -> StartScreen(ready = fontsReady) {
            services.speaker.say("Hi friend! Let's play!")
            started = true
        }
        else -> TalkAndLearnApp(services)
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
