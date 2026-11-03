package com.ranaaclskn.formfit

import com.ranaaclskn.formfit.analysis.FeedbackKind
import com.ranaaclskn.formfit.analysis.LiveFeedback
import com.ranaaclskn.formfit.analysis.RepResult
import com.ranaaclskn.formfit.analysis.VoiceCoach
import com.ranaaclskn.formfit.data.RepRecord
import com.ranaaclskn.formfit.data.WorkoutSession
import com.ranaaclskn.formfit.data.faultLog
import com.ranaaclskn.formfit.model.EXERCISES
import com.ranaaclskn.formfit.model.matches
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VoiceAndSearchTest {
    private var now = 0L
    private val said = mutableListOf<String>()
    private val coach = VoiceCoach(say = { said += it }, clock = { now })

    @Test
    fun repResultsAreAlwaysSpoken() {
        coach.onRep(RepResult(1, true, emptyList(), 1000, 90f), 1)
        coach.onRep(RepResult(2, false, listOf("Yeterince inmedin"), 1000, 120f), 1)
        assertEquals(listOf("Doğru, 1", "Yanlış. Yeterince inmedin"), said)
    }

    @Test
    fun sameWarningIsNotRepeatedTooOften() {
        val warn = LiveFeedback("Gövdeni dik tut", FeedbackKind.WARN)
        coach.onFeedback(warn)
        now += 2000; coach.onFeedback(warn)
        now += 3000; coach.onFeedback(warn)
        assertEquals(2, said.size)
    }

    @Test
    fun progressMessagesAreNotSpokenAndMuteWorks() {
        coach.onFeedback(LiveFeedback("Devam et…", FeedbackKind.INFO))
        coach.onFeedback(LiveFeedback("Güzel, şimdi başlangıca dön", FeedbackKind.GOOD))
        coach.enabled = false
        coach.onRep(RepResult(1, true, emptyList(), 1000, 90f), 1)
        assertTrue(said.isEmpty())
    }

    @Test
    fun targetIsAnnouncedOnce() {
        coach.onTargetReached(); coach.onTargetReached()
        assertEquals(1, said.size)
    }

    @Test
    fun faultLogGroupsByExerciseAndFaultWithDates() {
        fun session(id: String, at: Long, ex: String, vararg faults: List<String>) = WorkoutSession(
            id, ex, ex, at, 1000, faults.mapIndexed { i, f -> RepRecord(i + 1, f.isEmpty(), f, 1000) },
        )
        val log = faultLog(
            listOf(
                session("a", 100, "squat", listOf("Derin in"), listOf("Derin in", "Dik dur"), emptyList()),
                session("b", 200, "squat", listOf("Derin in")),
                session("c", 150, "pushup", listOf("Düz tut")),
            )
        )
        val deep = log.first { it.fault == "Derin in" }
        assertEquals(3, deep.total)
        assertEquals(listOf(200L, 100L), deep.occurrences.map { it.date })
        assertEquals(listOf(1, 2), deep.occurrences.map { it.count })
        assertEquals("Derin in", log.first().fault) // en son yapılan hata önce
        assertEquals(3, log.size)
    }

    @Test
    fun searchIgnoresTurkishCharactersAndCase() {
        fun ids(q: String) = EXERCISES.filter { it.matches(q) }.map { it.id }
        assertTrue("pushup" in ids("sinav"))
        assertTrue("pushup" in ids("ŞINAV"))
        assertTrue("lat-pulldown" in ids("lat"))
        assertTrue(ids("makine").isNotEmpty() && ids("makine").all { id -> EXERCISES.first { it.id == id }.gear.name == "MACHINE" || "makine" in id })
        assertTrue("biceps-curl" in ids("on kol"))
        assertEquals(EXERCISES.size, ids("  ").size)
        assertTrue(ids("xyzxyz").isEmpty())
    }
}
