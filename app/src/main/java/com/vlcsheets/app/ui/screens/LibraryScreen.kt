package com.vlcsheets.app.ui.screens

import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vlcsheets.app.MainViewModel
import com.vlcsheets.app.R
import com.vlcsheets.app.ui.components.pressScale
import com.vlcsheets.app.SortOrder
import com.vlcsheets.app.data.Song
import com.vlcsheets.app.ui.components.AnnotationBadge
import com.vlcsheets.app.ui.components.BrandMark
import com.vlcsheets.app.ui.components.ConfirmDialog
import com.vlcsheets.app.ui.components.EmptyState
import com.vlcsheets.app.ui.components.FavoriteBadge
import com.vlcsheets.app.ui.components.MoreButton
import com.vlcsheets.app.ui.components.SongThumbnail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    vm: MainViewModel,
    onOpenSong: (Song) -> Unit,
) {
    val songs by vm.filteredSongs.collectAsStateWithLifecycle()
    val library by vm.library.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val selectedCollection by vm.selectedCollection.collectAsStateWithLifecycle()
    val favoritesOnly by vm.favoritesOnly.collectAsStateWithLifecycle()
    val sortOrder by vm.sortOrder.collectAsStateWithLifecycle()
    val progress by vm.importProgress.collectAsStateWithLifecycle()

    var grid by rememberSaveable { mutableStateOf(true) }
    var sortMenu by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Song?>(null) }
    var deleteTarget by remember { mutableStateOf<Song?>(null) }
    var setlistTarget by remember { mutableStateOf<Song?>(null) }
    var showTuner by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) vm.importUris(uris, selectedCollection)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            LibraryHeader(
                songCount = library.songs.size,
                collectionCount = library.collections.size,
                grid = grid,
                onToggleGrid = { grid = !grid },
                onTuner = { showTuner = true },
                sortOrder = sortOrder,
                sortMenuOpen = sortMenu,
                onSortMenuOpen = { sortMenu = true },
                onSortMenuDismiss = { sortMenu = false },
                onSortSelected = {
                    vm.setSortOrder(it)
                    sortMenu = false
                },
            )

            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_clear))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                ),
            )

            Spacer(Modifier.height(10.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = selectedCollection == null && !favoritesOnly,
                        onClick = {
                            vm.selectCollection(null)
                            vm.setFavoritesOnly(false)
                        },
                        label = { Text(stringResource(R.string.filter_all)) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
                item {
                    FilterChip(
                        selected = favoritesOnly,
                        onClick = { vm.setFavoritesOnly(!favoritesOnly) },
                        label = { Text(stringResource(R.string.filter_favorites)) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiary,
                        ),
                    )
                }
                items(library.collections, key = { it.id }) { collection ->
                    FilterChip(
                        selected = selectedCollection == collection.name,
                        onClick = {
                            vm.setFavoritesOnly(false)
                            vm.selectCollection(
                                if (selectedCollection == collection.name) null else collection.name,
                            )
                        },
                        label = { Text(collection.name) },
                        shape = CircleShape,
                    )
                }
            }

            val currentProgress = progress
            if (currentProgress != null) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(
                        text = stringResource(R.string.importing, currentProgress.fileName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { currentProgress.current.toFloat() / currentProgress.total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            when {
                songs.isEmpty() && library.songs.isEmpty() -> {
                    EmptyState(
                        icon = {
                            Icon(
                                Icons.Rounded.LibraryMusic,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        title = stringResource(R.string.empty_library_title),
                        subtitle = stringResource(R.string.empty_library_subtitle),
                        modifier = Modifier.weight(1f),
                        action = {
                            Button(onClick = {
                                importLauncher.launch(arrayOf("application/pdf", "image/*", "text/*", "application/octet-stream"))
                            }) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.action_import))
                            }
                        },
                    )
                }

                songs.isEmpty() -> {
                    EmptyState(
                        icon = {
                            Icon(
                                Icons.Rounded.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        title = stringResource(R.string.empty_results_title),
                        subtitle = stringResource(R.string.empty_results_subtitle),
                        modifier = Modifier.weight(1f),
                        action = {
                            TextButton(onClick = {
                                vm.setQuery("")
                                vm.selectCollection(null)
                                vm.setFavoritesOnly(false)
                            }) { Text(stringResource(R.string.clear_filters)) }
                        },
                    )
                }

                grid -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 148.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 108.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        items(songs, key = { it.id }) { song ->
                            SongGridCard(
                                song = song,
                                modifier = Modifier.animateItem(),
                                onClick = { onOpenSong(song) },
                                onEdit = { editing = song },
                                onDelete = { deleteTarget = song },
                                onAddToSetlist = { setlistTarget = song },
                                onToggleFavorite = { vm.toggleFavorite(song) },
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 108.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(songs, key = { it.id }) { song ->
                            SongListRow(
                                song = song,
                                modifier = Modifier.animateItem(),
                                onClick = { onOpenSong(song) },
                                onEdit = { editing = song },
                                onDelete = { deleteTarget = song },
                                onAddToSetlist = { setlistTarget = song },
                                onToggleFavorite = { vm.toggleFavorite(song) },
                            )
                        }
                    }
                }
            }
        }

        // An empty screen has its own big button; the floating one comes in once there is content.
        AnimatedVisibility(
            visible = library.songs.isNotEmpty(),
            modifier = Modifier.align(Alignment.BottomEnd),
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
        ) {
            ExtendedFloatingActionButton(
                onClick = { importLauncher.launch(arrayOf("application/pdf", "image/*", "text/*", "application/octet-stream")) },
                modifier = Modifier
                    .pressScale()
                    .padding(20.dp),
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_import)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }

    editing?.let { song ->
        SongEditorSheet(
            song = song,
            collections = library.collections.map { it.name },
            onDismiss = { editing = null },
            onSave = {
                vm.saveSong(it)
                editing = null
            },
            onDelete = {
                vm.deleteSongs(setOf(song.id))
                editing = null
            },
        )
    }

    if (showTuner) TunerSheet(vm) { showTuner = false }

    deleteTarget?.let { song ->
        ConfirmDialog(
            title = stringResource(R.string.delete_song_title),
            text = stringResource(R.string.delete_song_text, song.title),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = { vm.deleteSongs(setOf(song.id)) },
            onDismiss = { deleteTarget = null },
        )
    }

    setlistTarget?.let { song ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { setlistTarget = null }, sheetState = sheetState) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(
                    text = stringResource(R.string.add_to_setlist),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(12.dp))
                if (library.setlists.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_setlists),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    library.setlists.forEach { setlist ->
                        Row(
                            modifier = Modifier
                                .pressScale()
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .combinedClickable(
                                    onClick = {
                                        vm.addSongsToSetlist(setlist, listOf(song.id))
                                        setlistTarget = null
                                    },
                                    onLongClick = {},
                                )
                                .padding(vertical = 14.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.PlaylistAdd, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(setlist.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    pluralStringResource(R.plurals.song_count, setlist.songIds.size, setlist.songIds.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    songCount: Int,
    collectionCount: Int,
    grid: Boolean,
    onToggleGrid: () -> Unit,
    onTuner: () -> Unit,
    sortOrder: SortOrder,
    sortMenuOpen: Boolean,
    onSortMenuOpen: () -> Unit,
    onSortMenuDismiss: () -> Unit,
    onSortSelected: (SortOrder) -> Unit,
) {
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
                text = stringResource(R.string.app_wordmark),
                style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 2.sp),
            )
            Text(
                text = pluralStringResource(R.plurals.score_count, songCount, songCount) + " · " +
                    pluralStringResource(R.plurals.collection_count, collectionCount, collectionCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        IconButton(onClick = onTuner) {
            Icon(Icons.Rounded.GraphicEq, contentDescription = stringResource(R.string.tuner))
        }
        IconButton(onClick = onToggleGrid) {
            Icon(
                imageVector = if (grid) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView,
                contentDescription = stringResource(R.string.action_change_view),
            )
        }

        Box {
            IconButton(onClick = onSortMenuOpen) {
                Icon(Icons.Rounded.Sort, contentDescription = stringResource(R.string.action_sort))
            }
            DropdownMenu(expanded = sortMenuOpen, onDismissRequest = onSortMenuDismiss) {
                SortOrder.entries.forEach { order ->
                    DropdownMenuItem(
                        text = { Text(sortLabel(order)) },
                        onClick = { onSortSelected(order) },
                        leadingIcon = {
                            if (order == sortOrder) {
                                Icon(
                                    Icons.Rounded.Favorite,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun sortLabel(order: SortOrder): String = when (order) {
    SortOrder.TITLE -> stringResource(R.string.sort_title)
    SortOrder.COMPOSER -> stringResource(R.string.sort_composer)
    SortOrder.YEAR -> stringResource(R.string.sort_year)
    SortOrder.RECENT -> stringResource(R.string.sort_recent)
    SortOrder.ADDED -> stringResource(R.string.sort_added)
}

@Composable
private fun SongActions(
    expanded: Boolean,
    song: Song,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddToSetlist: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_edit)) },
            onClick = {
                onDismiss()
                onEdit()
            },
            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
        )
        DropdownMenuItem(
            text = {
                Text(stringResource(if (song.favorite) R.string.action_unfavorite else R.string.action_favorite))
            },
            onClick = {
                onDismiss()
                onToggleFavorite()
            },
            leadingIcon = {
                Icon(
                    if (song.favorite) Icons.Rounded.FavoriteBorder else Icons.Rounded.Favorite,
                    contentDescription = null,
                )
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.add_to_setlist)) },
            onClick = {
                onDismiss()
                onAddToSetlist()
            },
            leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, contentDescription = null) },
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
            onClick = {
                onDismiss()
                onDelete()
            },
            leadingIcon = {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongGridCard(
    song: Song,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddToSetlist: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val unknownComposer = stringResource(R.string.unknown_composer)

    Column(
        modifier = modifier
            .pressScale()
            .fillMaxWidth()
            // Small bottom corners: a 22 dp curve clipped the "J" of the composer line.
            .clip(RoundedCornerShape(22.dp, 22.dp, 8.dp, 8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { menu = true },
            ),
    ) {
        Box {
            SongThumbnail(
                song = song,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f),
                cornerRadius = 22.dp,
            )
            if (song.favorite) {
                FavoriteBadge(modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
            }
            MoreButton(
                onClick = { menu = true },
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            )
            if (song.hasAnnotations) {
                AnnotationBadge(modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp))
            }
            SongActions(
                expanded = menu,
                song = song,
                onDismiss = { menu = false },
                onEdit = onEdit,
                onDelete = onDelete,
                onAddToSetlist = onAddToSetlist,
                onToggleFavorite = onToggleFavorite,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        Text(
            text = song.composer.ifBlank { song.genre }.ifBlank { unknownComposer },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongListRow(
    song: Song,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddToSetlist: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val unknownComposer = stringResource(R.string.unknown_composer)

    Box(modifier) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            modifier = Modifier
                .pressScale()
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = { menu = true }),
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SongThumbnail(
                    song = song,
                    modifier = Modifier
                        .width(56.dp)
                        .height(74.dp),
                    cornerRadius = 14.dp,
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = song.composer.ifBlank {
                            song.collections.firstOrNull() ?: unknownComposer
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = pluralStringResource(R.plurals.page_count, song.pageCount, song.pageCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    )
                }
                if (song.favorite) {
                    Icon(
                        Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                }
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = null)
                }
            }
        }

        SongActions(
            expanded = menu,
            song = song,
            onDismiss = { menu = false },
            onEdit = onEdit,
            onDelete = onDelete,
            onAddToSetlist = onAddToSetlist,
            onToggleFavorite = onToggleFavorite,
        )
    }
}
