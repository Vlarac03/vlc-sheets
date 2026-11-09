package com.vlcsheets.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vlcsheets.app.R
import com.vlcsheets.app.data.Song
import com.vlcsheets.app.data.SongKind
import com.vlcsheets.app.data.formatDuration
import com.vlcsheets.app.data.parseDuration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SongEditorSheet(
    song: Song,
    collections: List<String>,
    onDismiss: () -> Unit,
    onSave: (Song) -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(song.id) { mutableStateOf(song.title) }
    var composer by remember(song.id) { mutableStateOf(song.composer) }
    var genre by remember(song.id) { mutableStateOf(song.genre) }
    var key by remember(song.id) { mutableStateOf(song.key) }
    var tempo by remember(song.id) { mutableStateOf(if (song.tempo > 0) song.tempo.toString() else "") }
    var year by remember(song.id) { mutableStateOf(if (song.year > 0) song.year.toString() else "") }
    var duration by remember(song.id) { mutableStateOf(if (song.durationSec > 0) formatDuration(song.durationSec) else "") }
    val durationSec = parseDuration(duration)
    var tags by remember(song.id) { mutableStateOf(song.tags.joinToString(", ")) }
    var notes by remember(song.id) { mutableStateOf(song.notes) }
    var favorite by remember(song.id) { mutableStateOf(song.favorite) }
    var newCollection by remember(song.id) { mutableStateOf("") }
    val untitledLabel = stringResource(R.string.untitled)
    val selectedCollections = remember(song.id) {
        mutableStateListOf<String>().apply { addAll(song.collections) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.edit_song),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = (if (song.kind == SongKind.PDF) "PDF" else stringResource(R.string.image)) +
                            " · " + pluralStringResource(R.plurals.page_count, song.pageCount, song.pageCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_close))
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.field_title)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = composer,
                onValueChange = { composer = it },
                label = { Text(stringResource(R.string.field_composer)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text(stringResource(R.string.field_genre)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text(stringResource(R.string.field_key)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = tempo,
                    onValueChange = { value -> tempo = value.filter { it.isDigit() }.take(3) },
                    label = { Text(stringResource(R.string.field_tempo)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = year,
                    onValueChange = { value -> year = value.filter { it.isDigit() }.take(4) },
                    label = { Text(stringResource(R.string.field_year)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = duration,
                    onValueChange = { value -> duration = value.filter { it.isDigit() || it == ':' }.take(8) },
                    label = { Text(stringResource(R.string.field_duration)) },
                    singleLine = true,
                    isError = durationSec == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1.3f),
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text(stringResource(R.string.field_tags)) },
                supportingText = { Text(stringResource(R.string.field_tags_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            if (collections.isNotEmpty() || selectedCollections.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.field_collections),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (collections + selectedCollections).distinct().forEach { collection ->
                        FilterChip(
                            selected = collection in selectedCollections,
                            onClick = {
                                if (collection in selectedCollections) {
                                    selectedCollections.remove(collection)
                                } else {
                                    selectedCollections.add(collection)
                                }
                            },
                            label = { Text(collection) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newCollection,
                    onValueChange = { newCollection = it },
                    placeholder = { Text(stringResource(R.string.field_new_collection)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(6.dp))
                IconButton(
                    onClick = {
                        val name = newCollection.trim()
                        if (name.isNotBlank()) {
                            if (name !in selectedCollections) selectedCollections.add(name)
                            newCollection = ""
                        }
                    },
                    enabled = newCollection.isNotBlank(),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.field_new_collection))
                }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.field_notes)) },
                minLines = 2,
                maxLines = 5,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.favorite), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.favorite_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = favorite, onCheckedChange = { favorite = it })
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    onSave(
                        song.copy(
                            title = title.trim().ifBlank { untitledLabel },
                            composer = composer.trim(),
                            genre = genre.trim(),
                            key = key.trim(),
                            tempo = tempo.toIntOrNull() ?: 0,
                            year = year.toIntOrNull() ?: 0,
                            durationSec = durationSec ?: song.durationSec,
                            tags = tags.split(',').map { it.trim() }.filter { it.isNotBlank() }.distinct(),
                            notes = notes.trim(),
                            collections = selectedCollections.toList(),
                            favorite = favorite,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(R.string.action_save))
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(
                    R.string.added_on,
                    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(song.createdAt)),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            TextButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.delete_song), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
