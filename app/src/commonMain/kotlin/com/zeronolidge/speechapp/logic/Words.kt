package com.zeronolidge.speechapp.logic

/**
 * What the app remembers about each word the child has practised. Every practised word becomes
 * a sticker; a word the microphone heard clearly becomes a shiny sticker.
 */
data class WordStat(
    val text: String,
    val emoji: String,
    /** Honest tries, whether or not the microphone understood. */
    val tries: Int = 0,
    /** Times the microphone heard the word clearly. */
    val clear: Int = 0,
    /** Misses in a row (the microphone heard something, but not the word). */
    val missRun: Int = 0,
    val firstDay: Int = 0,
    val lastDay: Int = 0,
    /** On how many different days the word has been practised. */
    val days: Int = 0,
    /** Sticker number, so the sticker book shows stickers in the order they were earned. */
    val order: Int = 0,
) {
    val shiny get() = clear > 0

    fun encode() = listOf(text, emoji, tries, clear, missRun, firstDay, lastDay, days, order).joinToString(SEP)

    companion object {
        private const val SEP = "\u001E"

        fun decode(s: String): WordStat? {
            val f = s.split(SEP)
            if (f.size < 9) return null
            return runCatching {
                WordStat(f[0], f[1], f[2].toInt(), f[3].toInt(), f[4].toInt(), f[5].toInt(), f[6].toInt(), f[7].toInt(), f[8].toInt())
            }.getOrNull()
        }
    }
}

/** How a grown-up sees a word on the parent page. Deliberately no scores. */
enum class WordStatus { GOING_WELL, STILL_TRICKY, JUST_STARTED, NOT_HEARD }

/** The child's sticker book and word history. Plain data, so it is easy to test. */
data class WordBook(val words: Map<String, WordStat> = emptyMap()) {

    operator fun get(text: String) = words[text.lowercase()]

    /** Stickers in the order they were earned. */
    val stickers: List<WordStat> get() = words.values.sortedBy { it.order }

    /**
     * Records one honest try at [text] on day [today]. [clear] means the microphone heard it
     * clearly; [heard] means the microphone was used and heard something (so a miss counts).
     */
    fun record(text: String, emoji: String, today: Int, clear: Boolean, heard: Boolean): WordBook {
        val key = text.lowercase()
        val old = words[key] ?: WordStat(key, emoji, firstDay = today, order = (words.values.maxOfOrNull { it.order } ?: 0) + 1)
        val new = old.copy(
            emoji = emoji,
            tries = old.tries + 1,
            clear = old.clear + if (clear) 1 else 0,
            missRun = when {
                clear -> 0
                heard -> old.missRun + 1
                else -> old.missRun
            },
            lastDay = today,
            days = old.days + if (old.lastDay != today || old.tries == 0) 1 else 0,
        )
        return copy(words = words + (key to new))
    }

    /**
     * Words to bring back in a chapter on day [today]: practised on an earlier day, the trickiest
     * and longest-unseen first, so practice is spread over several days.
     */
    fun dueForReview(today: Int, limit: Int): List<WordStat> = words.values
        .filter { it.lastDay < today }
        .sortedWith(
            compareByDescending<WordStat> { status(it) == WordStatus.STILL_TRICKY }
                .thenBy { it.days }
                .thenBy { it.lastDay },
        )
        .take(limit)

    fun status(w: WordStat): WordStatus = when {
        w.clear == 0 && w.tries >= 3 && w.missRun >= 2 -> WordStatus.STILL_TRICKY
        w.missRun >= 2 -> WordStatus.STILL_TRICKY
        w.clear > 0 -> WordStatus.GOING_WELL
        w.tries < 3 -> WordStatus.JUST_STARTED
        else -> WordStatus.NOT_HEARD
    }

    fun encode(): Set<String> = words.values.map { it.encode() }.toSet()

    companion object {
        fun decode(set: Set<String>): WordBook =
            WordBook(set.mapNotNull { WordStat.decode(it) }.associateBy { it.text })
    }
}
