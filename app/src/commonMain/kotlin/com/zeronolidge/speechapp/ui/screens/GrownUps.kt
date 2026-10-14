package com.zeronolidge.speechapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.zeronolidge.speechapp.logic.Trail
import com.zeronolidge.speechapp.logic.WordStat
import com.zeronolidge.speechapp.logic.WordStatus
import com.zeronolidge.speechapp.state.Progress
import com.zeronolidge.speechapp.state.today
import com.zeronolidge.speechapp.ui.Nav
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.PaperChip
import com.zeronolidge.speechapp.ui.components.ScreenFrame
import com.zeronolidge.speechapp.ui.components.SpeakOnOpen
import com.zeronolidge.speechapp.ui.theme.Kid
import kotlin.math.roundToInt

/** Pictures a grown-up can pick for their own words. */
private val ownWordPictures = listOf("🧑", "👩", "👨", "👵", "👴", "👶", "🐶", "🐱", "🧸", "🚗", "⚽", "🦖", "🍕", "⭐")

/** Behind the grown-up check: settings, the words the child has tried, and the grown-up's own words. */
@Composable
fun GrownUpsScreen(nav: Nav) {
    val app = LocalApp.current
    val p = app.progress
    var confirmReset by remember { mutableStateOf(false) }
    SpeakOnOpen("Grown-ups page.")

    ScreenFrame(onHome = nav.home, top = { PaperChip("Grown-ups ⚙️", fontSize = 24.sp) }) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(Modifier.weight(1f)) {
                Heading("Words ${p.heroName} has practised")
                WordsReport()
                Spacer(Modifier.padding(4.dp))
                Heading("Add your own words")
                OwnWords()
            }
            Card(Modifier.weight(1f)) {
                Heading("Settings")
                OutlinedTextField(
                    value = p.childName,
                    onValueChange = { p.updateChildName(it.take(20)) },
                    label = { Text("Child's name (used in the stories)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Speech sounds to focus on", fontSize = 18.sp)
                Text(
                    if (p.focusSounds.isEmpty()) "None picked, so stories use every sound." else "Stories use the sounds you picked.",
                    fontSize = 14.sp,
                    color = Kid.ink.copy(alpha = 0.7f),
                )
                SoundChips(p.focusSounds) { p.toggleFocusSound(it) }
                Text("Voice speed: ${(p.speechRate * 100).roundToInt() / 100.0}", fontSize = 18.sp)
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
                    if (app.listener.available) "Speech recognition is available. Any honest try moves the story on; a word heard clearly earns a shiny sticker."
                    else "Speech recognition isn't available here, so the app models each word and your child taps the green arrow after saying it.",
                    fontSize = 14.sp,
                    color = Kid.ink.copy(alpha = 0.7f),
                )
                Heading("Progress")
                val lap = p.chapter / Trail.size
                Text(
                    "${p.chapter} chapters finished" + (if (lap > 0) " (${lap} trips around the island)" else "") +
                        " · ${p.pieces.size - 1} paper pieces · ${p.words.stickers.size} stickers",
                    fontSize = 16.sp,
                )
                Spacer(Modifier.padding(4.dp))
                OutlinedButton(onClick = { confirmReset = true }) { Text("Reset all progress") }
                Heading("Tips")
                Tip("Sit with your child for speaking. Say the word together, face to face, so they can watch your mouth.")
                Tip("The microphone often misses young voices. That's fine: any try moves the story on. Shiny stickers are a bonus.")
                Tip("Each chapter takes about 3 to 5 minutes and ends with “see you next time”. One or two chapters a day is plenty.")
                Tip("Words he's tried come back on later days, and the trickiest come back first.")
                Tip("This app is for practice and fun. It doesn't replace a speech-language pathologist. Ask them which sounds to focus on, and add their words above.")
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset progress?") },
            text = { Text("This clears the buddy, the trail, paper pieces, stickers and word history. Settings and your own words are kept.") },
            confirmButton = {
                Button(onClick = { p.reset(); confirmReset = false }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
            },
        )
    }
}

/** Words grouped by how they're going, with when each was last practised. No scores. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordsReport() {
    val p = LocalApp.current.progress
    val book = p.words
    if (book.words.isEmpty()) {
        Text("No words yet. Words show up here after the first chapter.", fontSize = 16.sp, color = Kid.ink.copy(alpha = 0.7f))
        return
    }
    val now = today()
    val groups = listOf(
        WordStatus.STILL_TRICKY to "Still tricky (the microphone keeps missing these, so practise them together)",
        WordStatus.GOING_WELL to "Going well",
        WordStatus.JUST_STARTED to "Just started",
        WordStatus.NOT_HEARD to "Tried, but the microphone hasn't caught them yet",
    )
    groups.forEach { (status, title) ->
        val ws = book.stickers.filter { book.status(it) == status }
        if (ws.isNotEmpty()) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ws.forEach { WordChip(it, now) }
            }
        }
    }
}

@Composable
private fun WordChip(w: WordStat, now: Int) {
    val ago = when (val d = now - w.lastDay) {
        0 -> "today"
        1 -> "yesterday"
        else -> "$d days ago"
    }
    Text(
        "${w.emoji} ${w.text} · $ago",
        fontSize = 15.sp,
        color = Kid.ink,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFF3ECDD))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Up to three of the grown-up's own words (family names, favourite things), each with a picture. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OwnWords() {
    val app = LocalApp.current
    val p = app.progress
    var text by remember { mutableStateOf("") }
    var pic by remember { mutableStateOf(ownWordPictures.first()) }
    Text(
        "Family names or favourite things. They come up often in the stories. Up to ${Progress.MAX_CUSTOM}.",
        fontSize = 14.sp,
        color = Kid.ink.copy(alpha = 0.7f),
    )
    p.customWords.forEach { (w, e) ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$e $w", fontSize = 18.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = { p.removeCustomWord(w) }) { Text("Remove") }
        }
    }
    if (p.customWords.size < Progress.MAX_CUSTOM) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(24) },
            label = { Text("A word, like Grandma or Rex") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ownWordPictures.forEach { e ->
                Text(
                    e,
                    fontSize = 26.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (e == pic) Color(0xFFFFE082) else Color.Transparent)
                        .clickable { pic = e }
                        .padding(4.dp),
                )
            }
        }
        Button(enabled = text.isNotBlank(), onClick = {
            p.addCustomWord(text, pic)
            app.speaker.say(text)
            text = ""
        }) { Text("Add word") }
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
    Text(text, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Kid.ink)
}

@Composable
private fun Tip(text: String) {
    Row {
        Text("•", fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 15.sp, color = Kid.ink)
    }
}
