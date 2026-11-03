package com.ranaaclskn.formfit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { ScreenTitle("Geçmiş", "Yaptığın tüm antrenmanlar ve tekrarların.") }
        if (all.isEmpty()) {
            item {
                Text(
                    "Henüz kayıtlı antrenman yok. Antrenman sekmesinden bir hareket seçip başla; bitirdiğinde burada görünecek.",
                    color = FfColors.TextMuted,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(20.dp),
                )
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
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("Tümü") }) }
                items(done) { (id, name) ->
                    FilterChip(selected = filter == id, onClick = { filter = id }, label = { Text(name) })
                }
            }
        }
        items(list, key = { it.id }) { s -> SessionRow(s) { onOpen(s.id) } }
    }
}

@Composable
private fun SessionRow(s: WorkoutSession, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(FfColors.Surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(s.exerciseName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Text(
                "${formatDateTime(s.startedAt)} · ${formatDuration(s.durationMs)}",
                color = FfColors.TextMuted,
                fontSize = 13.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("✓ ${s.correctCount} doğru", FfColors.Good)
                Chip("✗ ${s.incorrectCount} yanlış", FfColors.Bad)
            }
        }
        Text(
            "%${s.accuracy}",
            color = accuracyColor(s.accuracy),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun accuracyColor(a: Int) = when {
    a >= 80 -> FfColors.Good
    a >= 50 -> FfColors.Warn
    else -> FfColors.Bad
}

@Composable
fun SessionDetailScreen(sessionId: String, onBack: () -> Unit) {
    val all by HistoryRepository.sessions.collectAsState()
    val s = all.firstOrNull { it.id == sessionId }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri", tint = Color.White) }
            Text(
                s?.exerciseName ?: "Antrenman",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (s != null) {
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Sil", tint = FfColors.TextMuted) }
            }
        }
        if (s == null) {
            Text("Bu kayıt bulunamadı.", color = FfColors.TextMuted, modifier = Modifier.padding(20.dp))
            return@Column
        }
        Text(
            formatDateTime(s.startedAt),
            color = FfColors.TextMuted,
            modifier = Modifier.padding(start = 20.dp, bottom = 12.dp),
        )
        StatsRow(
            listOf(
                "Doğru" to "${s.correctCount}",
                "Yanlış" to "${s.incorrectCount}",
                "Doğruluk" to "%${s.accuracy}",
                "Süre" to formatDuration(s.durationMs),
            ),
        )
        val faults = s.topFaults()
        if (faults.isNotEmpty()) {
            SectionCard("En sık hatalar", faults.map { "${it.first} (${it.second}×)" }, FfColors.Warn)
        }
        RepList(s.reps.map { Triple(it.correct, it.durationMs, it.faults) })
    }

    if (confirmDelete && s != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Kayıt silinsin mi?") },
            text = { Text("${s.exerciseName} antrenmanı geçmişten kalıcı olarak silinecek.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    HistoryRepository.delete(s.id)
                    onBack()
                }) { Text("Sil", color = FfColors.Bad) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Vazgeç") } },
        )
    }
}
