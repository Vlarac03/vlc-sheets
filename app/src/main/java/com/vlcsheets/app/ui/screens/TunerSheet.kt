package com.vlcsheets.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vlcsheets.app.MainViewModel
import com.vlcsheets.app.R
import com.vlcsheets.app.media.TunerEngine
import com.vlcsheets.app.media.noteFor
import com.vlcsheets.app.ui.theme.Brass
import com.vlcsheets.app.ui.theme.Sage
import kotlin.math.abs
import kotlin.math.roundToInt

/** Chromatic tuner: listens to the microphone (only while open) and shows the nearest note. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunerSheet(vm: MainViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val engine = remember { TunerEngine() }
    DisposableEffect(granted) {
        if (granted) engine.start()
        onDispose { engine.stop() }
    }
    val hz by engine.hz.collectAsStateWithLifecycle()
    // Keep the last note on screen for a moment between notes instead of flickering.
    var shown by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(hz) { hz?.let { shown = it } }
    val names = stringArrayResource(R.array.note_names)
    val a4 = settings.tunerA4

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.tuner), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(18.dp))
            if (!granted) {
                Text(stringResource(R.string.tuner_permission), textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { askPermission.launch(Manifest.permission.RECORD_AUDIO) }) {
                    Text(stringResource(R.string.tuner_allow))
                }
                return@Column
            }
            val note = shown?.let { noteFor(it, a4.toFloat()) }
            val inTune = note != null && abs(note.cents) < 5f
            // The needle and its colour glide instead of jumping with every reading.
            val color by animateColorAsState(
                when {
                    note == null -> MaterialTheme.colorScheme.onSurfaceVariant
                    inTune -> Sage
                    else -> Brass
                },
                label = "tunerColor",
            )
            val cents by animateFloatAsState(note?.cents ?: 0f, spring(stiffness = Spring.StiffnessMediumLow), label = "needle")
            Text(
                note?.let { "${names[it.index]}${it.octave}" } ?: "–",
                style = MaterialTheme.typography.displayLarge,
                color = color,
            )
            Text(
                if (note == null) stringResource(R.string.tuner_listening)
                else "%+d ct · %.1f Hz".format(note.cents.roundToInt(), shown),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            // Needle: the centre line is in tune; each side is half a semitone.
            val track = MaterialTheme.colorScheme.outlineVariant
            Canvas(Modifier.fillMaxWidth().height(40.dp)) {
                val mid = size.width / 2
                drawLine(track, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 3.dp.toPx())
                listOf(-0.5f, 0f, 0.5f).forEach { f ->
                    val x = mid + f * size.width
                    val h = if (f == 0f) size.height else size.height * 0.5f
                    drawLine(track, Offset(x.coerceIn(1f, size.width - 1f), (size.height - h) / 2), Offset(x.coerceIn(1f, size.width - 1f), (size.height + h) / 2), strokeWidth = 2.dp.toPx())
                }
                note?.let {
                    val x = mid + cents / 100f * size.width
                    drawCircle(color, radius = 9.dp.toPx(), center = Offset(x, size.height / 2))
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                FilledIconButton(
                    onClick = { vm.updateSettings { it.copy(tunerA4 = (it.tunerA4 - 1).coerceAtLeast(415)) } },
                    modifier = Modifier.size(40.dp),
                ) { Icon(Icons.Rounded.Remove, contentDescription = null) }
                Text(
                    stringResource(R.string.tuner_a4, a4),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
                FilledIconButton(
                    onClick = { vm.updateSettings { it.copy(tunerA4 = (it.tunerA4 + 1).coerceAtMost(466)) } },
                    modifier = Modifier.size(40.dp),
                ) { Icon(Icons.Rounded.Add, contentDescription = null) }
            }
        }
    }
}
