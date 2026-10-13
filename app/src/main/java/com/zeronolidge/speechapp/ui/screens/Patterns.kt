package com.zeronolidge.speechapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.logic.OddOneOut
import com.zeronolidge.speechapp.logic.Patterns
import com.zeronolidge.speechapp.state.Progress
import com.zeronolidge.speechapp.ui.components.AnswerRow
import com.zeronolidge.speechapp.ui.components.KidScreen
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.Prompt
import com.zeronolidge.speechapp.ui.theme.Kid
import kotlin.random.Random

private const val LEVEL_KEY = "patterns"

/** Every few right answers the patterns get a little longer and trickier. */
@Composable
fun WhatsNextScreen(back: () -> Unit) {
    val app = LocalApp.current
    val rnd = remember { Random(System.nanoTime()) }
    var roundNo by remember { mutableIntStateOf(0) }
    var level by remember { mutableIntStateOf(app.progress.level(LEVEL_KEY)) }
    var streak by remember { mutableIntStateOf(0) }
    val round = remember(roundNo) { Patterns.make(level, rnd) }
    val spoken = "What comes next?"
    LaunchedEffect(roundNo) { app.speaker.say(spoken) }

    KidScreen("What's next? 🔴🔵", Kid.patterns, back) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Prompt(spoken) { app.speaker.say(spoken) }
            Row(
                Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val size = if (round.shown.size > 8) 40.sp else 52.sp
                round.shown.forEach { Text(it, fontSize = size) }
                Box(
                    Modifier
                        .size(68.dp)
                        .border(4.dp, Color(0xFF7C4DFF), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) { Text("❓", fontSize = 36.sp) }
            }
            AnswerRow(
                options = round.choice.options,
                answer = round.choice.answer,
                area = Progress.AREA_PATTERNS,
                roundKey = roundNo,
                modifier = Modifier.fillMaxWidth(),
                cardModifier = Modifier.size(130.dp),
                onSolved = {
                    streak++
                    if (streak >= 4 && level < Patterns.maxLevel()) {
                        streak = 0
                        level++
                        app.progress.setLevel(LEVEL_KEY, level)
                    }
                    roundNo++
                },
            ) { Text(it, fontSize = 72.sp) }
        }
    }
}

@Composable
fun OddOneOutScreen(back: () -> Unit) {
    val app = LocalApp.current
    val rnd = remember { Random(System.nanoTime()) }
    var roundNo by remember { mutableIntStateOf(0) }
    var solvedCount by remember { mutableIntStateOf(0) }
    val round = remember(roundNo) { OddOneOut.make(if (solvedCount < 5) 0 else 1, rnd) }
    val spoken = "Which one is different?"
    LaunchedEffect(roundNo) { app.speaker.say(spoken) }

    KidScreen("Odd one out 🔍", Kid.patterns, back) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Prompt(spoken) { app.speaker.say(spoken) }
            // Options are positions, so three identical pictures are still separate cards.
            AnswerRow(
                options = round.items.indices.toList(),
                answer = round.items.indexOf(round.odd),
                area = Progress.AREA_PATTERNS,
                roundKey = roundNo,
                modifier = Modifier.fillMaxWidth(),
                cardModifier = Modifier.size(140.dp),
                onSolved = {
                    solvedCount++
                    roundNo++
                },
            ) { i -> Text(round.items[i], fontSize = 80.sp) }
        }
    }
}
