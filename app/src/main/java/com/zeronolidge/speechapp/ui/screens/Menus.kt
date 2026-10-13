package com.zeronolidge.speechapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.data.SpeechContent
import com.zeronolidge.speechapp.logic.LetterMode
import com.zeronolidge.speechapp.ui.Screen
import com.zeronolidge.speechapp.ui.components.BigTile
import com.zeronolidge.speechapp.ui.components.KidScreen
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.StarCount
import com.zeronolidge.speechapp.ui.theme.Kid

/** One option on a menu: what it looks like, what it says, and where it goes. */
data class MenuItem(val emoji: String, val label: String, val spoken: String, val color: Color, val target: Screen)

@Composable
fun MenuGrid(items: List<MenuItem>, go: (Screen) -> Unit, modifier: Modifier = Modifier, columns: Int = 3) {
    val app = LocalApp.current
    val rows = items.chunked(columns)
    Column(
        modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        rows.forEach { row ->
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { item ->
                    BigTile(
                        item.emoji, item.label, item.color,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        emojiSize = 64.sp,
                    ) {
                        app.speaker.say(item.spoken)
                        go(item.target)
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun HomeScreen(go: (Screen) -> Unit) {
    val app = LocalApp.current
    Column(
        Modifier
            .fillMaxSize()
            .background(Kid.background)
            .padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Talk & Learn",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Kid.ink,
                modifier = Modifier.weight(1f),
            )
            StarCount(app.progress.stars)
            Spacer(Modifier.width(12.dp))
            // Grown-ups only: press and hold so little fingers don't wander in.
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0x22000000))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { app.speaker.say("Ask a grown-up to press and hold.") },
                            onLongPress = { go(Screen.GrownUps) },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) { Text("⚙️", fontSize = 26.sp) }
        }
        Spacer(Modifier.size(12.dp))
        MenuGrid(
            listOf(
                MenuItem("🗣️", "Talk", "Let's talk!", Kid.talk, Screen.TalkMenu),
                MenuItem("🔤", "Letters", "Letters!", Kid.letters, Screen.LettersMenu),
                MenuItem("📖", "Read", "Let's read!", Kid.reading, Screen.ReadMenu),
                MenuItem("🔢", "Numbers", "Numbers!", Kid.numbers, Screen.NumbersMenu),
                MenuItem("🧩", "Patterns", "Patterns and puzzles!", Kid.patterns, Screen.PatternsMenu),
                MenuItem("🤖", "Robot", "Robot! Tell the robot where to go.", Kid.robot, Screen.RobotMenu),
            ),
            go,
        )
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

@Composable
fun LettersMenu(go: (Screen) -> Unit, back: () -> Unit) {
    KidScreen("Letters 🔤", Kid.letters, back) {
        MenuGrid(
            listOf(
                MenuItem("🔠", "ABC", "The alphabet!", Kid.letters, Screen.Alphabet),
                MenuItem("🅰️", "Big → small", "Find the little letter!", Kid.letters, Screen.LetterGame(LetterMode.BIG_TO_SMALL)),
                MenuItem("🔡", "small → Big", "Find the big letter!", Kid.letters, Screen.LetterGame(LetterMode.SMALL_TO_BIG)),
                MenuItem("👂", "First sound", "What sound does it start with?", Kid.letters, Screen.LetterGame(LetterMode.FIRST_SOUND)),
            ),
            go,
            columns = 2,
        )
    }
}

@Composable
fun ReadMenu(go: (Screen) -> Unit, back: () -> Unit) {
    KidScreen("Read 📖", Kid.reading, back) {
        MenuGrid(
            listOf(
                MenuItem("🧱", "Sound it out", "Let's sound out words!", Kid.reading, Screen.SoundItOut),
                MenuItem("🖼️", "Which word?", "Which word matches the picture?", Kid.reading, Screen.WhichWord),
            ),
            go,
            columns = 2,
        )
    }
}

@Composable
fun NumbersMenu(go: (Screen) -> Unit, back: () -> Unit) {
    KidScreen("Numbers 🔢", Kid.numbers, back) {
        MenuGrid(
            listOf(
                MenuItem("🔟", "1 2 3", "Let's learn numbers!", Kid.numbers, Screen.NumberLine),
                MenuItem("🍎", "Count", "Let's count!", Kid.numbers, Screen.Counting),
                MenuItem("⚖️", "More?", "Which has more?", Kid.numbers, Screen.MoreOrLess),
                MenuItem("➕", "Add", "Let's add!", Kid.numbers, Screen.MathGame(plus = true)),
                MenuItem("➖", "Take away", "Let's take away!", Kid.numbers, Screen.MathGame(plus = false)),
            ),
            go,
        )
    }
}

@Composable
fun PatternsMenu(go: (Screen) -> Unit, back: () -> Unit) {
    KidScreen("Patterns 🧩", Kid.patterns, back) {
        MenuGrid(
            listOf(
                MenuItem("🔴🔵", "What's next?", "What comes next?", Kid.patterns, Screen.WhatsNext),
                MenuItem("🔍", "Odd one out", "Which one is different?", Kid.patterns, Screen.OddOneOutGame),
            ),
            go,
            columns = 2,
        )
    }
}

@Composable
fun RobotMenu(go: (Screen) -> Unit, back: () -> Unit) {
    KidScreen("Robot 🤖", Kid.robot, back) {
        MenuGrid(
            listOf(
                MenuItem("⬆️⬇️", "Arrows", "Use arrows to move the robot!", Kid.robot, Screen.RobotGame(relative = false)),
                MenuItem("↩️↪️", "Turn & go", "Turn the robot and go forward!", Kid.robot, Screen.RobotGame(relative = true)),
            ),
            go,
            columns = 2,
        )
    }
}
