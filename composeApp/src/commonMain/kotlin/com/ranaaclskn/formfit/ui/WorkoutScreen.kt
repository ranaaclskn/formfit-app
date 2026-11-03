package com.ranaaclskn.formfit.ui

import androidx.compose.animation.animateColorAsState
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
import com.ranaaclskn.formfit.data.nowMillis
import com.ranaaclskn.formfit.model.Exercise
import com.ranaaclskn.formfit.model.PoseFrame
import kotlinx.coroutines.delay
import kotlin.time.TimeSource

private enum class Stage { SETUP, COUNTDOWN, ACTIVE, SUMMARY }

/** Kameradan gelen kareleri sayaca verir ve ekranda gösterilecek durumu tutar. */
private class WorkoutController(val exercise: Exercise) {
    private val counter = RepCounter(exercise.rule)
    private val clock = TimeSource.Monotonic.markNow()

    var frame by mutableStateOf<PoseFrame?>(null)
    var correct by mutableIntStateOf(0)
    var incorrect by mutableIntStateOf(0)
    var feedback by mutableStateOf(counter.feedback)
    var lastRep by mutableStateOf<RepResult?>(null)
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
        if (!counting) return
        val rep = counter.process(f, clock.elapsedNow().inWholeMilliseconds)
        feedback = counter.feedback
        if (rep != null) {
            reps += rep
            lastRep = rep
            correct = counter.correctCount
            incorrect = counter.incorrectCount
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
        )
        HistoryRepository.add(session)
        return session
    }
}

@Composable
fun WorkoutScreen(exercise: Exercise, onClose: () -> Unit, onShowHowTo: () -> Unit) {
    val controller = remember(exercise) { WorkoutController(exercise) }
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
            onRestart = { saved = null; stage = Stage.COUNTDOWN },
            onClose = onClose,
        )
        return
    }

    // Son tekrarın rengi kısa süre iskelette görünsün
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
            null -> if (controller.feedback.kind == FeedbackKind.WARN) FfColors.Warn else FfColors.Accent
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
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)))
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Kapat", tint = Color.White) }
                Column(Modifier.weight(1f)) {
                    Text(exercise.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        if (stage == Stage.ACTIVE) "Süre ${formatDuration(elapsed)} · Hedef ${exercise.reps} tekrar"
                        else exercise.cameraAngle.title,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                    )
                }
                IconButton(onClick = { frontCamera = !frontCamera }) {
                    Icon(Icons.Filled.Cameraswitch, "Kamerayı çevir", tint = Color.White)
                }
            }
            if (stage == Stage.ACTIVE) {
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Counter("DOĞRU", controller.correct, FfColors.Good, Modifier.weight(1f))
                    Counter("YANLIŞ", controller.incorrect, FfColors.Bad, Modifier.weight(1f))
                }
            }
        }

        when (stage) {
            Stage.SETUP -> SetupCard(
                exercise,
                onStart = { stage = Stage.COUNTDOWN },
                onShowHowTo = onShowHowTo,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            Stage.COUNTDOWN -> Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("$countdown", color = Color.White, fontSize = 110.sp, fontWeight = FontWeight.Bold)
                Text("Pozisyonunu al", color = Color.White, fontSize = 18.sp)
            }
            Stage.ACTIVE -> Column(
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FeedbackBanner(controller.feedback)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        durationAtEnd = controller.elapsedMs()
                        saved = controller.save()
                        stage = Stage.SUMMARY
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                ) { Text("Antrenmanı bitir", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
            Stage.SUMMARY -> Unit
        }
    }
}

@Composable
private fun Counter(label: String, value: Int, color: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$value", color = color, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
    }
}

@Composable
private fun FeedbackBanner(feedback: LiveFeedback) {
    val color = when (feedback.kind) {
        FeedbackKind.GOOD -> FfColors.Good
        FeedbackKind.BAD -> FfColors.Bad
        FeedbackKind.WARN -> FfColors.Warn
        FeedbackKind.INFO -> Color.White
    }
    Text(
        feedback.message,
        color = if (feedback.kind == FeedbackKind.INFO) Color.Black else Color.White,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.92f))
            .padding(horizontal = 18.dp, vertical = 10.dp),
    )
}

@Composable
private fun SetupCard(exercise: Exercise, onStart: () -> Unit, onShowHowTo: () -> Unit, modifier: Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(FfColors.Surface.copy(alpha = 0.95f))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Kamerayı yerleştir", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(exercise.setup, color = Color(0xFFCBD5E1), fontSize = 15.sp, lineHeight = 21.sp)
        Text(
            "Başla'ya bastıktan sonra 5 saniyen var. Tüm vücudun iskelet çizgileriyle görünüyorsa hazırsın.",
            color = FfColors.TextMuted,
            fontSize = 13.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onShowHowTo, modifier = Modifier.weight(1f).height(50.dp)) {
                Text("Nasıl yapılır?")
            }
            Button(
                onClick = onStart,
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FfColors.Accent),
            ) { Text("Başla", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun SummaryView(
    exercise: Exercise,
    reps: List<RepResult>,
    durationMs: Long,
    saved: Boolean,
    onRestart: () -> Unit,
    onClose: () -> Unit,
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
        ScreenTitle("Antrenman bitti", exercise.name)
        StatsRow(
            listOf(
                "Doğru" to "$correct",
                "Yanlış" to "${reps.size - correct}",
                "Doğruluk" to "%$accuracy",
                "Süre" to formatDuration(durationMs),
            ),
        )
        Text(
            if (saved) "Geçmişe kaydedildi." else "Hiç tekrar sayılmadığı için kaydedilmedi.",
            color = if (saved) FfColors.Good else FfColors.TextMuted,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        if (faults.isNotEmpty()) {
            SectionCard("En sık hatalar", faults.map { "${it.first} (${it.second}×)" }, FfColors.Warn)
        }
        if (reps.isNotEmpty()) RepList(reps.map { Triple(it.correct, it.durationMs, it.faults) })
        Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onRestart, modifier = Modifier.weight(1f).height(52.dp)) { Text("Tekrar başla") }
            Button(
                onClick = onClose,
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FfColors.Accent),
            ) { Text("Tamam", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun StatsRow(stats: List<Pair<String, String>>) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        stats.forEach { (label, value) ->
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(FfColors.Surface)
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(label, color = FfColors.TextMuted, fontSize = 12.sp)
            }
        }
    }
}

/** Tekrar tekrar sonuç listesi: (doğru mu, süre, hatalar). */
@Composable
fun RepList(reps: List<Triple<Boolean, Long, List<String>>>) {
    Column(
        Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(FfColors.Surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Tekrarlar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        reps.forEachIndexed { i, (ok, ms, faults) ->
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    if (ok) "✓" else "✗",
                    color = if (ok) FfColors.Good else FfColors.Bad,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.width(26.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        "${i + 1}. tekrar · ${(ms / 100) / 10.0} sn",
                        color = Color.White,
                        fontSize = 15.sp,
                    )
                    if (faults.isNotEmpty()) {
                        Text(faults.joinToString(" · "), color = FfColors.TextMuted, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
