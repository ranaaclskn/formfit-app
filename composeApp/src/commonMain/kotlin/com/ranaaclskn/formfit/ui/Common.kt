package com.ranaaclskn.formfit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Küçük, harf aralıklı, gri üst başlık: "ANTRENMAN", "ALT VÜCUT" gibi. */
@Composable
fun Overline(text: String, modifier: Modifier = Modifier, color: Color = FfColors.TextMuted) {
    Text(text.trUpper(), style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier)
}

/** Sekme sayfalarının üst kısmı. */
@Composable
fun ScreenHeader(overline: String, title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)) {
        Overline(overline)
        Spacer(Modifier.height(6.dp))
        Text(title.trUpper(), style = MaterialTheme.typography.displayMedium, color = FfColors.Text)
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = FfColors.TextSecondary)
        }
    }
}

/** Çerçeveli etiket; [filled] ise beyaz zemin. */
@Composable
fun Chip(text: String, modifier: Modifier = Modifier, filled: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = if (filled) FfColors.OnAccent else FfColors.TextSecondary,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) FfColors.Accent else Color.Transparent)
            .border(1.dp, if (filled) FfColors.Accent else FfColors.Border, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** Seçilebilir hap düğme (kategori, hız, filtre). */
@Composable
fun PillOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text.trUpper(),
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) FfColors.OnAccent else FfColors.TextSecondary,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) FfColors.Accent else FfColors.Surface)
            .border(1.dp, if (selected) FfColors.Accent else FfColors.Border, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = FfColors.Accent, contentColor = FfColors.OnAccent),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text.trUpper(), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, FfColors.Border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = FfColors.Text),
    ) { Text(text.trUpper(), style = MaterialTheme.typography.labelLarge) }
}

/** Yuvarlak, gri zeminli ikon düğmesi (geri, kapat vb.). */
@Composable
fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(FfColors.SurfaceHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = FfColors.Text, modifier = Modifier.size(22.dp)) }
}

enum class Bullet { NUMBER, CAUTION, TIP, DOT }

/** Başlıklı madde listesi kartı. */
@Composable
fun SectionCard(title: String, items: List<String>, bullet: Bullet, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(FfColors.Card)
            .border(1.dp, FfColors.Border, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title.trUpper(), style = MaterialTheme.typography.titleLarge, color = FfColors.Text)
        items.forEachIndexed { i, item ->
            Row(verticalAlignment = Alignment.Top) {
                when (bullet) {
                    Bullet.NUMBER -> Text(
                        (i + 1).toString().padStart(2, '0'),
                        style = MaterialTheme.typography.titleLarge,
                        color = FfColors.TextMuted,
                        modifier = Modifier.width(34.dp),
                    )
                    Bullet.CAUTION, Bullet.TIP -> Box(
                        Modifier
                            .padding(top = 1.dp, end = 12.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (bullet == Bullet.CAUTION) FfColors.Accent else FfColors.SurfaceHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (bullet == Bullet.CAUTION) "!" else "+",
                            color = if (bullet == Bullet.CAUTION) FfColors.OnAccent else FfColors.Text,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                    }
                    Bullet.DOT -> Box(
                        Modifier
                            .padding(top = 8.dp, end = 14.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(FfColors.TextMuted),
                    )
                }
                Text(item, style = MaterialTheme.typography.bodyMedium, color = FfColors.TextSecondary)
            }
        }
    }
}

/** Yan yana büyük rakamlı istatistik kutuları. */
@Composable
fun StatsRow(stats: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        stats.forEach { (label, value) ->
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(FfColors.Card)
                    .border(1.dp, FfColors.Border, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp),
            ) {
                Text(value, style = MaterialTheme.typography.headlineLarge, color = FfColors.Text, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Overline(label)
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

/** Türkçe büyük harf: "i" → "İ", "ı" → "I". Standart uppercase() "i"yi "I" yapar. */
fun String.trUpper(): String = replace('i', 'İ').uppercase()

/** Hareketin büyük harfli adı. */
fun com.ranaaclskn.formfit.model.Exercise.titleUpper(): String = upperName ?: name.trUpper()

/** Kayıtlı antrenmandaki hareketin büyük harfli adı. */
fun upperExerciseName(id: String, name: String): String =
    com.ranaaclskn.formfit.model.exerciseById(id)?.titleUpper() ?: name.trUpper()
