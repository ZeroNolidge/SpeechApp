package com.zeronolidge.speechapp.ui.screens

import com.zeronolidge.speechapp.ui.theme.LetterFont
import com.zeronolidge.speechapp.ui.components.AnswerRow
import com.zeronolidge.speechapp.logic.Key
import com.zeronolidge.speechapp.logic.AnswerKey
import com.zeronolidge.speechapp.logic.SpeechSteps
import com.zeronolidge.speechapp.ui.art.loop
import com.zeronolidge.speechapp.logic.Echo
import com.zeronolidge.speechapp.ui.art.paperOval
import com.zeronolidge.speechapp.ui.art.paperRect
import com.zeronolidge.speechapp.ui.art.paperCircle
import com.zeronolidge.speechapp.ui.art.paperCard
import com.zeronolidge.speechapp.ui.components.wiggleOnTap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.data.Praise
import com.zeronolidge.speechapp.data.SpeechSound
import com.zeronolidge.speechapp.data.SpeechContent
import com.zeronolidge.speechapp.data.Word
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.ui.art.Critter
import com.zeronolidge.speechapp.ui.art.Mood
import com.zeronolidge.speechapp.logic.WordStat
import com.zeronolidge.speechapp.state.Progress
import com.zeronolidge.speechapp.ui.Nav
import com.zeronolidge.speechapp.ui.art.PaperStyle
import com.zeronolidge.speechapp.ui.components.ContinueAction
import com.zeronolidge.speechapp.ui.components.ContinueHost
import com.zeronolidge.speechapp.ui.components.ContinueSize
import com.zeronolidge.speechapp.ui.components.ContinueReserve
import com.zeronolidge.speechapp.ui.components.KidScreen
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.PaperButton
import com.zeronolidge.speechapp.ui.components.PaperChip
import com.zeronolidge.speechapp.ui.components.PaperColorsUi
import com.zeronolidge.speechapp.ui.components.RoundIcon
import com.zeronolidge.speechapp.ui.components.popIn
import com.zeronolidge.speechapp.ui.components.peelIn
import com.zeronolidge.speechapp.ui.platform.rememberMicPermission

private enum class Speak { MODELLING, LISTENING, WAITING_TAP, DONE, PARROT_RECORDING, PARROT_PLAYING }

/** Set when the microphone couldn't be opened, so later words go straight to "say it out loud". */
private var micBroken = false

/** Free practice of one speech sound, word by word. Each word moves on by itself after a try. */
@Composable
fun TalkPracticeScreen(sound: SpeechSound, nav: Nav) {
    val app = LocalApp.current
    // The quiet speech step: one word, then two words, then a short sentence.
    val words = remember(sound) { SpeechSteps.wordsFor(sound, app.progress.speechStep) }
    var index by remember { mutableIntStateOf(0) }
    val word = words[index % words.size]
    val intro = if (index == 0) "Let's practice the ${sound.label} sound. ${sound.modelSound}." else null
    KidScreen("${sound.emoji} ${sound.label}", nav.home, "Let's say ${sound.modelSound}", onBack = nav.back) {
        ContinueHost {
            SpeakMoment(word, sound, Modifier.fillMaxSize(), intro = intro, showParrot = true, key = index) {
                // After about 12 minutes of play, Rory wraps up here, between words.
                if (app.sessionLong()) nav.go(com.zeronolidge.speechapp.ui.Screen.Rest) else index++
            }
        }
    }
}

/**
 * A speaking moment, used inside the story and in free practice. In order:
 *  1. The picture and word, played slowly and clearly (slow, then normal).
 *  2. "Your turn!" and about 4 seconds of listening, with a throbbing microphone. Listening only
 *     checks for a voice (loudness), not the exact word, so a whisper or part of the word counts.
 *     It works with young voices and on iPhones and iPads.
 *  3. Voice heard: a celebration and a shiny sticker. Nothing heard: "Let's say it together!",
 *     the word again, and one more listen; still quiet: Rory asks once more and keeps listening
 *     until any sound or "I said it!" (the card never moves on by itself). No microphone (or
 *     it's blocked): Rory says "Say it out loud to me!" and shows a big "I said it!" button.
 *  4. The continue button (bottom-right, as on every step) skips at any time. Every try (not a
 *     skip) puts a gem in the backpack. The microphone is never asked for here, mid-story.
 * The microphone never blocks the story and quiet is never a fail.
 * [onDone] gets true if a voice was heard.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpeakMoment(
    word: Word,
    sound: SpeechSound?,
    modifier: Modifier = Modifier,
    /** Story narration said first. */
    intro: String? = null,
    /** Said after the try, e.g. "Now we can open the door!". */
    afterLine: String? = null,
    showParrot: Boolean = false,
    /** What to show instead of the word's emoji, e.g. a sleeping friend. */
    picture: (@Composable (Modifier) -> Unit)? = null,
    key: Any = word,
    /** After saying a word with a speech sound: "Tap the B!" with 2 or 3 letters (skipped with ▶). */
    tapLetter: Boolean = true,
    /**
     * His word makes it happen: called the moment the microphone hears a voice (about 0.15 s in,
     * long before listening ends), or when the try ends another way ("I said it!", a quiet try),
     * so a story can show its action at once. Not called for a skip.
     */
    onAction: (() -> Unit)? = null,
    onDone: (heard: Boolean) -> Unit,
) {
    val app = LocalApp.current
    var acted by remember(key) { mutableStateOf(false) }
    fun act() {
        if (!acted) {
            acted = true
            onAction?.invoke()
        }
    }
    val p = app.progress
    val micPermission = rememberMicPermission()
    var state by remember(key) { mutableStateOf(Speak.MODELLING) }
    var level by remember { mutableFloatStateOf(0f) }
    var shiny by remember(key) { mutableStateOf<WordStat?>(null) }
    /** Rory is waiting for the child to say it out loud (no microphone). */
    var roryWaiting by remember(key) { mutableStateOf(false) }
    // Bumped whenever a new step starts, so callbacks from an interrupted one are ignored.
    var session by remember(key) { mutableIntStateOf(0) }
    /** Listens so far for this word (at most two). */
    var tries by remember(key) { mutableIntStateOf(0) }
    /** The "Tap the B!" letters, once the word has been said; and whether a voice was heard. */
    var letterKey by remember(key) { mutableStateOf<Key<String>?>(null) }
    var heardIt by remember(key) { mutableStateOf(false) }
    /** Two quiet tries: listening carries on until his voice or "I said it!". */
    var waitingVoice by remember(key) { mutableStateOf(false) }
    /** His voice's push on the picture this word (0 to 1, see VoicePush): pops hop, holds grow. */
    val pushModel = remember(key) { com.zeronolidge.speechapp.logic.VoicePush(com.zeronolidge.speechapp.logic.VoicePush.modeForLine(word.text, sound?.id)) }
    var pushed by remember(key) { mutableFloatStateOf(0f) }
    /** Rory has modelled a near-miss once for this word (never more). */
    var modelledOnce by remember(key) { mutableStateOf(false) }
    /** The sound whose mouth shape Rory is showing while he models it. */
    var mouthCue by remember(key) { mutableStateOf<String?>(null) }
    /** Rory's echo with one word added ("A big dog!"), sliding in under the word once he's said it. */
    var expansion by remember(key) { mutableStateOf<String?>(null) }
    /** Rory modelling the word: a pulse of the word card for each time he says it (the 3rd bigger). */
    var modelBeat by remember(key) { mutableIntStateOf(0) }
    var modelBig by remember(key) { mutableStateOf(false) }
    fun micUsable() = p.useMic && app.voice.available && micPermission.granted() && !micBroken
    val yourTurn = "Your turn!"

    fun stopAll() {
        app.voice.cancel()
        app.parrot.stop()
        session++
        level = 0f
    }

    /** The try is over: record it, reward it, and move the story on. */
    fun finish(heard: Boolean, micUsed: Boolean, skipped: Boolean = false) {
        if (state == Speak.DONE) return
        stopAll()
        state = Speak.DONE
        roryWaiting = false
        val next = { onDone(heard) }
        if (!skipped) act()
        if (skipped) {
            app.speaker.say(listOfNotNull("Okay! Let's keep going.", afterLine).joinToString(" "), onDone = next)
            return
        }
        val stat = p.recordWord(word.text, word.emoji, clear = heard, heard = micUsed)
        if (micUsed) p.speechResult(heard)
        if (heard) shiny = stat
        // Gems are for trying to say words, not for getting things right.
        val piece = p.addGem()
        val praise = when {
            // Rory says the target word back and adds one word ("Dog! A big dog!").
            heard -> "${Echo.expansion(word.text, stat.tries)} I heard you! You got a shiny sticker!"
            micUsed -> "Great job saying it together!"
            else -> listOf("Great talking!", "Good saying!", "I love your talking!").random()
        }
        val gemLine = if (piece != null) "Your backpack is full! Your buddy got a new ${piece.name}!" else "A gem for your backpack!"
        // Then find the sound's letter, the same way as every quiz (letters from the answer key).
        fun afterTry() {
            if (tapLetter && sound != null) {
                heardIt = heard
                letterKey = AnswerKey.tapLetter(sound, if (p.speechStep == 0) 2 else 3)
            } else {
                next()
            }
        }
        // Voice heard: Rory says the target word back, slowly and clearly, then expands it
        // ("Ball! ... A big red ball!").
        val praiseNow = { app.reward(Progress.AREA_SPEECH, listOfNotNull(praise, gemLine, afterLine).joinToString(" "), onDone = { afterTry() }) }
        // The slow echo of his word ("Climb!") plays to the end first, then the praise, which
        // adds a word ("A big climb!"), shown sliding in under the word.
        if (heard) {
            app.speaker.say("${word.text}!", slow = true) {
                expansion = Echo.expansion(word.text, stat.tries)
                praiseNow()
            }
        } else {
            praiseNow()
        }
        if (piece != null) app.showGift(piece)
    }

    /** No microphone: Rory asks for it out loud and waits for a tap on ▶. */
    fun askOutLoud() {
        state = Speak.WAITING_TAP
        roryWaiting = true
        app.instruct("Say it out loud to me! Then tap the big green I said it button.")
    }

    lateinit var listen: () -> Unit

    /** Says [lines] (text to slow?), then "Your turn!" and listening, or Rory's "say it out loud". */
    fun speakThen(lines: List<Pair<String, Boolean>>, thenListen: Boolean) {
        if (state == Speak.DONE) return
        stopAll()
        val s = session
        state = Speak.MODELLING
        roryWaiting = false
        val useMic = thenListen && micUsable()
        val all = lines + if (useMic) listOf(yourTurn to false) else emptyList()
        if (all.isEmpty()) {
            if (useMic) listen() else askOutLoud()
            return
        }
        // Each time Rory says the word itself, the card pulses once (bigger and slower when he
        // says it slowly), so his modelling is something to watch, not just hear.
        fun isWord(t: String) = t == word.text || t == word.syllables
        fun pulseFor(k: Int) {
            if (k in all.indices && isWord(all[k].first)) {
                modelBig = all[k].second
                modelBeat++
            }
        }
        all.forEachIndexed { i, (text, slow) ->
            val cb: (() -> Unit) = if (i == all.lastIndex) ({
                if (s == session && state != Speak.DONE) {
                    if (useMic) listen() else askOutLoud()
                }
            }) else ({ if (s == session) pulseFor(i + 1) })
            if (i == 0) {
                pulseFor(0)
                app.speaker.say(text, slow, cb)
            } else {
                app.speaker.then(text, slow, cb)
            }
        }
    }

    // Rory models the word before his turn: three times, the third time slower.
    fun model(): List<Pair<String, Boolean>> = listOf(
        "Listen." to false,
        word.text to false,
        word.text to false,
        // The third time slower (in syllables for a longer word: "piz - za").
        word.syllables to true,
    )

    listen = {
        val s = session
        state = Speak.LISTENING
        tries++
        // Word listening: records what was heard (listen-only). When the grown-up has turned
        // modelling on, the answer waits (about a second at most) for the words.
        val check = app.wordCheck(word.text, sound?.id, modeled = !waitingVoice)
        val waitWords = check != null && app.modelingOn && !modelledOnce
        var loud: com.zeronolidge.speechapp.audio.VoiceTry? = null
        val handle: (com.zeronolidge.speechapp.audio.VoiceTry, Pair<com.zeronolidge.speechapp.logic.Heard, Float>?) -> Unit = handle@{ t, judged ->
            val heard = when (t) {
                com.zeronolidge.speechapp.audio.VoiceTry.HEARD -> true
                com.zeronolidge.speechapp.audio.VoiceTry.NO_MIC -> null
                // While waiting, any sound at all counts.
                com.zeronolidge.speechapp.audio.VoiceTry.SOFT -> tries > 2
                else -> false
            }
            if (s != session || state == Speak.DONE) return@handle
            level = 0f
            app.listenLevel = 0f
            // A whispered try still gives the picture a small nudge, never nothing.
            if (t == com.zeronolidge.speechapp.audio.VoiceTry.SOFT) pushed = maxOf(pushed, 0.3f)
            // After Rory modelled the sound: this was the one more try; the word is done either way.
            if (modelledOnce && heard == false) {
                finish(heard = false, micUsed = true)
                return@handle
            }
            when (heard) {
                true -> {
                    // Modelling on: a confident near-miss gets the sound modelled once (the
                    // action has already happened), then one more try at most.
                    val model = check?.let { c -> judged?.let { (r, conf) -> com.zeronolidge.speechapp.logic.Corrections.reply(c, r, conf, app.corrections()) } }
                    val target = check?.let { c -> SpeechContent.sounds.firstOrNull { it.id == c.sound } }
                    if (waitWords) app.corrections().noteReply(check!!.sound, model == com.zeronolidge.speechapp.logic.Reply.MODEL_SOUND && target != null)
                    if (waitWords && model == com.zeronolidge.speechapp.logic.Reply.MODEL_SOUND && target != null) {
                        modelledOnce = true
                        state = Speak.MODELLING
                        mouthCue = target.id
                        app.speaker.say("Listen. ${target.modelSound}. ${word.text}.")
                        app.speaker.then("One more time!") {
                            mouthCue = null
                            if (s == session && state != Speak.DONE) listen()
                        }
                    } else {
                        finish(heard = true, micUsed = true)
                    }
                }
                false ->
                    when {
                        // Quiet: say it together and listen once more.
                        tries < 2 -> speakThen(listOf("Let's say it together!" to false, word.syllables to true, word.text to false), true)
                        // Still quiet: the card waits for his voice or "I said it!" and never
                        // moves on by itself. Rory asks once (not listening while he talks),
                        // then just keeps listening.
                        tries == 2 -> {
                            state = Speak.MODELLING
                            waitingVoice = true
                            app.speaker.say("Say it out loud to me! Then tap the big green I said it button.") {
                                if (s == session && state != Speak.DONE) listen()
                            }
                        }
                        else -> listen()
                    }
                null -> {
                    // The microphone couldn't be opened: carry on without it.
                    micBroken = true
                    askOutLoud()
                }
            }
        }
        val mode = com.zeronolidge.speechapp.logic.VoicePush.modeForLine(word.text, sound?.id)
        app.voice.listenTry(
            4000, onLevel = { level = it; app.listenLevel = it }, onVoice = { if (s == session) act() },
            // His voice pushes the picture: a hop for each pop (p b t d g k), growing while a
            // long sound (s m f sh) is held.
            onPush = { push -> if (s == session && push > 0f) { pushModel.feed(push, 50); pushed = pushModel.progress } },
            mode = mode,
            words = check?.grammar,
            onWords = { heard ->
                val judged = check?.let { app.wordHeard(it, heard) }
                if (waitWords) loud?.let { handle(it, judged) }
            },
        ) { t -> if (waitWords) loud = t else handle(t, null) }
    }

    /** Never asks for the microphone mid-story: that happens on the start screen or the grown-ups page. */
    fun withMic(onDenied: () -> Unit, action: () -> Unit) {
        if (micPermission.granted()) action() else onDenied()
    }

    fun parrotNow() {
        stopAll()
        val s = session
        state = Speak.MODELLING
        // The copy-me parrot waits for him with the same listening look (never a fixed few
        // seconds): it copies him as soon as he has spoken, and keeps waiting through quiet.
        lateinit var wait: () -> Unit
        wait = {
            if (s == session && state == Speak.PARROT_RECORDING) {
                app.parrot.listenThenPlay(
                    6000,
                    onLevel = { level = it; app.listenLevel = it },
                    onPlaying = { if (s == session) { level = 0f; app.listenLevel = 0f; state = Speak.PARROT_PLAYING } },
                    onQuiet = { wait() },
                    onDone = { if (s == session) state = Speak.WAITING_TAP },
                )
            }
        }
        app.speaker.say("Say ${word.text} and I'll copy you!") {
            if (s != session || state != Speak.MODELLING) return@say
            state = Speak.PARROT_RECORDING
            wait()
        }
    }

    // Start: narration, the word played slowly, "Your turn!", then listen.
    LaunchedEffect(key) {
        app.rememberInstruction("${word.syllables}. ${word.text}. Your turn! Say ${word.text}!")
        val lines = listOfNotNull(intro?.let { it to false }) + model()
        // The first talk turn of a sitting: a quick quiet check of the room before Rory talks.
        if (micUsable()) app.voice.checkRoom { if (state == Speak.MODELLING) speakThen(lines, micUsable()) } else speakThen(lines, false)
    }
    DisposableEffect(key) { onDispose { stopAll(); app.listening = false } }
    // His turn to talk: the scene behind dims slightly.
    // His turn (listening, or waiting for "I said it!" without a microphone): the listening look.
    LaunchedEffect(state) { app.listening = state == Speak.LISTENING || state == Speak.WAITING_TAP || state == Speak.PARROT_RECORDING }
    val inStory = LocalHasStage.current

    // The word card and its big icon buttons: on a phone the buttons sit in a row under the card;
    // on any screen wider than 700 (an iPad either way up) the card is on the left and the buttons
    // are in a column on the right, above the continue button's corner.
    BoxWithConstraints(modifier) {
        val wide = maxWidth > 700.dp
        val size = 68.dp
        // Once the word is done the card and buttons stay fully visible (faded ones looked broken
        // while Rory praises him); taps just do nothing until the story moves on.
        fun whileActive(action: () -> Unit) { if (state != Speak.DONE && !app.tapsSettling) action() }
        val icons: @Composable () -> Unit = {
            RoundIcon("🔊", PaperColorsUi.hear, size = size) { whileActive { speakThen(model(), true) } }
            RoundIcon("🐢", Color(0xFFC8E6C9), size = size) {
                whileActive { speakThen(listOf(word.syllables to true, word.text to true), true) }
            }
            if (sound != null) {
                RoundIcon("👄", Color(0xFFFFCDD2), size = size) {
                    whileActive { speakThen(listOf(sound.tip to false, sound.modelSound to true), true) }
                }
            }
            RoundIcon("🎤", Color(0xFFFFE0B2), size = size) {
                whileActive {
                    micBroken = false
                    withMic(onDenied = { speakThen(emptyList(), false) }) { speakThen(emptyList(), true) }
                }
            }
            if (showParrot && p.useMic) {
                RoundIcon("🦜", Color(0xFFB2EBF2), size = size) {
                    whileActive { withMic(onDenied = { app.speaker.say("Ask a grown-up to turn on the microphone.") }) { parrotNow() } }
                }
            }
        }
        val card: @Composable ColumnScope.() -> Unit = {
            // One pulse per word Rory says (the slow third one bigger and longer).
            val beatPulse = remember { Animatable(1f) }
            LaunchedEffect(modelBeat) {
                if (modelBeat > 0) {
                    beatPulse.animateTo(if (modelBig) 1.22f else 1.1f, tween(if (modelBig) 420 else 160))
                    beatPulse.animateTo(1f, tween(if (modelBig) 480 else 220))
                }
            }
            val pulse by animateFloatAsState(
                when (state) {
                    Speak.LISTENING -> 1f + level * 0.25f
                    else -> 1f
                } * (1f + 0.18f * pushed),
                label = "pulse",
            )
            // The picture card: tap it to hear the word again. On its own (Talk) the card is the
            // scene for the listening look, laid out in bands: the picture in the top third, the
            // word in its own band under it, and Rory listening in the lower half, so his glow
            // never covers either. In a story the story scene beside it is the stage.
            ListeningStage(Modifier.weight(1f).fillMaxWidth(), enabled = !inStory, lowerHalf = !inStory) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val small = maxHeight < 240.dp
                val cardW = maxWidth
                // A narrow card (the right-hand column on an upright iPad, a phone): picture above
                // the word instead of beside it, so the word always fits on one line.
                val narrowCard = cardW < 400.dp
                val textW = if (narrowCard) cardW - 32.dp else (cardW - 24.dp) * (1f / 2.3f)
                // Big, but never wider than the room it has (about 0.62 of the size per letter).
                val wordSize = minOf(if (small) 30f else if (word.text.length > 12) 36f else 50f, textW.value / (word.text.length.coerceAtLeast(3) * 0.62f)).sp
                val wordAndState: @Composable () -> Unit = {
                    Text(
                        highlighted(word.text, sound?.id),
                        fontSize = wordSize,
                        fontWeight = FontWeight.ExtraBold,
                        color = PaperStyle.ink,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.graphicsLayer { scaleX = beatPulse.value; scaleY = beatPulse.value },
                    )
                    // Rory's echo with a word added: the new words slide in on paper.
                    expansion?.let { ExpandChip(word.text, it, Modifier.padding(top = 4.dp)) }
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.height(if (small) 84.dp else 124.dp), contentAlignment = Alignment.Center) {
                        when (state) {
                            // Rory listens big in the scene (ListeningStage); here just the cue.
                            Speak.LISTENING ->
                                if (textW >= (if (small) 170.dp else 220.dp)) PaperChip("👂 Your turn!", fontSize = if (small) 18.sp else 24.sp, maxLines = 1)
                                else Text("👂", fontSize = 52.sp)
                            // The copy-me parrot listens with Rory's listening look too; here just the cue.
                            Speak.PARROT_RECORDING -> Text("🦜", fontSize = 52.sp)
                            Speak.PARROT_PLAYING -> Text("🦜", fontSize = 52.sp)
                            Speak.MODELLING -> Text("👂", fontSize = 52.sp)
                            Speak.WAITING_TAP -> Text("🗣️", fontSize = 52.sp)
                            Speak.DONE -> shiny?.let { Sticker(it, if (small) 64.dp else 92.dp, Modifier.peelIn(it.text)) }
                        }
                    }
                }
                PaperButton(
                    PaperStyle.chip,
                    Modifier.fillMaxSize().padding(bottom = 8.dp),
                    onClick = { whileActive { speakThen(model(), true) } },
                ) {
                    if (!inStory) {
                        val cardH = maxHeight
                        val bandH = cardH * 0.17f
                        // The word fits its band: never wider than the card, never taller than the band.
                        val talkWord = minOf(wordSize.value * (if (narrowCard) 1f else 1.25f), (cardW.value - 40f) / (word.text.length.coerceAtLeast(3) * 0.62f), bandH.value * 0.62f).sp
                        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            WordPicture(word, picture, pulse * beatPulse.value, Modifier.fillMaxWidth().height(cardH * 0.3f))
                            Box(Modifier.fillMaxWidth().height(bandH), contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        highlighted(word.text, sound?.id),
                                        fontSize = talkWord,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PaperStyle.ink,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.graphicsLayer { scaleX = beatPulse.value; scaleY = beatPulse.value },
                                    )
                                    expansion?.let { ExpandChip(word.text, it, Modifier.padding(start = 8.dp)) }
                                }
                            }
                            // The lower half: Rory's place while he listens; otherwise the cue or sticker.
                            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                if (!app.listening) when (state) {
                                    Speak.PARROT_RECORDING, Speak.PARROT_PLAYING -> Text("🦜", fontSize = 52.sp)
                                    Speak.MODELLING -> Text("👂", fontSize = 52.sp)
                                    Speak.DONE -> shiny?.let { Sticker(it, if (small) 64.dp else 92.dp, Modifier.peelIn(it.text)) }
                                    else -> {}
                                }
                            }
                        }
                    } else if (narrowCard) {
                        Column(
                            Modifier.fillMaxSize().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            WordPicture(word, picture, pulse * beatPulse.value, Modifier.weight(1f).fillMaxWidth())
                            wordAndState()
                        }
                    } else {
                        Row(
                            Modifier.fillMaxSize().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            WordPicture(word, picture, pulse * beatPulse.value, Modifier.weight(1.3f).fillMaxHeight())
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { wordAndState() }
                        }
                    }
                }
                // While Rory models the word, there he is, talking: in the card's corner (in Talk
                // the lower-left one, clear of the picture at the top).
                if (state == Speak.MODELLING) {
                    Critter(
                        Species.ROBOT, Modifier.align(if (inStory) Alignment.TopStart else Alignment.BottomStart).padding(8.dp).size(width = if (small) 48.dp else 70.dp, height = if (small) 60.dp else 88.dp),
                        Color(0xFFFFCC80), mood = Mood.HAPPY, talking = true,
                    )
                }
                // Rory models a sound: his mouth shape for it, big, in the card's corner.
                mouthCue?.let { MouthCue(it, Modifier.align(Alignment.TopEnd).padding(10.dp).popIn("mouth")) }
                // No microphone: Rory pops up big and asks for the word out loud, then waits for ▶.
                if (roryWaiting) {
                    // His turn without a microphone: Rory listens big in the scene; the word to say here.
                    PaperChip("🗣️ ${word.text}!", Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 16.dp).popIn("rory"), fontSize = 22.sp)
                }
            }
            }
        }
        val letters = letterKey
        if (letters != null && sound != null) {
            LetterTap(letters, sound, key, Modifier.fillMaxSize()) { onDone(heardIt) }
        } else if (wide) {
            // Wide: the picture card takes about 60%, then a gap, then the buttons in their own
            // panel on the right (above the continue button's corner). Buttons keep their size.
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.weight(0.6f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) { card() }
                Box(
                    Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                        .padding(bottom = ContinueSize + 20.dp)
                        .paperCard(ButtonPanelColor, 24.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    ) { icons() }
                }
            }
        } else {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                card()
                Spacer(Modifier.height(12.dp))
                // The buttons sit in their own panel under the card. The continue button is pinned
                // bottom-right like on every step, so the panel leaves room for it; the buttons wrap
                // onto a second line if they don't fit.
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    FlowRow(
                        Modifier.weight(1f).paperCard(ButtonPanelColor, 20.dp).padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) { icons() }
                    Spacer(Modifier.width(ContinueReserve))
                }
            }
        }
    }
    // Always there: carry on (or skip). With no microphone it becomes a big "I said it!" button.
    // It is pointed at only when it's the way forward, so it doesn't pull attention while listening.
    // The skip arrow waits a few seconds (it sits where the last step's continue arrow was, and a
    // child tapping it again and again skipped every word of a story); "I said it!" shows at once.
    // On his turn (listening, or waiting without a microphone) the button is "I said it!" and
    // counts as his try; while Rory is still modelling it is the plain skip, after 3 s.
    val saidIt = roryWaiting || state == Speak.LISTENING || state == Speak.WAITING_TAP || state == Speak.PARROT_RECORDING || waitingVoice
    ContinueAction(
        enabled = state != Speak.DONE,
        hint = state == Speak.WAITING_TAP || waitingVoice,
        label = if (saidIt) "I said it!" else null,
        delayMs = if (saidIt) 0 else 3000,
    ) {
        if (saidIt) finish(heard = false, micUsed = false) else finish(heard = false, micUsed = false, skipped = true)
    }
}

/**
 * "Tap the B!": right after saying a word, he finds the sound's letter among 2 or 3. Taps go
 * through [AnswerRow], so it's the same try-again pattern as every quiz (wiggle and "Try again!",
 * then Rory's hint with the right one glowing, then the answer is shown and it moves on).
 */
@Composable
private fun LetterTap(letters: Key<String>, sound: SpeechSound, roundKey: Any, modifier: Modifier, onDone: () -> Unit) {
    val app = LocalApp.current
    val ask = "Tap the ${sound.label}!"
    LaunchedEffect(roundKey) { app.instruct("Now find the letter. $ask") }
    BoxWithConstraints(modifier) {
        val n = letters.options.size
        val w = maxWidth
        val h = maxHeight
        val card = minOf(150.dp, (w - 24.dp * (n - 1) - 8.dp) / n, h * 0.42f)
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            PaperChip("👆 $ask", fontSize = 30.sp, maxLines = 1)
            AnswerRow(
                letters, Progress.AREA_LETTERS, roundKey,
                Modifier.fillMaxWidth(), Modifier.size(card),
                onSolved = { onDone() },
                describe = { it },
                hint = "Listen: ${sound.modelSound}. ${sound.label} says ${sound.modelSound}. Find the ${sound.label}!",
            ) { l ->
                Text(l, fontSize = (card.value * 0.5f).sp, fontWeight = FontWeight.ExtraBold, fontFamily = LetterFont, color = Color(0xFF1E88E5))
            }
        }
    }
}

/** The panel the word's round buttons sit in, on their own, apart from the picture card. */
private val ButtonPanelColor = Color(0xF2FFFAF0)

/** The word's picture, as big as its space allows; a tap gives it a little wiggle. */
@Composable
private fun WordPicture(word: Word, picture: (@Composable (Modifier) -> Unit)?, pulse: Float, modifier: Modifier) {
    BoxWithConstraints(modifier.scale(pulse).wiggleOnTap(), contentAlignment = Alignment.Center) {
        val room = minOf(maxWidth, maxHeight)
        if (picture != null) picture(Modifier.fillMaxHeight()) else Text(word.emoji, fontSize = (room.value * 0.92f).coerceIn(40f, 260f).sp)
    }
}

/**
 * His turn to talk: Rory cups his hand to a big, glowing paper ear by his head, and a paper ring
 * around him grows with the loudness of his voice, so he can see he's being listened to.
 */
@Composable
internal fun RoryListens(level: Float, size: Dp) {
    val t = rememberInfiniteTransition(label = "listen")
    val glow by t.loop(0.55f, 1f, 600, label = "earGlow")
    val voice by animateFloatAsState(level, tween(90), label = "voice")
    Box(Modifier.size(size * 1.35f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val base = size.toPx() * 0.5f
            // The paper ring that grows with his voice.
            val r = base * (0.95f + 0.5f * voice)
            paperCircle(Color(0xFFFFF3C4), r, center)
            drawCircle(Color.White, r, style = Stroke(4.dp.toPx()))
            drawCircle(Color(0xFFFFC94A).copy(alpha = 0.6f), r * 0.86f, style = Stroke(3.dp.toPx()))
        }
        Critter(Species.ROBOT, Modifier.fillMaxHeight(0.9f), Color(0xFFFFCC80), mood = Mood.LISTEN)
        // The ear, at the side of Rory's head where his hand is raised: a soft gold glow, a big
        // paper ear and the cupped hand behind it.
        Canvas(Modifier.fillMaxSize()) {
            val s = size.toPx() * 1.35f
            val h = s * 0.9f
            val w = h * 100f / 140f
            val left = (s - w) / 2
            // Big enough to read as an ear at a glance: about half Rory's width, sticking out
            // from the side of his head.
            val ear = Offset(left + w * 0.98f, (s - h) / 2 + h * 0.3f)
            drawCircle(Color(0xFFFFE082).copy(alpha = 0.6f * glow), w * 0.42f * (1f + 0.3f * voice + 0.15f * glow), ear)
            // The ear itself: a tall rounded shape with a lobe at the bottom and the curl inside,
            // a C that opens towards Rory's face, the way a child draws an ear.
            paperOval(Color(0xFFFFD54F), Offset(ear.x - w * 0.15f, ear.y - w * 0.24f), Size(w * 0.3f, w * 0.48f))
            paperCircle(Color(0xFFFFD54F), w * 0.09f, Offset(ear.x - w * 0.03f, ear.y + w * 0.22f))
            drawArc(
                Color(0xFFE65100), startAngle = -100f, sweepAngle = 200f, useCenter = false,
                topLeft = Offset(ear.x - w * 0.09f, ear.y - w * 0.15f), size = Size(w * 0.16f, w * 0.28f),
                style = Stroke(w * 0.04f, cap = StrokeCap.Round),
            )
        }
    }
}

/** True inside a screen whose scene already shows the listening look (a story step's scene). */
internal val LocalHasStage = androidx.compose.runtime.compositionLocalOf { false }

/**
 * The listening look, the same on every turn that shows "I said it!": the scene dims and Rory,
 * his ear cupped, grows to about a third of the scene's height and slides from the bottom-left
 * to one spot, the middle and a little low, his ear pulsing and the ring growing with his voice.
 * The target's glow hides while he listens (drawn by the screens), so nothing overlaps his ear.
 */
@Composable
internal fun ListeningStage(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** Talk practice: Rory stays in the stage's lower half (the picture and word are above). */
    lowerHalf: Boolean = false,
    /** Drawn over the dimmed scene, under Rory (a voice story's wind strips and confetti); gets Rory's centre. */
    aboveDim: (@Composable androidx.compose.foundation.layout.BoxScope.(roryAt: Offset) -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val app = LocalApp.current
    val still = com.zeronolidge.speechapp.ui.art.LocalReduceMotion.current
    val k by animateFloatAsState(if (enabled && app.listening) 1f else 0f, tween(if (still) 0 else 450), label = "stage")
    BoxWithConstraints(modifier) {
        val w = maxWidth
        val h = maxHeight
        content()
        val spot = listenSpot(w.value, h.value, lowerHalf)
        // RoryListens draws Rory about 1.08 x its size tall, in a box 1.35 x its size.
        val rory = spot.size.dp
        val box = rory * 1.35f
        if (k > 0.001f) Canvas(Modifier.fillMaxSize()) { drawRect(Color.Black.copy(alpha = 0.3f * k)) }
        aboveDim?.invoke(this, Offset(spot.cx, spot.cy))
        if (k > 0.001f) {
            val startX = 0.dp
            val startY = h - box
            val endLeft = (w * spot.cx - box / 2).coerceIn(0.dp, (w - box).coerceAtLeast(0.dp))
            val endTop = (h * spot.cy - box / 2).coerceIn(0.dp, (h - box).coerceAtLeast(0.dp))
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .graphicsLayer {
                        translationX = (startX + (endLeft - startX) * k).toPx()
                        translationY = (startY + (endTop - startY) * k).toPx()
                        alpha = k
                    },
            ) { RoryListens(app.listenLevel, rory) }
        }
    }
}

/** Where the listening Rory stands (centre, parts of the stage) and how big he is (dp). */
internal class ListenSpot(val cx: Float, val cy: Float, val size: Float)

/**
 * The one listening spot: the middle, a little low, Rory about a third of the stage's height.
 * With [lowerHalf] (Talk practice) he fits entirely in the lower half, under the picture and word.
 */
internal fun listenSpot(w: Float, h: Float, lowerHalf: Boolean = false): ListenSpot {
    val size = minOf(maxOf(h * 0.31f, 120f), w * 0.75f / 1.35f, if (lowerHalf) h * 0.47f / 1.35f else Float.MAX_VALUE)
    val box = size * 1.35f / h
    return ListenSpot(0.5f, if (lowerHalf) maxOf(0.64f, 0.5f + box / 2 + 0.02f).coerceAtMost(1f - box / 2) else 0.64f, size)
}

/** True once the big listening Rory has fully gone, so the small Rory beside the buddy can come back (never both at once). */
@Composable
internal fun listenLookGone(): Boolean {
    val app = LocalApp.current
    val still = com.zeronolidge.speechapp.ui.art.LocalReduceMotion.current
    val k by animateFloatAsState(if (app.listening) 1f else 0f, tween(if (still) 0 else 450), label = "lookGone")
    return !app.listening && k < 0.01f
}

/** Makes the practised sound stand out in the word, e.g. the "sh" in "fish". */
private fun highlighted(text: String, soundId: String?) = buildAnnotatedString {
    if (soundId == null) {
        append(text)
        return@buildAnnotatedString
    }
    // "c" makes the K sound in words like "cat" and "cookie".
    val at = text.indexOf(soundId).takeIf { it >= 0 }
        ?: if (soundId == "k") text.indexOf('c') else -1
    if (at < 0) {
        append(text)
        return@buildAnnotatedString
    }
    val len = if (soundId == "k" && text[at] == 'c') 1 else soundId.length
    append(text.substring(0, at))
    withStyle(SpanStyle(color = Color(0xFFE53935))) { append(text.substring(at, at + len)) }
    append(text.substring(at + len))
}

/**
 * Rory's mouth shape for a sound, as a simple paper lip cue while he models it: lips pressed
 * together (p b m), top teeth on the bottom lip (f v), tongue tip up behind the teeth (t d n l),
 * open with the tongue back (k g), round pushed-out lips (sh ch w), a teeth-together smile (s z).
 */
@Composable
internal fun MouthCue(sound: String, modifier: Modifier = Modifier) {
    Box(modifier.size(110.dp).paperCard(Color(0xFFFFE0B2), circle = true), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(14.dp)) {
            val w = size.width
            val h = size.height
            val lip = Color(0xFFE57373)
            val dark = Color(0xFF5D1F1F)
            val teeth = Color.White
            val tongue = Color(0xFFF48FB1)
            when (sound) {
                "p", "b", "m" -> {
                    // Lips pressed together.
                    paperOval(lip, Offset(w * 0.1f, h * 0.36f), Size(w * 0.8f, h * 0.16f))
                    paperOval(lip, Offset(w * 0.1f, h * 0.5f), Size(w * 0.8f, h * 0.16f))
                }
                "f", "v" -> {
                    paperOval(lip, Offset(w * 0.1f, h * 0.24f), Size(w * 0.8f, h * 0.22f))
                    paperRect(teeth, Offset(w * 0.28f, h * 0.42f), Size(w * 0.44f, h * 0.12f))
                    paperOval(lip, Offset(w * 0.14f, h * 0.5f), Size(w * 0.72f, h * 0.22f))
                }
                "t", "d", "n", "l" -> {
                    paperOval(lip, Offset(w * 0.08f, h * 0.22f), Size(w * 0.84f, h * 0.56f))
                    paperOval(dark, Offset(w * 0.2f, h * 0.32f), Size(w * 0.6f, h * 0.36f))
                    paperRect(teeth, Offset(w * 0.3f, h * 0.32f), Size(w * 0.4f, h * 0.07f))
                    // The tongue tip lifted to just behind the top teeth.
                    paperOval(tongue, Offset(w * 0.34f, h * 0.38f), Size(w * 0.32f, h * 0.26f))
                }
                "k", "g" -> {
                    paperOval(lip, Offset(w * 0.1f, h * 0.18f), Size(w * 0.8f, h * 0.64f))
                    paperOval(dark, Offset(w * 0.22f, h * 0.28f), Size(w * 0.56f, h * 0.44f))
                    // The tongue low at the front (its back is up, out of sight).
                    paperOval(tongue, Offset(w * 0.3f, h * 0.56f), Size(w * 0.4f, h * 0.14f))
                }
                "sh", "ch", "w", "r" -> {
                    // Round, pushed-out lips.
                    paperCircle(lip, w * 0.32f, Offset(w * 0.5f, h * 0.5f))
                    paperCircle(dark, w * 0.15f, Offset(w * 0.5f, h * 0.5f))
                }
                else -> {
                    // A smile with the teeth together (s, z and the rest).
                    paperOval(lip, Offset(w * 0.06f, h * 0.3f), Size(w * 0.88f, h * 0.4f))
                    paperRect(teeth, Offset(w * 0.18f, h * 0.42f), Size(w * 0.64f, h * 0.16f))
                }
            }
        }
    }
}

/**
 * Rory's echo with a word added ("go" to "go, boat"): the words he already said sit still, and
 * each new word slides in on its own paper scrap, one after another (under half a second each).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ExpandChip(base: String, full: String, modifier: Modifier = Modifier) {
    val still = com.zeronolidge.speechapp.ui.art.LocalReduceMotion.current
    val had = base.lowercase().split(Regex("[^a-z']+")).filter { it.isNotBlank() }.toSet()
    val words = full.split(" ").filter { it.isNotBlank() }
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        var newIndex = 0
        words.forEach { w ->
            val isNew = w.lowercase().trim { !it.isLetter() } !in had
            val order = if (isNew) newIndex++ else 0
            val slide = remember(full, w) { Animatable(if (isNew && !still) 0f else 1f) }
            LaunchedEffect(full, w) {
                if (isNew && !still) {
                    kotlinx.coroutines.delay(250L * order)
                    slide.animateTo(1f, tween(380))
                }
            }
            Text(
                w, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PaperStyle.ink, maxLines = 1,
                modifier = Modifier
                    .graphicsLayer {
                        translationX = (1f - slide.value) * 60.dp.toPx()
                        alpha = slide.value
                    }
                    .paperCard(if (isNew) Color(0xFFFFF59D) else PaperStyle.chip, 10.dp)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}
