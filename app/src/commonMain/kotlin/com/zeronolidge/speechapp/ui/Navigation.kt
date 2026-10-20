package com.zeronolidge.speechapp.ui

import com.zeronolidge.speechapp.data.SpeechSound
import com.zeronolidge.speechapp.logic.Land
import com.zeronolidge.speechapp.ui.screens.PracticeKind

/** Every place the app can go. The app keeps a simple back stack of these; 🏠 always returns to the map. */
sealed interface Screen {
    data object Map : Screen
    /**
     * A chapter: the next one on the trail, or (with [visit]) a fresh story in an area the trail
     * has already reached, which doesn't move the trail on. [replay]: a finished stop's ⭐ was
     * tapped; a fresh story in its land that never moves the trail on.
     */
    data class Chapter(val visit: Land? = null, val replay: Boolean = false) : Screen
    /** An area's own games, opened from its label or picture on the map (Meadow: counting, adding...). */
    data class LandGames(val land: Land) : Screen
    data object Closet : Screen
    data object Stickers : Screen
    /** The grown-up check in front of the grown-ups page. */
    data object Gate : Screen
    data object GrownUps : Screen
    data object HeroPick : Screen

    data object Practice : Screen
    data class PracticeGame(val kind: PracticeKind) : Screen
    data object TalkMenu : Screen
    data class TalkPractice(val sound: SpeechSound) : Screen
    data object Alphabet : Screen
    data object NumberLine : Screen
    data object Pip : Screen
    /** The crystal cave reward stop; [rest] ends with Rory's "let's rest". */
    data class EchoCave(val rest: Boolean = false) : Screen
    data object Rest : Screen
    /** A voice story (logic/VoiceStories.kt), from its stop on the map. */
    data class VoiceStory(val id: String, val startBeat: Int = 0) : Screen
    /** Test hook only: a story say-a-word step. */
    data object SayCheck : Screen
    /** Test hook only: a chapter step where he says [word] (a story word like "climb"). */
    data class SayBeatCheck(val word: String) : Screen
}

/**
 * Where he is, as a short saved key (the screen, and for a voice story its beat), so a page that
 * reloads by itself (the tablet's screen went off and the browser dropped the page) opens back
 * at the same place instead of the title screen. Null for places not worth returning to
 * (grown-up pages, test hooks, the rest screen).
 */
fun Screen.placeKey(): String? = when (this) {
    Screen.Map -> "map"
    is Screen.Chapter -> "chapter:" + (visit?.name ?: "") + (if (replay) ":replay" else "")
    is Screen.LandGames -> "land:" + land.name
    Screen.Closet -> "closet"
    Screen.Stickers -> "stickers"
    Screen.Practice -> "practice"
    is Screen.PracticeGame -> "game:" + kind.name
    Screen.TalkMenu -> "talk"
    is Screen.TalkPractice -> "talk:" + sound.id
    Screen.Alphabet -> "alphabet"
    Screen.NumberLine -> "numbers"
    Screen.Pip -> "pip"
    is Screen.EchoCave -> "cave:" + rest
    is Screen.VoiceStory -> "story:$id@$startBeat"
    else -> null
}

fun screenForPlace(key: String): Screen? = runCatching {
    val (kind, arg) = key.split(":", limit = 2).let { it[0] to it.getOrNull(1) }
    when (kind) {
        "map" -> Screen.Map
        "chapter" -> arg.orEmpty().split(":").let { parts ->
            Screen.Chapter(parts[0].takeIf { it.isNotEmpty() }?.let { Land.valueOf(it) }, replay = parts.getOrNull(1) == "replay")
        }
        "land" -> Screen.LandGames(Land.valueOf(arg!!))
        "closet" -> Screen.Closet
        "stickers" -> Screen.Stickers
        "practice" -> Screen.Practice
        "game" -> Screen.PracticeGame(PracticeKind.valueOf(arg!!))
        "talk" -> if (arg == null) Screen.TalkMenu else com.zeronolidge.speechapp.data.SpeechContent.sounds.firstOrNull { it.id == arg }?.let { Screen.TalkPractice(it) }
        "alphabet" -> Screen.Alphabet
        "numbers" -> Screen.NumberLine
        "pip" -> Screen.Pip
        "cave" -> Screen.EchoCave(arg == "true")
        "story" -> arg!!.split("@").let { Screen.VoiceStory(it[0], it.getOrNull(1)?.toIntOrNull() ?: 0) }
        else -> null
    }
}.getOrNull()
