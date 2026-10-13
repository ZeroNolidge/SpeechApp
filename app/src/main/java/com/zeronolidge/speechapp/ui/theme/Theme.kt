package com.zeronolidge.speechapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Kid {
    val background = Color(0xFFFFF6E5)
    val ink = Color(0xFF3A2E5C)
    val card = Color.White
    val talk = Color(0xFFFF8A80)
    val letters = Color(0xFF82B1FF)
    val reading = Color(0xFFB388FF)
    val numbers = Color(0xFFFFD180)
    val patterns = Color(0xFFA7FFEB)
    val robot = Color(0xFFB9F6CA)
    val good = Color(0xFF66BB6A)
    val soft = Color(0xFFFFE0B2)
    val highlight = Color(0xFFFFEB3B)
    val grass = Color(0xFFDCEDC8)
    val grassDark = Color(0xFFC5E1A5)
}

private val scheme = lightColorScheme(
    primary = Color(0xFF7C4DFF),
    secondary = Color(0xFFFF7043),
    background = Kid.background,
    surface = Kid.card,
    onBackground = Kid.ink,
    onSurface = Kid.ink,
)

private val type = Typography(
    headlineLarge = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.ExtraBold),
    headlineMedium = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 20.sp),
)

@Composable
fun SpeechAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}
