package com.ranaaclskn.formfit.data

/** Bir antrenmanın grafikteki noktası. */
data class ProgressPoint(val date: Long, val accuracy: Int, val reps: Int)

data class ExerciseProgress(val exerciseId: String, val exerciseName: String, val points: List<ProgressPoint>) {
    val first: Int? get() = points.firstOrNull()?.accuracy
    val last: Int? get() = points.lastOrNull()?.accuracy
    val best: Int? get() = points.maxOfOrNull { it.accuracy }
    val totalReps: Int get() = points.sumOf { it.reps }

    /** "%62'den %85'e çıktı" gibi kısa yorum; tek antrenman varsa null. */
    fun trend(): String? {
        val a = first ?: return null
        val b = last ?: return null
        if (points.size < 2) return null
        val diff = b - a
        return when {
            diff > 0 -> "Doğruluğun ilk antrenmanına göre %$a'den %$b'e çıktı (+$diff). Harika gidiyorsun!"
            diff < 0 -> "Doğruluğun %$a'den %$b'e düştü ($diff). Hatalarım bölümüne bakıp formuna odaklan."
            else -> "Doğruluğun %$a'de sabit. Bir sonraki hedefin daha temiz tekrarlar."
        }
    }
}

/** Antrenman yapılmış hareketler, en çok çalışılandan aza. */
fun practicedExercises(sessions: List<WorkoutSession>): List<Pair<String, String>> =
    sessions.groupBy { it.exerciseId }
        .entries.sortedByDescending { it.value.size }
        .map { it.key to it.value.first().exerciseName }

/** Bir hareketin son [maxPoints] antrenmanı, eskiden yeniye. */
fun exerciseProgress(sessions: List<WorkoutSession>, exerciseId: String, maxPoints: Int = 10): ExerciseProgress {
    val mine = sessions.filter { it.exerciseId == exerciseId && it.reps.isNotEmpty() }.sortedBy { it.startedAt }
    return ExerciseProgress(
        exerciseId = exerciseId,
        exerciseName = mine.firstOrNull()?.exerciseName ?: exerciseId,
        points = mine.takeLast(maxPoints).map { ProgressPoint(it.startedAt, it.accuracy, it.reps.size) },
    )
}
