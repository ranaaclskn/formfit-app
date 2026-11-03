package com.ranaaclskn.formfit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ranaaclskn.formfit.data.HistoryRepository
import com.ranaaclskn.formfit.data.WorkoutSession
import com.ranaaclskn.formfit.data.formatDateTime

@Composable
fun HistoryScreen(onOpen: (String) -> Unit) {
    val all by HistoryRepository.sessions.collectAsState()
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val list = all.filter { filter == null || it.exerciseId == filter }
    val totalReps = list.sumOf { it.reps.size }
    val totalCorrect = list.sumOf { it.correctCount }

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 28.dp)) {
        item { ScreenHeader("Kayıtlar", "Geçmiş", "Yaptığın tüm antrenmanlar, tekrar tekrar.") }
        if (all.isEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(FfColors.Card)
                        .border(1.dp, FfColors.Border, RoundedCornerShape(22.dp))
                        .padding(24.dp),
                ) {
                    Text("HENÜZ KAYIT YOK", style = MaterialTheme.typography.headlineMedium, color = FfColors.Text)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Antrenman sekmesinden bir hareket seç ve başla. Bitirdiğinde her tekrarın burada görünecek.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FfColors.TextSecondary,
                    )
                }
            }
            return@LazyColumn
        }
        item {
            StatsRow(
                listOf(
                    "Antrenman" to "${list.size}",
                    "Tekrar" to "$totalReps",
                    "Doğruluk" to "%${if (totalReps == 0) 0 else totalCorrect * 100 / totalReps}",
                ),
            )
        }
        item {
            val done = all.map { it.exerciseId to it.exerciseName }.distinct()
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { PillOption("Tümü", filter == null) { filter = null } }
                items(done) { (id, name) -> PillOption(upperExerciseName(id, name).substringBefore(" ("), filter == id) { filter = id } }
            }
        }
        items(list, key = { it.id }) { s -> SessionRow(s) { onOpen(s.id) } }
    }
}

@Composable
private fun SessionRow(s: WorkoutSession, onClick: () -> Unit) {
    // "3 Kas 2026, 18:42" → gün, ay, saat
    val parts = formatDateTime(s.startedAt).replace(",", "").split(" ")
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(FfColors.Card)
            .border(1.dp, FfColors.Border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .width(54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(FfColors.SurfaceHigh)
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(parts.getOrElse(0) { "" }.padStart(2, '0'), style = MaterialTheme.typography.headlineMedium, color = FfColors.Text)
            Overline(parts.getOrElse(1) { "" })
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(upperExerciseName(s.exerciseId, s.exerciseName).substringBefore(" ("), style = MaterialTheme.typography.titleLarge, color = FfColors.Text)
            Spacer(Modifier.height(2.dp))
            Text(
                "${s.correctCount} doğru · ${s.incorrectCount} yanlış · ${formatDuration(s.durationMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = FfColors.TextMuted,
            )
            Spacer(Modifier.height(8.dp))
            AccuracyBar(s.accuracy)
        }
        Spacer(Modifier.width(14.dp))
        Text("%${s.accuracy}", style = MaterialTheme.typography.headlineLarge, color = FfColors.Text)
    }
}

@Composable
private fun AccuracyBar(accuracy: Int) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(50))
            .background(FfColors.Border),
    ) {
        Box(
            Modifier
                .fillMaxWidth(accuracy / 100f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(FfColors.Accent),
        )
    }
}

@Composable
fun SessionDetailScreen(sessionId: String, onBack: () -> Unit) {
    val all by HistoryRepository.sessions.collectAsState()
    val s = all.firstOrNull { it.id == sessionId }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Geri", onBack)
            Spacer(Modifier.weight(1f))
            if (s != null) RoundIconButton(Icons.Filled.Delete, "Sil", { confirmDelete = true })
        }
        if (s == null) {
            Text("Bu kayıt bulunamadı.", color = FfColors.TextMuted, modifier = Modifier.padding(20.dp))
            return@Column
        }
        ScreenHeader(formatDateTime(s.startedAt), upperExerciseName(s.exerciseId, s.exerciseName))
        StatsRow(
            listOf(
                "Doğru" to "${s.correctCount}",
                "Yanlış" to "${s.incorrectCount}",
                "Doğruluk" to "%${s.accuracy}",
            ),
        )
        Spacer(Modifier.height(6.dp))
        val faults = s.topFaults()
        if (faults.isNotEmpty()) {
            SectionCard("En sık hatalar", faults.map { "${it.first}  ·  ${it.second}×" }, Bullet.CAUTION)
        }
        RepList(s.reps.map { Triple(it.correct, it.durationMs, it.faults) })
    }

    if (confirmDelete && s != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = FfColors.Surface,
            title = { Text("KAYIT SİLİNSİN Mİ?", style = MaterialTheme.typography.headlineMedium) },
            text = { Text("${s.exerciseName} antrenmanı geçmişten kalıcı olarak silinecek.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    HistoryRepository.delete(s.id)
                    onBack()
                }) { Text("SİL", color = FfColors.Bad, style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("VAZGEÇ", color = FfColors.Text, style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }
}
