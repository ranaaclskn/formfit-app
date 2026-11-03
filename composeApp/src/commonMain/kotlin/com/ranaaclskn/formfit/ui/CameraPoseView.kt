package com.ranaaclskn.formfit.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ranaaclskn.formfit.model.PoseFrame

/**
 * Kamera önizlemesi + MediaPipe Pose. Her işlenen karede [onPose] ana iş parçacığında çağrılır;
 * kişi bulunamazsa null gelir. Önizleme alanı doldurur (kenarlar kırpılır), nokta koordinatları
 * ekranda görünen (ön kamerada aynalanmış) görüntüye göredir.
 */
@Composable
expect fun CameraPoseView(
    frontCamera: Boolean,
    onPose: (PoseFrame?) -> Unit,
    modifier: Modifier = Modifier,
)
