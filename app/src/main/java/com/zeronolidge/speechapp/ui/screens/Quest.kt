package com.zeronolidge.speechapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.logic.Catalog
import com.zeronolidge.speechapp.logic.Challenge
import com.zeronolidge.speechapp.logic.Land
import com.zeronolidge.speechapp.logic.Leveling
import com.zeronolidge.speechapp.logic.Patterns
import com.zeronolidge.speechapp.logic.Power
import com.zeronolidge.speechapp.logic.Quest
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.logic.StoryGen
import com.zeronolidge.speechapp.ui.Screen
import com.zeronolidge.speechapp.ui.art.Critter
import com.zeronolidge.speechapp.ui.art.HeroWithPet
import com.zeronolidge.speechapp.ui.art.Mood
import com.zeronolidge.speechapp.ui.art.Outfit
import com.zeronolidge.speechapp.ui.art.Scenery
import com.zeronolidge.speechapp.ui.challenge.ChallengeView
import com.zeronolidge.speechapp.ui.challenge.PowerTicks
import com.zeronolidge.speechapp.ui.challenge.promptFor
import com.zeronolidge.speechapp.ui.components.AfterDelay
import com.zeronolidge.speechapp.ui.components.GoButton
import com.zeronolidge.speechapp.ui.components.Nudge
import com.zeronolidge.speechapp.ui.components.LevelPill
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.RoundIcon
import com.zeronolidge.speechapp.ui.components.SpeechBubble
import com.zeronolidge.speechapp.ui.components.XpBar
import com.zeronolidge.speechapp.ui.theme.Kid
import kotlin.random.Random

private sealed interface Phase {
    data object Intro : Phase
    data class Beat(val index: Int) : Phase
    data object Ending : Phase
    data object Rewards : Phase
}

/** A randomly generated story: meet a friend, solve challenges along the way, and earn rewards. */
@Composable
fun QuestScreen(land: Land, mixed: Boolean, back: () -> Unit, go: (Screen) -> Unit) {
    val app = LocalApp.current
    val p = app.progress
    val quest = remember { StoryGen.make(land, p.storyParams(), Random(System.nanoTime()), mixed) }
    val startLevel = remember { p.heroLevel }
    val startXp = remember { p.xp }
    var phase by remember { mutableStateOf<Phase>(Phase.Intro) }
    var cheer by remember { mutableStateOf(false) }
    var earnedGems by remember { mutableIntStateOf(0) }
    var patternWins by remember { mutableIntStateOf(0) }
    val species = p.hero ?: Species.KID

    fun next() {
        cheer = false
        phase = when (val ph = phase) {
            Phase.Intro -> Phase.Beat(0)
            is Phase.Beat -> if (ph.index + 1 < quest.beats.size) Phase.Beat(ph.index + 1) else Phase.Ending
            Phase.Ending -> Phase.Rewards
            Phase.Rewards -> Phase.Rewards
        }
    }

    fun solved(c: Challenge, firstTry: Boolean) {
        val xp = Leveling.XP_PER_CHALLENGE + if (firstTry) Leveling.XP_FIRST_TRY_BONUS else 0
        val gems = if (firstTry) 2 else 1
        earnedGems += gems
        p.gain(xp, gems)
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

    Scenery(quest.land, Modifier.fillMaxSize(), seed = (phase as? Phase.Beat)?.index ?: 9) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            QuestTopBar(quest, phase, back)
            Spacer(Modifier.height(6.dp))
            when (val ph = phase) {
                Phase.Intro -> StoryScene(
                    quest, quest.intro, species, Mood.HAPPY, Mood.THINK,
                    onNext = ::next,
                )
                is Phase.Beat -> BeatScene(quest, ph.index, species, cheer, onSolved = { c, ft -> solved(c, ft) }, onNext = ::next)
                Phase.Ending -> {
                    LaunchedEffect(Unit) {
                        p.gain(Leveling.XP_PER_QUEST, 5)
                        earnedGems += 5
                        p.finishQuest()
                    }
                    StoryScene(
                        quest, quest.ending, species, Mood.CHEER, Mood.CHEER,
                        showGoal = true, onNext = ::next,
                    )
                }
                Phase.Rewards -> Rewards(
                    xpGained = p.xp - startXp,
                    gems = earnedGems,
                    startLevel = startLevel,
                    onMap = back,
                    onDressUp = { back(); go(Screen.Wardrobe) },
                )
            }
        }
    }
}

@Composable
private fun QuestTopBar(quest: Quest, phase: Phase, back: () -> Unit) {
    val app = LocalApp.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RoundIcon("🗺️", Color.White, size = 52.dp) { app.speaker.stop(); back() }
        Spacer(Modifier.width(10.dp))
        Text(
            quest.land.title,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Kid.ink,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xCCFFFFFF))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )
        Spacer(Modifier.weight(1f))
        // The trail: one footprint per challenge, ending at the goal.
        val current = when (phase) {
            Phase.Intro -> -1
            is Phase.Beat -> phase.index
            else -> quest.beats.size
        }
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xCCFFFFFF))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            quest.beats.indices.forEach { i ->
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                i < current -> Kid.good
                                i == current -> Kid.highlight
                                else -> Color(0xFFECEFF1)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) { Text(if (i < current) "✓" else "👣", fontSize = 16.sp, color = Color.White) }
            }
            Text("➜", fontSize = 18.sp, color = Kid.ink, modifier = Modifier.padding(horizontal = 4.dp))
            Text(quest.goal.emoji, fontSize = 28.sp, modifier = Modifier.alpha(if (current >= quest.beats.size) 1f else 0.5f))
        }
    }
}

/** A full-width story moment: the hero and the friend talking, with a big button to continue. */
@Composable
private fun StoryScene(
    quest: Quest,
    text: String,
    hero: Species,
    heroMood: Mood,
    friendMood: Mood,
    showGoal: Boolean = false,
    onNext: () -> Unit,
) {
    val app = LocalApp.current
    val p = app.progress
    val goLine = "Tap the big green arrow!"
    LaunchedEffect(text) {
        app.speaker.say(text)
        app.speaker.then(goLine)
    }
    Nudge(text, goLine)
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        SpeechBubble(text, Modifier.fillMaxWidth(0.8f), fontSize = 22) { app.speaker.say(text) }
        Row(
            Modifier.weight(1f).fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            HeroWithPet(hero, p.heroColor(hero), p.outfit, p.pet, Modifier.fillMaxHeight(0.95f).width(200.dp), heroMood)
            if (showGoal) Text(quest.goal.emoji, fontSize = 80.sp, modifier = Modifier.padding(bottom = 30.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxHeight(0.95f)) {
                Critter(quest.friend.species, Modifier.weight(1f), Color(quest.friend.color), Outfit(), friendMood, mirrored = true)
                NameTag(quest.friend.name)
            }
            // The only way forward, so make it impossible to miss.
            GoButton(Modifier.align(Alignment.CenterVertically), size = 104.dp, onClick = onNext)
        }
    }
}

@Composable
private fun NameTag(name: String) {
    Text(
        name,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Kid.ink,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xDDFFFFFF))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** One step of the story: characters on the left, the challenge on the right, powers below. */
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
    val text = listOfNotNull(beat.travel, beat.narration).joinToString(" ")
    if (solvedHere) AfterDelay(index, 900) { onNext() }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // Story side.
        Column(Modifier.weight(0.34f).fillMaxHeight()) {
            SpeechBubble(text, Modifier.fillMaxWidth(), fontSize = 16) { app.speaker.say(text) }
            Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                HeroWithPet(
                    hero, p.heroColor(hero), p.outfit, p.pet,
                    Modifier.weight(1f).fillMaxHeight(0.9f),
                    if (cheer) Mood.CHEER else Mood.HAPPY,
                )
                Critter(
                    quest.friend.species, Modifier.weight(0.8f).fillMaxHeight(0.75f),
                    Color(quest.friend.color), Outfit(),
                    if (cheer) Mood.CHEER else Mood.THINK, mirrored = true,
                )
            }
            PowerBar(beat.challenge, used) { power ->
                used = used + power
                when (power) {
                    Power.OWL -> {
                        app.speaker.say("Hoo hoo! Listen carefully.")
                        app.speaker.then(promptFor(beat.challenge), slow = true)
                        (beat.challenge as? Challenge.Say)?.let { app.speaker.then(it.word.syllables, slow = true) }
                    }
                    Power.WAND -> {
                        app.speaker.say("Abracadabra!")
                        powers.wand++
                    }
                    Power.MAP -> powers.map++
                    else -> {}
                }
            }
        }
        // Challenge side.
        Box(
            Modifier
                .weight(0.66f)
                .fillMaxHeight()
                .shadow(8.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xEEFFFFFF))
                .padding(12.dp),
        ) {
            ChallengeView(beat.challenge, Modifier.fillMaxSize(), intro = text, powers = powers) { firstTry ->
                if (!solvedHere) {
                    solvedHere = true
                    onSolved(beat.challenge, firstTry)
                }
            }
        }
    }
}

/** Helper powers the hero has learned; each can be used once per challenge. */
@Composable
private fun PowerBar(challenge: Challenge, used: Set<Power>, onUse: (Power) -> Unit) {
    val level = LocalApp.current.progress.heroLevel
    val useful = buildList {
        // Talking has its own "hear it" buttons, and the owl would talk over the microphone.
        if (challenge !is Challenge.Say) add(Power.OWL)
        if (challenge !is Challenge.Say && challenge !is Challenge.Robot) add(Power.WAND)
        if (challenge is Challenge.Robot) add(Power.MAP)
    }.filter { it.level <= level }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        useful.forEach { power ->
            val spent = power in used
            Box(
                Modifier
                    .size(52.dp)
                    .alpha(if (spent) 0.35f else 1f)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF8E1))
                    .border(3.dp, Color(0xFFFFB300), CircleShape)
                    .clickable(enabled = !spent) { onUse(power) },
                contentAlignment = Alignment.Center,
            ) { Text(power.emoji, fontSize = 26.sp) }
        }
    }
}

/** End of the quest: XP and gems, and the big level-up moment with new powers and gifts. */
@Composable
private fun Rewards(xpGained: Int, gems: Int, startLevel: Int, onMap: () -> Unit, onDressUp: () -> Unit) {
    val app = LocalApp.current
    val p = app.progress
    val level = p.heroLevel
    val levelled = level > startLevel
    val newPowers = (startLevel + 1..level).flatMap { Leveling.newPowers(it) }
    val newGifts = (startLevel + 1..level).flatMap { Catalog.freeAt(it) }
    val newShop = (startLevel + 1..level).flatMap { l -> Catalog.items.filter { it.level == l && it.price > 0 } }
    val species = p.hero ?: Species.KID
    LaunchedEffect(Unit) {
        app.speaker.say("Quest complete! You earned $xpGained stars of experience and $gems gems!")
        if (levelled) {
            app.speaker.then("Level up! You are now level $level!")
            newPowers.forEach { app.speaker.then("New power: ${it.label}! ${it.describe}") }
            newGifts.forEach { app.speaker.then("You got a present: ${it.name}!") }
            if (newShop.isNotEmpty()) app.speaker.then("And there are new things in the dress up shop!")
        }
        app.speaker.then("Tap the green arrow to go back to the map, or the pink backpack to dress up!")
    }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight()
                .shadow(10.dp, RoundedCornerShape(32.dp))
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xF5FFFFFF))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HeroWithPet(species, p.heroColor(species), p.outfit, p.pet, Modifier.weight(0.8f).fillMaxHeight(), Mood.CHEER)
            Column(
                Modifier.weight(1.4f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (levelled) "🎉 LEVEL UP! 🎉" else "Quest complete!",
                    fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (levelled) Color(0xFF7C4DFF) else Kid.ink,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LevelPill(level)
                    Spacer(Modifier.width(10.dp))
                    XpBar(Leveling.progress(p.xp), Modifier.width(220.dp), height = 18.dp)
                }
                Text("+$xpGained XP    +$gems 💎", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Kid.ink)
                if (newPowers.isNotEmpty() || newGifts.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        newPowers.forEach { Unlock(it.emoji, it.label) }
                        newGifts.forEach { Unlock("🎁", it.name) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundIcon("🎒", Color(0xFFF8BBD0), size = 76.dp, onClick = onDressUp)
                    GoButton(size = 84.dp, onClick = onMap)
                }
            }
        }
    }
}

@Composable
private fun Unlock(emoji: String, label: String) {
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFFF8E1))
            .border(2.dp, Color(0xFFFFB300), RoundedCornerShape(16.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 32.sp)
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Kid.ink, textAlign = TextAlign.Center)
    }
}
