package com.zeronolidge.speechapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.ui.art.Critter
import com.zeronolidge.speechapp.ui.art.PaperStyle
import com.zeronolidge.speechapp.ui.art.paperCard

/** The buddy's face on a raised paper button, top-left of the map. Tapping it opens the closet. */
@Composable
fun HeroBadge(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalApp.current.progress
    val species = p.hero ?: Species.KID
    PaperButton(PaperStyle.chip, modifier.height(CornerButtonSize), corner = 999.dp, onClick = onClick) {
        Row(
            Modifier.padding(start = 6.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(CornerButtonSize - 12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE1F5FE)),
                contentAlignment = Alignment.Center,
            ) {
                Critter(species, Modifier.fillMaxHeight(), p.heroColor(species), p.outfit, animate = false)
            }
            Spacer(Modifier.width(10.dp))
            Text("🎒", fontSize = 34.sp)
        }
    }
}

/** A comic-style speech bubble: crisp words on plain paper. Tapping it reads it again. */
@Composable
fun SpeechBubble(text: String, modifier: Modifier = Modifier, fontSize: Int = 20, tailLeft: Boolean = true, onClick: () -> Unit) {
    Column(modifier) {
        PaperButton(PaperStyle.chip, corner = 22.dp, onClick = onClick) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text, fontSize = fontSize.sp, color = PaperStyle.ink, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f, fill = false))
            }
        }
        Canvas(
            Modifier
                .padding(start = if (tailLeft) 40.dp else 0.dp, end = if (tailLeft) 0.dp else 40.dp)
                .size(22.dp, 14.dp)
                .align(if (tailLeft) Alignment.Start else Alignment.End),
        ) {
            val p = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(if (tailLeft) size.width * 0.2f else size.width * 0.8f, size.height)
                close()
            }
            drawPath(p, PaperStyle.chip)
        }
    }
}

/** A word or name tag under a character. */
@Composable
fun NameTag(name: String, modifier: Modifier = Modifier) = PaperChip(name, modifier, fontSize = 16.sp)
