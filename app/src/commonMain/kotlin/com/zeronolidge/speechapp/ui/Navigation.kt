package com.zeronolidge.speechapp.ui

import com.zeronolidge.speechapp.data.SpeechSound
import com.zeronolidge.speechapp.logic.Land
import com.zeronolidge.speechapp.ui.screens.PracticeKind

/** Every place the app can go. The app keeps a simple back stack of these; 🏠 always returns to the map. */
sealed interface Screen {
    data object Map : Screen
    /**
     * A chapter: the next one on the trail, or (with [visit]) a fresh story in an area the trail
     * has already reached, which doesn't move the trail on.
     */
    data class Chapter(val visit: Land? = null) : Screen
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
    /** Test hook only: a story say-a-word step. */
    data object SayCheck : Screen
}
