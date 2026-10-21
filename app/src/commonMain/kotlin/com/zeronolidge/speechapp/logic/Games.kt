package com.zeronolidge.speechapp.logic

import com.zeronolidge.speechapp.data.LetterContent
import com.zeronolidge.speechapp.data.LetterInfo
import com.zeronolidge.speechapp.data.Things
import com.zeronolidge.speechapp.data.Word
import kotlin.random.Random

/** A multiple-choice round: a set of options, one of which is right. */
data class Choice<T>(val options: List<T>, val answer: T)

fun <T> choiceOf(answer: T, pool: List<T>, count: Int, rnd: Random): Choice<T> {
    val wrong = pool.filter { it != answer }.distinct().shuffled(rnd).take(count - 1)
    return Choice((wrong + answer).shuffled(rnd), answer)
}

// ---------- Patterns ----------

data class PatternRound(
    /** The part of the pattern shown to the child. */
    val shown: List<String>,
    val choice: Choice<String>,
    val unit: List<String>,
)

object Patterns {
    /** Pattern "units" from easiest to hardest. Letters stand for different pictures. */
    private val unitsByLevel = listOf(
        listOf("AB"),
        listOf("AB", "AAB", "ABB"),
        listOf("AB", "AAB", "ABB", "ABC"),
        listOf("AAB", "ABB", "ABC", "AABB", "ABCC"),
    )

    fun maxLevel() = unitsByLevel.size - 1

    /** The quiet difficulty steps (see [Ladder]): AB, then ABB (or AAB), then ABC. */
    private val unitsByStep = listOf(listOf("AB"), listOf("ABB", "AAB"), listOf("ABC"))

    fun makeStep(step: Int, rnd: Random): PatternRound = fromShape(unitsByStep[step.coerceIn(0, unitsByStep.size - 1)].random(rnd), rnd)

    fun make(level: Int, rnd: Random): PatternRound = fromShape(unitsByLevel[level.coerceIn(0, maxLevel())].random(rnd), rnd)

    private fun fromShape(shape: String, rnd: Random): PatternRound =
        fromPool(shape, if (rnd.nextBoolean()) Things.shapes else Things.categories.random(rnd), rnd)

    /** A pattern round from any pool of pictures (or words): used for the usual shape/category
     * pools above, and for [BalloonGame]'s colours. */
    fun fromPool(shape: String, pool: List<String>, rnd: Random): PatternRound {
        val shuffled = pool.shuffled(rnd)
        val letters = shape.toSet().sorted()
        val pick = letters.zip(shuffled).toMap()
        val unit = shape.map { pick.getValue(it) }
        // Show at least two full repeats, then ask for the next item.
        val total = unit.size * 2 + rnd.nextInt(0, unit.size)
        val seq = List(total + 1) { unit[it % unit.size] }
        val answer = seq.last()
        return PatternRound(seq.dropLast(1), choiceOf(answer, shuffled.take(letters.size + 1), 3, rnd), unit)
    }
}

/**
 * The balloon colour-pattern mini game inside a voice-story beat (see [VoiceBeat.game]): finish a
 * row of balloons with a colour pattern (a [PatternRound] of colour words, reusing [Patterns.fromPool]).
 * Every colour starts with one of his target sounds, p or b, so the word he says afterwards (the
 * colour he picked) is also speech practice. Its own quiet ladder (see [Ladder.GAME_PATTERN])
 * steps AB -> AAB -> ABC, the same rule as every other difficulty step.
 */
object BalloonGame {
    val colors = listOf("blue", "purple", "pink", "brown")

    private val shapesByStep = listOf("AB", "AAB", "ABC")

    fun make(step: Int, rnd: Random): PatternRound = Patterns.fromPool(shapesByStep[step.coerceIn(0, shapesByStep.lastIndex)], colors, rnd)

    /** The word he says for [color]: one word, two words, a short sentence. */
    fun lines(color: String): List<String> = listOf(color, "$color balloon", "Say $color!")

    /** The speech sound it practises: every colour in [colors] starts with p or b. */
    fun sound(color: String): String = color.first().toString()
}

/** Three of one kind and one that doesn't belong. */
data class OddOneOutRound(val items: List<String>, val odd: String)

object OddOneOut {
    fun make(level: Int, rnd: Random): OddOneOutRound {
        return if (level == 0) {
            // Easy: three identical pictures and one different one.
            val (same, odd) = Things.countables.shuffled(rnd).take(2)
            OddOneOutRound((List(3) { same } + odd).shuffled(rnd), odd)
        } else {
            // Harder: three animals and a food, etc.
            val cats = Things.categories.shuffled(rnd)
            val same = cats[0].shuffled(rnd).take(3)
            val odd = cats[1].random(rnd)
            OddOneOutRound((same + odd).shuffled(rnd), odd)
        }
    }
}

// ---------- Numbers ----------

data class CountRound(val emoji: String, val count: Int, val choice: Choice<Int>)

data class MathRound(
    val emoji: String,
    val a: Int,
    val b: Int,
    val plus: Boolean,
    val choice: Choice<Int>,
) {
    val answer get() = choice.answer
    val spoken get() = "$a ${if (plus) "plus" else "take away"} $b"
}

data class CompareRound(val emoji: String, val left: Int, val right: Int) {
    val moreIsLeft get() = left > right
}

/** A counting round or a sum, for [Numbers.forStep]. */
sealed interface MathOrCount {
    data class C(val round: CountRound) : MathOrCount
    data class M(val round: MathRound) : MathOrCount
}

object Numbers {
    private fun nearby(n: Int, min: Int, max: Int, rnd: Random): Choice<Int> {
        val pool = ((n - 3)..(n + 3)).filter { it in min..max && it != n }
        return choiceOf(n, pool + n, 3, rnd)
    }

    fun count(max: Int, rnd: Random, emoji: String = Things.countables.random(rnd)): CountRound {
        val n = rnd.nextInt(1, max + 1)
        return CountRound(emoji, n, nearby(n, 1, max.coerceAtLeast(4), rnd))
    }

    /** Addition and subtraction that stay within [max] and never go below zero. */
    fun math(plus: Boolean, max: Int, rnd: Random, emoji: String = Things.countables.random(rnd)): MathRound {
        return if (plus) {
            val total = rnd.nextInt(2, max + 1)
            val a = rnd.nextInt(1, total)
            MathRound(emoji, a, total - a, true, nearby(total, 0, max, rnd))
        } else {
            val a = rnd.nextInt(2, max + 1)
            val b = rnd.nextInt(1, a + 1)
            MathRound(emoji, a, b, false, nearby(a - b, 0, max, rnd))
        }
    }

    /** "One more": a few things and one more come along (the top numbers step). */
    fun addOne(rnd: Random, emoji: String = Things.countables.random(rnd)): MathRound {
        val a = rnd.nextInt(1, 10)
        return MathRound(emoji, a, 1, true, nearby(a + 1, 0, 10, rnd))
    }

    /** A numbers puzzle for a quiet difficulty step: count to 5, count to 10, then add one. */
    fun forStep(step: Int, rnd: Random, emoji: String = Things.countables.random(rnd)): MathOrCount = when (step.coerceIn(0, 2)) {
        0 -> MathOrCount.C(count(5, rnd, emoji))
        1 -> MathOrCount.C(count(10, rnd, emoji))
        else -> MathOrCount.M(addOne(rnd, emoji))
    }

    fun compare(max: Int, rnd: Random, emoji: String = Things.countables.random(rnd)): CompareRound {
        val l = rnd.nextInt(1, max + 1)
        var r = rnd.nextInt(1, max + 1)
        while (r == l) r = rnd.nextInt(1, max + 1)
        return CompareRound(emoji, l, r)
    }

    private val ones = listOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
        "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen",
        "nineteen", "twenty",
    )

    fun word(n: Int): String = ones.getOrElse(n) { n.toString() }
}

// ---------- Coins (the market) ----------

/**
 * Market games. PAY: tap coins into the shopkeeper's jar until there are [price] of them.
 * PURSE: pick the purse that holds exactly [price] coins.
 */
enum class CoinMode { PAY, PURSE }

data class CoinRound(val item: String, val price: Int, val mode: CoinMode, val purses: Choice<Int>) {
    /** Coins on the table to pay with: always a few more than needed. */
    val onTable get() = price + 2
}

object Coins {
    /** The things you can get at the market stalls. */
    val goods = listOf("🍎", "🍌", "🥕", "🍞", "🧀", "🍓", "🥚", "🍪")

    fun make(chapter: Int, rnd: Random, item: String = goods.random(rnd)): CoinRound {
        val max = if (chapter >= Trail.size) 6 else 4
        val price = rnd.nextInt(1, max + 1)
        val mode = if (rnd.nextInt(3) == 0) CoinMode.PURSE else CoinMode.PAY
        val pool = (1..max + 1).filter { it != price }
        return CoinRound(item, price, mode, choiceOf(price, pool + price, 3, rnd))
    }
}

// ---------- Letters & reading ----------

enum class LetterMode { BIG_TO_SMALL, SMALL_TO_BIG, FIRST_SOUND }

data class LetterRound(val mode: LetterMode, val target: LetterInfo, val choice: Choice<LetterInfo>)

object Letters {
    /** Pairs that look alike are good practice once the basics are known. */
    private val confusable = listOf("bdpq", "mnwu", "ilt", "ceo", "hn", "vwy")

    /** The letters his target sounds and own words start with ("sh" and "Sam" both give S). */
    fun targetsFor(focusSounds: Set<String>, ownWords: List<String>): List<LetterInfo> =
        (focusSounds.mapNotNull { it.firstOrNull() } + ownWords.mapNotNull { it.trim().firstOrNull() })
            .filter { it.isLetter() }
            .map { LetterContent.of(it.lowercaseChar()) }
            .distinct()

    /**
     * The letter a round asks for: his own letters (from [targetsFor]) first, three rounds in
     * four, so the grown-up's target sounds and words come up most; otherwise any letter (null).
     */
    fun pickTarget(mine: List<LetterInfo>, rnd: Random): LetterInfo? =
        if (mine.isNotEmpty() && rnd.nextInt(4) != 0) mine.random(rnd) else null

    /**
     * What a letter tile shows: always a letter, one case per round. Big A asks for little a,
     * little a asks for big A, and the first-sound puzzle shows both on every tile ("Aa").
     */
    fun tileText(mode: LetterMode, l: LetterInfo): String = when (mode) {
        LetterMode.BIG_TO_SMALL -> "${l.lower}"
        LetterMode.SMALL_TO_BIG -> "${l.upper}"
        LetterMode.FIRST_SOUND -> "${l.upper}${l.lower}"
    }

    /** The letter shown as the question, or null when the question is a picture (first sound). */
    fun questionText(mode: LetterMode, t: LetterInfo): String? = when (mode) {
        LetterMode.BIG_TO_SMALL -> "${t.upper}"
        LetterMode.SMALL_TO_BIG -> "${t.lower}"
        LetterMode.FIRST_SOUND -> null
    }

    fun make(mode: LetterMode, rnd: Random, tricky: Boolean = false, target: LetterInfo? = null): LetterRound {
        val target = target ?: LetterContent.letters.random(rnd)
        val similar = confusable.firstOrNull { target.lower in it }
            ?.map { LetterContent.of(it) }.orEmpty()
        val pool = if (tricky && similar.size >= 2) similar + LetterContent.letters.shuffled(rnd).take(2)
        else LetterContent.letters
        return LetterRound(mode, target, choiceOf(target, pool, 3, rnd))
    }
}

data class ReadRound(val word: Word, val choice: Choice<Word>)

object Reading {
    fun make(words: List<Word>, rnd: Random, avoid: Word? = null): ReadRound {
        val word = words.filter { it != avoid }.random(rnd)
        return ReadRound(word, choiceOf(word, words, 3, rnd))
    }

    /** The individual letter sounds for a simple word, e.g. "cat" -> ["kuh", "ah", "tuh"]. */
    fun sounds(word: String): List<String> = word.filter { it.isLetter() }.map { LetterContent.of(it).sound }
}

// ---------- Speech ----------

object SpeechMatch {
    /**
     * Speech recognizers find young children hard to understand, so this is generous:
     * any recognised phrase that contains the word, or is close to it, counts.
     * Returns a score from 0.0 to 1.0.
     */
    fun score(target: String, heard: List<String>): Double {
        val t = normalize(target)
        if (t.isEmpty()) return 0.0
        var best = 0.0
        for (phrase in heard) {
            val p = normalize(phrase)
            if (p.isEmpty()) continue
            val words = p.split(' ')
            if (t in words || p == t) return 1.0
            for (w in words + p.replace(" ", "")) {
                best = maxOf(best, similarity(t, w))
            }
        }
        return best
    }

    fun isGood(target: String, heard: List<String>) = score(target, heard) >= 0.6

    fun normalize(s: String) = s.lowercase().replace(Regex("[^a-z ]"), " ").trim().replace(Regex("\\s+"), " ")

    fun similarity(a: String, b: String): Double {
        if (a.isEmpty() && b.isEmpty()) return 1.0
        val d = levenshtein(a, b)
        return 1.0 - d.toDouble() / maxOf(a.length, b.length)
    }

    fun levenshtein(a: String, b: String): Int {
        var prev = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val cur = IntArray(b.length + 1)
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
            }
            prev = cur
        }
        return prev[b.length]
    }
}
