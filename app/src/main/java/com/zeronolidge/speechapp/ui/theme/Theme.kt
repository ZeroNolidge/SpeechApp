package com.zeronolidge.speechapp.ui.theme

import com.zeronolidge.speechapp.R

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Warm, bright construction-paper colours. */
object Kid {
    val background = Color(0xFFF5E6C8) // kraft paper
    val ink = Color(0xFF3B2A4A)
    val card = Color(0xFFFFF8EA) // cream paper
    val talk = Color(0xFFFF8A65)
    val letters = Color(0xFF6EC6F5)
    val reading = Color(0xFFB98AE8)
    val numbers = Color(0xFFFFC94A)
    val patterns = Color(0xFF5FD3B0)
    val robot = Color(0xFF8EDB6A)
    val good = Color(0xFF5BBF4A)
    val soft = Color(0xFFFFD9A8)
    val highlight = Color(0xFFFFD93D)
    val grass = Color(0xFFCDE8A6)
    val grassDark = Color(0xFFB5DB86)
}

/** Sniglet: a rounded, crafty font that suits paper cut-outs (SIL Open Font License). */
val Sniglet = FontFamily(
    Font(R.font.sniglet_regular, FontWeight.Normal),
    Font(R.font.sniglet_regular, FontWeight.Medium),
    Font(R.font.sniglet_extrabold, FontWeight.SemiBold),
    Font(R.font.sniglet_extrabold, FontWeight.Bold),
    Font(R.font.sniglet_extrabold, FontWeight.ExtraBold),
    Font(R.font.sniglet_extrabold, FontWeight.Black),
)

private val scheme = lightColorScheme(
    primary = Color(0xFF7C4DFF),
    secondary = Color(0xFFFF7043),
    background = Kid.background,
    surface = Kid.card,
    onBackground = Kid.ink,
    onSurface = Kid.ink,
)

private val type = Typography().let { t ->
    fun TextStyle.sniglet() = copy(fontFamily = Sniglet)
    t.copy(
        displayLarge = t.displayLarge.sniglet(),
        displayMedium = t.displayMedium.sniglet(),
        displaySmall = t.displaySmall.sniglet(),
        headlineLarge = TextStyle(fontFamily = Sniglet, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold),
        headlineMedium = TextStyle(fontFamily = Sniglet, fontSize = 30.sp, fontWeight = FontWeight.Bold),
        headlineSmall = t.headlineSmall.sniglet(),
        titleLarge = TextStyle(fontFamily = Sniglet, fontSize = 24.sp, fontWeight = FontWeight.Bold),
        titleMedium = t.titleMedium.sniglet(),
        titleSmall = t.titleSmall.sniglet(),
        bodyLarge = TextStyle(fontFamily = Sniglet, fontSize = 20.sp),
        bodyMedium = t.bodyMedium.sniglet(),
        bodySmall = t.bodySmall.sniglet(),
        labelLarge = t.labelLarge.sniglet(),
        labelMedium = t.labelMedium.sniglet(),
        labelSmall = t.labelSmall.sniglet(),
    )
}

@Composable
fun SpeechAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}
