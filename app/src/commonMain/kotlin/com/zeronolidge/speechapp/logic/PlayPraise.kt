package com.zeronolidge.speechapp.logic

import com.zeronolidge.speechapp.data.Praise

/**
 * What Rory says after a story beat. "Good talking!" stays only when he really made a sound.
 * A tap, "I said it!", or the turn moving on by itself keeps the story line and drops the claim.
 */
object StoryPraise {
    fun line(success: String, voice: Boolean): String {
        if (voice || !claimsTalking(success)) return success
        val stripped = success.replace("Good talking! ", "").replace("Good talking!", "")
            .replace(Regex(" +"), " ").trim()
        return stripped.ifEmpty { "Good trying!" }
    }

    private fun claimsTalking(line: String) = "talk" in ClipText.normalize(line)
}

/**
 * Praise after a say-a-word try. Speech is named only when a voice was actually heard.
 * A quiet mic try, or "I said it!" with no voice, cheers the trying.
 */
object TurnPraise {
    fun linesFor(heard: Boolean, micUsed: Boolean): List<String> = when {
        heard -> emptyList()
        micUsed -> listOf("Good trying!")
        else -> Praise.great
    }

    fun lineFor(heard: Boolean, micUsed: Boolean): String = linesFor(heard, micUsed).first()
}
