package com.zeronolidge.speechapp.logic

/**
 * When the microphone hears him say a word (any voice counts; no speech recognition), Rory says
 * the TARGET word back and adds one word, the way speech therapists model language:
 * "Dog! A big dog!", "Grandma! Hi, Grandma!", "Big red ball! Yay!".
 */
object Echo {
    private val describing = listOf("big", "little", "happy", "funny", "silly", "nice")

    /**
     * Just the expansion, said after Rory has said the target word back clearly and slowly:
     * "A big red ball!", "Hi, Grandma!", "Yay!".
     */
    fun expansion(text: String, turn: Int = 0): String {
        val t = text.trim()
        if (t.isEmpty() || ' ' in t) return "Yay!"
        if (t.first().isUpperCase()) return "Hi, ${t.replaceFirstChar { it.uppercase() }}!"
        val i = ((turn % describing.size) + describing.size) % describing.size
        val colour = colours[((turn / describing.size) % colours.size + colours.size) % colours.size]
        return "A ${describing[i]} $colour ${t.lowercase()}!"
    }

    private val colours = listOf("red", "blue", "yellow", "green")

    fun line(text: String, turn: Int = 0): String {
        val t = text.trim()
        if (t.isEmpty()) return "Yay!"
        val shown = t.replaceFirstChar { it.uppercase() }
        return when {
            // A short phrase: say it back and add a cheer.
            ' ' in t -> "$shown! Yay!"
            // A name (the grown-up's own words are often names): say hello.
            t.first().isUpperCase() -> "$shown! Hi, $shown!"
            else -> "$shown! A ${describing[((turn % describing.size) + describing.size) % describing.size]} ${t.lowercase()}!"
        }
    }
}
