package com.ranaaclskn.formfit.analysis

import com.ranaaclskn.formfit.model.PoseFrame

data class RepResult(
    val index: Int,
    val correct: Boolean,
    val faults: List<String>,
    val durationMs: Long,
    val peakAngle: Float,
)

enum class FeedbackKind { INFO, GOOD, WARN, BAD }

data class LiveFeedback(val message: String, val kind: FeedbackKind)

/**
 * Kameradan gelen karelere bakarak tekrarları sayar ve her tekrarı doğru/yanlış olarak sınıflar.
 * Platformdan bağımsızdır; kareler sırayla [process] fonksiyonuna verilir.
 */
class RepCounter(private val rule: ExerciseRule) {

    enum class Phase { WAITING_FOR_REST, REST, IN_REP }

    var phase = Phase.WAITING_FOR_REST
        private set
    var correctCount = 0
        private set
    var incorrectCount = 0
        private set
    val reps = mutableListOf<RepResult>()
    var feedback = LiveFeedback("Tüm vücudun kamerada görünecek şekilde geç", FeedbackKind.INFO)
        private set
    /** Son karedeki (yumuşatılmış) ana açı; ekranda göstermek için. */
    var currentAngle: Float? = null
        private set

    private var smoothed: Float? = null
    private var lastValidMs = 0L
    private var repStartMs = 0L
    private var peak = 0f
    private var reachedTarget = false
    private var context: RepContext? = null
    private val checkFails = IntArray(rule.checks.size)
    private val checkTotals = IntArray(rule.checks.size)

    private val motion = rule.primary
    private val decreasing = motion.direction == Direction.DECREASING

    /** Kareyi işler; bir tekrar tamamlandıysa sonucunu döndürür. Kişi görünmüyorsa frame null olabilir. */
    fun process(frame: PoseFrame?, timestampMs: Long = frame?.timestampMs ?: 0L): RepResult? {
        val raw = frame?.let { primaryAngle(it) }
        if (frame == null || raw == null) {
            onLost(timestampMs, "Tüm vücudun kamerada görünecek şekilde geç")
            return null
        }
        if (rule.gate != null && !rule.gate.invoke(frame)) {
            onLost(timestampMs, rule.gateMessage)
            return null
        }
        lastValidMs = timestampMs

        val prev = smoothed
        val a = if (prev == null) raw else prev + SMOOTHING * (raw - prev)
        smoothed = a
        currentAngle = a

        when (phase) {
            Phase.WAITING_FOR_REST -> {
                if (atRest(a)) {
                    phase = Phase.REST
                    feedback = LiveFeedback("Hazırsın, harekete başla", FeedbackKind.INFO)
                } else {
                    feedback = LiveFeedback(rule.gateMessage, FeedbackKind.INFO)
                }
            }
            Phase.REST -> if (pastEnter(a)) startRep(frame, a, timestampMs)
            Phase.IN_REP -> {
                if (timestampMs - repStartMs > MAX_REP_MS) {
                    cancelRep("Çok uzun beklendi, başlangıç pozisyonuna dön")
                    return null
                }
                updateRep(frame, a)
                if (atRest(a)) return finishRep(timestampMs)
            }
        }
        return null
    }

    fun reset() {
        phase = Phase.WAITING_FOR_REST
        correctCount = 0
        incorrectCount = 0
        reps.clear()
        smoothed = null
        currentAngle = null
    }

    private fun primaryAngle(frame: PoseFrame): Float? {
        val sides = listOf(motion.left, motion.right)
            .filter { frame.isVisible(it.a, it.b, it.c) }
            .map { frame.angle(it.a, it.b, it.c) }
        if (sides.isEmpty()) return null
        return when (motion.sideMode) {
            SideMode.MIN -> sides.min()
            SideMode.MAX -> sides.max()
            SideMode.AVERAGE -> sides.average().toFloat()
        }
    }

    private fun pastEnter(a: Float) = if (decreasing) a < motion.enterAt else a > motion.enterAt
    private fun pastTarget(a: Float) = if (decreasing) a <= motion.targetAt else a >= motion.targetAt
    private fun atRest(a: Float) = if (decreasing) a > motion.exitAt else a < motion.exitAt

    private fun startRep(frame: PoseFrame, a: Float, timestampMs: Long) {
        phase = Phase.IN_REP
        repStartMs = timestampMs
        peak = a
        reachedTarget = false
        context = RepContext(frame)
        checkFails.fill(0)
        checkTotals.fill(0)
        feedback = LiveFeedback("Devam et…", FeedbackKind.INFO)
    }

    private fun updateRep(frame: PoseFrame, a: Float) {
        peak = if (decreasing) minOf(peak, a) else maxOf(peak, a)
        val atPeak = pastTarget(a)
        if (atPeak) reachedTarget = true
        val ctx = context ?: return

        var warning: String? = null
        rule.checks.forEachIndexed { i, check ->
            if (check.phase == CheckPhase.PEAK && !atPeak) return@forEachIndexed
            val ok = check.evaluate(frame, ctx) ?: return@forEachIndexed
            checkTotals[i]++
            if (!ok) {
                checkFails[i]++
                if (warning == null) warning = check.message
            }
        }
        feedback = when {
            warning != null -> LiveFeedback(warning!!, FeedbackKind.WARN)
            reachedTarget -> LiveFeedback("Güzel, şimdi başlangıca dön", FeedbackKind.GOOD)
            else -> LiveFeedback("Devam et…", FeedbackKind.INFO)
        }
    }

    private fun finishRep(timestampMs: Long): RepResult? {
        phase = Phase.REST
        val duration = timestampMs - repStartMs
        // Çok kısa salınımlar gerçek bir tekrar değil, ölçüm gürültüsü
        if (duration < NOISE_MS) {
            feedback = LiveFeedback("Hazırsın, harekete başla", FeedbackKind.INFO)
            return null
        }
        val faults = mutableListOf<String>()
        if (!reachedTarget) faults += rule.depthMessage
        rule.checks.forEachIndexed { i, check ->
            val total = checkTotals[i]
            if (total >= 2 && checkFails[i].toFloat() / total >= check.minFailRatio) faults += check.message
        }
        if (duration < rule.minRepMs) faults += "Çok hızlı, hareketi kontrollü yap"

        val correct = faults.isEmpty()
        if (correct) correctCount++ else incorrectCount++
        val result = RepResult(
            index = reps.size + 1,
            correct = correct,
            faults = faults,
            durationMs = duration,
            peakAngle = peak,
        )
        reps += result
        feedback = if (correct) {
            LiveFeedback("Doğru tekrar!", FeedbackKind.GOOD)
        } else {
            LiveFeedback(faults.first(), FeedbackKind.BAD)
        }
        return result
    }

    private fun onLost(timestampMs: Long, message: String) {
        if (phase == Phase.IN_REP && timestampMs - lastValidMs > LOST_TIMEOUT_MS) {
            cancelRep(message)
        } else if (phase != Phase.IN_REP) {
            feedback = LiveFeedback(message, FeedbackKind.INFO)
            smoothed = null
            currentAngle = null
            phase = Phase.WAITING_FOR_REST
        }
    }

    private fun cancelRep(message: String) {
        phase = Phase.WAITING_FOR_REST
        smoothed = null
        currentAngle = null
        feedback = LiveFeedback(message, FeedbackKind.INFO)
    }

    companion object {
        private const val SMOOTHING = 0.5f
        private const val NOISE_MS = 250L
        private const val LOST_TIMEOUT_MS = 1500L
        private const val MAX_REP_MS = 15_000L
    }
}
