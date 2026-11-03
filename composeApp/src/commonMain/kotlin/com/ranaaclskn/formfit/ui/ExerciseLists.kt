package com.ranaaclskn.formfit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranaaclskn.formfit.model.Category
import com.ranaaclskn.formfit.model.EXERCISES
import com.ranaaclskn.formfit.model.Exercise

/** Antrenman sekmesi: hareket seçip kamerayla başlatma. */
@Composable
fun WorkoutHomeScreen(onStart: (String) -> Unit) {
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val list = EXERCISES.filter { filter == null || it.category.name == filter }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { ScreenTitle("Antrenman", "Hareketi seç, kamerayı aç. FormFit doğru ve yanlış tekrarlarını saysın.") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                item { CategoryChip("Tümü", filter == null) { filter = null } }
                items(Category.entries) { c -> CategoryChip(c.title, filter == c.name) { filter = c.name } }
            }
        }
        items(list, key = { it.id }) { ex ->
            ExerciseRow(ex, onClick = { onStart(ex.id) }) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(FfColors.Accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.PlayArrow, "Başla", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

/** Hareketler sekmesi: nasıl yapıldığını gösteren sayfalara giriş. */
@Composable
fun LibraryScreen(onOpen: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { ScreenTitle("Hareketler", "Hareketin videosunu izle, dikkat edilmesi gerekenleri ve püf noktalarını öğren.") }
        Category.entries.forEach { cat ->
            val inCat = EXERCISES.filter { it.category == cat }
            if (inCat.isEmpty()) return@forEach
            item(key = "h-${cat.name}") {
                Text(
                    cat.title.uppercase(),
                    color = FfColors.TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
                )
            }
            items(inCat, key = { it.id }) { ex ->
                ExerciseRow(ex, onClick = { onOpen(ex.id) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = FfColors.TextMuted)
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = FfColors.Accent,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@Composable
private fun ExerciseRow(ex: Exercise, onClick: () -> Unit, trailing: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(FfColors.Surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExerciseDemoView(
            ex.demo,
            playing = false,
            speed = 1f,
            showCue = false,
            modifier = Modifier.size(76.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(ex.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Text(
                "${ex.sets} set × ${ex.reps} tekrar · ${ex.cameraAngle.title}",
                color = FfColors.TextMuted,
                fontSize = 13.sp,
            )
            Chip(ex.category.title)
        }
        trailing()
    }
}
