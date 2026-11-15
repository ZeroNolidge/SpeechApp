package com.zeronolidge.speechapp.logic

import com.zeronolidge.speechapp.data.LetterContent
import com.zeronolidge.speechapp.data.Praise
import com.zeronolidge.speechapp.data.ReadingContent
import com.zeronolidge.speechapp.data.SpeechContent
import com.zeronolidge.speechapp.data.Things
import com.zeronolidge.speechapp.ui.challenge.promptFor
import com.zeronolidge.speechapp.ui.screens.letterIntro
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * test55 (Timothy: "some of the words had the default voice, like cherry"): every line built from
 * the word lists (game prompts, hints, Talk turns, letter and number lines, market and counting
 * lines) is fully covered by Rory's recorded clips in audio/manifest.json, so none of it falls
 * back to the device voice. The smoke test's VOICEGAP check only sees lines a walk happens to
 * reach; this one goes through every word in every list.
 */
class VoiceCoverageTest {
    private val keys: Set<String> by lazy {
        val f = listOf("src/wasmJsMain/resources/audio/manifest.json", "app/src/wasmJsMain/resources/audio/manifest.json").map(::File).first { it.exists() }
        val clips = f.readText().substringAfter("\"clips\"").substringAfter("{").substringBefore("}")
        Regex("\"((?:[^\"\\\\]|\\\\.)*)\"\\s*:").findAll(clips).map { it.groupValues[1] }.toSet()
    }

    private fun gaps(lines: Collection<String>): Map<String, String> {
        val out = linkedMapOf<String, String>()
        for (l in lines) ClipText.plan(l) { it in keys }.filterIsInstance<ClipText.Part.Speak>().forEach { out.getOrPut(it.text) { l } }
        return out
    }

    private fun dynamicLines(): Set<String> {
        val lines = linkedSetOf<String>()
        val rnd = Random(55)
        // Every game's question, many rounds of each.
        repeat(400) {
            lines += promptFor(Challenge.Count(Numbers.count(10, rnd)))
            lines += promptFor(Challenge.Math(Numbers.math(true, 10, rnd)))
            lines += promptFor(Challenge.Math(Numbers.math(false, 10, rnd)))
            lines += promptFor(Challenge.More(Numbers.compare(10, rnd)))
            LetterMode.entries.forEach { m -> lines += promptFor(Challenge.Letter(Letters.make(m, rnd))) }
            lines += promptFor(Challenge.WhichWord(Reading.make(ReadingContent.cvcWords, rnd)))
            lines += promptFor(Challenge.Coins(Coins.make(rnd.nextInt(0, 40), rnd)))
        }
        // Counting things by name, in every place they are said.
        (Things.countables + Things.countableNames.keys + Coins.goods).distinct().forEach { e ->
            val name = Things.nameOf(e)
            lines += "How many $name? Tap each one to count."
            lines += "Which side has more $name?"
            lines += "We found some $name that can help. Let's count them!"
            (1..6).forEach { n -> lines += "${Numbers.word(n)}! That's just right. Here are your $name!" }
        }
        (0..20).forEach { lines += Numbers.word(it) }
        // Letters: the alphabet intro and every hint.
        LetterContent.letters.forEach { t ->
            lines += letterIntro(t)
            lines += "Listen: ${t.sound}, ${t.sound}, ${t.word.text}! ${t.upper} says ${t.sound}."
            lines += "${t.word.text}. ${t.sound}, ${t.sound}, ${t.word.text}. Which letter does ${t.word.text} start with?"
        }
        // Sounding out: each word's letter sounds, the blend and the question.
        ReadingContent.cvcWords.forEach { w ->
            val s = Reading.sounds(w.text)
            lines += s.joinToString("... ")
            lines += "${w.text}!"
            lines += "Which picture is ${w.text}?"
            lines += "Find the word ${w.text}."
            lines += "Listen: ${s.joinToString("... ")}... ${w.text}! It starts with ${s.firstOrNull() ?: w.text}."
        }
        // Talk: every sound's model, tip and words (said plain, in syllables, and in its turn lines).
        SpeechContent.sounds.forEach { sound ->
            lines += "Let's practice this sound. ${sound.modelSound}."
            lines += "Let's practice the ${sound.label} sound. ${sound.modelSound}."
            lines += "Let's say ${sound.modelSound}"
            lines += sound.tip
            lines += "Listen: ${sound.modelSound}. ${sound.label} says ${sound.modelSound}. Find the ${sound.label}!"
            (sound.words + sound.phrases).forEach { w ->
                lines += "${w.text}!"
                lines += w.syllables
                lines += "Say ${w.text}."
                lines += "Listen. ${sound.modelSound}. ${w.text}."
                lines += "Say ${w.text} and I'll copy you!"
                lines += "${w.syllables}. ${w.text}. Your turn! Say ${w.text}!"
            }
        }
        // Voice stories: every beat's problem, words and success, and the turn lines built from them.
        (VoiceStories.all).forEach { st0 ->
            val st = VoiceStories.ready(st0, emptyList())
            lines += st.setup
            lines += st.ending
            st.beats.forEach { b ->
                lines += b.problem
                lines += b.success
                b.lines.forEach { l -> lines += l; lines += "$l!" }
                lines += VoiceStories.kitchenYourTurnLine(b.lines.first())
            }
        }
        BalloonGame.colors.forEach { c -> lines += BalloonGame.lines(c); lines += BalloonGame.modelPop(c) }
        // The crystal cave's echo of each practice word.
        SpeechContent.sounds.flatMap { it.words }.forEach { w -> lines += EchoCave.echo(w.text) }
        // The closet's pieces and the buddies, in the lines that name them.
        Catalog.items.forEach { c ->
            lines += "${c.name}! Looking good!"
            lines += "Your buddy got a new ${c.name} from the backpack!"
            lines += "Your backpack is full! Your buddy got a new ${c.name}!"
        }
        Species.entries.forEach { sp -> lines += "Where shall we go, ${sp.label}?" }
        // Patterns said by name (shapes and counted things).
        Things.shapes.forEach { lines += Things.nameOf(it) }
        lines += Praise.great + Praise.tryAgain + Praise.speechTry
        return lines
    }

    @Test
    fun everyWordListLineHasRorysVoice() {
        val g = gaps(dynamicLines())
        g.forEach { (w, l) -> println("VOICEGAP-JVM [$w] in: $l") }
        assertEquals(emptyMap(), g, "words that would fall back to the device voice")
    }
}
