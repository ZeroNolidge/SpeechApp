package com.zeronolidge.speechapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.data.LetterContent
import com.zeronolidge.speechapp.data.LetterInfo
import com.zeronolidge.speechapp.logic.LetterMode
import com.zeronolidge.speechapp.logic.Letters
import com.zeronolidge.speechapp.state.Progress
import com.zeronolidge.speechapp.ui.components.AnswerRow
import com.zeronolidge.speechapp.ui.components.KidScreen
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.Prompt
import com.zeronolidge.speechapp.ui.theme.Kid
import kotlin.random.Random

/** Says a letter's name, its sound and a word that starts with it. */
fun letterIntro(l: LetterInfo) = "${l.upper}. ${l.upper} says ${l.sound}. ${l.sound}, ${l.word.text}."

@Composable
fun AlphabetScreen(back: () -> Unit) {
    val app = LocalApp.current
    var current by remember { mutableStateOf(LetterContent.letters.first()) }
    KidScreen("ABC 🔠", Kid.letters, back, "Tap a letter") {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // The big letter card.
            Column(
                Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
                    .shadow(8.dp, RoundedCornerShape(32.dp))
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White)
                    .clickable { app.speaker.say(letterIntro(current)) }
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${current.upper}", fontSize = 110.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E88E5))
                    Text("${current.lower}", fontSize = 90.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE53935))
                }
                Text(current.word.emoji, fontSize = 72.sp)
                Text(current.word.text, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Kid.ink)
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.weight(1.4f).fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(LetterContent.letters) { l ->
                    val selected = l == current
                    Column(
                        Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) Kid.highlight else Color.White)
                            .clickable {
                                current = l
                                app.speaker.say(letterIntro(l))
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("${l.upper}${l.lower}", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Kid.ink)
                    }
                }
            }
        }
    }
}

@Composable
fun LetterGameScreen(mode: LetterMode, back: () -> Unit) {
    val app = LocalApp.current
    val rnd = remember { Random(System.nanoTime()) }
    var roundNo by remember { mutableIntStateOf(0) }
    // After a few right answers, mix in look-alike letters such as b, d, p and q.
    val tricky = app.progress.areaStars(Progress.AREA_LETTERS) > 15
    val round = remember(roundNo) { Letters.make(mode, rnd, tricky) }
    val t = round.target

    val prompt = when (mode) {
        LetterMode.BIG_TO_SMALL -> "Find little ${t.lower}"
        LetterMode.SMALL_TO_BIG -> "Find big ${t.upper}"
        LetterMode.FIRST_SOUND -> "${t.word.text} starts with…"
    }
    val spoken = when (mode) {
        LetterMode.BIG_TO_SMALL -> "This is big ${t.upper}. Can you find little ${t.lower}?"
        LetterMode.SMALL_TO_BIG -> "This is little ${t.lower}. Can you find big ${t.upper}?"
        LetterMode.FIRST_SOUND -> "${t.word.text}. ${t.sound}, ${t.sound}, ${t.word.text}. Which letter does ${t.word.text} start with?"
    }
    LaunchedEffect(roundNo) { app.speaker.say(spoken) }

    KidScreen(
        when (mode) {
            LetterMode.BIG_TO_SMALL -> "Big → small"
            LetterMode.SMALL_TO_BIG -> "small → Big"
            LetterMode.FIRST_SOUND -> "First sound"
        },
        Kid.letters, back,
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Prompt(prompt) { app.speaker.say(spoken) }
            Text(
                when (mode) {
                    LetterMode.BIG_TO_SMALL -> "${t.upper}"
                    LetterMode.SMALL_TO_BIG -> "${t.lower}"
                    LetterMode.FIRST_SOUND -> t.word.emoji
                },
                fontSize = 100.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E88E5),
                modifier = Modifier.clickable { app.speaker.say(spoken) },
            )
            AnswerRow(
                options = round.choice.options,
                answer = round.choice.answer,
                area = Progress.AREA_LETTERS,
                roundKey = roundNo,
                modifier = Modifier.fillMaxWidth(),
                cardModifier = Modifier.size(130.dp),
                onSolved = { roundNo++ },
                describe = { l -> if (mode == LetterMode.FIRST_SOUND) "${l.upper}, ${l.sound}" else "${l.upper}" },
            ) { l ->
                Text(
                    when (mode) {
                        LetterMode.BIG_TO_SMALL -> "${l.lower}"
                        LetterMode.SMALL_TO_BIG -> "${l.upper}"
                        LetterMode.FIRST_SOUND -> "${l.upper}${l.lower}"
                    },
                    fontSize = 72.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Kid.ink,
                )
            }
        }
    }
}
