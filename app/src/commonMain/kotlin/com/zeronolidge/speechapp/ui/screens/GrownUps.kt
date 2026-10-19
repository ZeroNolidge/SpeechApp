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
import androidx.compose.foundation.layout.height
import com.zeronolidge.speechapp.ui.art.paperCard
import com.zeronolidge.speechapp.ui.art.paperCircle
import com.zeronolidge.speechapp.ui.art.paperRoundRect
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
import androidx.compose.runtime.LaunchedEffect
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
                Spacer(Modifier.padding(4.dp))
                Heading("Word listening")
                WordListeningSection()
                SoundCardsSection()
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

/**
 * On-device word listening: what it is, whether it's ready, the grown-up test mode (rate each try
 * "right" or "off") with how often the app agrees, and the near-miss modelling switch, which only
 * unlocks after 30 rated tries at 90% agreement.
 */
@Composable
private fun WordListeningSection() {
    val app = LocalApp.current
    val p = app.progress
    val dim = Kid.ink.copy(alpha = 0.7f)
    // Re-read the download state every couple of seconds while this page is open.
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2000)
            tick++
        }
    }
    val waitingForWifi = p.wordListening && p.wordModelWifiOnly && !p.wordModelSaved &&
        com.zeronolidge.speechapp.ui.platform.AppHome.onMobileData()
    val status = tick.let {
        when (app.voice.words.status) {
            com.zeronolidge.speechapp.audio.WordListener.Status.READY -> "Ready (saved on this device, works offline)"
            com.zeronolidge.speechapp.audio.WordListener.Status.LOADING -> if (p.wordModelSaved) "Loading from this device…" else "Downloading (41 MB, one time)…"
            com.zeronolidge.speechapp.audio.WordListener.Status.UNAVAILABLE -> "Not available on this device: listening by loudness only, as before"
            com.zeronolidge.speechapp.audio.WordListener.Status.OFF -> when {
                !p.wordListening -> "Off. Nothing is downloaded until you turn it on."
                waitingForWifi -> "Waiting for Wi-Fi to download (41 MB, one time)."
                !p.useMic -> "Starts once listening (the microphone) is on."
                else -> "Starts a few seconds after the app opens."
            }
        }
    }
    Text(
        "Besides hearing that your child spoke, the app can try to recognise the word, on this device only. " +
            "It keeps no recordings and no text: just a small note per try (the word, its sound, what it heard and how sure). " +
            "Right now it only listens: Rory reacts exactly as before. Turning it on downloads a 41 MB word model once.",
        fontSize = 14.sp, color = dim,
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Word listening", fontSize = 18.sp, modifier = Modifier.weight(1f))
        Switch(checked = p.wordListening, onCheckedChange = { p.updateWordListening(it) })
    }
    Text(status, fontSize = 14.sp, color = dim)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Download on Wi-Fi only", fontSize = 18.sp, modifier = Modifier.weight(1f))
        Switch(checked = p.wordModelWifiOnly, onCheckedChange = { p.updateWordModelWifiOnly(it) })
    }
    Text(
        if (p.wordModelWifiOnly) "On mobile data it waits for Wi-Fi. (iPads can't tell the app which they're on, so they download straight away.)"
        else "Downloads on any connection, including mobile data.",
        fontSize = 14.sp, color = dim,
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Test mode: I'll rate each try", fontSize = 18.sp, modifier = Modifier.weight(1f))
        Switch(checked = p.wordTestMode, onCheckedChange = { p.updateWordTestMode(it) })
    }
    Text(
        "Sit with your child. After each try a small bar appears: tap \"Right\" if the sound was right, \"Off\" if it wasn't. " +
            "This checks how well the app hears your child.",
        fontSize = 14.sp, color = dim,
    )
    val called = com.zeronolidge.speechapp.logic.Agreement.called(p.wordTries)
    val rated = p.wordTries.count { it.rating != null }
    val agree = com.zeronolidge.speechapp.logic.Agreement.rate(p.wordTries)
    Text(
        if (rated == 0) "No rated tries yet."
        else "You've rated $rated ${if (rated == 1) "try" else "tries"}. The app made a call on ${called.size}" +
            (if (called.isNotEmpty()) " and agreed with you on ${(agree * 100).roundToInt()}%." else "."),
        fontSize = 16.sp, fontWeight = FontWeight.Bold,
    )
    val allowed = com.zeronolidge.speechapp.logic.Agreement.modelingAllowed(p.wordTries)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Rory models near-miss sounds", fontSize = 18.sp, modifier = Modifier.weight(1f), color = if (allowed) Kid.ink else dim)
        Switch(checked = p.nearMissModeling && allowed, enabled = allowed, onCheckedChange = { p.updateNearMissModeling(it) })
    }
    Text(
        if (allowed) "When the app is sure a sound was close, Rory says it once more clearly (with his mouth shape) and asks for one more try. At most 1 try in 3, never twice in a row, and never for sounds that are still developing at this age."
        else "Unlocks after ${com.zeronolidge.speechapp.logic.Agreement.MIN_RATED} rated tries that the app made a call on, with at least ${(com.zeronolidge.speechapp.logic.Agreement.MIN_AGREE * 100).roundToInt()}% agreement (now ${called.size}" +
            (if (called.isNotEmpty()) ", ${(agree * 100).roundToInt()}%)." else ")."),
        fontSize = 14.sp, color = dim,
    )
}

/** One card per sound (after about 20 tries): start, middle and end of words, and a 4-week trend. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SoundCardsSection() {
    val p = LocalApp.current.progress
    val cards = remember(p.wordTries.size) { com.zeronolidge.speechapp.logic.SoundCards.cards(p.wordTries, today()) }
    if (cards.isEmpty()) {
        Text("Sound cards appear here after about ${com.zeronolidge.speechapp.logic.SoundCards.MIN_TRIES} tries of a sound.", fontSize = 14.sp, color = Kid.ink.copy(alpha = 0.7f))
        return
    }
    Text("App estimate, not a speech assessment.", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8D6E63))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cards.forEach { c -> SoundCard(c) }
    }
    val focus = com.zeronolidge.speechapp.logic.SoundCards.focus(cards)
    if (focus.isNotEmpty()) {
        Text("Sounds to practise together", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        focus.forEach { (s, words) ->
            val label = SpeechContent.sounds.firstOrNull { it.id == s }?.label ?: s
            Tip("$label: ${words.joinToString(", ")}. Say each slowly, face to face, then let your child try.")
        }
    }
    Tip("If you're worried about your child's speech, talk to a speech-language pathologist (speech therapist). This app can't diagnose anything.")
}

@Composable
private fun SoundCard(c: com.zeronolidge.speechapp.logic.SoundCards.Card) {
    val label = SpeechContent.sounds.firstOrNull { it.id == c.sound }?.label ?: c.sound
    fun pct(v: Float?) = v?.let { "${(it * 100).roundToInt()}%" } ?: "–"
    Column(
        Modifier.width(220.dp).paperCard(Color(0xFFFFF8E1), 18.dp).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("$label  (${c.tries} tries)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Start ${pct(c.byPos[com.zeronolidge.speechapp.logic.SoundPos.START])} · Middle ${pct(c.byPos[com.zeronolidge.speechapp.logic.SoundPos.MIDDLE])} · End ${pct(c.byPos[com.zeronolidge.speechapp.logic.SoundPos.END])}", fontSize = 14.sp)
        // A small paper trend line over the last 4 weeks (shown once there are 2 weeks to compare).
        val pts = c.weeks.mapIndexedNotNull { i, v -> v?.let { i to it } }
        if (pts.size >= 2) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(56.dp)) {
                val w = size.width
                val h = size.height
                paperRoundRect(Color(0xFFFFECB3), androidx.compose.ui.geometry.Offset.Zero, size, androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()))
                fun at(i: Int, v: Float) = androidx.compose.ui.geometry.Offset(w * (0.1f + 0.8f * i / 3f), h * (0.85f - 0.7f * v))
                for (j in 1 until pts.size) {
                    drawLine(Color(0xFF6D4C41), at(pts[j - 1].first, pts[j - 1].second), at(pts[j].first, pts[j].second), strokeWidth = 3.dp.toPx())
                }
                pts.forEach { (i, v) -> paperCircle(Color(0xFFFF8F00), 5.dp.toPx(), at(i, v)) }
            }
            Text("Last 4 weeks", fontSize = 12.sp, color = Kid.ink.copy(alpha = 0.6f))
        }
    }
}
