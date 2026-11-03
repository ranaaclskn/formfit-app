package com.ranaaclskn.formfit.analysis

import com.ranaaclskn.formfit.model.Lm
import com.ranaaclskn.formfit.model.PoseFrame
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sqrt

/** Açılar piksel uzayında hesaplanır; normalize koordinatlar kare olmayan görüntüde açıyı bozardı. */
data class Vec(val x: Float, val y: Float) {
    operator fun minus(o: Vec) = Vec(x - o.x, y - o.y)
    operator fun plus(o: Vec) = Vec(x + o.x, y + o.y)
    operator fun div(f: Float) = Vec(x / f, y / f)
    val length: Float get() = sqrt(x * x + y * y)
}

const val MIN_VISIBILITY = 0.5f

fun PoseFrame.point(index: Int): Vec {
    val l = this[index]
    return Vec(l.x * imageWidth, l.y * imageHeight)
}

fun PoseFrame.mid(a: Int, b: Int): Vec = (point(a) + point(b)) / 2f

fun PoseFrame.isVisible(vararg indices: Int, min: Float = MIN_VISIBILITY): Boolean =
    indices.all { this[it].visibility >= min }

/** b noktasındaki açı (a-b-c), 0..180 derece. */
fun angleAt(a: Vec, b: Vec, c: Vec): Float {
    val r = atan2(c.y - b.y, c.x - b.x) - atan2(a.y - b.y, a.x - b.x)
    var deg = abs(r * 180f / PI_F)
    if (deg > 180f) deg = 360f - deg
    return deg
}

fun PoseFrame.angle(a: Int, b: Int, c: Int): Float = angleAt(point(a), point(b), point(c))

/** a→b doğrusunun dikeyle yaptığı açı: 0 = tam dik, 90 = yatay. */
fun angleFromVertical(a: Vec, b: Vec): Float {
    val d = b - a
    val len = d.length
    if (len == 0f) return 0f
    return acos((abs(d.y) / len).coerceIn(0f, 1f)) * 180f / PI_F
}

fun distance(a: Vec, b: Vec): Float = (a - b).length

/** Gövdenin (omuz ortası - kalça ortası) dikeyden sapması. */
fun PoseFrame.torsoLean(): Float =
    angleFromVertical(mid(Lm.LEFT_HIP, Lm.RIGHT_HIP), mid(Lm.LEFT_SHOULDER, Lm.RIGHT_SHOULDER))

fun PoseFrame.torsoLength(): Float =
    distance(mid(Lm.LEFT_HIP, Lm.RIGHT_HIP), mid(Lm.LEFT_SHOULDER, Lm.RIGHT_SHOULDER))

/** Kişi kameraya önden mi bakıyor? Omuz genişliği gövde boyuna göre geniş görünüyorsa evet. */
fun PoseFrame.isFrontView(): Boolean {
    val torso = torsoLength()
    if (torso == 0f) return false
    return distance(point(Lm.LEFT_SHOULDER), point(Lm.RIGHT_SHOULDER)) / torso > 0.45f
}

/** En iyi görünen taraf: true = sol. */
fun PoseFrame.leftSideMoreVisible(vararg leftRightPairs: Pair<Int, Int>): Boolean {
    val l = leftRightPairs.sumOf { this[it.first].visibility.toDouble() }
    val r = leftRightPairs.sumOf { this[it.second].visibility.toDouble() }
    return l >= r
}

internal const val PI_F = 3.1415927f
