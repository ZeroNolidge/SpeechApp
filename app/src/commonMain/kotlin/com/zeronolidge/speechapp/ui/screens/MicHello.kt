package com.zeronolidge.speechapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.ui.art.Critter
import com.zeronolidge.speechapp.ui.art.Mood
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.PaperButton
import com.zeronolidge.speechapp.ui.components.PaperChip
import com.zeronolidge.speechapp.ui.components.PaperIconButton
import com.zeronolidge.speechapp.ui.components.PointingHand
import com.zeronolidge.speechapp.ui.components.ScreenFrame
import com.zeronolidge.speechapp.ui.components.SpeakOnOpen
import com.zeronolidge.speechapp.ui.components.popIn
import com.zeronolidge.speechapp.ui.platform.rememberMicPermission
import com.zeronolidge.speechapp.ui.theme.Kid
import androidx.compose.material3.Text

/**
 * First launch only: Rory asks "Can Rory hear you?". Tapping the big microphone asks for the
 * microphone (so the question never pops up mid-story); "Not now" carries on without it. Either
 * way it's never shown again, and the grown-ups page can turn the microphone on later.
 */
@Composable
fun MicHelloScreen(onDone: () -> Unit) {
    val app = LocalApp.current
    val mic = rememberMicPermission()
    var asking by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    val line = "Can Rory hear you? Tap the big microphone! A grown-up can tap Allow."
    SpeakOnOpen(line)
    fun finish(heard: Boolean) {
        if (done) return
        done = true
        app.progress.markMicAsked()
        app.speaker.say(if (heard) "Yay! Rory can hear you!" else "That's okay! Let's play!")
        onDone()
    }
    ScreenFrame(onHome = null) {
        Row(
            Modifier.fillMaxSize().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                PaperChip("👂 Can Rory hear you?", fontSize = 26.sp)
                Spacer(Modifier.height(12.dp))
                Critter(Species.ROBOT, Modifier.weight(1f).fillMaxWidth(), Color(0xFFFFCC80), mood = Mood.CHEER, talking = true)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                PaperIconButton("🎤", Color(0xFFFFE0B2), size = 150.dp, enabled = !asking, modifier = Modifier.popIn("mic")) {
                    asking = true
                    mic.request { ok -> finish(ok) }
                }
                PointingHand(size = 56.dp)
                Spacer(Modifier.height(20.dp))
                // For the grown-up: carry on without the microphone.
                PaperButton(Kid.soft, Modifier.height(64.dp), corner = 999.dp, onClick = { finish(false) }) {
                    Box(Modifier.padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
                        Text("Not now", fontSize = 22.sp, color = Kid.ink, maxLines = 1)
                    }
                }
            }
        }
    }
}
