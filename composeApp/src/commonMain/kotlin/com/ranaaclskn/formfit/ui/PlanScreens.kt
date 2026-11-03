package com.ranaaclskn.formfit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ranaaclskn.formfit.data.HistoryRepository
import com.ranaaclskn.formfit.data.PRESET_GROUP
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.AutoAwesome
import com.ranaaclskn.formfit.data.PLAN_TEMPLATES
import com.ranaaclskn.formfit.data.PlanItem
import com.ranaaclskn.formfit.data.PlanRepository
import com.ranaaclskn.formfit.data.WorkoutPlan
import com.ranaaclskn.formfit.data.formatDateTime
import com.ranaaclskn.formfit.data.newId
import com.ranaaclskn.formfit.data.planRuns
import com.ranaaclskn.formfit.data.runProgress
import com.ranaaclskn.formfit.model.EXERCISES
import com.ranaaclskn.formfit.model.exerciseById

// ---------------------------------------------------------------- Program listesi (sekme)

@Composable
fun PlansScreen(onOpen: (String) -> Unit, onCreate: () -> Unit, onRecommend: () -> Unit) {
    LaunchedEffect(Unit) { PlanRepository.load() }
    val plans by PlanRepository.plans.collectAsState()
    val sessions by HistoryRepository.sessions.collectAsState()
    val saved by PlanRepository.lastSavedMessage.collectAsState()
    // Kendi ve önerilen programlar önce, hazır programlar en sonda
    val groups = plans.groupBy { it.group.ifBlank { "Programlarım" } }
        .toList()
        .sortedBy { (g, _) -> if (g == PRESET_GROUP) 1 else 0 }

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 28.dp)) {
        item {
            ScreenHeader(
                "Çalışma planın",
                "Programlarım",
                "Haftalık günlerini oluştur: itiş, çekiş, bacak… Her günü kamerayla yap, doğru ve yanlış tekrarlarını takip et.",
            )
        }
        if (saved != null) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(FfColors.SurfaceHigh)
                        .padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(FfColors.Good))
                    Spacer(Modifier.width(10.dp))
                    Text(saved!!, style = MaterialTheme.typography.titleSmall, color = FfColors.Text, modifier = Modifier.weight(1f))
                    RoundIconButton(Icons.Filled.Close, "Kapat", { PlanRepository.clearSavedMessage() })
                }
            }
        }
        item { RecommendCard(onRecommend) }
        item { CreatePlanCard(onCreate) }
        groups.forEach { (group, list) ->
            item(key = "g-$group") {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Overline(group, color = FfColors.Text)
                    Spacer(Modifier.width(10.dp))
                    HorizontalDivider(Modifier.weight(1f), color = FfColors.Border)
                    Spacer(Modifier.width(10.dp))
                    Overline("${list.size} gün")
                }
            }
            items(list, key = { it.id }) { plan ->
                val last = planRuns(plan.id, sessions).firstOrNull()
                PlanCard(plan, last?.let { "Son: ${formatDateTime(it.startedAt).substringBefore(",")} · %${it.accuracy} doğru" }) {
                    onOpen(plan.id)
                }
            }
        }
    }
}

@Composable
private fun RecommendCard(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(FfColors.Card)
            .border(1.dp, FfColors.TextSecondary, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Overline("Sana özel")
            Spacer(Modifier.height(2.dp))
            Text("PROGRAM ÖNER", style = MaterialTheme.typography.headlineMedium, color = FfColors.Text)
            Spacer(Modifier.height(2.dp))
            Text(
                "Hedefini, seviyeni ve kaç gün çalışacağını söyle; sana uygun haftalık programı hazırlayalım.",
                style = MaterialTheme.typography.bodySmall,
                color = FfColors.TextSecondary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(FfColors.Accent),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.AutoAwesome, "Program öner", tint = FfColors.OnAccent) }
    }
}

@Composable
private fun CreatePlanCard(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(FfColors.Accent)
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Overline("Kendi planın", color = FfColors.OnAccent.copy(alpha = 0.55f))
            Spacer(Modifier.height(2.dp))
            Text("YENİ PROGRAM OLUŞTUR", style = MaterialTheme.typography.headlineMedium, color = FfColors.OnAccent)
            Spacer(Modifier.height(2.dp))
            Text(
                "Hareketleri seç, set ve tekrar sayısını belirle.",
                style = MaterialTheme.typography.bodySmall,
                color = FfColors.OnAccent.copy(alpha = 0.7f),
            )
        }
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(FfColors.OnAccent),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Add, "Oluştur", tint = FfColors.Accent) }
    }
}

@Composable
private fun PlanCard(plan: WorkoutPlan, lastRun: String?, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(FfColors.Card)
            .border(1.dp, FfColors.Border, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Overline("${plan.items.size} hareket · ${plan.totalSets} set", Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = FfColors.TextMuted, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(plan.name.trUpper(), style = MaterialTheme.typography.headlineLarge, color = FfColors.Text)
        if (plan.description.isNotBlank()) {
            Text(plan.description, style = MaterialTheme.typography.bodyMedium, color = FfColors.TextSecondary)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            plan.items.take(4).forEach { item ->
                val ex = exerciseById(item.exerciseId) ?: return@forEach
                ExerciseDemoView(
                    ex.demo, playing = false, speed = 1f, showCue = false, stillProgress = 1f,
                    modifier = Modifier.size(52.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            plan.items.mapNotNull { exerciseById(it.exerciseId)?.name?.substringBefore(" (") }.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = FfColors.TextMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
        Overline(lastRun ?: "Henüz yapılmadı", color = if (lastRun != null) FfColors.Text else FfColors.TextMuted)
    }
}

// ---------------------------------------------------------------- Program detayı

@Composable
fun PlanDetailScreen(
    planId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onStart: (runId: String) -> Unit,
    onOpenRun: (runId: String) -> Unit,
) {
    val plans by PlanRepository.plans.collectAsState()
    val sessions by HistoryRepository.sessions.collectAsState()
    val plan = plans.firstOrNull { it.id == planId }
    var confirmDelete by remember { mutableStateOf(false) }
    if (plan == null) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp)) {
            RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Geri", onBack)
            Text("Program bulunamadı.", color = FfColors.TextMuted, modifier = Modifier.padding(top = 20.dp))
        }
        return
    }
    val runs = planRuns(plan.id, sessions)

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 110.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Geri", onBack)
                Spacer(Modifier.weight(1f))
                RoundIconButton(Icons.Filled.Edit, "Düzenle", onEdit)
                RoundIconButton(Icons.Filled.Delete, "Sil", { confirmDelete = true })
            }
            ScreenHeader("Program", plan.name, plan.description.ifBlank { null })
            StatsRow(
                listOf(
                    "Hareket" to "${plan.items.size}",
                    "Set" to "${plan.totalSets}",
                    "Son doğruluk" to (runs.firstOrNull()?.let { "%${it.accuracy}" } ?: "–"),
                ),
            )
            Spacer(Modifier.height(18.dp))
            SectionTitle("Hareketler")
            plan.items.forEachIndexed { i, item ->
                val ex = exerciseById(item.exerciseId) ?: return@forEachIndexed
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        (i + 1).toString().padStart(2, '0'),
                        style = MaterialTheme.typography.headlineMedium,
                        color = FfColors.TextMuted,
                        modifier = Modifier.width(44.dp),
                    )
                    ExerciseDemoView(
                        ex.demo, playing = false, speed = 1f, showCue = false, stillProgress = 1f,
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(ex.titleUpper(), style = MaterialTheme.typography.titleLarge, color = FfColors.Text)
                        Overline(ex.category.title)
                    }
                    Text("${item.sets} × ${item.reps}", style = MaterialTheme.typography.headlineMedium, color = FfColors.Text)
                }
            }
            if (runs.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                SectionTitle("Geçmiş uygulamalar")
                runs.forEach { run ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 5.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(FfColors.Card)
                            .border(1.dp, FfColors.Border, RoundedCornerShape(18.dp))
                            .clickable { onOpenRun(run.runId) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(formatDateTime(run.startedAt), style = MaterialTheme.typography.titleSmall, color = FfColors.Text)
                            Text(
                                "${run.sets} set · ${run.correct} doğru · ${run.incorrect} yanlış",
                                style = MaterialTheme.typography.bodySmall,
                                color = FfColors.TextMuted,
                            )
                        }
                        Text("%${run.accuracy}", style = MaterialTheme.typography.headlineLarge, color = FfColors.Text)
                    }
                }
            }
        }
        BottomAction {
            PrimaryButton(
                "Antrenmanı başlat",
                onClick = { onStart(newId("run")) },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.PlayArrow,
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = FfColors.Surface,
            title = { Text("PROGRAM SİLİNSİN Mİ?", style = MaterialTheme.typography.headlineMedium) },
            text = { Text("${plan.name} silinecek. Daha önce yaptığın setler geçmişte kalır.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    PlanRepository.delete(plan.id)
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

@Composable
private fun SectionTitle(text: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Overline(text, color = FfColors.Text)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(1.dp).background(FfColors.Border))
    }
}

/** Ekranın altına sabitlenmiş, arkası kararan düğme alanı. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.BottomAction(content: @Composable () -> Unit) {
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, FfColors.Background, FfColors.Background)))
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp),
    ) { content() }
}

// ---------------------------------------------------------------- Program oluşturma / düzenleme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanEditScreen(planId: String?, onDone: () -> Unit, onCancel: () -> Unit) {
    val existing = remember(planId) { planId?.let { PlanRepository.find(it) } }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    val planItems = remember { mutableStateListOf<PlanItem>().apply { addAll(existing?.items.orEmpty()) } }
    var picking by remember { mutableStateOf(false) }
    val canSave = name.isNotBlank() && planItems.isNotEmpty()

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 110.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
                RoundIconButton(Icons.Filled.Close, "Vazgeç", onCancel)
            }
            ScreenHeader(if (existing == null) "Yeni program" else "Düzenle", if (existing == null) "Planını oluştur" else existing.name)

            if (existing == null) {
                SectionTitle("Şablondan başla")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(PLAN_TEMPLATES) { t ->
                        PillOption(t.name, selected = name == t.name) {
                            name = t.name
                            description = t.description
                            planItems.clear()
                            planItems.addAll(t.items)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FfTextField(name, { name = it }, "Program adı (ör. İtiş Günü)")
                FfTextField(description, { description = it }, "Açıklama (isteğe bağlı)")
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("Hareketler · ${planItems.size}")
            if (planItems.isEmpty()) {
                Text(
                    "Henüz hareket yok. Aşağıdan ekle ya da bir şablon seç.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FfColors.TextMuted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            planItems.forEachIndexed { i, item ->
                val ex = exerciseById(item.exerciseId) ?: return@forEachIndexed
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 5.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(FfColors.Card)
                        .border(1.dp, FfColors.Border, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            (i + 1).toString().padStart(2, '0'),
                            style = MaterialTheme.typography.headlineMedium,
                            color = FfColors.TextMuted,
                            modifier = Modifier.width(40.dp),
                        )
                        Text(ex.titleUpper(), style = MaterialTheme.typography.titleLarge, color = FfColors.Text, modifier = Modifier.weight(1f))
                        if (i > 0) {
                            SmallTextButton("↑") { planItems.add(i - 1, planItems.removeAt(i)) }
                        }
                        SmallTextButton("✕") { planItems.removeAt(i) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Stepper("Set", item.sets, 1..10, Modifier.weight(1f)) { planItems[i] = item.copy(sets = it) }
                        Stepper("Tekrar", item.reps, 1..50, Modifier.weight(1f)) { planItems[i] = item.copy(reps = it) }
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, FfColors.Border, RoundedCornerShape(20.dp))
                    .clickable { picking = true }
                    .padding(18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Add, null, tint = FfColors.Text)
                Spacer(Modifier.width(8.dp))
                Text("HAREKET EKLE", style = MaterialTheme.typography.labelLarge, color = FfColors.Text)
            }
        }
        BottomAction {
            PrimaryButton(
                if (canSave) "Kaydet" else if (name.isBlank()) "Program adı yaz" else "Hareket ekle",
                onClick = {
                    if (!canSave) return@PrimaryButton
                    PlanRepository.upsert(
                        WorkoutPlan(
                            id = existing?.id ?: newId("plan"),
                            name = name.trim(),
                            description = description.trim(),
                            items = planItems.toList(),
                        )
                    )
                    onDone()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (picking) {
        ExercisePicker(
            onPick = { ex ->
                planItems.add(PlanItem(ex.id, ex.sets, ex.reps))
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun FfTextField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        textStyle = MaterialTheme.typography.titleMedium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = FfColors.Accent,
            unfocusedBorderColor = FfColors.Border,
            focusedLabelColor = FfColors.Text,
            unfocusedLabelColor = FfColors.TextMuted,
            cursorColor = FfColors.Accent,
            focusedContainerColor = FfColors.Card,
            unfocusedContainerColor = FfColors.Card,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SmallTextButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(start = 6.dp)
            .size(34.dp)
            .clip(CircleShape)
            .background(FfColors.SurfaceHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = FfColors.Text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
private fun Stepper(label: String, value: Int, range: IntRange, modifier: Modifier, onChange: (Int) -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(FfColors.SurfaceHigh)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallTextButton("−") { if (value > range.first) onChange(value - 1) }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", style = MaterialTheme.typography.headlineMedium, color = FfColors.Text)
            Overline(label)
        }
        SmallTextButton("+") { if (value < range.last) onChange(value + 1) }
    }
}

@Composable
private fun ExercisePicker(onPick: (com.ranaaclskn.formfit.model.Exercise) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(FfColors.Surface)
                .border(1.dp, FfColors.Border, RoundedCornerShape(26.dp))
                .padding(vertical = 18.dp),
        ) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("HAREKET SEÇ", style = MaterialTheme.typography.headlineMedium, color = FfColors.Text, modifier = Modifier.weight(1f))
                RoundIconButton(Icons.Filled.Close, "Kapat", onDismiss)
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn {
                itemsIndexed(EXERCISES) { _, ex ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(ex) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ExerciseDemoView(
                            ex.demo, playing = false, speed = 1f, showCue = false, stillProgress = 1f,
                            modifier = Modifier.size(52.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(ex.titleUpper(), style = MaterialTheme.typography.titleLarge, color = FfColors.Text)
                            Overline("${ex.category.title} · ${ex.sets} × ${ex.reps}")
                        }
                        Icon(Icons.Filled.Add, "Ekle", tint = FfColors.Text)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Programı uygulama (set set takip)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanRunScreen(
    planId: String,
    runId: String,
    onBack: () -> Unit,
    onStartSet: (itemIndex: Int) -> Unit,
) {
    val plans by PlanRepository.plans.collectAsState()
    val sessions by HistoryRepository.sessions.collectAsState()
    val plan = plans.firstOrNull { it.id == planId }
    if (plan == null) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp)) {
            RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Geri", onBack)
            Text("Program bulunamadı.", color = FfColors.TextMuted, modifier = Modifier.padding(top = 20.dp))
        }
        return
    }
    val progress = runProgress(plan, runId, sessions)
    val started = sessions.filter { it.planRunId == runId }.minOfOrNull { it.startedAt }
    val next = progress.nextIndex

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 110.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
                RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Geri", onBack)
            }
            ScreenHeader(started?.let { formatDateTime(it) } ?: "Bugün", plan.name)

            // Genel ilerleme
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(FfColors.Accent)
                    .padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Overline("İlerleme", color = FfColors.OnAccent.copy(alpha = 0.55f))
                        Text(
                            "${progress.doneSets}/${progress.totalSets} SET",
                            style = MaterialTheme.typography.displayMedium,
                            color = FfColors.OnAccent,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            progress.accuracy?.let { "%$it" } ?: "–",
                            style = MaterialTheme.typography.displayMedium,
                            color = FfColors.OnAccent,
                        )
                        Overline("${progress.correct} doğru · ${progress.incorrect} yanlış", color = FfColors.OnAccent.copy(alpha = 0.55f))
                    }
                }
                Spacer(Modifier.height(14.dp))
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(FfColors.OnAccent.copy(alpha = 0.15f))) {
                    Box(
                        Modifier
                            .fillMaxWidth(if (progress.totalSets == 0) 0f else progress.doneSets.toFloat() / progress.totalSets)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(FfColors.OnAccent),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            progress.items.forEachIndexed { i, p ->
                val ex = exerciseById(p.item.exerciseId) ?: return@forEachIndexed
                val isNext = i == next
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(FfColors.Card)
                        .border(if (isNext) 2.dp else 1.dp, if (isNext) FfColors.Accent else FfColors.Border, RoundedCornerShape(22.dp))
                        .padding(18.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ExerciseDemoView(
                            ex.demo, playing = false, speed = 1f, showCue = false, stillProgress = 1f,
                            modifier = Modifier.size(56.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Overline(
                                when {
                                    p.finished -> "Tamamlandı"
                                    isNext -> "Sıradaki"
                                    else -> "${(i + 1).toString().padStart(2, '0')} · ${ex.category.title}"
                                },
                                color = if (isNext || p.finished) FfColors.Text else FfColors.TextMuted,
                            )
                            Text(ex.titleUpper(), style = MaterialTheme.typography.titleLarge, color = FfColors.Text)
                        }
                        Text("${p.item.sets} × ${p.item.reps}", style = MaterialTheme.typography.headlineMedium, color = FfColors.TextSecondary)
                    }
                    Spacer(Modifier.height(14.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        p.sets.forEachIndexed { s, r ->
                            SetChip("SET ${s + 1}", "${r.correct}✓  ${r.incorrect}✗", done = true)
                        }
                        for (s in p.doneSets until p.item.sets) {
                            SetChip("SET ${s + 1}", "${p.item.reps} tekrar", done = false)
                        }
                    }
                    if (!p.finished) {
                        Spacer(Modifier.height(14.dp))
                        if (isNext) {
                            PrimaryButton(
                                "Set ${p.doneSets + 1} başlat",
                                onClick = { onStartSet(i) },
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Filled.PlayArrow,
                            )
                        } else {
                            SecondaryButton("Set ${p.doneSets + 1} başlat", { onStartSet(i) }, Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
        BottomAction {
            if (next == null) {
                PrimaryButton("Program tamamlandı · Bitir", onBack, Modifier.fillMaxWidth())
            } else {
                SecondaryButton(if (progress.doneSets == 0) "Vazgeç" else "Şimdilik bırak", onBack, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SetChip(title: String, value: String, done: Boolean) {
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (done) FfColors.SurfaceHigh else Color.Transparent)
            .border(1.dp, if (done) FfColors.SurfaceHigh else FfColors.Border, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Overline(title, color = if (done) FfColors.TextSecondary else FfColors.TextMuted)
        Text(value, style = MaterialTheme.typography.titleSmall, color = if (done) FfColors.Text else FfColors.TextMuted)
    }
}
