package com.zeronolidge.speechapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.data.LetterContent
import com.zeronolidge.speechapp.data.ReadingContent
import com.zeronolidge.speechapp.data.Word
import com.zeronolidge.speechapp.logic.Reading
import com.zeronolidge.speechapp.state.Progress
import com.zeronolidge.speechapp.ui.components.AnswerRow
import com.zeronolidge.speechapp.ui.components.KidScreen
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.Prompt
import com.zeronolidge.speechapp.ui.components.RoundIcon
import com.zeronolidge.speechapp.ui.theme.Kid
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Sound it out: tap each letter to hear its sound, then blend the sounds into a word
 * and find the matching picture.
 */
@Composable
fun SoundItOutScreen(back: () -> Unit) {
    val app = LocalApp.current
    val rnd = remember { Random(System.nanoTime()) }
    val scope = rememberCoroutineScope()
    var roundNo by remember { mutableIntStateOf(0) }
    var last by remember { mutableStateOf<Word?>(null) }
    val round = remember(roundNo) { Reading.make(ReadingContent.cvcWords, rnd, last) }
    val word = round.word.text
    var lit by remember(roundNo) { mutableIntStateOf(-1) }
    var tapped by remember(roundNo) { mutableStateOf(setOf<Int>()) }
    var blended by remember(roundNo) { mutableStateOf(false) }
    var blendJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(roundNo) {
        last = round.word
        app.speaker.say("Tap each letter to hear its sound.")
    }

    fun blend() {
        val sounds = Reading.sounds(word)
        app.speaker.say(sounds.joinToString("... "), slow = true)
        app.speaker.then("$word!")
        // Light each letter up roughly in time with the voice.
        blendJob?.cancel()
        blendJob = scope.launch {
            sounds.indices.forEach { i ->
                lit = i
                delay(900)
            }
            lit = -1
            blended = true
            delay(600)
            app.speaker.then("Which picture is $word?")
        }
    }

    KidScreen("Sound it out 🧱", Kid.reading, back, "Tap each letter, then blend the sounds") {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                word.forEachIndexed { i, c ->
                    val letter = LetterContent.of(c)
                    Box(
                        Modifier
                            .size(120.dp)
                            .shadow(6.dp, RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
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
                        Text("$c", fontSize = 80.sp, fontWeight = FontWeight.ExtraBold, color = Kid.ink)
                    }
                }
                Box(Modifier.width(8.dp))
                // Blend becomes extra inviting once every letter has been tapped.
                RoundIcon(
                    "🐢", if (tapped.size == word.length) Kid.highlight else Kid.soft, size = 96.dp,
                ) { blend() }
            }
            Box(Modifier.height(190.dp), contentAlignment = Alignment.Center) {
                if (blended) {
                    AnswerRow(
                        options = round.choice.options,
                        answer = round.choice.answer,
                        area = Progress.AREA_READING,
                        roundKey = roundNo,
                        cardModifier = Modifier.size(150.dp),
                        onSolved = { roundNo++ },
                        describe = { it.text },
                    ) { w -> Text(w.emoji, fontSize = 84.sp) }
                } else {
                    Text("Tap 🐢 to blend!", fontSize = 26.sp, color = Kid.ink.copy(alpha = 0.6f))
                }
            }
        }
    }
}

/** Look at the picture and pick the word that matches it. */
@Composable
fun WhichWordScreen(back: () -> Unit) {
    val app = LocalApp.current
    val rnd = remember { Random(System.nanoTime()) }
    var roundNo by remember { mutableIntStateOf(0) }
    var last by remember { mutableStateOf<Word?>(null) }
    val round = remember(roundNo) { Reading.make(ReadingContent.cvcWords, rnd, last) }
    val spoken = "Find the word ${round.word.text}."

    LaunchedEffect(roundNo) {
        last = round.word
        app.speaker.say(spoken)
    }

    KidScreen("Which word? 🖼️", Kid.reading, back) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Prompt("Which word?") { app.speaker.say(spoken) }
            Text(round.word.emoji, fontSize = 110.sp, modifier = Modifier.clickable { app.speaker.say(spoken) })
            AnswerRow(
                options = round.choice.options,
                answer = round.choice.answer,
                area = Progress.AREA_READING,
                roundKey = roundNo,
                modifier = Modifier.fillMaxWidth(),
                cardModifier = Modifier.padding(4.dp).width(190.dp).height(120.dp),
                onSolved = { roundNo++ },
                describe = { it.text },
            ) { w -> Text(w.text, fontSize = 60.sp, fontWeight = FontWeight.ExtraBold, color = Kid.ink) }
        }
    }
}
