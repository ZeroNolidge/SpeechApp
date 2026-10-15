package com.zeronolidge.speechapp.logic

/**
 * When the microphone hears him say a word (any voice counts; no speech recognition), Rory says
 * the TARGET word back and adds one word, the way speech therapists model language:
 * "Dog! A big dog!", "Grandma! Hi, Grandma!", "Big red ball! Yay!".
 */
object Echo {
    private val describing = listOf("big", "little", "happy", "funny", "silly", "nice")

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
