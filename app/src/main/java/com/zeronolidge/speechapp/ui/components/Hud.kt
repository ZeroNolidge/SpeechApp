package com.zeronolidge.speechapp.ui.components

import com.zeronolidge.speechapp.ui.art.paperCard
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.logic.Leveling
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.ui.art.Critter
import com.zeronolidge.speechapp.ui.theme.Kid

/** The hero's face, level, XP bar and gems, shown at the top of the map. */
@Composable
fun HeroBadge(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalApp.current.progress
    val species = p.hero ?: Species.KID
    Row(
        modifier
            .paperCard(Kid.card, 999.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFFE1F5FE)),
            contentAlignment = Alignment.Center,
        ) {
            Critter(species, Modifier.fillMaxHeight(), p.heroColor(species), p.outfit, animate = false)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.heroName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Kid.ink)
                Spacer(Modifier.width(8.dp))
                LevelPill(p.heroLevel)
            }
            Spacer(Modifier.height(4.dp))
            XpBar(Leveling.progress(p.xp), Modifier.width(140.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text("💎 ${p.gems}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Kid.ink)
    }
}

@Composable
fun LevelPill(level: Int) {
    Text(
        "Lv $level",
        fontSize = 14.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF7C4DFF))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
fun XpBar(fraction: Float, modifier: Modifier = Modifier, height: Dp = 12.dp) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(900), label = "xp")
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFEDE7F6)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFFFC107)),
        )
    }
}

/** A comic-style speech bubble. Tapping it reads it again. */
@Composable
fun SpeechBubble(text: String, modifier: Modifier = Modifier, fontSize: Int = 20, tailLeft: Boolean = true, onClick: () -> Unit) {
    Column(modifier) {
        Row(
            Modifier
                .paperCard(Kid.card, 22.dp)
                .clip(RoundedCornerShape(22.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, fontSize = fontSize.sp, color = Kid.ink, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f, fill = false))
            Spacer(Modifier.width(8.dp))
            Text("🔊", fontSize = (fontSize + 2).sp)
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
            drawPath(p, Color.White)
        }
    }
}
