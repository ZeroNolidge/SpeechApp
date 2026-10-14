package com.zeronolidge.speechapp.ui

import com.zeronolidge.speechapp.data.SpeechSound
import com.zeronolidge.speechapp.logic.Land
import com.zeronolidge.speechapp.ui.screens.PracticeKind

/** Every place the app can go. The app keeps a simple back stack of these. */
sealed interface Screen {
    data object Map : Screen
    data class Quest(val land: Land, val mixed: Boolean = false) : Screen
    data object Wardrobe : Screen
    data object GrownUps : Screen

    data object Practice : Screen
    data class PracticeGame(val kind: PracticeKind) : Screen
    data object TalkMenu : Screen
    data class TalkPractice(val sound: SpeechSound) : Screen
    data object Alphabet : Screen
    data object NumberLine : Screen
}
