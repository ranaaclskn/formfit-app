package com.ranaaclskn.formfit

import com.ranaaclskn.formfit.analysis.Direction
import com.ranaaclskn.formfit.analysis.RepCounter
import com.ranaaclskn.formfit.analysis.SideMode
import com.ranaaclskn.formfit.analysis.angle
import com.ranaaclskn.formfit.analysis.isVisible
import com.ranaaclskn.formfit.data.Equipment
import com.ranaaclskn.formfit.data.Goal
import com.ranaaclskn.formfit.data.Level
import com.ranaaclskn.formfit.data.RecommendationRequest
import com.ranaaclskn.formfit.data.recommend
import com.ranaaclskn.formfit.model.Category
import com.ranaaclskn.formfit.model.EXERCISES
import com.ranaaclskn.formfit.model.exerciseById
import com.ranaaclskn.formfit.model.Exercise
import com.ranaaclskn.formfit.model.Landmark
import com.ranaaclskn.formfit.model.Lm
import com.ranaaclskn.formfit.model.PoseFrame
import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import com.ranaaclskn.formfit.demo.Joint as DJ

/**
 * Her hareketin "nasıl yapılır" animasyonu, kameradan gelmiş gibi tekrar sayacına verilir.
 * Animasyon doğru formu gösterdiği için her tekrar DOĞRU sayılmalı; hareket yarıda
 * bırakılırsa (hedef açıya ulaşılmazsa) her tekrar YANLIŞ sayılmalı. Böylece hem kurallar
 * hem animasyonlar birbiriyle tutarlı mı, her hareket için tek tek kontrol edilir.
 */
class DemoConsistencyTest {

    private val mapping = listOf(
        Lm.NOSE to DJ.HEAD,
        Lm.LEFT_SHOULDER to DJ.SH_L, Lm.RIGHT_SHOULDER to DJ.SH_R,
        Lm.LEFT_ELBOW to DJ.EL_L, Lm.RIGHT_ELBOW to DJ.EL_R,
        Lm.LEFT_WRIST to DJ.WR_L, Lm.RIGHT_WRIST to DJ.WR_R,
        Lm.LEFT_HIP to DJ.HIP_L, Lm.RIGHT_HIP to DJ.HIP_R,
        Lm.LEFT_KNEE to DJ.KN_L, Lm.RIGHT_KNEE to DJ.KN_R,
        Lm.LEFT_ANKLE to DJ.AN_L, Lm.RIGHT_ANKLE to DJ.AN_R,
        Lm.LEFT_HEEL to DJ.AN_L, Lm.RIGHT_HEEL to DJ.AN_R,
        Lm.LEFT_FOOT to DJ.TOE_L, Lm.RIGHT_FOOT to DJ.TOE_R,
    )

    /** Animasyondaki çubuk adamı MediaPipe noktalarına çevirir. */
    private fun frame(ex: Exercise, progress: Float, ts: Long): PoseFrame {
        val joints = ex.demo.jointsAt(progress)
        val pts = MutableList(33) { Landmark(0.5f, 0.5f, 0f, 0f) }
        for ((lm, j) in mapping) {
            val v = joints.getValue(j)
            pts[lm] = Landmark((v.x + 200f) / 400f, (v.y + 250f) / 400f, 0f, 0.99f)
        }
        return PoseFrame(pts, 1000, 1000, ts)
    }

    /** Animasyonu [maxProgress] noktasına kadar [reps] kez oynatır. */
    private fun play(ex: Exercise, maxProgress: Float, reps: Int = 3, repMs: Long = 2400): RepCounter {
        val counter = RepCounter(ex.rule)
        var ts = 0L
        fun feed(p: Float) { counter.process(frame(ex, p, ts), ts); ts += 33 }
        repeat(20) { feed(0f) }
        repeat(reps) {
            val n = (repMs / 33).toInt()
            for (i in 0..n) {
                val t = i.toDouble() / n
                feed((maxProgress * (0.5 - 0.5 * cos(2 * PI * t))).toFloat())
            }
            repeat(10) { feed(0f) }
        }
        return counter
    }

    /** Animasyonun belli bir anındaki ana açı (sayacın kullandığı şekilde). */
    private fun primaryAngle(ex: Exercise, p: Float): Float {
        val f = frame(ex, p, 0)
        val m = ex.rule.primary
        val sides = listOf(m.left, m.right).filter { f.isVisible(it.a, it.b, it.c) }.map { f.angle(it.a, it.b, it.c) }
        return when (m.sideMode) {
            SideMode.MIN -> sides.min()
            SideMode.MAX -> sides.max()
            SideMode.AVERAGE -> sides.average().toFloat()
        }
    }

    @Test
    fun correctDemoIsCountedAsCorrectForEveryExercise() {
        val problems = mutableListOf<String>()
        for (ex in EXERCISES) {
            val c = play(ex, 1f)
            if (c.correctCount != 3 || c.incorrectCount != 0) {
                problems += "${ex.id}: doğru=${c.correctCount} yanlış=${c.incorrectCount} " +
                    "hatalar=${c.reps.flatMap { it.faults }.distinct()} " +
                    "açı ${primaryAngle(ex, 0f)}→${primaryAngle(ex, 1f)}"
            }
        }
        if (problems.isNotEmpty()) fail(problems.joinToString("\n"))
    }

    @Test
    fun halfRepIsCountedAsIncorrectForEveryExercise() {
        val problems = mutableListOf<String>()
        for (ex in EXERCISES) {
            val m = ex.rule.primary
            // Başlangıç eşiğini geçen ama hedefe ulaşmayan en derin noktayı bul
            val mid = (m.enterAt + m.targetAt) / 2
            val p = (0..100).map { it / 100f }.firstOrNull { p ->
                val a = primaryAngle(ex, p)
                if (m.direction == Direction.DECREASING) a <= mid else a >= mid
            }
            if (p == null) { problems += "${ex.id}: animasyon hedef açıya hiç ulaşmıyor"; continue }
            val c = play(ex, p)
            val depthOk = c.reps.all { ex.rule.depthMessage in it.faults }
            if (c.incorrectCount != 3 || c.correctCount != 0 || !depthOk) {
                problems += "${ex.id}: yarım tekrarda doğru=${c.correctCount} yanlış=${c.incorrectCount}"
            }
        }
        if (problems.isNotEmpty()) fail(problems.joinToString("\n"))
    }

    @Test
    fun standingStillCountsNothing() {
        for (ex in EXERCISES) {
            val c = play(ex, 0f)
            assertEquals(0, c.correctCount + c.incorrectCount, ex.id)
        }
    }

    @Test
    fun gymExercisesAreInTheCatalog() {
        assertTrue(EXERCISES.count { it.gear.isGym } >= 8)
        assertEquals(EXERCISES.size, EXERCISES.map { it.id }.distinct().size, "hareket kimlikleri tekil olmalı")
    }

    @Test
    fun recommendationsNeverUseGymMachines() {
        for (eq in Equipment.entries) for (goal in Goal.entries) for (days in 2..5) for (focus in Category.entries) {
            val r = recommend(RecommendationRequest(goal, Level.INTERMEDIATE, days, eq, 60, setOf(focus)))
            val gym = r.plans.flatMap { it.items }.mapNotNull { exerciseById(it.exerciseId) }.filter { it.gear.isGym }
            assertTrue(gym.isEmpty(), "$eq $goal $days gün $focus: ${gym.map { it.id }}")
        }
    }
}
