package com.zeronolidge.speechapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.logic.IslandStory
import com.zeronolidge.speechapp.logic.Slot
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.ui.art.Critter
import com.zeronolidge.speechapp.ui.art.Outfit

/** Test hook only ("#test=costumes"): Rory wearing each island-story costume, for review. */
@Composable
fun CostumeSheet() {
    Column(
        Modifier.fillMaxSize().background(Color(0xFFFFF8E7)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IslandStory.places.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { pl ->
                    val c = pl.costume
                    val outfit = when (c.slot) {
                        Slot.HAT -> Outfit(hat = c)
                        Slot.CAPE -> Outfit(cape = c)
                        Slot.GLASSES -> Outfit(glasses = c)
                        else -> Outfit()
                    }
                    Column(Modifier.weight(1f).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Critter(Species.ROBOT, Modifier.fillMaxSize(), Color(0xFFFFCC80), outfit = outfit, animate = false)
                        }
                        Text("${pl.land.emoji} ${c.name}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3A2E5C))
                    }
                }
            }
        }
    }
}
