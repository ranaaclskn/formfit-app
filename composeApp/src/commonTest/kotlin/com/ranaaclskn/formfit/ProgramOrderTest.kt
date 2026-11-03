package com.ranaaclskn.formfit

import com.ranaaclskn.formfit.data.DEFAULT_PROGRAM
import com.ranaaclskn.formfit.data.PRESET_GROUP
import com.ranaaclskn.formfit.data.WorkoutPlan
import com.ranaaclskn.formfit.data.orderedPrograms
import com.ranaaclskn.formfit.data.programName
import kotlin.test.Test
import kotlin.test.assertEquals

class ProgramOrderTest {
    private fun day(id: String, group: String) = WorkoutPlan(id, id, group = group)

    @Test
    fun programsFollowPriorityAndSkipPresets() {
        val plans = listOf(
            day("a", "Güç · 3 gün"), day("b", "Güç · 3 gün"), day("c", ""), day("d", "Programlarım"),
            day("e", PRESET_GROUP), day("f", "Kondisyon"),
        )
        // Sıra verilmemişse ilk görülme sırası
        assertEquals(listOf("Güç · 3 gün", DEFAULT_PROGRAM, "Kondisyon"), orderedPrograms(plans, emptyList()))
        // Öncelik sırası uygulanır, silinmiş program yok sayılır, sırası olmayanlar sona eklenir
        assertEquals(
            listOf("Kondisyon", "Güç · 3 gün", DEFAULT_PROGRAM),
            orderedPrograms(plans, listOf("Kondisyon", "Silinmiş", "Güç · 3 gün")),
        )
    }

    @Test
    fun oldOwnPlansBelongToDefaultProgram() {
        assertEquals(DEFAULT_PROGRAM, day("x", "").programName)
        assertEquals(DEFAULT_PROGRAM, day("y", "Programlarım").programName)
        assertEquals("Bacak", day("z", "Bacak").programName)
    }
}
