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
import com.zeronolidge.speechapp.logic.ReviewQueue
import com.zeronolidge.speechapp.logic.AnswerKey
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
import com.zeronolidge.speechapp.ui.theme.LetterFont
import kotlin.random.Random
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.drawBehind
import com.zeronolidge.speechapp.ui.art.paperRect
import com.zeronolidge.speechapp.ui.components.EmojiPic
import com.zeronolidge.speechapp.ui.components.fittingFontSize

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

/**
 * Every practice game at once (test48): Talk, ABC, 1 2 3 and the 14 games, as big picture tiles on
 * one screen. Phones get 3 columns with tiles as large as the width and height allow (never under
 * 100dp; only then does the grid scroll); tablets get ~120dp tiles in as many columns as fit, so
 * the whole set is one screen. No daily shuffle and no "new games" button any more.
 */
@Composable
fun PracticeMenu(nav: Nav) {
    val app = LocalApp.current
    data class Tile(val emoji: String, val label: String, val spoken: String, val color: Color, val target: Screen, val framed: Boolean = false)
    val tiles = listOf(
        Tile("🗣️", "Talk", "Pick a sound to practice", Kid.talk, Screen.TalkMenu),
        Tile("🔠", "ABC", "The alphabet!", Kid.letters, Screen.Alphabet),
        Tile("🔟", "1 2 3", "Let's learn numbers!", Kid.numbers, Screen.NumberLine),
    ) + PracticeKind.entries.map { Tile(it.emoji, it.label, it.spoken, it.color, Screen.PracticeGame(it), framed = it == PracticeKind.WHICH_WORD) }
    KidScreen("🎯 Practice", nav.home, "Practice games! Tap a picture to play.") {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = maxWidth
            val h = maxHeight
            val gap = 8.dp
            val phone = w < 600.dp
            val cols = if (phone) 3 else practiceColumns(w.value, h.value, tiles.size, gap.value)
            val rows = (tiles.size + cols - 1) / cols
            val byWidth = (w - gap * (cols - 1) - 8.dp) / cols
            val byHeight = (h - gap * (rows - 1) - 8.dp) / rows
            val fit = minOf(byWidth, byHeight)
            val side = if (phone) maxOf(fit, 100.dp) else fit.coerceIn(100.dp, 132.dp)
            val scroll = side * rows + gap * (rows - 1) > h
            Column(
                Modifier.fillMaxSize().then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                verticalArrangement = Arrangement.spacedBy(gap, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                tiles.chunked(cols).forEachIndexed { r, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally)) {
                        row.forEachIndexed { c, t ->
                            val i = r * cols + c
                            Box(Modifier.size(side).popIn(i, (i * 25).coerceAtMost(400)), contentAlignment = Alignment.Center) {
                                PracticeGridTile(t.emoji, t.label, t.color, t.framed, side) {
                                    app.speaker.say(t.spoken)
                                    nav.go(t.target)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Tablet columns: the column count that gives the biggest tiles (up to ~130dp) with every row
 * on the screen; on a tie, fewer columns (bigger rows, less sideways reach). */
internal fun practiceColumns(w: Float, h: Float, n: Int, gap: Float): Int =
    (3..9).maxByOrNull { cols ->
        val rows = (n + cols - 1) / cols
        val side = minOf((w - gap * (cols - 1)) / cols, (h - gap * (rows - 1)) / rows)
        minOf(side, 132f) * 100f - cols
    } ?: 6

/** One picture tile in the practice grid: the picture on its cream circle, the word under it. */
@Composable
private fun PracticeGridTile(emoji: String, label: String, color: Color, framed: Boolean, side: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    PaperButton(color, Modifier.fillMaxSize(), onClick = onClick) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            val pic = (side.value * 0.36f).coerceIn(26f, 56f).sp
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (framed) FramedPicture(side * 0.5f) else EmojiPic(emoji, pic)
            }
            Text(
                label,
                fontSize = fittingFontSize(label, side - 12.dp, max = (side.value * 0.15f).coerceIn(13f, 20f)),
                fontWeight = FontWeight.ExtraBold,
                color = PaperStyle.ink,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2,
                lineHeight = 15.sp,
            )
        }
    }
}

/**
 * "Which word?" picture: a little wooden picture frame with an apple in it, drawn in paper (the
 * 🖼️ emoji showed as an empty frame and didn't read as a game).
 */
@Composable
private fun FramedPicture(side: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier.size(side).drawBehind {
            val s = size.minDimension
            val o = androidx.compose.ui.geometry.Offset((size.width - s) / 2, (size.height - s) / 2)
            paperRect(Color(0xFF8D5A35), o, androidx.compose.ui.geometry.Size(s, s))
            val inset = s * 0.14f
            paperRect(Color(0xFFFFF6E0), o + androidx.compose.ui.geometry.Offset(inset, inset), androidx.compose.ui.geometry.Size(s - 2 * inset, s - 2 * inset))
        },
        contentAlignment = Alignment.Center,
    ) { Text("🍎", fontSize = (side.value * 0.42f).sp) }
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
data class LandTile(val emoji: String, val label: String, val spoken: String, val color: Color, val target: Screen)

/** What an area of the map opens: the talking forest has its story, Pip and talking; every other area its own games. */
fun landTiles(land: Land): List<LandTile> = if (land == Land.FOREST) {
    listOf(
        LandTile("📖", "Story", "A forest story!", Kid.talk, Screen.Chapter(Land.FOREST)),
        LandTile("🐶", "Find Pip", "Let's find Pip the puppy!", Kid.talk, Screen.Pip),
        LandTile("🗣️", "Talk", "Let's practise talking!", Kid.talk, Screen.TalkMenu),
    )
} else {
    gamesFor(land).map { LandTile(it.emoji, it.label, it.spoken, it.color, Screen.PracticeGame(it)) }
}

@Composable
fun LandGamesScreen(land: Land, nav: Nav) {
    val app = LocalApp.current
    val tiles = landTiles(land)
    KidScreen("${land.emoji} ${land.shortName}", nav.home, "${land.title}! Pick a game. Tap a picture to play.") {
        PagedChoices(tiles, perPage = if (tiles.size == 4) 4 else 3, modifier = Modifier.fillMaxSize().padding(vertical = 24.dp)) { t, m ->
            ChoiceTile(t.emoji, t.label, t.color, m, onClick = {
                app.speaker.say(t.spoken)
                nav.go(t.target)
            })
        }
    }
}

/** Test-link hook: "#test=WHICH_WORD&end=6" wraps the game up after 6 rounds (to show the end check). */
object ReviewTest { var endAfter = 0 }

@Composable
fun PracticeGameScreen(kind: PracticeKind, nav: Nav) {
    val app = LocalApp.current
    val p = app.progress
    val rnd = remember { Random(Random.nextLong()) }
    var roundNo by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var last by remember { mutableStateOf<String?>(null) }
    // test50: missed questions come back (logic/ReviewQueue.kt): once with 2 choices 2-3 rounds
    // later, then (if right) once more near the end, at the usual number of choices.
    val reviews = remember(kind) { ReviewQueue(rnd, ReviewQueue.droppedToday(com.zeronolidge.speechapp.state.today().toLong())) }
    var ending by remember { mutableStateOf(false) }
    // The repeat this round is (set as the round is made, read by the same composition below).
    val reviewNow = remember { arrayOfNulls<ReviewQueue.Review>(1) }
    val challenge = remember(roundNo) {
        reviewNow[0] = reviews.next(roundNo, ending)
        reviewNow[0]?.let { rv ->
            println("REVIEW round=$roundNo stage=${rv.stage} key=${rv.key} choices=${AnswerKey.of(rv.challenge)?.options?.size}")
            return@remember rv.challenge
        }
        val tricky = p.areaStars(Progress.AREA_LETTERS) > 15
        when (kind) {
            // Every letter game asks mostly for the letters of his target sounds and own words.
            PracticeKind.BIG_SMALL, PracticeKind.SMALL_BIG, PracticeKind.FIRST_SOUND -> {
                val mode = when (kind) {
                    PracticeKind.BIG_SMALL -> LetterMode.BIG_TO_SMALL
                    PracticeKind.SMALL_BIG -> LetterMode.SMALL_TO_BIG
                    else -> LetterMode.FIRST_SOUND
                }
                val mine = Letters.targetsFor(p.focusSounds, p.customWords.map { it.first })
                Challenge.Letter(Letters.make(mode, rnd, tricky, Letters.pickTarget(mine, rnd)))
            }
            PracticeKind.SOUND_OUT -> Challenge.SoundOut(Reading.make(ReadingContent.cvcWords, rnd, ReadingContent.cvcWords.firstOrNull { it.text == last }))
            PracticeKind.WHICH_WORD -> Challenge.WhichWord(Reading.make(ReadingContent.cvcWords, rnd, ReadingContent.cvcWords.firstOrNull { it.text == last }))
            // Counting and adding follow the quiet numbers step: count to 5, to 10, then add one.
            PracticeKind.COUNT -> Challenge.Count(Numbers.count(if (p.numbersStep >= 1) 10 else 5, rnd))
            PracticeKind.MORE -> Challenge.More(Numbers.compare(8, rnd))
            PracticeKind.ADD -> Challenge.Math(if (p.numbersStep >= 2) Numbers.addOne(rnd) else Numbers.math(true, 5, rnd))
            PracticeKind.TAKE_AWAY -> Challenge.Math(Numbers.math(false, if (p.numbersStep >= 1) 10 else 5, rnd))
            PracticeKind.NEXT -> Challenge.Pattern(Patterns.makeStep(p.patternStep, rnd))
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
        key(roundNo) { ChallengeView(challenge, Modifier.fillMaxSize(), sayTwice = reviewNow[0]?.sayTwice == true) { firstTry, revealed ->
            val missed = !firstTry || revealed
            val thisReview = reviewNow[0]
            reviews.result(roundNo, challenge, missed, thisReview)
            println("QUIZ round=$roundNo missed=$missed review=${thisReview?.stage} waiting=${reviews.waiting.map { "${it.stage}@${it.dueRound}" }}")
            // Numbers and patterns quietly step up or back (right first time counts as right).
            // The easier 2-choice retry doesn't move the difficulty steps; the end check does.
            if (thisReview?.stage != ReviewQueue.Stage.RETRY) p.quizResult(challenge, firstTry && !revealed)
            when (kind) {
                PracticeKind.ROBOT_ARROWS -> p.setLevel("robot_easy", p.level("robot_easy") + 1)
                PracticeKind.ROBOT_TURNS -> p.setLevel("robot_turn", p.level("robot_turn") + 1)
                else -> {}
            }
            last = (challenge as? Challenge.SoundOut)?.round?.word?.text ?: (challenge as? Challenge.WhichWord)?.round?.word?.text
            // After about 12 minutes of play, Rory wraps up here, between rounds -- but an end
            // check that is waiting plays first, as the game's last question.
            val long = app.sessionLong() || (ReviewTest.endAfter in 1..roundNo + 1)
            when {
                long && !ending && reviews.hasRecheck() -> { ending = true; roundNo++ }
                long -> nav.go(Screen.Rest)
                else -> roundNo++
            }
        } }
    }
}

@Composable
fun TalkMenu(nav: Nav) {
    val app = LocalApp.current
    KidScreen("🗣️", nav.home, "Pick a picture to practice its sound. Tap the arrow to see more sounds.", onBack = nav.back) {
        // Picture tiles only (he can't read yet): f, sh, v, ch, sp, sk, sm, then the rest.
        // test48: four big square picture tiles a page (a 2x2 block on a phone, a row on a
        // tablet), with the big arrows for more, so 16 sounds never shrink the tiles.
        val sounds = SpeechContent.picker
        val per = 4
        var page by remember { mutableIntStateOf(0) }
        val pages = (sounds.size + per - 1) / per
        val shown = sounds.drop(page * per).take(per)
        fun go(to: Int, line: String) { page = to; app.speaker.say(line) }
        @Composable
        fun tile(sound: com.zeronolidge.speechapp.data.SpeechSound, i: Int, side: androidx.compose.ui.unit.Dp) {
            Box(Modifier.size(side).popIn(page to i, i * 60), contentAlignment = Alignment.Center) {
                PaperButton(Kid.talk, Modifier.fillMaxSize(), onClick = {
                    app.speaker.say(sound.modelSound)
                    nav.go(Screen.TalkPractice(sound))
                }) { EmojiPic(sound.emoji, (side.value * 0.4f).coerceIn(36f, 96f).sp) }
            }
        }
        BoxWithConstraints(Modifier.fillMaxSize().padding(vertical = 12.dp)) {
            val w = maxWidth
            val h = maxHeight
            val arrow = 72.dp
            val gap = 14.dp
            if (w < 520.dp) {
                val side = minOf((w - gap) / 2, (h - arrow - gap * 3) / 2, 200.dp)
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(gap, Alignment.CenterVertically)) {
                    shown.chunked(2).forEachIndexed { r, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            row.forEachIndexed { c, snd -> tile(snd, r * 2 + c, side) }
                            if (row.size < 2) androidx.compose.foundation.layout.Spacer(Modifier.size(side))
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(arrow)) { if (page > 0) PageChevron(arrow, left = true) { go(page - 1, "Back!") } }
                        PageDots(page, pages)
                        Box(Modifier.size(arrow)) { if (page < pages - 1) PageChevron(arrow) { go(page + 1, "More!") } }
                    }
                }
            } else {
                val side = minOf((w - arrow * 2 - gap * 5) / per, h * 0.8f, 220.dp)
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(gap * 2, Alignment.CenterVertically)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally)) {
                    Box(Modifier.size(arrow)) { if (page > 0) PageChevron(arrow, left = true) { go(page - 1, "Back!") } }
                    shown.forEachIndexed { i, snd -> tile(snd, i, side) }
                    repeat(per - shown.size) { androidx.compose.foundation.layout.Spacer(Modifier.size(side)) }
                    Box(Modifier.size(arrow)) { if (page < pages - 1) PageChevron(arrow) { go(page + 1, "More!") } }
                }
                PageDots(page, pages)
                }
            }
        }
    }
}

/**
 * test49: the Talk picker's page turn: a thin chevron on a neutral grey paper square, so it never
 * reads as the round green ▶ "go" button.
 */
@Composable
private fun PageChevron(size: androidx.compose.ui.unit.Dp, left: Boolean = false, onClick: () -> Unit) {
    PaperButton(Color(0xFFDDE3E8), Modifier.size(size), onClick = onClick) {
        androidx.compose.foundation.Canvas(Modifier.size(size * 0.46f)) {
            val w = this.size.width
            val hh = this.size.height
            val x0 = if (left) w * 0.68f else w * 0.32f
            val x1 = if (left) w * 0.30f else w * 0.70f
            val p = androidx.compose.ui.graphics.Path().apply { moveTo(x0, hh * 0.12f); lineTo(x1, hh * 0.5f); lineTo(x0, hh * 0.88f) }
            drawPath(p, Color(0xFF546E7A), style = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.16f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
    }
}

/** Little page dots: the page he's on is filled. */
@Composable
private fun PageDots(page: Int, pages: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(pages) { i ->
            androidx.compose.foundation.Canvas(Modifier.size(if (i == page) 16.dp else 12.dp)) {
                drawCircle(if (i == page) Color(0xFF546E7A) else Color(0xFFB0BEC5))
            }
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
                Text("${l.upper}", fontSize = 130.sp, fontWeight = FontWeight.ExtraBold, fontFamily = LetterFont, color = Color(0xFF1E88E5))
                Text("${l.lower}", fontSize = 110.sp, fontWeight = FontWeight.ExtraBold, fontFamily = LetterFont, color = Color(0xFFE53935))
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
