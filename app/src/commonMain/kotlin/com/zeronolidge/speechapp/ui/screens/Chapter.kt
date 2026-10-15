package com.zeronolidge.speechapp.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.logic.Challenge
import com.zeronolidge.speechapp.logic.Cosmetic
import com.zeronolidge.speechapp.logic.Feeling
import com.zeronolidge.speechapp.logic.Friend
import com.zeronolidge.speechapp.logic.Patterns
import com.zeronolidge.speechapp.logic.Power
import com.zeronolidge.speechapp.logic.Quest
import com.zeronolidge.speechapp.logic.Scene
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.logic.StoryGen
import com.zeronolidge.speechapp.logic.Trail
import com.zeronolidge.speechapp.logic.WordStat
import com.zeronolidge.speechapp.logic.emoji
import com.zeronolidge.speechapp.data.Word
import com.zeronolidge.speechapp.ui.Nav
import com.zeronolidge.speechapp.ui.art.Critter
import com.zeronolidge.speechapp.ui.art.HeroWithPet
import com.zeronolidge.speechapp.ui.art.Mood
import com.zeronolidge.speechapp.ui.art.Outfit
import com.zeronolidge.speechapp.ui.art.PaperStyle
import com.zeronolidge.speechapp.ui.art.Scenery
import com.zeronolidge.speechapp.ui.art.SetPieceView
import com.zeronolidge.speechapp.ui.art.paperCard
import com.zeronolidge.speechapp.ui.challenge.ChallengeView
import com.zeronolidge.speechapp.ui.challenge.PowerTicks
import com.zeronolidge.speechapp.ui.components.AfterDelay
import com.zeronolidge.speechapp.ui.components.BackpackMeter
import com.zeronolidge.speechapp.ui.components.GoButton
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.NameTag
import com.zeronolidge.speechapp.ui.components.Nudge
import com.zeronolidge.speechapp.ui.components.PaperChip
import com.zeronolidge.speechapp.ui.components.PaperIconButton
import com.zeronolidge.speechapp.ui.components.ScreenFrame
import com.zeronolidge.speechapp.ui.components.SpeechBubble
import com.zeronolidge.speechapp.ui.components.popIn
import kotlin.random.Random

private sealed interface Phase {
    data object Intro : Phase
    data class Beat(val index: Int) : Phase
    /** A story moment without a challenge, shown just before beat [before]. */
    data class Twist(val before: Int) : Phase
    data object Ending : Phase
    data object Goodbye : Phase
}

/**
 * One chapter of the trail, about 3 to 5 minutes: meet a friend, get past a few obstacles
 * (each one a little challenge), a twist, a happy ending, and "see you next time" with a new
 * paper piece for the buddy and the stickers earned on the way.
 */
@Composable
fun ChapterScreen(nav: Nav) {
    val app = LocalApp.current
    val p = app.progress
    val chapter = remember { p.chapter }
    val land = remember { Trail.landOf(chapter) }
    val quest = remember { StoryGen.make(land, p.storyParams(), Random(Random.nextLong()), mixed = false) }
    var phase by remember { mutableStateOf<Phase>(Phase.Intro) }
    var cheer by remember { mutableStateOf(false) }
    var patternWins by remember { mutableStateOf(0) }
    val piecesBefore = remember { p.pieces.map { it.id }.toSet() }
    var woken by remember { mutableStateOf(false) }
    val stickersBefore = remember { p.words.words.keys }
    val species = p.hero ?: Species.KID

    fun next() {
        cheer = false
        phase = when (val ph = phase) {
            Phase.Intro -> Phase.Beat(0)
            is Phase.Beat -> when {
                ph.index + 1 >= quest.beats.size -> Phase.Ending
                ph.index + 1 == quest.twistBefore -> Phase.Twist(ph.index + 1)
                else -> Phase.Beat(ph.index + 1)
            }
            is Phase.Twist -> Phase.Beat(ph.before)
            Phase.Ending -> Phase.Goodbye
            Phase.Goodbye -> Phase.Goodbye
        }
    }

    fun solved(c: Challenge, firstTry: Boolean) {
        when (c) {
            is Challenge.Robot -> p.setLevel("story_robot", p.level("story_robot") + 1)
            is Challenge.Pattern -> if (firstTry) {
                patternWins++
                if (patternWins % 3 == 0) p.setLevel("patterns", (p.level("patterns") + 1).coerceAtMost(Patterns.maxLevel()))
            }
            else -> {}
        }
        cheer = true
    }

    ScreenFrame(
        onHome = nav.home,
        background = { Scenery(quest.land, Modifier.fillMaxSize(), seed = (phase as? Phase.Beat)?.index ?: 9) },
        top = { Footprints(quest, phase) },
    ) {
        when (val ph = phase) {
            Phase.Intro -> StoryScene(quest, quest.intro, species, Mood.HAPPY, onNext = ::next)
            is Phase.Twist -> if (!woken) {
                WakeScene(quest, species) { woken = true }
            } else {
                StoryScene(quest, quest.twist, species, Mood.HAPPY, visitor = quest.visitor, onNext = ::next)
            }
            is Phase.Beat -> BeatScene(quest, ph.index, species, cheer, onSolved = { c, ft -> solved(c, ft) }, onNext = ::next)
            Phase.Ending -> {
                LaunchedEffect(Unit) {
                    if (p.chapter == chapter) p.finishChapter()
                }
                StoryScene(quest, quest.ending, species, Mood.CHEER, solved = true, onNext = ::next)
            }
            Phase.Goodbye -> {
                val earned = p.words.stickers.filter { it.text !in stickersBefore }
                Goodbye(p.pieces.lastOrNull { it.id !in piecesBefore }, earned, onMap = nav.home)
            }
        }
    }
}

/** Footprints across the top: one per obstacle, ending at the goal. */
@Composable
private fun RowScope.Footprints(quest: Quest, phase: Phase) {
    val current = when (phase) {
        Phase.Intro -> -1
        is Phase.Beat -> phase.index
        is Phase.Twist -> phase.before
        else -> quest.beats.size
    }
    // The place this chapter happens, for grown-ups (the child follows the pictures).
    PaperChip("${quest.land.emoji} ${quest.land.title}", fontSize = 18.sp)
    Spacer(Modifier.weight(1f))
    Row(
        Modifier.paperCard(PaperStyle.chip, 999.dp).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        quest.beats.indices.forEach { i ->
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            i < current -> Color(0xFF5BBF4A)
                            i == current -> Color(0xFFFFD93D)
                            else -> Color(0xFFECE3D0)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) { Text(if (i < current) "✔️" else "👣", fontSize = 17.sp, color = Color.White) }
        }
        Text("▶", fontSize = 18.sp, modifier = Modifier.padding(horizontal = 6.dp))
        Text(quest.goal, fontSize = 28.sp, modifier = Modifier.alpha(if (current >= quest.beats.size) 1f else 0.5f))
    }
}

/** How a friend's feelings show on their face. */
private fun Feeling.mood() = when (this) {
    Feeling.HAPPY -> Mood.HAPPY
    Feeling.SAD -> Mood.SAD
    Feeling.WORRIED -> Mood.THINK
    Feeling.CHEER -> Mood.CHEER
}

/** The buddy strolls in from the left whenever the scene changes. */
@Composable
private fun walkIn(key: Any): Float {
    val walk = remember(key) { Animatable(-1f) }
    LaunchedEffect(key) { walk.animateTo(0f, tween(900)) }
    return walk.value
}

/**
 * A full-width story moment: the picture for the scene, the buddy and the friend acting it out,
 * and a big green arrow to carry on. The words are spoken; the bubble is for grown-ups.
 */
@Composable
private fun StoryScene(
    quest: Quest,
    scene: Scene,
    hero: Species,
    heroMood: Mood,
    visitor: Friend? = null,
    solved: Boolean = false,
    onNext: () -> Unit,
) {
    val app = LocalApp.current
    val p = app.progress
    val text = scene.text
    val goLine = "Tap the big green arrow!"
    LaunchedEffect(text) {
        app.instruct(text)
        app.speaker.then(goLine)
    }
    Nudge(text, goLine)
    val walk = walkIn(scene)
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        SpeechBubble(text, Modifier.fillMaxWidth(0.8f), fontSize = 20) { app.speaker.say(text) }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            SetPieceView(scene.piece, scene.prop, solved, Modifier.fillMaxSize())
            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                HeroWithPet(
                    hero, p.heroColor(hero), p.outfit, p.pet,
                    Modifier
                        .fillMaxHeight(0.62f)
                        .width(150.dp)
                        .graphicsLayer { translationX = walk * size.width * 2f },
                    heroMood,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxHeight(0.58f)) {
                    Critter(quest.friend.species, Modifier.weight(1f), Color(quest.friend.color), Outfit(), scene.feeling.mood(), mirrored = true)
                    NameTag(quest.friend.name)
                }
                if (visitor != null) {
                    Critter(visitor.species, Modifier.fillMaxHeight(0.5f).popIn(visitor), Color(visitor.color), Outfit(), Mood.HAPPY, mirrored = true)
                }
                // The only way forward, so make it impossible to miss.
                GoButton(Modifier.align(Alignment.CenterVertically), size = 104.dp, onClick = onNext)
            }
        }
    }
}

/** One step of the story: characters on the left, the challenge on the right. */
@Composable
private fun BeatScene(
    quest: Quest,
    index: Int,
    hero: Species,
    cheer: Boolean,
    onSolved: (Challenge, Boolean) -> Unit,
    onNext: () -> Unit,
) {
    val app = LocalApp.current
    val p = app.progress
    val beat = quest.beats[index]
    val powers = remember(index) { PowerTicks() }
    var used by remember(index) { mutableStateOf(setOf<Power>()) }
    var solvedHere by remember(index) { mutableStateOf(false) }
    val text = beat.narration
    // Pause on the solved picture (stones appear, the door opens) before moving on.
    if (solvedHere) AfterDelay(index, 1800) { onNext() }
    val walk = walkIn(index)

    StorySplit(story = { m ->
        Column(m) {
            SpeechBubble(text, Modifier.fillMaxWidth(), fontSize = 15) { app.speaker.say(text) }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                SetPieceView(beat.scene.piece, beat.scene.prop, solvedHere, Modifier.fillMaxSize())
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.Bottom) {
                    HeroWithPet(
                        hero, p.heroColor(hero), p.outfit, p.pet,
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(0.55f)
                            .graphicsLayer { translationX = walk * size.width * 2f },
                        if (cheer) Mood.CHEER else Mood.HAPPY,
                    )
                    Critter(
                        quest.friend.species, Modifier.weight(0.8f).fillMaxHeight(0.45f),
                        Color(quest.friend.color), Outfit(),
                        if (cheer) Mood.CHEER else beat.scene.feeling.mood(), mirrored = true,
                    )
                }
            }
            HelperPower(beat.challenge, used) { power ->
                used = used + power
                when (power) {
                    Power.WAND -> {
                        app.speaker.say("Abracadabra!")
                        powers.wand++
                    }
                    Power.MAP -> powers.map++
                    else -> {}
                }
            }
        }
    }, challenge = { m ->
        // Challenge side, on a big paper card.
        Box(m.paperCard(Color(0xF2FFFAF0), 28.dp).padding(14.dp)) {
            ChallengeView(beat.challenge, Modifier.fillMaxSize(), intro = text, powers = powers) { firstTry ->
                if (!solvedHere) {
                    solvedHere = true
                    onSolved(beat.challenge, firstTry)
                }
            }
        }
    })
}

/**
 * The story picture and the challenge: side by side on a tablet (story left, challenge right),
 * stacked on a narrow phone screen.
 */
@Composable
private fun StorySplit(story: @Composable (Modifier) -> Unit, challenge: @Composable (Modifier) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth < 600.dp) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                story(Modifier.fillMaxWidth().weight(0.34f))
                challenge(Modifier.fillMaxWidth().weight(0.66f))
            }
        } else {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                story(Modifier.weight(0.36f).fillMaxHeight())
                challenge(Modifier.weight(0.64f).fillMaxHeight())
            }
        }
    }
}

/**
 * Before the twist, someone is fast asleep. The child wakes them by calling their name out loud
 * (any try works, and ▶ skips). Then the twist carries on with them awake.
 */
@Composable
private fun WakeScene(quest: Quest, hero: Species, onAwake: () -> Unit) {
    val app = LocalApp.current
    val p = app.progress
    val v = quest.visitor
    var awake by remember { mutableStateOf(false) }
    val text = "Shh! Someone is fast asleep. It's ${v.callName}! Let's wake them up by calling their name."
    if (awake) AfterDelay("awake", 1200) { onAwake() }
    StorySplit(story = { m ->
        Column(m) {
            SpeechBubble(text, Modifier.fillMaxWidth(), fontSize = 15) { app.speaker.say(text) }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                SetPieceView(quest.twist.piece, null, false, Modifier.fillMaxSize())
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.Bottom) {
                    HeroWithPet(hero, p.heroColor(hero), p.outfit, p.pet, Modifier.weight(1f).fillMaxHeight(0.55f), Mood.HAPPY)
                    Critter(quest.friend.species, Modifier.weight(0.8f).fillMaxHeight(0.45f), Color(quest.friend.color), Outfit(), Mood.HAPPY, mirrored = true)
                }
            }
        }
    }, challenge = { m ->
        Box(m.paperCard(Color(0xF2FFFAF0), 28.dp).padding(14.dp)) {
            SpeakMoment(
                Word(v.callName, v.species.emoji), null, Modifier.fillMaxSize(),
                intro = text,
                afterLine = "${v.callName} is awake!",
                picture = { pm -> Critter(v.species, pm, Color(v.color), Outfit(), if (awake) Mood.CHEER else Mood.SLEEP, mirrored = true) },
            ) { awake = true }
        }
    })
}

/** One helper power per challenge: the magic wand for picking, the treasure map for the robot. */
@Composable
private fun HelperPower(challenge: Challenge, used: Set<Power>, onUse: (Power) -> Unit) {
    val power = when (challenge) {
        is Challenge.Robot -> Power.MAP
        is Challenge.Say, is Challenge.Coins, is Challenge.SoundOut -> null
        else -> Power.WAND
    } ?: return
    Row(Modifier.padding(top = 8.dp)) {
        PaperIconButton(power.emoji, Color(0xFFFFF3C4), size = 64.dp, enabled = power !in used) { onUse(power) }
    }
}

/**
 * The end of the chapter: the buddy shows off a new paper piece, the stickers earned pop up,
 * and everyone says "see you next time". The big green arrow goes back to the map, where the
 * next stop unfolds.
 */
@Composable
private fun Goodbye(piece: Cosmetic?, stickers: List<WordStat>, onMap: () -> Unit) {
    val app = LocalApp.current
    val p = app.progress
    val species = p.hero ?: Species.KID
    LaunchedEffect(Unit) {
        val parts = buildList {
            add("What an adventure!")
            if (piece != null) add("Your buddy got a new ${piece.name} from the backpack!")
            if (stickers.isNotEmpty()) add(if (stickers.size == 1) "And you got a new sticker!" else "And you got ${stickers.size} new stickers!")
            add("See you next time, ${p.heroName}! Tap the big green arrow to go back to the map.")
        }
        app.celebrate(parts.joinToString(" "))
        app.rememberInstruction(parts.last())
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight()
                .paperCard(Color(0xF5FFFAF0), 32.dp)
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(0.9f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                HeroWithPet(species, p.heroColor(species), p.outfit, p.pet, Modifier.weight(1f).fillMaxWidth().popIn(piece), Mood.CHEER)
                if (piece != null) PaperChip("🎁 ${piece.name}", Modifier.popIn(piece, 400))
                BackpackMeter(gemSize = 26)
            }
            Column(
                Modifier.weight(1.3f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PaperChip("👋 See you next time!", fontSize = 30.sp)
                if (stickers.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        stickers.take(4).forEachIndexed { i, w -> Sticker(w, 96.dp, Modifier.popIn(w.text, 600 + i * 200)) }
                    }
                }
                GoButton(size = 100.dp, onClick = onMap)
            }
        }
    }
}
