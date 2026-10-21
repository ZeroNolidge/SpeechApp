package com.zeronolidge.speechapp.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import com.zeronolidge.speechapp.data.SpeechContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.audio.VoiceTry
import com.zeronolidge.speechapp.logic.BalloonGame
import com.zeronolidge.speechapp.logic.GameBeatPhase
import com.zeronolidge.speechapp.logic.GameBeatState
import com.zeronolidge.speechapp.logic.PatternRound
import com.zeronolidge.speechapp.logic.Prop
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.logic.VoiceStories
import com.zeronolidge.speechapp.logic.VoicePush
import com.zeronolidge.speechapp.logic.WordStat
import com.zeronolidge.speechapp.ui.Nav
import com.zeronolidge.speechapp.ui.Screen
import com.zeronolidge.speechapp.ui.placeKey
import com.zeronolidge.speechapp.ui.art.Critter
import com.zeronolidge.speechapp.ui.art.HeroWithPet
import com.zeronolidge.speechapp.ui.art.LocalReduceMotion
import com.zeronolidge.speechapp.ui.art.Mood
import com.zeronolidge.speechapp.ui.art.PaperStyle
import com.zeronolidge.speechapp.ui.art.VoiceBeatScene
import com.zeronolidge.speechapp.logic.Move
import com.zeronolidge.speechapp.ui.art.VoiceListenExtras
import com.zeronolidge.speechapp.ui.art.HolePunchConfetti
import com.zeronolidge.speechapp.ui.art.balloonColor
import com.zeronolidge.speechapp.ui.art.loop
import com.zeronolidge.speechapp.ui.art.paperBalloon
import com.zeronolidge.speechapp.ui.art.voiceTarget
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.runtime.mutableFloatStateOf
import com.zeronolidge.speechapp.ui.art.paperCard
import com.zeronolidge.speechapp.ui.components.popIn
import com.zeronolidge.speechapp.ui.components.AnswerCard
import com.zeronolidge.speechapp.ui.components.ContinueAction
import com.zeronolidge.speechapp.ui.components.ContinueHost
import com.zeronolidge.speechapp.ui.components.ContinueReserve
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.PaperChip
import com.zeronolidge.speechapp.ui.components.ScreenFrame
import com.zeronolidge.speechapp.ui.components.peelIn
import com.zeronolidge.speechapp.ui.components.revealLine
import com.zeronolidge.speechapp.ui.components.wiggleOnTap
import com.zeronolidge.speechapp.ui.platform.rememberMicPermission
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private enum class VPhase { INTRO, MODEL, LISTEN, WAIT_TAP, ACT, END, GAME }

/**
 * A voice story (logic/VoiceStories.kt): his word makes each thing happen. Every beat: Rory names
 * the problem, says the word three times (slower the third time), then the listening look (the
 * thing to move glows). His voice pushes the action live ([VoicePush]): louder pushes further,
 * holding the sound keeps it moving, shown as paper wind strips from Rory. Any voice finishes it
 * (the rest gently); a quiet try nudges it and Rory says "Again, together!"; the second try and
 * "I said it!" always finish it. Every success: hole-punch confetti, then Rory says it back and
 * adds one more ("Go! Go, boat!"). A calm goodbye beat, then a sticker.
 */
@Composable
fun VoiceStoryScreen(id: String, nav: Nav, firstBeat: Int = 0) {
    val app = LocalApp.current
    val p = app.progress
    val story = remember(id) { VoiceStories.ready(VoiceStories.byId(id) ?: VoiceStories.boat, p.customWords) }
    var beats by remember(id) { mutableStateOf(story.beats) }
    val mic = rememberMicPermission()
    val scope = rememberCoroutineScope()
    val still = LocalReduceMotion.current
    var index by remember { mutableIntStateOf(-1) }
    var phase by remember { mutableStateOf(VPhase.INTRO) }
    var strengths by remember { mutableStateOf(mapOf<Int, Float>()) }
    var session by remember { mutableIntStateOf(0) }
    var sticker by remember { mutableStateOf<WordStat?>(null) }
    /** The current beat's action, 0 to 1, moved live by his voice (see [VoicePush]). */
    val act = remember { Animatable(0f) }
    /** How hard his voice pushes right now (0 when quiet), for the paper wind strips. */
    var pushNow by remember { mutableFloatStateOf(0f) }
    /** Hole-punch confetti: where, and its 0 to 1 run (1.6 s). */
    var confettiAt by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    val confetti = remember { Animatable(1f) }
    val model = remember { arrayOf(VoicePush()) }
    /** Modelling on: the sound to model after this beat's move, and whether it was modelled already. */
    val modelSound = remember { arrayOf<String?>(null) }
    val modelledBeat = remember { arrayOf(false) }
    /** The sound whose mouth shape Rory shows while he models it. */
    var mouthCue by remember { mutableStateOf<String?>(null) }
    /** One pulse of the word chip per word Rory models (the slow third one bigger). */
    var chipBeat by remember { mutableIntStateOf(0) }
    var chipBig by remember { mutableStateOf(false) }
    val chipPulse = remember { Animatable(1f) }
    LaunchedEffect(chipBeat) {
        if (chipBeat > 0 && !still) {
            chipPulse.animateTo(if (chipBig) 1.25f else 1.12f, tween(if (chipBig) 420 else 160))
            chipPulse.animateTo(1f, tween(if (chipBig) 480 else 220))
        }
    }
    // The balloon colour-pattern puzzle (see GameBeat.kt), for the one beat with `game = true`.
    val gameRnd = remember(id) { Random(Random.nextLong()) }
    var puzzle by remember { mutableStateOf<PatternRound?>(null) }
    var gamePhase by remember { mutableStateOf(GameBeatState()) }
    var wrongChoice by remember { mutableIntStateOf(-1) }
    var wiggleBump by remember { mutableIntStateOf(0) }
    // The choice balloons' screen centres (CSS px), for the GAMEBEAT log the screenshot test reads.
    val choiceCenters = remember { arrayOf(Offset.Zero, Offset.Zero, Offset.Zero) }
    fun logGame(g: GameBeatState) {
        val round = puzzle ?: return
        val answerIndex = round.choice.options.indexOf(round.choice.answer)
        val pts = choiceCenters.joinToString(";") { "${it.x.toInt()},${it.y.toInt()}" }
        println("GAMEBEAT phase=${g.phase} choices=$pts answerIndex=$answerIndex")
    }
    fun micOk() = p.useMic && app.voice.available && mic.granted()
    fun line(k: Int) = beats[k].line(p.speechStep)
    fun strength(k: Int) = strengths[k] ?: 0.7f

    lateinit var startBeat: (Int) -> Unit

    fun finish() {
        phase = VPhase.END
        p.finishVoiceStory(story.id)
        // The sticker for his collection (shiny).
        sticker = p.recordWord(story.sticker.first, story.sticker.second, clear = true, heard = false)
        app.celebrate(story.ending)
        app.rememberInstruction("${story.ending} Tap the big green arrow.")
    }

    /** It worked: hole-punch confetti at the thing that moved (gone in under 2 s). */
    fun cheer(k: Int) {
        confettiAt = voiceTarget(beats[k], act.value, strength(k))
        scope.launch {
            confetti.snapTo(0f)
            confetti.animateTo(1f, tween(if (still) 0 else 1600))
        }
    }

    /** After the action: Rory says it back and adds one more, tells what happened, and the story goes on. */
    fun after(k: Int, heard: Boolean, micUsed: Boolean) {
        val s = session
        // Modelling on, after a confident near-miss: the move has played; now Rory models the
        // sound once, clearly, with his mouth shape, and listens for one more try (any try is fine).
        modelSound[0]?.let { id ->
            modelSound[0] = null
            modelledBeat[0] = true
            val target = SpeechContent.sounds.first { it.id == id }
            phase = VPhase.MODEL
            app.listening = false
            mouthCue = id
            app.speaker.say("Listen. ${target.modelSound}. ${line(k)}.")
            app.speaker.then("One more time!") {
                if (s != session) return@then
                mouthCue = null
                phase = VPhase.LISTEN
                app.listening = true
                val again = app.wordCheck(line(k), beats[k].sound, modeled = true)
                app.voice.listenTry(4000, onLevel = { app.listenLevel = it }, onVoice = null, words = again?.grammar,
                    onWords = { h -> again?.let { app.wordHeard(it, h) } }) { _ ->
                    if (s != session) return@listenTry
                    app.listening = false
                    app.listenLevel = 0f
                    after(k, heard, micUsed)
                }
            }
            return
        }
        phase = VPhase.ACT
        val b = beats[k]
        // The colour talk turn just finished: Rory models the pop once, then waits for his own
        // pop (see GameBeatPhase.MODEL_POP); nothing here records a word or a gem yet, since the
        // beat isn't done until he's popped it himself.
        if (b.game && gamePhase.phase == GameBeatPhase.TALK_COLOUR) {
            p.gameBeatResult(gamePhase.rightFirstTry)
            gamePhase = gamePhase.talked()
            logGame(gamePhase)
            return
        }
        // The pop talk turn just finished (his voice, or "I said it!"): the fix is about to play
        // (success line + sticker).
        if (b.game) {
            gamePhase = gamePhase.popped()
            logGame(gamePhase)
        }
        p.recordWord(b.word, b.emoji, clear = heard, heard = micUsed)
        if (micUsed) p.speechResult(heard)
        p.addGem()?.let { app.showGift(it) }
        app.speaker.say(b.success) {
            if (s != session) return@say
            if (b.game) { gamePhase = gamePhase.closed(); logGame(gamePhase) }
            when {
                k + 1 >= beats.size -> finish()
                // After about 12 minutes of play: straight to the calm goodbye beat.
                !beats[k + 1].goodbye && app.sessionLong() -> startBeat(beats.lastIndex)
                else -> startBeat(k + 1)
            }
        }
    }

    /** The rest of the action at a gentle speed ("I said it!", or after a try), then confetti and Rory's echo. */
    fun finishGently(k: Int, heard: Boolean, micUsed: Boolean) {
        val s = session
        app.listening = false
        pushNow = 0f
        strengths = strengths + (k to model[0].strength)
        phase = VPhase.ACT
        scope.launch {
            if (act.value < 1f) act.animateTo(1f, tween(if (still) 0 else (1400 * (1f - act.value)).toInt().coerceAtLeast(250)))
            if (s != session) return@launch
            cheer(k)
            after(k, heard, micUsed)
        }
    }

    fun waitForTap(k: Int, ask: String) {
        phase = VPhase.WAIT_TAP
        // Rory still listens the same way, beside the "I said it!" button.
        app.listening = true
        app.speaker.say(ask)
        app.rememberInstruction("${line(k)}! $ask")
    }

    lateinit var listen: (Int) -> Unit
    listen = { k ->
        val s = session
        if (!micOk()) {
            waitForTap(k, "Say it out loud! Then tap the big green I said it button.")
        } else {
            phase = VPhase.LISTEN
            app.listening = true
            var reached = false
            // Word listening: records what was heard (listen-only). With modelling on, the
            // answer waits (about a second at most) for the words; the action has already moved.
            val check = app.wordCheck(line(k), beats[k].sound, modeled = !model[0].waiting)
            val waitWords = check != null && app.modelingOn && !modelledBeat[0]
            var loud: VoiceTry? = null
            lateinit var handle: (VoiceTry, Pair<com.zeronolidge.speechapp.logic.Heard, Float>?) -> Unit
            app.voice.listenTry(
                4000,
                onLevel = { app.listenLevel = it },
                onVoice = null,
                // Every 50 ms: louder pushes further, holding the sound keeps it moving.
                onPush = { push ->
                    if (s == session && phase == VPhase.LISTEN) {
                        pushNow = push
                        if (push > 0f && !reached) {
                            reached = model[0].feed(push, 50)
                            strengths = strengths + (k to model[0].strength)
                            val to = model[0].progress
                            scope.launch { act.animateTo(to, tween(if (still) 0 else 70)) }
                            if (reached) cheer(k)
                        }
                    }
                },
                words = check?.grammar,
                onWords = { heard ->
                    val judged = check?.let { app.wordHeard(it, heard) }
                    if (waitWords) loud?.let { handle(it, judged) }
                },
                mode = model[0].mode,
            ) { t -> if (waitWords) loud = t else handle(t, null) }
            handle = handle@{ t, judged ->
                if (s != session || phase != VPhase.LISTEN) return@handle
                app.listenLevel = 0f
                pushNow = 0f
                if (t == VoiceTry.NO_MIC) {
                    waitForTap(k, "Say it out loud! Then tap the big green I said it button.")
                    return@handle
                }
                when (model[0].afterTry(heard = t == VoiceTry.HEARD, soft = t == VoiceTry.SOFT)) {
                    // He spoke: the rest of the action goes on by itself (gently), then confetti.
                    VoicePush.Next.FINISH_GENTLY -> {
                        if (waitWords && check != null && judged != null) {
                            val reply = com.zeronolidge.speechapp.logic.Corrections.reply(check, judged.first, judged.second, app.corrections())
                            val canModel = reply == com.zeronolidge.speechapp.logic.Reply.MODEL_SOUND && SpeechContent.sounds.any { it.id == check.sound }
                            app.corrections().noteReply(check.sound, canModel)
                            // A clear match: the full-strength move.
                            if (reply == com.zeronolidge.speechapp.logic.Reply.CELEBRATE) strengths = strengths + (k to 1f)
                            // A confident near-miss: after the move, Rory models the sound once.
                            if (canModel) modelSound[0] = check.sound
                        }
                        if (reached) {
                            phase = VPhase.ACT
                            app.listening = false
                            after(k, heard = t == VoiceTry.HEARD, micUsed = true)
                        } else {
                            finishGently(k, heard = t == VoiceTry.HEARD, micUsed = true)
                        }
                    }
                    // Quiet: it still moves a little, then "Again, together!" and one more listen.
                    VoicePush.Next.AGAIN_TOGETHER -> {
                        phase = VPhase.MODEL
                        app.listening = false
                        strengths = strengths + (k to model[0].strength)
                        val to = model[0].progress
                        scope.launch { act.animateTo(to, tween(if (still) 0 else 500)) }
                        app.speaker.say("Again, together!")
                        app.speaker.then(line(k), slow = true) { if (s == session) listen(k) }
                    }
                    // Still quiet after two tries: it waits for his voice or "I said it!" and
                    // never moves on by itself. Rory asks once (not listening while he talks),
                    // then just listens.
                    VoicePush.Next.WAIT -> if (model[0].tries > VoicePush.MAX_TRIES) {
                        listen(k)
                    } else {
                        phase = VPhase.MODEL
                        app.listening = false
                        app.speaker.say("Say it out loud! Then tap the big green I said it button.") { if (s == session) listen(k) }
                        app.rememberInstruction("${line(k)}! Say it out loud! Then tap the big green I said it button.")
                    }
                }
            }
        }
    }

    /** Rory says the word three times, slower the third time (the word chip pulses once as each
     * one starts, the slow third one bigger), then listens. Shared by every beat's model turn and,
     * once the puzzle below has picked a word, the game beat's own. */
    fun modelWord(k: Int, s: Int) {
        val say = line(k)
        fun pulse(big: Boolean) { if (s == session) { chipBig = big; chipBeat++ } }
        pulse(false)
        app.speaker.say(say) { pulse(false) }
        app.speaker.then(say) { pulse(true) }
        app.speaker.then(say, slow = true) { if (s == session && phase == VPhase.MODEL) listen(k) }
    }

    startBeat = { k ->
        session++
        val s = session
        index = k
        // A page reload carries on from this beat.
        app.notePlace(com.zeronolidge.speechapp.ui.Screen.VoiceStory(id, k).placeKey())
        model[0] = VoicePush(VoicePush.modeFor(beats[k].sound))
        modelSound[0] = null
        modelledBeat[0] = false
        mouthCue = null
        pushNow = 0f
        scope.launch { act.snapTo(0f) }
        app.listening = false
        val b = beats[k]
        if (b.game) {
            // The puzzle comes first: Rory names it, then waits for a tap (see GameBeatState).
            // Which word he ends up saying isn't known until it's solved, so the usual "say the
            // word three times" wait till then.
            puzzle = BalloonGame.make(p.gamePatternStep, gameRnd)
            gamePhase = GameBeatState()
            wrongChoice = -1
            phase = VPhase.GAME
            app.rememberInstruction(b.problem)
            logGame(gamePhase)
            app.speaker.say(b.problem) { if (s == session) { gamePhase = gamePhase.named(); logGame(gamePhase) } }
        } else {
            phase = VPhase.MODEL
            app.rememberInstruction("${b.problem} ${line(k)}. ${line(k)}. ${line(k)}.")
            app.speaker.say(b.problem) { modelWord(k, s) }
        }
    }

    /** The puzzle is solved (right away, or after Rory showed the answer): the word to say is
     * now known, so the usual talk turn (full listening look, "I said it!") plays out exactly
     * like any other beat's. The balloon slides into the gap flat (Move.SLIDE); it won't inflate
     * until he pops it himself, in the second talk turn below. */
    fun startGameTalk() {
        val k = index
        val round = puzzle ?: return
        val word = BalloonGame.talkWord(round.choice.answer)
        val fixed = beats[k].copy(
            lines = BalloonGame.lines(word),
            sound = BalloonGame.sound(word),
            prop = Prop.GAP_BALLOON,
            gapColor = round.choice.answer,
        )
        beats = beats.toMutableList().also { it[k] = fixed }
        val s = session
        phase = VPhase.MODEL
        app.listening = false
        app.rememberInstruction("${fixed.line(p.speechStep)}. ${fixed.line(p.speechStep)}. ${fixed.line(p.speechStep)}.")
        modelWord(k, s)
    }

    /** Rory's asked for the pop once (see GameBeatPhase.MODEL_POP -> TALK_POP): the second talk
     * turn plays out just like any other beat's (full listening look, "I said it!" backup, the
     * same quiet/re-prompt rules), but never moves on by itself. Only his pop (or "I said it!")
     * inflates the balloon (Move.GROW; see VoiceBeatScene's Prop.GAP_BALLOON look). */
    fun startGamePop() {
        val k = index
        val fixed = beats[k].copy(lines = BalloonGame.popLines, sound = "p", move = Move.GROW, success = BalloonGame.success())
        beats = beats.toMutableList().also { it[k] = fixed }
        model[0] = VoicePush(VoicePush.modeFor("p"))
        modelSound[0] = null
        modelledBeat[0] = false
        mouthCue = null
        pushNow = 0f
        scope.launch { act.snapTo(0f) }
        app.listening = false
        phase = VPhase.MODEL
        app.rememberInstruction("${fixed.line(p.speechStep)}! Say it out loud! Then tap the big green I said it button.")
        listen(k)
    }

    /** A tap on one of the puzzle's choice balloons (ignored outside the puzzle phase itself). */
    fun tapChoice(i: Int) {
        val round = puzzle ?: return
        if (gamePhase.phase != GameBeatPhase.PUZZLE) return
        val right = round.choice.options[i] == round.choice.answer
        val next = gamePhase.tap(right)
        if (!right && next.phase == GameBeatPhase.WIGGLE) wiggleBump++
        wrongChoice = if (!right) i else wrongChoice
        gamePhase = next
        logGame(next)
    }

    // The wiggle settles back to the puzzle (the right choice glowing), and a shown answer moves
    // on to the usual talk turn, entirely on their own: no tap needed.
    LaunchedEffect(gamePhase.phase) {
        val s = session
        when (gamePhase.phase) {
            GameBeatPhase.WIGGLE -> {
                app.speaker.say("Try again!")
                delay(650)
                if (s == session) {
                    gamePhase = gamePhase.wiggled()
                    logGame(gamePhase)
                }
            }
            GameBeatPhase.SHOW_ANSWER -> {
                val answer = puzzle?.choice?.answer ?: ""
                app.speaker.say(revealLine(answer)) {
                    if (s == session) {
                        gamePhase = gamePhase.shown()
                        logGame(gamePhase)
                    }
                }
            }
            GameBeatPhase.TALK_COLOUR -> startGameTalk()
            GameBeatPhase.MODEL_POP -> {
                val color = puzzle?.choice?.answer ?: ""
                app.speaker.say(BalloonGame.modelPop(color)) {
                    if (s == session) {
                        gamePhase = gamePhase.modeled()
                        logGame(gamePhase)
                    }
                }
            }
            GameBeatPhase.TALK_POP -> startGamePop()
            else -> {}
        }
    }

    LaunchedEffect(Unit) {
        app.rememberInstruction(story.setup)
        val s = session
        // The first talk turn of a sitting: a quick quiet check of the room before Rory talks.
        val first = firstBeat.coerceIn(0, beats.lastIndex)
        val go = { if (s == session) app.speaker.say(story.setup) { if (s == session && phase == VPhase.INTRO) startBeat(first) } }
        if (micOk()) app.voice.checkRoom(go) else go()
    }
    DisposableEffect(Unit) {
        onDispose {
            session++
            app.voice.cancel()
            app.speaker.stop()
            app.listening = false
            app.listenLevel = 0f
        }
    }

    ScreenFrame(onHome = nav.home, background = {}, top = { PaperChip("${story.emoji} ${story.title}", fontSize = 20.sp, maxLines = 1) }) {
        ContinueHost {
            // Rory stands in the one listening spot, moved aside only if he'd cover the path of
            // the thing his voice moves.
            val beat = beats[index.coerceAtLeast(0)]
            ListeningStage(
                Modifier.fillMaxSize(),
                aboveDim = { rory ->
                    // The thing to move glows; his voice streams to it as paper wind strips.
                    if (index >= 0 && phase != VPhase.END) {
                        VoiceListenExtras(
                            target = { voiceTarget(beat, act.value, strength(index)) },
                            fromX = rory.x, fromY = rory.y,
                            // The glow shows what will move while Rory names it, and hides on his
                            // turn so it never overlaps Rory's ear.
                            glowing = phase == VPhase.MODEL && !app.listening,
                            push = { pushNow },
                        )
                    }
                    HolePunchConfetti(confettiAt, { confetti.value })
                },
            ) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val w = maxWidth
                    val h = maxHeight
                    // While he's choosing, the choice strip is reserved space, not an overlay:
                    // the scene sits fully above it, never behind it. Landscape (wider than tall)
                    // uses a slimmer strip (~18% of the stage); portrait keeps the taller one.
                    val landscape = phase == VPhase.GAME && w > h
                    val stripFraction = if (landscape) BalloonLandscapeStripFraction else BalloonStripFraction
                    val gameStrip = if (phase == VPhase.GAME) (h * stripFraction).coerceAtLeast(BalloonStripMinHeight) else 0.dp
                    Box(Modifier.align(Alignment.TopStart).fillMaxWidth().height(h - gameStrip).clipToBounds()) {
                        if (landscape) {
                            // Keep the scene at its usual full-stage size (so Bear is never
                            // shrunk) and move it up clear of the strip instead, with room to
                            // spare above the strip's top edge; the lifted-off top is clipped.
                            val lift = gameStrip + BalloonLandscapeGap
                            Box(Modifier.fillMaxWidth().height(h).offset(y = -lift)) {
                                VoiceBeatScene(beat, { if (index < 0) 0f else act.value }, strength(index), { pushNow }, Modifier.fillMaxSize(), puzzleSafe = phase == VPhase.GAME)
                            }
                        } else {
                            VoiceBeatScene(beat, { if (index < 0) 0f else act.value }, strength(index), { pushNow }, Modifier.fillMaxSize(), puzzleSafe = phase == VPhase.GAME)
                        }
                    }
                    // The sticker he earned, peeling on in the middle at the end.
                    sticker?.let { st ->
                        Box(Modifier.align(Alignment.Center)) { Sticker(st, minOf(h * 0.35f, 180.dp), Modifier.peelIn(st.text)) }
                    }
                    // The buddy and Rory, bottom-left, clear of the green button's corner. Hidden
                    // under the puzzle panel while he's choosing: nothing else competes for taps.
                    val narrow = w < 520.dp
                    if (phase != VPhase.GAME) Row(
                        Modifier.align(Alignment.BottomStart).padding(start = 4.dp).height(h * if (narrow) 0.24f else 0.3f).widthIn(max = (w - ContinueReserve).coerceAtLeast(0.dp)),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        val species = p.hero ?: Species.KID
                        HeroWithPet(species, p.heroColor(species), p.outfit, null, Modifier.fillMaxHeight().wiggleOnTap(), if (phase == VPhase.END) Mood.CHEER else Mood.HAPPY)
                        // Rory beside the buddy, his mouth going as he models each word (on a phone
                        // too while he talks; the big listening Rory takes over on his turn).
                        if (listenLookGone() && (!narrow || phase == VPhase.MODEL)) Critter(Species.ROBOT, Modifier.fillMaxHeight(0.75f), Color(0xFFFFCC80), mood = Mood.HAPPY, talking = phase == VPhase.MODEL || phase == VPhase.ACT)
                    }
                    // The balloon pattern puzzle, right in the scene: Rory's named the problem, now
                    // it waits for a tap (see GameBeatState). Nothing times out, nothing else moves.
                    if (phase == VPhase.GAME) puzzle?.let { round ->
                        BalloonPatternPuzzle(
                            round, gamePhase, wrongChoice, wiggleBump, gameStrip,
                            onTap = ::tapChoice,
                            onCenter = { i, c -> choiceCenters[i] = c },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            // The word to say, big, at the top: drawn over everything (scene, dim and the glowing
            // prop), so it's never hidden. When the prop starts high (it sinks), the word sits
            // top-left, clear of it.
            val k = index
            // After the action: Rory's echo adds a word, and the new words slide in on paper
            // ("go" to "go, boat").
            if (k >= 0 && phase == VPhase.ACT) {
                val more = beats[k].line(p.speechStep + 1)
                Row(
                    Modifier.align(if (beats[k].move == Move.SINK) Alignment.TopStart else Alignment.TopCenter)
                        .padding(top = 8.dp, start = 8.dp).widthIn(max = 560.dp).zIndex(4f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(beats[k].emoji, fontSize = 34.sp)
                    Spacer(Modifier.width(8.dp))
                    ExpandChip(line(k), more)
                }
            }
            if (k >= 0 && phase != VPhase.END && phase != VPhase.ACT && phase != VPhase.GAME) {
                Row(
                    Modifier.align(if (beats[k].move == Move.SINK) Alignment.TopStart else Alignment.TopCenter)
                        .padding(top = 8.dp, start = 8.dp)
                        .widthIn(max = 560.dp)
                        .zIndex(4f)
                        .graphicsLayer { scaleX = chipPulse.value; scaleY = chipPulse.value }
                        .paperCard(PaperStyle.chip, 22.dp)
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(beats[k].emoji, fontSize = 40.sp, modifier = Modifier.wiggleOnTap())
                    Spacer(Modifier.width(10.dp))
                    Text(line(k), fontSize = if (line(k).length > 14) 28.sp else 40.sp, fontWeight = FontWeight.ExtraBold, color = PaperStyle.ink, maxLines = 1)
                }
            }
            // Rory models a sound: his mouth shape for it, big, top-right.
            mouthCue?.let { MouthCue(it, Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp).zIndex(4f).popIn("mouth")) }
            // "I said it!" is always there in the same spot: it does the same action as his voice.
            // The puzzle has no continue button at all (a quiz step moves on by answering).
            if (phase != VPhase.GAME) ContinueAction(
                hint = phase == VPhase.WAIT_TAP || phase == VPhase.END || (phase == VPhase.LISTEN && model[0].waiting),
                label = if (phase == VPhase.END || phase == VPhase.INTRO) null else "I said it!",
            ) {
                when (phase) {
                    VPhase.INTRO -> {
                        app.speaker.stop()
                        startBeat(0)
                    }
                    VPhase.MODEL, VPhase.LISTEN, VPhase.WAIT_TAP -> {
                        val k = index
                        session++
                        app.voice.cancel()
                        app.speaker.stop()
                        // The same action as his voice, at a gentle speed.
                        model[0].pressed()
                        finishGently(k, heard = false, micUsed = false)
                    }
                    VPhase.ACT, VPhase.GAME -> {}
                    VPhase.END -> if (app.sessionLong()) nav.go(Screen.Rest) else nav.home()
                }
            }
        }
    }
}

/** The centre of [c], in window px (CSS px on the web): what the GAMEBEAT log reports. */
private fun choiceCenter(c: androidx.compose.ui.layout.LayoutCoordinates): Offset {
    val o = c.positionInWindow()
    return Offset(o.x + c.size.width / 2f, o.y + c.size.height / 2f)
}

/** One paper-style balloon: a round body on a thin string. */
@Composable
private fun Balloon(color: Color, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) { paperBalloon(color) }
}

/** The pattern's empty slot: a dashed outline (gently pulsing while he decides) until the answer
 * is shown, then the same balloon. */
@Composable
private fun GapBalloon(size: Dp, color: Color?, modifier: Modifier = Modifier) {
    if (color != null) {
        Balloon(color, size, modifier.popIn("gap"))
    } else {
        val t = rememberInfiniteTransition(label = "gap")
        val scale by t.loop(1f, 1.12f, 750, label = "gapScale")
        val alpha by t.loop(0.55f, 1f, 750, label = "gapAlpha")
        Canvas(modifier.size(size).graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }) {
            drawCircle(
                Color(0xFF9E9E9E), this.size.minDimension * 0.3f, Offset(this.size.width / 2f, this.size.height * 0.42f),
                style = Stroke(2.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 7f))),
            )
        }
    }
}

/** The strip's share of the stage height, and its floor so it's never cramped on a short
 * landscape screen (see [BalloonPatternPuzzle]). */
private const val BalloonStripFraction = 0.22f
/** Landscape (stage wider than tall) uses a slimmer strip, since there's less height to spare;
 * the scene above it is moved up, not shrunk (see the puzzle's scene box in VoiceStoryScreen). */
private const val BalloonLandscapeStripFraction = 0.18f
/** The least clear space kept between Bear's feet and the strip's top, in landscape. */
private val BalloonLandscapeGap = 16.dp
private val BalloonStripMinHeight = 128.dp
private val BalloonChoiceSize = 88.dp
private val BalloonChoiceGap = 24.dp

/** The soft sky-blue wash behind the choice strip: a colour, not [PaperStyle.sceneryWash]'s
 * cream, so it reads apart from the meadow above it. Solid, not translucent, so it reads as a
 * light sky-blue band rather than a grey wash over whatever sits behind the strip. */
private val BalloonStripWash = Color(0xFFDCEFFB)

/**
 * The balloon colour-pattern puzzle, drawn right in the story scene (see [GameBeatState]): the
 * meadow stays visible above it, shrunk by the caller to sit clear of [stripHeight] (see the
 * caller in `VoiceStory`). The pattern so far sits near the top, its balloons tied to a little
 * rope, with only a small soft paper strip behind the row itself for contrast (never a card over
 * the whole scene); up to 3 big choice balloons sit centred in the bottom strip, a plain band
 * reserved just for them so they never land over the scene below. Nothing moves by itself (but
 * for the empty gap's gentle pulse) and there's no timer; every tap on a choice goes through
 * [onTap], and [onCenter] reports each choice's screen centre (for the GAMEBEAT log the
 * screenshot test reads).
 */
@Composable
private fun BalloonPatternPuzzle(
    round: PatternRound,
    game: GameBeatState,
    wrongChoice: Int,
    wiggleBump: Int,
    stripHeight: Dp,
    onTap: (Int) -> Unit,
    onCenter: (Int, Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val answerShown = game.phase == GameBeatPhase.SHOW_ANSWER
    BoxWithConstraints(modifier) {
        val stageHeight = maxHeight
        BoxWithConstraints(
            Modifier.align(Alignment.TopCenter).padding(top = 16.dp).fillMaxWidth(0.9f).height(stageHeight - stripHeight).padding(bottom = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            val slot = minOf(maxWidth / (round.shown.size + 1.6f), 68.dp)
            Box(
                Modifier.fillMaxWidth().height(slot * 1.7f).paperCard(PaperStyle.sceneryWash, 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(Modifier.fillMaxWidth(0.8f).height(3.dp)) {
                        drawLine(Color(0xFF8D6E63), Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 3.dp.toPx())
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        round.shown.forEach { Balloon(balloonColor(it), slot) }
                        GapBalloon(slot, if (answerShown) balloonColor(round.choice.answer) else null)
                    }
                }
            }
        }
        // The choice strip: its own reserved band at the bottom, the choices centred inside it.
        // No ContinueReserve here: the puzzle has no continue button to leave room for.
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(stripHeight).paperCard(BalloonStripWash, 28.dp),
            horizontalArrangement = Arrangement.spacedBy(BalloonChoiceGap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            round.choice.options.forEachIndexed { i, opt ->
                val right = opt == round.choice.answer
                AnswerCard(
                    modifier = Modifier.size(BalloonChoiceSize).onGloballyPositioned { c -> onCenter(i, choiceCenter(c)) },
                    correct = answerShown && right,
                    glow = game.glow && right && !answerShown,
                    wiggle = if (i == wrongChoice) wiggleBump else 0,
                    onClick = { onTap(i) },
                ) { Balloon(balloonColor(opt), 64.dp) }
            }
        }
    }
}
