package com.ranaaclskn.formfit.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranaaclskn.formfit.analysis.Vec
import com.ranaaclskn.formfit.demo.DemoAnimation
import com.ranaaclskn.formfit.demo.DemoView
import com.ranaaclskn.formfit.demo.Joint
import kotlin.math.min

/**
 * Hareketin nasıl yapıldığını gösteren animasyonlu video. [playing] false iken durur,
 * [speed] 0.5 = yarı hız.
 */
@Composable
fun ExerciseDemoView(
    demo: DemoAnimation,
    playing: Boolean,
    speed: Float,
    modifier: Modifier = Modifier,
    showCue: Boolean = true,
) {
    var cycle by remember(demo) { mutableFloatStateOf(0f) }
    LaunchedEffect(demo, playing, speed) {
        if (!playing) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            cycle += (now - last) / 1_000_000f / demo.cycleMs * speed
            if (cycle > 1000f) cycle -= 1000f
            last = now
        }
    }
    val (progress, towardEnd) = demo.progressAt(cycle)

    // Kadrajı tüm döngüye göre bir kez hesapla ki figür zıplamasın
    val bounds = remember(demo) {
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        for (i in 0..20) {
            for (v in demo.jointsAt(i / 20f).values) {
                minX = min(minX, v.x); minY = min(minY, v.y)
                maxX = maxOf(maxX, v.x); maxY = maxOf(maxY, v.y)
            }
        }
        floatArrayOf(minX - 14f, minY - 14f, maxX + 14f, maxY + 6f)
    }

    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF101820)),
    ) {
        Canvas(Modifier.fillMaxSize().padding(if (showCue) 16.dp else 6.dp)) {
            val (bx0, by0, bx1, by1) = bounds.toList()
            val scale = min(size.width / (bx1 - bx0), (size.height - (if (showCue) 28.dp.toPx() else 0f)) / (by1 - by0))
            val ox = (size.width - (bx1 - bx0) * scale) / 2f - bx0 * scale
            val oy = size.height - 4.dp.toPx() - by1 * scale
            fun p(v: Vec) = Offset(v.x * scale + ox, v.y * scale + oy)

            // Zemin
            drawLine(
                Color.White.copy(alpha = 0.18f),
                Offset(0f, (by1 - 6f) * scale + oy),
                Offset(size.width, (by1 - 6f) * scale + oy),
                strokeWidth = 2.dp.toPx(),
            )

            val j = demo.jointsAt(progress)
            val near = FfColors.Accent
            val far = FfColors.Accent.copy(alpha = if (demo.view == DemoView.SIDE) 0.35f else 1f)
            // Çizgi kalınlığı figürün boyuna göre; küçük önizlemede de orantılı görünsün
            val w = (4.5f * scale).coerceIn(1.5.dp.toPx(), 7.dp.toPx())
            fun bone(a: Joint, b: Joint, c: Color) =
                drawLine(c, p(j.getValue(a)), p(j.getValue(b)), strokeWidth = w, cap = StrokeCap.Round)

            // Arkada kalan taraf önce çizilir
            bone(Joint.HIP_C, Joint.HIP_R, far); bone(Joint.HIP_R, Joint.KN_R, far)
            bone(Joint.KN_R, Joint.AN_R, far); bone(Joint.AN_R, Joint.TOE_R, far)
            bone(Joint.NECK, Joint.SH_R, far); bone(Joint.SH_R, Joint.EL_R, far); bone(Joint.EL_R, Joint.WR_R, far)
            if (demo.dumbbells) drawDumbbell(p(j.getValue(Joint.WR_R)), far, w)

            bone(Joint.HIP_C, Joint.NECK, near)
            bone(Joint.HIP_C, Joint.HIP_L, near); bone(Joint.HIP_L, Joint.KN_L, near)
            bone(Joint.KN_L, Joint.AN_L, near); bone(Joint.AN_L, Joint.TOE_L, near)
            bone(Joint.NECK, Joint.SH_L, near); bone(Joint.SH_L, Joint.EL_L, near); bone(Joint.EL_L, Joint.WR_L, near)
            if (demo.dumbbells) drawDumbbell(p(j.getValue(Joint.WR_L)), near, w)

            drawCircle(near, radius = 9f * scale, center = p(j.getValue(Joint.HEAD)))
            for (k in listOf(Joint.KN_L, Joint.EL_L, Joint.HIP_C)) {
                drawCircle(Color.White, radius = w * 0.45f, center = p(j.getValue(k)))
            }
        }
        if (showCue) Text(
            if (towardEnd) demo.toEndCue else demo.toStartCue,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDumbbell(at: Offset, color: Color, w: Float) {
    drawLine(color, at.copy(x = at.x - w * 1.6f), at.copy(x = at.x + w * 1.6f), strokeWidth = w * 0.6f)
    drawCircle(color, radius = w * 0.9f, center = at.copy(x = at.x - w * 1.6f))
    drawCircle(color, radius = w * 0.9f, center = at.copy(x = at.x + w * 1.6f))
}
