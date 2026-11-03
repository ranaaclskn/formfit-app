package com.ranaaclskn.formfit

import com.ranaaclskn.formfit.data.PLAN_TEMPLATES
import com.ranaaclskn.formfit.data.RepRecord
import com.ranaaclskn.formfit.data.WorkoutSession
import com.ranaaclskn.formfit.data.encodeSetNumber
import com.ranaaclskn.formfit.data.planRuns
import com.ranaaclskn.formfit.data.runProgress
import com.ranaaclskn.formfit.model.exerciseById
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PlanProgressTest {
    private val plan = PLAN_TEMPLATES.first { it.id == "preset-push" }

    private fun set(itemIndex: Int, set: Int, correct: Int, wrong: Int, runId: String = "run-1", at: Long = 1000L + itemIndex * 10 + set) =
        WorkoutSession(
            id = "s-$runId-$itemIndex-$set",
            exerciseId = plan.items[itemIndex].exerciseId,
            exerciseName = "x",
            startedAt = at,
            durationMs = 1000,
            reps = List(correct) { RepRecord(it, true, durationMs = 1000) } + List(wrong) { RepRecord(it, false, durationMs = 1000) },
            planId = plan.id,
            planName = plan.name,
            planRunId = runId,
            setNumber = encodeSetNumber(itemIndex, set),
        )

    @Test
    fun templatesOnlyUseKnownExercises() {
        for (t in PLAN_TEMPLATES) for (item in t.items) assertNotNull(exerciseById(item.exerciseId), "${t.id}: ${item.exerciseId}")
    }

    @Test
    fun progressCountsSetsAndAccuracy() {
        val sessions = listOf(set(0, 1, 8, 2), set(0, 2, 10, 0), set(1, 1, 5, 5), set(0, 1, 1, 1, runId = "other"))
        val p = runProgress(plan, "run-1", sessions)
        assertEquals(listOf(2, 1, 0), p.items.map { it.doneSets })
        assertEquals(3, p.doneSets)
        assertEquals(9, p.totalSets)
        assertEquals(23, p.correct)
        assertEquals(7, p.incorrect)
        assertEquals(76, p.accuracy)
        assertEquals(0, p.nextIndex)
    }

    @Test
    fun finishedPlanHasNoNextExercise() {
        val sessions = plan.items.flatMapIndexed { i, item -> (1..item.sets).map { s -> set(i, s, 10, 0) } }
        val p = runProgress(plan, "run-1", sessions)
        assertNull(p.nextIndex)
        assertEquals(100, p.accuracy)
    }

    @Test
    fun runsAreGroupedNewestFirst() {
        val sessions = listOf(set(0, 1, 5, 5, runId = "a", at = 100), set(0, 1, 10, 0, runId = "b", at = 200))
        val runs = planRuns(plan.id, sessions)
        assertEquals(listOf("b", "a"), runs.map { it.runId })
        assertEquals(listOf(100, 50), runs.map { it.accuracy })
    }
}
