package com.zeronolidge.speechapp.logic

import com.zeronolidge.speechapp.data.LetterInfo
import com.zeronolidge.speechapp.data.SpeechContent
import com.zeronolidge.speechapp.data.SpeechSound
import com.zeronolidge.speechapp.data.Word
import kotlin.random.Random

/** A quiz's answer tiles and the one correct tile. */
data class Key<T>(val options: List<T>, val answer: T)

/**
 * The answer key: the ONE place that says which tile is correct for every kind of quiz. Every
 * quiz screen takes its tiles and its correct answer from here, and the tests play each kind of
 * quiz against it (a right tap and wrong taps). Quizzes without wrong answers (saying a word,
 * paying coins into the jar, the robot) have no key.
 */
object AnswerKey {
    fun count(c: Challenge.Count): Key<Int> = Key(c.round.choice.options, c.round.choice.answer)
    fun math(c: Challenge.Math): Key<Int> = Key(c.round.choice.options, c.round.choice.answer)
    /** Which side has more: true is the left pile. */
    fun more(c: Challenge.More): Key<Boolean> = Key(listOf(true, false), c.round.moreIsLeft)
    fun letter(c: Challenge.Letter): Key<LetterInfo> = Key(c.round.choice.options, c.round.choice.answer)
    fun soundOut(c: Challenge.SoundOut): Key<Word> = Key(c.round.choice.options, c.round.choice.answer)
    fun whichWord(c: Challenge.WhichWord): Key<Word> = Key(c.round.choice.options, c.round.choice.answer)
    fun pattern(c: Challenge.Pattern): Key<String> = Key(c.round.choice.options, c.round.choice.answer)
    /** Options are positions, so identical pictures are still separate tiles. */
    fun oddOne(c: Challenge.OddOne): Key<Int> = Key(c.round.items.indices.toList(), c.round.items.indexOf(c.round.odd))
    fun purse(c: Challenge.Coins): Key<Int>? = if (c.round.mode == CoinMode.PURSE) Key(c.round.purses.options, c.round.price) else null

    /**
     * "Tap the B!" right after he says a word with that sound: the sound's letter and one or two
     * other letters ([count] tiles, 2 or 3 in all).
     */
    fun tapLetter(sound: SpeechSound, count: Int, rnd: Random = Random): Key<String> {
        val others = SpeechContent.sounds.map { it.label }.filter { it != sound.label }.distinct().shuffled(rnd)
        return Key((others.take(count.coerceIn(2, 3) - 1) + sound.label).shuffled(rnd), sound.label)
    }

    fun of(c: Challenge): Key<*>? = when (c) {
        is Challenge.Count -> count(c)
        is Challenge.Math -> math(c)
        is Challenge.More -> more(c)
        is Challenge.Letter -> letter(c)
        is Challenge.SoundOut -> soundOut(c)
        is Challenge.WhichWord -> whichWord(c)
        is Challenge.Pattern -> pattern(c)
        is Challenge.OddOne -> oddOne(c)
        is Challenge.Coins -> purse(c)
        is Challenge.Say, is Challenge.Robot -> null
    }
}
