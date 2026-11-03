package com.ranaaclskn.formfit.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Antrenman geçmişi ve programların tek dosyalık yedeği. */
@Serializable
data class BackupFile(
    val app: String = BACKUP_APP,
    val version: Int = 1,
    val exportedAt: Long = 0,
    val sessions: List<WorkoutSession> = emptyList(),
    val plans: List<WorkoutPlan> = emptyList(),
    val programOrder: List<String> = emptyList(),
)

data class MergeResult(
    val sessions: List<WorkoutSession>,
    val plans: List<WorkoutPlan>,
    val programOrder: List<String>,
    val addedSessions: Int,
    val addedPlans: Int,
)

private const val BACKUP_APP = "FormFit"
private val backupJson = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

fun encodeBackup(sessions: List<WorkoutSession>, plans: List<WorkoutPlan>, order: List<String>, now: Long): String =
    backupJson.encodeToString(BackupFile(exportedAt = now, sessions = sessions, plans = plans, programOrder = order))

/** Geçerli bir FormFit yedeği değilse null. */
fun decodeBackup(text: String): BackupFile? =
    runCatching { backupJson.decodeFromString<BackupFile>(text) }.getOrNull()?.takeIf { it.app == BACKUP_APP }

/**
 * Yedeği mevcut verinin üstüne yazmaz, birleştirir: telefonda olmayan antrenmanlar ve
 * program günleri eklenir; aynı kimlikli kayıtlar tekrar eklenmez.
 */
fun mergeBackup(
    sessions: List<WorkoutSession>,
    plans: List<WorkoutPlan>,
    order: List<String>,
    backup: BackupFile,
): MergeResult {
    val sessionIds = sessions.map { it.id }.toSet()
    val planIds = plans.map { it.id }.toSet()
    val newSessions = backup.sessions.filter { it.id !in sessionIds }
    val newPlans = backup.plans.filter { it.id !in planIds }
    return MergeResult(
        sessions = (sessions + newSessions).sortedByDescending { it.startedAt },
        plans = plans + newPlans,
        programOrder = order + backup.programOrder.filter { it !in order },
        addedSessions = newSessions.size,
        addedPlans = newPlans.size,
    )
}

/** Uygulamanın verisini yedekler ve yedekten geri yükler. */
object BackupManager {
    fun export(): String {
        HistoryRepository.load()
        PlanRepository.load()
        return encodeBackup(HistoryRepository.sessions.value, PlanRepository.plans.value, PlanRepository.programOrder.value, nowMillis())
    }

    /** Kullanıcıya gösterilecek sonuç mesajı. */
    fun import(text: String?): String {
        val backup = text?.let { decodeBackup(it) } ?: return "Dosya okunamadı. Lütfen bir FormFit yedek dosyası seç."
        HistoryRepository.load()
        PlanRepository.load()
        val r = mergeBackup(HistoryRepository.sessions.value, PlanRepository.plans.value, PlanRepository.programOrder.value, backup)
        HistoryRepository.restore(r.sessions)
        PlanRepository.restore(r.plans, r.programOrder)
        return if (r.addedSessions + r.addedPlans == 0) "Yedekteki her şey zaten telefonunda."
        else "Yedek geri yüklendi: ${r.addedSessions} antrenman, ${r.addedPlans} program günü eklendi."
    }

    fun fileName(): String = "formfit-yedek-${formatDateTime(nowMillis()).substringBefore(",").replace(" ", "-")}.json"
}
