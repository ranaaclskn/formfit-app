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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ranaaclskn.formfit.data.HistoryRepository
import com.ranaaclskn.formfit.data.nowMillis
import com.ranaaclskn.formfit.model.Category
import com.ranaaclskn.formfit.model.EXERCISES
import com.ranaaclskn.formfit.model.Exercise
import com.ranaaclskn.formfit.model.Gear
import com.ranaaclskn.formfit.model.matches

private const val GYM_FILTER = "GYM"

private fun Exercise.number(): String = (EXERCISES.indexOf(this) + 1).toString().padStart(2, '0')

/** Antrenman sekmesi: hareketler iki sütunlu kartlar halinde. */
@Composable
fun WorkoutHomeScreen(onStart: (String) -> Unit) {
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val list = EXERCISES.filter {
        val inFilter = when (filter) {
            null -> true
            GYM_FILTER -> it.gear.isGym
            else -> it.category.name == filter
        }
        inFilter && it.matches(query)
    }
    val sessions by HistoryRepository.sessions.collectAsState()
    val weekAgo = nowMillis() - 7L * 24 * 60 * 60 * 1000
    val thisWeek = sessions.filter { it.startedAt >= weekAgo }
    val weekReps = thisWeek.sumOf { it.reps.size }
    val weekCorrect = thisWeek.sumOf { it.correctCount }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(2) }) {
            Column(Modifier.padding(top = 20.dp, bottom = 4.dp)) {
                Overline("FORMFIT · Antrenman")
                Spacer(Modifier.height(6.dp))
                Text("BUGÜN NE\nÇALIŞIYORUZ?", style = MaterialTheme.typography.displayLarge, color = FfColors.Text)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Hareketi seç, telefonu yerleştir. Kamera formunu izler, doğru ve yanlış tekrarlarını sayar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FfColors.TextSecondary,
                )
            }
        }
        item(span = { GridItemSpan(2) }) {
            WeekStrip(
                workouts = thisWeek.size,
                reps = weekReps,
                accuracy = if (weekReps == 0) null else weekCorrect * 100 / weekReps,
            )
        }
        // Arama çubuğu filtrelerin hemen üstünde: önce ara, sonra kategoriyle daralt
        item(span = { GridItemSpan(2) }) {
            ExerciseSearchField(query, { query = it }, Modifier.padding(top = 4.dp))
        }
        item(span = { GridItemSpan(2) }) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                item { PillOption("Tümü", filter == null) { filter = null } }
                item { PillOption("Salon aletleri", filter == GYM_FILTER) { filter = GYM_FILTER } }
                items(Category.entries) { c -> PillOption(c.title, filter == c.name) { filter = c.name } }
            }
        }
        if (list.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Text(
                    if (query.isNotBlank()) "\"$query\" ile eşleşen hareket bulunamadı." else "Bu filtrede hareket yok.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FfColors.TextMuted,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }
        items(list, key = { it.id }) { ex -> ExerciseTile(ex) { onStart(ex.id) } }
    }
}

@Composable
private fun WeekStrip(workouts: Int, reps: Int, accuracy: Int?) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(FfColors.Accent)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Overline("Bu hafta", color = FfColors.OnAccent.copy(alpha = 0.55f))
            Spacer(Modifier.height(2.dp))
            Text(
                if (workouts == 0) "İLK ANTRENMANINI BAŞLAT" else "$workouts ANTRENMAN · $reps TEKRAR",
                style = MaterialTheme.typography.titleLarge,
                color = FfColors.OnAccent,
            )
        }
        if (accuracy != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text("%$accuracy", style = MaterialTheme.typography.headlineLarge, color = FfColors.OnAccent)
                Overline("Doğruluk", color = FfColors.OnAccent.copy(alpha = 0.55f))
            }
        }
    }
}

@Composable
private fun ExerciseTile(ex: Exercise, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(FfColors.Card)
            .border(1.dp, FfColors.Border, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
    ) {
        ExerciseDemoView(
            ex.demo,
            playing = false,
            speed = 1f,
            showCue = false,
            stillProgress = 1f,
            modifier = Modifier.fillMaxWidth().aspectRatio(1.1f).padding(6.dp),
        )
        Row(
            Modifier.padding(start = 14.dp, end = 10.dp, top = 6.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Overline("${ex.number()} · ${if (ex.gear.isGym) ex.gear.title else ex.category.title}")
                Spacer(Modifier.height(3.dp))
                Text(
                    ex.titleUpper().substringBefore(" ("),
                    style = MaterialTheme.typography.titleLarge,
                    color = FfColors.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${ex.sets} × ${ex.reps} · ${ex.cameraAngle.title.substringBefore(" ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = FfColors.TextMuted,
                )
            }
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(FfColors.Accent),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.PlayArrow, "Başla", tint = FfColors.OnAccent, modifier = Modifier.size(22.dp)) }
        }
    }
}

/** Hareketler sekmesi: numaralı, kategorilere ayrılmış liste. */
@Composable
fun LibraryScreen(onOpen: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 28.dp)) {
        item {
            ScreenHeader(
                "Kütüphane",
                "Hareketler",
                "Her hareketin videosu, dikkat edilmesi gerekenler ve püf noktaları.",
            )
        }
        item { ExerciseSearchField(query, { query = it }, Modifier.padding(horizontal = 20.dp)) }
        if (query.isNotBlank()) {
            val found = EXERCISES.filter { it.matches(query) }
            item(key = "found") {
                Overline(
                    if (found.isEmpty()) "Eşleşen hareket yok" else "${found.size} hareket bulundu",
                    Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
                )
            }
            items(found, key = { "s-" + it.id }) { ex -> LibraryRow(ex) { onOpen(ex.id) } }
            return@LazyColumn
        }
        Category.entries.forEach { cat ->
            val inCat = EXERCISES.filter { it.category == cat }
            if (inCat.isEmpty()) return@forEach
            item(key = "h-${cat.name}") {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Overline(cat.title, color = FfColors.Text)
                    Spacer(Modifier.width(10.dp))
                    HorizontalDivider(Modifier.weight(1f), color = FfColors.Border)
                    Spacer(Modifier.width(10.dp))
                    Overline("${inCat.size} hareket")
                }
            }
            items(inCat, key = { it.id }) { ex -> LibraryRow(ex) { onOpen(ex.id) } }
        }
    }
}

@Composable
private fun LibraryRow(ex: Exercise, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                ex.number(),
                style = MaterialTheme.typography.headlineMedium,
                color = FfColors.TextMuted,
                modifier = Modifier.width(44.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(ex.titleUpper(), style = MaterialTheme.typography.titleLarge, color = FfColors.Text)
                Spacer(Modifier.height(3.dp))
                Text(
                    (listOfNotNull(ex.gear.title.takeIf { ex.gear != Gear.BODYWEIGHT }) + ex.muscles.take(3)).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = FfColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            ExerciseDemoView(
                ex.demo,
                playing = false,
                speed = 1f,
                showCue = false,
                stillProgress = 1f,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.width(10.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = FfColors.TextMuted, modifier = Modifier.size(20.dp))
        }
        HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = FfColors.Border)
    }
}
