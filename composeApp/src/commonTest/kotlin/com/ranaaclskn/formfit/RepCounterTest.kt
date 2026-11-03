package com.ranaaclskn.formfit

import com.ranaaclskn.formfit.analysis.Direction
import com.ranaaclskn.formfit.analysis.RepCounter
import com.ranaaclskn.formfit.model.EXERCISES
import com.ranaaclskn.formfit.model.Landmark
import com.ranaaclskn.formfit.model.Lm
import com.ranaaclskn.formfit.model.PoseFrame
import com.ranaaclskn.formfit.model.exerciseById
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Yandan görülen, diz açısı ve gövde eğimi verilen yapay bir insan pozu. */
private fun squatPose(kneeAngle: Double, leanDeg: Double = 15.0, ts: Long): PoseFrame {
    val pts = MutableList(33) { Landmark(0.5f, 0.5f, 0f, 0f) }
    fun set(i: Int, x: Double, y: Double) { pts[i] = Landmark(x.toFloat(), y.toFloat(), 0f, 0.99f) }
    val ankle = 0.5 to 0.9
    val knee = 0.5 to 0.7
    val dir = (90.0 - kneeAngle) * PI / 180
    val hip = knee.first + 0.2 * cos(dir) to knee.second + 0.2 * sin(dir)
    val lean = leanDeg * PI / 180
    val shoulder = hip.first + 0.3 * sin(lean) to hip.second - 0.3 * cos(lean)
    for ((l, r) in listOf(Lm.LEFT_ANKLE to Lm.RIGHT_ANKLE)) { set(l, ankle.first, ankle.second); set(r, ankle.first, ankle.second) }
    set(Lm.LEFT_KNEE, knee.first, knee.second); set(Lm.RIGHT_KNEE, knee.first, knee.second)
    set(Lm.LEFT_HIP, hip.first, hip.second); set(Lm.RIGHT_HIP, hip.first, hip.second)
    set(Lm.LEFT_SHOULDER, shoulder.first, shoulder.second); set(Lm.RIGHT_SHOULDER, shoulder.first, shoulder.second)
    set(Lm.LEFT_ELBOW, shoulder.first, shoulder.second + 0.15); set(Lm.RIGHT_ELBOW, shoulder.first, shoulder.second + 0.15)
    set(Lm.LEFT_WRIST, shoulder.first, shoulder.second + 0.3); set(Lm.RIGHT_WRIST, shoulder.first, shoulder.second + 0.3)
    return PoseFrame(pts, 1000, 1000, ts)
}

private class Sim(exerciseId: String = "squat") {
    val counter = RepCounter(exerciseById(exerciseId)!!.rule)
    var ts = 0L

    fun hold(angle: Double, frames: Int = 15, lean: Double = 15.0) = repeat(frames) { feed(angle, lean) }

    /** Dinlenmeden [bottom] açısına inip geri dönen tek tekrar. */
    fun rep(bottom: Double, durationMs: Long = 1800, top: Double = 172.0, lean: (Double) -> Double = { 15.0 }) {
        val frames = (durationMs / FRAME_MS).toInt()
        for (i in 0..frames) {
            val t = i.toDouble() / frames
            val a = bottom + (top - bottom) * (0.5 + 0.5 * cos(2 * PI * t))
            feed(a, lean(t))
        }
    }

    fun feed(angle: Double, lean: Double) {
        counter.process(squatPose(angle, lean, ts), ts)
        ts += FRAME_MS
    }

    fun lost(frames: Int) = repeat(frames) { counter.process(null, ts); ts += FRAME_MS }

    companion object { const val FRAME_MS = 33L }
}

class RepCounterTest {
    @Test
    fun fullRepsAreCountedAsCorrect() {
        val s = Sim()
        s.hold(172.0)
        repeat(3) { s.rep(85.0); s.hold(172.0, 5) }
        assertEquals(3, s.counter.correctCount)
        assertEquals(0, s.counter.incorrectCount)
    }

    @Test
    fun shallowRepIsIncorrect() {
        val s = Sim()
        s.hold(172.0)
        s.rep(125.0)
        assertEquals(0, s.counter.correctCount)
        assertEquals(1, s.counter.incorrectCount)
        assertTrue(s.counter.reps.single().faults.any { it.contains("inmedin") })
    }

    @Test
    fun smallWobbleIsNotARep() {
        val s = Sim()
        s.hold(172.0)
        repeat(20) { s.feed(if (it % 2 == 0) 168.0 else 158.0, 15.0) }
        assertEquals(0, s.reps())
    }

    @Test
    fun tooFastRepIsIncorrect() {
        val s = Sim()
        s.hold(172.0)
        s.rep(85.0, durationMs = 500)
        s.hold(172.0, 5)
        assertEquals(1, s.counter.incorrectCount)
        assertTrue(s.counter.reps.single().faults.any { it.contains("hızlı") })
    }

    @Test
    fun leaningForwardIsIncorrect() {
        val s = Sim()
        s.hold(172.0)
        s.rep(85.0) { t -> if (t in 0.2..0.8) 70.0 else 15.0 }
        assertEquals(1, s.counter.incorrectCount)
        assertTrue(s.counter.reps.single().faults.any { it.contains("eğiliyorsun") })
    }

    @Test
    fun repIsNotCountedUntilStartingPositionIsSeen() {
        val s = Sim()
        // Kişi kameraya squat'ın altındayken giriyor: önce ayağa kalkmadan sayılmamalı
        s.hold(90.0)
        s.hold(172.0)
        assertEquals(0, s.reps())
        s.rep(85.0)
        assertEquals(1, s.counter.correctCount)
    }

    @Test
    fun losingThePersonCancelsTheRep() {
        val s = Sim()
        s.hold(172.0)
        repeat(20) { s.feed(172.0 - it * 4, 15.0) } // aşağı iniyor
        s.lost(60) // 2 saniye görünmüyor
        s.hold(172.0)
        assertEquals(0, s.reps())
    }

    @Test
    fun pushupIsNotCountedWhileStanding() {
        val s = Sim("pushup")
        // Ayakta duran biri kollarını büküp açıyor: şınav kapısı bunu saymamalı
        repeat(60) { s.feed(172.0, 5.0) }
        assertEquals(0, s.reps())
    }

    @Test
    fun everyExerciseHasConsistentThresholds() {
        for (ex in EXERCISES) {
            val m = ex.rule.primary
            if (m.direction == Direction.DECREASING) {
                assertTrue(m.targetAt < m.enterAt && m.enterAt < m.exitAt, ex.id)
            } else {
                assertTrue(m.targetAt > m.enterAt && m.enterAt > m.exitAt, ex.id)
            }
            for (p in listOf(0f, 0.5f, 1f)) {
                assertTrue(ex.demo.jointsAt(p).values.all { it.x.isFinite() && it.y.isFinite() }, ex.id)
            }
        }
    }

    private fun Sim.reps() = counter.correctCount + counter.incorrectCount
}
