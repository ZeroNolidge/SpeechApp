package com.zeronolidge.speechapp.logic

/**
 * Pre-made voice clips (OpenAI voices made once by tools/make_voice.py, never in the app): how a
 * sentence is matched to clips. The build script uses exactly the same rules, so keep the two in
 * step:
 *  - a clip's key is its text, normalized: lower case, only a-z, 0-9 and apostrophes, single
 *    spaces ([normalize]);
 *  - text is cut into phrases at . ! ? : ; and … ([phrases]);
 *  - each phrase is covered by the fewest clips, left to right; words with no clip (his name, the
 *    grown-up's own words) are said by the device's voice ([plan]).
 */
object ClipText {
    private val notKept = Regex("[^a-z0-9' ]")
    private val spaces = Regex(" +")
    private val breaks = Regex("[.!?:;…]+")

    fun normalize(s: String): String =
        s.lowercase().replace('’', '\'').replace(notKept, " ").replace(spaces, " ").trim().trim('\'').trim()

    fun phrases(text: String): List<String> = text.split(breaks).map { it.trim() }.filter { normalize(it).isNotEmpty() }

    sealed interface Part {
        /** One or more clips, played back to back with no gap. */
        data class Clips(val keys: List<String>) : Part
        /** Words with no clip, said by the device's voice. */
        data class Speak(val text: String) : Part
    }

    /** How to say [text] with the clips that exist ([has]); consecutive clips are grouped. */
    fun plan(text: String, maxWords: Int = 40, has: (String) -> Boolean): List<Part> {
        val out = mutableListOf<Part>()
        fun addClip(k: String) {
            val last = out.lastOrNull()
            if (last is Part.Clips) out[out.size - 1] = Part.Clips(last.keys + k) else out += Part.Clips(listOf(k))
        }
        fun addWords(w: String) {
            val last = out.lastOrNull()
            if (last is Part.Speak) out[out.size - 1] = Part.Speak(last.text + " " + w) else out += Part.Speak(w)
        }
        for (phrase in phrases(text)) {
            val words = normalize(phrase).split(' ')
            val n = words.size
            // Fewest words left to the device voice first, then fewest pieces.
            val cost = IntArray(n + 1) { Int.MAX_VALUE / 2 }
            val from = IntArray(n + 1)
            val clip = BooleanArray(n + 1)
            cost[0] = 0
            for (j in 1..n) {
                if (cost[j - 1] + 100 < cost[j]) { cost[j] = cost[j - 1] + 100; from[j] = j - 1; clip[j] = false }
                for (i in maxOf(0, j - maxWords) until j) {
                    if (cost[i] + 1 < cost[j] && has(words.subList(i, j).joinToString(" "))) {
                        cost[j] = cost[i] + 1; from[j] = i; clip[j] = true
                    }
                }
            }
            val pieces = mutableListOf<Pair<Boolean, String>>()
            var j = n
            while (j > 0) {
                pieces += clip[j] to words.subList(from[j], j).joinToString(" ")
                j = from[j]
            }
            pieces.reversed().forEach { (isClip, s) -> if (isClip) addClip(s) else addWords(s) }
        }
        return out
    }
}
