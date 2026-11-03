package com.ranaaclskn.formfit.analysis

import com.ranaaclskn.formfit.model.PoseFrame

/** Ana açı hareket sırasında küçülüyor mu (squat'ta diz) yoksa büyüyor mu (yana açışta omuz)? */
enum class Direction { DECREASING, INCREASING }

/** İki taraf da görünüyorsa hangi açı kullanılsın? */
enum class SideMode { MIN, MAX, AVERAGE }

/** b noktasındaki a-b-c açısı. */
data class Joint(val a: Int, val b: Int, val c: Int)

/**
 * Tekrarı sayan ana açı ve eşikleri.
 *
 * Dinlenme konumundan [enterAt] eşiğini geçince tekrar başlar, [targetAt] eşiğine ulaşılırsa
 * hareket tam yapılmış sayılır, [exitAt] eşiğine geri dönülünce tekrar biter. enterAt ile exitAt
 * arasındaki boşluk, açıdaki küçük titremelerin çift sayılmasını engeller.
 */
data class PrimaryMotion(
    val left: Joint,
    val right: Joint,
    val direction: Direction,
    val sideMode: SideMode,
    val enterAt: Float,
    val targetAt: Float,
    val exitAt: Float,
)

/** Form kontrolünün tekrarın hangi bölümünde yapılacağı. */
enum class CheckPhase {
    /** Tekrar boyunca her karede. */
    ACTIVE,
    /** Sadece hedef açıya ulaşılmış karelerde (ör. squat'ın en alt noktası). */
    PEAK,
}

/** Hareketin başındaki değerler; sallanma gibi değişimleri ölçmek için. */
class RepContext(val startFrame: PoseFrame) {
    val startTorsoLean: Float = startFrame.torsoLean()
}

/**
 * Tek bir form kuralı. [evaluate] true = doğru, false = hatalı, null = bu karede ölçülemiyor.
 * Tekrarın ölçülebilen karelerinin en az [minFailRatio] kadarında hatalıysa tekrar yanlış sayılır.
 */
class FormCheck(
    val message: String,
    val phase: CheckPhase = CheckPhase.ACTIVE,
    val minFailRatio: Float = 0.35f,
    val evaluate: (PoseFrame, RepContext) -> Boolean?,
)

class ExerciseRule(
    val primary: PrimaryMotion,
    /** Hedef açıya ulaşılmazsa gösterilecek hata. */
    val depthMessage: String,
    val checks: List<FormCheck> = emptyList(),
    /** Doğru başlangıç duruşunda değilse (ör. şınavda ayakta duruyorsa) kareyi yok say. */
    val gate: ((PoseFrame) -> Boolean)? = null,
    val gateMessage: String = "Başlangıç pozisyonuna geç",
    val minRepMs: Long = 700,
)
