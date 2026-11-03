package com.ranaaclskn.formfit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ranaaclskn.formfit.model.Exercise

/** Hareketin nasıl yapıldığını gösteren sayfa: video, adımlar, dikkat edilmesi gerekenler, püf noktaları. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailScreen(exercise: Exercise, onBack: () -> Unit, onStartWorkout: () -> Unit) {
    var playing by remember { mutableStateOf(true) }
    var speed by remember { mutableFloatStateOf(1f) }
    Box(Modifier.fillMaxSize().background(FfColors.Background)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 110.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Geri", onBack)
                Spacer(Modifier.weight(1f))
                Overline(exercise.category.title)
            }

            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(exercise.titleUpper(), style = MaterialTheme.typography.displayMedium, color = FfColors.Text)
            }

            Box(Modifier.fillMaxWidth().height(340.dp).padding(horizontal = 20.dp, vertical = 8.dp)) {
                ExerciseDemoView(exercise.demo, playing = playing, speed = speed, modifier = Modifier.fillMaxSize())
                Row(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(FfColors.Accent)
                            .clickable { playing = !playing },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            if (playing) "Durdur" else "Oynat",
                            tint = FfColors.OnAccent,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    SpeedToggle(speed) { speed = it }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                InfoBox("Hedef", "${exercise.sets}×${exercise.reps}", Modifier.weight(1f))
                InfoBox("Kamera", exercise.cameraAngle.title.substringBefore(" ").trUpper(), Modifier.weight(1f))
                InfoBox("Kas grubu", "${exercise.muscles.size}", Modifier.weight(1f))
            }

            FlowRow(
                Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                exercise.muscles.forEach { Chip(it) }
            }

            SectionCard("Nasıl yapılır", exercise.steps, Bullet.NUMBER)
            SectionCard("Dikkat edilmesi gerekenler", exercise.cautions, Bullet.CAUTION)
            SectionCard("Püf noktaları", exercise.tips, Bullet.TIP)
            SectionCard("Kamera yerleşimi", listOf(exercise.setup), Bullet.DOT)
        }

        // Sabit alt düğme
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, FfColors.Background, FfColors.Background)))
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp),
        ) {
            PrimaryButton("Antrenmana başla", onStartWorkout, Modifier.fillMaxWidth(), icon = Icons.Filled.Videocam)
        }
    }
}

@Composable
private fun SpeedToggle(speed: Float, onChange: (Float) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.6f))
            .border(1.dp, FfColors.Border, RoundedCornerShape(50))
            .padding(4.dp),
    ) {
        listOf(0.5f to "0.5×", 1f to "1×").forEach { (v, label) ->
            val selected = speed == v
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) FfColors.OnAccent else FfColors.TextSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) FfColors.Accent else Color.Transparent)
                    .clickable { onChange(v) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun InfoBox(label: String, value: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(FfColors.Card)
            .border(1.dp, FfColors.Border, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Overline(label)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.headlineMedium, color = FfColors.Text, maxLines = 1)
    }
}
