package com.ranaaclskn.formfit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ranaaclskn.formfit.resources.Res
import com.ranaaclskn.formfit.resources.barlow_bold
import com.ranaaclskn.formfit.resources.barlow_medium
import com.ranaaclskn.formfit.resources.barlow_regular
import com.ranaaclskn.formfit.resources.barlow_semibold
import com.ranaaclskn.formfit.resources.barlowcondensed_bold
import com.ranaaclskn.formfit.resources.barlowcondensed_extrabold
import com.ranaaclskn.formfit.resources.barlowcondensed_semibold
import org.jetbrains.compose.resources.Font

/** Siyah, beyaz ve gri tonlarından oluşan palet. Renk sadece doğru/yanlış tekrarı ayırmak için kullanılır. */
object FfColors {
    val Background = Color(0xFF080808)
    val Surface = Color(0xFF131313)
    val SurfaceHigh = Color(0xFF1C1C1C)
    val Card = Color(0xFF181818)
    val Border = Color(0xFF2A2A2A)
    val Text = Color(0xFFF5F5F5)
    val TextSecondary = Color(0xFFB0B0B0)
    val TextMuted = Color(0xFF7A7A7A)
    val Accent = Color(0xFFFFFFFF)
    val OnAccent = Color(0xFF080808)
    /** Doğru tekrar: grilere yakın, yumuşak yeşil. */
    val Good = Color(0xFF7DDC9A)
    /** Yanlış tekrar: yumuşak kırmızı. */
    val Bad = Color(0xFFFF7A7A)
    val Warn = Color(0xFFE6C36A)
}

/** Başlıklar için dar, güçlü yazı tipi. */
@Composable
fun displayFont() = FontFamily(
    Font(Res.font.barlowcondensed_semibold, FontWeight.SemiBold),
    Font(Res.font.barlowcondensed_bold, FontWeight.Bold),
    Font(Res.font.barlowcondensed_extrabold, FontWeight.ExtraBold),
)

/** Metinler için okunaklı yazı tipi. */
@Composable
fun bodyFont() = FontFamily(
    Font(Res.font.barlow_regular, FontWeight.Normal),
    Font(Res.font.barlow_medium, FontWeight.Medium),
    Font(Res.font.barlow_semibold, FontWeight.SemiBold),
    Font(Res.font.barlow_bold, FontWeight.Bold),
)

private val colors = darkColorScheme(
    primary = FfColors.Accent,
    onPrimary = FfColors.OnAccent,
    secondary = FfColors.TextSecondary,
    background = FfColors.Background,
    onBackground = FfColors.Text,
    surface = FfColors.Surface,
    onSurface = FfColors.Text,
    surfaceVariant = FfColors.SurfaceHigh,
    onSurfaceVariant = FfColors.TextSecondary,
    surfaceContainer = FfColors.Surface,
    surfaceContainerHigh = FfColors.SurfaceHigh,
    surfaceContainerHighest = FfColors.SurfaceHigh,
    outline = FfColors.Border,
    outlineVariant = FfColors.Border,
    secondaryContainer = FfColors.SurfaceHigh,
    onSecondaryContainer = FfColors.Text,
    error = FfColors.Bad,
)

@Composable
fun FormFitTheme(content: @Composable () -> Unit) {
    val display = displayFont()
    val body = bodyFont()
    val base = Typography()
    val typography = Typography(
        displayLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.ExtraBold, fontSize = 44.sp, lineHeight = 44.sp),
        displayMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, lineHeight = 38.sp),
        headlineLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 32.sp),
        headlineMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 26.sp),
        titleLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 24.sp),
        titleMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
        titleSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 25.sp),
        bodyMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
        bodySmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
        labelLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.6.sp),
        labelMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.4.sp),
        labelSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.6.sp),
        headlineSmall = base.headlineSmall.copy(fontFamily = display, fontWeight = FontWeight.Bold),
        displaySmall = base.displaySmall.copy(fontFamily = display, fontWeight = FontWeight.ExtraBold),
    )
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
