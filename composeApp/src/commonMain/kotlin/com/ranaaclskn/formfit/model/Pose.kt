package com.ranaaclskn.formfit.model

/** MediaPipe Pose'un bulduğu tek bir vücut noktası. x ve y 0..1 aralığında, görüntüye göre. */
data class Landmark(
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val visibility: Float = 1f,
)

/**
 * Tek bir kamera karesindeki 33 vücut noktası. Koordinatlar ekranda görünen görüntüye göredir
 * (ön kamerada aynalanmış hali), böylece iskelet doğrudan önizlemenin üzerine çizilebilir.
 */
class PoseFrame(
    val landmarks: List<Landmark>,
    val imageWidth: Int,
    val imageHeight: Int,
    val timestampMs: Long,
) {
    operator fun get(index: Int): Landmark = landmarks[index]
}

/** MediaPipe Pose nokta numaraları. */
object Lm {
    const val NOSE = 0
    const val LEFT_EAR = 7
    const val RIGHT_EAR = 8
    const val LEFT_SHOULDER = 11
    const val RIGHT_SHOULDER = 12
    const val LEFT_ELBOW = 13
    const val RIGHT_ELBOW = 14
    const val LEFT_WRIST = 15
    const val RIGHT_WRIST = 16
    const val LEFT_HIP = 23
    const val RIGHT_HIP = 24
    const val LEFT_KNEE = 25
    const val RIGHT_KNEE = 26
    const val LEFT_ANKLE = 27
    const val RIGHT_ANKLE = 28
    const val LEFT_HEEL = 29
    const val RIGHT_HEEL = 30
    const val LEFT_FOOT = 31
    const val RIGHT_FOOT = 32

    /** İskelet çizimi için birbirine bağlı nokta çiftleri. */
    val CONNECTIONS = listOf(
        LEFT_SHOULDER to RIGHT_SHOULDER,
        LEFT_SHOULDER to LEFT_ELBOW, LEFT_ELBOW to LEFT_WRIST,
        RIGHT_SHOULDER to RIGHT_ELBOW, RIGHT_ELBOW to RIGHT_WRIST,
        LEFT_SHOULDER to LEFT_HIP, RIGHT_SHOULDER to RIGHT_HIP,
        LEFT_HIP to RIGHT_HIP,
        LEFT_HIP to LEFT_KNEE, LEFT_KNEE to LEFT_ANKLE, LEFT_ANKLE to LEFT_HEEL, LEFT_HEEL to LEFT_FOOT, LEFT_ANKLE to LEFT_FOOT,
        RIGHT_HIP to RIGHT_KNEE, RIGHT_KNEE to RIGHT_ANKLE, RIGHT_ANKLE to RIGHT_HEEL, RIGHT_HEEL to RIGHT_FOOT, RIGHT_ANKLE to RIGHT_FOOT,
    )

    val BODY_POINTS = listOf(
        NOSE, LEFT_SHOULDER, RIGHT_SHOULDER, LEFT_ELBOW, RIGHT_ELBOW, LEFT_WRIST, RIGHT_WRIST,
        LEFT_HIP, RIGHT_HIP, LEFT_KNEE, RIGHT_KNEE, LEFT_ANKLE, RIGHT_ANKLE, LEFT_FOOT, RIGHT_FOOT,
    )
}
