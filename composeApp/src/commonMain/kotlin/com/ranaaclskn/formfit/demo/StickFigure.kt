package com.ranaaclskn.formfit.demo

import com.ranaaclskn.formfit.analysis.Vec
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hareket kütüphanesindeki "nasıl yapılır" animasyonları için basit çubuk adam.
 *
 * Her kemiğin ekrandaki mutlak açısı verilir (derece; 0 = sağ, 90 = aşağı, -90 = yukarı) ve
 * eklem konumları kalçadan başlayarak hesaplanır. Animasyon iki poz arasındaki açıları
 * yumuşakça geçirir; konumları değil açıları geçirdiği için uzuvlar kısalıp uzamaz.
 */
enum class DemoView { SIDE, FRONT }

enum class Joint {
    HIP_C, NECK, HEAD,
    SH_L, SH_R, EL_L, EL_R, WR_L, WR_R,
    HIP_L, HIP_R, KN_L, KN_R, AN_L, AN_R, TOE_L, TOE_R,
}

enum class Bone(val from: Joint, val to: Joint) {
    SPINE(Joint.HIP_C, Joint.NECK),
    HEAD(Joint.NECK, Joint.HEAD),
    SHOULDER_L(Joint.NECK, Joint.SH_L), SHOULDER_R(Joint.NECK, Joint.SH_R),
    UPPER_ARM_L(Joint.SH_L, Joint.EL_L), UPPER_ARM_R(Joint.SH_R, Joint.EL_R),
    FOREARM_L(Joint.EL_L, Joint.WR_L), FOREARM_R(Joint.EL_R, Joint.WR_R),
    HIP_L(Joint.HIP_C, Joint.HIP_L), HIP_R(Joint.HIP_C, Joint.HIP_R),
    THIGH_L(Joint.HIP_L, Joint.KN_L), THIGH_R(Joint.HIP_R, Joint.KN_R),
    SHIN_L(Joint.KN_L, Joint.AN_L), SHIN_R(Joint.KN_R, Joint.AN_R),
    FOOT_L(Joint.AN_L, Joint.TOE_L), FOOT_R(Joint.AN_R, Joint.TOE_R),
}

/** Bir pozdaki tüm kemik açıları. */
class Keyframe(val angles: Map<Bone, Float>)

/**
 * Poz tanımlamak için yardımcı. Sadece "sol" taraf yazılır: yandan görünümde sağ taraf
 * aynısıdır (arkada kalır), önden görünümde ayna simetriğidir. Farklı olması gerekiyorsa
 * (ör. lunge'da arka bacak) ...R alanları doldurulur.
 */
class KeyframeBuilder(private val view: DemoView) {
    var spine = -90f
    var head: Float? = null
    var upperArm = 90f
    var forearm = 90f
    var thigh = 90f
    var shin = 90f
    var foot: Float = if (view == DemoView.SIDE) 0f else 120f
    var upperArmR: Float? = null
    var forearmR: Float? = null
    var thighR: Float? = null
    var shinR: Float? = null
    var footR: Float? = null

    private fun right(left: Float, override: Float?): Float =
        override ?: if (view == DemoView.FRONT) 180f - left else left

    fun build(): Keyframe = Keyframe(
        mapOf(
            Bone.SPINE to spine,
            Bone.HEAD to (head ?: spine),
            Bone.SHOULDER_L to 180f, Bone.SHOULDER_R to 0f,
            Bone.HIP_L to 180f, Bone.HIP_R to 0f,
            Bone.UPPER_ARM_L to upperArm, Bone.UPPER_ARM_R to right(upperArm, upperArmR),
            Bone.FOREARM_L to forearm, Bone.FOREARM_R to right(forearm, forearmR),
            Bone.THIGH_L to thigh, Bone.THIGH_R to right(thigh, thighR),
            Bone.SHIN_L to shin, Bone.SHIN_R to right(shin, shinR),
            Bone.FOOT_L to foot, Bone.FOOT_R to right(foot, footR),
        )
    )
}

fun keyframe(view: DemoView, block: KeyframeBuilder.() -> Unit): Keyframe =
    KeyframeBuilder(view).apply(block).build()

class DemoAnimation(
    val view: DemoView,
    val start: Keyframe,
    val end: Keyframe,
    /** Animasyon boyunca yerinde sabit kalan eklem (ör. squat'ta ayak bileği). */
    val anchor: Joint,
    /** Başlangıçtan bitiş pozuna giderken gösterilen komut, ör. "Aşağı in". */
    val toEndCue: String,
    /** Bitişten başlangıca dönerken gösterilen komut, ör. "Yukarı çık". */
    val toStartCue: String,
    val cycleMs: Int = 3000,
    val holdStart: Float = 0.15f,
    val holdEnd: Float = 0.15f,
    val dumbbells: Boolean = false,
) {
    private fun length(bone: Bone): Float = when (bone) {
        Bone.SPINE -> 52f
        Bone.HEAD -> 13f
        Bone.SHOULDER_L, Bone.SHOULDER_R -> if (view == DemoView.FRONT) 15f else 0f
        Bone.HIP_L, Bone.HIP_R -> if (view == DemoView.FRONT) 9f else 0f
        Bone.UPPER_ARM_L, Bone.UPPER_ARM_R -> 27f
        Bone.FOREARM_L, Bone.FOREARM_R -> 25f
        Bone.THIGH_L, Bone.THIGH_R -> 40f
        Bone.SHIN_L, Bone.SHIN_R -> 39f
        Bone.FOOT_L, Bone.FOOT_R -> if (view == DemoView.FRONT) 6f else 11f
    }

    /** Döngüdeki zamana (0..1) göre bitiş pozuna ne kadar yaklaşıldığı (0..1) ve hareket yönü. */
    fun progressAt(cycle: Float): Pair<Float, Boolean> {
        val c = cycle - cycle.toInt()
        val moveEach = (1f - holdStart - holdEnd) / 2f
        return when {
            c < holdStart -> 0f to true
            c < holdStart + moveEach -> ease((c - holdStart) / moveEach) to true
            c < holdStart + moveEach + holdEnd -> 1f to false
            else -> 1f - ease((c - holdStart - moveEach - holdEnd) / moveEach) to false
        }
    }

    /** Verilen ilerlemedeki eklem konumları; [anchor] eklemi (0,0) noktasındadır. */
    fun jointsAt(progress: Float): Map<Joint, Vec> {
        val pos = HashMap<Joint, Vec>()
        pos[Joint.HIP_C] = Vec(0f, 0f)
        for (bone in Bone.entries) {
            val a0 = start.angles.getValue(bone)
            val a1 = end.angles.getValue(bone)
            // En kısa yoldan dön: 180'den -105'e giden kol aşağıdan değil yukarıdan geçsin
            var diff = (a1 - a0) % 360f
            if (diff > 180f) diff -= 360f
            if (diff < -180f) diff += 360f
            val deg = a0 + diff * progress
            val rad = deg * PI_DEG
            val p = pos.getValue(bone.from)
            val len = length(bone)
            pos[bone.to] = Vec(p.x + cos(rad) * len, p.y + sin(rad) * len)
        }
        val origin = pos.getValue(anchor)
        return pos.mapValues { (_, v) -> v - origin }
    }

    private fun ease(t: Float): Float = t * t * (3f - 2f * t)

    private companion object {
        const val PI_DEG = 3.1415927f / 180f
    }
}
