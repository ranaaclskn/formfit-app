package com.ranaaclskn.formfit

import com.ranaaclskn.formfit.analysis.checkPlacement
import com.ranaaclskn.formfit.model.CameraAngle
import com.ranaaclskn.formfit.model.Landmark
import com.ranaaclskn.formfit.model.Lm
import com.ranaaclskn.formfit.model.PoseFrame
import com.ranaaclskn.formfit.model.exerciseById
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.ranaaclskn.formfit.demo.Joint as DJ

class PlacementTest {
    /** Hareketin animasyonundaki duruşu, kadrajın [scale] kadarını kaplayacak şekilde kareye çevirir. */
    private fun frame(id: String, scale: Float = 1f, shiftY: Float = 0f): PoseFrame {
        val j = exerciseById(id)!!.demo.jointsAt(0f)
        val map = mapOf(
            Lm.NOSE to DJ.HEAD, Lm.LEFT_SHOULDER to DJ.SH_L, Lm.RIGHT_SHOULDER to DJ.SH_R,
            Lm.LEFT_ELBOW to DJ.EL_L, Lm.RIGHT_ELBOW to DJ.EL_R, Lm.LEFT_WRIST to DJ.WR_L, Lm.RIGHT_WRIST to DJ.WR_R,
            Lm.LEFT_HIP to DJ.HIP_L, Lm.RIGHT_HIP to DJ.HIP_R, Lm.LEFT_KNEE to DJ.KN_L, Lm.RIGHT_KNEE to DJ.KN_R,
            Lm.LEFT_ANKLE to DJ.AN_L, Lm.RIGHT_ANKLE to DJ.AN_R,
        )
        val pts = MutableList(33) { Landmark(0.5f, 0.5f, 0f, 0f) }
        // En alttaki nokta (ayaklar) kadrajın %90'ına gelsin
        val bottom = j.values.maxOf { it.y }
        for ((lm, dj) in map) {
            val v = j.getValue(dj)
            pts[lm] = Landmark(0.5f + v.x * scale / 250f, 0.9f + shiftY + (v.y - bottom) * scale / 250f, 0f, 0.99f)
        }
        return PoseFrame(pts, 1000, 1000, 0)
    }

    @Test
    fun sideExerciseFilmedFromTheSideIsOk() {
        val p = checkPlacement(frame("squat"), CameraAngle.SIDE)
        assertTrue(p.ok, p.message)
    }

    @Test
    fun wrongAngleIsDetected() {
        assertEquals("Kameraya önden dön", checkPlacement(frame("squat"), CameraAngle.FRONT).message)
        assertEquals("Kameraya yan dön", checkPlacement(frame("shoulder-press"), CameraAngle.SIDE).message)
        assertTrue(checkPlacement(frame("shoulder-press"), CameraAngle.FRONT).ok)
    }

    @Test
    fun cutOffBodyAndNoPersonAreReported() {
        // Ayaklar kadrajın altından taşıyor
        val cut = checkPlacement(frame("squat", shiftY = 0.3f), CameraAngle.SIDE)
        assertFalse(cut.bodyVisible)
        assertEquals("Tüm vücudun kadrajda olsun, biraz uzaklaş", cut.message)
        assertFalse(checkPlacement(null, CameraAngle.SIDE).ok)
    }
}
