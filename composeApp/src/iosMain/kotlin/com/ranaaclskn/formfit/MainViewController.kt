package com.ranaaclskn.formfit

import androidx.compose.ui.window.ComposeUIViewController
import com.ranaaclskn.formfit.ui.App
import com.ranaaclskn.formfit.ui.IosCamera
import com.ranaaclskn.formfit.ui.PoseCameraFactory

/** Swift tarafı (iosApp/iosApp/ContentView.swift) burayı çağırır ve kamerayı verir. */
fun MainViewController(cameraFactory: PoseCameraFactory) = ComposeUIViewController {
    IosCamera.factory = cameraFactory
    App()
}
