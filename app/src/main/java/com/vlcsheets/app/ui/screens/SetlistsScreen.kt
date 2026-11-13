package com.vlcsheets.app.ui.screens

import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vlcsheets.app.MainViewModel
import com.vlcsheets.app.R
import com.vlcsheets.app.ui.components.pressScale
import com.vlcsheets.app.data.LibraryRepository
import com.vlcsheets.app.data.Setlist
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.rounded.Archive
import com.vlcsheets.app.data.Song
import com.vlcsheets.app.data.formatDuration
import com.vlcsheets.app.ui.theme.Brass
import com.vlcsheets.app.ui.components.BrandMark
import com.vlcsheets.app.ui.components.ConfirmDialog
import com.vlcsheets.app.ui.components.EmptyState
import com.vlcsheets.app.ui.components.SongThumbnail
import com.vlcsheets.app.ui.theme.BrandGradient
import com.vlcsheets.app.ui.theme.CoolGradient
import com.vlcsheets.app.ui.theme.WarmGradient

@Composable
fun SetlistsScreen(
    vm: MainViewModel,
    onOpenSetlist: (Setlist) -> Unit,
    onPlaySetlist: (Setlist) -> Unit,
) {
    val library by vm.library.collectAsStateWithLifecycle()
    val songs = library.songs

    var showCreate by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Setlist?>(null) }
    var deleteTarget by remember { mutableStateOf<Setlist?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandMark(modifier = Modifier.size(44.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.setlists_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = pluralStringResource(R.plurals.setlist_count, library.setlists.size, library.setlists.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { showCreate = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.new_setlist))
                }
            }

            if (library.setlists.isEmpty()) {
                EmptyState(
                    icon = {
                        Icon(
                            Icons.Rounded.QueueMusic,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    title = stringResource(R.string.empty_setlists_title),
                    subtitle = stringResource(R.string.empty_setlists_subtitle),
                    modifier = Modifier.weight(1f),
                    action = {
                        Button(onClick = { showCreate = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.new_setlist))
                        }
                    },
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 280.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 108.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(library.setlists, key = { it.id }) { setlist ->
                        SetlistCard(
                            setlist = setlist,
                            modifier = Modifier.animateItem(),
                            songs = songs,
                            onClick = { onOpenSetlist(setlist) },
                            onPlay = { onPlaySetlist(setlist) },
                            onRename = { renameTarget = setlist },
                            onDelete = { deleteTarget = setlist },
                        )
                    }
                }
            }
        }

        // An empty screen has its own big button; the floating one comes in once there is content.
        AnimatedVisibility(
            visible = library.setlists.isNotEmpty(),
            modifier = Modifier.align(Alignment.BottomEnd),
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
        ) {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                modifier = Modifier
                    .pressScale()
                    .padding(20.dp),
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.new_setlist)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }

    if (showCreate) {
        NameDialog(
            title = stringResource(R.string.new_setlist),
            placeholder = stringResource(R.string.setlist_name),
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                vm.createSetlist(name)
                showCreate = false
            },
        )
    }

    renameTarget?.let { setlist ->
        NameDialog(
            title = stringResource(R.string.rename_setlist),
            initial = setlist.name,
            placeholder = stringResource(R.string.setlist_name),
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                vm.renameSetlist(setlist, name)
                renameTarget = null
            },
        )
    }

    deleteTarget?.let { setlist ->
        ConfirmDialog(
            title = stringResource(R.string.delete_setlist_title),
            text = stringResource(R.string.delete_setlist_text, setlist.name),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = { vm.deleteSetlist(setlist) },
            onDismiss = { deleteTarget = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SetlistCard(
    setlist: Setlist,
    modifier: Modifier = Modifier,
    songs: List<Song>,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val palette = remember(setlist.id) {
        listOf(BrandGradient, CoolGradient, WarmGradient)[(setlist.id.hashCode() and 0x7fffffff) % 3]
    }
    val preview = setlist.songIds.take(3).mapNotNull { id -> songs.firstOrNull { it.id == id }?.title }

    Box(modifier) {
        Column(
            modifier = Modifier
                .pressScale()
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(palette)
                .combinedClickable(onClick = onClick, onLongClick = { menu = true })
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.18f),
                    modifier = Modifier.size(42.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.QueueMusic,
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(40.dp),
                    onClick = onPlay,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(R.string.action_play),
                            tint = Color.White,
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { menu = true }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more_options), tint = Color.White)
                }
            }
            Spacer(Modifier.height(26.dp))
            Text(
                text = setlist.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = pluralStringResource(R.plurals.song_count, setlist.songIds.size, setlist.songIds.size),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.82f),
            )
            if (preview.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = preview.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_rename)) },
                onClick = {
                    menu = false
                    onRename()
                },
                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                onClick = {
                    menu = false
                    onDelete()
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                },
            )
        }
    }
}

@Composable
fun NameDialog(
    title: String,
    placeholder: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    initial: String = "",
    allowBlank: Boolean = false,
) {
    var value by remember { mutableStateOf(initial) }
    val ok = allowBlank || value.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (ok) onConfirm(value.trim()) },
                enabled = ok,
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SetlistDetailScreen(
    vm: MainViewModel,
    setlistId: String,
    onBack: () -> Unit,
    onOpenSong: (String, List<String>) -> Unit,
) {
    val library by vm.library.collectAsStateWithLifecycle()
    val setlist = library.setlists.firstOrNull { it.id == setlistId }
    val songs = library.songs
    var addSheet by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf(false) }
    var noteFor by remember { mutableStateOf<String?>(null) }
    val shareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val s = library.setlists.firstOrNull { it.id == setlistId }
        if (uri != null && s != null) vm.exportShare(uri, s.songIds, s)
    }

    LaunchedEffect(setlist, library.loaded) {
        if (setlist == null && library.loaded) onBack()
    }
    val current = setlist ?: return
    val unknownComposer = stringResource(R.string.unknown_composer)

    val setlistSongs = current.songIds.mapNotNull { id -> songs.firstOrNull { it.id == id } }
    val totalPages = setlistSongs.sumOf { it.pageCount }
    val totalSeconds = setlistSongs.sumOf { it.durationSec }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        setlistSongs.firstOrNull()?.let { onOpenSong(it.id, current.songIds) }
                    }) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.action_play))
                    }
                    IconButton(
                        enabled = setlistSongs.isNotEmpty(),
                        onClick = { shareLauncher.launch("${current.name.ifBlank { "setlist" }}.${LibraryRepository.SHARE_EXTENSION}") },
                    ) {
                        Icon(Icons.Rounded.Archive, contentDescription = stringResource(R.string.export_share))
                    }
                    IconButton(onClick = { rename = true }) {
                        Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.action_rename))
                    }
                    IconButton(onClick = { deleteConfirm = true }) {
                        Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_delete))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                    ) {
                        StatColumn(
                            value = setlistSongs.size.toString(),
                            label = stringResource(R.string.songs),
                        )
                        StatColumn(
                            value = totalPages.toString(),
                            label = stringResource(R.string.pages),
                        )
                        StatColumn(
                            value = setlistSongs.count { it.hasAnnotations }.toString(),
                            label = stringResource(R.string.annotated),
                        )
                        if (totalSeconds > 0) {
                            StatColumn(
                                value = formatDuration(totalSeconds),
                                label = stringResource(R.string.duration),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                if (setlistSongs.isEmpty()) {
                    EmptyState(
                        icon = {
                            Icon(
                                Icons.Rounded.PlaylistAdd,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        title = stringResource(R.string.empty_setlist_title),
                        subtitle = stringResource(R.string.empty_setlist_subtitle),
                        modifier = Modifier.weight(1f),
                        action = {
                            Button(onClick = { addSheet = true }) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.add_songs))
                            }
                        },
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 110.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(setlistSongs, key = { it.id }) { song ->
                            val index = current.songIds.indexOf(song.id)
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .animateItem()
                                    .pressScale()
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { onOpenSong(song.id, current.songIds) },
                                        onLongClick = { noteFor = song.id },
                                    ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = (index + 1).toString().padStart(2, '0'),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(26.dp),
                                    )
                                    SongThumbnail(
                                        song = song,
                                        modifier = Modifier
                                            .width(44.dp)
                                            .height(58.dp),
                                        cornerRadius = 10.dp,
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = listOfNotNull(
                                                song.composer.ifBlank { unknownComposer },
                                                song.durationSec.takeIf { it > 0 }?.let(::formatDuration),
                                            ).joinToString("  ·  "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        current.songNotes[song.id]?.let { note ->
                                            Text(
                                                text = note,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.tertiary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                    IconButton(onClick = { noteFor = song.id }) {
                                        Icon(
                                            Icons.Rounded.EditNote,
                                            contentDescription = stringResource(R.string.setlist_note_title),
                                            tint = if (song.id in current.songNotes) Brass else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Column {
                                        IconButton(
                                            onClick = {
                                                vm.moveSetlistSong(current, index, index - 1)
                                            },
                                            enabled = index > 0,
                                            modifier = Modifier.size(30.dp),
                                        ) {
                                            Icon(
                                                Icons.Rounded.ArrowUpward,
                                                contentDescription = stringResource(R.string.action_move_up),
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                vm.moveSetlistSong(current, index, index + 1)
                                            },
                                            enabled = index < current.songIds.lastIndex,
                                            modifier = Modifier.size(30.dp),
                                        ) {
                                            Icon(
                                                Icons.Rounded.ArrowDownward,
                                                contentDescription = stringResource(R.string.action_move_down),
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                    IconButton(onClick = { vm.removeSongFromSetlist(current, song.id) }) {
                                        Icon(
                                            Icons.Rounded.RemoveCircleOutline,
                                            contentDescription = stringResource(R.string.action_remove),
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // An empty screen has its own big button; the floating one comes in once there is content.
            AnimatedVisibility(
                visible = setlistSongs.isNotEmpty(),
                modifier = Modifier.align(Alignment.BottomEnd),
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 0.8f),
            ) {
                ExtendedFloatingActionButton(
                    onClick = { addSheet = true },
                    modifier = Modifier
                        .pressScale()
                        .padding(20.dp),
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.add_songs)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }

    if (addSheet) {
        AddSongsSheet(
            allSongs = songs,
            existing = current.songIds.toSet(),
            onDismiss = { addSheet = false },
            onConfirm = { ids ->
                vm.addSongsToSetlist(current, ids)
                addSheet = false
            },
        )
    }

    noteFor?.let { songId ->
        NameDialog(
            title = stringResource(R.string.setlist_note_title),
            initial = current.songNotes[songId].orEmpty(),
            placeholder = stringResource(R.string.setlist_note_placeholder),
            allowBlank = true,
            onDismiss = { noteFor = null },
            onConfirm = { note ->
                vm.setSetlistNote(current, songId, note)
                noteFor = null
            },
        )
    }

    if (rename) {
        NameDialog(
            title = stringResource(R.string.rename_setlist),
            initial = current.name,
            placeholder = stringResource(R.string.setlist_name),
            onDismiss = { rename = false },
            onConfirm = { name ->
                vm.renameSetlist(current, name)
                rename = false
            },
        )
    }

    if (deleteConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.delete_setlist_title),
            text = stringResource(R.string.delete_setlist_text, current.name),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = {
                vm.deleteSetlist(current)
                onBack()
            },
            onDismiss = { deleteConfirm = false },
        )
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSongsSheet(
    allSongs: List<Song>,
    existing: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var search by remember { mutableStateOf("") }
    val unknownComposer = stringResource(R.string.unknown_composer)
    val selected = remember { mutableStateListOf<String>() }
    val filtered = allSongs.filter {
        it.id !in existing && (search.isBlank() ||
            it.title.contains(search, ignoreCase = true) ||
            it.composer.contains(search, ignoreCase = true))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        ) {
            Text(
                text = stringResource(R.string.add_songs),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            if (filtered.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_songs_available),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(filtered, key = { it.id }) { song ->
                        Row(
                            modifier = Modifier
                                .pressScale()
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .combinedClickable(
                                    onClick = {
                                        if (song.id in selected) selected.remove(song.id)
                                        else selected.add(song.id)
                                    },
                                    onLongClick = {},
                                )
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = song.id in selected,
                                onCheckedChange = { checked ->
                                    if (checked) selected.add(song.id) else selected.remove(song.id)
                                },
                            )
                            SongThumbnail(
                                song = song,
                                modifier = Modifier
                                    .width(38.dp)
                                    .height(50.dp),
                                cornerRadius = 8.dp,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    song.composer.ifBlank { unknownComposer },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { onConfirm(selected.toList()) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_songs_count, selected.size))
            }
        }
    }
}
