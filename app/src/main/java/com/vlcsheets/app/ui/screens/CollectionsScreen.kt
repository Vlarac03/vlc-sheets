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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.vlcsheets.app.data.Collection
import com.vlcsheets.app.ui.components.BrandMark
import com.vlcsheets.app.ui.components.ConfirmDialog
import com.vlcsheets.app.ui.components.EmptyState
import com.vlcsheets.app.ui.theme.BrandGradient
import com.vlcsheets.app.ui.theme.CoolGradient
import com.vlcsheets.app.ui.theme.Brass
import com.vlcsheets.app.ui.theme.Clay
import com.vlcsheets.app.ui.theme.Ink
import com.vlcsheets.app.ui.theme.Sage
import com.vlcsheets.app.ui.theme.WarmGradient

private val collectionGradients = listOf(
    BrandGradient,
    CoolGradient,
    WarmGradient,
    Brush.linearGradient(listOf(Color(0xFF7A5A1E), Brass)),
    Brush.linearGradient(listOf(Ink, Sage)),
    Brush.linearGradient(listOf(Color(0xFF4A2F3A), Clay)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    vm: MainViewModel,
    onOpenCollection: (String) -> Unit,
) {
    val library by vm.library.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }

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
                        text = stringResource(R.string.collections_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = pluralStringResource(R.plurals.collection_count, library.collections.size, library.collections.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { showCreate = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.new_collection))
                }
            }

            if (library.collections.isEmpty()) {
                EmptyState(
                    icon = {
                        Icon(
                            Icons.Rounded.CollectionsBookmark,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    title = stringResource(R.string.empty_collections_title),
                    subtitle = stringResource(R.string.empty_collections_subtitle),
                    modifier = Modifier.weight(1f),
                    action = {
                        Button(onClick = { showCreate = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.new_collection))
                        }
                    },
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 164.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 108.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    itemsIndexed(library.collections, key = { _, c -> c.id }) { index, collection ->
                        CollectionCard(
                            collection = collection,
                            songCount = library.songs.count { collection.name in it.collections },
                            gradient = collectionGradients[index % collectionGradients.size],
                            onClick = { onOpenCollection(collection.name) },
                            onRename = { renameTarget = collection.name },
                            onDelete = { deleteTarget = collection.name },
                        )
                    }
                }
            }
        }

        // An empty screen has its own big button; the floating one comes in once there is content.
        AnimatedVisibility(
            visible = library.collections.isNotEmpty(),
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
                text = { Text(stringResource(R.string.new_collection)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }

    if (showCreate) {
        NameDialog(
            title = stringResource(R.string.new_collection),
            placeholder = stringResource(R.string.collection_name),
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                vm.createCollection(name)
                showCreate = false
            },
        )
    }

    renameTarget?.let { name ->
        NameDialog(
            title = stringResource(R.string.rename_collection),
            initial = name,
            placeholder = stringResource(R.string.collection_name),
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                vm.renameCollection(name, newName)
                renameTarget = null
            },
        )
    }

    deleteTarget?.let { name ->
        ConfirmDialog(
            title = stringResource(R.string.delete_collection_title),
            text = stringResource(R.string.delete_collection_text, name),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = { vm.deleteCollection(name) },
            onDismiss = { deleteTarget = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionCard(
    collection: Collection,
    songCount: Int,
    gradient: Brush,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Column(
            modifier = Modifier
                .pressScale()
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(gradient)
                .combinedClickable(onClick = onClick, onLongClick = { menu = true })
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface2(icon = Icons.Rounded.CollectionsBookmark)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { menu = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more_options), tint = Color.White)
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = collection.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = pluralStringResource(R.plurals.song_count, songCount, songCount),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
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
private fun Surface2(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    androidx.compose.material3.Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.18f),
        modifier = Modifier.size(38.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}
