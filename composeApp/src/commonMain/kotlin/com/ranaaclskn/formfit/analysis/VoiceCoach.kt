package com.ranaaclskn.formfit.analysis

/**
 * Antrenman sırasında ekrana yazı yazmak yerine sesli geri bildirim verir: tekrar sonucu,
 * form uyarıları ve kurulum yönlendirmeleri. Aynı cümleyi sürekli tekrarlamaması için
 * uyarılar seyreltilir; tekrar sonucu her zaman söylenir.
 */
class VoiceCoach(
    private val say: (String) -> Unit,
    private val clock: () -> Long,
) {
    var enabled = true
    private var lastText: String? = null
    private var lastAt = Long.MIN_VALUE / 2
    private var targetAnnounced = false

    /** Bir tekrar tamamlandı. */
    fun onRep(rep: RepResult, correctCount: Int) {
        if (rep.correct) speak("Doğru, $correctCount", force = true)
        else speak("Yanlış. ${rep.faults.firstOrNull() ?: ""}".trim(), force = true)
    }

    /** Sayacın canlı geri bildirimi değişti. */
    fun onFeedback(feedback: LiveFeedback) {
        when (feedback.kind) {
            // Tekrar sırasında bozulan form: hemen uyar, ama aynı uyarıyı 4 sn içinde tekrarlama
            FeedbackKind.WARN -> speak(feedback.message, minGapSameMs = 4000)
            // Kurulum yönlendirmeleri ("Tüm vücudun kamerada görünsün"): seyrek söyle
            FeedbackKind.INFO -> if (!feedback.message.endsWith("…")) speak(feedback.message, minGapSameMs = 8000)
            // Doğru/yanlış sonuçları onRep ile söylenir
            FeedbackKind.GOOD, FeedbackKind.BAD -> Unit
        }
    }

    /** Başlamadan önce kamera yerleşimi değişti: kadraj/açı yönlendirmesi. */
    fun onPlacement(message: String) = speak(message, minGapSameMs = 6000)

    fun onTargetReached() {
        if (targetAnnounced) return
        targetAnnounced = true
        speak("Hedefe ulaştın, seti bitirebilirsin", force = true)
    }

    private fun speak(text: String, minGapSameMs: Long = 0, force: Boolean = false) {
        if (!enabled || text.isBlank()) return
        val now = clock()
        if (!force) {
            if (text == lastText && now - lastAt < minGapSameMs) return
            // Bir önceki cümle bitmeden yenisine geçme
            if (now - lastAt < MIN_GAP_MS) return
        }
        say(text)
        lastText = text
        lastAt = now
    }

    private companion object {
        const val MIN_GAP_MS = 1500L
    }
}
