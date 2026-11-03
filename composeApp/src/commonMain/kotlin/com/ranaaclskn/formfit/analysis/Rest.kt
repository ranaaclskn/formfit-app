package com.ranaaclskn.formfit.analysis

import com.ranaaclskn.formfit.model.Category
import com.ranaaclskn.formfit.model.Exercise

/**
 * Setler arası dinlenme süresi: ağır ve az tekrarlı setlerde daha uzun, kardiyoda kısa.
 * (Güç için 2 dk, kas/dayanıklılık için 60-90 sn yaygın öneridir.)
 */
fun restSecondsFor(exercise: Exercise, reps: Int): Int = when {
    exercise.category == Category.CARDIO -> 45
    reps <= 8 -> 120
    reps >= 15 -> 60
    else -> 90
}

/** Dinlenme sırasında belli saniyelerde sesli söylenecek cümle; diğer saniyelerde null. */
fun restCue(secondsLeft: Int, total: Int): String? = when (secondsLeft) {
    60 -> if (total > 60) "1 dakika kaldı" else null
    30 -> if (total > 30) "30 saniye kaldı" else null
    10 -> "10 saniye kaldı, hazırlan"
    3 -> "3"
    2 -> "2"
    1 -> "1"
    0 -> "Dinlenme bitti. Sıradaki sete başlayabilirsin."
    else -> null
}
