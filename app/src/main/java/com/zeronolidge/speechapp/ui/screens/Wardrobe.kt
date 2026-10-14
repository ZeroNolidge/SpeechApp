package com.zeronolidge.speechapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.logic.Catalog
import com.zeronolidge.speechapp.logic.Cosmetic
import com.zeronolidge.speechapp.logic.Land
import com.zeronolidge.speechapp.logic.Power
import com.zeronolidge.speechapp.logic.Slot
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.logic.heroSpecies
import com.zeronolidge.speechapp.ui.art.Critter
import com.zeronolidge.speechapp.ui.art.HeroWithPet
import com.zeronolidge.speechapp.ui.art.Mood
import com.zeronolidge.speechapp.ui.art.Outfit
import com.zeronolidge.speechapp.ui.art.Scenery
import com.zeronolidge.speechapp.ui.components.LevelPill
import com.zeronolidge.speechapp.ui.components.LocalApp
import com.zeronolidge.speechapp.ui.components.RoundIcon
import com.zeronolidge.speechapp.ui.theme.Kid

private enum class Tab(val emoji: String, val label: String) {
    HEROES("🦸", "Heroes"), HATS("🎩", "Hats"), CAPES("🧣", "Capes"), GLASSES("👓", "Glasses"),
    COLORS("🎨", "Colors"), PETS("🐾", "Pets"), POWERS("⚡", "Powers"),
}

private fun Tab.slot(): Slot? = when (this) {
    Tab.HATS -> Slot.HAT
    Tab.CAPES -> Slot.CAPE
    Tab.GLASSES -> Slot.GLASSES
    Tab.COLORS -> Slot.COLOR
    Tab.PETS -> Slot.PET
    else -> null
}

/** Dress up the hero, buy things with gems, swap heroes and look at powers. */
@Composable
fun WardrobeScreen(back: () -> Unit) {
    val app = LocalApp.current
    val p = app.progress
    var tab by remember { mutableStateOf(Tab.HATS) }
    var cheer by remember { mutableStateOf(false) }
    val species = p.hero ?: Species.KID
    LaunchedEffect(Unit) { app.speaker.say("Dress up time! You have ${p.gems} gems.") }
    LaunchedEffect(cheer) {
        if (cheer) {
            kotlinx.coroutines.delay(1500)
            cheer = false
        }
    }

    fun tapItem(c: Cosmetic) {
        val slot = c.slot
        when {
            p.owns(c) -> {
                if (p.equippedIn(slot)?.id == c.id && slot != Slot.COLOR) {
                    p.equip(slot, null)
                    app.speaker.say("Off it goes!")
                } else {
                    p.equip(slot, c)
                    cheer = true
                    app.speaker.say("${c.name}! Looking good!")
                }
            }
            p.heroLevel < c.level -> app.speaker.say("${c.name}. Reach level ${c.level} to get it!")
            p.gems >= c.price -> {
                p.buy(c)
                p.equip(slot, c)
                cheer = true
                app.celebrate("You bought the ${c.name}! Hooray!")
            }
            else -> app.speaker.say("The ${c.name} costs ${c.price} gems. You need ${c.price - p.gems} more. Go on an adventure to get more gems!")
        }
    }

    Scenery(Land.MEADOW, Modifier.fillMaxSize(), seed = 7) {
        Row(Modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // The hero, big.
            Column(Modifier.weight(0.38f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RoundIcon("⬅️", Color.White, size = 52.dp) { app.speaker.stop(); back() }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "💎 ${p.gems}",
                        fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Kid.ink,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White)
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                }
                HeroWithPet(
                    species, p.heroColor(species), p.outfit, p.pet,
                    Modifier.weight(1f).fillMaxWidth(),
                    if (cheer) Mood.CHEER else Mood.HAPPY,
                )
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(p.heroName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Kid.ink)
                    Spacer(Modifier.width(8.dp))
                    LevelPill(p.heroLevel)
                }
            }

            // Tabs and items.
            Column(
                Modifier
                    .weight(0.62f)
                    .fillMaxHeight()
                    .shadow(8.dp, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xF2FFFFFF))
                    .padding(10.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Tab.entries.forEach { t ->
                        Column(
                            Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (t == tab) Kid.highlight else Color(0xFFF3E5F5))
                                .clickable {
                                    tab = t
                                    app.speaker.say(t.label)
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(t.emoji, fontSize = 24.sp)
                            Text(t.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Kid.ink)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                when (tab) {
                    Tab.HEROES -> ItemGrid(heroSpecies) { sp ->
                        ItemCard(
                            label = sp.label,
                            selected = sp == species,
                            status = null,
                            onClick = {
                                p.chooseHero(sp)
                                cheer = true
                                app.speaker.say("You're a ${sp.label} now!")
                            },
                        ) { Critter(sp, Modifier.fillMaxHeight(), p.heroColor(sp), animate = false) }
                    }
                    Tab.POWERS -> ItemGrid(Power.entries.toList()) { power ->
                        val have = p.heroLevel >= power.level
                        ItemCard(
                            label = power.label,
                            selected = false,
                            status = if (have) null else "🔒 Lv ${power.level}",
                            dim = !have,
                            onClick = { app.speaker.say(if (have) "${power.label}! ${power.describe}" else "${power.label}. You'll learn it at level ${power.level}!") },
                        ) { Text(power.emoji, fontSize = 48.sp) }
                    }
                    else -> ItemGrid(Catalog.inSlot(tab.slot()!!)) { c ->
                        val owned = p.owns(c)
                        val locked = !owned && p.heroLevel < c.level
                        ItemCard(
                            label = c.name,
                            selected = p.equippedIn(c.slot)?.id == c.id || (c.id == "color_default" && p.equippedIn(Slot.COLOR) == null),
                            status = when {
                                owned -> null
                                locked -> "🔒 Lv ${c.level}"
                                else -> "💎 ${c.price}"
                            },
                            dim = locked,
                            onClick = { tapItem(c) },
                        ) { Preview(species, c) }
                    }
                }
            }
        }
    }
}

/** Shows what an item looks like on the hero (or the pet itself). */
@Composable
private fun Preview(species: Species, c: Cosmetic) {
    val p = LocalApp.current.progress
    when (c.slot) {
        Slot.PET -> Critter(c.pet!!, Modifier.fillMaxHeight(), animate = false)
        Slot.COLOR -> Critter(
            species, Modifier.fillMaxHeight(),
            Color(if (c.color != 0L) c.color else if (species == Species.KID) 0xFFFF7043 else species.defaultColor),
            animate = false,
        )
        Slot.HAT -> Critter(species, Modifier.fillMaxHeight(), p.heroColor(species), Outfit(hat = c), animate = false)
        Slot.CAPE -> Critter(species, Modifier.fillMaxHeight(), p.heroColor(species), Outfit(cape = c), animate = false)
        Slot.GLASSES -> Critter(species, Modifier.fillMaxHeight(), p.heroColor(species), Outfit(glasses = c), animate = false)
    }
}

@Composable
private fun <T> ItemGrid(items: List<T>, card: @Composable (T) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(108.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items) { card(it) }
    }
}

@Composable
private fun ItemCard(
    label: String,
    selected: Boolean,
    status: String?,
    dim: Boolean = false,
    onClick: () -> Unit,
    picture: @Composable () -> Unit,
) {
    Column(
        Modifier
            .aspectRatio(0.8f)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Color(0xFFE8F5E9) else Color.White)
            .border(if (selected) 4.dp else 1.dp, if (selected) Kid.good else Color(0xFFE0E0E0), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.weight(1f).fillMaxWidth().alpha(if (dim) 0.4f else 1f), contentAlignment = Alignment.Center) { picture() }
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Kid.ink, textAlign = TextAlign.Center, maxLines = 1)
        if (status != null) {
            Text(status, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF7C4DFF))
        } else if (selected) {
            Text("✓ On", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Kid.good)
        }
    }
}
