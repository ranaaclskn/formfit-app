package com.ranaaclskn.formfit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ranaaclskn.formfit.data.Equipment
import com.ranaaclskn.formfit.data.Goal
import com.ranaaclskn.formfit.data.Level
import com.ranaaclskn.formfit.data.PlanRepository
import com.ranaaclskn.formfit.data.Recommendation
import com.ranaaclskn.formfit.data.RecommendationRequest
import com.ranaaclskn.formfit.data.recommend
import com.ranaaclskn.formfit.model.Category
import com.ranaaclskn.formfit.model.exerciseById

private class Option(val title: String, val description: String? = null)

private const val QUESTIONS = 6

/** Kişinin cevaplarına göre program öneren adım adım sayfa. Öneri kabul edilirse programlara kaydedilir. */
@Composable
fun RecommendScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var goal by remember { mutableStateOf<Goal?>(null) }
    var level by remember { mutableStateOf<Level?>(null) }
    var days by remember { mutableStateOf<Int?>(null) }
    var equipment by remember { mutableStateOf<Equipment?>(null) }
    var minutes by remember { mutableStateOf<Int?>(null) }
    var focus by remember { mutableStateOf<Set<Category>?>(null) }
    var result by remember { mutableStateOf<Recommendation?>(null) }

    val r = result
    if (r != null) {
        RecommendationResult(
            r,
            onAccept = {
                PlanRepository.addAll(r.plans, message = "${r.title} programlarına eklendi.")
                onSaved()
            },
            onChange = { result = null; step = 0 },
            onClose = onBack,
        )
        return
    }

    val answered = when (step) {
        0 -> goal != null
        1 -> level != null
        2 -> days != null
        3 -> equipment != null
        4 -> minutes != null
        else -> focus != null
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 110.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (step == 0) RoundIconButton(Icons.Filled.Close, "Kapat", onBack)
                else RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Geri", { step-- })
                Spacer(Modifier.width(16.dp))
                // Adım göstergesi
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(QUESTIONS) { i ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (i <= step) FfColors.Accent else FfColors.Border),
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Overline("${step + 1}/$QUESTIONS")
            }

            when (step) {
                0 -> Question(
                    "Program önerisi",
                    "Hedefin ne?",
                    Goal.entries.map { Option(it.title, it.description) },
                    selected = setOfNotNull(goal?.ordinal),
                ) { goal = Goal.entries[it] }
                1 -> Question(
                    "Seviye",
                    "Ne kadar deneyimlisin?",
                    Level.entries.map { Option(it.title, it.description) },
                    selected = setOfNotNull(level?.ordinal),
                ) { level = Level.entries[it] }
                2 -> Question(
                    "Sıklık",
                    "Haftada kaç gün çalışabilirsin?",
                    (2..5).map { Option("$it gün", dayHint(it)) },
                    selected = setOfNotNull(days?.minus(2)),
                ) { days = it + 2 }
                3 -> Question(
                    "Ekipman",
                    "Elinde ne var?",
                    Equipment.entries.map { Option(it.title, it.description) },
                    selected = setOfNotNull(equipment?.ordinal),
                ) { equipment = Equipment.entries[it] }
                4 -> Question(
                    "Süre",
                    "Bir antrenman ne kadar sürsün?",
                    listOf(Option("20 dakika", "Kısa ve yoğun, 3 hareket"), Option("30 dakika", "Dengeli, 4 hareket"), Option("45 dakika", "Kapsamlı, 5 hareket")),
                    selected = setOfNotNull(minutes?.let { listOf(20, 30, 45).indexOf(it) }),
                ) { minutes = listOf(20, 30, 45)[it] }
                else -> {
                    val cats = listOf(Category.LOWER, Category.UPPER, Category.BACK, Category.CORE)
                    val current = focus ?: emptySet()
                    Question(
                        "Odak",
                        "Özellikle çalıştırmak istediğin bir bölge var mı?",
                        listOf(Option("Dengeli", "Tüm vücudu eşit çalıştır")) + cats.map { Option(it.title) },
                        selected = if (focus == null) emptySet() else if (current.isEmpty()) setOf(0) else current.map { cats.indexOf(it) + 1 }.toSet(),
                        multiHint = "Birden fazla seçebilirsin",
                    ) { i ->
                        focus = if (i == 0) emptySet() else {
                            val c = cats[i - 1]
                            if (c in current) current - c else current + c
                        }
                    }
                }
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(FfColors.Background)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            val last = step == QUESTIONS - 1
            Box(Modifier.fillMaxWidth()) {
                PrimaryButton(
                    if (last) "Programımı hazırla" else "Devam",
                    onClick = {
                        if (!answered) return@PrimaryButton
                        if (last) {
                            result = recommend(
                                RecommendationRequest(goal!!, level!!, days!!, equipment!!, minutes!!, focus ?: emptySet()),
                            )
                        } else {
                            step++
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!answered) {
                    // Seçim yapılmadan düğme soluk görünsün
                    Box(Modifier.matchParentSize().clip(RoundedCornerShape(16.dp)).background(FfColors.Background.copy(alpha = 0.6f)))
                }
            }
        }
    }
}

private fun dayHint(days: Int) = when (days) {
    2 -> "Tüm vücut, A ve B günleri"
    3 -> "İtiş · Çekiş · Bacak (dambılla)"
    4 -> "Üst vücut · Alt vücut, ikişer kez"
    else -> "İtiş · Çekiş · Bacak + tüm vücut + kondisyon"
}

@Composable
private fun Question(
    overline: String,
    title: String,
    options: List<Option>,
    selected: Set<Int>,
    multiHint: String? = null,
    onSelect: (Int) -> Unit,
) {
    ScreenHeader(overline, title, multiHint)
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEachIndexed { i, o ->
            val on = i in selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (on) FfColors.Accent else FfColors.Card)
                    .border(1.dp, if (on) FfColors.Accent else FfColors.Border, RoundedCornerShape(20.dp))
                    .clickable { onSelect(i) }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(o.title.trUpper(), style = MaterialTheme.typography.titleLarge, color = if (on) FfColors.OnAccent else FfColors.Text)
                    if (o.description != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            o.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (on) FfColors.OnAccent.copy(alpha = 0.65f) else FfColors.TextMuted,
                        )
                    }
                }
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (on) FfColors.OnAccent else Color.Transparent)
                        .border(2.dp, if (on) FfColors.OnAccent else FfColors.Border, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { if (on) Text("✓", color = FfColors.Accent, style = MaterialTheme.typography.labelMedium) }
            }
        }
    }
}

@Composable
private fun RecommendationResult(r: Recommendation, onAccept: () -> Unit, onChange: () -> Unit, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 180.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
                RoundIconButton(Icons.Filled.Close, "Kapat", onClose)
            }
            ScreenHeader("Sana özel öneri", r.title, r.summary)

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(FfColors.Accent)
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Overline("Haftalık takvim", color = FfColors.OnAccent.copy(alpha = 0.6f))
                    Spacer(Modifier.height(4.dp))
                    Text(r.schedule.trUpper(), style = MaterialTheme.typography.titleLarge, color = FfColors.OnAccent)
                }
            }

            r.plans.forEachIndexed { i, plan ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(FfColors.Card)
                        .border(1.dp, FfColors.Border, RoundedCornerShape(20.dp))
                        .padding(18.dp),
                ) {
                    Overline("Gün ${i + 1} · ${plan.description}")
                    Spacer(Modifier.height(2.dp))
                    Text(plan.name.substringAfter("· ").trUpper(), style = MaterialTheme.typography.headlineMedium, color = FfColors.Text)
                    Spacer(Modifier.height(10.dp))
                    plan.items.forEachIndexed { j, item ->
                        val ex = exerciseById(item.exerciseId) ?: return@forEachIndexed
                        if (j > 0) HorizontalDivider(color = FfColors.Border)
                        Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            ExerciseDemoView(ex.demo, playing = false, speed = 1f, showCue = false, stillProgress = 1f, modifier = Modifier.size(44.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                ex.titleUpper(),
                                style = MaterialTheme.typography.titleMedium,
                                color = FfColors.Text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text("${item.sets} × ${item.reps}", style = MaterialTheme.typography.titleLarge, color = FfColors.TextSecondary)
                        }
                    }
                }
            }
            SectionCard("Öneriler", r.notes, Bullet.TIP)
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(FfColors.Background)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PrimaryButton("Programı kaydet", onAccept, Modifier.fillMaxWidth())
            SecondaryButton("Cevapları değiştir", onChange, Modifier.fillMaxWidth())
        }
    }
}
