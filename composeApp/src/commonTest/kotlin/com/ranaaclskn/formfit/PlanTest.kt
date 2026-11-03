package com.ranaaclskn.formfit

import com.ranaaclskn.formfit.data.DUMBBELL_EXERCISES
import com.ranaaclskn.formfit.data.Equipment
import com.ranaaclskn.formfit.data.Goal
import com.ranaaclskn.formfit.data.Level
import com.ranaaclskn.formfit.data.PLAN_TEMPLATES
import com.ranaaclskn.formfit.data.RecommendationRequest
import com.ranaaclskn.formfit.data.RepRecord
import com.ranaaclskn.formfit.data.WorkoutSession
import com.ranaaclskn.formfit.data.encodeSetNumber
import com.ranaaclskn.formfit.data.recommend
import com.ranaaclskn.formfit.data.runProgress
import com.ranaaclskn.formfit.model.Category
import com.ranaaclskn.formfit.model.exerciseById
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlanTest {
    @Test
    fun presetsOnlyUseKnownExercises() {
        for (p in PLAN_TEMPLATES) for (i in p.items) assertTrue(exerciseById(i.exerciseId) != null, i.exerciseId)
    }

    @Test
    fun threeDaysWithDumbbellsIsPushPullLegs() {
        val r = recommend(RecommendationRequest(Goal.STRENGTH, Level.INTERMEDIATE, 3, Equipment.DUMBBELLS, 30))
        assertEquals(listOf("Gün 1 · İtiş", "Gün 2 · Çekiş", "Gün 3 · Bacak"), r.plans.map { it.name })
        assertTrue(r.plans.all { it.items.size == 4 })
        assertTrue(r.plans.flatMap { it.items }.filter { it.exerciseId != "jumping-jack" }.all { it.reps == 8 && it.sets == 4 })
    }

    @Test
    fun noEquipmentNeverSuggestsDumbbellExercises() {
        for (days in 2..5) for (goal in Goal.entries) {
            val r = recommend(RecommendationRequest(goal, Level.BEGINNER, days, Equipment.NONE, 45))
            val ids = r.plans.flatMap { p -> p.items.map { it.exerciseId } }
            assertTrue(ids.none { it in DUMBBELL_EXERCISES }, "$days gün $goal: $ids")
            assertTrue(r.plans.all { it.items.isNotEmpty() && it.items.map { i -> i.exerciseId }.distinct().size == it.items.size })
        }
    }

    @Test
    fun fatLossStartsEveryDayWithCardio() {
        val r = recommend(RecommendationRequest(Goal.FAT_LOSS, Level.BEGINNER, 4, Equipment.DUMBBELLS, 20))
        assertTrue(r.plans.all { it.items.any { i -> i.exerciseId == "jumping-jack" } })
        assertTrue(r.plans.all { it.items.size == 3 })
    }

    @Test
    fun focusPutsChosenAreaFirst() {
        val r = recommend(RecommendationRequest(Goal.TONE, Level.INTERMEDIATE, 2, Equipment.DUMBBELLS, 30, setOf(Category.CORE)))
        assertTrue(r.plans.all { exerciseById(it.items.first().exerciseId)?.category == Category.CORE })
    }

    @Test
    fun runProgressCountsSetsPerExercise() {
        val plan = PLAN_TEMPLATES.first()
        fun session(itemIndex: Int, set: Int, ok: Int, bad: Int) = WorkoutSession(
            id = "s$itemIndex$set",
            exerciseId = plan.items[itemIndex].exerciseId,
            exerciseName = "",
            startedAt = (itemIndex * 10 + set).toLong(),
            durationMs = 1000,
            reps = List(ok) { RepRecord(it, true, durationMs = 1) } + List(bad) { RepRecord(it, false, durationMs = 1) },
            planId = plan.id,
            planRunId = "run1",
            setNumber = encodeSetNumber(itemIndex, set),
        )
        val p = runProgress(plan, "run1", listOf(session(0, 1, 9, 1), session(0, 2, 8, 2), session(1, 1, 10, 0)))
        assertEquals(2, p.items[0].doneSets)
        assertEquals(1, p.items[1].doneSets)
        assertEquals(0, p.nextIndex)
        assertEquals(27, p.correct)
        assertEquals(90, p.accuracy)
    }
}
