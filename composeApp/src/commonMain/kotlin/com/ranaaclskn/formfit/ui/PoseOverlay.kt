package com.ranaaclskn.formfit.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.ranaaclskn.formfit.analysis.MIN_VISIBILITY
import com.ranaaclskn.formfit.model.Lm
import com.ranaaclskn.formfit.model.PoseFrame
import kotlin.math.max

/** Kamera önizlemesinin üzerine iskeleti çizer. Önizleme gibi alanı doldurup ortadan kırpar. */
@Composable
fun PoseOverlay(frame: PoseFrame?, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val f = frame ?: return@Canvas
        if (f.imageWidth <= 0 || f.imageHeight <= 0) return@Canvas
        val scale = max(size.width / f.imageWidth, size.height / f.imageHeight)
        val dx = (size.width - f.imageWidth * scale) / 2f
        val dy = (size.height - f.imageHeight * scale) / 2f
        fun at(i: Int): Offset? {
            val l = f[i]
            if (l.visibility < MIN_VISIBILITY) return null
            return Offset(l.x * f.imageWidth * scale + dx, l.y * f.imageHeight * scale + dy)
        }
        val stroke = 5.dp.toPx()
        for ((a, b) in Lm.CONNECTIONS) {
            val pa = at(a) ?: continue
            val pb = at(b) ?: continue
            drawLine(color.copy(alpha = 0.85f), pa, pb, strokeWidth = stroke, cap = StrokeCap.Round)
        }
        for (i in Lm.BODY_POINTS) {
            val p = at(i) ?: continue
            drawCircle(Color.White, radius = stroke * 0.9f, center = p)
            drawCircle(color, radius = stroke * 0.55f, center = p)
        }
    }
}
