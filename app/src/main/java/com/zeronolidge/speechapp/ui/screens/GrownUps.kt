package com.zeronolidge.speechapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.data.SpeechContent
import com.zeronolidge.speechapp.state.Progress
import com.zeronolidge.speechapp.ui.components.KidScreen
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.theme.Kid

private val areaNames = mapOf(
    Progress.AREA_SPEECH to "🗣️ Talking",
    Progress.AREA_LETTERS to "🔤 Letters",
    Progress.AREA_READING to "📖 Reading",
    Progress.AREA_NUMBERS to "🔢 Numbers",
    Progress.AREA_PATTERNS to "🧩 Patterns",
    Progress.AREA_ROBOT to "🤖 Robot",
)

@Composable
fun GrownUpsScreen(back: () -> Unit) {
    val app = LocalApp.current
    val p = app.progress
    var confirmReset by remember { mutableStateOf(false) }
    // Read stars so the per-area numbers refresh after a reset.
    val total = p.stars

    KidScreen("Grown-ups ⚙️", Color(0xFFCFD8DC), back) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(Modifier.weight(1f)) {
                Heading("Settings")
                OutlinedTextField(
                    value = p.childName,
                    onValueChange = { p.updateChildName(it.take(20)) },
                    label = { Text("Child's name (used in the stories)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Open every land (skip level locks)", fontSize = 18.sp, modifier = Modifier.weight(1f))
                    Switch(checked = p.openAllLands, onCheckedChange = { p.updateOpenAllLands(it) })
                }
                Text("Speech sounds to focus on", fontSize = 18.sp)
                Text(
                    if (p.focusSounds.isEmpty()) "None picked, so stories use every sound." else "Stories only use the sounds you picked.",
                    fontSize = 14.sp,
                    color = Kid.ink.copy(alpha = 0.7f),
                )
                SoundChips(p.focusSounds) { p.toggleFocusSound(it) }
                Text("Voice speed: ${"%.2f".format(p.speechRate)}", fontSize = 18.sp)
                Slider(
                    value = p.speechRate,
                    onValueChange = { p.updateSpeechRate(it); app.speaker.rate = it },
                    onValueChangeFinished = { app.speaker.say("This is how fast I talk.") },
                    valueRange = 0.5f..1.2f,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Use the microphone", fontSize = 18.sp, modifier = Modifier.weight(1f))
                    Switch(checked = p.useMic, onCheckedChange = { p.updateUseMic(it) })
                }
                Text(
                    if (app.listener.available) "Speech recognition is available on this device."
                    else "Speech recognition isn't available on this device. Use the 👍 button to reward good tries.",
                    fontSize = 14.sp,
                    color = Kid.ink.copy(alpha = 0.7f),
                )
                Spacer(Modifier.padding(6.dp))
                OutlinedButton(onClick = { confirmReset = true }) { Text("Reset all progress") }
            }
            Card(Modifier.weight(1f)) {
                Heading("Progress")
                Text(
                    "${p.heroName} the ${p.hero?.label ?: "hero"}: level ${p.heroLevel}, ${p.xp} XP, ${p.gems} 💎, ${p.questsDone} quests",
                    fontSize = 18.sp,
                )
                Text("Correct answers by area — $total ⭐ in total", fontSize = 16.sp, color = Kid.ink.copy(alpha = 0.7f))
                Progress.areas.forEach { area ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(areaNames.getValue(area), fontSize = 18.sp, modifier = Modifier.weight(1f))
                        Text("${p.areaStars(area)} ⭐", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.padding(6.dp))
                Heading("Tips")
                Tip("Sit with your child for the Talk games. The speech recognizer can struggle with young voices, so use 👍 “Said it!” whenever a try is good.")
                Tip("🦜 “Copy me” records three seconds and plays it back. Hearing themselves helps children notice how they sound.")
                Tip("👄 “How?” explains where the lips and tongue go. Make the sound together, facing each other, so they can watch your mouth.")
                Tip("Each quest is a short story with 3–5 challenges and takes about 5 minutes. Every quest includes some talking practice. Use “Speech sounds to focus on” to match what the speech therapist is working on.")
                Tip("Keep sessions short: 5–10 minutes a day works better than one long session.")
                Tip("This app is for practice and fun. It doesn't replace a speech-language pathologist. Ask them which sounds to focus on.")
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset progress?") },
            text = { Text("This clears the hero, levels, gems, items and stars. Settings are kept.") },
            confirmButton = {
                Button(onClick = { p.reset(); confirmReset = false }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SoundChips(selected: Set<String>, onToggle: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SpeechContent.sounds.forEach { s ->
            val on = s.id in selected
            Text(
                "${s.emoji} ${s.label}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (on) Color.White else Kid.ink,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) Color(0xFF7C4DFF) else Color(0xFFEDE7F6))
                    .clickable { onToggle(s.id) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun Card(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun Heading(text: String) {
    Text(text, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Kid.ink)
}

@Composable
private fun Tip(text: String) {
    Row {
        Text("•", fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 16.sp, color = Kid.ink)
    }
}
