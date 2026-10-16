package com.zeronolidge.speechapp.logic

/**
 * One quiet difficulty ladder (speech, numbers or patterns). He never sees it: no levels, no
 * numbers. After about 4 right out of the last 5, spread over at least two sessions, it steps up;
 * after 2 misses in a row it quietly steps back. Each step starts its count afresh.
 */
data class Ladder(val step: Int = 0, val recent: List<Pair<Int, Boolean>> = emptyList()) {
    fun record(right: Boolean, session: Int, maxStep: Int): Ladder {
        val r = (recent + (session to right)).takeLast(WINDOW)
        if (!right && r.size >= 2 && !r[r.size - 2].second) return Ladder((step - 1).coerceAtLeast(0))
        if (r.size == WINDOW && r.count { it.second } >= RIGHT_TO_STEP_UP && r.map { it.first }.distinct().size >= 2 && step < maxStep) {
            return Ladder(step + 1)
        }
        return Ladder(step.coerceIn(0, maxStep), r)
    }

    fun encode(): String = "$step;" + recent.joinToString(",") { "${it.first}:${if (it.second) 1 else 0}" }

    companion object {
        const val WINDOW = 5
        const val RIGHT_TO_STEP_UP = 4

        /** Speech: one word, then two words, then a short sentence. */
        const val SPEECH = "speech"
        /** Numbers: count to 5, then to 10, then add one. */
        const val NUMBERS = "numbers"
        /** Patterns: AB, then ABB, then ABC. */
        const val PATTERNS = "patterns"
        val MAX = mapOf(SPEECH to 2, NUMBERS to 2, PATTERNS to 2)

        fun decode(s: String?): Ladder {
            if (s.isNullOrBlank()) return Ladder()
            val (st, rest) = s.split(';', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            val recent = rest.split(',').filter { ':' in it }.mapNotNull { p ->
                val (a, b) = p.split(':')
                a.toIntOrNull()?.let { it to (b == "1") }
            }
            return Ladder(st.toIntOrNull() ?: 0, recent)
        }
    }
}
