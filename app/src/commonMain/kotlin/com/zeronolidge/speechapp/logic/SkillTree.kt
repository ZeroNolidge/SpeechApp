package com.zeronolidge.speechapp.logic

import com.zeronolidge.speechapp.data.SpeechContent

/**
 * test59: the skill trees. One list of skills per area (talking, letters, numbers, thinking,
 * stories), in teaching order, shared by the grown-ups' "Skill tree" tab and the child's own
 * trees. Progress is a few counts per skill, saved on this device only (no names, no words that were
 * said, no identifiers).
 *
 * How a skill is mastered:
 *  - a sound: only when a grown-up taps "Mastered" (the six dots under it are the grown-up's own
 *    marks: on its own, start, middle, end of words, phrases, sentences);
 *  - a tap game: about 80% right first time on 2 different days ([AUTO_DAYS], [AUTO_RATE]);
 *  - ABC and 1 2 3 (no right or wrong answers): a grown-up's tap;
 *  - a story: playing it to the end.
 * Tries alone never master anything: on the child's tree they grow the fruit to at most [GROW_CAP].
 */
enum class SkillBranch(val label: String, val emoji: String, val treeLine: String) {
    TALKING("Talking", "🗣️", "This is your talking tree."),
    LETTERS("Letters", "🔤", "This is your letters tree."),
    NUMBERS("Numbers", "🔢", "This is your numbers tree."),
    THINKING("Thinking", "🧩", "This is your thinking tree."),
    STORIES("Stories", "📖", "This is your stories tree."),
}

enum class SkillTier(val label: String) { START("Start here"), NEXT("Next"), STRETCH("Stretch") }

enum class Mastery { GROWN_UP, TAP_GAMES, FINISHED }

data class SkillNode(
    val id: String,
    val branch: SkillBranch,
    /** On the grown-ups' tab, e.g. "/f/" or "Count". */
    val label: String,
    /** On the child's tree fruit: a letter for a sound, a picture otherwise. */
    val fruit: String,
    /** What Rory calls it ("your fff sound", "counting"). */
    val spoken: String,
    val tier: SkillTier,
    val mastery: Mastery,
    /** Talking: the sound ids this node covers. */
    val sounds: List<String> = emptyList(),
)

/** One day's tries at a skill: all tries, and (tap games) how many were judged and right first time. */
data class DayCount(val tries: Int = 0, val judged: Int = 0, val right: Int = 0)

data class SkillState(
    val tries: Int = 0,
    val days: Map<Int, DayCount> = emptyMap(),
    /** Talking: the grown-up's marks on the six dots ([SkillTree.LEVELS] indexes). */
    val levels: Set<Int> = emptySet(),
    val mastered: Boolean = false,
) {
    val lastDay: Int get() = days.keys.maxOrNull() ?: 0

    fun encode(): String = "$tries;${if (mastered) 1 else 0};${levels.sorted().joinToString("")};" +
        days.entries.sortedBy { it.key }.joinToString(",") { (d, c) -> "$d:${c.tries}:${c.judged}:${c.right}" }

    companion object {
        fun decode(s: String): SkillState? = runCatching {
            val p = s.split(';')
            SkillState(
                tries = p[0].toInt(),
                mastered = p[1] == "1",
                levels = p.getOrElse(2) { "" }.mapNotNull { it.digitToIntOrNull() }.toSet(),
                days = p.getOrElse(3) { "" }.split(',').filter { it.count { c -> c == ':' } == 3 }.associate { e ->
                    val (d, t, j, r) = e.split(':').map { it.toInt() }
                    d to DayCount(t, j, r)
                },
            )
        }.getOrNull()
    }
}

enum class SkillStatus { MASTERED, CURRENT, UPCOMING }

object SkillTree {
    /** Where the 🌳 stands on the island map (placed like a story stop, clear of everything). */
    val MAP_LAND = Land.FOREST

    /** Right first time on this share of a day's answers makes a good day... */
    const val AUTO_RATE = 0.8f
    /** ...with at least this many answers that day... */
    const val AUTO_MIN_PER_DAY = 5
    /** ...and this many good days masters a tap game. */
    const val AUTO_DAYS = 2
    /** Tries alone grow the fruit to at most this much (full and ripe only on mastery). */
    const val GROW_CAP = 0.8f
    /** About this many tries grow a fruit to [GROW_CAP]. */
    const val GROW_TRIES = 25
    /** Days kept per skill (enough for the week line and the 2-day rule). */
    const val KEEP_DAYS = 60

    /** The six dots under each sound, in order. */
    val LEVELS = listOf("On its own", "Start of words", "Middle of words", "End of words", "Phrases", "Sentences")
    /** Short tags drawn under those dots on the grown-ups tab. */
    val LEVEL_TAGS = listOf("alone", "start", "mid", "end", "phrase", "sentence")
    private val levelNext = listOf("on its own", "at the start of words", "in the middle of words", "at the end of words", "in phrases", "in sentences")

    /** Grown-ups: tap a sound's dot to fill or clear it. Does not master the sound. */
    fun toggleLevel(s: SkillState, level: Int): SkillState =
        s.copy(levels = if (level in s.levels) s.levels - level else s.levels + level)

    /**
     * What one grown-ups Skill tree row shows: one state, one control.
     * Not mastered: dots (if talking) and a "Mark mastered" button (grown-up skills).
     * Mastered: a faded row, a grey check, and a long-press to undo (not for finished stories).
     */
    data class ParentRow(
        val faded: Boolean,
        val showCheck: Boolean,
        val showDots: Boolean,
        val dotsEnabled: Boolean,
        val markMastered: Boolean,
        val holdToUndo: Boolean,
        val showAutoNote: Boolean,
    )

    fun parentRow(node: SkillNode, states: Map<String, SkillState>): ParentRow {
        val mastered = status(node, states) == SkillStatus.MASTERED
        return ParentRow(
            faded = mastered,
            showCheck = mastered,
            showDots = node.branch == SkillBranch.TALKING,
            dotsEnabled = node.branch == SkillBranch.TALKING && !mastered,
            markMastered = !mastered && node.mastery == Mastery.GROWN_UP,
            holdToUndo = mastered && node.mastery != Mastery.FINISHED,
            showAutoNote = !mastered && node.mastery == Mastery.TAP_GAMES,
        )
    }

    private fun sound(id: String, label: String, spoken: String, tier: SkillTier) =
        SkillNode("talk:$id", SkillBranch.TALKING, label, id, spoken, tier, Mastery.GROWN_UP, listOf(id))

    private fun game(kind: String, branch: SkillBranch, label: String, fruit: String, spoken: String, tier: SkillTier) =
        SkillNode("game:$kind", branch, label, fruit, spoken, tier, Mastery.TAP_GAMES)

    val nodes: List<SkillNode> = listOf(
        sound("p", "/p/", "your puh sound", SkillTier.START),
        sound("b", "/b/", "your buh sound", SkillTier.START),
        sound("m", "/m/", "your mmm sound", SkillTier.START),
        sound("t", "/t/", "your tuh sound", SkillTier.START),
        sound("d", "/d/", "your duh sound", SkillTier.START),
        sound("k", "/k/", "your kuh sound", SkillTier.START),
        sound("g", "/g/", "your guh sound", SkillTier.START),
        sound("f", "/f/", "your fff sound", SkillTier.NEXT),
        sound("sh", "/sh/", "your shh sound", SkillTier.NEXT),
        sound("s", "/s/", "your sss sound", SkillTier.NEXT),
        sound("l", "/l/", "your la la sound", SkillTier.NEXT),
        sound("v", "/v/", "your vvv sound", SkillTier.NEXT),
        sound("ch", "/ch/", "your choo choo sound", SkillTier.NEXT),
        SkillNode("talk:blends", SkillBranch.TALKING, "s-blends (sp, sk, sm)", "sp", "words like spoon and sky", SkillTier.STRETCH, Mastery.GROWN_UP, listOf("sp", "sk", "sm")),

        SkillNode("abc", SkillBranch.LETTERS, "ABC", "🔠", "your A B Cs", SkillTier.START, Mastery.GROWN_UP),
        game("BIG_SMALL", SkillBranch.LETTERS, "A to a", "🅰️", "big and little letters", SkillTier.START),
        game("SMALL_BIG", SkillBranch.LETTERS, "a to A", "🔡", "little and big letters", SkillTier.START),
        game("FIRST_SOUND", SkillBranch.LETTERS, "First sound", "👂", "first sounds", SkillTier.NEXT),
        game("WHICH_WORD", SkillBranch.LETTERS, "Which word?", "🖼️", "finding words", SkillTier.NEXT),
        game("SOUND_OUT", SkillBranch.LETTERS, "Sound it out", "🧱", "sounding out words", SkillTier.STRETCH),

        SkillNode("123", SkillBranch.NUMBERS, "1 2 3", "🔟", "your numbers", SkillTier.START, Mastery.GROWN_UP),
        game("COUNT", SkillBranch.NUMBERS, "Count", "🍎", "counting", SkillTier.START),
        game("MORE", SkillBranch.NUMBERS, "More?", "⚖️", "finding more", SkillTier.NEXT),
        game("ADD", SkillBranch.NUMBERS, "Add", "➕", "adding", SkillTier.NEXT),
        game("TAKE_AWAY", SkillBranch.NUMBERS, "Take away", "➖", "taking away", SkillTier.NEXT),
        game("COINS", SkillBranch.NUMBERS, "Market", "💰", "paying at the market", SkillTier.STRETCH),

        game("ODD", SkillBranch.THINKING, "Odd one out", "🔍", "odd one out", SkillTier.START),
        game("NEXT", SkillBranch.THINKING, "What's next?", "🔁", "what comes next", SkillTier.START),
        game("ROBOT_ARROWS", SkillBranch.THINKING, "Robot arrows", "⬆️", "robot arrows", SkillTier.NEXT),
        game("ROBOT_TURNS", SkillBranch.THINKING, "Robot turns", "↪️", "robot turns", SkillTier.STRETCH),
    ) + VoiceStories.all.map { s ->
        SkillNode("story:${s.id}", SkillBranch.STORIES, s.title, s.emoji, s.title, SkillTier.START, Mastery.FINISHED)
    }

    fun of(branch: SkillBranch): List<SkillNode> = nodes.filter { it.branch == branch }
    fun byId(id: String): SkillNode? = nodes.firstOrNull { it.id == id }

    /** The talking node a said word counts for: its sound's list, else how it starts. */
    fun talkNodeFor(word: String, soundId: String? = null): SkillNode? {
        val w = word.trim().lowercase()
        val id = soundId ?: SpeechContent.sounds.firstOrNull { s -> (s.words + s.phrases).any { it.text.lowercase() == w } }?.id
            ?: listOf("sh", "ch", "sp", "sk", "sm").firstOrNull { w.startsWith(it) }
            ?: w.firstOrNull()?.toString()
            ?: return null
        return of(SkillBranch.TALKING).firstOrNull { id in it.sounds }
    }

    fun goodDays(s: SkillState): Int = s.days.values.count { it.judged >= AUTO_MIN_PER_DAY && it.right >= AUTO_RATE * it.judged }

    /**
     * One try at [node] on [day]; [right] is right first time for a tap game's answer (null: a try
     * with no right or wrong, like saying a word). A tap game masters itself by the 2-day rule.
     */
    fun record(s: SkillState, node: SkillNode, day: Int, right: Boolean?): SkillState {
        val c = s.days[day] ?: DayCount()
        val nc = DayCount(c.tries + 1, c.judged + (if (right != null) 1 else 0), c.right + (if (right == true) 1 else 0))
        val days = (s.days + (day to nc)).filterKeys { it > day - KEEP_DAYS }
        val next = s.copy(tries = s.tries + 1, days = days)
        return if (node.mastery == Mastery.TAP_GAMES && !s.mastered && goodDays(next) >= AUTO_DAYS) next.copy(mastered = true) else next
    }

    /**
     * The skill in progress in [branch]: the unmastered one tried most recently (ties: more tries),
     * or, before any tries, the first unmastered one in order. Null when all are mastered.
     */
    fun current(branch: SkillBranch, states: Map<String, SkillState>): SkillNode? {
        val open = of(branch).filter { states[it.id]?.mastered != true }
        return open.filter { (states[it.id]?.tries ?: 0) > 0 }
            .maxWithOrNull(compareBy<SkillNode>({ states[it.id]!!.lastDay }, { states[it.id]!!.days[states[it.id]!!.lastDay]?.tries ?: 0 }))
            ?: open.firstOrNull()
    }

    fun status(node: SkillNode, states: Map<String, SkillState>): SkillStatus = when {
        states[node.id]?.mastered == true -> SkillStatus.MASTERED
        current(node.branch, states)?.id == node.id -> SkillStatus.CURRENT
        else -> SkillStatus.UPCOMING
    }

    /** The grown-ups' progress bar, 0 to 1, toward mastering [node]. */
    fun progress(node: SkillNode, s: SkillState?): Float {
        if (s == null) return 0f
        if (s.mastered) return 1f
        return when (node.mastery) {
            Mastery.GROWN_UP -> if (node.branch == SkillBranch.TALKING) s.levels.size / LEVELS.size.toFloat() else (s.tries / 40f).coerceAtMost(0.9f)
            Mastery.TAP_GAMES -> {
                val good = goodDays(s)
                // A good day is half the way; the best other day so far adds a little more.
                val best = s.days.values.filter { it.judged > 0 && !(it.judged >= AUTO_MIN_PER_DAY && it.right >= AUTO_RATE * it.judged) }
                    .maxOfOrNull { (it.right.toFloat() / it.judged) * (it.judged.coerceAtMost(AUTO_MIN_PER_DAY) / AUTO_MIN_PER_DAY.toFloat()) } ?: 0f
                ((good + best * 0.9f) / AUTO_DAYS).coerceIn(0f, 0.95f)
            }
            Mastery.FINISHED -> 0f
        }
    }

    /** How full the fruit for [node] is: tries grow it to [GROW_CAP], only mastery fills it. Never shrinks. */
    fun fruitGrowth(s: SkillState?): Float = when {
        s == null -> 0f
        s.mastered -> 1f
        else -> GROW_CAP * (s.tries.toFloat() / GROW_TRIES).coerceAtMost(1f)
    }

    /** Leaves on the tree: one per try in that area (never fewer, whatever days are skipped). */
    fun leaves(branch: SkillBranch, states: Map<String, SkillState>): Int = of(branch).sumOf { states[it.id]?.tries ?: 0 }

    /** The skill practised last in [branch] (for Rory to name), or null before any tries. */
    fun lastPracticed(branch: SkillBranch, states: Map<String, SkillState>): SkillNode? =
        of(branch).filter { (states[it.id]?.tries ?: 0) > 0 }
            .maxWithOrNull(compareBy<SkillNode>({ states[it.id]!!.lastDay }, { states[it.id]!!.days[states[it.id]!!.lastDay]?.tries ?: 0 }))

    private fun plural(n: Int, one: String, many: String) = "$n ${if (n == 1) one else many}"

    /** The grown-ups' one line at the top of a branch, e.g. "This week: 42 tries on /f/ over 4 days. Next up: /f/ at the start of words." */
    fun weekSummary(branch: SkillBranch, states: Map<String, SkillState>, today: Int): String {
        val cur = current(branch, states) ?: return "Everything here is mastered."
        val s = states[cur.id]
        val week = s?.days?.filterKeys { it in (today - 6)..today }.orEmpty()
        val tries = week.values.sumOf { it.tries }
        val next = nextUp(cur, s, states)
        return if (tries == 0) "No tries on ${cur.label} this week yet. Next up: $next."
        else "This week: ${plural(tries, "try", "tries")} on ${cur.label} over ${plural(week.size, "day", "days")}. Next up: $next."
    }

    private fun nextUp(cur: SkillNode, s: SkillState?, states: Map<String, SkillState>): String {
        if (cur.branch == SkillBranch.TALKING) {
            val lvl = LEVELS.indices.firstOrNull { it !in (s?.levels ?: emptySet()) } ?: return "${cur.label}, ready to mark mastered"
            return "${cur.label} ${levelNext[lvl]}"
        }
        if ((s?.tries ?: 0) == 0) return cur.label
        val list = of(cur.branch)
        val after = list.drop(list.indexOf(cur) + 1).firstOrNull { states[it.id]?.mastered != true }
        return after?.label ?: cur.label
    }

    // ---------- Saving: one line per skill, "id=state" ----------

    fun encodeAll(states: Map<String, SkillState>): String =
        states.entries.filter { byId(it.key) != null }.sortedBy { it.key }.joinToString("\n") { "${it.key}=${it.value.encode()}" }

    fun decodeAll(s: String?): Map<String, SkillState> =
        s.orEmpty().split('\n').filter { '=' in it }.mapNotNull { line ->
            val id = line.substringBefore('=')
            if (byId(id) == null) null else SkillState.decode(line.substringAfter('='))?.let { id to it }
        }.toMap()

    // ---------- What Rory says on the tree (process praise only: what was practised, that trying continued) ----------

    fun openLine(branch: SkillBranch, states: Map<String, SkillState>): String =
        lastPracticed(branch, states)?.let { "You practiced ${it.spoken}! Every try grew a new leaf." }
            ?: "${branch.treeLine} Every try grows a new leaf!"

    fun fruitLine(node: SkillNode, states: Map<String, SkillState>): String = when (status(node, states)) {
        SkillStatus.MASTERED -> "You kept working on ${node.spoken}. Now this fruit is ripe!"
        else -> if ((states[node.id]?.tries ?: 0) > 0) "You practiced ${node.spoken}. Keep trying, and this fruit will grow!"
        else "This bud is ${node.spoken}. It's coming next!"
    }
}
