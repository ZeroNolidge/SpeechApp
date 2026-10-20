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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.logic.Trail
import com.zeronolidge.speechapp.ui.art.Critter
import com.zeronolidge.speechapp.ui.art.IslandMap
import com.zeronolidge.speechapp.ui.art.Mood
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
        voice = VoiceDetector(WebMicLevels(), com.zeronolidge.speechapp.audio.WebWords()),
        parrot = WebParrot(),
        progress = progress,
        sfx = com.zeronolidge.speechapp.audio.WebSfx(),
    )
    hideLoading()
    ComposeViewport(document.body!!) { WebRoot(services) }
}

// The page's #test=... hook. Kept for this tab (sessionStorage) too, so a hard reload that loses
// the hash still opens the same test screen; a plain link with no hook clears it.
private fun locationHash(): String = js("""(() => {
    try {
        const h = window.location.hash || '';
        if (h.startsWith('#test=')) { sessionStorage.setItem('tlTest', h); return h; }
        if (h === '' && performance && performance.getEntriesByType) {
            const nav = performance.getEntriesByType('navigation')[0];
            if (nav && nav.type === 'reload') return sessionStorage.getItem('tlTest') || '';
        }
        sessionStorage.removeItem('tlTest');
        return h;
    } catch (e) { return ''; }
})()""")

/**
 * For the automatic screenshot check only: "#test=ADD" opens that practice game and
 * "#test=land-MEADOW" an area's games, so layouts can be checked at different screen sizes.
 */
private fun testStart(hash: String): Screen? {
    val v = hash.removePrefix("#test=").removeSuffix("+words").takeIf { hash.startsWith("#test=") } ?: return null
    return runCatching {
        when {
            v.startsWith("land-") -> Screen.LandGames(Land.valueOf(v.removePrefix("land-")))
            v == "talk" -> Screen.TalkPractice(SpeechContent.sounds.first())
            v == "pip" -> Screen.Pip
            v == "say" -> Screen.SayCheck
            v == "cave" -> Screen.EchoCave()
            v == "rest" -> Screen.Rest
            // "story-balloon@3": that story, starting at its 4th beat.
            v.startsWith("story-") -> v.removePrefix("story-").split("@").let { Screen.VoiceStory(it[0], it.getOrNull(1)?.toIntOrNull() ?: 0) }
            v.startsWith("saybeat-") -> Screen.SayBeatCheck(v.removePrefix("saybeat-"))
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
    // Browsers (iPad Safari above all) only let a page talk after a tap, so it always starts with
    // a big "play" button, after a reload too. If the page reloaded by itself mid-play (the
    // tablet's screen went off and the browser dropped it), that tap opens straight back where
    // he was.
    val testHash = remember { locationHash() }
    val resume = remember {
        if (testHash.startsWith("#test=") || !services.progress.micAsked || services.progress.hero == null) null
        else services.placeToResume()
    }
    LaunchedEffect(Unit) { services.testMode = testHash.startsWith("#test=") }
    var started by remember { mutableStateOf(false) }
    // The tap is taken at once: Rory waves and the button pops away in the very next frame, and
    // only then (two frames later, so that wave is on screen) is the rest of the app built, which
    // can take a few seconds on a tablet.
    var starting by remember { mutableStateOf(false) }
    LaunchedEffect(starting) {
        if (!starting) return@LaunchedEffect
        withFrameNanos { }
        println("STARTFEEDBACK")
        withFrameNanos { }
        started = true
    }
    when {
        // The start screen's ▶ is a paper button, which reads the app's services (its crinkle).
        !started || !fontsReady -> CompositionLocalProvider(LocalApp provides services) {
            // While he looks at the start screen, the map is already being built underneath (out
            // of sight, taking no taps), so it's ready when he taps. Only when the map comes next.
            val mapNext = fontsReady && resume == null && !testHash.startsWith("#test=") &&
                services.progress.micAsked && services.progress.hero != null
            Box(Modifier.fillMaxSize()) {
                if (mapNext) WarmMap(services)
                StartScreen(ready = fontsReady, starting = starting) {
                    if (starting) return@StartScreen
                    println("STARTTAP")
                    services.sfx.crinkle()
                    // First launch: the next screen ("Can Rory hear you?") asks for the microphone. After
                    // that, if the browser has forgotten the answer, ask again here at the start, so the
                    // question never pops up in the middle of a story.
                    if (services.progress.micAsked) {
                        services.speaker.say("Hi friend! Let's play!")
                        if (services.progress.useMic && WebMic.status == MicStatus.NOT_ASKED) WebMic.request { }
                    }
                    starting = true
                    // Automatic check only ("#test=...+words"): load word listening straight away.
                    if (testHash.endsWith("+words")) services.voice.words.prepare()
                }
            }
        }
        else -> TalkAndLearnApp(services, start = remember { testStart(testHash) ?: resume })
    }
}

/** The map, drawn once under the start screen so its paper art and code are warmed up. */
@Composable
private fun WarmMap(services: AppServices) {
    val chapter = services.progress.chapter
    IslandMap(
        chapter = chapter,
        reached = { it in Trail.reached(chapter) },
        unfoldNew = false,
        onUnfolded = {},
        onStop = {},
        onElsewhere = {},
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun StartScreen(ready: Boolean, starting: Boolean, onStart: () -> Unit) {
    SpeechAppTheme {
        // Opaque and taking every tap (so the map being built underneath never gets one): a tap
        // anywhere starts, not only on ▶.
        Box(
            Modifier.fillMaxSize().background(Kid.background).pointerInput(ready) { detectTapGestures { if (ready) onStart() } },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Rory's Talk Trail", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = Kid.ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Rory is there from the start (already drawn), and waves the moment ▶ is tapped.
                    if (ready) Critter(
                        Species.ROBOT, Modifier.size(width = 110.dp, height = 140.dp), Color(0xFFFFCC80),
                        mood = if (starting) Mood.CHEER else Mood.HAPPY, waving = starting,
                    )
                    Box(Modifier.size(width = 150.dp, height = 210.dp), contentAlignment = Alignment.Center) {
                        when {
                            !ready -> Text("Loading…", fontSize = 24.sp, color = Kid.ink)
                            !starting -> GoButton(size = 130.dp, onClick = onStart)
                        }
                    }
                }
            }
        }
    }
}
