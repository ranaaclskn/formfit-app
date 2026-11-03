package com.ranaaclskn.formfit

import com.ranaaclskn.formfit.data.RepRecord
import com.ranaaclskn.formfit.data.WorkoutPlan
import com.ranaaclskn.formfit.data.WorkoutSession
import com.ranaaclskn.formfit.data.decodeBackup
import com.ranaaclskn.formfit.data.encodeBackup
import com.ranaaclskn.formfit.data.mergeBackup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BackupTest {
    private fun s(id: String, at: Long) = WorkoutSession(id, "squat", "Squat", at, 1000, listOf(RepRecord(1, true, durationMs = 900)), planName = "Bacak")
    private fun p(id: String, group: String) = WorkoutPlan(id, "Gün $id", group = group)

    @Test
    fun roundTripKeepsEverything() {
        val text = encodeBackup(listOf(s("a", 1), s("b", 2)), listOf(p("x", "Güç")), listOf("Güç"), now = 99)
        val b = decodeBackup(text)!!
        assertEquals(listOf("a", "b"), b.sessions.map { it.id })
        assertEquals("Bacak", b.sessions.first().planName)
        assertEquals(listOf("Güç"), b.programOrder)
        assertEquals(99, b.exportedAt)
    }

    @Test
    fun mergeAddsOnlyMissingRecords() {
        val backup = decodeBackup(encodeBackup(listOf(s("a", 1), s("c", 3)), listOf(p("x", "Güç"), p("y", "Kondisyon")), listOf("Kondisyon", "Güç"), 0))!!
        val r = mergeBackup(listOf(s("a", 1), s("b", 2)), listOf(p("x", "Güç")), listOf("Güç"), backup)
        assertEquals(1, r.addedSessions)
        assertEquals(1, r.addedPlans)
        assertEquals(listOf("c", "b", "a"), r.sessions.map { it.id })
        assertEquals(listOf("Güç", "Kondisyon"), r.programOrder) // mevcut öncelik korunur
    }

    @Test
    fun invalidFilesAreRejected() {
        assertNull(decodeBackup("merhaba"))
        assertNull(decodeBackup("""{"app":"BaskaUygulama","sessions":[]}"""))
    }
}
