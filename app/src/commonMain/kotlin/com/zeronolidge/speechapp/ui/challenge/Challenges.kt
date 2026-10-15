package com.zeronolidge.speechapp.ui.challenge

import com.zeronolidge.speechapp.ui.art.paperCard
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.data.LetterContent
import com.zeronolidge.speechapp.data.Praise
import com.zeronolidge.speechapp.data.Things
import com.zeronolidge.speechapp.logic.Cell
import com.zeronolidge.speechapp.logic.Challenge
import com.zeronolidge.speechapp.logic.Cmd
import com.zeronolidge.speechapp.logic.Dir
import com.zeronolidge.speechapp.logic.LetterMode
import com.zeronolidge.speechapp.logic.Numbers
import com.zeronolidge.speechapp.logic.Reading
import com.zeronolidge.speechapp.logic.TileLook
import com.zeronolidge.speechapp.logic.AnswerKey
import com.zeronolidge.speechapp.logic.QuizState
import com.zeronolidge.speechapp.logic.TapResult
import com.zeronolidge.speechapp.logic.tileLook
import com.zeronolidge.speechapp.logic.RobotLevel
import com.zeronolidge.speechapp.logic.RobotSim
import com.zeronolidge.speechapp.logic.RobotState
import com.zeronolidge.speechapp.logic.StepResult
import com.zeronolidge.speechapp.logic.Subject
import com.zeronolidge.speechapp.state.Progress
import com.zeronolidge.speechapp.ui.components.AnswerRow
import com.zeronolidge.speechapp.ui.components.ArrowIcon
import com.zeronolidge.speechapp.ui.components.PaperArrowButton
import com.zeronolidge.speechapp.ui.art.LocalReduceMotion
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.foundation.layout.heightIn
import com.zeronolidge.speechapp.ui.components.LocalAnswersStacked
import com.zeronolidge.speechapp.ui.components.TryAgainHeight
import com.zeronolidge.speechapp.ui.components.TryAgainSlot
import com.zeronolidge.speechapp.ui.components.countAloud
import com.zeronolidge.speechapp.ui.components.PaperButton
import com.zeronolidge.speechapp.ui.components.PaperColorsUi
import com.zeronolidge.speechapp.ui.components.PaperChip
import com.zeronolidge.speechapp.ui.components.popIn
import com.zeronolidge.speechapp.logic.CoinMode
import com.zeronolidge.speechapp.ui.art.paperCircle
import com.zeronolidge.speechapp.ui.art.star
import com.zeronolidge.speechapp.ui.art.paperPath
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.RoundIcon
import com.zeronolidge.speechapp.ui.screens.SpeakMoment
import com.zeronolidge.speechapp.ui.theme.Kid
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** What the challenge asks, out loud. The owl power says it again, slowly. */
fun promptFor(c: Challenge): String = when (c) {
    is Challenge.Say -> "Say ${c.word.text}."
    is Challenge.Count -> "How many ${Things.nameOf(c.round.emoji)}? Tap each one to count."
    is Challenge.Math -> {
        val r = c.round
        val name = Things.nameOf(r.emoji)
        if (r.plus) "${Numbers.word(r.a)} $name, and ${Numbers.word(r.b)} more. How many altogether?"
        else "${Numbers.word(r.a)} $name. Take away ${Numbers.word(r.b)}. How many are left?"
    }
    is Challenge.More -> "Which side has more ${Things.nameOf(c.round.emoji)}?"
    is Challenge.Letter -> {
        val t = c.round.target
        when (c.round.mode) {
            LetterMode.BIG_TO_SMALL -> "This is big ${t.upper}. Can you find little ${t.lower}?"
            LetterMode.SMALL_TO_BIG -> "This is little ${t.lower}. Can you find big ${t.upper}?"
            LetterMode.FIRST_SOUND -> "${t.word.text}. ${t.sound}, ${t.sound}, ${t.word.text}. Which letter does ${t.word.text} start with?"
        }
    }
    is Challenge.SoundOut -> "Tap each letter to hear its sound. Then tap the turtle to blend them."
    is Challenge.WhichWord -> "Find the word ${c.round.word.text}."
    is Challenge.Pattern -> "What comes next?"
    is Challenge.OddOne -> "Which one is different?"
    is Challenge.Robot -> "Tap the arrows to make a plan, then press Go!"
    is Challenge.Coins -> when (c.round.mode) {
        CoinMode.PAY -> "The ${Things.nameOf(c.round.item)} cost ${Numbers.word(c.round.price)} ${if (c.round.price == 1) "coin" else "coins"}. Tap the coins to put them in the jar!"
        CoinMode.PURSE -> "Which purse has ${Numbers.word(c.round.price)} ${if (c.round.price == 1) "coin" else "coins"}?"
    }
}

fun areaOf(c: Challenge): String = when (c.subject) {
    Subject.SPEECH -> Progress.AREA_SPEECH
    Subject.LETTERS -> Progress.AREA_LETTERS
    Subject.READING -> Progress.AREA_READING
    Subject.NUMBERS -> Progress.AREA_NUMBERS
    Subject.PATTERNS -> Progress.AREA_PATTERNS
    Subject.ROBOT -> Progress.AREA_ROBOT
    Subject.COINS -> Progress.AREA_COINS
}

/** Helper powers poke challenges through these counters. */
class PowerTicks {
    var wand by mutableIntStateOf(0)
    var map by mutableIntStateOf(0)
}

/**
 * Shows one challenge. [onDone] gets `firstTry` (solved without any wrong answers) and
 * `revealed` (the answer was shown after two misses, so nothing should cheer).
 * The prompt is queued after whatever is being said, so story narration comes first.
 */
@Composable
fun ChallengeView(
    challenge: Challenge,
    modifier: Modifier = Modifier,
    /** Story narration said just before the challenge's own question. */
    intro: String? = null,
    powers: PowerTicks = remember(challenge) { PowerTicks() },
    onDone: (firstTry: Boolean, revealed: Boolean) -> Unit,
) {
    val app = LocalApp.current
    var misses by remember(challenge) { mutableIntStateOf(0) }
    LaunchedEffect(challenge) {
        // The speaking challenge says its own intro, then turns the microphone on.
        if (challenge !is Challenge.Say) {
            app.rememberInstruction(listOfNotNull(intro, promptFor(challenge)).joinToString(" "))
            if (intro != null) app.speaker.say(intro)
            app.speaker.then(promptFor(challenge))
        }
    }
    val miss: () -> Unit = { misses++ }
    val done: (Boolean) -> Unit = { revealed -> onDone(misses == 0 && !revealed, revealed) }
    val area = areaOf(challenge)
    // A fresh start for every puzzle: no "Try again!", Rory hint, glow or faded cards carry over.
    Box(modifier) { key(challenge) {
        when (challenge) {
            is Challenge.Say -> SpeakMoment(
                challenge.word, challenge.sound, Modifier.fillMaxSize(), intro = intro,
                afterLine = challenge.doing?.let { "Now we can $it!" },
            ) { onDone(true, false) }
            is Challenge.Count -> CountView(challenge, area, powers.wand, miss, done)
            is Challenge.Math -> MathView(challenge, area, powers.wand, miss, done)
            is Challenge.More -> MoreView(challenge, area, powers.wand, miss, done)
            is Challenge.Letter -> LetterView(challenge, area, powers.wand, miss, done)
            is Challenge.SoundOut -> SoundOutView(challenge, area, powers.wand, miss, done)
            is Challenge.WhichWord -> WhichWordView(challenge, area, powers.wand, miss, done)
            is Challenge.Pattern -> PatternView(challenge, area, powers.wand, miss, done)
            is Challenge.OddOne -> OddOneView(challenge, area, powers.wand, miss, done)
            is Challenge.Robot -> RobotView(challenge, area, powers.map, miss, done)
            is Challenge.Coins -> CoinsView(challenge, area, miss, done)
        }
    } }
}

/**
 * Lays out a question area and the answer cards, scaled to fit: the picture above a row of
 * answers, or on a wide screen (over 700) the picture on the left and the answers in a column on
 * the right.
 */
@Composable
private fun QuestionLayout(
    /** Off for answers that only work as a row (the market's coins and jar). */
    allowSide: Boolean = true,
    question: @Composable (cardSize: Dp) -> Unit,
    answers: @Composable (cardSize: Dp) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        if (allowSide && w > 700.dp) {
            // Three cards stacked, plus the "Try again!" slot, must fit the height.
            val card = minOf(w / 6f, (h - TryAgainHeight - 60.dp) / 3.2f).coerceIn(64.dp, 112.dp)
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                // The picture side has the room, so its pictures are drawn bigger than the cards.
                val big = minOf(card * 1.6f, w * 0.5f / 3f, h / 3f).coerceAtLeast(card)
                Box(Modifier.weight(1.3f).fillMaxHeight(), contentAlignment = Alignment.Center) { question(big) }
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    CompositionLocalProvider(LocalAnswersStacked provides true) { answers(card) }
                }
            }
        } else {
            // Same physical size on every screen: never smaller than a thumb, never blown up on a tablet.
            val card = minOf(w / 4.4f, h / 2.6f).coerceIn(64.dp, 112.dp)
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { question(card) }
                answers(card)
            }
        }
    }
}

@Composable
private fun NumberText(n: Int, card: Dp) {
    Text("$n", fontSize = (card.value * 0.5f).sp, fontWeight = FontWeight.ExtraBold, color = Kid.ink)
}

/** A neat group of emoji. Tapping one counts it out loud, if [onTapItem] is set. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmojiGroup(
    emoji: String,
    count: Int,
    modifier: Modifier = Modifier,
    size: Int = 56,
    crossedOut: Int = 0,
    counted: List<Int> = emptyList(),
    onTapItem: ((Int) -> Unit)? = null,
) {
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(if (onTapItem != null) 12.dp else 4.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(if (onTapItem != null) 14.dp else 4.dp),
    ) {
        repeat(count) { i ->
            val crossed = i >= count - crossedOut
            val countIndex = counted.indexOf(i)
            val inner: @Composable () -> Unit = {
                Text(emoji, fontSize = size.sp, modifier = Modifier.alpha(if (crossed) 0.3f else 1f))
                if (crossed) Text("❌", fontSize = (size * 0.7f).sp)
            }
            if (onTapItem != null) {
                // Things to count are raised paper, so it's clear they can be tapped.
                Box {
                    PaperButton(
                        if (countIndex >= 0) Kid.highlight else PaperColorsUi.choice,
                        Modifier.sizeIn(minWidth = 64.dp, minHeight = 64.dp),
                        circle = true,
                        onClick = { onTapItem(i) },
                    ) {
                        Box(Modifier.padding(8.dp), contentAlignment = Alignment.Center) { inner() }
                    }
                    if (countIndex >= 0) {
                        Text(
                            "${countIndex + 1}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFD84315),
                            modifier = Modifier.align(Alignment.TopEnd),
                        )
                    }
                }
            } else {
                Box(Modifier.padding(3.dp), contentAlignment = Alignment.Center) { inner() }
            }
        }
    }
}

@Composable
private fun CountView(c: Challenge.Count, area: String, wand: Int, miss: () -> Unit, done: (revealed: Boolean) -> Unit) {
    val app = LocalApp.current
    var counted by remember(c) { mutableStateOf(listOf<Int>()) }
    QuestionLayout(
        question = { card ->
            EmojiGroup(
                c.round.emoji, c.round.count, Modifier.fillMaxWidth(0.9f),
                size = (card.value * 0.38f).toInt(),
                counted = counted,
                onTapItem = { i ->
                    if (i !in counted) {
                        counted = counted + i
                        app.speaker.say(Numbers.word(counted.size))
                    }
                },
            )
        },
        answers = { card ->
            AnswerRow(
                AnswerKey.count(c), area, c,
                Modifier.fillMaxWidth(), Modifier.size(card),
                onSolved = done, describe = { Numbers.word(it) }, onMiss = miss, wandTick = wand,
                hint = "Count with me: ${countAloud(c.round.count)}!",
            ) { NumberText(it, card) }
        },
    )
}

@Composable
private fun MathView(c: Challenge.Math, area: String, wand: Int, miss: () -> Unit, done: (revealed: Boolean) -> Unit) {
    val r = c.round
    QuestionLayout(
        question = { card ->
            val s = (card.value * 0.4f).toInt()
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (r.plus) "${r.a} + ${r.b} = ?" else "${r.a} − ${r.b} = ?",
                    fontSize = (card.value * 0.38f).coerceAtLeast(30f).sp, fontWeight = FontWeight.ExtraBold, color = Kid.ink,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (r.plus) {
                        EmojiGroup(r.emoji, r.a, Modifier.weight(1f), size = s)
                        Text("➕", fontSize = 32.sp, modifier = Modifier.padding(horizontal = 8.dp))
                        EmojiGroup(r.emoji, r.b, Modifier.weight(1f), size = s)
                    } else {
                        EmojiGroup(r.emoji, r.a, Modifier.fillMaxWidth(0.9f), size = s, crossedOut = r.b)
                    }
                }
            }
        },
        answers = { card ->
            AnswerRow(
                AnswerKey.math(c), area, c,
                Modifier.fillMaxWidth(), Modifier.size(card),
                onSolved = done, describe = { Numbers.word(it) }, onMiss = miss, wandTick = wand,
                hint = when {
                    r.plus -> "Count them all with me: ${countAloud(r.answer)}!"
                    r.answer == 0 -> "Take them all away. None are left!"
                    else -> "Count the ones that are left: ${countAloud(r.answer)}!"
                },
            ) { NumberText(it, card) }
        },
    )
}

@Composable
private fun MoreView(c: Challenge.More, area: String, wand: Int, miss: () -> Unit, done: (revealed: Boolean) -> Unit) {
    val r = c.round
    val app = LocalApp.current
    val key = AnswerKey.more(c)
    // The round's state and every tap go through the one tested rule (QuizState in logic/Quiz.kt).
    var quiz by remember(c) { mutableStateOf(QuizState()) }
    var wanded by remember(c) { mutableStateOf(setOf<Boolean>()) }
    var wiggleLeft by remember(c) { mutableIntStateOf(0) }
    var wiggleRight by remember(c) { mutableIntStateOf(0) }
    val shake = remember(c) { Animatable(0f) }
    val still = LocalReduceMotion.current
    LaunchedEffect(wiggleLeft, wiggleRight) {
        if (wiggleLeft + wiggleRight > 0 && !still) {
            repeat(3) {
                shake.animateTo(8f, tween(55))
                shake.animateTo(-8f, tween(55))
            }
            shake.animateTo(0f, tween(55))
        }
    }
    LaunchedEffect(c, wand) {
        if (wand > 0 && !quiz.solved) wanded = setOf(!key.answer)
    }
    val moreLine = "${Numbers.word(maxOf(r.left, r.right))} is more than ${Numbers.word(minOf(r.left, r.right))}."
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        TryAgainSlot(if (quiz.solved) 0 else quiz.misses.coerceAtMost(2))
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(true to r.left, false to r.right).forEach { (isLeft, n) ->
                val right = isLeft == key.answer
                val wiggling = (isLeft && wiggleLeft > 0) || (!isLeft && wiggleRight > 0)
                // The shared rule (logic/Quiz.kt): only the side with more can turn green or glow.
                val look = quiz.look(right, wanded = isLeft in wanded)
                val grey = look == TileLook.GREY
                PaperButton(
                    when (look) {
                        TileLook.RIGHT -> Kid.good
                        TileLook.GLOW -> Kid.highlight
                        TileLook.GREY -> Color(0xFFDCDCDC)
                        TileLook.PLAIN -> PaperColorsUi.choice
                    },
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(bottom = 10.dp)
                        .alpha(if (grey) 0.45f else 1f)
                        .rotate(if (wiggling && !right) shake.value else 0f),
                    enabled = !quiz.solved && !grey,
                    onClick = {
                        val (next, result) = quiz.tap(right)
                        quiz = next
                        when (result) {
                            TapResult.RIGHT -> app.reward(area, "${Praise.great.random()} $moreLine", onDone = { done(false) })
                            TapResult.TRY_AGAIN -> {
                                if (isLeft) wiggleLeft++ else wiggleRight++
                                miss()
                                app.speaker.say("Try again!")
                            }
                            // Second miss: Rory counts each side out loud while the right side glows.
                            TapResult.HINT -> {
                                if (isLeft) wiggleLeft++ else wiggleRight++
                                miss()
                                app.speaker.say(
                                    "Count with me! This side: ${countAloud(r.left)}. That side: ${countAloud(r.right)}. Which side has more? Tap the shining one!",
                                )
                            }
                            // Third miss: this side greys out, the other is shown, move on.
                            TapResult.SHOWN -> {
                                miss()
                                app.speaker.say("This side has more. $moreLine Good trying! Let's keep going.", onDone = { done(true) })
                            }
                            TapResult.IGNORED -> {}
                        }
                    },
                ) {
                    EmojiGroup(r.emoji, n, Modifier.padding(10.dp), size = 40)
                    if (quiz.solved) {
                        Text("$n", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Kid.ink, modifier = Modifier.align(Alignment.TopEnd))
                    }
                }
            }
        }
    }
}

@Composable
private fun LetterView(c: Challenge.Letter, area: String, wand: Int, miss: () -> Unit, done: (revealed: Boolean) -> Unit) {
    val app = LocalApp.current
    val t = c.round.target
    val mode = c.round.mode
    QuestionLayout(
        question = { card ->
            Text(
                when (mode) {
                    LetterMode.BIG_TO_SMALL -> "${t.upper}"
                    LetterMode.SMALL_TO_BIG -> "${t.lower}"
                    LetterMode.FIRST_SOUND -> t.word.emoji
                },
                fontSize = (card.value * 0.8f).sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E88E5),
                modifier = Modifier.clickable { app.speaker.say(promptFor(c)) },
            )
        },
        answers = { card ->
            AnswerRow(
                AnswerKey.letter(c), area, c,
                Modifier.fillMaxWidth(), Modifier.size(card),
                onSolved = done,
                describe = { l -> if (mode == LetterMode.FIRST_SOUND) "${l.upper}, ${l.sound}" else "${l.upper}" },
                onMiss = miss, wandTick = wand,
                hint = "Listen: ${t.sound}, ${t.sound}, ${t.word.text}! ${t.upper} says ${t.sound}.",
            ) { l ->
                Text(
                    when (mode) {
                        LetterMode.BIG_TO_SMALL -> "${l.lower}"
                        LetterMode.SMALL_TO_BIG -> "${l.upper}"
                        LetterMode.FIRST_SOUND -> "${l.upper}${l.lower}"
                    },
                    fontSize = (card.value * 0.5f).sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Kid.ink,
                )
            }
        },
    )
}

@Composable
private fun SoundOutView(c: Challenge.SoundOut, area: String, wand: Int, miss: () -> Unit, done: (revealed: Boolean) -> Unit) {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    val word = c.round.word.text
    var lit by remember(c) { mutableIntStateOf(-1) }
    var tapped by remember(c) { mutableStateOf(setOf<Int>()) }
    var blended by remember(c) { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    DisposableEffect(c) { onDispose { job?.cancel() } }

    fun blend() {
        val sounds = Reading.sounds(word)
        app.speaker.say(sounds.joinToString("... "), slow = true)
        app.speaker.then("$word!")
        job?.cancel()
        job = scope.launch {
            sounds.indices.forEach { i ->
                lit = i
                delay(900)
            }
            lit = -1
            blended = true
            delay(500)
            app.speaker.then("Which picture is $word?")
        }
    }

    QuestionLayout(
        question = { card ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                word.forEachIndexed { i, ch ->
                    val letter = LetterContent.of(ch)
                    PaperButton(
                        when {
                            lit == i -> Kid.highlight
                            i in tapped -> Color(0xFFE1F5FE)
                            else -> PaperColorsUi.choice
                        },
                        Modifier.size((card * 0.85f).coerceAtLeast(64.dp)),
                        corner = 20.dp,
                        onClick = {
                            tapped = tapped + i
                            lit = i
                            app.speaker.say(letter.sound)
                        },
                    ) {
                        Text("$ch", fontSize = (card.value * 0.55f).sp, fontWeight = FontWeight.ExtraBold, color = Kid.ink)
                    }
                }
                Box(Modifier.width(4.dp))
                RoundIcon("🐢", if (tapped.size == word.length) Kid.highlight else Kid.soft, size = (card * 0.75f).coerceAtLeast(64.dp)) { blend() }
            }
        },
        answers = { card ->
            Box(Modifier.heightIn(min = card + 8.dp + TryAgainHeight), contentAlignment = Alignment.Center) {
                if (blended) {
                    AnswerRow(
                        AnswerKey.soundOut(c), area, c,
                        cardModifier = Modifier.size(card),
                        onSolved = done, describe = { it.text }, onMiss = miss, wandTick = wand,
                        hint = "Listen: ${Reading.sounds(word).joinToString("... ")}... $word!",
                    ) { w -> Text(w.emoji, fontSize = (card.value * 0.55f).sp) }
                } else {
                    Text("Tap 🐢 to blend!", fontSize = 24.sp, color = Kid.ink.copy(alpha = 0.6f))
                }
            }
        },
    )
}

@Composable
private fun WhichWordView(c: Challenge.WhichWord, area: String, wand: Int, miss: () -> Unit, done: (revealed: Boolean) -> Unit) {
    val app = LocalApp.current
    QuestionLayout(
        question = { card ->
            Text(c.round.word.emoji, fontSize = (card.value * 0.75f).sp, modifier = Modifier.clickable { app.speaker.say(promptFor(c)) })
        },
        answers = { card ->
            AnswerRow(
                AnswerKey.whichWord(c), area, c,
                Modifier.fillMaxWidth(), Modifier.width(card * 1.3f).height(card * 0.8f),
                onSolved = done, describe = { it.text }, onMiss = miss, wandTick = wand,
                hint = c.round.word.text.let { w -> "Listen: ${Reading.sounds(w).joinToString("... ")}... $w! It starts with ${Reading.sounds(w).firstOrNull() ?: w}." },
            ) { w -> Text(w.text, fontSize = (card.value * 0.38f).sp, fontWeight = FontWeight.ExtraBold, color = Kid.ink) }
        },
    )
}

@Composable
private fun PatternView(c: Challenge.Pattern, area: String, wand: Int, miss: () -> Unit, done: (revealed: Boolean) -> Unit) {
    val r = c.round
    QuestionLayout(
        question = { _ ->
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val slot = minOf(maxWidth / (r.shown.size + 1.5f), 64.dp)
                Row(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    r.shown.forEach { Text(it, fontSize = (slot.value * 0.72f).sp) }
                    Box(
                        Modifier
                            .size(slot)
                            .border(3.dp, Color(0xFF7C4DFF), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Text("❓", fontSize = (slot.value * 0.5f).sp) }
                }
            }
        },
        answers = { card ->
            AnswerRow(
                AnswerKey.pattern(c), area, c,
                Modifier.fillMaxWidth(), Modifier.size(card),
                onSolved = done, onMiss = miss, wandTick = wand,
                hint = r.shown.map { Things.nameOf(it) }.takeIf { names -> "things" !in names }
                    ?.let { names -> "Say it with me: ${names.joinToString(", ")}... What comes next?" }
                    ?: "Look at the pattern again. It goes round and round. What comes next?",
            ) { Text(it, fontSize = (card.value * 0.5f).sp) }
        },
    )
}

@Composable
private fun OddOneView(c: Challenge.OddOne, area: String, wand: Int, miss: () -> Unit, done: (revealed: Boolean) -> Unit) {
    val r = c.round
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val card = minOf(maxWidth / 5.2f, (maxHeight - TryAgainHeight) / 1.6f).coerceIn(64.dp, 112.dp)
        // Options are positions, so identical pictures are still separate cards.
        AnswerRow(
            AnswerKey.oddOne(c), area, c,
            Modifier.fillMaxWidth(), Modifier.size(card),
            onSolved = done, onMiss = miss, wandTick = wand,
            hint = if (r.items.distinct().size == 2) "Three are the same. Find the one that looks different!" else "Three of them go together. Which one doesn't belong?",
        ) { i -> Text(r.items[i], fontSize = (card.value * 0.55f).sp) }
    }
}

// ---------------- Robot ----------------

private const val MAX_PROGRAM = 10

/**
 * Plan a path, then press Go and watch the robot follow it, one step at a time,
 * just like a Cubetto.
 */
@Composable
private fun RobotView(c: Challenge.Robot, area: String, mapTick: Int, miss: () -> Unit, done: (revealed: Boolean) -> Unit) {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    val level = c.level
    val program = remember(c) { mutableStateListOf<Cmd>() }
    var robot by remember(c) { mutableStateOf(RobotState(level.start, level.startFacing)) }
    var runningStep by remember(c) { mutableIntStateOf(-1) }
    var bumpAt by remember(c) { mutableStateOf<Cell?>(null) }
    var won by remember(c) { mutableStateOf(false) }
    var hint by remember(c) { mutableStateOf<Cmd?>(null) }
    var failures by remember(c) { mutableIntStateOf(0) }
    var autoGo by remember(c) { mutableStateOf(false) }
    // The plan was shown after two failed runs: the robot still goes, but nobody cheers.
    var shownWay by remember(c) { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    val running = runningStep >= 0
    val wobble = remember { Animatable(0f) }
    val palette = RobotSim.palette(level)

    LaunchedEffect(c) {
        if (level.relative) {
            app.speaker.then("Forward goes the way the robot is looking. The turn buttons spin the robot around.")
        }
    }
    DisposableEffect(c) { onDispose { job?.cancel() } }

    // The treasure map shows the next good move from wherever the plan ends.
    LaunchedEffect(c, mapTick) {
        if (mapTick == 0) return@LaunchedEffect
        val end = RobotSim.stateAfter(level, program)
        if (end == null) {
            app.speaker.say("The robot will bump! Tap undo to take away the last arrow.")
        } else {
            val next = RobotSim.solve(level, end)?.firstOrNull()
            hint = next
            app.speaker.say(if (next == null) "The plan is ready! Press Go!" else "Try ${next.spoken}!")
        }
    }

    fun reset() {
        robot = RobotState(level.start, level.startFacing)
        runningStep = -1
        bumpAt = null
    }

    /**
     * The same gentle pattern as every other question: after the 1st failed run "try again"
     * (said and shown), after the 2nd Rory says the first good move while it glows, after the
     * 3rd the robot is shown the way and the story moves on.
     */
    fun afterFailure() {
        reset()
        if (failures == 1) {
            app.speaker.say("Try again! Let's fix the plan.")
        } else if (failures == 2) {
            program.clear()
            hint = RobotSim.solve(level)?.firstOrNull()
            app.speaker.say(hint?.let { "Here's a clue: start with ${it.spoken}! It's shining." } ?: "Let's try again!")
        } else {
            // Show the way: fill in a working plan and let the robot go.
            program.clear()
            RobotSim.solve(level)?.let { program.addAll(it.take(MAX_PROGRAM)) }
            shownWay = true
            app.speaker.say("Here's a plan that works. Watch the robot go!")
            autoGo = true
        }
    }

    fun go() {
        if (program.isEmpty()) {
            app.speaker.say("Tap the arrows to make a plan first!")
            return
        }
        hint = null
        job?.cancel()
        reset()
        job = scope.launch {
            var state = RobotState(level.start, level.startFacing)
            for ((i, cmd) in program.toList().withIndex()) {
                runningStep = i
                app.speaker.say(cmd.spoken)
                delay(450)
                when (val r = RobotSim.step(level, state, cmd)) {
                    is StepResult.Moved -> state = r.state
                    is StepResult.Turned -> state = r.state
                    is StepResult.Bumped -> {
                        robot = r.state
                        bumpAt = r.into
                        miss()
                        failures++
                        app.speaker.say(if (r.into in level.blocks) "Bump! A tree is in the way." else "Bump! That's the edge.")
                        repeat(3) {
                            wobble.animateTo(8f, tween(60))
                            wobble.animateTo(-8f, tween(60))
                        }
                        wobble.animateTo(0f, tween(60))
                        delay(1500)
                        afterFailure()
                        return@launch
                    }
                }
                robot = state
                delay(550)
            }
            runningStep = -1
            if (state.pos == level.goal) {
                won = true
                if (shownWay) {
                    app.speaker.say("The robot made it. Good trying! Let's keep going.", onDone = { done(true) })
                } else {
                    app.reward(area, "Hooray! The robot made it! You did it!", onDone = { done(false) })
                }
            } else {
                miss()
                failures++
                app.speaker.say("Hmm, the robot didn't get there.")
                delay(2200)
                afterFailure()
            }
        }
    }

    LaunchedEffect(autoGo) {
        if (autoGo) {
            delay(2500)
            autoGo = false
            go()
        }
    }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            val cell: Dp = minOf(maxWidth / level.cols, maxHeight / level.rows)
            Board(level, robot, cell, bumpAt, won, c.goalEmoji, Modifier.offset(x = wobble.value.dp))
        }
        Column(
            Modifier.weight(1.1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TryAgainSlot(if (running || won || shownWay) 0 else failures.coerceAtMost(2), Modifier.fillMaxWidth())
            ProgramStrip(program, runningStep, Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                palette.forEach { cmd ->
                    PaperButton(
                        if (cmd == hint) Kid.highlight else PaperColorsUi.choice,
                        Modifier.weight(1f).height(76.dp),
                        corner = 16.dp,
                        enabled = !running && !won,
                        onClick = {
                            hint = null
                            if (program.size < MAX_PROGRAM) {
                                program.add(cmd)
                                app.speaker.say(cmd.spoken)
                            } else {
                                app.speaker.say("The plan is full! Press Go.")
                            }
                        },
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(cmd.symbol, fontSize = 30.sp)
                            Text(cmd.spoken, fontSize = 12.sp, color = Kid.ink)
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIcon("↩️", Kid.soft, size = 72.dp, enabled = !running && !won && program.isNotEmpty()) {
                    program.removeAt(program.lastIndex)
                    app.speaker.say("Undo.")
                }
                PaperArrowButton(Kid.good, size = 84.dp, arrowColor = Color.White, enabled = !running && !won) { go() }
            }
        }
    }
}

@Composable
private fun Board(level: RobotLevel, robot: RobotState, cell: Dp, bumpAt: Cell?, won: Boolean, goal: String, modifier: Modifier) {
    Column(
        modifier
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp)),
    ) {
        for (r in 0 until level.rows) {
            Row {
                for (col in 0 until level.cols) {
                    val here = Cell(r, col)
                    Box(
                        Modifier
                            .size(cell)
                            .background(if ((r + col) % 2 == 0) Kid.grass else Kid.grassDark)
                            .then(if (here == bumpAt) Modifier.border(3.dp, Color(0xFFE84A7F)) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        val big = (cell.value * 0.55f).sp
                        when {
                            here == robot.pos -> RobotFace(robot.facing, cell, if (won) "🥳" else "🤖")
                            here == level.goal -> Text(goal, fontSize = big)
                            here in level.blocks -> Text("🌳", fontSize = big)
                        }
                    }
                }
            }
        }
    }
}

/** The robot, with a little arrow showing which way it is looking. */
@Composable
private fun RobotFace(facing: Dir, cell: Dp, face: String) {
    Box(Modifier.size(cell), contentAlignment = Alignment.Center) {
        Text(face, fontSize = (cell.value * 0.5f).sp)
        val align = when (facing) {
            Dir.UP -> Alignment.TopCenter
            Dir.RIGHT -> Alignment.CenterEnd
            Dir.DOWN -> Alignment.BottomCenter
            Dir.LEFT -> Alignment.CenterStart
        }
        // A little paper arrowhead (drawn, so it shows on every device).
        Canvas(Modifier.align(align).padding(2.dp).size(cell * 0.22f).rotate(facing.degrees)) {
            val tri = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width / 2, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            paperPath(tri, Color(0xFFD84315))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgramStrip(program: List<Cmd>, runningStep: Int, modifier: Modifier) {
    FlowRow(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(MAX_PROGRAM) { i ->
            val cmd = program.getOrNull(i)
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            i == runningStep -> Kid.highlight
                            cmd != null -> Color(0xFFE8F5E9)
                            else -> Color(0xFFF5F5F5)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (cmd != null) Text(cmd.symbol, fontSize = 24.sp)
            }
        }
    }
}

// ---------------- Market coins ----------------

/** A gold paper coin, drawn rather than an emoji so it shows on every device. */
@Composable
fun Coin(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2
        paperCircle(Color(0xFFF9A825), r, center)
        paperCircle(Color(0xFFFFD54F), r * 0.78f, center)
        star(center, r * 0.42f, Color(0xFFF9A825))
    }
}

/** A row of coins for showing an amount. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoinPile(n: Int, coin: Dp, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(n) { Coin(coin) }
    }
}

/**
 * The market stall. PAY: tap coins on the table to drop them into the stall keeper's jar; each
 * one is counted out loud, and when there are enough the goods are yours. PURSE: pick the purse
 * that holds just the right number of coins.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoinsView(c: Challenge.Coins, area: String, miss: () -> Unit, done: (revealed: Boolean) -> Unit) {
    val app = LocalApp.current
    val r = c.round
    val name = Things.nameOf(r.item)
    QuestionLayout(
        question = { card ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(r.item, fontSize = (card.value * 0.6f).sp)
                Row(
                    Modifier.paperCard(Color(0xFFFFFAF0), 999.dp).padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PaperChip("${r.price}", fontSize = 34.sp, color = Color.Transparent)
                    Coin(44.dp)
                }
            }
        },
        answers = { card ->
            when (r.mode) {
                CoinMode.PURSE -> AnswerRow(
                    AnswerKey.purse(c)!!, area, c,
                    Modifier.fillMaxWidth(), Modifier.size(card * 1.2f, card),
                    onSolved = done,
                    describe = { "${Numbers.word(it)} ${if (it == 1) "coin" else "coins"}" },
                    onMiss = miss,
                    hint = "We need ${Numbers.word(r.price)}. Count the coins with me: ${countAloud(r.price)}!",
                ) { n -> CoinPile(n, card * 0.22f) }
                CoinMode.PAY -> {
                    var paid by remember(c) { mutableIntStateOf(0) }
                    val stacked = LocalAnswersStacked.current
                    val coinSize = (card * 0.62f).coerceAtLeast(64.dp)
                    // Coins on the table: each one is a raised paper button.
                    val coins = @Composable {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            repeat(r.onTable - paid) {
                                PaperButton(Color(0xFFFFE082), Modifier.size(coinSize), circle = true, onClick = {
                                    if (paid < r.price) {
                                        paid++
                                        if (paid == r.price) {
                                            app.reward(area, "${Numbers.word(paid)}! That's just right. Here are your $name!", onDone = { done(false) })
                                        } else {
                                            app.speaker.say(Numbers.word(paid))
                                        }
                                    }
                                }) { Coin(card * 0.46f) }
                            }
                        }
                    }
                    // The stall keeper's jar fills up.
                    val jar = @Composable {
                        Box(
                            Modifier.size(card * 1.3f, card).paperCard(Color(0xFFB3E5FC), 22.dp).padding(8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CoinPile(paid, card * 0.24f)
                            if (paid == 0) Text("🧺", fontSize = (card.value * 0.4f).sp, modifier = Modifier.alpha(0.5f))
                        }
                    }
                    if (stacked) {
                        // Wide screens: the coins and the jar in the right-hand column.
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            coins()
                            ArrowIcon(30.dp, Modifier.rotate(90f), color = Kid.ink)
                            jar()
                        }
                    } else {
                        Row(
                            Modifier.fillMaxWidth().height(card),
                            horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.weight(1f, fill = false)) { coins() }
                            ArrowIcon(30.dp, color = Kid.ink)
                            jar()
                        }
                    }
                }
            }
        },
    )
}
