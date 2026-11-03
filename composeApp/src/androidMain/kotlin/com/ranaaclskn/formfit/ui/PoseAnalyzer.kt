package com.ranaaclskn.formfit.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.ranaaclskn.formfit.model.Landmark
import com.ranaaclskn.formfit.model.PoseFrame

/**
 * CameraX karelerini MediaPipe Pose Landmarker'a verir. Kare önce dik konuma döndürülür,
 * ön kamerada ayrıca aynalanır; böylece bulunan noktalar ekrandaki önizlemeyle birebir örtüşür.
 * [onResult] analiz iş parçacığında çağrılır.
 */
class PoseAnalyzer(context: Context, private val onResult: (PoseFrame?) -> Unit) {

    private var landmarker: PoseLandmarker? = create(context, Delegate.GPU) ?: create(context, Delegate.CPU)
    private var lastTimestamp = 0L

    private fun create(context: Context, delegate: Delegate): PoseLandmarker? = try {
        val base = BaseOptions.builder()
            .setModelAssetPath(MODEL)
            .setDelegate(delegate)
            .build()
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(base)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumPoses(1)
            .setMinPoseDetectionConfidence(0.5f)
            .setMinPosePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setResultListener(this::handleResult)
            .setErrorListener { e -> Log.e(TAG, "MediaPipe hatası", e) }
            .build()
        PoseLandmarker.createFromOptions(context, options)
    } catch (e: Exception) {
        Log.w(TAG, "$delegate ile başlatılamadı", e)
        null
    }

    /** Analiz iş parçacığında çağrılır; [proxy] her durumda kapatılır. */
    fun analyze(proxy: ImageProxy, mirror: Boolean) {
        val lm = landmarker
        if (lm == null) {
            proxy.close()
            return
        }
        val upright: Bitmap
        try {
            val bitmap = proxy.toBitmap()
            val matrix = Matrix().apply {
                postRotate(proxy.imageInfo.rotationDegrees.toFloat())
                if (mirror) postScale(-1f, 1f)
            }
            upright = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } finally {
            proxy.close()
        }
        // MediaPipe zaman damgalarının kesin artan olmasını ister
        val ts = maxOf(SystemClock.uptimeMillis(), lastTimestamp + 1)
        lastTimestamp = ts
        lm.detectAsync(BitmapImageBuilder(upright).build(), ts)
    }

    private fun handleResult(result: PoseLandmarkerResult, input: MPImage) {
        val points = result.landmarks().firstOrNull()
        if (points == null) {
            onResult(null)
            return
        }
        val landmarks = points.map {
            Landmark(it.x(), it.y(), it.z(), it.visibility().orElse(0f))
        }
        onResult(PoseFrame(landmarks, input.width, input.height, result.timestampMs()))
    }

    /** Analiz iş parçacığından çağrılmalı ki çalışan bir tespitin ortasında kapanmasın. */
    fun close() {
        landmarker?.close()
        landmarker = null
    }

    private companion object {
        const val TAG = "PoseAnalyzer"
        const val MODEL = "pose_landmarker_full.task"
    }
}
