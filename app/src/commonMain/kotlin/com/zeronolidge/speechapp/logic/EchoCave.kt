package com.zeronolidge.speechapp.logic

import com.zeronolidge.speechapp.data.SpeechContent
import com.zeronolidge.speechapp.data.Word
import kotlin.random.Random

/**
 * "Echo Cave", a short reward stop after finding Pip: Rory says a word, he says it into the paper
 * cave, the echo bounces back and a crystal lights up. About 8 tries; any voice counts (loudness
 * only), the "I said it!" button always works, and there is no score, timer or fail.
 */
object EchoCave {
    const val TRIES = 8

    /** His own words first, then words for his target sounds, then everyday words, at his speech step. */
    fun words(own: List<Pair<String, String>>, focusSounds: Set<String>, speechStep: Int, rnd: Random): List<Word> {
        val mine = own.map { Word(it.first, it.second) }
        val target = SpeechContent.sounds.filter { it.id in focusSounds }.flatMap { SpeechSteps.wordsFor(it, speechStep) }.shuffled(rnd)
        val more = SpeechContent.sounds.flatMap { SpeechSteps.wordsFor(it, speechStep) }.shuffled(rnd)
        return (mine + target + more).distinctBy { it.text.lowercase() }.take(TRIES)
    }

    /** The echo bouncing back: the word, softer and softer. */
    fun echo(word: String): String {
        val w = word.trim().trimEnd('!', '.')
        return "$w... $w... $w..."
    }
}

/** For the grown-ups page: 2 or 3 words to say with him today. */
object TodayWords {
    /** Tricky words first (most recent first), then words due back from earlier days, then his target-sound words. */
    fun pick(book: WordBook, today: Int, targetWords: List<String>, limit: Int = 3): List<String> {
        val tricky = book.words.values
            .filter { book.status(it) == WordStatus.STILL_TRICKY || book.status(it) == WordStatus.NOT_HEARD }
            .sortedByDescending { it.lastDay }
            .map { it.text }
        val due = book.dueForReview(today, limit).map { it.text }
        return (tricky + due + targetWords).distinctBy { it.lowercase() }.take(limit)
    }
}
