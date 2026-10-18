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
import com.zeronolidge.speechapp.ui.platform.MicStatus
import com.zeronolidge.speechapp.ui.platform.rememberCopyText
import com.zeronolidge.speechapp.ui.platform.rememberMicPermission
import com.zeronolidge.speechapp.ui.platform.AppHome
import com.zeronolidge.speechapp.ui.theme.Kid
import kotlin.math.roundToInt

/** Pictures a grown-up can pick for their own words. */
private val ownWordPictures = listOf(
    "🧑", "👩", "👨", "👵", "👴", "👶", "🐶", "🐱", // family and pets
    "🧸", "🚗", "⚽", "🦖", "🚂", "🎈", "🚲", // toys
    "🍕", "🍌", "🍎", "🍪", "🧀", "🍓", "⭐", // foods and more
)

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
                Heading("Words ${p.heroName} has tried")
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
                Text("Target sounds (pick 1 or 2)", fontSize = 18.sp)
                Text(
                    if (p.focusSounds.isEmpty()) "None picked, so stories use every sound." else "Most practice words use the sounds you picked.",
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
                MicStatusSection()
                // 2 or 3 words to say together today: tricky ones first, then ones due back, then
                // his target-sound and own words.
                Heading("Words to say today")
                val today = remember { p.wordsToSayToday() }
                if (today.isEmpty()) {
                    Text("Play a little first: the words he practises will show up here.", fontSize = 16.sp)
                } else {
                    Text(today.joinToString("  ·  "), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Say each one slowly and clearly, then let him have a turn. Any try is great.", fontSize = 14.sp, color = Kid.ink.copy(alpha = 0.7f))
                }
                Heading("Progress")
                val lap = p.chapter / Trail.size
                Text(
                    "${p.chaptersDone} ${if (p.chaptersDone == 1) "chapter" else "chapters"} finished" +
                        " · now at stop ${Trail.stopOf(p.chapter) + 1} of ${Trail.size}" +
                        (if (lap > 0) " (trip ${lap + 1} around the island)" else "") +
                        " · ${p.pieces.size - 1} paper pieces · ${p.words.stickers.size} stickers",
                    fontSize = 16.sp,
                )
                Spacer(Modifier.padding(4.dp))
                Heading("Play offline and add to the home screen")
                HomeScreenSection()
                Heading("Keep progress safe")
                BackupSection()
                Spacer(Modifier.padding(4.dp))
                OutlinedButton(onClick = { confirmReset = true }) { Text("Reset all progress") }
                Heading("Tips")
                Tip("Sit with your child for speaking. Say the word together, face to face, so they can watch your mouth.")
                Tip("Gems go in the backpack for every word your child tries to say, not for right answers. A full backpack gives the buddy a new paper piece.")
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

/**
 * Progress is saved in this browser (or app) only. A backup code carries it to a new tablet, or
 * back after the browser's data is cleared.
 */
@Composable
private fun BackupSection() {
    val app = LocalApp.current
    val p = app.progress
    val copy = rememberCopyText()
    var code by remember { mutableStateOf("") }
    var paste by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var confirmRestore by remember { mutableStateOf(false) }
    Text(
        "Progress is saved on this device only. Make a backup code and keep it somewhere safe (email it to yourself). " +
            "On a new tablet, or if the browser's data is cleared, paste it below to carry on where your child left off.",
        fontSize = 14.sp,
        color = Kid.ink.copy(alpha = 0.7f),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {
            code = p.exportCode()
            message = if (copy(code)) "Backup code made and copied." else "Backup code made. Select it and copy it."
        }) { Text("Save a backup code") }
        if (code.isNotEmpty()) OutlinedButton(onClick = { message = if (copy(code)) "Copied." else "Select the code and copy it." }) { Text("Copy") }
    }
    if (code.isNotEmpty()) {
        OutlinedTextField(value = code, onValueChange = {}, readOnly = true, label = { Text("Backup code") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
    }
    OutlinedTextField(
        value = paste,
        onValueChange = { paste = it },
        label = { Text("Paste a backup code to restore") },
        maxLines = 3,
        modifier = Modifier.fillMaxWidth(),
    )
    Button(enabled = paste.isNotBlank(), onClick = { confirmRestore = true }) { Text("Restore from code") }
    if (message.isNotEmpty()) Text(message, fontSize = 14.sp, color = Kid.ink)
    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Restore this backup?") },
            text = { Text("This replaces the progress on this device with the progress in the code.") },
            confirmButton = {
                Button(onClick = {
                    confirmRestore = false
                    message = if (p.importCode(paste)) {
                        paste = ""
                        "Restored! Welcome back."
                    } else {
                        "That code didn't work. Check it was copied completely."
                    }
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Cancel") } },
        )
    }
}

/** Offline play, installing the web app full screen, and whether saved progress is protected. */
@Composable
private fun HomeScreenSection() {
    if (!AppHome.isWeb) {
        Text("The app works without internet and keeps progress on this tablet.", fontSize = 14.sp, color = Kid.ink.copy(alpha = 0.7f))
        return
    }
    Text(
        if (AppHome.offlineReady()) "Ready to play without internet." else "Saving the app for offline play (stay online for a minute).",
        fontSize = 16.sp, fontWeight = FontWeight.Bold,
        color = if (AppHome.offlineReady()) Color(0xFF2E7D32) else Color(0xFFEF6C00),
    )
    Text(
        if (AppHome.storageProtected()) "This browser has agreed to keep the saved progress." else "Make a backup code below now and then, in case the browser clears its data.",
        fontSize = 14.sp, color = Kid.ink.copy(alpha = 0.7f),
    )
    if (AppHome.canInstall()) Button(onClick = { AppHome.install() }) { Text("📲 Add to home screen") }
    Tip("iPad or iPhone (Safari): tap the Share button, then \"Add to Home Screen\". It opens full screen, with no browser bar, and works offline.")
    Tip("Android (Chrome): tap the three-dots menu, then \"Install app\" or \"Add to Home screen\".")
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
    Text(
        "Every word your child has tried, and how many times. There's no right or wrong here: each try counts.",
        fontSize = 14.sp,
        color = Kid.ink.copy(alpha = 0.7f),
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        book.stickers.sortedByDescending { it.lastDay }.forEach { WordChip(it, now) }
    }
}

/**
 * Whether the microphone is on, with a button to allow it (a grown-up taps Allow here, never
 * mid-story) and short steps to turn it back on if it was blocked.
 */
@Composable
private fun MicStatusSection() {
    val app = LocalApp.current
    val mic = rememberMicPermission()
    val status = mic.status
    val (title, color) = when {
        !app.voice.available -> "No microphone found on this device" to Color(0xFF8D6E63)
        !app.progress.useMic -> "Microphone switched off above" to Color(0xFF8D6E63)
        status == MicStatus.ON -> "Microphone is on" to Color(0xFF2E7D32)
        status == MicStatus.BLOCKED -> "Microphone is blocked" to Color(0xFFC62828)
        app.progress.micAsked -> "Listening is off for now (\"Not now\" was tapped on the \"Can Rory hear you?\" screen)" to Color(0xFFEF6C00)
        else -> "Microphone not allowed yet" to Color(0xFFEF6C00)
    }
    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
    Text(
        "It only listens for your child's voice (how loud), not the exact word, so a whisper or part of a word counts. " +
            "Without the microphone, the app asks your child to say the word out loud and tap the big \"I said it!\" button.",
        fontSize = 14.sp,
        color = Kid.ink.copy(alpha = 0.7f),
    )
    // One clear way to switch listening on later, whether it was "Not now", switched off above,
    // or never asked.
    if (app.voice.available && (!app.progress.useMic || status != MicStatus.ON)) {
        Button(onClick = {
            app.progress.updateUseMic(true)
            if (status != MicStatus.ON) mic.request { ok -> if (ok) app.speaker.say("Yay! Rory can hear you now!") }
        }) { Text("🎤 Turn on listening", fontSize = 18.sp) }
    }
    if (status == MicStatus.BLOCKED) {
        Text("To turn it back on:", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Tip("iPad or iPhone: open Settings, scroll to Safari, tap Microphone, and choose Allow (or Ask). Then come back here and tap Turn on listening.")
        Tip("In Safari itself: tap the aA button in the address bar, then Website Settings, then Microphone: Allow.")
        Tip("Android tablet in Chrome: tap the icon to the left of the web address, then Permissions, then Microphone: Allow.")
        Tip("Android app: open Settings, then Apps, then Rory's Talk Trail, then Permissions, then Microphone: Allow.")
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
        "${w.emoji} ${w.text} · ${if (w.tries == 1) "1 try" else "${w.tries} tries"} · $ago",
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
        "Family names, toys, foods: words he loves. Stories and games use them (and his target sounds) most of the time. Up to ${Progress.MAX_CUSTOM}.",
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
