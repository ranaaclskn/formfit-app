package com.ranaaclskn.formfit.data

import com.ranaaclskn.formfit.model.Category
import com.ranaaclskn.formfit.model.EXERCISES
import com.ranaaclskn.formfit.model.exerciseById

enum class Goal(val title: String, val description: String) {
    STRENGTH("Güç ve kas", "Daha güçlü olmak, kas kütlesi kazanmak"),
    FAT_LOSS("Yağ yakımı", "Kalori yakmak, kondisyonu artırmak"),
    TONE("Sıkılaşma", "Vücudu sıkılaştırmak, şekillendirmek"),
    FITNESS("Genel form", "Sağlıklı ve hareketli kalmak"),
}

enum class Level(val title: String, val description: String) {
    BEGINNER("Yeni başlıyorum", "Düzenli spor yapmıyorum"),
    INTERMEDIATE("Orta seviye", "Birkaç aydır düzenli çalışıyorum"),
    ADVANCED("İleri seviye", "Uzun süredir düzenli çalışıyorum"),
}

enum class Equipment(val title: String, val description: String) {
    NONE("Ekipmansız", "Sadece vücut ağırlığı"),
    DUMBBELLS("Dambıl var", "Bir çift dambılım var"),
}

data class RecommendationRequest(
    val goal: Goal,
    val level: Level,
    val daysPerWeek: Int,
    val equipment: Equipment,
    val minutes: Int,
    /** Öncelik verilecek bölgeler; boşsa dengeli. */
    val focus: Set<Category> = emptySet(),
)

data class Recommendation(
    val title: String,
    val summary: String,
    val schedule: String,
    val plans: List<WorkoutPlan>,
    val notes: List<String>,
)

/** Dambıl gerektiren hareketler. */
val DUMBBELL_EXERCISES = setOf("biceps-curl", "shoulder-press", "lateral-raise", "dumbbell-row", "romanian-deadlift")

private enum class Day(val title: String, val description: String, val pool: List<String>) {
    PUSH("İtiş", "Göğüs, omuz, arka kol", listOf("pushup", "shoulder-press", "lateral-raise", "jumping-jack")),
    PULL("Çekiş", "Sırt, arka bacak, ön kol", listOf("dumbbell-row", "romanian-deadlift", "biceps-curl", "glute-bridge")),
    LEGS("Bacak", "Ön bacak, arka bacak, kalça", listOf("squat", "lunge", "glute-bridge", "romanian-deadlift", "jumping-jack")),
    UPPER("Üst Vücut", "Göğüs, sırt, omuz, kollar", listOf("pushup", "dumbbell-row", "shoulder-press", "biceps-curl", "lateral-raise")),
    LOWER("Alt Vücut", "Bacaklar ve kalça", listOf("squat", "romanian-deadlift", "lunge", "glute-bridge", "jumping-jack")),
    FULL_A("Tüm Vücut A", "Tüm vücut", listOf("squat", "pushup", "dumbbell-row", "glute-bridge", "jumping-jack")),
    FULL_B("Tüm Vücut B", "Tüm vücut", listOf("lunge", "shoulder-press", "romanian-deadlift", "pushup", "jumping-jack")),
    FULL_C("Tüm Vücut C", "Tüm vücut", listOf("squat", "dumbbell-row", "lateral-raise", "glute-bridge", "jumping-jack")),
    CONDITION("Kondisyon", "Kardiyo ve kalça-core", listOf("jumping-jack", "squat", "glute-bridge", "lunge", "pushup")),
}

/** Ekipman yokken eksik kalan günleri doldurmak için vücut ağırlığı hareketleri. */
private val BODYWEIGHT_FALLBACK = listOf("squat", "pushup", "lunge", "glute-bridge", "jumping-jack")

private fun split(days: Int, equipment: Equipment): List<Day> {
    // Çekiş hareketleri dambıl ister; ekipman yoksa tüm vücut günleri kullanılır
    val canPull = equipment == Equipment.DUMBBELLS
    return when (days.coerceIn(2, 5)) {
        2 -> listOf(Day.FULL_A, Day.FULL_B)
        3 -> if (canPull) listOf(Day.PUSH, Day.PULL, Day.LEGS) else listOf(Day.FULL_A, Day.FULL_B, Day.FULL_C)
        4 -> listOf(Day.UPPER, Day.LOWER, Day.UPPER, Day.LOWER)
        else -> if (canPull) {
            listOf(Day.PUSH, Day.PULL, Day.LEGS, Day.FULL_A, Day.CONDITION)
        } else {
            listOf(Day.FULL_A, Day.LOWER, Day.FULL_B, Day.CONDITION, Day.FULL_C)
        }
    }
}

private fun schedule(days: Int): String = when (days.coerceIn(2, 5)) {
    2 -> "Pazartesi · Perşembe"
    3 -> "Pazartesi · Çarşamba · Cuma"
    4 -> "Pazartesi · Salı · Perşembe · Cuma"
    else -> "Pazartesi · Salı · Çarşamba · Cuma · Cumartesi"
}

/** Hedef ve seviyeye göre set × tekrar. */
private fun volume(goal: Goal, level: Level, exerciseId: String): Pair<Int, Int> {
    val baseSets = when (level) {
        Level.BEGINNER -> 2
        Level.INTERMEDIATE -> 3
        Level.ADVANCED -> 4
    }
    if (exerciseId == "jumping-jack") {
        return baseSets to if (goal == Goal.FAT_LOSS) 30 else 20
    }
    val reps = when (goal) {
        Goal.STRENGTH -> if (level == Level.BEGINNER) 10 else 8
        Goal.FAT_LOSS -> 15
        Goal.TONE -> 15
        Goal.FITNESS -> 12
    }
    val sets = if (goal == Goal.STRENGTH) (baseSets + 1).coerceAtMost(5) else baseSets
    return sets to reps
}

/** Seans süresine göre günde kaç hareket. */
private fun exercisesPerDay(minutes: Int): Int = when {
    minutes <= 20 -> 3
    minutes <= 30 -> 4
    else -> 5
}

/**
 * Kişinin cevaplarına göre haftalık program önerir. Cihaz üzerinde, kurallarla hesaplanır:
 * gün sayısına göre bölünme (tüm vücut, itiş-çekiş-bacak, üst-alt), ekipmana göre hareket seçimi,
 * hedef ve seviyeye göre set-tekrar, seans süresine göre hareket sayısı.
 */
fun recommend(req: RecommendationRequest): Recommendation {
    val allowed: (String) -> Boolean = { id ->
        exerciseById(id) != null && (req.equipment == Equipment.DUMBBELLS || id !in DUMBBELL_EXERCISES)
    }
    val perDay = exercisesPerDay(req.minutes)
    val days = split(req.daysPerWeek, req.equipment)
    val group = "${req.goal.title} programı · ${days.size} gün"
    val stamp = nowMillis()

    val plans = days.mapIndexed { i, day ->
        var pool = day.pool.filter(allowed)
        if (pool.size < perDay) pool = (pool + BODYWEIGHT_FALLBACK.filter(allowed)).distinct()
        // Yağ yakımında her güne ısınma olarak jumping jack
        if (req.goal == Goal.FAT_LOSS && "jumping-jack" !in pool.take(perDay)) pool = listOf("jumping-jack") + pool
        // Odak bölgedeki hareketler öne alınır
        if (req.focus.isNotEmpty()) {
            // Günün havuzunda odak bölgeden hareket yoksa birini ekle
            for (cat in req.focus) {
                if (pool.none { exerciseById(it)?.category == cat }) {
                    EXERCISES.firstOrNull { it.category == cat && allowed(it.id) }?.let { pool = listOf(it.id) + pool }
                }
            }
            pool = pool.sortedByDescending { id -> exerciseById(id)?.category in req.focus }
        }
        val chosen = pool.distinct().take(perDay)
        WorkoutPlan(
            id = "rec-$stamp-$i",
            name = "Gün ${i + 1} · ${day.title}",
            description = day.description,
            items = chosen.map { id ->
                val (sets, reps) = volume(req.goal, req.level, id)
                PlanItem(id, sets, reps)
            },
            group = group,
        )
    }

    val rest = when (req.goal) {
        Goal.STRENGTH -> "Setler arasında 90-120 saniye dinlen."
        Goal.FAT_LOSS -> "Setler arasında 30-45 saniye dinlen, tempoyu yüksek tut."
        Goal.TONE -> "Setler arasında 45-60 saniye dinlen."
        Goal.FITNESS -> "Setler arasında 60 saniye dinlen."
    }
    val notes = buildList {
        add(rest)
        add("Antrenman günleri arasında en az bir gün dinlenmeye çalış.")
        when (req.level) {
            Level.BEGINNER -> add("İlk iki hafta formu oturtmaya odaklan; doğruluk %80'in üstüne çıkınca tekrarları artır.")
            Level.INTERMEDIATE -> add("Her hafta bir harekette 1-2 tekrar artırmayı hedefle.")
            Level.ADVANCED -> add("Tempoyu yavaşlatarak (3 saniye iniş) hareketleri zorlaştırabilirsin.")
        }
        if (req.equipment == Equipment.NONE) add("Dambıl edindiğinde çekiş hareketleriyle daha dengeli bir program önerebiliriz.")
        add("Her seti FormFit kamerasıyla yap; doğru ve yanlış tekrarların programına kaydedilir.")
    }
    val focusText = if (req.focus.isEmpty()) "dengeli" else req.focus.joinToString(", ") { it.title.lowercase() } + " ağırlıklı"
    return Recommendation(
        title = group,
        summary = "${req.level.title.lowercase().replaceFirstChar { it.uppercase() }} seviyesi için, haftada ${days.size} gün, " +
            "seans başına yaklaşık ${req.minutes} dakika, $focusText bir program.",
        schedule = schedule(req.daysPerWeek),
        plans = plans,
        notes = notes,
    )
}
