package com.zeronolidge.speechapp.logic

import com.zeronolidge.speechapp.data.SpeechContent

/**
 * On-device word recognition, the plain-Kotlin half (unit-tested). The recogniser (Vosk, web
 * only) listens for a tiny list per talk turn: the target word, up to 3 likely swaps for its
 * speech sound, and "not sure". Loudness still moves the story at once; this only adds a quiet
 * record of what was heard, and (when a grown-up turns it on, after checking it agrees with them)
 * a gentle model of the sound. Nothing here can stop the story or show a failure.
 */

/** Where the speech sound sits in the word. */
enum class SoundPos { START, MIDDLE, END }

/** What the recogniser made of a try. "Unsure" always counts as a pass. */
enum class Heard { RIGHT, NEAR_MISS, UNSURE }

/** The word list for one talk turn. */
data class WordCheck(
    /** The word scored, lower case ("pig"). */
    val target: String,
    /** Its speech sound ("p"). */
    val sound: String,
    val pos: SoundPos,
    /** Up to 3 likely swaps ("big", "tig", "pi"). */
    val swaps: List<String>,
    /** Swaps that still count as right (a lisp-like "th" for s). */
    val passes: List<String>,
    /** Other words said in the same line ("big" and "pig" in "big pig"), so they aren't mistaken for swaps. */
    val others: List<String>,
    /** Rory said the word just before this try (false: he said it on his own, later). */
    val modeled: Boolean,
) {
    /** Is this sound scored at age 5 (stops, nasals, f; never r l s z sh ch j th v or clusters)? */
    val scored: Boolean get() = Speech5.scored(sound) && !Swaps.inCluster(target, sound)

    /** Everything the recogniser may choose from; "[unk]" is "not sure". */
    val grammar: List<String> get() = (listOf(target) + swaps + passes + others).distinct() + "[unk]"
}

/** One small saved record per try. No audio, no transcript: just this. */
data class TryRecord(
    val day: Int,
    val word: String,
    val sound: String,
    val pos: SoundPos,
    /** The word picked from the list, or "?" for not sure. */
    val heard: String,
    /** The recogniser's confidence, 0 to 1 (two decimals). */
    val conf: Float,
    val modeled: Boolean,
    val result: Heard,
    val scored: Boolean,
    /** Grown-up test mode: true "right", false "off", null not rated. */
    val rating: Boolean? = null,
) {
    fun encode(): String = listOf(
        day, clean(word), sound, pos.ordinal, clean(heard), (conf * 100).toInt(), if (modeled) 1 else 0,
        result.ordinal, if (scored) 1 else 0, when (rating) { true -> 1; false -> 0; null -> -1 },
    ).joinToString("|")

    companion object {
        private fun clean(s: String) = s.replace("|", "").replace("\n", "").take(30)

        fun decode(s: String): TryRecord? = runCatching {
            val f = s.split("|")
            TryRecord(
                f[0].toInt(), f[1], f[2], SoundPos.entries[f[3].toInt()], f[4], f[5].toInt() / 100f, f[6] == "1",
                Heard.entries[f[7].toInt()], f[8] == "1", when (f[9]) { "1" -> true; "0" -> false; else -> null },
            )
        }.getOrNull()

        /** The saved list, newest last, kept to [MAX] records. */
        fun decodeAll(s: String?): List<TryRecord> = s.orEmpty().split("\n").mapNotNull { if (it.isBlank()) null else decode(it) }
        fun encodeAll(list: List<TryRecord>): String = list.takeLast(MAX).joinToString("\n") { it.encode() }
        const val MAX = 1500
    }
}

/** What's fair to expect from a five-year-old. */
object Speech5 {
    /** Sounds never counted as errors at age 5. */
    val developing = setOf("r", "l", "s", "z", "sh", "ch", "j", "th", "v")
    fun scored(sound: String) = sound !in developing
}

/** Likely swaps for a word's speech sound, by spelling (the recogniser drops any that aren't real words). */
object Swaps {
    /** Sound to the sounds a young child often says instead ("" = the final sound dropped). */
    private val table = mapOf(
        "p" to listOf("b", "t", ""), "b" to listOf("p", "m", "d"), "m" to listOf("b", "n"),
        "t" to listOf("d", "k", ""), "d" to listOf("t", "g", "b", ""), "k" to listOf("t", "g", ""),
        "g" to listOf("d", "k"), "f" to listOf("p", "b", "s"), "s" to listOf("t", ""), "sh" to listOf("s", "ch"),
    )

    /** The letters that spell [sound] in [word], and where: the first place it's found. */
    fun find(word: String, sound: String): Pair<Int, Int>? {
        val w = word.lowercase()
        val spellings = when (sound) {
            "k" -> listOf("ck", "k", "c")
            "f" -> listOf("ph", "f")
            "s" -> listOf("ss", "s")
            else -> listOf(sound)
        }
        var best: Pair<Int, Int>? = null
        for (sp in spellings) {
            var i = w.indexOf(sp)
            while (i >= 0) {
                // "sh" and "ch" aren't s or c; "c" before e/i/y is an s.
                val bad = (sound == "s" && w.getOrNull(i + sp.length) == 'h') ||
                    (sound == "k" && sp == "c" && (w.getOrNull(i + 1) in setOf('e', 'i', 'y', 'h'))) ||
                    (sound == "t" && w.getOrNull(i + 1) == 'h') || (sound == "p" && w.getOrNull(i + 1) == 'h')
                if (!bad) {
                    if (best == null || i < best.first) best = i to sp.length
                    break
                }
                i = w.indexOf(sp, i + 1)
            }
        }
        return best
    }

    fun position(word: String, sound: String): SoundPos {
        val w = word.lowercase()
        val (i, n) = find(w, sound) ?: return SoundPos.START
        val end = i + n
        return when {
            i == 0 -> SoundPos.START
            end == w.length || (end == w.length - 1 && w.last() == 'e') -> SoundPos.END
            else -> SoundPos.MIDDLE
        }
    }

    private val vowels = setOf('a', 'e', 'i', 'o', 'u', 'y')

    /** True when [sound] sits in a consonant cluster ("st" in "star", "bl" in "blue", "sp" in "spoon"). */
    fun inCluster(word: String, sound: String): Boolean {
        val w = word.lowercase()
        val (i, n) = find(w, sound) ?: return false
        val before = w.getOrNull(i - 1)
        val after = w.getOrNull(i + n)
        fun cons(c: Char?) = c != null && c.isLetter() && c !in vowels && c != 'h'
        // A doubled letter ("happy") or a silent final e isn't a cluster.
        return (cons(before) && before != w[i]) || (cons(after) && after != w[i + n - 1] && !(after == 'e' && i + n + 1 == w.length))
    }

    /** Up to 3 likely swaps, real words first. */
    fun forWord(word: String, sound: String): List<String> {
        val w = word.lowercase()
        val (i, n) = find(w, sound) ?: return emptyList()
        val pos = position(w, sound)
        val out = mutableListOf<String>()
        for (to in table[sound].orEmpty()) {
            if (to.isEmpty()) {
                if (pos == SoundPos.END) out += w.removeRange(i, i + n).let { if (it.endsWith("e") && it.length > 2) it.dropLast(1) else it }
                continue
            }
            out += w.replaceRange(i, i + n, spell(to, w, i + n))
        }
        // In a cluster, one sound dropped is the likely near-miss ("top" for "stop").
        if (inCluster(w, sound)) {
            val other = if (w.getOrNull(i + n)?.let { it !in vowels } == true) i + n else i - 1
            if (other in w.indices) out += w.removeRange(other, other + 1)
            out += w.removeRange(i, i + n)
        }
        val swaps = out.filter { it.length >= 2 && it != w }.distinct()
        return (swaps.filter { it in RealWords.all } + swaps.filterNot { it in RealWords.all }).take(3)
    }

    /** A sound's usual spelling where it lands ("k" before a/o/u at the start is "c": "cop"). */
    private fun spell(sound: String, word: String, next: Int): String = when {
        sound == "k" && word.getOrNull(next) in setOf('a', 'o', 'u') -> "c"
        else -> sound
    }

    /** A lisp-like "th" for s still counts as right ("thun" for "sun"). */
    fun passes(word: String, sound: String): List<String> {
        if (sound != "s") return emptyList()
        val w = word.lowercase()
        val (i, n) = find(w, sound) ?: return emptyList()
        return listOf(w.replaceRange(i, i + n, "th"))
    }

    fun hasRealSwap(word: String, sound: String): Boolean = forWord(word, sound).any { it in RealWords.all }

    /** The word list for a talk turn on [line] (a word or a short line) practising [sound] (null: its first consonant). */
    fun check(line: String, sound: String?, modeled: Boolean): WordCheck? {
        val words = line.lowercase().split(Regex("[^a-z']+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return null
        val s = sound ?: firstSound(words.first()) ?: return null
        val target = words.firstOrNull { find(it, s) != null } ?: return null
        val others = words - target
        return WordCheck(target, s, position(target, s), forWord(target, s) - others.toSet(), passes(target, s), others, modeled)
    }

    /** The first consonant sound of a word, for talk turns with no target sound ("go" -> g). */
    fun firstSound(word: String): String? {
        val w = word.lowercase()
        for (digraph in listOf("sh", "ch", "th")) if (w.startsWith(digraph)) return digraph
        val c = w.firstOrNull { it.isLetter() && it !in vowels } ?: return null
        return when (c) { 'c' -> if (w.getOrNull(w.indexOf(c) + 1) in setOf('e', 'i', 'y')) "s" else "k"; 'q' -> "k"; 'x' -> null; else -> c.toString() }
    }
}

/** Short real words, for picking practice words whose swaps are real words too. */
object RealWords {
    private const val COMMON = "bat pat mat cat hat rat sat fat tat vat bad pad mad dad had sad lad tad " +
        "bin pin tin din fin win kin sin bit pit tit kit sit fit hit lit wit dig big pig fig wig gig jig " +
        "bed red fed led wed ted bet pet met net get set let jet wet vet beg peg leg keg " +
        "bog dog fog hog log jog cog bop pop top mop hop cop shop chop stop sop " +
        "bun pun ton tun done dun gun fun sun run nun bug dug hug mug pug rug tug jug " +
        "cup pup tub cub dub pub sub rub bus fuss bum gum hum mum sum come " +
        "bee pea tea key knee me see she sea free tree three ski " +
        "bow toe go doe no so low row sew mow tow foe hoe " +
        "boo moo too do two who shoe zoo you new few " +
        "bay pay day may say way hay ray lay gay kay " +
        "bye pie tie die my sigh high lie why fly buy guy " +
        "boat goat coat note moat tote vote bone tone cone phone home dome comb " +
        "bake cake take make lake fake rake wake " +
        "ball doll tall call fall wall hall mall pall gall " +
        "bell dell tell sell fell well yell shell " +
        "bike pike hike like mike " +
        "bear pear tear dare hair care fair share chair " +
        "boot moot toot root suit shoot " +
        "bag tag gag nag rag wag sag " +
        "bus pus " +
        "door more four poor tore core pour sore shore " +
        "feet beat meat seat heat neat peat " +
        "fan pan ban man tan can van ran " +
        "fish dish wish " +
        "foot put book cook look hook took " +
        "game came name same tame " +
        "gate date mate late hate " +
        "goose moose loose " +
        "kite bite white might night tight " +
        "milk silk mill bill pill till hill fill will " +
        "moon noon soon tune dune " +
        "mouse house " +
        "nose rose hose toes " +
        "sock dock lock rock knock " +
        "soap rope hope " +
        "star tar car bar far jar " +
        "sock suck tuck duck luck " +
        "spoon soon poon " +
        "snake sake make " +
        "boat bow toad told cold gold " +
        "bad bag back pack tack sack " +
        "cub cup cut " +
        "sheep seep cheap jeep keep deep beep peep " +
        "ship sip chip dip tip hip lip rip zip " +
        "shoe sue chew " +
        "sun thun some tongue " +
        "pig pick pink " +
        "map mat man mad mop "
    val all: Set<String> by lazy {
        (COMMON.split(" ").filter { it.isNotBlank() } +
            SpeechContent.sounds.flatMap { s -> (s.words + s.phrases).flatMap { it.text.lowercase().split(" ") } } +
            StoryWords.all.flatMap { sw -> sw.words.flatMap { it.lowercase().split(Regex("[^a-z]+")) } }).filter { it.length >= 2 }.toSet()
    }
}

/** Turns what the recogniser picked into right / near-miss / unsure. */
object Judge {
    /** Confidence needed to call it right, and (higher) to call it a near-miss. */
    const val RIGHT_CONF = 0.55f
    const val NEAR_CONF = 0.7f
    /** A near-miss this sure may be modelled (when the grown-up has turned that on). */
    const val CONFIDENT_NEAR = 0.8f

    /** [heard] the words the recogniser picked, each with its confidence. */
    fun judge(check: WordCheck, heard: List<Pair<String, Float>>): Pair<String, Pair<Heard, Float>> {
        val words = heard.filter { it.first != "[unk]" && it.first.isNotBlank() }
        val hit = words.filter { it.first == check.target || it.first in check.passes }.maxByOrNull { it.second }
        if (hit != null) return hit.first to ((if (hit.second >= RIGHT_CONF) Heard.RIGHT else Heard.UNSURE) to hit.second)
        val swap = words.filter { it.first in check.swaps }.maxByOrNull { it.second }
        if (swap != null) return swap.first to ((if (swap.second >= NEAR_CONF) Heard.NEAR_MISS else Heard.UNSURE) to swap.second)
        return "?" to (Heard.UNSURE to (words.maxOfOrNull { it.second } ?: 0f))
    }
}

/** How Rory answers a try once the grown-up has turned near-miss modelling on. */
enum class Reply { CELEBRATE, MODEL_SOUND, ECHO }

/**
 * The gentle rules for modelling a sound in one sitting: only for scored sounds and confident
 * near-misses, at most 1 in 3 tries for any sound, never twice in a row, and not at all once his
 * tries get quieter or shorter (he may be tiring or put off).
 */
class Corrections {
    private val tries = mutableMapOf<String, Int>()
    private val corrected = mutableMapOf<String, Int>()
    private var lastWasCorrection = false
    private val effort = mutableListOf<Pair<Float, Int>>()
    var stopped = false
        private set

    /** Every try with sound: how loud (0 to 1) and how long (ms of voice) it was. */
    fun noteTry(sound: String, loudest: Float, voicedMs: Int) {
        tries[sound] = (tries[sound] ?: 0) + 1
        effort += loudest to voicedMs
        if (effort.size >= 6) {
            val first = effort.take(3)
            val last = effort.takeLast(3)
            fun avgL(l: List<Pair<Float, Int>>) = l.map { it.first }.average()
            fun avgT(l: List<Pair<Float, Int>>) = l.map { it.second }.average()
            if (avgL(last) < avgL(first) * 0.7 || avgT(last) < avgT(first) * 0.7) stopped = true
        }
    }

    /** May Rory model [sound] now (call after [noteTry] for this try)? */
    fun mayCorrect(sound: String): Boolean {
        if (stopped || lastWasCorrection) return false
        return ((corrected[sound] ?: 0) + 1) * 3 <= (tries[sound] ?: 0)
    }

    /** What this try got: a correction or not. */
    fun noteReply(sound: String, correction: Boolean) {
        if (correction) corrected[sound] = (corrected[sound] ?: 0) + 1
        lastWasCorrection = correction
    }

    companion object {
        /** Rory's reply to a judged try (modelling on). */
        fun reply(check: WordCheck, result: Heard, conf: Float, corrections: Corrections): Reply = when {
            result == Heard.RIGHT -> Reply.CELEBRATE
            result == Heard.NEAR_MISS && check.scored && conf >= Judge.CONFIDENT_NEAR && corrections.mayCorrect(check.sound) -> Reply.MODEL_SOUND
            else -> Reply.ECHO
        }
    }
}

/** Does the recogniser agree with the grown-up's ratings (test mode)? */
object Agreement {
    const val MIN_RATED = 30
    const val MIN_AGREE = 0.9f

    /** Rated tries where the recogniser made a call (right or near-miss). */
    fun called(records: List<TryRecord>) = records.filter { it.rating != null && it.result != Heard.UNSURE }

    fun rate(records: List<TryRecord>): Float {
        val c = called(records)
        if (c.isEmpty()) return 0f
        return c.count { (it.rating == true) == (it.result == Heard.RIGHT) }.toFloat() / c.size
    }

    fun modelingAllowed(records: List<TryRecord>) = called(records).size >= MIN_RATED && rate(records) >= MIN_AGREE
}

/** The grown-ups page's per-sound cards: an app estimate, never a speech assessment. */
object SoundCards {
    const val MIN_TRIES = 20

    data class Card(
        val sound: String,
        val tries: Int,
        /** Right out of the tries it made a call on, per position (null: too few to say). */
        val byPos: Map<SoundPos, Float?>,
        /** The same, per week over the last 4 weeks, oldest first (null: too few that week). */
        val weeks: List<Float?>,
        val overall: Float,
    )

    private fun rate(l: List<TryRecord>, min: Int = 3): Float? {
        val calls = l.filter { it.result != Heard.UNSURE }
        return if (calls.size < min) null else calls.count { it.result == Heard.RIGHT }.toFloat() / calls.size
    }

    /** Cards for scored sounds with about [MIN_TRIES] tries or more. */
    fun cards(records: List<TryRecord>, today: Int): List<Card> =
        records.filter { it.scored }.groupBy { it.sound }.filter { it.value.size >= MIN_TRIES }.map { (s, l) ->
            Card(
                s, l.size,
                SoundPos.entries.associateWith { p -> rate(l.filter { it.pos == p }) },
                (3 downTo 0).map { w -> rate(l.filter { (today - it.day) / 7 == w }) },
                rate(l, 1) ?: 1f,
            )
        }.sortedBy { SpeechContent.sounds.indexOfFirst { s -> s.id == it.sound } }

    /** 1 or 2 sounds to focus on (the lowest estimates under 80%), each with 3 practice words. */
    fun focus(cards: List<Card>): List<Pair<String, List<String>>> =
        cards.filter { it.overall < 0.8f }.sortedBy { it.overall }.take(2).map { c ->
            val weakest = c.byPos.filterValues { it != null }.minByOrNull { it.value!! }?.key
            val words = SpeechContent.sounds.firstOrNull { it.id == c.sound }?.words.orEmpty().map { it.text }
                .filter { !it.contains(' ') }
                .sortedWith(compareBy({ if (weakest != null && Swaps.position(it, c.sound) == weakest) 0 else 1 }, { if (Swaps.hasRealSwap(it, c.sound)) 0 else 1 }))
            c.sound to words.take(3)
        }
}
