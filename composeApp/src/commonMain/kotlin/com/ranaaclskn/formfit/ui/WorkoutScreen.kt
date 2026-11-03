package com.ranaaclskn.formfit.ui

import androidx.compose.animation.animateColorAsState
import com.ranaaclskn.formfit.analysis.checkPlacement
import com.ranaaclskn.formfit.analysis.Placement
import androidx.compose.foundation.layout.fillMaxHeight
import com.ranaaclskn.formfit.analysis.restSecondsFor
import com.ranaaclskn.formfit.analysis.restCue
import androidx.compose.foundation.clickable
import com.ranaaclskn.formfit.analysis.VoiceCoach
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranaaclskn.formfit.analysis.FeedbackKind
import com.ranaaclskn.formfit.analysis.LiveFeedback
import com.ranaaclskn.formfit.analysis.RepCounter
import com.ranaaclskn.formfit.analysis.RepResult
import com.ranaaclskn.formfit.data.HistoryRepository
import com.ranaaclskn.formfit.data.RepRecord
import com.ranaaclskn.formfit.data.WorkoutSession
import com.ranaaclskn.formfit.data.encodeSetNumber
import com.ranaaclskn.formfit.data.nowMillis
import com.ranaaclskn.formfit.model.Exercise
import com.ranaaclskn.formfit.model.PoseFrame
import kotlinx.coroutines.delay
import kotlin.time.TimeSource

private enum class Stage { SETUP, COUNTDOWN, ACTIVE, SUMMARY }

/** Antrenman bir programın parçasıysa: hangi program, hangi hareket, kaçıncı set. */
data class PlanContext(
    val planId: String,
    val planName: String,
    val runId: String,
    val itemIndex: Int,
    val setNumber: Int,
    val totalSets: Int,
    val targetReps: Int,
)

/** Kameradan gelen kareleri sayaca verir ve ekranda gösterilecek durumu tutar. */
private class WorkoutController(val exercise: Exercise, val plan: PlanContext?, private val target: Int) {
    private val counter = RepCounter(exercise.rule)
    private val clock = TimeSource.Monotonic.markNow()
    /** Uyarılar ekrana yazılmak yerine sesli söylenir. */
    val coach = VoiceCoach(say = { Speech.speak(it) }, clock = { clock.elapsedNow().inWholeMilliseconds })

    var frame by mutableStateOf<PoseFrame?>(null)
    var correct by mutableIntStateOf(0)
    var incorrect by mutableIntStateOf(0)
    var feedback by mutableStateOf(counter.feedback)
    var lastRep by mutableStateOf<RepResult?>(null)
    /** Kamera yerleşimi: başlamadan önce kadraj ve açı kontrolü. */
    var placement by mutableStateOf(checkPlacement(null, exercise.cameraAngle))
    val reps = mutableStateListOf<RepResult>()
    var startedAt = 0L
    private var startMark = clock

    fun start() {
        counter.reset()
        reps.clear()
        correct = 0
        incorrect = 0
        lastRep = null
        startedAt = nowMillis()
        startMark = TimeSource.Monotonic.markNow()
    }

    fun elapsedMs(): Long = startMark.elapsedNow().inWholeMilliseconds

    fun onPose(f: PoseFrame?, counting: Boolean) {
        frame = f
        if (!counting) {
            val p = checkPlacement(f, exercise.cameraAngle)
            if (p.message != placement.message) coach.onPlacement(p.message)
            placement = p
            return
        }
        val rep = counter.process(f, clock.elapsedNow().inWholeMilliseconds)
        val changed = counter.feedback != feedback
        feedback = counter.feedback
        if (rep != null) {
            reps += rep
            lastRep = rep
            correct = counter.correctCount
            incorrect = counter.incorrectCount
            coach.onRep(rep, correct)
            if (correct + incorrect >= target) coach.onTargetReached()
        } else if (changed) {
            coach.onFeedback(feedback)
        }
    }

    /** Antrenmanı geçmişe kaydeder; hiç tekrar yoksa kaydetmez. */
    fun save(): WorkoutSession? {
        if (reps.isEmpty()) return null
        val session = WorkoutSession(
            id = "${startedAt}-${exercise.id}",
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            startedAt = startedAt,
            durationMs = elapsedMs(),
            reps = reps.map { RepRecord(it.index, it.correct, it.faults, it.durationMs) },
            planId = plan?.planId,
            planName = plan?.planName,
            planRunId = plan?.runId,
            setNumber = plan?.let { encodeSetNumber(it.itemIndex, it.setNumber) },
        )
        HistoryRepository.add(session)
        return session
    }
}

@Composable
fun WorkoutScreen(
    exercise: Exercise,
    onClose: () -> Unit,
    onShowHowTo: () -> Unit,
    plan: PlanContext? = null,
) {
    val target = plan?.targetReps ?: exercise.reps
    val controller = remember(exercise) { WorkoutController(exercise, plan, target) }
    var voiceOn by remember { mutableStateOf(true) }
    controller.coach.enabled = voiceOn
    DisposableEffect(Unit) { onDispose { Speech.stop() } }
    val setLabel = plan?.let { "${it.planName} · Set ${it.setNumber}/${it.totalSets}" }
    var stage by remember { mutableStateOf(Stage.SETUP) }
    var frontCamera by remember { mutableStateOf(true) }
    var countdown by remember { mutableIntStateOf(5) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var saved by remember { mutableStateOf<WorkoutSession?>(null) }
    var durationAtEnd by remember { mutableLongStateOf(0L) }

    LaunchedEffect(stage) {
        when (stage) {
            Stage.COUNTDOWN -> {
                for (i in 5 downTo 1) { countdown = i; delay(1000) }
                controller.start()
                if (voiceOn) Speech.speak("Başla")
                stage = Stage.ACTIVE
            }
            Stage.ACTIVE -> while (true) { elapsed = controller.elapsedMs(); delay(500) }
            else -> Unit
        }
    }

    if (stage == Stage.SUMMARY) {
        SummaryView(
            exercise = exercise,
            reps = controller.reps,
            durationMs = durationAtEnd,
            saved = saved != null,
            onRestart = if (plan == null) ({ saved = null; stage = Stage.COUNTDOWN }) else null,
            // Programda setten sonra dinlenme sayacı
            restSeconds = plan?.let { restSecondsFor(exercise, it.targetReps) },
            voiceOn = voiceOn,
            onClose = onClose,
        )
        return
    }

    // Son tekrarın sonucu kısa süre iskelette renkle görünsün
    var flash by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(controller.lastRep) {
        val r = controller.lastRep ?: return@LaunchedEffect
        flash = r.correct
        delay(700)
        flash = null
    }
    val skeletonColor by animateColorAsState(
        when (flash) {
            true -> FfColors.Good
            false -> FfColors.Bad
            null -> Color.White
        }
    )

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CameraPoseView(
            frontCamera = frontCamera,
            onPose = { controller.onPose(it, counting = stage == Stage.ACTIVE) },
            modifier = Modifier.fillMaxSize(),
        )
        PoseOverlay(controller.frame, skeletonColor, Modifier.fillMaxSize())

        // Üst bar
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(Icons.Filled.Close, "Kapat", onClose)
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(exercise.titleUpper(), style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 1)
                    Overline(
                        if (stage == Stage.ACTIVE) "${setLabel ?: "Süre ${formatDuration(elapsed)}"} · Hedef $target"
                        else setLabel ?: exercise.cameraAngle.title,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
                RoundIconButton(
                    if (voiceOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    if (voiceOn) "Sesli koçu kapat" else "Sesli koçu aç",
                    {
                        voiceOn = !voiceOn
                        if (!voiceOn) Speech.stop()
                    },
                )
                Spacer(Modifier.width(8.dp))
                RoundIconButton(Icons.Filled.Cameraswitch, "Kamerayı çevir", { frontCamera = !frontCamera })
            }
            if (stage == Stage.ACTIVE) {
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Counter("Doğru", controller.correct, FfColors.Good, Modifier.weight(1f))
                    Counter("Yanlış", controller.incorrect, FfColors.Bad, Modifier.weight(1f))
                }
            }
            // Sağ üstte küçük "nasıl yapılır" videosu: bakarak yapabilmek için. Dokununca büyür/küçülür.
            Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.TopEnd) {
                MiniDemo(exercise)
            }
        }

        when (stage) {
            Stage.SETUP -> SetupCard(
                exercise,
                placement = controller.placement,
                onStart = { stage = Stage.COUNTDOWN },
                onShowHowTo = onShowHowTo,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            Stage.COUNTDOWN -> Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("$countdown", style = MaterialTheme.typography.displayLarge.copy(fontSize = 150.sp, lineHeight = 150.sp), color = Color.White)
                Overline("Pozisyonunu al", color = Color.White)
            }
            Stage.ACTIVE -> Column(
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Uyarılar sesli söylenir; ekranda sadece sesli koçun durumu görünür
                VoiceStatus(voiceOn)
                Spacer(Modifier.height(12.dp))
                PrimaryButton(
                    if (plan != null) "Seti bitir" else "Antrenmanı bitir",
                    onClick = {
                        durationAtEnd = controller.elapsedMs()
                        saved = controller.save()
                        if (voiceOn) {
                            val wrong = controller.incorrect
                            Speech.speak(
                                "Set bitti. ${controller.correct} doğru, $wrong yanlış." +
                                    if (wrong > 0) " Hatalarını ekranda görebilirsin." else " Harika, hatasız!"
                            )
                        }
                        stage = Stage.SUMMARY
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Stage.SUMMARY -> Unit
        }
    }
}

@Composable
private fun Counter(label: String, value: Int, dot: Color, modifier: Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
                Spacer(Modifier.width(6.dp))
                Overline(label, color = Color.White.copy(alpha = 0.75f))
            }
        }
        Text("$value", style = MaterialTheme.typography.displayLarge, color = Color.White)
    }
}

/** Setler arası dinlenme geri sayımı; belli saniyelerde sesli uyarır. */
@Composable
private fun RestTimer(initial: Int, voiceOn: Boolean) {
    var total by remember { mutableIntStateOf(initial) }
    var left by remember { mutableIntStateOf(initial) }
    var running by remember { mutableStateOf(true) }
    LaunchedEffect(running) {
        // Set sonu konuşması bitsin diye ilk uyarı birkaç saniye sonra
        while (running && left > 0) {
            delay(1000)
            left -= 1
            if (voiceOn) restCue(left, total)?.let { Speech.speak(it) }
        }
        if (left <= 0) running = false
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(FfColors.Accent)
            .padding(20.dp),
    ) {
        Overline(if (left > 0) "Dinlenme" else "Dinlenme bitti", color = FfColors.OnAccent.copy(alpha = 0.55f))
        Text(
            if (left > 0) formatDuration(left * 1000L) else "HAZIRSIN",
            style = MaterialTheme.typography.displayLarge,
            color = FfColors.OnAccent,
        )
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(FfColors.OnAccent.copy(alpha = 0.15f))) {
            Box(
                Modifier
                    .fillMaxWidth(if (total == 0) 0f else (left.toFloat() / total).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(FfColors.OnAccent),
            )
        }
        if (left > 0) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "+30 SN",
                    style = MaterialTheme.typography.labelLarge,
                    color = FfColors.OnAccent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, FfColors.OnAccent.copy(alpha = 0.3f), RoundedCornerShape(50))
                        .clickable { left += 30; total += 30 }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Text(
                    "ATLA",
                    style = MaterialTheme.typography.labelLarge,
                    color = FfColors.OnAccent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, FfColors.OnAccent.copy(alpha = 0.3f), RoundedCornerShape(50))
                        .clickable { left = 0; running = false }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CheckLine(label: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).background(if (ok) FfColors.Good else FfColors.Border),
            contentAlignment = Alignment.Center,
        ) { Text(if (ok) "✓" else "–", color = FfColors.OnAccent, style = MaterialTheme.typography.labelMedium) }
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = if (ok) FfColors.Text else FfColors.TextMuted)
    }
}

@Composable
private fun MiniDemo(exercise: Exercise) {
    var large by remember { mutableStateOf(false) }
    Column(
        Modifier
            .width(if (large) 200.dp else 112.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
            .clickable { large = !large },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ExerciseDemoView(
            exercise.demo,
            playing = true,
            speed = 1f,
            showCue = large,
            modifier = Modifier.fillMaxWidth().height(if (large) 240.dp else 132.dp),
        )
        Text(
            if (large) "KÜÇÜLT" else "NASIL YAPILIR",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.7f)).padding(vertical = 4.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun VoiceStatus(on: Boolean) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.6f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (on) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
            null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Overline(if (on) "Sesli koç açık · uyarıları dinle" else "Sesli koç kapalı", color = Color.White)
    }
}

@Composable
private fun FeedbackBanner(feedback: LiveFeedback) {
    val dot = when (feedback.kind) {
        FeedbackKind.GOOD -> FfColors.Good
        FeedbackKind.BAD -> FfColors.Bad
        FeedbackKind.WARN -> FfColors.Warn
        FeedbackKind.INFO -> FfColors.TextMuted
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(10.dp))
        Text(feedback.message, style = MaterialTheme.typography.titleSmall, color = FfColors.OnAccent, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SetupCard(
    exercise: Exercise,
    placement: Placement,
    onStart: () -> Unit,
    onShowHowTo: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(FfColors.Surface.copy(alpha = 0.96f))
            .border(1.dp, FfColors.Border, RoundedCornerShape(26.dp))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Overline("Hazırlık · ${exercise.cameraAngle.title}")
        Text("KAMERAYI YERLEŞTİR", style = MaterialTheme.typography.headlineLarge, color = FfColors.Text)
        Text(exercise.setup, style = MaterialTheme.typography.bodyMedium, color = FfColors.TextSecondary)
        // Canlı yerleşim kontrolü
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(FfColors.SurfaceHigh)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CheckLine("Tüm vücut kadrajda", placement.bodyVisible)
            CheckLine(exercise.cameraAngle.title.replace(" çek", "") + " açı", placement.angleOk == true)
            Text(placement.message, style = MaterialTheme.typography.bodySmall, color = if (placement.ok) FfColors.Good else FfColors.Warn)
        }
        Text(
            "Başla'ya bastıktan sonra 5 saniyen var. İskelet çizgileri tüm vücudunda görünüyorsa hazırsın.",
            style = MaterialTheme.typography.bodySmall,
            color = FfColors.TextMuted,
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Nasıl yapılır", onShowHowTo, Modifier.weight(1f))
            PrimaryButton("Başla", onStart, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryView(
    exercise: Exercise,
    reps: List<RepResult>,
    durationMs: Long,
    saved: Boolean,
    onRestart: (() -> Unit)?,
    onClose: () -> Unit,
    restSeconds: Int? = null,
    voiceOn: Boolean = true,
) {
    val correct = reps.count { it.correct }
    val accuracy = if (reps.isEmpty()) 0 else correct * 100 / reps.size
    val faults = reps.flatMap { it.faults }.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }
    Column(
        Modifier
            .fillMaxSize()
            .background(FfColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader("Set bitti · ${exercise.titleUpper()}", "%$accuracy doğru")
        // Hiç tekrar yapılmadan bitirilen sette dinlenmeye gerek yok
        if (restSeconds != null && reps.isNotEmpty()) RestTimer(restSeconds, voiceOn)
        StatsRow(
            listOf(
                "Doğru" to "$correct",
                "Yanlış" to "${reps.size - correct}",
                "Süre" to formatDuration(durationMs),
            ),
        )
        Text(
            if (saved) "Geçmişe kaydedildi." else "Hiç tekrar sayılmadığı için kaydedilmedi.",
            style = MaterialTheme.typography.bodySmall,
            color = FfColors.TextMuted,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
        if (reps.isNotEmpty()) {
            if (faults.isEmpty()) {
                SectionCard("Bu sette hata yok", listOf("Tüm tekrarlar doğru formda yapıldı. Böyle devam!"), Bullet.TIP)
            } else {
                SectionCard(
                    "Bu sette yapılan hatalar",
                    faults.map { (fault, count) ->
                        val which = reps.filter { fault in it.faults }.joinToString(", ") { "${it.index}." }
                        "$fault — $count kez ($which tekrar)"
                    },
                    Bullet.CAUTION,
                )
            }
        }
        if (reps.isNotEmpty()) RepList(reps.map { Triple(it.correct, it.durationMs, it.faults) })
        Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (onRestart != null) SecondaryButton("Tekrar başla", onRestart, Modifier.weight(1f))
            PrimaryButton(if (onRestart == null) "Programa dön" else "Tamam", onClose, Modifier.weight(1f))
        }
    }
}

/** Tekrar tekrar sonuç listesi: (doğru mu, süre, hatalar). */
@Composable
fun RepList(reps: List<Triple<Boolean, Long, List<String>>>) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(FfColors.Card)
            .border(1.dp, FfColors.Border, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("TEKRARLAR", style = MaterialTheme.typography.titleLarge, color = FfColors.Text)
        reps.forEachIndexed { i, (ok, ms, faults) ->
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    (i + 1).toString().padStart(2, '0'),
                    style = MaterialTheme.typography.titleLarge,
                    color = FfColors.TextMuted,
                    modifier = Modifier.width(34.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        if (ok) "Doğru" else "Yanlış",
                        style = MaterialTheme.typography.titleSmall,
                        color = FfColors.Text,
                    )
                    Text(
                        if (faults.isEmpty()) "${(ms / 100) / 10.0} sn" else faults.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = FfColors.TextMuted,
                    )
                }
                Box(
                    Modifier.padding(top = 2.dp).size(24.dp).clip(CircleShape).background(if (ok) FfColors.Good else FfColors.Bad),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (ok) "✓" else "✕", color = FfColors.OnAccent, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
