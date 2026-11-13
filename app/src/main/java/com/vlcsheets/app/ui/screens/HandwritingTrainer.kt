package com.vlcsheets.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke as DrawScopeStroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vlcsheets.app.MainViewModel
import com.vlcsheets.app.R
import com.vlcsheets.app.data.HANDWRITING_SYMBOLS
import com.vlcsheets.app.data.InkSample
import com.vlcsheets.app.data.Pt

private const val SAMPLES_WANTED = 10

/** Teaches the recognizer the user's handwriting: each symbol written a few times in a box. */
@Composable
fun HandwritingTrainer(vm: MainViewModel, onDismiss: () -> Unit) {
    val library by vm.library.collectAsStateWithLifecycle()
    val counts = library.ink.groupingBy { it.symbol }.eachCount()
    var symbol by remember { mutableStateOf(HANDWRITING_SYMBOLS.first()) }
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    val live = remember { mutableStateListOf<Offset>() }
    val count = counts[symbol] ?: 0

    // The writing box, sized by the layout: beside the controls in landscape, under them in portrait.
    val pad: @Composable (Modifier) -> Unit = { size ->
        Box(
            modifier = size
                .background(Color.White, MaterialTheme.shapes.medium)
                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center,
        ) {
            if (strokes.isEmpty() && live.isEmpty()) {
                Text(
                    stringResource(R.string.handwriting_write_here, symbol),
                    color = Color.Gray,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Canvas(
                modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                    // Pen or finger: here every touch writes.
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        live.clear()
                        live += down.position
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            live += change.position
                            change.consume()
                            if (!change.pressed) break
                        }
                        strokes += live.toList()
                        live.clear()
                    }
                },
            ) {
                val ink = DrawScopeStroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                (strokes + listOf(live.toList())).filter { it.isNotEmpty() }.forEach { pts ->
                    val path = Path().apply {
                        moveTo(pts[0].x, pts[0].y)
                        pts.drop(1).forEach { lineTo(it.x, it.y) }
                        if (pts.size == 1) lineTo(pts[0].x + 0.1f, pts[0].y)
                    }
                    drawPath(path, Color.Black, style = ink)
                }
            }
        }
    }
    val top: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.handwriting_teach),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_close))
            }
        }
        Text(
            stringResource(R.string.handwriting_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HANDWRITING_SYMBOLS.forEach { s ->
                FilterChip(
                    selected = s == symbol,
                    onClick = { symbol = s; strokes.clear() },
                    // The symbol, then how many samples it has, small and apart so "1" and "0" never read as "10".
                    label = {
                        Text(s, fontWeight = if ((counts[s] ?: 0) >= 3) FontWeight.Bold else FontWeight.Normal)
                        Text(
                            "·${counts[s] ?: 0}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    },
                )
            }
        }
        Text(
            pluralStringResource(R.plurals.handwriting_samples, count, count),
            style = MaterialTheme.typography.labelLarge,
        )
        // What was saved for this symbol, so a wrong sample can be deleted on its own.
        // The row keeps its height when empty, so the box below does not jump after the first save.
        val saved = library.ink.filter { it.symbol == symbol }
        Row(
            modifier = Modifier.height(64.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            saved.forEach { sample -> SampleThumb(sample) { vm.removeInkSample(sample) } }
        }
    }
    val bottom: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { strokes.clear() }, enabled = strokes.isNotEmpty()) {
                Text(stringResource(R.string.action_clear))
            }
            Button(
                onClick = {
                    vm.addInkSample(InkSample(symbol, strokes.map { s -> s.map { Pt(it.x, it.y) } }))
                    strokes.clear()
                    // Enough samples: move on to the next symbol.
                    if (count + 1 >= SAMPLES_WANTED) {
                        HANDWRITING_SYMBOLS.getOrNull(HANDWRITING_SYMBOLS.indexOf(symbol) + 1)?.let { symbol = it }
                    }
                },
                // More than 10 samples adds little and slows the matching down.
                enabled = strokes.isNotEmpty() && count < SAMPLES_WANTED,
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
        if (count > 0) {
            TextButton(onClick = { vm.forgetInkSamples(symbol) }) {
                Text(stringResource(R.string.handwriting_forget, symbol), color = MaterialTheme.colorScheme.error)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            BoxWithConstraints(Modifier.fillMaxSize().padding(20.dp)) {
                val spacing = Arrangement.spacedBy(14.dp)
                val half = maxWidth / 2
                if (maxWidth > maxHeight) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = spacing,
                        ) { top(); bottom() }
                        // Half the screen height: big enough to write, without taking over the screen.
                        pad(Modifier.fillMaxHeight(0.5f).widthIn(max = half).aspectRatio(1f, matchHeightConstraintsFirst = true))
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = spacing) {
                        top()
                        // Takes only the height left, so the buttons below always fit.
                        pad(Modifier.widthIn(max = 370.dp).weight(1f, fill = false).aspectRatio(1f, matchHeightConstraintsFirst = true))
                        bottom()
                    }
                }
            }
        }
    }
}

/** One saved sample, drawn to fit a small box, with a cross to delete it. */
@Composable
private fun SampleThumb(sample: InkSample, onDelete: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(Color.White, MaterialTheme.shapes.small)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small),
    ) {
        Canvas(Modifier.fillMaxSize().padding(10.dp)) {
            val pts = sample.strokes.flatten()
            if (pts.isEmpty()) return@Canvas
            val minX = pts.minOf { it.x }
            val minY = pts.minOf { it.y }
            val span = maxOf(pts.maxOf { it.x } - minX, pts.maxOf { it.y } - minY, 1f)
            val k = size.minDimension / span
            val ink = DrawScopeStroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            sample.strokes.filter { it.isNotEmpty() }.forEach { s ->
                val path = Path().apply {
                    moveTo((s[0].x - minX) * k, (s[0].y - minY) * k)
                    s.drop(1).forEach { lineTo((it.x - minX) * k, (it.y - minY) * k) }
                    if (s.size == 1) lineTo((s[0].x - minX) * k + 0.1f, (s[0].y - minY) * k)
                }
                drawPath(path, Color.Black, style = ink)
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd).offset(12.dp, (-12).dp).size(48.dp)) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.handwriting_delete_sample),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
