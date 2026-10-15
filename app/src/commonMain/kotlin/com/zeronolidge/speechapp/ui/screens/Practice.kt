package com.zeronolidge.speechapp.ui.screens

import com.zeronolidge.speechapp.ui.art.paperCard
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.key
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
import com.zeronolidge.speechapp.logic.Land
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
import com.zeronolidge.speechapp.ui.components.ChoiceTile
import com.zeronolidge.speechapp.ui.components.PaperArrowButton
import com.zeronolidge.speechapp.ui.components.PagedChoices
import com.zeronolidge.speechapp.ui.components.PaperButton
import com.zeronolidge.speechapp.ui.components.PaperChip
import com.zeronolidge.speechapp.ui.components.PaperColorsUi
import com.zeronolidge.speechapp.ui.components.RoundIcon
import com.zeronolidge.speechapp.ui.components.popIn
import com.zeronolidge.speechapp.ui.art.PaperStyle
import com.zeronolidge.speechapp.ui.Nav
import com.zeronolidge.speechapp.logic.Coins
import com.zeronolidge.speechapp.state.today
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.width
import com.zeronolidge.speechapp.ui.components.KidScreen
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.theme.Kid
import kotlin.random.Random

/** Free-play games, outside the story. Each one is an endless loop of one kind of challenge. */
enum class PracticeKind(val emoji: String, val label: String, val spoken: String, val color: Color) {
    BIG_SMALL("🅰️", "A to a", "Find the little letter!", Kid.letters),
    SMALL_BIG("🔡", "a to A", "Find the big letter!", Kid.letters),
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
    COINS("💰", "Market", "Let's pay with coins at the market!", Kid.numbers),
}

@Composable
fun PracticeMenu(nav: Nav) {
    val app = LocalApp.current
    data class Tile(val emoji: String, val label: String, val spoken: String, val color: Color, val target: Screen)
    val tiles = listOf(
        Tile("🗣️", "Talk", "Pick a sound to practice", Kid.talk, Screen.TalkMenu),
        Tile("🔠", "ABC", "The alphabet!", Kid.letters, Screen.Alphabet),
        Tile("🔟", "1 2 3", "Let's learn numbers!", Kid.numbers, Screen.NumberLine),
    ) + PracticeKind.entries.map { Tile(it.emoji, it.label, it.spoken, it.color, Screen.PracticeGame(it)) }
    // A short set of three games that changes every day; 🔄 swaps in three others.
    var start by remember { mutableIntStateOf((today() * 3).mod(tiles.size)) }
    KidScreen("🎯", nav.home, "Practice games! Tap a game to play. Tap the round arrows for different games.") {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // Three games on a tablet; two on a narrow phone so the tiles and their words stay big.
            val count = if (maxWidth < 520.dp) 2 else 3
            val shown = (0 until count).map { tiles[(start + it) % tiles.size] }
            Row(
                Modifier.fillMaxSize().padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(if (count == 2) 14.dp else 24.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                shown.forEachIndexed { i, t ->
                    Box(Modifier.weight(1f).popIn(start to i, i * 60), contentAlignment = Alignment.Center) {
                        ChoiceTile(
                            t.emoji, t.label, t.color,
                            Modifier.fillMaxWidth().widthIn(max = 240.dp).heightIn(min = 120.dp, max = 340.dp).fillMaxHeight(),
                            onClick = {
                                app.speaker.say(t.spoken)
                                nav.go(t.target)
                            },
                        )
                    }
                }
                RoundIcon("🔄", PaperColorsUi.choice, size = 72.dp) {
                    start = (start + count) % tiles.size
                    app.speaker.say("New games!")
                }
            }
        }
    }
}

/** The games that belong to each area of the map. */
fun gamesFor(land: Land): List<PracticeKind> = when (land) {
    Land.FOREST -> emptyList() // talking: opens the sounds menu
    Land.MEADOW -> listOf(PracticeKind.COUNT, PracticeKind.ADD, PracticeKind.TAKE_AWAY, PracticeKind.MORE)
    Land.LAGOON -> listOf(PracticeKind.BIG_SMALL, PracticeKind.SMALL_BIG, PracticeKind.FIRST_SOUND)
    Land.MARKET -> listOf(PracticeKind.COINS, PracticeKind.COUNT)
    Land.PEAKS -> listOf(PracticeKind.NEXT, PracticeKind.ODD)
    Land.FACTORY -> listOf(PracticeKind.ROBOT_ARROWS, PracticeKind.ROBOT_TURNS)
    Land.CAVES -> listOf(PracticeKind.SOUND_OUT, PracticeKind.WHICH_WORD)
    Land.CLOUDS -> PracticeKind.entries.toList()
}

/**
 * An area's own games, opened by tapping its label or picture on the map: Number Meadow has the
 * counting and adding games, Letter Lagoon the letter games, and so on. Big picture tiles, a few
 * at a time; the talking forest opens the sounds.
 */
@Composable
fun LandGamesScreen(land: Land, nav: Nav) {
    val app = LocalApp.current
    if (land == Land.FOREST) {
        TalkMenu(nav)
        return
    }
    val games = gamesFor(land)
    KidScreen("${land.emoji} ${land.shortName}", nav.home, "${land.title}! Pick a game. Tap a picture to play.") {
        PagedChoices(games, perPage = if (games.size == 4) 4 else 3, modifier = Modifier.fillMaxSize().padding(vertical = 24.dp)) { kind, m ->
            ChoiceTile(kind.emoji, kind.label, kind.color, m, onClick = {
                app.speaker.say(kind.spoken)
                nav.go(Screen.PracticeGame(kind))
            })
        }
    }
}

@Composable
fun PracticeGameScreen(kind: PracticeKind, nav: Nav) {
    val app = LocalApp.current
    val p = app.progress
    val rnd = remember { Random(Random.nextLong()) }
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
            PracticeKind.COINS -> Challenge.Coins(Coins.make(p.chapter, rnd))
        }
    }
    KidScreen(kind.emoji, nav.home, kind.spoken, onBack = nav.back) {
        // key: a new round always starts fresh, even if it happens to look like the last one.
        key(roundNo) { ChallengeView(challenge, Modifier.fillMaxSize()) { firstTry, _ ->
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
        } }
    }
}

@Composable
fun TalkMenu(nav: Nav) {
    val app = LocalApp.current
    KidScreen("🗣️", nav.home, "Pick a sound to practice. Tap the arrow to see more sounds.", onBack = nav.back) {
        PagedChoices(SpeechContent.sounds, perPage = 3, modifier = Modifier.fillMaxSize().padding(vertical = 24.dp)) { sound, m ->
            ChoiceTile(sound.emoji, sound.label, Kid.talk, m, onClick = {
                app.speaker.say(sound.modelSound)
                nav.go(Screen.TalkPractice(sound))
            })
        }
    }
}

/** Says a letter's name, its sound and a word that starts with it. */
fun letterIntro(l: LetterInfo) = "${l.upper}. ${l.upper} says ${l.sound}. ${l.sound}, ${l.word.text}."

/** One big letter at a time, with big arrows to go back and forward. Tap the letter to hear it again. */
@Composable
fun AlphabetScreen(nav: Nav) {
    val app = LocalApp.current
    val letters = LetterContent.letters
    var i by remember { mutableIntStateOf(0) }
    val l = letters[i]
    KidScreen("🔠", nav.home, "Tap the arrows to see the letters. Tap the letter to hear it.", onBack = nav.back) {
        Explorer(
            onPrev = if (i > 0) ({ i--; app.speaker.say(letterIntro(letters[i])) }) else null,
            onNext = { i = (i + 1) % letters.size; app.speaker.say(letterIntro(letters[i])) },
            onTap = { app.speaker.say(letterIntro(l)) },
            key = i,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${l.upper}", fontSize = 130.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E88E5))
                Text("${l.lower}", fontSize = 110.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE53935))
            }
            Text(l.word.emoji, fontSize = 80.sp)
            PaperChip(l.word.text, fontSize = 32.sp)
        }
    }
}

/** One big number at a time, with that many dots. Big arrows go back and forward. */
@Composable
fun NumberLineScreen(nav: Nav) {
    val app = LocalApp.current
    var n by remember { mutableIntStateOf(1) }
    KidScreen("🔟", nav.home, "Tap the arrows to count up and down. Tap the number to hear it.", onBack = nav.back) {
        Explorer(
            onPrev = if (n > 1) ({ n--; app.speaker.say(Numbers.word(n)) }) else null,
            onNext = { n = if (n >= 20) 1 else n + 1; app.speaker.say(Numbers.word(n)) },
            onTap = { app.speaker.say(Numbers.word(n)) },
            key = n,
        ) {
            Text("$n", fontSize = 140.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF57C00))
            EmojiGroup("🔵", n, Modifier.widthIn(max = 420.dp), size = 30)
        }
    }
}

/** A big paper card in the middle with ◀ and ▶ on either side: three things to tap, nothing more. */
@Composable
private fun Explorer(
    onPrev: (() -> Unit)?,
    onNext: () -> Unit,
    onTap: () -> Unit,
    key: Any,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        Box(Modifier.width(96.dp), contentAlignment = Alignment.Center) {
            if (onPrev != null) PaperArrowButton(PaperColorsUi.choice, size = 96.dp, pointLeft = true, onClick = onPrev)
        }
        PaperButton(PaperStyle.chip, Modifier.weight(1f).fillMaxHeight().padding(vertical = 12.dp).popIn(key), onClick = onTap) {
            Column(
                Modifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
                content = content,
            )
        }
        PaperArrowButton(PaperColorsUi.choice, size = 96.dp, onClick = onNext)
    }
}
