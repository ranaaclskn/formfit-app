package com.ranaaclskn.formfit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitView
import com.ranaaclskn.formfit.model.Landmark
import com.ranaaclskn.formfit.model.PoseFrame
import platform.UIKit.UIView

/**
 * Kamera ve MediaPipe iOS'ta Swift ile yazıldı (iosApp/iosApp/PoseCamera.swift), çünkü MediaPipe'ın
 * iOS kütüphanesi CocoaPods ile geliyor. Swift bu arayüzü uygular.
 */
interface PoseCameraFactory {
    fun createCameraView(frontCamera: Boolean, sink: PoseSink): UIView
    fun stopCameraView(view: UIView)
}

/** Swift'in bulduğu noktaları Kotlin'e aktardığı köprü. Ana iş parçacığından çağrılmalı. */
class PoseSink(private val onFrame: (PoseFrame?) -> Unit) {
    /** [values]: her nokta için x, y, z, visibility sırasıyla (33 × 4). */
    fun submit(values: FloatArray, imageWidth: Int, imageHeight: Int, timestampMs: Long) {
        val landmarks = List(values.size / 4) { i ->
            Landmark(values[i * 4], values[i * 4 + 1], values[i * 4 + 2], values[i * 4 + 3])
        }
        onFrame(PoseFrame(landmarks, imageWidth, imageHeight, timestampMs))
    }

    fun submitEmpty() = onFrame(null)
}

object IosCamera {
    var factory: PoseCameraFactory? = null
}

@Composable
actual fun CameraPoseView(
    frontCamera: Boolean,
    onPose: (PoseFrame?) -> Unit,
    modifier: Modifier,
) {
    val factory = IosCamera.factory
    if (factory == null) {
        Box(modifier, contentAlignment = Alignment.Center) { Text("Kamera başlatılamadı", color = Color.White) }
        return
    }
    val current by rememberUpdatedState(onPose)
    val sink = remember { PoseSink { current(it) } }
    key(frontCamera) {
        UIKitView(
            factory = { factory.createCameraView(frontCamera, sink) },
            modifier = modifier,
            onRelease = { factory.stopCameraView(it) },
        )
    }
}
