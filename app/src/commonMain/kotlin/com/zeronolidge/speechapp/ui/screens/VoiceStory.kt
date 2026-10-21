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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.audio.VoiceTry
import com.zeronolidge.speechapp.logic.VoiceBeat
import com.zeronolidge.speechapp.logic.Backdrop
import com.zeronolidge.speechapp.logic.BalloonGame
import com.zeronolidge.speechapp.logic.Choice
import com.zeronolidge.speechapp.logic.GameBeatPhase
import com.zeronolidge.speechapp.logic.GameBeatState
import com.zeronolidge.speechapp.logic.KitchenBeatState
import com.zeronolidge.speechapp.logic.KitchenEndStage
import com.zeronolidge.speechapp.logic.KitchenEnding
import com.zeronolidge.speechapp.logic.KitchenFlow
import com.zeronolidge.speechapp.logic.KitchenGame
import com.zeronolidge.speechapp.logic.KitchenLayout
import com.zeronolidge.speechapp.logic.KitchenPhase
import com.zeronolidge.speechapp.logic.Numbers
import com.zeronolidge.speechapp.logic.PatternRound
import com.zeronolidge.speechapp.logic.Prop
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.logic.VoiceStories
import com.zeronolidge.speechapp.logic.VoicePush
import com.zeronolidge.speechapp.logic.WordStat
import com.zeronolidge.speechapp.logic.talkGoal
import com.zeronolidge.speechapp.ui.Nav
import com.zeronolidge.speechapp.ui.Screen
import com.zeronolidge.speechapp.ui.placeKey
import com.zeronolidge.speechapp.ui.art.BlueberryArt
import com.zeronolidge.speechapp.ui.art.Critter
import com.zeronolidge.speechapp.ui.art.HeroWithPet
import com.zeronolidge.speechapp.ui.art.LocalReduceMotion
import com.zeronolidge.speechapp.ui.art.Mood
import com.zeronolidge.speechapp.ui.art.PaperStyle
import com.zeronolidge.speechapp.ui.art.VoiceBeatScene
import com.zeronolidge.speechapp.ui.art.KitchenEndingScene
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
import com.zeronolidge.speechapp.ui.components.LocalScreenHeight
import com.zeronolidge.speechapp.ui.components.PaperChip
import com.zeronolidge.speechapp.ui.components.ScreenFrame
import com.zeronolidge.speechapp.ui.components.peelIn
import com.zeronolidge.speechapp.ui.components.revealLine
import com.zeronolidge.speechapp.ui.components.wiggleOnTap
import com.zeronolidge.speechapp.ui.platform.rememberMicPermission
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

private enum class VPhase { INTRO, MODEL, LISTEN, WAIT_TAP, ACT, END, GAME, KITCHEN }

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
    // Rory's Kitchen's tap/talk flow (see KitchenBeatState), for the beats with `kitchen != null`.
    var kitchenPhase by remember { mutableStateOf(KitchenBeatState(KitchenFlow.FLIP)) }
    var kitchenRound by remember { mutableStateOf<Choice<String>?>(null) }
    var kitchenHopped by remember { mutableStateOf(setOf<Int>()) }
    var kitchenWrong by remember { mutableIntStateOf(-1) }
    var kitchenWiggleBump by remember { mutableIntStateOf(0) }
    // The kitchen tap tiles' screen centres (CSS px), for the KITCHEN log below.
    val kitchenCenters = remember { arrayOf(Offset.Zero, Offset.Zero, Offset.Zero) }
    fun logKitchen(k: KitchenBeatState) {
        val b = beats.getOrNull(index) ?: return
        val tiles = kitchenTiles(b, k.flow, kitchenRound)
        val pts = tiles.indices.joinToString(";") { i -> kitchenCenters.getOrElse(i) { Offset.Zero }.let { "${it.x.toInt()},${it.y.toInt()}" } }
        println("KITCHEN step=$index phase=${k.phase} targets=$pts")
    }
    // The counter's on-screen span, in landscape (Ruby, K2a): logged once per step so the
    // screenshot test can check it keeps KitchenLayout.MIN_VISIBLE_FRACTION of the screen.
    var kitchenLayoutLoggedStep by remember { mutableIntStateOf(-1) }
    // Bear, Rory, the bowl, the pan and the choice strip's own on-screen boxes (CSS px), logged
    // once per kitchen beat so Ruby's overlap check (K2b) can be run against a real screenshot.
    var kitchenRectsLoggedStep by remember { mutableIntStateOf(-1) }
    fun micOk() = p.useMic && app.voice.available && mic.granted()
    fun line(k: Int) = beats[k].line(p.speechStep)
    fun strength(k: Int) = strengths[k] ?: 0.7f

    // Rory's Kitchen's own ending (Ruby's K2c): the stack of pancakes, Bear's bite, the sticker's
    // flight into the backpack and the carry-on bye, in place of every other story's "tap the big
    // green arrow" wait (see KitchenEnding.kt). [kitchenEnd] null until the last beat's fix has
    // played; [kitchenSticker] is the sticker's 0 to 1 flight, [backpackBounce] its little landing
    // bounce, [backpackCenter] the backpack icon's own window position once it's drawn (see the
    // `top` row below), captured so the sticker can arc into exactly where it sits.
    val isKitchenStory = story.id == "kitchen"
    var kitchenEnd by remember { mutableStateOf<KitchenEndStage?>(null) }
    val kitchenSticker = remember { Animatable(0f) }
    val backpackBounce = remember { Animatable(1f) }
    var backpackCenter by remember { mutableStateOf<Offset?>(null) }
    var sceneOrigin by remember { mutableStateOf<Offset?>(null) }

    lateinit var startBeat: (Int) -> Unit
    lateinit var startKitchenTalk: () -> Unit
    lateinit var startKitchenEnding: () -> Unit

    fun finish() {
        p.finishVoiceStory(story.id)
        // The sticker for his collection (shiny).
        sticker = p.recordWord(story.sticker.first, story.sticker.second, clear = true, heard = false)
        phase = VPhase.END
        if (isKitchenStory) {
            startKitchenEnding()
        } else {
            app.celebrate(story.ending)
            app.rememberInstruction("${story.ending} Tap the big green arrow.")
        }
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
        // A kitchen talk turn just finished (his voice, or "I said it!"): one more counted; a
        // flow with several tries (stir, bubbles) repeats the same listening turn, otherwise the
        // fix below plays, same as any other beat.
        if (b.kitchen != null) {
            val next = kitchenPhase.talked()
            kitchenPhase = next
            logKitchen(next)
            if (next.phase == KitchenPhase.TALK) {
                startKitchenTalk()
                return
            }
        }
        p.recordWord(b.word, b.emoji, clear = heard, heard = micUsed)
        if (micUsed) p.speechResult(heard)
        p.addGem()?.let { app.showGift(it) }
        app.speaker.say(b.success) {
            if (s != session) return@say
            if (b.game) { gamePhase = gamePhase.closed(); logGame(gamePhase) }
            if (b.kitchen != null) { kitchenPhase = kitchenPhase.closed(); logKitchen(kitchenPhase) }
            if (b.backdrop == Backdrop.KITCHEN && b.kitchen == null) println("KITCHEN step=$k phase=DONE")
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
        // Rory's Kitchen's plain talk-turn beats only (the flow beats log their own TALK through
        // KitchenBeatState/logKitchen); see the NAME log in startBeat and the DONE log in after().
        if (beats.getOrNull(k)?.let { it.backdrop == Backdrop.KITCHEN && it.kitchen == null } == true) println("KITCHEN step=$k phase=TALK")
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
        if (b.kitchen != null) {
            // Rory names the problem, then its tap or talk flow plays out (see KitchenBeatState);
            // the kitchen effect below drives it on from there.
            kitchenPhase = KitchenBeatState(flow = b.kitchen, goal = b.kitchenGoal)
            kitchenRound = if (b.kitchen == KitchenFlow.TAP_CHOICE) KitchenGame.tapRound(b.word, b.kitchenChoices.size + 1, gameRnd) else null
            kitchenHopped = emptySet()
            kitchenWrong = -1
            phase = VPhase.KITCHEN
            app.rememberInstruction(b.problem)
            logKitchen(kitchenPhase)
            app.speaker.say(b.problem) { if (s == session) { kitchenPhase = kitchenPhase.named(); logKitchen(kitchenPhase) } }
        } else if (b.game) {
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
            // Rory's Kitchen's plain talk-turn beats (milk, pour, "mmm") carry no KitchenFlow of
            // their own, but Ruby's K2c test script still drives them from the same KITCHEN log
            // the flow beats print (see `listen`/`after` below for TALK/DONE).
            if (b.backdrop == Backdrop.KITCHEN) println("KITCHEN step=$k phase=NAME")
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

    /** A kitchen talk turn's single model line (not three; see KitchenFlow), then the usual listen. */
    fun modelKitchenWord(k: Int, s: Int) {
        val say = line(k)
        if (s == session) { chipBig = false; chipBeat++ }
        app.speaker.say(say) { if (s == session && phase == VPhase.MODEL) listen(k) }
    }

    startKitchenTalk = {
        val k = index
        val s = session
        // A flow with several tries (stir, bubbles) calls this again for each one: a fresh push
        // and a fresh action, same as any new talk turn, so each stir or pop plays out in full.
        model[0] = VoicePush(VoicePush.modeFor(beats[k].sound))
        modelSound[0] = null
        modelledBeat[0] = false
        mouthCue = null
        pushNow = 0f
        scope.launch { act.snapTo(0f) }
        app.listening = false
        phase = VPhase.MODEL
        app.rememberInstruction("${line(k)}! Say it out loud! Then tap the big green I said it button.")
        modelKitchenWord(k, s)
    }

    // Rory's Kitchen's own ending (Ruby's K2c): no tap needed anywhere in this sequence (see
    // KitchenEnding.kt for the timed stages); the story carries on by itself once Rory's bye line
    // finishes, same place every other story's "tap the big green arrow" would have sent him
    // (nav.go(Screen.Rest) when the sitting's run long, else nav.home()). 🏠 keeps working
    // throughout, since it's drawn by ScreenFrame, outside this whole sequence.
    startKitchenEnding = {
        val s = session
        kitchenEnd = KitchenEndStage.STACK
        println("KITCHEN end stage=STACK")
        scope.launch {
            delay(KitchenEnding.STACK_MS.toLong())
            if (s != session) return@launch
            kitchenEnd = KitchenEndStage.BITE
            println("KITCHEN end stage=BITE")
            delay(KitchenEnding.BITE_MS.toLong())
            if (s != session) return@launch
            kitchenEnd = KitchenEndStage.STICKER_FLY
            println("KITCHEN end stage=STICKER_FLY")
            kitchenSticker.snapTo(0f)
            launch { kitchenSticker.animateTo(1f, tween(if (still) 0 else KitchenEnding.FLY_MS)) }
            delay(KitchenEnding.FLY_MS.toLong())
            if (s != session) return@launch
            kitchenEnd = KitchenEndStage.IN_BACKPACK
            println("KITCHEN end stage=IN_BACKPACK")
            launch {
                backpackBounce.snapTo(1f)
                backpackBounce.animateTo(1.35f, tween(if (still) 0 else 140))
                backpackBounce.animateTo(1f, tween(if (still) 0 else 160))
            }
            delay(KitchenEnding.BOUNCE_MS.toLong())
            if (s != session) return@launch
            kitchenEnd = KitchenEndStage.BYE
            println("KITCHEN end stage=BYE")
            app.rememberInstruction(story.ending)
            app.speaker.say(story.ending) {
                if (s != session) return@say
                scope.launch {
                    delay(KitchenEnding.LEAVE_DELAY_MS.toLong())
                    if (s != session) return@launch
                    println("KITCHEN end stage=LEAVE")
                    if (app.sessionLong()) nav.go(Screen.Rest) else nav.home()
                }
            }
        }
    }

    /** A tap on one of the kitchen flow's tiles (ignored outside its TAP phase; see KitchenBeatState). */
    fun tapKitchen(i: Int) {
        if (kitchenPhase.phase != KitchenPhase.TAP) return
        when (beats[index].kitchen) {
            KitchenFlow.TAP_CHOICE -> {
                val round = kitchenRound ?: return
                val right = round.options.getOrNull(i) == round.answer
                val next = kitchenPhase.tap(right)
                if (!right && next.phase == KitchenPhase.WIGGLE) kitchenWiggleBump++
                if (!right) kitchenWrong = i
                kitchenPhase = next
                logKitchen(next)
            }
            KitchenFlow.COUNT -> {
                if (i in kitchenHopped) return
                kitchenHopped = kitchenHopped + i
                val next = kitchenPhase.tap(true)
                kitchenPhase = next
                app.speaker.say(Numbers.word(next.tapsDone))
                logKitchen(next)
            }
            KitchenFlow.FLIP -> {
                val next = kitchenPhase.tap(true)
                kitchenPhase = next
                logKitchen(next)
            }
            else -> {}
        }
    }

    // The kitchen flow's wiggle and shown-answer settle on their own, same as the balloon puzzle;
    // its talk turn (one model line, then listen) starts once its taps are done, or at once for
    // the stir/bubbles flows (no taps at all); the count flow has no talk turn, so its fix plays
    // here too, straight from its last tap.
    LaunchedEffect(kitchenPhase.phase) {
        val s = session
        val b = beats.getOrNull(index) ?: return@LaunchedEffect
        when (kitchenPhase.phase) {
            KitchenPhase.WIGGLE -> {
                app.speaker.say("Try again!")
                delay(650)
                if (s == session) {
                    kitchenPhase = kitchenPhase.wiggled()
                    logKitchen(kitchenPhase)
                }
            }
            KitchenPhase.SHOW_ANSWER -> {
                val answer = kitchenRound?.answer ?: b.word
                app.speaker.say(revealLine(answer)) {
                    if (s == session) {
                        kitchenPhase = kitchenPhase.shown()
                        logKitchen(kitchenPhase)
                    }
                }
            }
            KitchenPhase.TALK -> startKitchenTalk()
            KitchenPhase.CLOSE -> if (b.kitchen?.talkGoal(b.kitchenGoal) == 0) {
                // The count flow: no talk turn happened, so the fix plays straight from here.
                phase = VPhase.ACT
                app.speaker.then(b.success) {
                    if (s == session) {
                        kitchenPhase = kitchenPhase.closed()
                        logKitchen(kitchenPhase)
                        when {
                            index + 1 >= beats.size -> finish()
                            !beats[index + 1].goodbye && app.sessionLong() -> startBeat(beats.lastIndex)
                            else -> startBeat(index + 1)
                        }
                    }
                }
            }
            else -> {}
        }
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
        val go = {
            if (s == session) {
                // Ruby's K2c: the kitchen story's very first line, before any beat (story.setup
                // itself, unchanged from VoiceStories.kitchen).
                if (isKitchenStory) println("KITCHEN intro say=${story.setup}")
                app.speaker.say(story.setup) { if (s == session && phase == VPhase.INTRO) startBeat(first) }
            }
        }
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

    ScreenFrame(
        onHome = nav.home,
        background = {},
        top = {
            PaperChip("${story.emoji} ${story.title}", fontSize = 20.sp, maxLines = 1)
            // Rory's Kitchen's own ending (Ruby's K2c): the backpack his sticker flies into,
            // clear of 🏠 and 🔊 (this row sits between them; see ScreenFrame). Only shown once
            // the ending starts, so it never crowds the title the rest of the story.
            if (isKitchenStory && kitchenEnd != null) {
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(40.dp)
                        .graphicsLayer { scaleX = backpackBounce.value; scaleY = backpackBounce.value }
                        .onGloballyPositioned { c -> backpackCenter = c.positionInWindow() + Offset(c.size.width / 2f, c.size.height / 2f) },
                    contentAlignment = Alignment.Center,
                ) { Text("🎒", fontSize = 30.sp) }
            }
        },
    ) {
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
                BoxWithConstraints(
                    Modifier.fillMaxSize().let { m ->
                        // Rory's Kitchen's ending (Ruby's K2c): this box's own window position,
                        // so the flying sticker can arc from its centre into the backpack icon up
                        // in the top bar (captured separately; see the `top` row above).
                        if (isKitchenStory) m.onGloballyPositioned { c -> sceneOrigin = c.positionInWindow() } else m
                    },
                ) {
                    val w = maxWidth
                    val h = maxHeight
                    val density = LocalDensity.current
                    val screenHeight = LocalScreenHeight.current
                    // While he's choosing or tapping, the choice strip is reserved space, not an
                    // overlay: the scene sits fully above it, never behind it. Landscape (wider
                    // than tall) uses a slimmer strip (~18% of the stage); portrait keeps the
                    // taller one. The kitchen's tap tiles share the same strip as the balloon puzzle.
                    val stripShown = phase == VPhase.GAME || phase == VPhase.KITCHEN
                    val landscape = stripShown && w > h
                    val isKitchen = phase == VPhase.KITCHEN
                    val stripFraction = if (landscape) BalloonLandscapeStripFraction else BalloonStripFraction
                    val gameStrip = if (stripShown) (h * stripFraction).coerceAtLeast(BalloonStripMinHeight) else 0.dp
                    // Rory's Kitchen: his mouth talks whenever Rory is the one speaking (naming
                    // the step, modelling the word, or echoing after it), and he hides while the
                    // big listening Rory takes over, same as the small bottom-left Rory he replaces.
                    val kitchenTalking = phase == VPhase.MODEL || phase == VPhase.ACT || phase == VPhase.KITCHEN
                    val kitchenRoryVisible = listenLookGone()
                    // Bear, Rory, the bowl, the pan and the choice strip's own on-screen boxes
                    // (CSS px), once per kitchen beat, so Ruby's overlap check can run against a
                    // real screenshot (never printed with the "KITCHEN step=" prefix above).
                    fun logKitchenRects(c: androidx.compose.ui.layout.LayoutCoordinates, sceneLandscape: Boolean) {
                        if (kitchenRectsLoggedStep == index) return
                        kitchenRectsLoggedStep = index
                        val origin = c.positionInWindow()
                        val sw = c.size.width.toFloat()
                        val sh = c.size.height.toFloat()
                        fun fmt(r: KitchenLayout.Rect): String {
                            val l = (origin.x + r.l * sw).roundToInt()
                            val t = (origin.y + r.t * sh).roundToInt()
                            val right = (origin.x + r.r * sw).roundToInt()
                            val b = (origin.y + r.b * sh).roundToInt()
                            return "$l,$t,$right,$b"
                        }
                        val stripTop = origin.y + sh
                        val stripBottomPx = stripTop + with(density) { gameStrip.toPx() }
                        val stripRect = "${origin.x.roundToInt()},${stripTop.roundToInt()},${(origin.x + sw).roundToInt()},${stripBottomPx.roundToInt()}"
                        println(
                            "KITCHEN rects bear=${fmt(KitchenLayout.bearBox())} rory=${fmt(KitchenLayout.roryBox(sceneLandscape))} " +
                                "bowl=${fmt(KitchenLayout.bowlBox(sceneLandscape))} pan=${fmt(KitchenLayout.panBox(sceneLandscape))} strip=$stripRect",
                        )
                    }
                    Box(Modifier.align(Alignment.TopStart).fillMaxWidth().height(h - gameStrip).clipToBounds()) {
                        if (landscape) {
                            // Keep the scene at its usual full-stage size (so Bear/the counter is
                            // never shrunk) and move it up clear of the strip instead, with room
                            // to spare above the strip's top edge; the lifted-off top is clipped.
                            // Rory's Kitchen uses no extra gap (its counter, not sky, sits at the
                            // top once lifted, and KitchenLayout already draws it tall there; see
                            // KitchenLayoutTest): the balloon puzzle's bigger gap would clip into it.
                            val lift = gameStrip + (if (isKitchen) KitchenLandscapeGap else BalloonLandscapeGap)
                            Box(
                                Modifier.fillMaxWidth().height(h).offset(y = -lift).let { m ->
                                    if (!isKitchen) return@let m
                                    m.onGloballyPositioned { c ->
                                        if (kitchenLayoutLoggedStep == index) return@onGloballyPositioned
                                        kitchenLayoutLoggedStep = index
                                        val top = c.positionInWindow().y
                                        val sceneHeightPx = c.size.height.toFloat()
                                        val counterTop = top + KitchenLayout.counterTop(true) * sceneHeightPx
                                        val counterBottom = top + sceneHeightPx
                                        val screenHPx = with(density) { screenHeight.toPx() }
                                        println("KITCHEN layout counterTop=${counterTop.roundToInt()} counterBottom=${counterBottom.roundToInt()} screenH=${screenHPx.roundToInt()}")
                                    }
                                },
                            ) {
                                val end = kitchenEnd
                                if (end != null) {
                                    KitchenEndingScene(end, landscape = true, modifier = Modifier.fillMaxSize())
                                } else {
                                    VoiceBeatScene(
                                        beat, { if (index < 0) 0f else act.value }, strength(index), { pushNow }, Modifier.fillMaxSize(),
                                        puzzleSafe = stripShown, landscape = true,
                                        kitchenTalking = kitchenTalking, kitchenRoryVisible = kitchenRoryVisible,
                                        onSceneRect = if (beat.backdrop == Backdrop.KITCHEN) { c -> logKitchenRects(c, landscape) } else null,
                                    )
                                }
                            }
                        } else {
                            val end = kitchenEnd
                            if (end != null) {
                                KitchenEndingScene(end, landscape = false, modifier = Modifier.fillMaxSize())
                            } else {
                                VoiceBeatScene(
                                    beat, { if (index < 0) 0f else act.value }, strength(index), { pushNow }, Modifier.fillMaxSize(),
                                    puzzleSafe = stripShown,
                                    kitchenTalking = kitchenTalking, kitchenRoryVisible = kitchenRoryVisible,
                                    onSceneRect = if (beat.backdrop == Backdrop.KITCHEN) { c -> logKitchenRects(c, landscape) } else null,
                                )
                            }
                        }
                    }
                    // The sticker he earned, peeling on in the middle at the end. Rory's Kitchen's
                    // own ending flies its sticker into the backpack instead (see below): no
                    // screen-takeover card for that story.
                    if (!isKitchenStory) sticker?.let { st ->
                        Box(Modifier.align(Alignment.Center)) { Sticker(st, minOf(h * 0.35f, 180.dp), Modifier.peelIn(st.text)) }
                    }
                    // Rory's Kitchen's own ending (Ruby's K2c): the sticker arcs from the middle
                    // of this scene up into the backpack icon in the top bar, shrinking as it
                    // nears it; it stays there (scaled right down) through IN_BACKPACK too, so it
                    // never pops back to its starting size.
                    if (isKitchenStory && (kitchenEnd == KitchenEndStage.STICKER_FLY || kitchenEnd == KitchenEndStage.IN_BACKPACK)) sticker?.let { st ->
                        val wPx = with(density) { w.toPx() }
                        val hPx = with(density) { h.toPx() }
                        val start = Offset(wPx / 2f, hPx * 0.42f)
                        val target = backpackCenter?.let { bp -> sceneOrigin?.let { origin -> bp - origin } }
                        val end = target ?: start
                        val t = kitchenSticker.value
                        val bow = hPx * 0.16f
                        val x = start.x + (end.x - start.x) * t
                        val y = start.y + (end.y - start.y) * t - bow * kotlin.math.sin(kotlin.math.PI.toFloat() * t)
                        val scale = 1f - 0.6f * t
                        val stickerSize = minOf(h * 0.3f, 150.dp)
                        val sizePx = with(density) { stickerSize.toPx() }
                        Box(
                            Modifier.offset { IntOffset((x - sizePx * scale / 2f).roundToInt(), (y - sizePx * scale / 2f).roundToInt()) }
                                .zIndex(6f),
                        ) { Sticker(st, stickerSize, Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) }
                    }
                    // The buddy and Rory, bottom-left, clear of the green button's corner. Hidden
                    // under the puzzle/tap panel while he's choosing: nothing else competes for taps.
                    val narrow = w < 520.dp
                    if (!stripShown) Row(
                        Modifier.align(Alignment.BottomStart).padding(start = 4.dp).height(h * if (narrow) 0.24f else 0.3f).widthIn(max = (w - ContinueReserve).coerceAtLeast(0.dp)),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        val species = p.hero ?: Species.KID
                        HeroWithPet(species, p.heroColor(species), p.outfit, null, Modifier.fillMaxHeight().wiggleOnTap(), if (phase == VPhase.END) Mood.CHEER else Mood.HAPPY)
                        // Rory beside the buddy, his mouth going as he models each word (on a phone
                        // too while he talks; the big listening Rory takes over on his turn). Rory's
                        // Kitchen's own Rory, behind the counter, replaces him there.
                        if (beat.backdrop != Backdrop.KITCHEN && listenLookGone() && (!narrow || phase == VPhase.MODEL)) Critter(Species.ROBOT, Modifier.fillMaxHeight(0.75f), Color(0xFFFFCC80), mood = Mood.HAPPY, talking = phase == VPhase.MODEL || phase == VPhase.ACT)
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
                    // Rory's Kitchen's tap tiles (bowl choices, berries to count, the pancake to
                    // flip), right in the scene, same as the balloon puzzle above.
                    if (phase == VPhase.KITCHEN) KitchenTapRow(
                        beat, kitchenPhase, kitchenRound, kitchenHopped, kitchenWrong, kitchenWiggleBump, gameStrip,
                        onTap = ::tapKitchen,
                        onCenter = { i, c -> kitchenCenters[i] = c },
                        modifier = Modifier.fillMaxSize(),
                    )
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
            if (k >= 0 && phase != VPhase.END && phase != VPhase.ACT && phase != VPhase.GAME && phase != VPhase.KITCHEN) {
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
            // The puzzle and the kitchen's tap tiles have no continue button at all (a quiz step
            // moves on by answering).
            // Rory's Kitchen's own ending (Ruby's K2c) has no continue button at all: it carries
            // on by itself, no tap needed (🏠, drawn by ScreenFrame outside this whole sequence,
            // still works throughout).
            if (phase != VPhase.GAME && phase != VPhase.KITCHEN && !(isKitchenStory && phase == VPhase.END)) ContinueAction(
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
                    VPhase.ACT, VPhase.GAME, VPhase.KITCHEN -> {}
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
/** Rory's Kitchen's own landscape gap (Ruby, K2a): none, since the counter (not sky) sits at the
 * top once lifted and every extra dp of gap is a dp less of visible counter (see
 * KitchenLayout.MIN_VISIBLE_FRACTION, checked by the KITCHEN layout log above). */
private val KitchenLandscapeGap = 0.dp
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

/** The emoji for a kitchen tap choice word: the bowl beat's own word and its two distractors
 * (see [KitchenGame.distractors]); nothing else ever shows here. */
private fun kitchenWordEmoji(word: String): String = when (word) {
    "bowl" -> "🥣"
    "cup" -> "☕"
    "spoon" -> "🥄"
    else -> "❓"
}

/** The kitchen flow's tap tiles, in order: the bowl choice's shuffled options, three berries to
 * count in (drawn as paper art, not this placeholder; see [KitchenTapRow]), or the pancake to
 * flip (its own emoji); [KitchenFlow.STIR]/[KitchenFlow.BUBBLES] have none (no tap phase at all;
 * see [KitchenFlow.tapGoal]). */
private fun kitchenTiles(beat: VoiceBeat, flow: KitchenFlow, round: Choice<String>?): List<String> = when (flow) {
    KitchenFlow.TAP_CHOICE -> round?.options?.map(::kitchenWordEmoji) ?: emptyList()
    KitchenFlow.COUNT -> List(3) { "🫐" }
    KitchenFlow.FLIP -> listOf(beat.emoji)
    KitchenFlow.STIR, KitchenFlow.BUBBLES -> emptyList()
}

private val KitchenBerryTileSize = 44.dp

private val KitchenChoiceGap = 32.dp

/**
 * Rory's Kitchen's tap tiles (see [KitchenBeatState]), drawn in the same reserved bottom strip as
 * the balloon puzzle above: "Tap the bowl!" among its distractors (wrong taps wiggle, the right
 * one glows after a first miss, same as [BalloonPatternPuzzle]'s choices), three berries that hop
 * in as he taps each one, or the pancake to flip. Empty (and drawn nothing) for the stir/bubbles
 * flows, which have no tap phase. [onCenter] reports each tile's screen centre for the KITCHEN log
 * the screenshot test reads.
 */
@Composable
private fun KitchenTapRow(
    beat: VoiceBeat,
    state: KitchenBeatState,
    round: Choice<String>?,
    hopped: Set<Int>,
    wrongChoice: Int,
    wiggleBump: Int,
    stripHeight: Dp,
    onTap: (Int) -> Unit,
    onCenter: (Int, Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tiles = kitchenTiles(beat, state.flow, round)
    if (tiles.isEmpty()) return
    val answerShown = state.phase == KitchenPhase.SHOW_ANSWER
    Box(modifier) {
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(stripHeight).paperCard(BalloonStripWash, 28.dp),
            horizontalArrangement = Arrangement.spacedBy(KitchenChoiceGap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tiles.forEachIndexed { i, emoji ->
                val isAnswer = state.flow == KitchenFlow.TAP_CHOICE && round != null && round.options.getOrNull(i) == round.answer
                AnswerCard(
                    modifier = Modifier.size(BalloonChoiceSize).onGloballyPositioned { c -> onCenter(i, choiceCenter(c)) },
                    correct = (answerShown && isAnswer) || i in hopped,
                    glow = state.glow && isAnswer && !answerShown,
                    wiggle = if (i == wrongChoice) wiggleBump else 0,
                    onClick = { onTap(i) },
                ) { if (state.flow == KitchenFlow.COUNT) BlueberryArt(KitchenBerryTileSize) else Text(emoji, fontSize = 40.sp) }
            }
        }
    }
}
