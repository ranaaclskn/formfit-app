package com.ranaaclskn.formfit

import com.ranaaclskn.formfit.analysis.restCue
import com.ranaaclskn.formfit.analysis.restSecondsFor
import com.ranaaclskn.formfit.model.exerciseById
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RestTest {
    @Test
    fun restDependsOnLoad() {
        assertEquals(120, restSecondsFor(exerciseById("bench-press")!!, 8))
        assertEquals(90, restSecondsFor(exerciseById("squat")!!, 12))
        assertEquals(60, restSecondsFor(exerciseById("glute-bridge")!!, 15))
        assertEquals(45, restSecondsFor(exerciseById("jumping-jack")!!, 20))
    }

    @Test
    fun cuesAreSpokenAtTheRightSeconds() {
        val spoken = (90 downTo 0).mapNotNull { restCue(it, 90) }
        assertEquals(
            listOf("1 dakika kaldı", "30 saniye kaldı", "10 saniye kaldı, hazırlan", "3", "2", "1",
                "Dinlenme bitti. Sıradaki sete başlayabilirsin."),
            spoken,
        )
        // 45 sn'lik dinlenmede "1 dakika kaldı" söylenmez
        assertNull(restCue(60, 45))
    }
}
