package com.ranaaclskn.formfit

import com.ranaaclskn.formfit.data.RepRecord
import com.ranaaclskn.formfit.data.WorkoutSession
import com.ranaaclskn.formfit.data.exerciseProgress
import com.ranaaclskn.formfit.data.practicedExercises
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProgressTest {
    private fun s(id: String, ex: String, at: Long, ok: Int, bad: Int) = WorkoutSession(
        id, ex, ex.uppercase(), at, 1000,
        List(ok) { RepRecord(it, true, durationMs = 1) } + List(bad) { RepRecord(it, false, listOf("x"), 1) },
    )

    @Test
    fun pointsAreChronologicalAndLimited() {
        val sessions = (1..15).map { s("s$it", "squat", it * 100L, it, 15 - it) } + s("p", "pushup", 50, 5, 5)
        val p = exerciseProgress(sessions, "squat")
        assertEquals(10, p.points.size)
        assertEquals((6..15).map { it * 100L }, p.points.map { it.date })
        assertEquals(40, p.first) // 6 doğru / 15
        assertEquals(100, p.last)
        assertTrue(p.trend()!!.contains("çıktı"))
        assertEquals(listOf("squat", "pushup"), practicedExercises(sessions).map { it.first })
    }

    @Test
    fun singleSessionHasNoTrend() {
        val p = exerciseProgress(listOf(s("a", "squat", 1, 5, 5)), "squat")
        assertNull(p.trend())
        assertEquals(50, p.best)
    }
}
