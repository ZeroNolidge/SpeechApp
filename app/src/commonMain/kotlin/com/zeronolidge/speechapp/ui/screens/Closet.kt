package com.zeronolidge.speechapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import com.zeronolidge.speechapp.ui.components.GoButton
import com.zeronolidge.speechapp.ui.components.ContinueSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.logic.Cosmetic
import com.zeronolidge.speechapp.logic.Land
import com.zeronolidge.speechapp.logic.Slot
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.ui.Nav
import com.zeronolidge.speechapp.ui.Screen
import com.zeronolidge.speechapp.ui.art.Critter
import com.zeronolidge.speechapp.ui.art.HeroWithPet
import com.zeronolidge.speechapp.ui.art.Mood
import com.zeronolidge.speechapp.ui.art.Outfit
import com.zeronolidge.speechapp.ui.art.PaperStyle
import com.zeronolidge.speechapp.ui.art.Scenery
import com.zeronolidge.speechapp.ui.components.ChoiceTile
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.PagedChoices
import com.zeronolidge.speechapp.ui.components.ScreenFrame
import com.zeronolidge.speechapp.ui.components.SpeakOnOpen

/** One thing in the closet: a paper piece, or the "pick a different buddy" tile at the end. */
private sealed interface ClosetItem {
    data class Piece(val c: Cosmetic) : ClosetItem
    data object NewBuddy : ClosetItem
}

/**
 * The buddy's closet: every paper piece earned so far (never taken away). Tap a piece to put it
 * on, tap it again to take it off. Three pieces at a time, with big arrows for more.
 */
@Composable
fun ClosetScreen(nav: Nav) {
    val app = LocalApp.current
    val p = app.progress
    var cheer by remember { mutableStateOf(false) }
    val species = p.hero ?: Species.KID
    val items = p.pieces.map { ClosetItem.Piece(it) } + ClosetItem.NewBuddy
    SpeakOnOpen(
        (if (p.pieces.size <= 1) "Your buddy's closet! Finish adventures, and your buddy gets new paper hats, wings and capes."
        else "Your buddy's closet! Tap a piece to put it on. Tap it again to take it off.") +
            " Tap the big green arrow to go back to the map.",
    )
    // The present waiting here has been seen.
    LaunchedEffect(Unit) { app.newPiece = null }
    LaunchedEffect(cheer) {
        if (cheer) {
            kotlinx.coroutines.delay(1500)
            cheer = false
        }
    }

    fun tap(c: Cosmetic) {
        if (p.equippedIn(c.slot)?.id == c.id && c.slot != Slot.COLOR) {
            p.equip(c.slot, null)
            app.speaker.say("Off it goes!")
        } else {
            p.equip(c.slot, if (c.id == "color_default") null else c)
            cheer = true
            app.speaker.say("${c.name}! Looking good!")
        }
    }

    ScreenFrame(
        onHome = nav.home,
        background = { Scenery(Land.MEADOW, Modifier.fillMaxSize(), seed = 7) },
    ) {
        Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HeroWithPet(
                species, p.heroColor(species), p.outfit, p.pet,
                Modifier.weight(0.34f).fillMaxHeight(),
                if (cheer) Mood.CHEER else Mood.HAPPY,
            )
            PagedChoices(items, perPage = 3, modifier = Modifier.weight(0.66f).fillMaxHeight().padding(top = 30.dp, bottom = ContinueSize + 12.dp), spacing = 16.dp) { item, m ->
                when (item) {
                    is ClosetItem.Piece -> {
                        val c = item.c
                        val on = p.equippedIn(c.slot)?.id == c.id || (c.id == "color_default" && p.equippedIn(Slot.COLOR) == null)
                        ChoiceTile("", null, PaperStyle.chip, m, selected = on, onClick = { tap(c) }) { Preview(species, c) }
                    }
                    ClosetItem.NewBuddy -> ChoiceTile(
                        "🔄", null, Color(0xFFE1F5FE), m,
                        onClick = { app.speaker.say("Pick a buddy!"); nav.go(Screen.HeroPick) },
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔄", fontSize = 48.sp)
                            Row {
                                listOf(Species.PUPPY, Species.DINO).forEach {
                                    Critter(it, Modifier.fillMaxHeight(0.5f), animate = false)
                                }
                            }
                        }
                    }
                }
            }
        }
        // The green arrow: back to the map.
        GoButton(Modifier.align(Alignment.BottomEnd), size = ContinueSize, hint = false, onClick = nav.home)
        }
    }
}

/** Shows what a piece looks like on the buddy (or the pet itself). */
@Composable
private fun Preview(species: Species, c: Cosmetic) {
    val p = LocalApp.current.progress
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (c.slot) {
            Slot.PET -> Critter(c.pet!!, Modifier.fillMaxHeight(), animate = false)
            Slot.COLOR -> Critter(
                species, Modifier.fillMaxHeight(),
                Color(if (c.color != 0L) c.color else if (species == Species.KID) 0xFF5B7FDB else species.defaultColor),
                animate = false,
            )
            Slot.HAT -> Critter(species, Modifier.fillMaxHeight(), p.heroColor(species), Outfit(hat = c), animate = false)
            Slot.CAPE -> Critter(species, Modifier.fillMaxHeight(), p.heroColor(species), Outfit(cape = c), animate = false)
            Slot.WINGS -> Critter(species, Modifier.fillMaxHeight(), p.heroColor(species), Outfit(wings = c), animate = false)
            Slot.GLASSES -> Critter(species, Modifier.fillMaxHeight(), p.heroColor(species), Outfit(glasses = c), animate = false)
        }
    }
}
