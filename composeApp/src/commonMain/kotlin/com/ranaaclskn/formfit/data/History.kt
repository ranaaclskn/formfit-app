package com.ranaaclskn.formfit.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RepRecord(
    val index: Int,
    val correct: Boolean,
    val faults: List<String> = emptyList(),
    val durationMs: Long,
)

@Serializable
data class WorkoutSession(
    val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val startedAt: Long,
    val durationMs: Long,
    val reps: List<RepRecord>,
) {
    val correctCount: Int get() = reps.count { it.correct }
    val incorrectCount: Int get() = reps.size - correctCount
    /** 0..100 */
    val accuracy: Int get() = if (reps.isEmpty()) 0 else correctCount * 100 / reps.size

    /** En sık yapılan hatalar, çoktan aza. */
    fun topFaults(): List<Pair<String, Int>> =
        reps.flatMap { it.faults }.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }
}

@Serializable
private data class HistoryFile(val version: Int = 1, val sessions: List<WorkoutSession> = emptyList())

/** Antrenman geçmişi; cihazda tek bir JSON dosyasında tutulur. */
object HistoryRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _sessions = MutableStateFlow<List<WorkoutSession>>(emptyList())
    val sessions: StateFlow<List<WorkoutSession>> = _sessions.asStateFlow()
    private var loaded = false

    fun load() {
        if (loaded) return
        loaded = true
        val text = runCatching { readHistoryFile() }.getOrNull() ?: return
        val file = runCatching { json.decodeFromString<HistoryFile>(text) }.getOrNull() ?: return
        _sessions.value = file.sessions.sortedByDescending { it.startedAt }
    }

    fun add(session: WorkoutSession) {
        load()
        _sessions.value = (listOf(session) + _sessions.value).sortedByDescending { it.startedAt }
        save()
    }

    fun delete(id: String) {
        _sessions.value = _sessions.value.filterNot { it.id == id }
        save()
    }

    fun find(id: String): WorkoutSession? = _sessions.value.firstOrNull { it.id == id }

    private fun save() {
        runCatching { writeHistoryFile(json.encodeToString(HistoryFile(sessions = _sessions.value))) }
    }
}
