package com.zeronolidge.speechapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.zeronolidge.speechapp.data.ReadingContent
import com.zeronolidge.speechapp.data.SpeechContent
import com.zeronolidge.speechapp.logic.Challenge
import com.zeronolidge.speechapp.logic.LetterMode
import com.zeronolidge.speechapp.logic.Letters
import com.zeronolidge.speechapp.logic.Numbers
import com.zeronolidge.speechapp.logic.OddOneOut
import com.zeronolidge.speechapp.logic.Patterns
import com.zeronolidge.speechapp.logic.Reading
import com.zeronolidge.speechapp.logic.RobotLevels
import com.zeronolidge.speechapp.state.Progress
import com.zeronolidge.speechapp.ui.Screen
import com.zeronolidge.speechapp.ui.challenge.ChallengeView
import com.zeronolidge.speechapp.ui.challenge.EmojiGroup
import com.zeronolidge.speechapp.ui.components.BigTile
import com.zeronolidge.speechapp.ui.components.KidScreen
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.theme.Kid
import kotlin.random.Random

/** Free-play games, outside the story. Each one is an endless loop of one kind of challenge. */
enum class PracticeKind(val emoji: String, val label: String, val spoken: String, val color: Color) {
    BIG_SMALL("🅰️", "Big → small", "Find the little letter!", Kid.letters),
    SMALL_BIG("🔡", "small → Big", "Find the big letter!", Kid.letters),
    FIRST_SOUND("👂", "First sound", "What sound does it start with?", Kid.letters),
    SOUND_OUT("🧱", "Sound it out", "Let's sound out words!", Kid.reading),
    WHICH_WORD("🖼️", "Which word?", "Which word matches the picture?", Kid.reading),
    COUNT("🍎", "Count", "Let's count!", Kid.numbers),
    MORE("⚖️", "More?", "Which has more?", Kid.numbers),
    ADD("➕", "Add", "Let's add!", Kid.numbers),
    TAKE_AWAY("➖", "Take away", "Let's take away!", Kid.numbers),
    NEXT("🔁", "What's next?", "What comes next?", Kid.patterns),
    ODD("🔍", "Odd one out", "Which one is different?", Kid.patterns),
    ROBOT_ARROWS("⬆️", "Robot arrows", "Use arrows to move the robot!", Kid.robot),
    ROBOT_TURNS("↪️", "Robot turns", "Turn the robot and go forward!", Kid.robot),
}

@Composable
fun PracticeMenu(go: (Screen) -> Unit, back: () -> Unit) {
    val app = LocalApp.current
    data class Tile(val emoji: String, val label: String, val spoken: String, val color: Color, val target: Screen)
    val tiles = listOf(
        Tile("🗣️", "Talk", "Pick a sound to practice", Kid.talk, Screen.TalkMenu),
        Tile("🔠", "ABC", "The alphabet!", Kid.letters, Screen.Alphabet),
        Tile("🔟", "1 2 3", "Let's learn numbers!", Kid.numbers, Screen.NumberLine),
    ) + PracticeKind.entries.map { Tile(it.emoji, it.label, it.spoken, it.color, Screen.PracticeGame(it)) }
    KidScreen("Practice 🎯", Kid.patterns, back, "Pick a game") {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(130.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(tiles) { t ->
                BigTile(t.emoji, t.label, t.color, Modifier.size(130.dp), emojiSize = 44.sp) {
                    app.speaker.say(t.spoken)
                    go(t.target)
                }
            }
        }
    }
}

@Composable
fun PracticeGameScreen(kind: PracticeKind, back: () -> Unit) {
    val app = LocalApp.current
    val p = app.progress
    val rnd = remember { Random(System.nanoTime()) }
    var roundNo by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var last by remember { mutableStateOf<String?>(null) }
    val challenge = remember(roundNo) {
        val big = p.areaStars(Progress.AREA_NUMBERS) > 15
        val tricky = p.areaStars(Progress.AREA_LETTERS) > 15
        when (kind) {
            PracticeKind.BIG_SMALL -> Challenge.Letter(Letters.make(LetterMode.BIG_TO_SMALL, rnd, tricky))
            PracticeKind.SMALL_BIG -> Challenge.Letter(Letters.make(LetterMode.SMALL_TO_BIG, rnd, tricky))
            PracticeKind.FIRST_SOUND -> Challenge.Letter(Letters.make(LetterMode.FIRST_SOUND, rnd, tricky))
            PracticeKind.SOUND_OUT -> Challenge.SoundOut(Reading.make(ReadingContent.cvcWords, rnd, ReadingContent.cvcWords.firstOrNull { it.text == last }))
            PracticeKind.WHICH_WORD -> Challenge.WhichWord(Reading.make(ReadingContent.cvcWords, rnd, ReadingContent.cvcWords.firstOrNull { it.text == last }))
            PracticeKind.COUNT -> Challenge.Count(Numbers.count(if (big) 10 else 6, rnd))
            PracticeKind.MORE -> Challenge.More(Numbers.compare(8, rnd))
            PracticeKind.ADD -> Challenge.Math(Numbers.math(true, if (big) 10 else 5, rnd))
            PracticeKind.TAKE_AWAY -> Challenge.Math(Numbers.math(false, if (big) 10 else 5, rnd))
            PracticeKind.NEXT -> Challenge.Pattern(Patterns.make(p.level("patterns"), rnd))
            PracticeKind.ODD -> Challenge.OddOne(OddOneOut.make(if (roundNo < 5) 0 else 1, rnd))
            PracticeKind.ROBOT_ARROWS -> {
                val n = p.level("robot_easy")
                Challenge.Robot(RobotLevels.easy.getOrNull(n) ?: RobotLevels.random(false, rnd), "🍪")
            }
            PracticeKind.ROBOT_TURNS -> {
                val n = p.level("robot_turn")
                Challenge.Robot(RobotLevels.robot.getOrNull(n) ?: RobotLevels.random(true, rnd), "🍪")
            }
        }
    }
    KidScreen("${kind.label} ${kind.emoji}", kind.color, back, kind.spoken) {
        ChallengeView(challenge, Modifier.fillMaxSize()) { firstTry ->
            when (kind) {
                PracticeKind.NEXT -> {
                    // Every few right-first-time answers, the patterns get trickier.
                    streak = if (firstTry) streak + 1 else 0
                    if (streak >= 4 && p.level("patterns") < Patterns.maxLevel()) {
                        streak = 0
                        p.setLevel("patterns", p.level("patterns") + 1)
                    }
                }
                PracticeKind.ROBOT_ARROWS -> p.setLevel("robot_easy", p.level("robot_easy") + 1)
                PracticeKind.ROBOT_TURNS -> p.setLevel("robot_turn", p.level("robot_turn") + 1)
                else -> {}
            }
            last = (challenge as? Challenge.SoundOut)?.round?.word?.text ?: (challenge as? Challenge.WhichWord)?.round?.word?.text
            roundNo++
        }
    }
}

@Composable
fun TalkMenu(go: (Screen) -> Unit, back: () -> Unit) {
    val app = LocalApp.current
    KidScreen("Talk 🗣️", Kid.talk, back, "Pick a sound to practice") {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(130.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(SpeechContent.sounds) { sound ->
                BigTile(sound.emoji, sound.label, Kid.talk, Modifier.size(130.dp), emojiSize = 44.sp) {
                    app.speaker.say(sound.modelSound)
                    go(Screen.TalkPractice(sound))
                }
            }
        }
    }
}

/** Says a letter's name, its sound and a word that starts with it. */
fun letterIntro(l: LetterInfo) = "${l.upper}. ${l.upper} says ${l.sound}. ${l.sound}, ${l.word.text}."

@Composable
fun AlphabetScreen(back: () -> Unit) {
    val app = LocalApp.current
    var current by remember { mutableStateOf(LetterContent.letters.first()) }
    KidScreen("ABC 🔠", Kid.letters, back, "Tap a letter") {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
                    Text("${current.upper}", fontSize = 96.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E88E5))
                    Text("${current.lower}", fontSize = 80.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE53935))
                }
                Text(current.word.emoji, fontSize = 64.sp)
                Text(current.word.text, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Kid.ink)
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.weight(1.4f).fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(LetterContent.letters) { l ->
                    Column(
                        Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (l == current) Kid.highlight else Color.White)
                            .clickable {
                                current = l
                                app.speaker.say(letterIntro(l))
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("${l.upper}${l.lower}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Kid.ink)
                    }
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
                Text("$current", fontSize = 90.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF57C00))
                EmojiGroup("🔵", current, Modifier.widthIn(max = 300.dp), size = 24)
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
                        Text("$n", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Kid.ink)
                    }
                }
            }
        }
    }
}
