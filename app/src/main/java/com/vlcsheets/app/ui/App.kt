package com.vlcsheets.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vlcsheets.app.MainViewModel
import com.vlcsheets.app.R
import com.vlcsheets.app.ui.screens.CollectionsScreen
import com.vlcsheets.app.ui.screens.LibraryScreen
import com.vlcsheets.app.ui.screens.SetlistDetailScreen
import com.vlcsheets.app.ui.screens.SetlistsScreen
import com.vlcsheets.app.ui.screens.SettingsScreen
import com.vlcsheets.app.ui.screens.ViewerScreen
import com.vlcsheets.app.ui.theme.VlcSheetsTheme

enum class Tab { LIBRARY, SETLISTS, COLLECTIONS, SETTINGS }

/** What fills the window; a change of kind cross-fades, a change inside the same kind does not. */
private sealed interface Screen {
    data class Viewer(val request: ViewerRequest) : Screen
    data class Setlist(val id: String) : Screen
    data object Tabs : Screen
}

data class ViewerRequest(
    val songId: String,
    val queue: List<String> = listOf(songId),
    val label: String? = null,
)

@Composable
fun VlcSheetsRoot(vm: MainViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    VlcSheetsTheme(settings.themeMode, settings.dynamicColor) {
        val resources = LocalResources.current
        val snackbarHostState = remember { SnackbarHostState() }
        var tab by rememberSaveable { mutableStateOf(Tab.LIBRARY) }
        var viewer by remember { mutableStateOf<ViewerRequest?>(null) }
        var openSetlistId by rememberSaveable { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            vm.messages.collect { message ->
                snackbarHostState.showSnackbar(
                    message.quantity?.let { resources.getQuantityString(message.resId, it, *message.args.toTypedArray()) }
                        ?: resources.getString(message.resId, *message.args.toTypedArray()),
                )
            }
        }

        val openSongId by vm.openSongId.collectAsStateWithLifecycle()
        LaunchedEffect(openSongId) {
            val id = openSongId
            if (id != null) {
                tab = Tab.LIBRARY
                openSetlistId = null
                viewer = ViewerRequest(songId = id, queue = listOf(id))
                vm.consumeOpenSong()
            }
        }

        val screen = viewer?.let { Screen.Viewer(it) } ?: openSetlistId?.let { Screen.Setlist(it) } ?: Screen.Tabs
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = screen,
                contentKey = { it::class },
                transitionSpec = {
                    (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.97f)) togetherWith fadeOut(tween(150))
                },
                label = "screen",
            ) { current ->
                when (current) {
                    is Screen.Viewer -> {
                        val request = current.request
                        BackHandler { viewer = null }
                        ViewerScreen(
                            vm = vm,
                            request = request,
                            onClose = { viewer = null },
                            onOpenSong = { id ->
                                viewer = request.copy(songId = id)
                            },
                        )
                    }

                    is Screen.Setlist -> {
                        BackHandler { openSetlistId = null }
                        SetlistDetailScreen(
                            vm = vm,
                            setlistId = current.id,
                            onBack = { openSetlistId = null },
                            onOpenSong = { id, queue ->
                                val label = vm.library.value.setlists
                                    .firstOrNull { it.id == current.id }?.name
                                viewer = ViewerRequest(songId = id, queue = queue, label = label)
                            },
                        )
                    }

                    Screen.Tabs -> {
                        Scaffold(
                            bottomBar = {
                                NavigationBar(tonalElevation = 0.dp) {
                                    NavigationBarItem(
                                        selected = tab == Tab.LIBRARY,
                                        onClick = { tab = Tab.LIBRARY },
                                        icon = { Icon(Icons.Rounded.LibraryMusic, contentDescription = null) },
                                        label = { Text(stringResource(R.string.nav_library)) },
                                    )
                                    NavigationBarItem(
                                        selected = tab == Tab.SETLISTS,
                                        onClick = { tab = Tab.SETLISTS },
                                        icon = { Icon(Icons.Rounded.QueueMusic, contentDescription = null) },
                                        label = { Text(stringResource(R.string.nav_setlists)) },
                                    )
                                    NavigationBarItem(
                                        selected = tab == Tab.COLLECTIONS,
                                        onClick = { tab = Tab.COLLECTIONS },
                                        icon = { Icon(Icons.Rounded.CollectionsBookmark, contentDescription = null) },
                                        label = { Text(stringResource(R.string.nav_collections)) },
                                    )
                                    NavigationBarItem(
                                        selected = tab == Tab.SETTINGS,
                                        onClick = { tab = Tab.SETTINGS },
                                        icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                                        label = { Text(stringResource(R.string.nav_settings)) },
                                    )
                                }
                            },
                        ) { padding ->
                            Crossfade(tab, Modifier.fillMaxSize().padding(padding), tween(180), label = "tab") { shown ->
                                when (shown) {
                                    Tab.LIBRARY -> LibraryScreen(
                                        vm = vm,
                                        onOpenSong = { song ->
                                            viewer = ViewerRequest(songId = song.id)
                                        },
                                    )

                                    Tab.SETLISTS -> SetlistsScreen(
                                        vm = vm,
                                        onOpenSetlist = { setlist -> openSetlistId = setlist.id },
                                        onPlaySetlist = { setlist ->
                                            setlist.songIds.firstOrNull()?.let { first ->
                                                viewer = ViewerRequest(
                                                    songId = first,
                                                    queue = setlist.songIds,
                                                    label = setlist.name,
                                                )
                                            }
                                        },
                                    )

                                    Tab.COLLECTIONS -> CollectionsScreen(
                                        vm = vm,
                                        onOpenCollection = { name ->
                                            vm.selectCollection(name)
                                            vm.setFavoritesOnly(false)
                                            tab = Tab.LIBRARY
                                        },
                                    )

                                    Tab.SETTINGS -> SettingsScreen(vm = vm)
                                }
                            }
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            )
        }
    }
}
