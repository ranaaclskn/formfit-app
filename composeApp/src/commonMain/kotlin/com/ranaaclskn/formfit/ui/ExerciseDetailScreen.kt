package com.ranaaclskn.formfit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranaaclskn.formfit.model.Exercise

/** Hareketin nasıl yapıldığını gösteren sayfa: video, adımlar, dikkat edilmesi gerekenler, püf noktaları. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailScreen(exercise: Exercise, onBack: () -> Unit, onStartWorkout: () -> Unit) {
    var playing by remember { mutableStateOf(true) }
    var speed by remember { mutableFloatStateOf(1f) }
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri", tint = Color.White) }
            Text(exercise.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        ExerciseDemoView(
            exercise.demo,
            playing = playing,
            speed = speed,
            modifier = Modifier.fillMaxWidth().height(300.dp).padding(horizontal = 20.dp, vertical = 8.dp),
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledIconButton(onClick = { playing = !playing }) {
                Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playing) "Durdur" else "Oynat")
            }
            Spacer(Modifier.weight(1f))
            listOf(0.5f to "0.5×", 1f to "1×").forEach { (v, label) ->
                FilterChip(selected = speed == v, onClick = { speed = v }, label = { Text(label) })
            }
        }

        FlowRow(
            Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Chip(exercise.category.title)
            exercise.muscles.forEach { Chip(it, FfColors.Good) }
        }

        SectionCard("Nasıl yapılır?", exercise.steps, FfColors.Accent, numbered = true)
        SectionCard("Dikkat edilmesi gerekenler", exercise.cautions, FfColors.Warn)
        SectionCard("Püf noktaları", exercise.tips, FfColors.Good)
        SectionCard("Kamera yerleşimi", listOf(exercise.cameraAngle.title + ". " + exercise.setup), FfColors.TextMuted)

        Button(
            onClick = onStartWorkout,
            modifier = Modifier.fillMaxWidth().padding(20.dp).height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FfColors.Accent),
        ) {
            Icon(Icons.Filled.Videocam, null)
            Spacer(Modifier.padding(4.dp))
            Text("Bu hareketle antrenmana başla", fontWeight = FontWeight.SemiBold)
        }
    }
}
