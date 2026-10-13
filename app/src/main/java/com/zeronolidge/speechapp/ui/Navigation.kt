package com.zeronolidge.speechapp.ui

import com.zeronolidge.speechapp.data.SpeechSound
import com.zeronolidge.speechapp.logic.LetterMode

/** Every place the app can go. The app keeps a simple back stack of these. */
sealed interface Screen {
    data object Home : Screen
    data object GrownUps : Screen

    data object TalkMenu : Screen
    data class TalkPractice(val sound: SpeechSound) : Screen

    data object LettersMenu : Screen
    data object Alphabet : Screen
    data class LetterGame(val mode: LetterMode) : Screen

    data object ReadMenu : Screen
    data object SoundItOut : Screen
    data object WhichWord : Screen

    data object NumbersMenu : Screen
    data object NumberLine : Screen
    data object Counting : Screen
    data class MathGame(val plus: Boolean) : Screen
    data object MoreOrLess : Screen

    data object PatternsMenu : Screen
    data object WhatsNext : Screen
    data object OddOneOutGame : Screen

    data object RobotMenu : Screen
    data class RobotGame(val relative: Boolean) : Screen
}
