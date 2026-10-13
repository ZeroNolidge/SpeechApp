package com.zeronolidge.speechapp.ui.screens

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.data.Praise
import com.zeronolidge.speechapp.data.Things
import com.zeronolidge.speechapp.logic.Numbers
import com.zeronolidge.speechapp.state.Progress
import com.zeronolidge.speechapp.ui.components.AfterDelay
import com.zeronolidge.speechapp.ui.components.AnswerRow
import com.zeronolidge.speechapp.ui.components.KidScreen
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.Prompt
import com.zeronolidge.speechapp.ui.theme.Kid
import kotlin.random.Random

private val NumberCard = Modifier.size(120.dp)

@Composable
private fun NumberText(n: Int) {
    Text("$n", fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, color = Kid.ink)
}

/** A neat group of emoji. Tapping one counts it out loud, if [countable] is set. */
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
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(count) { i ->
            val crossed = i >= count - crossedOut
            val countIndex = counted.indexOf(i)
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(if (countIndex >= 0) Kid.highlight else Color.Transparent)
                    .then(if (onTapItem != null) Modifier.clickable { onTapItem(i) } else Modifier)
                    .padding(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, fontSize = size.sp, modifier = Modifier.alpha(if (crossed) 0.3f else 1f))
                if (crossed) Text("❌", fontSize = (size * 0.7f).sp)
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
        }
    }
}

/** Tap any number from 1 to 20 to hear it and see that many dots. */
@Composable
fun NumberLineScreen(back: () -> Unit) {
    val app = LocalApp.current
    var current by remember { mutableIntStateOf(1) }
    KidScreen("1 2 3 🔟", Kid.numbers, back, "Tap a number") {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .shadow(8.dp, RoundedCornerShape(32.dp))
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White)
                    .clickable { app.speaker.say(Numbers.word(current)) }
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                Text("$current", fontSize = 100.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF57C00))
                // Ten-frames: rows of five are easy to see at a glance.
                EmojiGroup("🔵", current, Modifier.widthIn(max = 320.dp), size = 26)
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.weight(1.2f).fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items((1..20).toList()) { n ->
                    Box(
                        Modifier
                            .aspectRatio(1.3f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (n == current) Kid.highlight else Color.White)
                            .clickable {
                                current = n
                                app.speaker.say(Numbers.word(n))
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$n", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Kid.ink)
                    }
                }
            }
        }
    }
}

/** Tap each thing to count it, then pick the number. */
@Composable
fun CountingScreen(back: () -> Unit) {
    val app = LocalApp.current
    val rnd = remember { Random(System.nanoTime()) }
    var roundNo by remember { mutableIntStateOf(0) }
    val max = if (app.progress.areaStars(Progress.AREA_NUMBERS) > 10) 10 else 6
    val round = remember(roundNo) { Numbers.count(max, rnd) }
    var counted by remember(roundNo) { mutableStateOf(listOf<Int>()) }
    val name = Things.nameOf(round.emoji)
    val spoken = "How many $name? Tap each one to count."
    LaunchedEffect(roundNo) { app.speaker.say(spoken) }

    KidScreen("Count 🍎", Kid.numbers, back) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Prompt("How many?") { app.speaker.say(spoken) }
            EmojiGroup(
                round.emoji, round.count,
                Modifier.fillMaxWidth(0.8f),
                size = 60,
                counted = counted,
                onTapItem = { i ->
                    if (i !in counted) {
                        counted = counted + i
                        app.speaker.say(Numbers.word(counted.size))
                    }
                },
            )
            AnswerRow(
                options = round.choice.options,
                answer = round.choice.answer,
                area = Progress.AREA_NUMBERS,
                roundKey = roundNo,
                modifier = Modifier.fillMaxWidth(),
                cardModifier = NumberCard,
                onSolved = { roundNo++ },
                describe = { Numbers.word(it) },
            ) { NumberText(it) }
        }
    }
}

/** Adding: two groups join together. Taking away: some get crossed out. */
@Composable
fun MathScreen(plus: Boolean, back: () -> Unit) {
    val app = LocalApp.current
    val rnd = remember { Random(System.nanoTime()) }
    var roundNo by remember { mutableIntStateOf(0) }
    val max = if (app.progress.areaStars(Progress.AREA_NUMBERS) > 20) 10 else 5
    val round = remember(roundNo) { Numbers.math(plus, max, rnd) }
    val name = Things.nameOf(round.emoji)
    val spoken = if (plus) {
        "${Numbers.word(round.a)} $name, and ${Numbers.word(round.b)} more. How many altogether?"
    } else {
        "${Numbers.word(round.a)} $name. Take away ${Numbers.word(round.b)}. How many are left?"
    }
    LaunchedEffect(roundNo) { app.speaker.say(spoken) }

    KidScreen(if (plus) "Add ➕" else "Take away ➖", Kid.numbers, back) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Prompt(if (plus) "${round.a} + ${round.b} = ?" else "${round.a} − ${round.b} = ?") { app.speaker.say(spoken) }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (plus) {
                    EmojiGroup(round.emoji, round.a, Modifier.weight(1f), size = 48)
                    Text("➕", fontSize = 48.sp, modifier = Modifier.padding(horizontal = 12.dp))
                    EmojiGroup(round.emoji, round.b, Modifier.weight(1f), size = 48)
                } else {
                    EmojiGroup(round.emoji, round.a, Modifier.fillMaxWidth(0.8f), size = 52, crossedOut = round.b)
                }
            }
            AnswerRow(
                options = round.choice.options,
                answer = round.choice.answer,
                area = Progress.AREA_NUMBERS,
                roundKey = roundNo,
                modifier = Modifier.fillMaxWidth(),
                cardModifier = NumberCard,
                onSolved = { roundNo++ },
                describe = { Numbers.word(it) },
            ) { NumberText(it) }
        }
    }
}

/** Which side has more? */
@Composable
fun MoreOrLessScreen(back: () -> Unit) {
    val app = LocalApp.current
    val rnd = remember { Random(System.nanoTime()) }
    var roundNo by remember { mutableIntStateOf(0) }
    val round = remember(roundNo) { Numbers.compare(8, rnd) }
    var picked by remember(roundNo) { mutableStateOf<Boolean?>(null) }
    var faded by remember(roundNo) { mutableStateOf(setOf<Boolean>()) }
    val name = Things.nameOf(round.emoji)
    val spoken = "Which side has more $name?"
    LaunchedEffect(roundNo) { app.speaker.say(spoken) }
    if (picked != null) AfterDelay(roundNo, 3500) { roundNo++ }

    KidScreen("Which has more? ⚖️", Kid.numbers, back) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Prompt("Which has more?") { app.speaker.say(spoken) }
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                listOf(true to round.left, false to round.right).forEach { (isLeft, n) ->
                    val right = isLeft == round.moreIsLeft
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .alpha(if (isLeft in faded) 0.35f else 1f)
                            .shadow(6.dp, RoundedCornerShape(28.dp))
                            .clip(RoundedCornerShape(28.dp))
                            .background(if (picked == isLeft) Kid.good else Color.White)
                            .clickable(enabled = picked == null && isLeft !in faded) {
                                if (right) {
                                    picked = isLeft
                                    val more = maxOf(round.left, round.right)
                                    val less = minOf(round.left, round.right)
                                    app.reward(
                                        Progress.AREA_NUMBERS,
                                        "${Praise.great.random()} ${Numbers.word(more)} is more than ${Numbers.word(less)}.",
                                    )
                                } else {
                                    faded = faded + isLeft
                                    app.tryAgain()
                                }
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmojiGroup(round.emoji, n, size = 48)
                        if (picked != null) {
                            Text(
                                "$n",
                                fontSize = 40.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Kid.ink,
                                modifier = Modifier.align(Alignment.TopEnd),
                            )
                        }
                    }
                }
            }
        }
    }
}
