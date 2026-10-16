package com.zeronolidge.speechapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.zeronolidge.speechapp.res.Res
import com.zeronolidge.speechapp.res.andika_bold
import com.zeronolidge.speechapp.res.sniglet_extrabold
import com.zeronolidge.speechapp.res.sniglet_regular
import org.jetbrains.compose.resources.Font

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
val Sniglet: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.sniglet_regular, FontWeight.Normal),
        Font(Res.font.sniglet_regular, FontWeight.Medium),
        Font(Res.font.sniglet_extrabold, FontWeight.SemiBold),
        Font(Res.font.sniglet_extrabold, FontWeight.Bold),
        Font(Res.font.sniglet_extrabold, FontWeight.ExtraBold),
        Font(Res.font.sniglet_extrabold, FontWeight.Black),
    )

/**
 * Andika, a font made for children learning to read (SIL Open Font License): plain letter shapes,
 * a single-storey a and g. Every letter he has to recognise uses it, because Sniglet's rounded
 * small letters look like other things (its v looks like a heart, its g like a 9, its t like a +).
 */
val LetterFont: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.andika_bold, FontWeight.Normal),
        Font(Res.font.andika_bold, FontWeight.Bold),
        Font(Res.font.andika_bold, FontWeight.ExtraBold),
    )

private val scheme = lightColorScheme(
    primary = Color(0xFF7C4DFF),
    secondary = Color(0xFFFF7043),
    background = Kid.background,
    surface = Kid.card,
    onBackground = Kid.ink,
    onSurface = Kid.ink,
)

private fun typography(font: FontFamily) = Typography().let { t ->
    fun TextStyle.sniglet() = copy(fontFamily = font)
    t.copy(
        displayLarge = t.displayLarge.sniglet(),
        displayMedium = t.displayMedium.sniglet(),
        displaySmall = t.displaySmall.sniglet(),
        headlineLarge = TextStyle(fontFamily = font, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold),
        headlineMedium = TextStyle(fontFamily = font, fontSize = 30.sp, fontWeight = FontWeight.Bold),
        headlineSmall = t.headlineSmall.sniglet(),
        titleLarge = TextStyle(fontFamily = font, fontSize = 24.sp, fontWeight = FontWeight.Bold),
        titleMedium = t.titleMedium.sniglet(),
        titleSmall = t.titleSmall.sniglet(),
        bodyLarge = TextStyle(fontFamily = font, fontSize = 20.sp),
        bodyMedium = t.bodyMedium.sniglet(),
        bodySmall = t.bodySmall.sniglet(),
        labelLarge = t.labelLarge.sniglet(),
        labelMedium = t.labelMedium.sniglet(),
        labelSmall = t.labelSmall.sniglet(),
    )
}

@Composable
fun SpeechAppTheme(content: @Composable () -> Unit) {
    val font = Sniglet
    val type = remember(font) { typography(font) }
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}
