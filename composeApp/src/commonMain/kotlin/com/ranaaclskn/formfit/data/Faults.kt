package com.ranaaclskn.formfit.data

/** Bir hatanın belli bir gündeki tekrarı. */
data class FaultOccurrence(val date: Long, val count: Int)

/** Bir harekette yapılan bir hata ve ne zaman, kaç kez yapıldığı (yeniden eskiye). */
data class FaultEntry(
    val exerciseId: String,
    val exerciseName: String,
    val fault: String,
    val occurrences: List<FaultOccurrence>,
) {
    val total: Int get() = occurrences.sumOf { it.count }
    val lastDate: Long get() = occurrences.maxOf { it.date }
}

/** Tüm antrenmanlardaki hataları hareket ve hata metnine göre toplar; en son yapılan önce. */
fun faultLog(sessions: List<WorkoutSession>): List<FaultEntry> =
    sessions
        .flatMap { s -> s.reps.flatMap { r -> r.faults.map { f -> Triple(s, f, 1) } } }
        .groupBy { (s, f, _) -> s.exerciseId to f }
        .map { (key, list) ->
            FaultEntry(
                exerciseId = key.first,
                exerciseName = list.first().first.exerciseName,
                fault = key.second,
                occurrences = list.groupBy { it.first.id }
                    .map { (_, inSession) -> FaultOccurrence(inSession.first().first.startedAt, inSession.size) }
                    .sortedByDescending { it.date },
            )
        }
        .sortedWith(compareByDescending<FaultEntry> { it.lastDate }.thenByDescending { it.total })
