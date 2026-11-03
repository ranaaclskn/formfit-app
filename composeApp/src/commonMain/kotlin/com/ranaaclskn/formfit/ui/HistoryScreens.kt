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
import androidx.compose.ui.unit.sp
import com.ranaaclskn.formfit.data.practicedExercises
import com.ranaaclskn.formfit.data.exerciseProgress
import com.ranaaclskn.formfit.data.ProgressPoint
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
import com.ranaaclskn.formfit.data.decodeSet
import com.ranaaclskn.formfit.data.formatDateTime
import com.ranaaclskn.formfit.data.FaultEntry
import com.ranaaclskn.formfit.data.faultLog

@Composable
fun HistoryScreen(onOpen: (String) -> Unit) {
    val all by HistoryRepository.sessions.collectAsState()
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var tab by rememberSaveable { mutableStateOf(0) }
    val list = all.filter { filter == null || it.exerciseId == filter }
    val totalReps = list.sumOf { it.reps.size }
    val totalCorrect = list.sumOf { it.correctCount }

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 28.dp)) {
        item { ScreenHeader("Kayıtlar", "Geçmiş", "Yaptığın tüm antrenmanlar ve hataların, tarihleriyle.") }
        item { SegmentedTabs(listOf("Antrenmanlar", "Hatalarım", "İlerleme"), tab, { tab = it }) }
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
        if (tab == 1) {
            faultItems(list)
            return@LazyColumn
        }
        if (tab == 2) {
            item { ProgressSection(all) }
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

/** "Hatalarım": her hareketteki her hata, kaç kez ve hangi tarihlerde yapıldığı. */
private fun androidx.compose.foundation.lazy.LazyListScope.faultItems(sessions: List<WorkoutSession>) {
    val log = faultLog(sessions)
    val totalReps = sessions.sumOf { it.reps.size }
    val wrongReps = sessions.sumOf { it.incorrectCount }
    item {
        StatsRow(
            listOf(
                "Hata" to "${log.sumOf { it.total }}",
                "Yanlış tekrar" to "$wrongReps",
                "Hata oranı" to "%${if (totalReps == 0) 0 else wrongReps * 100 / totalReps}",
            ),
            modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
        )
    }
    if (log.isEmpty()) {
        item {
            Text(
                "Kayıtlı hata yok. Tüm tekrarların doğru formda — harika!",
                style = MaterialTheme.typography.bodyMedium,
                color = FfColors.TextSecondary,
                modifier = Modifier.padding(20.dp),
            )
        }
        return
    }
    items(log, key = { it.exerciseId + it.fault }) { e -> FaultCard(e) }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FaultCard(e: FaultEntry) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(FfColors.Card)
            .border(1.dp, FfColors.Border, RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Overline(upperExerciseName(e.exerciseId, e.exerciseName).substringBefore(" ("))
                Spacer(Modifier.height(4.dp))
                Text(e.fault, style = MaterialTheme.typography.titleMedium, color = FfColors.Text)
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("${e.total}×", style = MaterialTheme.typography.headlineLarge, color = FfColors.Text)
                Overline("toplam")
            }
        }
        Spacer(Modifier.height(10.dp))
        Overline("Son: ${formatDateTime(e.lastDate)}", color = FfColors.TextSecondary)
        Spacer(Modifier.height(10.dp))
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            e.occurrences.take(8).forEach { o ->
                Text(
                    "${formatDateTime(o.date).substringBefore(",")} · ${o.count}×",
                    style = MaterialTheme.typography.labelMedium,
                    color = FfColors.TextSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, FfColors.Border, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
            if (e.occurrences.size > 8) Overline("+${e.occurrences.size - 8} gün")
        }
    }
}

/** "İlerleme": hareket seçilir, son antrenmanlardaki doğruluk grafiği ve yorum gösterilir. */
@Composable
private fun ProgressSection(sessions: List<WorkoutSession>) {
    val practiced = practicedExercises(sessions)
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val id = selected?.takeIf { s -> practiced.any { it.first == s } } ?: practiced.firstOrNull()?.first ?: return
    val progress = exerciseProgress(sessions, id)
    Column {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(practiced) { (exId, name) ->
                PillOption(upperExerciseName(exId, name).substringBefore(" ("), exId == id) { selected = exId }
            }
        }
        StatsRow(
            listOf(
                "İlk" to (progress.first?.let { "%$it" } ?: "–"),
                "Son" to (progress.last?.let { "%$it" } ?: "–"),
                "En iyi" to (progress.best?.let { "%$it" } ?: "–"),
            ),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(FfColors.Card)
                .border(1.dp, FfColors.Border, RoundedCornerShape(22.dp))
                .padding(18.dp),
        ) {
            Text("DOĞRULUK", style = MaterialTheme.typography.titleLarge, color = FfColors.Text)
            Overline("Son ${progress.points.size} antrenman · ${progress.totalReps} tekrar")
            Spacer(Modifier.height(16.dp))
            AccuracyChart(progress.points)
        }
        progress.trend()?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = FfColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
    }
}

/** Her antrenman için bir sütun: yükseklik doğruluk yüzdesi, üstünde değer, altında tarih. */
@Composable
private fun AccuracyChart(points: List<ProgressPoint>) {
    Row(Modifier.fillMaxWidth().height(200.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        points.forEachIndexed { i, p ->
            val latest = i == points.lastIndex
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("%${p.accuracy}", style = MaterialTheme.typography.labelSmall, color = if (latest) FfColors.Text else FfColors.TextMuted, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.7f)
                            .fillMaxHeight((p.accuracy / 100f).coerceAtLeast(0.02f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(if (latest) FfColors.Accent else FfColors.TextMuted.copy(alpha = 0.5f)),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    formatDateTime(p.date).substringBefore(" 20").substringBefore(","),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                    color = FfColors.TextMuted,
                    maxLines = 1,
                )
            }
        }
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
            if (s.planName != null) {
                Overline(s.planName + (s.setNumber?.let { " · Set ${decodeSet(it)}" } ?: ""))
            }
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
