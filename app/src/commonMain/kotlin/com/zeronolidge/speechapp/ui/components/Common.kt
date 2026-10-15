package com.zeronolidge.speechapp.ui.components

import com.zeronolidge.speechapp.ui.art.paperCard
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.audio.VoiceDetector
import com.zeronolidge.speechapp.audio.Parrot
import com.zeronolidge.speechapp.audio.Speaker
import com.zeronolidge.speechapp.data.Praise
import com.zeronolidge.speechapp.state.Progress
import com.zeronolidge.speechapp.ui.theme.Kid
import kotlinx.coroutines.delay

/** Everything a screen needs to talk, listen and award stars. */
class AppServices(
    val speaker: Speaker,
    /** "Did the child say something?" from microphone loudness. */
    val voice: VoiceDetector,
    val parrot: Parrot,
    val progress: Progress,
) {
    /** Bumped every time a star is earned; the celebration overlay watches it. */
    var celebrations by mutableIntStateOf(0)
        private set

    /**
     * True when speech didn't start or finish in time. The app shows a big replay speaker; the
     * story itself has already carried on, so nothing is ever stuck waiting for speech.
     */
    var audioTrouble by mutableStateOf(false)

    init {
        speaker.onTrouble = { audioTrouble = true }
    }

    /** A new paper piece to show off on a pop-up card (from the backpack or a welcome-back present). */
    var gift by mutableStateOf<com.zeronolidge.speechapp.logic.Cosmetic?>(null)
        private set

    fun showGift(piece: com.zeronolidge.speechapp.logic.Cosmetic) {
        gift = piece
    }

    fun closeGift() {
        gift = null
    }

        /** What the big 🔊 "hear it again" button says: the current screen's instruction. */
    var instruction by mutableStateOf("")
        private set

    /** Says an instruction out loud and remembers it for "hear it again". */
    fun instruct(text: String, onDone: (() -> Unit)? = null) {
        instruction = text
        speaker.say(text, onDone = onDone)
    }

    /** Sets what "hear it again" says without saying it now. */
    fun rememberInstruction(text: String) {
        instruction = text
    }

    fun repeatInstruction() {
        if (instruction.isNotBlank()) speaker.say(instruction)
    }

    fun reward(area: String, message: String = Praise.great.random(), onDone: (() -> Unit)? = null) {
        progress.addStar(area)
        celebrate(message, onDone)
    }

    /** Confetti and a cheer, without earning a star. */
    fun celebrate(message: String, onDone: (() -> Unit)? = null) {
        celebrations++
        speaker.say(message, onDone = onDone)
    }

    fun tryAgain(message: String = Praise.tryAgain.random()) = speaker.say(message)
}

val LocalApp = staticCompositionLocalOf<AppServices> { error("AppServices not provided") }

/**
 * A plain screen in the shared frame: 🏠 top-left, 🔊 top-right, a title chip between them.
 * [spoken] is said when the screen opens and is what "hear it again" repeats.
 */
@Composable
fun KidScreen(
    title: String,
    onHome: () -> Unit,
    spoken: String,
    /** Set where back goes somewhere other than the map (e.g. from a game back to the games). */
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    SpeakOnOpen(spoken)
    ScreenFrame(
        onHome = onHome,
        onBack = onBack,
        top = { PaperChip(title, fontSize = 26.sp) },
        content = content,
    )
}

/** A round emoji button on raised paper. */
@Composable
fun RoundIcon(
    emoji: String,
    color: Color,
    size: Dp = 72.dp,
    enabled: Boolean = true,
    onClick: () -> Unit,
) = PaperIconButton(emoji, color, size, enabled, onClick = onClick)

/**
 * A raised answer card. A wrong tap makes it wiggle; [glow] makes the right answer shine after a
 * second miss; [correct] turns it green.
 */
@Composable
fun AnswerCard(
    modifier: Modifier = Modifier,
    faded: Boolean = false,
    correct: Boolean = false,
    glow: Boolean = false,
    /** Each time this goes up, the card gives a little wiggle. */
    wiggle: Int = 0,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val s by pulse.animateFloat(
        1f, 1.08f,
        infiniteRepeatable(tween(400), RepeatMode.Reverse),
        label = "scale",
    )
    val fold by animateFloatAsState(if (faded) 1f else 0f, tween(300), label = "fold")
    val shake = remember { Animatable(0f) }
    LaunchedEffect(wiggle) {
        if (wiggle > 0) {
            repeat(3) {
                shake.animateTo(9f, tween(55))
                shake.animateTo(-9f, tween(55))
            }
            shake.animateTo(0f, tween(55))
        }
    }
    PaperButton(
        when {
            correct -> Kid.good
            glow -> Kid.highlight
            faded -> Color(0xFFDCDCDC)
            else -> PaperColorsUi.choice
        },
        modifier
            .scale(if (correct || glow) s else 1f)
            .graphicsLayer {
                rotationZ = shake.value
                scaleY = 1f - 0.12f * fold
                alpha = 1f - 0.6f * fold
            },
        corner = 24.dp,
        enabled = !faded && !correct,
        onClick = onClick,
    ) { Box(Modifier.padding(10.dp), contentAlignment = Alignment.Center) { content() } }
}

/**
 * How every question handles a wrong tap, the same everywhere:
 *  1st miss: the card wiggles, and "Try again!" is said and shown.
 *  2nd miss: the wrong cards grey out, the right one lights up and is said, then the game moves
 *  on by itself. A wrong answer is never cheered.
 */
class Misses {
    var count by mutableIntStateOf(0)
        private set

    fun add() = ++count

    /** Show the written "Try again!" after the first miss. */
    val tryAgain get() = count == 1

    val showAnswer get() = count >= 2
}

/** Height kept free above the answers for the written "Try again!", so nothing jumps. */
val TryAgainHeight = 46.dp

/** The written "Try again!" after a first miss, in its own reserved slot (never over a card). */
@Composable
fun TryAgainSlot(visible: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.height(TryAgainHeight), contentAlignment = Alignment.Center) {
        if (visible) PaperChip("🙂 Try again!", Modifier.popIn("again"), fontSize = 22.sp, maxLines = 1)
    }
}

/** Set by a wide two-column layout: answers go in a column instead of a row. */
val LocalAnswersStacked = staticCompositionLocalOf { false }

/** What is said when the right answer is shown after the second miss. */
fun revealLine(answerWords: String?) =
    "It's this one" + (answerWords?.let { ": $it" } ?: "") + ". Good trying! Let's keep going."

/**
 * Shows answer options and handles taps the same way everywhere (see [Misses]). Only the right
 * answer is cheered and earns a star. [onSolved] gets `revealed = true` when the answer was shown
 * after two misses, so the story doesn't cheer.
 */
@Composable
fun <T> AnswerRow(
    options: List<T>,
    answer: T,
    area: String,
    roundKey: Any,
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier,
    onSolved: (revealed: Boolean) -> Unit,
    /** Optional words for an option, said back to the child when they tap it. */
    describe: ((T) -> String)? = null,
    /** Called on each wrong tap, so stories can tell whether it was right first time. */
    onMiss: () -> Unit = {},
    /** Each time this goes up, the magic wand makes one wrong answer vanish. */
    wandTick: Int = 0,
    content: @Composable (T) -> Unit,
) {
    val app = LocalApp.current
    val misses = remember(roundKey) { Misses() }
    var faded by remember(roundKey) { mutableStateOf(setOf<T>()) }
    var wiggles by remember(roundKey) { mutableStateOf(mapOf<T, Int>()) }
    var solved by remember(roundKey) { mutableStateOf(false) }
    var revealed by remember(roundKey) { mutableStateOf(false) }
    var advanced by remember(roundKey) { mutableStateOf(false) }
    val advance = {
        if (!advanced) {
            advanced = true
            onSolved(revealed)
        }
    }
    LaunchedEffect(roundKey, wandTick) {
        if (wandTick > 0 && !solved) {
            val wrong = options.filter { it != answer && it !in faded }
            if (wrong.isNotEmpty()) faded = faded + wrong.random()
        }
    }
    // Normally the next round starts when the voice finishes; this is a backstop.
    if (solved) AfterDelay(roundKey, 6000) { advance() }
    val answerWords = describe?.invoke(answer)
    val cards: @Composable () -> Unit = {
        options.forEach { opt ->
            AnswerCard(
                modifier = cardModifier,
                faded = opt in faded,
                correct = solved && !revealed && opt == answer,
                glow = revealed && opt == answer,
                wiggle = wiggles[opt] ?: 0,
                onClick = {
                    // A tap is checked against the right answer before anything is cheered.
                    if (!solved && opt !in faded) {
                        val said = describe?.invoke(opt)?.let { "$it! " } ?: ""
                        if (opt == answer) {
                            solved = true
                            app.reward(area, said + Praise.great.random(), onDone = advance)
                        } else {
                            wiggles = wiggles + (opt to (wiggles[opt] ?: 0) + 1)
                            onMiss()
                            if (misses.add() == 1) {
                                app.speaker.say(said + "Try again!")
                            } else {
                                // Second miss: grey out the wrong ones, light up the right one, move on.
                                solved = true
                                revealed = true
                                faded = options.filter { it != answer }.toSet()
                                app.speaker.say(revealLine(answerWords), onDone = advance)
                            }
                        }
                    }
                },
            ) { content(opt) }
        }
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        TryAgainSlot(misses.tryAgain && !solved)
        if (LocalAnswersStacked.current) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally) { cards() }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) { cards() }
        }
    }
}

@Composable
fun Emoji(text: String, size: TextUnit, modifier: Modifier = Modifier) {
    Text(text, fontSize = size, modifier = modifier, textAlign = TextAlign.Center)
}

/** Falling stars and confetti whenever [AppServices.celebrations] changes. */
@Composable
fun CelebrationOverlay() {
    val app = LocalApp.current
    val key = app.celebrations
    if (key == 0) return
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        progress.animateTo(1f, tween(1600, easing = LinearEasing))
    }
    if (progress.value >= 1f) return
    val pieces = remember(key) {
        val r = kotlin.random.Random(key)
        List(16) {
            Triple(r.nextFloat(), 0.6f + r.nextFloat() * 0.8f, listOf("⭐", "🌟", "🎉", "✨", "🎈").random(r))
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        pieces.forEach { (x, speed, emoji) ->
            val y = (progress.value * speed * 1.4f - 0.2f)
            Text(
                emoji,
                fontSize = 40.sp,
                modifier = Modifier.offset(x = w * x - 20.dp, y = h * y),
            )
        }
    }
}

/** Waits, then runs [action]; handy for moving to the next round after praise. */
@Composable
fun AfterDelay(key: Any, ms: Long, action: () -> Unit) {
    LaunchedEffect(key) {
        delay(ms)
        action()
    }
}
