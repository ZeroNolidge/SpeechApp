package com.zeronolidge.speechapp.ui

import com.zeronolidge.speechapp.data.SpeechSound
import com.zeronolidge.speechapp.ui.screens.PracticeKind

/** Every place the app can go. The app keeps a simple back stack of these; 🏠 always returns to the map. */
sealed interface Screen {
    data object Map : Screen
    /** The next chapter on the trail. */
    data object Chapter : Screen
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
}
