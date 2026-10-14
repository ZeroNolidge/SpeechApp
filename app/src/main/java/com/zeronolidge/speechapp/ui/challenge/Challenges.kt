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
import com.zeronolidge.speechapp.logic.RobotLevel
import com.zeronolidge.speechapp.logic.RobotSim
import com.zeronolidge.speechapp.logic.RobotState
import com.zeronolidge.speechapp.logic.StepResult
import com.zeronolidge.speechapp.logic.Subject
import com.zeronolidge.speechapp.state.Progress
import com.zeronolidge.speechapp.ui.components.AnswerRow
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.RoundIcon
import com.zeronolidge.speechapp.ui.screens.SayPanel
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
}

fun areaOf(c: Challenge): String = when (c.subject) {
    Subject.SPEECH -> Progress.AREA_SPEECH
    Subject.LETTERS -> Progress.AREA_LETTERS
    Subject.READING -> Progress.AREA_READING
    Subject.NUMBERS -> Progress.AREA_NUMBERS
    Subject.PATTERNS -> Progress.AREA_PATTERNS
    Subject.ROBOT -> Progress.AREA_ROBOT
}

/** Helper powers poke challenges through these counters. */
class PowerTicks {
    var wand by mutableIntStateOf(0)
    var map by mutableIntStateOf(0)
}

/**
 * Shows one challenge. [onDone] gets true if it was solved without any wrong answers.
 * The prompt is queued after whatever is being said, so story narration comes first.
 */
@Composable
fun ChallengeView(
    challenge: Challenge,
    modifier: Modifier = Modifier,
    /** Story narration said just before the challenge's own question. */
    intro: String? = null,
    powers: PowerTicks = remember(challenge) { PowerTicks() },
    onDone: (firstTry: Boolean) -> Unit,
) {
    val app = LocalApp.current
    var misses by remember(challenge) { mutableIntStateOf(0) }
    LaunchedEffect(challenge) {
        // The speaking challenge says its own intro, then turns the microphone on.
        if (challenge !is Challenge.Say) {
            if (intro != null) app.speaker.say(intro)
            app.speaker.then(promptFor(challenge))
        }
    }
    val miss: () -> Unit = { misses++ }
    val done: () -> Unit = { onDone(misses == 0) }
    val area = areaOf(challenge)
    Box(modifier) {
        when (challenge) {
            is Challenge.Say -> {
                SayPanel(challenge.sound, challenge.word, Modifier.fillMaxSize(), intro = intro, onSaid = { onDone(true) })
            }
            is Challenge.Count -> CountView(challenge, area, powers.wand, miss, done)
            is Challenge.Math -> MathView(challenge, area, powers.wand, miss, done)
            is Challenge.More -> MoreView(challenge, area, powers.wand, miss, done)
            is Challenge.Letter -> LetterView(challenge, area, powers.wand, miss, done)
            is Challenge.SoundOut -> SoundOutView(challenge, area, powers.wand, miss, done)
            is Challenge.WhichWord -> WhichWordView(challenge, area, powers.wand, miss, done)
            is Challenge.Pattern -> PatternView(challenge, area, powers.wand, miss, done)
            is Challenge.OddOne -> OddOneView(challenge, area, powers.wand, miss, done)
            is Challenge.Robot -> RobotView(challenge, area, powers.map, miss, done)
        }
    }
}

/** Lays out a question area above a row of answer cards, scaled to fit. */
@Composable
private fun QuestionLayout(
    question: @Composable (cardSize: Dp) -> Unit,
    answers: @Composable (cardSize: Dp) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val card = minOf(maxWidth / 4.2f, maxHeight / 2.6f, 140.dp)
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
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(count) { i ->
            val crossed = i >= count - crossedOut
            val countIndex = counted.indexOf(i)
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(if (countIndex >= 0) Kid.highlight else Color.Transparent)
                    .then(if (onTapItem != null) Modifier.clickable { onTapItem(i) } else Modifier)
                    .padding(3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, fontSize = size.sp, modifier = Modifier.alpha(if (crossed) 0.3f else 1f))
                if (crossed) Text("❌", fontSize = (size * 0.7f).sp)
                if (countIndex >= 0) {
                    Text(
                        "${countIndex + 1}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFD84315),
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
            }
        }
    }
}

@Composable
private fun CountView(c: Challenge.Count, area: String, wand: Int, miss: () -> Unit, done: () -> Unit) {
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
                c.round.choice.options, c.round.choice.answer, area, c,
                Modifier.fillMaxWidth(), Modifier.size(card),
                onSolved = done, describe = { Numbers.word(it) }, onMiss = miss, wandTick = wand,
            ) { NumberText(it, card) }
        },
    )
}

@Composable
private fun MathView(c: Challenge.Math, area: String, wand: Int, miss: () -> Unit, done: () -> Unit) {
    val r = c.round
    QuestionLayout(
        question = { card ->
            val s = (card.value * 0.3f).toInt()
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (r.plus) "${r.a} + ${r.b} = ?" else "${r.a} − ${r.b} = ?",
                    fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Kid.ink,
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
                r.choice.options, r.choice.answer, area, c,
                Modifier.fillMaxWidth(), Modifier.size(card),
                onSolved = done, describe = { Numbers.word(it) }, onMiss = miss, wandTick = wand,
            ) { NumberText(it, card) }
        },
    )
}

@Composable
private fun MoreView(c: Challenge.More, area: String, wand: Int, miss: () -> Unit, done: () -> Unit) {
    val r = c.round
    val app = LocalApp.current
    var solved by remember(c) { mutableStateOf(false) }
    var faded by remember(c) { mutableStateOf(setOf<Boolean>()) }
    LaunchedEffect(c, wand) {
        if (wand > 0 && !solved) faded = setOf(!r.moreIsLeft)
    }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(true to r.left, false to r.right).forEach { (isLeft, n) ->
            val right = isLeft == r.moreIsLeft
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .alpha(if (isLeft in faded) 0.35f else 1f)
                    .paperCard(if (solved && right) Kid.good else Color.White, 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(enabled = !solved && isLeft !in faded) {
                        if (right) {
                            solved = true
                            app.reward(
                                area,
                                "${Praise.great.random()} ${Numbers.word(maxOf(r.left, r.right))} is more than ${Numbers.word(minOf(r.left, r.right))}.",
                                onDone = done,
                            )
                        } else {
                            faded = faded + isLeft
                            miss()
                            app.tryAgain()
                        }
                    }
                    .padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                EmojiGroup(r.emoji, n, size = 40)
                if (solved) {
                    Text("$n", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Kid.ink, modifier = Modifier.align(Alignment.TopEnd))
                }
            }
        }
    }
}

@Composable
private fun LetterView(c: Challenge.Letter, area: String, wand: Int, miss: () -> Unit, done: () -> Unit) {
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
                c.round.choice.options, c.round.choice.answer, area, c,
                Modifier.fillMaxWidth(), Modifier.size(card),
                onSolved = done,
                describe = { l -> if (mode == LetterMode.FIRST_SOUND) "${l.upper}, ${l.sound}" else "${l.upper}" },
                onMiss = miss, wandTick = wand,
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
private fun SoundOutView(c: Challenge.SoundOut, area: String, wand: Int, miss: () -> Unit, done: () -> Unit) {
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
                    Box(
                        Modifier
                            .size(card * 0.85f)
                            .shadow(6.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                when {
                                    lit == i -> Kid.highlight
                                    i in tapped -> Color(0xFFE1F5FE)
                                    else -> Color.White
                                },
                            )
                            .clickable {
                                tapped = tapped + i
                                lit = i
                                app.speaker.say(letter.sound)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$ch", fontSize = (card.value * 0.55f).sp, fontWeight = FontWeight.ExtraBold, color = Kid.ink)
                    }
                }
                Box(Modifier.width(4.dp))
                RoundIcon("🐢", if (tapped.size == word.length) Kid.highlight else Kid.soft, size = card * 0.75f) { blend() }
            }
        },
        answers = { card ->
            Box(Modifier.height(card + 8.dp), contentAlignment = Alignment.Center) {
                if (blended) {
                    AnswerRow(
                        c.round.choice.options, c.round.choice.answer, area, c,
                        cardModifier = Modifier.size(card),
                        onSolved = done, describe = { it.text }, onMiss = miss, wandTick = wand,
                    ) { w -> Text(w.emoji, fontSize = (card.value * 0.55f).sp) }
                } else {
                    Text("Tap 🐢 to blend!", fontSize = 24.sp, color = Kid.ink.copy(alpha = 0.6f))
                }
            }
        },
    )
}

@Composable
private fun WhichWordView(c: Challenge.WhichWord, area: String, wand: Int, miss: () -> Unit, done: () -> Unit) {
    val app = LocalApp.current
    QuestionLayout(
        question = { card ->
            Text(c.round.word.emoji, fontSize = (card.value * 0.75f).sp, modifier = Modifier.clickable { app.speaker.say(promptFor(c)) })
        },
        answers = { card ->
            AnswerRow(
                c.round.choice.options, c.round.choice.answer, area, c,
                Modifier.fillMaxWidth(), Modifier.width(card * 1.3f).height(card * 0.8f),
                onSolved = done, describe = { it.text }, onMiss = miss, wandTick = wand,
            ) { w -> Text(w.text, fontSize = (card.value * 0.38f).sp, fontWeight = FontWeight.ExtraBold, color = Kid.ink) }
        },
    )
}

@Composable
private fun PatternView(c: Challenge.Pattern, area: String, wand: Int, miss: () -> Unit, done: () -> Unit) {
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
                r.choice.options, r.choice.answer, area, c,
                Modifier.fillMaxWidth(), Modifier.size(card),
                onSolved = done, onMiss = miss, wandTick = wand,
            ) { Text(it, fontSize = (card.value * 0.5f).sp) }
        },
    )
}

@Composable
private fun OddOneView(c: Challenge.OddOne, area: String, wand: Int, miss: () -> Unit, done: () -> Unit) {
    val r = c.round
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val card = minOf(maxWidth / 5f, maxHeight / 1.6f, 140.dp)
        // Options are positions, so identical pictures are still separate cards.
        AnswerRow(
            r.items.indices.toList(), r.items.indexOf(r.odd), area, c,
            Modifier.fillMaxWidth(), Modifier.size(card),
            onSolved = done, onMiss = miss, wandTick = wand,
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
private fun RobotView(c: Challenge.Robot, area: String, mapTick: Int, miss: () -> Unit, done: () -> Unit) {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    val level = c.level
    val program = remember(c) { mutableStateListOf<Cmd>() }
    var robot by remember(c) { mutableStateOf(RobotState(level.start, level.startFacing)) }
    var runningStep by remember(c) { mutableIntStateOf(-1) }
    var bumpAt by remember(c) { mutableStateOf<Cell?>(null) }
    var won by remember(c) { mutableStateOf(false) }
    var hint by remember(c) { mutableStateOf<Cmd?>(null) }
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
                        app.speaker.say(if (r.into in level.blocks) "Bump! A tree is in the way." else "Bump! That's the edge.")
                        repeat(3) {
                            wobble.animateTo(8f, tween(60))
                            wobble.animateTo(-8f, tween(60))
                        }
                        wobble.animateTo(0f, tween(60))
                        delay(1500)
                        app.speaker.say("Let's fix the plan and try again!")
                        reset()
                        return@launch
                    }
                }
                robot = state
                delay(550)
            }
            runningStep = -1
            if (state.pos == level.goal) {
                won = true
                app.reward(area, "Hooray! The robot made it! You did it!", onDone = done)
            } else {
                miss()
                app.speaker.say("Hmm, the robot didn't get there. Let's change the plan!")
                delay(2200)
                reset()
            }
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
            ProgramStrip(program, runningStep, Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                palette.forEach { cmd ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(64.dp)
                            .paperCard(if (cmd == hint) Kid.highlight else Color.White, 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = !running && !won) {
                                hint = null
                                if (program.size < MAX_PROGRAM) {
                                    program.add(cmd)
                                    app.speaker.say(cmd.spoken)
                                } else {
                                    app.speaker.say("The plan is full! Press Go.")
                                }
                            },
                        contentAlignment = Alignment.Center,
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
                RoundIcon("🗑️", Kid.soft, size = 52.dp, enabled = !running && !won && program.isNotEmpty()) {
                    program.clear()
                    reset()
                    app.speaker.say("All gone!")
                }
                RoundIcon("⌫", Kid.soft, size = 52.dp, enabled = !running && !won && program.isNotEmpty()) {
                    program.removeAt(program.lastIndex)
                    app.speaker.say("Undo.")
                }
                RoundIcon("▶️", Kid.good, size = 72.dp, enabled = !running && !won) { go() }
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
                            .then(if (here == bumpAt) Modifier.border(3.dp, Color(0xFFE53935)) else Modifier),
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
        Text(
            "▲",
            fontSize = (cell.value * 0.2f).sp,
            color = Color(0xFFD84315),
            modifier = Modifier.align(align).padding(2.dp).rotate(facing.degrees),
        )
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
