package com.zeronolidge.speechapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.logic.Numbers
import com.zeronolidge.speechapp.ui.Nav
import com.zeronolidge.speechapp.ui.Screen
import com.zeronolidge.speechapp.ui.art.PaperStyle
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.PaperButton
import com.zeronolidge.speechapp.ui.components.PaperChip
import com.zeronolidge.speechapp.ui.components.ScreenFrame
import com.zeronolidge.speechapp.ui.components.SpeakOnOpen
import kotlin.random.Random

/**
 * A simple grown-up check in front of the settings: tap three numbers that are written as words
 * ("four, nine, two"). A child who can't read yet can't pass it by chance (1 in 729), and the
 * numbers are never said out loud. 🏠 is always there to go back.
 */
@Composable
fun ParentGateScreen(nav: Nav) {
    val app = LocalApp.current
    fun newCode() = List(3) { Random.nextInt(1, 10) }
    var code by remember { mutableStateOf(newCode()) }
    var entered by remember { mutableStateOf(listOf<Int>()) }
    SpeakOnOpen("This part is for grown-ups. Ask a grown-up to help, or tap the house to go back to the map.")
    ScreenFrame(onHome = nav.home) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
            PaperChip("Grown-ups: tap ${code.joinToString(", ") { Numbers.word(it) }}", fontSize = 26.sp)
            Text("●".repeat(entered.size) + "○".repeat(3 - entered.size), fontSize = 30.sp, color = PaperStyle.ink)
            (0..2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    (1..3).forEach { col ->
                        val n = row * 3 + col
                        PaperButton(PaperStyle.chip, Modifier.size(width = 96.dp, height = 70.dp), corner = 18.dp, onClick = {
                            val next = entered + n
                            when {
                                next != code.take(next.size) -> {
                                    // A wrong number quietly starts over with new numbers.
                                    entered = emptyList()
                                    code = newCode()
                                }
                                next.size == 3 -> {
                                    app.speaker.stop()
                                    nav.replace(Screen.GrownUps)
                                }
                                else -> entered = next
                            }
                        }) { Text("$n", fontSize = 30.sp, color = PaperStyle.ink) }
                    }
                }
            }
        }
    }
}
