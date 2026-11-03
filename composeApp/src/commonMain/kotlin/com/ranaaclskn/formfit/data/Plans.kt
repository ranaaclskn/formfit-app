package com.ranaaclskn.formfit.data

import com.ranaaclskn.formfit.model.exerciseById
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

@Serializable
data class PlanItem(
    val exerciseId: String,
    val sets: Int,
    val reps: Int,
)

/** Kullanıcının çalışma programındaki bir gün: "İtiş Günü" gibi. */
@Serializable
data class WorkoutPlan(
    val id: String,
    val name: String,
    val description: String = "",
    val items: List<PlanItem> = emptyList(),
    /** Programların gruplandığı başlık: "Hazır programlar", "Güç programı · 3 gün" gibi. */
    val group: String = "",
) {
    val totalSets: Int get() = items.sumOf { it.sets }
}

/** Hazır şablonlar; ilk açılışta programlara eklenir, editörde de şablon olarak seçilebilir. */
const val PRESET_GROUP = "Hazır programlar"

val PLAN_TEMPLATES: List<WorkoutPlan> = listOf(
    WorkoutPlan(
        id = "preset-push",
        name = "İtiş Günü",
        description = "Göğüs, omuz ve arka kol",
        items = listOf(PlanItem("pushup", 3, 10), PlanItem("shoulder-press", 3, 10), PlanItem("lateral-raise", 3, 12)),
        group = PRESET_GROUP,
    ),
    WorkoutPlan(
        id = "preset-pull",
        name = "Çekiş Günü",
        description = "Sırt, arka bacak ve ön kol",
        items = listOf(PlanItem("dumbbell-row", 3, 10), PlanItem("romanian-deadlift", 3, 10), PlanItem("biceps-curl", 3, 12)),
        group = PRESET_GROUP,
    ),
    WorkoutPlan(
        id = "preset-legs",
        name = "Bacak Günü",
        description = "Ön bacak, arka bacak ve kalça",
        items = listOf(PlanItem("squat", 4, 12), PlanItem("lunge", 3, 10), PlanItem("glute-bridge", 3, 15)),
        group = PRESET_GROUP,
    ),
    WorkoutPlan(
        id = "preset-full",
        name = "Tüm Vücut",
        description = "Kısa ve dengeli bir tam vücut antrenmanı",
        items = listOf(
            PlanItem("jumping-jack", 2, 20),
            PlanItem("squat", 3, 12),
            PlanItem("pushup", 3, 10),
            PlanItem("dumbbell-row", 3, 10),
        ),
        group = PRESET_GROUP,
    ),
)

fun newId(prefix: String): String = "$prefix-${nowMillis()}-${Random.nextInt(1000, 9999)}"

/** Bir programın tek bir uygulamasındaki ilerleme. */
data class SetResult(val correct: Int, val incorrect: Int)

data class ItemProgress(val item: PlanItem, val sets: List<SetResult>) {
    val doneSets: Int get() = sets.size
    val finished: Boolean get() = sets.size >= item.sets
}

data class RunProgress(val items: List<ItemProgress>) {
    val doneSets: Int get() = items.sumOf { minOf(it.doneSets, it.item.sets) }
    val totalSets: Int get() = items.sumOf { it.item.sets }
    val correct: Int get() = items.sumOf { p -> p.sets.sumOf { it.correct } }
    val incorrect: Int get() = items.sumOf { p -> p.sets.sumOf { it.incorrect } }
    val accuracy: Int? get() = (correct + incorrect).takeIf { it > 0 }?.let { correct * 100 / it }
    /** Sıradaki yapılacak hareket; hepsi bittiyse null. */
    val nextIndex: Int? get() = items.indexOfFirst { !it.finished }.takeIf { it >= 0 }
}

/** Kayıtlı setlerden programın bir uygulamasının ilerlemesini çıkarır. */
fun runProgress(plan: WorkoutPlan, runId: String, sessions: List<WorkoutSession>): RunProgress {
    val inRun = sessions.filter { it.planRunId == runId }.sortedBy { it.startedAt }
    return RunProgress(
        plan.items.mapIndexed { index, item ->
            // Aynı hareket programda iki kez olabilir; set numarası ve sıra ile ayrılır
            val mine = inRun.filter { it.exerciseId == item.exerciseId && (it.setNumber ?: 0) / 100 == index }
            ItemProgress(item, mine.map { SetResult(it.correctCount, it.incorrectCount) })
        }
    )
}

/** Set numarası, programdaki sıra ile birlikte saklanır: sıra × 100 + set. */
fun encodeSetNumber(itemIndex: Int, set: Int): Int = itemIndex * 100 + set

fun decodeSet(setNumber: Int): Int = setNumber % 100

/** Bir programın geçmişteki uygulamaları, yeniden eskiye. */
data class PlanRunSummary(val runId: String, val startedAt: Long, val correct: Int, val incorrect: Int, val sets: Int) {
    val accuracy: Int get() = if (correct + incorrect == 0) 0 else correct * 100 / (correct + incorrect)
}

fun planRuns(planId: String, sessions: List<WorkoutSession>): List<PlanRunSummary> =
    sessions.filter { it.planId == planId && it.planRunId != null }
        .groupBy { it.planRunId!! }
        .map { (runId, list) ->
            PlanRunSummary(
                runId = runId,
                startedAt = list.minOf { it.startedAt },
                correct = list.sumOf { it.correctCount },
                incorrect = list.sumOf { it.incorrectCount },
                sets = list.size,
            )
        }
        .sortedByDescending { it.startedAt }

@Serializable
private data class PlansFile(
    val version: Int = 1,
    val plans: List<WorkoutPlan> = emptyList(),
    /** Programların öncelik sırası: ilk eleman birinci öncelik. */
    val programOrder: List<String> = emptyList(),
)

/** Kullanıcının kendi oluşturduğu günlerin varsayılan programı. */
const val DEFAULT_PROGRAM = "Programım"

/** Bir günün ait olduğu program. Eski kayıtlardaki boş grup ve "Programlarım" varsayılan programa gider. */
val WorkoutPlan.programName: String
    get() = if (group.isBlank() || group == "Programlarım") DEFAULT_PROGRAM else group

/** Kullanıcının programları (hazır günler hariç), öncelik sırasına göre. */
fun orderedPrograms(plans: List<WorkoutPlan>, order: List<String>): List<String> {
    val names = plans.filter { it.group != PRESET_GROUP }.map { it.programName }.distinct()
    return order.filter { it in names } + names.filterNot { it in order }
}

/** Kullanıcının programları; cihazda plans.json dosyasında tutulur. */
object PlanRepository {
    private const val FILE = "plans.json"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _plans = MutableStateFlow<List<WorkoutPlan>>(emptyList())
    val plans: StateFlow<List<WorkoutPlan>> = _plans.asStateFlow()
    private var loaded = false

    /** Öneri kaydedildiğinde Programlar sayfasında gösterilecek bilgi. */
    private val _lastSavedMessage = MutableStateFlow<String?>(null)
    val lastSavedMessage: StateFlow<String?> = _lastSavedMessage.asStateFlow()

    fun clearSavedMessage() { _lastSavedMessage.value = null }

    private val _programOrder = MutableStateFlow<List<String>>(emptyList())
    val programOrder: StateFlow<List<String>> = _programOrder.asStateFlow()

    /** Yeni eklenen program: Programlar sayfası açılınca bu program seçili gelir. */
    var focusProgram: String? = null

    /** Programlar sayfasında şu an seçili program; yeni gün bu programa eklenir. */
    var currentProgram: String? = null

    fun programs(): List<String> = orderedPrograms(_plans.value, _programOrder.value)

    private fun uniqueProgramName(name: String, taken: Set<String>): String {
        var n = 2
        while ("$name ($n)" in taken) n++
        return "$name ($n)"
    }

    /** Programı birinci önceliğe taşır. */
    fun makeFirst(program: String) {
        _programOrder.value = listOf(program) + programs().filterNot { it == program }
        save()
    }

    /** Programı bir sıra yukarı taşır. */
    fun moveUp(program: String) {
        val list = programs().toMutableList()
        val i = list.indexOf(program)
        if (i > 0) {
            list.add(i - 1, list.removeAt(i))
            _programOrder.value = list
            save()
        }
    }

    /** Programı ve içindeki tüm günleri siler. */
    fun deleteProgram(program: String) {
        _plans.value = _plans.value.filterNot { it.group != PRESET_GROUP && it.programName == program }
        _programOrder.value = _programOrder.value.filterNot { it == program }
        save()
    }

    /** Hazır bir günü kullanıcının programına kopyalar. */
    fun copyToProgram(plan: WorkoutPlan, program: String) {
        addAll(listOf(plan.copy(id = newId("plan"), group = program)), "${plan.name}, \"$program\" programına eklendi.")
    }

    fun load() {
        if (loaded) return
        loaded = true
        val text = runCatching { readAppFile(FILE) }.getOrNull()
        val file = text?.let { runCatching { json.decodeFromString<PlansFile>(it) }.getOrNull() }
        if (file == null) {
            // İlk açılış: hazır programlarla başla
            _plans.value = PLAN_TEMPLATES
            save()
        } else {
            _plans.value = file.plans.map { p -> p.copy(items = p.items.filter { exerciseById(it.exerciseId) != null }) }
            _programOrder.value = file.programOrder
        }
    }

    fun find(id: String): WorkoutPlan? = _plans.value.firstOrNull { it.id == id }

    /** Yedekten geri yüklenen programlar ve öncelik sırası. */
    fun restore(plans: List<WorkoutPlan>, order: List<String>) {
        loaded = true
        _plans.value = plans
        _programOrder.value = order
        save()
    }

    fun upsert(plan: WorkoutPlan) {
        load()
        val list = _plans.value
        _plans.value = if (list.any { it.id == plan.id }) list.map { if (it.id == plan.id) plan else it } else list + plan
        save()
    }

    fun addAll(plansToAdd: List<WorkoutPlan>, message: String? = null) {
        load()
        // Aynı adlı program zaten varsa (ör. öneri iki kez kaydedildi) günler karışmasın: "(2)" eki ver
        val existing = programs().toSet()
        val newPlans = plansToAdd.map { p ->
            if (p.group == PRESET_GROUP || p.programName !in existing || plansToAdd.size == 1) p
            else p.copy(group = uniqueProgramName(p.programName, existing))
        }
        _plans.value = _plans.value + newPlans
        save()
        _lastSavedMessage.value = message
        focusProgram = newPlans.firstOrNull()?.programName
    }

    fun delete(id: String) {
        _plans.value = _plans.value.filterNot { it.id == id }
        save()
    }

    private fun save() {
        runCatching {
            writeAppFile(FILE, json.encodeToString(PlansFile(plans = _plans.value, programOrder = _programOrder.value)))
        }
    }
}
