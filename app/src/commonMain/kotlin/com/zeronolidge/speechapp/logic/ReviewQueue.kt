package com.zeronolidge.speechapp.logic

import com.zeronolidge.speechapp.data.LetterContent
import com.zeronolidge.speechapp.data.ReadingContent
import com.zeronolidge.speechapp.data.Things
import kotlin.random.Random

/**
 * test50: missed-question review for the tap-the-answer mini games (Timothy/Aida/Ruby's spec).
 *
 *  1. A miss keeps the usual flow (Try again / clue / Rory shows and says the right one), so the
 *     last thing he hears is right.
 *  2. The missed question comes back ONCE, 2 or 3 rounds later (never the very next round): same
 *     question, the right answer in a different spot, at least one new wrong choice, and only 2
 *     choices. Rory says the question one extra time ([Review.sayTwice]).
 *  3. Right on that retry: it comes back one more time near the end of the game (when the session
 *     wraps up, or [RECHECK_GAP] rounds later if he keeps playing), with the usual number of
 *     choices, shuffled again with new wrong choices.
 *  4. Missed on the retry (or on the end check): Rory has shown the answer, and it's dropped for
 *     the day -- never queued again today, no looping.
 *  5. At most [MAX_PENDING] missed items wait at once; at most one repeat per round.
 *  6. Repeats look exactly like new questions (no badge, no red, the usual praise): the screen
 *     just gets a [Challenge] like any other.
 *
 * Only questions that can be rebuilt meaningfully are queued ([Rebuild.retry] returns null for
 * the others: the robot mazes and paying coins into the jar have no wrong choices to swap).
 */
class ReviewQueue(
    private val rnd: Random = Random,
    /** Questions dropped for the day (shared by every visit to the game today, see [droppedToday]). */
    private val dropped: MutableSet<String> = mutableSetOf(),
) {
    enum class Stage { RETRY, RECHECK }

    data class Pending(val key: String, val original: Challenge, val seen: Challenge, val stage: Stage, val dueRound: Int)

    /** A repeat to play this round. */
    data class Review(val challenge: Challenge, val stage: Stage, val key: String) {
        /** The 2-choice retry: Rory says the question one extra time. */
        val sayTwice get() = stage == Stage.RETRY
    }

    private val pending = mutableListOf<Pending>()
    /** The repeat being played right now (settled by [result]). */
    private var inPlay: Pending? = null
    private val finished = mutableSetOf<String>()
    private var lastServedRound = Int.MIN_VALUE

    val waiting: List<Pending> get() = pending.toList()
    fun isDropped(key: String) = key in dropped
    fun hasRecheck() = pending.any { it.stage == Stage.RECHECK }

    /**
     * The repeat for [round], or null for a new question. [ending]: the game is wrapping up, so
     * an end check that is waiting plays now (its spacing no longer matters).
     */
    fun next(round: Int, ending: Boolean = false): Review? {
        if (round == lastServedRound) return null
        // A repeat that never got a result (he left mid-round) isn't looped: dropped for today.
        inPlay?.let { dropped += it.key; inPlay = null }
        val due = pending.filter { it.dueRound <= round } +
            (if (ending) pending.filter { it.stage == Stage.RECHECK && it.dueRound > round } else emptyList())
        val p = due.minByOrNull { it.dueRound } ?: return null
        val c = when (p.stage) {
            Stage.RETRY -> Rebuild.retry(p.original, rnd)
            Stage.RECHECK -> Rebuild.recheck(p.original, p.seen, rnd)
        } ?: run { pending.remove(p); return null }
        pending.remove(p)
        inPlay = p.copy(seen = c)
        lastServedRound = round
        return Review(c, p.stage, p.key)
    }

    /**
     * After a round: [missed] when any wrong tap happened (or the answer had to be shown).
     * [review] is the repeat this round was, if it was one.
     */
    fun result(round: Int, challenge: Challenge, missed: Boolean, review: Review? = null) {
        if (review != null) {
            val p = inPlay?.takeIf { it.key == review.key } ?: return
            inPlay = null
            when {
                missed -> dropped += p.key
                p.stage == Stage.RETRY -> pending.add(p.copy(seen = challenge, stage = Stage.RECHECK, dueRound = round + RECHECK_GAP))
                else -> finished += p.key
            }
            return
        }
        if (!missed) return
        val key = Rebuild.keyOf(challenge) ?: return
        if (key in dropped || key in finished || pending.any { it.key == key } || inPlay?.key == key) return
        if (pending.size + (if (inPlay != null) 1 else 0) >= MAX_PENDING) return
        if (Rebuild.retry(challenge, Random(0)) == null) return
        pending.add(Pending(key, challenge, challenge, Stage.RETRY, round + 2 + rnd.nextInt(2)))
    }

    companion object {
        const val MAX_PENDING = 2

        private var droppedDay = Long.MIN_VALUE
        private val droppedSet = mutableSetOf<String>()

        /** Today's dropped questions (cleared when the day changes). [day]: days since 1970. */
        fun droppedToday(day: Long): MutableSet<String> {
            if (day != droppedDay) { droppedDay = day; droppedSet.clear() }
            return droppedSet
        }
        /** If he keeps playing, the end check comes this many rounds after a right retry. */
        const val RECHECK_GAP = 6
    }
}

/** Rebuilding a question: same target, answer moved, new wrong choices. */
object Rebuild {
    /** What makes two questions "the same question" (the target), or null when it can't come back. */
    fun keyOf(c: Challenge): String? = when (c) {
        is Challenge.Count -> "count:${c.round.emoji}:${c.round.count}"
        is Challenge.Math -> "math:${c.round.a}${if (c.round.plus) "+" else "-"}${c.round.b}"
        is Challenge.More -> "more:${c.round.emoji}:${maxOf(c.round.left, c.round.right)}"
        is Challenge.Letter -> "letter:${c.round.mode}:${c.round.target.lower}"
        is Challenge.SoundOut -> "soundout:${c.round.word.text}"
        is Challenge.WhichWord -> "which:${c.round.word.text}"
        is Challenge.Pattern -> "pattern:${(c.round.shown + c.round.choice.answer).joinToString("")}"
        is Challenge.OddOne -> "odd:${c.round.odd}"
        is Challenge.Coins -> if (c.round.mode == CoinMode.PURSE) "purse:${c.round.item}:${c.round.price}" else null
        is Challenge.Say, is Challenge.Robot -> null
    }

    /** The 2-choice retry. */
    fun retry(c: Challenge, rnd: Random): Challenge? = rebuild(c, c, 2, rnd)

    /** The end check: the usual number of choices, new wrong ones, the answer moved again. */
    fun recheck(original: Challenge, last: Challenge, rnd: Random): Challenge? {
        val n = AnswerKey.of(original)?.options?.size ?: return null
        return rebuild(original, last, n, rnd)
    }

    private fun rebuild(c: Challenge, last: Challenge, n: Int, rnd: Random): Challenge? {
        val lastKey = AnswerKey.of(last)
        return when (c) {
            is Challenge.Count -> Challenge.Count(c.round.copy(choice = fresh(c.round.choice, last.optionsOf(), lastKey, nearby(c.round.count, 1, 10), n, rnd)))
            is Challenge.Math -> Challenge.Math(c.round.copy(choice = fresh(c.round.choice, last.optionsOf(), lastKey, nearby(c.round.answer, 0, 10), n, rnd)))
            is Challenge.Letter -> Challenge.Letter(c.round.copy(choice = fresh(c.round.choice, last.optionsOf(), lastKey, LetterContent.letters, n, rnd)))
            is Challenge.SoundOut -> Challenge.SoundOut(c.round.copy(choice = fresh(c.round.choice, last.optionsOf(), lastKey, ReadingContent.cvcWords, n, rnd)))
            is Challenge.WhichWord -> Challenge.WhichWord(c.round.copy(choice = fresh(c.round.choice, last.optionsOf(), lastKey, ReadingContent.cvcWords, n, rnd)))
            is Challenge.Pattern -> {
                val a = c.round.choice.answer
                val pool = (if (a in Things.shapes) Things.shapes else Things.categories.firstOrNull { a in it } ?: Things.shapes) + c.round.unit
                Challenge.Pattern(c.round.copy(choice = fresh(c.round.choice, last.optionsOf(), lastKey, pool, n, rnd)))
            }
            is Challenge.Coins -> if (c.round.mode != CoinMode.PURSE) null
                else Challenge.Coins(c.round.copy(purses = fresh(c.round.purses, last.optionsOf(), lastKey, (1..7).toList(), n, rnd)))
            // Which side has more: always two piles. The bigger pile swaps sides, and the smaller
            // pile gets a new count.
            is Challenge.More -> {
                val r = (last as? Challenge.More)?.round ?: c.round
                val big = maxOf(c.round.left, c.round.right)
                val oldSmall = minOf(r.left, r.right)
                val smalls = (1 until big).filter { it != oldSmall }.ifEmpty { (1 until big).toList() }
                if (smalls.isEmpty()) return null
                val small = smalls.random(rnd)
                val bigLeft = !r.moreIsLeft
                Challenge.More(c.round.copy(left = if (bigLeft) big else small, right = if (bigLeft) small else big))
            }
            // Odd one out needs at least 3 pictures to make sense, so its "2-choice" retry is the
            // smallest real question: 2 alike and the odd one (3 tiles), with new look-alikes (a new
            // repeated picture, or new members of the group), and the odd one in a new spot.
            is Challenge.OddOne -> {
                val odd = c.round.odd
                val lastR = (last as? Challenge.OddOne)?.round ?: c.round
                val same = c.round.items.filter { it != odd }
                val total = if (n <= 2) 3 else c.round.items.size
                val identical = same.distinct().size == 1
                val alike = if (identical) {
                    // three of one picture: a new look-alike picture, so the wrong ones change too
                    val lastSame = lastR.items.firstOrNull { it != odd }
                    val pick = Things.countables.filter { it != odd && it != lastSame }.randomOrNull(rnd) ?: same.first()
                    List(total - 1) { pick }
                } else {
                    val group = Things.categories.firstOrNull { g -> same.all { it in g } } ?: same
                    val lastAlike = lastR.items.filter { it != odd }.toSet()
                    (group.filter { it !in lastAlike && it != odd }.shuffled(rnd) + group.filter { it != odd }.shuffled(rnd)).distinct().take(total - 1)
                }
                if (alike.size < total - 1) return null
                val oldAt = lastR.items.indexOf(odd)
                val at = (0 until total).filter { it != oldAt }.random(rnd)
                Challenge.OddOne(OddOneOutRound(alike.toMutableList().apply { add(at, odd) }, odd))
            }
            is Challenge.Say, is Challenge.Robot -> null
        }
    }

    private fun Challenge.optionsOf(): List<Any?> = AnswerKey.of(this)?.options.orEmpty()

    private fun nearby(n: Int, min: Int, max: Int) = ((n - 3)..(n + 3)).filter { it in min..max }

    /**
     * [n] choices for [old]'s answer: wrong ones from [pool], new ones first (not shown last time,
     * so at least one is different), and the answer never in the spot it had last time.
     */
    fun <T> fresh(old: Choice<T>, lastOptions: List<Any?>, lastKey: Key<*>?, pool: List<T>, n: Int, rnd: Random): Choice<T> {
        val answer = old.answer
        val wrongPool = pool.filter { it != answer }.distinct()
        val unseen = wrongPool.filter { it !in lastOptions }.shuffled(rnd)
        val seen = wrongPool.filter { it in lastOptions }.shuffled(rnd)
        val wrong = (unseen + seen).take(n - 1)
        val oldAt = lastKey?.let { k -> k.options.indexOf(k.answer) } ?: -1
        val spots = (0..wrong.size).filter { it != oldAt }.ifEmpty { listOf(0) }
        val at = spots.random(rnd)
        return Choice(wrong.toMutableList().apply { add(at, answer) }, answer)
    }
}
