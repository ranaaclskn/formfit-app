package com.ranaaclskn.formfit.analysis

import com.ranaaclskn.formfit.model.CameraAngle
import com.ranaaclskn.formfit.model.Lm
import com.ranaaclskn.formfit.model.PoseFrame

/** Kameranın doğru yerleştirilip yerleştirilmediği: tüm vücut kadrajda mı, açı doğru mu. */
data class Placement(
    val bodyVisible: Boolean,
    /** Açı doğru mu; vücut görünmüyorsa ölçülemez (null). */
    val angleOk: Boolean?,
    val message: String,
) {
    val ok: Boolean get() = bodyVisible && angleOk == true
}

private const val EDGE = 0.02f

private fun PoseFrame.inFrame(i: Int): Boolean {
    val l = this[i]
    return l.visibility >= MIN_VISIBILITY && l.x in EDGE..(1 - EDGE) && l.y in EDGE..(1 - EDGE)
}

/**
 * Antrenmana başlamadan önce kamera yerleşimini kontrol eder. Yanlış açıdan ya da vücut
 * kesik çekim, hatalı sayımın en sık sebebidir.
 */
fun checkPlacement(frame: PoseFrame?, angle: CameraAngle): Placement {
    if (frame == null) return Placement(false, null, "Seni göremiyorum, kameranın karşısına geç")
    val left = listOf(Lm.LEFT_SHOULDER, Lm.LEFT_HIP, Lm.LEFT_KNEE, Lm.LEFT_ANKLE).all { frame.inFrame(it) }
    val right = listOf(Lm.RIGHT_SHOULDER, Lm.RIGHT_HIP, Lm.RIGHT_KNEE, Lm.RIGHT_ANKLE).all { frame.inFrame(it) }
    // Yandan çekimde arkada kalan taraf görünmeyebilir; önden çekimde iki taraf da görünmeli
    val body = if (angle == CameraAngle.FRONT) left && right else left || right
    if (!body) return Placement(false, null, "Tüm vücudun kadrajda olsun, biraz uzaklaş")
    val front = frame.isFrontView()
    val angleOk = if (angle == CameraAngle.FRONT) front else !front
    if (!angleOk) {
        return Placement(true, false, if (angle == CameraAngle.FRONT) "Kameraya önden dön" else "Kameraya yan dön")
    }
    return Placement(true, true, "Harika, kadraj ve açı doğru")
}
