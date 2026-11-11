package com.vlcsheets.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.input.KeyboardType
import com.vlcsheets.app.data.Jump
import com.vlcsheets.app.data.LibraryRepository
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.GraphicEq
import com.vlcsheets.app.data.Mask
import com.vlcsheets.app.data.SideAction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.InvertColors
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke as DrawScopeStroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vlcsheets.app.MainViewModel
import com.vlcsheets.app.R
import com.vlcsheets.app.data.AudioMark
import com.vlcsheets.app.data.Song
import com.vlcsheets.app.data.Stroke
import com.vlcsheets.app.media.AudioPlayer
import com.vlcsheets.app.media.effectiveBpm
import com.vlcsheets.app.media.loopJump
import com.vlcsheets.app.media.metronomeTurns
import com.vlcsheets.app.media.pageForPosition
import com.vlcsheets.app.media.practiceSpeeds
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.res.pluralStringResource
import com.vlcsheets.app.media.Crop
import com.vlcsheets.app.media.RenderedPage
import com.vlcsheets.app.media.DocumentRenderer
import com.vlcsheets.app.media.MetronomeState
import com.vlcsheets.app.ui.ViewerRequest
import com.vlcsheets.app.ui.components.ConfirmDialog
import com.vlcsheets.app.ui.components.SongThumbnail
import com.vlcsheets.app.ui.components.pressScale
import com.vlcsheets.app.ui.theme.Brass
import com.vlcsheets.app.ui.theme.Clay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class DrawTool { PEN, HIGHLIGHTER, ERASER }

data class PenStyle(
    val tool: DrawTool = DrawTool.PEN,
    val color: Long = 0xFFFFC107,
    val width: Float = 2f,
)

/** Tapping the page places jump markers or covers areas instead of turning pages. */
enum class PageEdit { JUMPS, MASKS }

private val WarmPaper = Color(0xFFF3E6CC)

private val inkColors = listOf(
    0xFFFFC107, // amber
    0xFFFF6B6B, // coral
    0xFF4FC3F7, // sky
    0xFF81C784, // green
    0xFFFFFFFF, // white
    0xFF111111, // ink
)

@Composable
fun ViewerScreen(
    vm: MainViewModel,
    request: ViewerRequest,
    onClose: () -> Unit,
    onOpenSong: (String) -> Unit,
) {
    val library by vm.library.collectAsStateWithLifecycle()
    val song = library.songs.firstOrNull { it.id == request.songId }
    if (song == null) {
        LaunchedEffect(library.loaded) { if (library.loaded) onClose() }
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF07080B))) {
            if (!library.loaded) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                )
            }
        }
        return
    }
    DisposableEffect(Unit) {
        onDispose { vm.metronome.stop() }
    }
    key(song.id) {
        ViewerContent(
            vm = vm,
            song = song,
            request = request,
            onClose = onClose,
            onNavigateSong = onOpenSong,
        )
    }
}

@Composable
private fun ViewerContent(
    vm: MainViewModel,
    song: Song,
    request: ViewerRequest,
    onClose: () -> Unit,
    onNavigateSong: (String) -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findActivity() as? com.vlcsheets.app.MainActivity
    val settings by vm.settings.collectAsStateWithLifecycle()
    val library by vm.library.collectAsStateWithLifecycle()

    val queue = remember(request.queue, request.songId) {
        request.queue.ifEmpty { listOf(request.songId) }
    }
    val queueIndex = queue.indexOf(song.id).coerceAtLeast(0)
    val hasNextSong = queueIndex < queue.lastIndex

    val renderer = remember(song.id) { DocumentRenderer(context, song) }
    DisposableEffect(renderer) { onDispose { renderer.close() } }

    LaunchedEffect(song.id) { vm.markOpened(song.id) }

    // Two pages side by side when the setting is on and the screen is wider than tall.
    // shortcut: in two-page mode a pinch zooms both pages, each around its own centre; zoom the pair as one if it bothers.
    val windowSize = LocalWindowInfo.current.containerSize
    val spread = if (settings.twoPagesLandscape && windowSize.width > windowSize.height && song.pageCount > 1) 2 else 1
    // Songs opened from a setlist (only those requests carry a label) start at page 1, as in a concert.
    val startPage = if (request.label != null) 0 else song.lastPage
    var lastDocPage by remember { mutableIntStateOf(startPage.coerceIn(0, (song.pageCount - 1).coerceAtLeast(0))) }
    val pagerState = key(spread) {
        rememberPagerState(initialPage = lastDocPage / spread, pageCount = { (song.pageCount + spread - 1) / spread })
    }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var chromeVisible by remember { mutableStateOf(true) }
    var annotationMode by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var showMetronome by remember { mutableStateOf(false) }
    var showAudio by remember { mutableStateOf(false) }
    val audio = remember { AudioPlayer() }
    DisposableEffect(audio) { onDispose { audio.release() } }
    var audioVersion by remember { mutableIntStateOf(0) }
    val audioFiles = remember(song.id, audioVersion) { vm.audioFiles(song.id) }
    var showInfo by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    // Settings → "Invert pages in dark mode" sets how a score opens; the menu still toggles it.
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var invertPage by remember { mutableStateOf(settings.invertPagesInDark && darkTheme) }
    var overflowOpen by remember { mutableStateOf(false) }
    var penStyle by remember { mutableStateOf(PenStyle()) }
    var concertMode by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf<PageEdit?>(null) }
    var pendingJump by remember { mutableStateOf<Jump?>(null) }
    var showTuner by remember { mutableStateOf(false) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val metronomeState by vm.metronomeState.collectAsStateWithLifecycle()

    val targetWidth = remember(windowSize.width, spread) { (windowSize.width * 1.4f / spread).toInt().coerceIn(900, 2000) }

    // Half-page turning: step 2k shows page k; step 2k+1 shows the top of page k+1 over the bottom of page k.
    val halfMode = settings.halfPageTurn && spread == 1 && !annotationMode && editMode == null && song.pageCount > 1
    var halfStep by remember { mutableIntStateOf(-1) }
    LaunchedEffect(halfMode) {
        if (halfMode) {
            halfStep = pagerState.currentPage * 2
        } else if (halfStep >= 0) {
            pagerState.requestScrollToPage((halfStep + 1) / 2)
            halfStep = -1
        }
    }
    val inHalfView = halfMode && halfStep >= 0
    val page = if (inHalfView) (halfStep + 1) / 2 else pagerState.currentPage * spread
    val visiblePages = page until (page + spread).coerceAtMost(song.pageCount)
    val pageLabel = stringResource(R.string.page_of, page + 1, song.pageCount)
    val queueLabel = if (queue.size > 1) {
        stringResource(R.string.queue_position, queueIndex + 1, queue.size)
    } else null
    val viewerSubtitle = listOfNotNull(pageLabel, request.label, queueLabel).joinToString("  ·  ")

    // Keep the screen awake while reading.
    DisposableEffect(settings.keepScreenOn, activity) {
        val window = activity?.window
        if (settings.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // Immersive reading mode.
    DisposableEffect(chromeVisible, activity) {
        val controller = activity?.let {
            WindowInsetsControllerCompat(it.window, it.window.decorView)
        }
        // The viewer is always dark: light status bar icons, put back as they were on leaving.
        val lightBars = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        if (chromeVisible) {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            lightBars?.let { controller.isAppearanceLightStatusBars = it }
        }
    }

    LaunchedEffect(chromeVisible, annotationMode, page, overflowOpen) {
        if (chromeVisible && !annotationMode && !overflowOpen) {
            delay(5000)
            chromeVisible = false
        }
    }

    LaunchedEffect(page) {
        scale = 1f
        offset = Offset.Zero
        lastDocPage = page
        delay(1200)
        vm.saveLastPage(song.id, page)
    }
    // Closing the score before the delay above still keeps the page.
    DisposableEffect(Unit) { onDispose { vm.saveLastPage(song.id, lastDocPage) } }

    // Page turns never leave the song; moving to the next song in a setlist is an explicit button.
    fun goPage(delta: Int) {
        if (inHalfView) {
            halfStep = (halfStep + delta).coerceIn(0, 2 * (song.pageCount - 1))
            return
        }
        val target = (pagerState.currentPage + delta).coerceIn(0, pagerState.pageCount - 1)
        if (target == pagerState.currentPage) return
        scope.launch { pagerState.animateScrollToPage(target, animationSpec = tween(140)) }
    }
    fun jumpTo(docPage: Int) {
        val p = docPage.coerceIn(0, song.pageCount - 1)
        if (inHalfView) halfStep = p * 2 else scope.launch { pagerState.animateScrollToPage(p / spread) }
    }
    val atLastPage = if (inHalfView) halfStep >= 2 * (song.pageCount - 1) else pagerState.currentPage >= pagerState.pageCount - 1
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { vm.exportPdf(song, it) }
    }
    val shareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let { vm.exportShare(it, listOf(song.id)) }
    }
    val onPageTap: (Int) -> Unit = { direction ->
        if (direction != 0) goPage(direction) else if (!concertMode) chromeVisible = !chromeVisible
    }
    fun runSideAction(action: SideAction) {
        when (action) {
            SideAction.NONE -> {}
            SideAction.FIRST_PAGE -> jumpTo(0)
            SideAction.LAST_PAGE -> jumpTo(song.pageCount - 1)
            SideAction.PREV_SONG -> if (queueIndex > 0) onNavigateSong(queue[queueIndex - 1])
            SideAction.NEXT_SONG -> if (hasNextSong) onNavigateSong(queue[queueIndex + 1])
            SideAction.AUDIO -> if (audio.file != null || audioFiles.firstOrNull()?.let { audio.load(it) } == true) audio.toggle()
            SideAction.METRONOME -> vm.toggleMetronome()
        }
    }
    fun playAudioButton(j: Jump) {
        val file = audioFiles.firstOrNull { it.name == j.audioFile } ?: return
        if (audio.file != file && !audio.load(file)) return
        audio.playFrom(j.audioMs)
    }
    // The tap modifier lives across recompositions, so it reads the latest handlers.
    val tapLatest by rememberUpdatedState(onPageTap)
    val holdLatest by rememberUpdatedState<(SideAction) -> Unit> { runSideAction(it) }
    // Auto handwriting: the strokes written since the last pause become text 1.5 s after the pen stops.
    val freshStrokes = remember { mutableStateListOf<String>() }
    LaunchedEffect(freshStrokes.size) {
        if (freshStrokes.isEmpty()) return@LaunchedEffect
        delay(1500)
        val ids = freshStrokes.toSet()
        freshStrokes.clear()
        vm.convertHandwriting(song.id, auto = true) { it.id in ids }
    }
    val removeMark: (String) -> Unit = { id ->
        vm.editSong(song.id) { s -> s.copy(jumps = s.jumps.filterNot { it.id == id }, masks = s.masks.filterNot { it.id == id }) }
    }

    @Composable
    fun Page(p: Int, interactive: Boolean, isCurrent: Boolean = false) = PageView(
        song = song,
        page = p,
        renderer = renderer,
        targetWidth = targetWidth,
        invert = invertPage,
        warm = settings.warmPaper,
        crop = settings.cropMargins,
        annotationMode = interactive && annotationMode,
        penStyle = penStyle,
        strokes = remember(song.annotations, p, settings.hideAnnotations, annotationMode) {
            if (settings.hideAnnotations && !annotationMode) emptyList() else song.annotations.filter { it.page == p }
        },
        jumps = remember(song.jumps, p) { song.jumps.filter { it.page == p } },
        masks = remember(song.masks, p) { song.masks.filter { it.page == p } },
        editMode = if (interactive) editMode else null,
        zoomEnabled = interactive && !concertMode && editMode == null,
        scale = if (isCurrent) scale else 1f,
        offset = if (isCurrent) offset else Offset.Zero,
        onTransform = { newScale, newOffset ->
            scale = newScale
            offset = newOffset
        },
        onAddStroke = { stroke ->
            vm.addStroke(song.id, stroke)
            if (settings.autoHandwriting) freshStrokes += stroke.id
        },
        onErase = { point -> eraseStrokeAt(song, p, point, vm) },
        onJump = { j -> if (j.isAudio) playAudioButton(j) else jumpTo(j.target) },
        onPlaceJump = { at -> pendingJump = Jump(java.util.UUID.randomUUID().toString(), p, at.x, at.y, 0) },
        onAddMask = { mask -> vm.editSong(song.id) { it.copy(masks = it.masks + mask) } },
        onRemoveMark = removeMark,
    )

    val keyHandler = rememberUpdatedState<(Int, Int) -> Boolean> { direction, keyCode ->
        val isVolume = keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        when {
            isVolume && !settings.volumeKeys -> false
            scale > 1.02f -> false
            else -> {
                goPage(direction)
                true
            }
        }
    }

    DisposableEffect(activity) {
        activity?.pageTurnHandler = { direction, keyCode -> keyHandler.value(direction, keyCode) }
        onDispose { activity?.pageTurnHandler = null }
    }

    // The metronome starts at the score's saved tempo and time signature.
    LaunchedEffect(song.id, song.tempo, song.beatsPerBar) {
        val m = vm.metronomeState.value
        val bpm = song.tempo.takeIf { it in 30..300 } ?: m.bpm
        val beats = song.beatsPerBar.takeIf { it > 0 } ?: m.beatsPerBar
        if (bpm != m.bpm || beats != m.beatsPerBar) vm.configureMetronome(bpm, beats, m.accentFirst)
    }

    // Practice speed: the audio and the metronome slow down together.
    LaunchedEffect(audio.speed) { vm.setPracticeSpeed(audio.speed) }

    // While the audio plays: repeat the loop and turn to the page of the last mark passed.
    val jumpToLatest by rememberUpdatedState<(Int) -> Unit> { jumpTo(it) }
    var loopStart by remember { mutableStateOf<Int?>(null) }
    var loopEnd by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(audio.file) {
        loopStart = null
        loopEnd = null
    }
    val fileMarks = remember(song.audioMarks, audio.file) {
        song.audioMarks.filter { it.file == audio.file?.name }.sortedBy { it.ms }
    }
    LaunchedEffect(audio.playing, fileMarks, loopStart, loopEnd) {
        var lastMarkPage: Int? = null
        while (audio.playing) {
            val pos = audio.positionMs
            loopJump(pos, loopStart, loopEnd)?.let { audio.seekTo(it) }
            val target = pageForPosition(fileMarks, pos)
            if (target != null && target != lastMarkPage) jumpToLatest(target)
            lastMarkPage = target
            delay(80)
        }
    }

    // Metronome page turns: one page every N bars after the count-in. Manual turns are kept.
    val goPageLatest by rememberUpdatedState<(Int) -> Unit> { goPage(it) }
    var metronomeTurnsDone by remember {
        // A song opened while the metronome runs (setlist) starts counting from now.
        val m = vm.metronomeState.value
        mutableIntStateOf(metronomeTurns(m.beatIndex, m.beatsPerBar, song.countInBars, song.barsPerPage))
    }
    LaunchedEffect(metronomeState.beatIndex) {
        if (metronomeState.beatIndex < 0) metronomeTurnsDone = 0
        val due = metronomeTurns(metronomeState.beatIndex, metronomeState.beatsPerBar, song.countInBars, song.barsPerPage)
        while (metronomeTurnsDone < due) {
            metronomeTurnsDone++
            goPageLatest(1)
        }
    }

    val currentSongBookmarks = song.bookmarks
    val concertHint = stringResource(R.string.concert_hint)

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF07080B))) {
        val tapZones = Modifier.pageTapZones(
            enabled = !annotationMode && editMode == null && scale <= 1.02f,
            sides = settings.tapToTurn,
            // Concert mode only lets pages turn.
            holds = if (concertMode) SideAction.NONE to SideAction.NONE else settings.leftHold to settings.rightHold,
            onTap = { tapLatest(it) },
            onHold = { holdLatest(it) },
        )
        if (inHalfView) {
            HalfPageView(
                step = halfStep,
                modifier = Modifier.fillMaxSize().then(tapZones),
            ) { index -> Page(index, interactive = false) }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = scale <= 1.02f && editMode == null,
                beyondViewportPageCount = 1,
            ) { index ->
                val isCurrent = index == pagerState.currentPage
                Row(modifier = Modifier.fillMaxSize().then(tapZones)) {
                    for (p in index * spread until (index * spread + spread).coerceAtMost(song.pageCount)) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            Page(p, interactive = true, isCurrent = isCurrent)
                        }
                    }
                    if (spread == 2 && index * 2 + 1 >= song.pageCount) Spacer(Modifier.weight(1f))
                }
            }
        }

        AnimatedVisibility(
            visible = chromeVisible && !annotationMode && !concertMode && editMode == null,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xE607080B), Color(0x9907080B), Color.Transparent),
                        ),
                    )
                    .statusBarsPadding()
                    .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = Color.White,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = viewerSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { vm.toggleFavorite(song) }) {
                        Icon(
                            if (song.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = stringResource(R.string.favorite),
                            tint = if (song.favorite) Brass else Color.White,
                        )
                    }
                    Box {
                        IconButton(onClick = { overflowOpen = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more_options), tint = Color.White)
                        }
                        DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.bookmarks)) },
                                onClick = {
                                    overflowOpen = false
                                    showBookmarks = true
                                },
                                leadingIcon = { Icon(Icons.Rounded.Bookmark, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.metronome)) },
                                onClick = {
                                    overflowOpen = false
                                    showMetronome = true
                                },
                                leadingIcon = { Icon(Icons.Rounded.Timer, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.tuner)) },
                                onClick = {
                                    overflowOpen = false
                                    showTuner = true
                                },
                                leadingIcon = { Icon(Icons.Rounded.GraphicEq, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.handwriting_convert)) },
                                onClick = {
                                    overflowOpen = false
                                    vm.convertHandwriting(song.id) { it.page in visiblePages }
                                },
                                leadingIcon = { Icon(Icons.Rounded.TextFields, contentDescription = null) },
                            )
                            ModeMenuItem(stringResource(R.string.hide_annotations), settings.hideAnnotations) {
                                vm.updateSettings { s -> s.copy(hideAnnotations = !s.hideAnnotations) }
                            }
                            ModeMenuItem(stringResource(R.string.warm_paper), settings.warmPaper) {
                                vm.updateSettings { s -> s.copy(warmPaper = !s.warmPaper) }
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.concert_mode)) },
                                onClick = {
                                    overflowOpen = false
                                    concertMode = true
                                    chromeVisible = false
                                    scale = 1f
                                    offset = Offset.Zero
                                    Toast.makeText(context, concertHint, Toast.LENGTH_LONG).show()
                                },
                                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.jump_points)) },
                                onClick = {
                                    overflowOpen = false
                                    editMode = PageEdit.JUMPS
                                },
                                leadingIcon = { Icon(Icons.Rounded.Flag, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.mask_zones)) },
                                onClick = {
                                    overflowOpen = false
                                    editMode = PageEdit.MASKS
                                },
                                leadingIcon = { Icon(Icons.Rounded.CropFree, contentDescription = null) },
                            )
                            ModeMenuItem(stringResource(R.string.crop_margins), settings.cropMargins) {
                                vm.updateSettings { s -> s.copy(cropMargins = !s.cropMargins) }
                            }
                            ModeMenuItem(stringResource(R.string.two_pages_landscape), settings.twoPagesLandscape) {
                                vm.updateSettings { s -> s.copy(twoPagesLandscape = !s.twoPagesLandscape) }
                            }
                            ModeMenuItem(stringResource(R.string.half_page_turn), settings.halfPageTurn) {
                                vm.updateSettings { s -> s.copy(halfPageTurn = !s.halfPageTurn) }
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_pdf)) },
                                onClick = {
                                    overflowOpen = false
                                    exportLauncher.launch("${song.title.ifBlank { "score" }}.pdf")
                                },
                                leadingIcon = { Icon(Icons.Rounded.PictureAsPdf, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_share)) },
                                onClick = {
                                    overflowOpen = false
                                    shareLauncher.launch("${song.title.ifBlank { "score" }}.${LibraryRepository.SHARE_EXTENSION}")
                                },
                                leadingIcon = { Icon(Icons.Rounded.Archive, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.audio)) },
                                onClick = {
                                    overflowOpen = false
                                    showAudio = true
                                },
                                leadingIcon = { Icon(Icons.Rounded.MusicNote, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.invert_colors)) },
                                onClick = {
                                    overflowOpen = false
                                    invertPage = !invertPage
                                },
                                leadingIcon = { Icon(Icons.Rounded.InvertColors, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit_song)) },
                                onClick = {
                                    overflowOpen = false
                                    showInfo = true
                                },
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = chromeVisible && !annotationMode && !concertMode && editMode == null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Surface(
                color = Color(0xF20B0D13),
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (song.pageCount > 1) {
                        var sliderPage by remember(page, song.pageCount) {
                            mutableFloatStateOf((page + 1).toFloat())
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${page + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                            Slider(
                                value = sliderPage,
                                onValueChange = { sliderPage = it },
                                onValueChangeFinished = { jumpTo(sliderPage.roundToInt() - 1) },
                                valueRange = 1f..song.pageCount.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = Brass,
                                    activeTrackColor = Brass,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.22f),
                                ),
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            )
                            Text(
                                text = "${song.pageCount}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { goPage(-1) }, modifier = Modifier.pressScale(0.88f)) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.action_prev_page),
                                tint = Color.White,
                            )
                        }
                        val showNextSong = atLastPage && hasNextSong
                        TextButton(onClick = {
                            if (showNextSong) onNavigateSong(queue[queueIndex + 1]) else goPage(1)
                        }) {
                            Text(
                                text = if (showNextSong) stringResource(R.string.next_song) else stringResource(R.string.action_next_page),
                                color = Color.White,
                                maxLines = 1,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.1f),
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(onClick = {
                                    annotationMode = true
                                    chromeVisible = true
                                }) {
                                    Icon(
                                        Icons.Rounded.Edit,
                                        contentDescription = stringResource(R.string.annotations),
                                        tint = Color.White,
                                    )
                                }
                                if (metronomeState.running) {
                                    IconButton(onClick = { showMetronome = true }) {
                                        Icon(
                                            Icons.Rounded.Timer,
                                            contentDescription = stringResource(R.string.metronome),
                                            tint = Brass,
                                        )
                                    }
                                }
                                if (audioFiles.isNotEmpty()) {
                                    IconButton(onClick = { showAudio = true }) {
                                        Icon(
                                            Icons.Rounded.MusicNote,
                                            contentDescription = stringResource(R.string.audio),
                                            tint = if (audio.playing) Brass else Color.White,
                                        )
                                    }
                                }
                                if (currentSongBookmarks.isNotEmpty()) {
                                    IconButton(onClick = { showBookmarks = true }) {
                                        Icon(
                                            Icons.Rounded.Bookmark,
                                            contentDescription = stringResource(R.string.bookmarks),
                                            tint = Color.White,
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                        IconButton(onClick = { goPage(1) }, modifier = Modifier.pressScale(0.88f)) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = stringResource(R.string.action_next_page),
                                tint = Color.White,
                            )
                        }
                    }
                }
            }
        }

        // A small player while reading (the bars are hidden): play/pause, the time, and the full audio panel.
        AnimatedVisibility(
            visible = audioFiles.isNotEmpty() && !chromeVisible && !annotationMode && !concertMode && editMode == null,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier.align(Alignment.BottomStart),
        ) {
            var shownMs by remember { mutableIntStateOf(0) }
            LaunchedEffect(audio.playing, audio.file) {
                shownMs = audio.positionMs
                while (audio.playing) {
                    delay(500)
                    shownMs = audio.positionMs
                }
            }
            Surface(
                color = Color(0xB30B0D13),
                shape = CircleShape,
                modifier = Modifier.navigationBarsPadding().padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                    IconButton(onClick = {
                        if (audio.file != null || audioFiles.firstOrNull()?.let { audio.load(it) } == true) audio.toggle()
                    }) {
                        Icon(
                            if (audio.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(if (audio.playing) R.string.stop else R.string.start),
                            tint = if (audio.playing) Brass else Color.White,
                        )
                    }
                    if (audio.file != null) {
                        Text(formatTime(shownMs.toFloat()), color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                    IconButton(onClick = { showAudio = true }) {
                        Icon(Icons.Rounded.MusicNote, contentDescription = stringResource(R.string.audio), tint = Color.White)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = annotationMode,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            AnnotationToolbar(
                penStyle = penStyle,
                onStyleChange = { penStyle = it },
                onUndo = { vm.undoAnnotation(song.id) },
                onClear = { showClearConfirm = true },
                onDone = {
                    annotationMode = false
                    chromeVisible = true
                },
            )
        }

        AnimatedVisibility(
            visible = concertMode,
            enter = fadeIn() + scaleIn(initialScale = 0.6f),
            exit = fadeOut() + scaleOut(targetScale = 0.6f),
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(12.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x550B0D13))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { Toast.makeText(context, concertHint, Toast.LENGTH_SHORT).show() },
                            onLongPress = {
                                concertMode = false
                                chromeVisible = true
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = stringResource(R.string.concert_exit),
                    tint = Color.White.copy(alpha = 0.7f),
                )
            }
        }

        AnimatedContent(
            targetState = editMode,
            transitionSpec = {
                (fadeIn() + slideInVertically { -it }) togetherWith (fadeOut() + slideOutVertically { -it })
            },
            modifier = Modifier.align(Alignment.TopCenter),
            label = "editBar",
        ) { mode ->
            if (mode != null) Surface(
                color = Color(0xF20B0D13),
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(if (mode == PageEdit.JUMPS) R.string.jump_points_hint else R.string.mask_zones_hint),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { editMode = null }) {
                        Text(stringResource(R.string.action_done), color = Brass)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !annotationMode && scale > 1.02f,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Surface(
                color = Color(0xCC0B0D13),
                shape = CircleShape,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 12.dp),
            ) {
                TextButton(onClick = {
                    scale = 1f
                    offset = Offset.Zero
                }) {
                    Text(stringResource(R.string.reset_zoom), color = Color.White)
                }
            }
        }
    }

    pendingJump?.let { jump ->
        var text by remember(jump.id) { mutableStateOf("") }
        val target = text.toIntOrNull()?.takeIf { it in 1..song.pageCount }
        AlertDialog(
            onDismissRequest = { pendingJump = null },
            title = { Text(stringResource(R.string.jump_target_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { v -> text = v.filter { it.isDigit() }.take(4) },
                        label = { Text(stringResource(R.string.jump_target_label)) },
                        supportingText = { Text("1 – ${song.pageCount}") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Spacer(Modifier.height(8.dp))
                    // An audio button starts the loaded track at the moment it is now.
                    val audioFile = audio.file
                    if (audioFile != null) {
                        val ms = audio.positionMs
                        OutlinedButton(
                            onClick = {
                                vm.editSong(song.id) { it.copy(jumps = it.jumps + jump.copy(audioFile = audioFile.name, audioMs = ms)) }
                                pendingJump = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.audio_button_make, formatTime(ms.toFloat())))
                        }
                    } else {
                        Text(stringResource(R.string.audio_button_hint), style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = target != null,
                    onClick = {
                        if (target != null) vm.editSong(song.id) { it.copy(jumps = it.jumps + jump.copy(target = target - 1)) }
                        pendingJump = null
                    },
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingJump = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (showBookmarks) {
        BookmarksSheet(
            song = song,
            currentPage = page,
            onDismiss = { showBookmarks = false },
            onAdd = { label -> vm.addBookmark(song.id, page, label) },
            onDelete = { bookmark -> vm.removeBookmark(song.id, bookmark) },
            onJump = { targetPage ->
                showBookmarks = false
                jumpTo(targetPage)
            },
        )
    }

    if (showMetronome) {
        MetronomeSheet(
            vm = vm,
            song = song,
            halfPage = inHalfView,
            onDismiss = { showMetronome = false },
        )
    }

    if (showTuner) TunerSheet(vm) { showTuner = false }

    if (showAudio) {
        AudioSheet(
            player = audio,
            files = audioFiles,
            currentPage = page,
            marks = fileMarks,
            onAddMark = { ms ->
                val name = audio.file?.name
                if (name != null) vm.editSong(song.id) { it.copy(audioMarks = it.audioMarks + AudioMark(name, ms, page)) }
            },
            onRemoveMark = { mark -> vm.editSong(song.id) { it.copy(audioMarks = it.audioMarks - mark) } },
            loopStart = loopStart,
            loopEnd = loopEnd,
            onLoopChange = { a, b ->
                loopStart = a
                loopEnd = b
            },
            onAdd = { uri -> vm.addAudio(song.id, uri) { audioVersion++ } },
            onRemove = { file ->
                if (audio.file == file) audio.release()
                vm.removeAudio(song.id, file) { audioVersion++ }
            },
            onDismiss = { showAudio = false },
        )
    }

    if (showInfo) {
        SongEditorSheet(
            song = song,
            collections = library.collections.map { it.name },
            onDismiss = { showInfo = false },
            onSave = {
                vm.saveSong(it)
                showInfo = false
            },
            onDelete = {
                vm.deleteSongs(setOf(song.id))
                showInfo = false
                onClose()
            },
        )
    }

    if (showClearConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.clear_annotations_title),
            text = stringResource(R.string.clear_annotations_text),
            confirmLabel = stringResource(R.string.action_clear),
            destructive = true,
            onConfirm = { visiblePages.forEach { vm.clearPage(song.id, it) } },
            onDismiss = { showClearConfirm = false },
        )
    }
}

@Composable
private fun PageView(
    song: Song,
    page: Int,
    renderer: DocumentRenderer,
    targetWidth: Int,
    invert: Boolean,
    warm: Boolean,
    crop: Boolean,
    annotationMode: Boolean,
    penStyle: PenStyle,
    strokes: List<Stroke>,
    jumps: List<Jump>,
    masks: List<Mask>,
    editMode: PageEdit?,
    zoomEnabled: Boolean,
    scale: Float,
    offset: Offset,
    onTransform: (Float, Offset) -> Unit,
    onAddStroke: (Stroke) -> Unit,
    onErase: (Offset) -> Unit,
    onJump: (Jump) -> Unit,
    onPlaceJump: (Offset) -> Unit,
    onAddMask: (Mask) -> Unit,
    onRemoveMark: (String) -> Unit,
) {
    val rendered by produceState<RenderedPage?>(initialValue = null, page, targetWidth, crop) {
        value = withContext(Dispatchers.IO) { renderer.render(page, targetWidth, crop) }
    }
    val bitmap = rendered?.bitmap
    // Strokes are stored relative to the whole page; this is the part of it on screen.
    val shown = rendered?.crop ?: Crop.FULL
    val aspect = bitmap?.let { it.width.toFloat() / it.height.toFloat() } ?: song.pageAspect

    val invertFilter = remember {
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        var pageWidth = maxWidth
        var pageHeight = pageWidth / aspect
        if (pageHeight > maxHeight) {
            pageHeight = maxHeight
            pageWidth = pageHeight * aspect
        }

        val density = LocalDensity.current
        val pageWidthPx = with(density) { pageWidth.toPx() }
        val pageHeightPx = with(density) { pageHeight.toPx() }
        val widthState = rememberUpdatedState(pageWidthPx)
        val heightState = rememberUpdatedState(pageHeightPx)

        // Points of the stroke being drawn, relative to the whole page (0..1).
        var livePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
        val currentShown by rememberUpdatedState(shown)

        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
            val newScale = (scale * zoomChange).coerceIn(1f, 6f)
            val maxX = (widthState.value * (newScale - 1f)) / 2f
            val maxY = (heightState.value * (newScale - 1f)) / 2f
            val newOffset = if (newScale <= 1.001f) {
                Offset.Zero
            } else {
                Offset(
                    (offset.x + panChange.x).coerceIn(-maxX, maxX),
                    (offset.y + panChange.y).coerceIn(-maxY, maxY),
                )
            }
            onTransform(newScale, newOffset)
        }

        val currentStyle by rememberUpdatedState(penStyle)
        val currentErase by rememberUpdatedState(onErase)
        val currentAddStroke by rememberUpdatedState(onAddStroke)
        val currentJumps by rememberUpdatedState(jumps)
        val currentMasks by rememberUpdatedState(masks)
        val currentOnJump by rememberUpdatedState(onJump)
        val currentPlaceJump by rememberUpdatedState(onPlaceJump)
        val currentAddMask by rememberUpdatedState(onAddMask)
        val currentRemoveMark by rememberUpdatedState(onRemoveMark)
        var liveMask by remember { mutableStateOf<Mask?>(null) }
        val markerRadius = with(density) { 14.dp.toPx() }

        // Jump markers and covered areas. Outside the edit modes only a tap on a marker is used;
        // any other tap is left for the page-turn zones.
        val markModifier = if (annotationMode) {
            Modifier
        } else {
            Modifier.pointerInput(page, editMode) {
                fun normalized(p: Offset): Offset {
                    val c = currentShown
                    return Offset(c.left + p.x / size.width * c.width, c.top + p.y / size.height * c.height)
                }
                fun jumpAt(p: Offset): Jump? {
                    val c = currentShown
                    return currentJumps.firstOrNull { j ->
                        val x = (j.x - c.left) / c.width * size.width
                        val y = (j.y - c.top) / c.height * size.height
                        hypot(x - p.x, y - p.y) < markerRadius * 1.8f
                    }
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    when (editMode) {
                        null -> {
                            val jump = jumpAt(down.position) ?: return@awaitEachGesture
                            val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                            up.consume()
                            currentOnJump(jump)
                        }
                        PageEdit.JUMPS -> {
                            down.consume()
                            val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                            up.consume()
                            val hit = jumpAt(up.position)
                            if (hit != null) currentRemoveMark(hit.id) else currentPlaceJump(normalized(up.position))
                        }
                        PageEdit.MASKS -> {
                            down.consume()
                            val start = normalized(down.position)
                            var end = start
                            while (true) {
                                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                change.consume()
                                end = normalized(change.position)
                                liveMask = maskBetween(page, start, end)
                            }
                            liveMask = null
                            if (abs(end.x - start.x) > 0.02f && abs(end.y - start.y) > 0.01f) {
                                currentAddMask(maskBetween(page, start, end).copy(id = java.util.UUID.randomUUID().toString()))
                            } else {
                                currentMasks.lastOrNull { start.x in it.left..it.right && start.y in it.top..it.bottom }
                                    ?.let { currentRemoveMark(it.id) }
                            }
                        }
                    }
                }
            }
        }
        val paperColor = when {
            invert -> Color.Black
            warm -> WarmPaper
            else -> Color.White
        }

        // Only the pen draws, so fingers still turn pages and pinch to zoom.
        // Holding the pen's side button (or its eraser end) erases while it is held.
        val drawModifier = if (!annotationMode) {
            Modifier
        } else {
            Modifier.pointerInput(page) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (down.type != PointerType.Stylus && down.type != PointerType.Eraser) return@awaitEachGesture
                    val buttons = currentEvent.buttons
                    val erasing = currentStyle.tool == DrawTool.ERASER ||
                        down.type == PointerType.Eraser ||
                        buttons.isPrimaryPressed || buttons.isSecondaryPressed
                    fun normalized(p: Offset): Offset {
                        val c = currentShown
                        return Offset(c.left + p.x / size.width * c.width, c.top + p.y / size.height * c.height)
                    }

                    down.consume()
                    if (erasing) currentErase(normalized(down.position)) else livePoints = listOf(normalized(down.position))
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        if (erasing) {
                            currentErase(normalized(change.position))
                        } else {
                            livePoints = livePoints + normalized(change.position)
                        }
                    }
                    if (!erasing && livePoints.size > 1 && size.width > 0) {
                        val style = currentStyle
                        currentAddStroke(
                            Stroke(
                                id = java.util.UUID.randomUUID().toString(),
                                page = page,
                                points = livePoints.map {
                                    com.vlcsheets.app.data.Pt(it.x.coerceIn(0f, 1f), it.y.coerceIn(0f, 1f))
                                },
                                color = style.color,
                                width = style.width,
                                highlighter = style.tool == DrawTool.HIGHLIGHTER,
                            ),
                        )
                    }
                    livePoints = emptyList()
                }
            }
        }

        Box(
            modifier = Modifier
                .size(pageWidth, pageHeight)
                .align(Alignment.Center)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .transformable(
                    state = transformState,
                    canPan = { scale > 1.02f },
                    enabled = zoomEnabled,
                )
                .then(drawModifier)
                .then(markModifier)
                .clip(RoundedCornerShape(2.dp))
                .background(paperColor),
        ) {
            bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                    colorFilter = when {
                        invert -> invertFilter
                        warm -> ColorFilter.tint(WarmPaper, BlendMode.Multiply)
                        else -> null
                    },
                )
            }

            // Finished marks live in their own layer, which is not redrawn while the pen moves:
            // only the layer on top, with the stroke being drawn, is.
            Canvas(modifier = Modifier.fillMaxSize().clipToBounds().graphicsLayer()) {
                val fullWidth = size.width / shown.width
                val fullHeight = size.height / shown.height
                translate(-shown.left * fullWidth, -shown.top * fullHeight) {
                    masks.forEach { m ->
                        val topLeft = Offset(m.left * fullWidth, m.top * fullHeight)
                        val area = Size((m.right - m.left) * fullWidth, (m.bottom - m.top) * fullHeight)
                        drawRect(paperColor, topLeft, area)
                        if (editMode == PageEdit.MASKS) drawRect(Brass, topLeft, area, style = DrawScopeStroke(width = 2.dp.toPx()))
                    }
                    strokes.forEach { stroke -> drawStroke(stroke, fullWidth, fullHeight) }
                    jumps.forEach { j -> drawJumpMarker(j, Offset(j.x * fullWidth, j.y * fullHeight), markerRadius) }
                }
            }
            Canvas(modifier = Modifier.fillMaxSize().clipToBounds().graphicsLayer()) {
                val fullWidth = size.width / shown.width
                val fullHeight = size.height / shown.height
                translate(-shown.left * fullWidth, -shown.top * fullHeight) {
                    liveMask?.let { m ->
                        val topLeft = Offset(m.left * fullWidth, m.top * fullHeight)
                        val area = Size((m.right - m.left) * fullWidth, (m.bottom - m.top) * fullHeight)
                        drawRect(Brass, topLeft, area, style = DrawScopeStroke(width = 2.dp.toPx()))
                    }
                    if (livePoints.size > 1) {
                        val liveStroke = Stroke(
                            id = "live",
                            page = page,
                            points = livePoints.map { com.vlcsheets.app.data.Pt(it.x, it.y) },
                            color = penStyle.color,
                            width = penStyle.width,
                            highlighter = penStyle.tool == DrawTool.HIGHLIGHTER,
                        )
                        drawStroke(liveStroke, fullWidth, fullHeight)
                    }
                }
            }
        }
    }
}

/**
 * Taps on the left or right part turn the page (if [sides]); a tap in the middle shows or hides the bars.
 * Holding the left or right part runs its [holds] action; a side without one turns the page as a tap.
 */
private fun Modifier.pageTapZones(
    enabled: Boolean,
    sides: Boolean,
    holds: Pair<SideAction, SideAction>,
    onTap: (Int) -> Unit,
    onHold: (SideAction) -> Unit,
): Modifier =
    if (!enabled) this else pointerInput(sides, holds) {
        fun side(x: Float, always: Boolean): Int {
            val w = size.width.toFloat()
            return when {
                (sides || always) && x < w * 0.28f -> -1
                (sides || always) && x > w * 0.72f -> 1
                else -> 0
            }
        }
        val hold: ((Offset) -> Unit)? = if (holds.first == SideAction.NONE && holds.second == SideAction.NONE) {
            null
        } else {
            { position ->
                val action = when (side(position.x, always = true)) {
                    -1 -> holds.first
                    1 -> holds.second
                    else -> SideAction.NONE
                }
                if (action == SideAction.NONE) onTap(side(position.x, always = false)) else onHold(action)
            }
        }
        // No double tap here: waiting to rule it out delayed every single-tap page turn.
        detectTapGestures(onLongPress = hold) { position -> onTap(side(position.x, always = false)) }
    }

/** The rectangle between two corners, in any drag direction. */
private fun maskBetween(page: Int, a: Offset, b: Offset) = Mask(
    id = "live",
    page = page,
    left = minOf(a.x, b.x).coerceIn(0f, 1f),
    top = minOf(a.y, b.y).coerceIn(0f, 1f),
    right = maxOf(a.x, b.x).coerceIn(0f, 1f),
    bottom = maxOf(a.y, b.y).coerceIn(0f, 1f),
)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawJumpMarker(jump: Jump, center: Offset, radius: Float) {
    drawCircle(Brass, radius, center)
    drawCircle(Color.White, radius, center, style = DrawScopeStroke(width = radius * 0.14f))
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.White.toArgb()
        textSize = radius * 1.05f
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
    }
    if (jump.isAudio) {
        val r = radius * 0.45f
        val play = Path().apply {
            moveTo(center.x - r * 0.7f, center.y - r)
            lineTo(center.x + r, center.y)
            lineTo(center.x - r * 0.7f, center.y + r)
            close()
        }
        drawPath(play, Color.White)
        return
    }
    drawContext.canvas.nativeCanvas.drawText("${jump.target + 1}", center.x, center.y - (paint.ascent() + paint.descent()) / 2, paint)
}

/** Step 2k: page k. Step 2k+1: top half of page k+1 above the bottom half of page k. */
@Composable
private fun HalfPageView(
    step: Int,
    modifier: Modifier,
    Page: @Composable (Int) -> Unit,
) {
    val k = step / 2
    if (step % 2 == 0) {
        Box(modifier) { Page(k) }
        return
    }
    BoxWithConstraints(modifier) {
        val full = maxHeight
        Column(Modifier.fillMaxSize()) {
            listOf(k + 1 to Alignment.Top, k to Alignment.Bottom).forEach { (index, align) ->
                Box(Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
                    Box(Modifier.fillMaxWidth().wrapContentHeight(align, unbounded = true).height(full)) { Page(index) }
                }
                if (align == Alignment.Top) Box(Modifier.fillMaxWidth().height(2.dp).background(Brass.copy(alpha = 0.6f)))
            }
        }
    }
}

@Composable
private fun ModeMenuItem(label: String, checked: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        trailingIcon = { if (checked) Icon(Icons.Rounded.Check, contentDescription = null) },
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStroke(
    stroke: Stroke,
    width: Float,
    height: Float,
) {
    if (stroke.text.isNotEmpty()) {
        com.vlcsheets.app.media.drawTextAnnotation(drawContext.canvas.nativeCanvas, stroke, width, height)
        return
    }
    if (stroke.points.size < 2) return
    val baseWidth = stroke.width * (width / 1000f) * 1.25f
    val path = Path()
    stroke.points.forEachIndexed { index, point ->
        val x = point.x * width
        val y = point.y * height
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    val color = Color(stroke.color.toInt()).copy(alpha = if (stroke.highlighter) 0.32f else 1f)
    drawPath(
        path = path,
        color = color,
        style = DrawScopeStroke(
            width = if (stroke.highlighter) baseWidth * 2.6f else baseWidth,
            cap = StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round,
        ),
    )
}

private fun eraseStrokeAt(song: Song, page: Int, normalized: Offset, vm: MainViewModel) {
    val threshold = 0.035f
    var best: Stroke? = null
    var bestDistance = Float.MAX_VALUE
    for (stroke in song.annotations.filter { it.page == page }) {
        for (i in 0 until stroke.points.size - 1) {
            val a = stroke.points[i]
            val b = stroke.points[i + 1]
            val distance = distanceToSegment(
                normalized.x,
                normalized.y,
                a.x,
                a.y,
                b.x,
                b.y,
            )
            if (distance < bestDistance) {
                bestDistance = distance
                best = stroke
            }
        }
    }
    if (best != null && bestDistance < threshold) {
        vm.removeStroke(song.id, best.id)
    }
}

private fun distanceToSegment(
    px: Float,
    py: Float,
    ax: Float,
    ay: Float,
    bx: Float,
    by: Float,
): Float {
    val dx = bx - ax
    val dy = by - ay
    val lengthSquared = dx * dx + dy * dy
    if (lengthSquared == 0f) return hypot(px - ax, py - ay)
    var t = ((px - ax) * dx + (py - ay) * dy) / lengthSquared
    t = t.coerceIn(0f, 1f)
    val cx = ax + t * dx
    val cy = ay + t * dy
    return sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy))
}

@Composable
private fun AnnotationToolbar(
    penStyle: PenStyle,
    onStyleChange: (PenStyle) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color(0xF20B0D13),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DrawTool.entries.forEach { tool ->
                    FilterChip(
                        selected = penStyle.tool == tool,
                        onClick = { onStyleChange(penStyle.copy(tool = tool)) },
                        label = {
                            Text(
                                when (tool) {
                                    DrawTool.PEN -> stringResource(R.string.tool_pen)
                                    DrawTool.HIGHLIGHTER -> stringResource(R.string.tool_highlighter)
                                    DrawTool.ERASER -> stringResource(R.string.tool_eraser)
                                },
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = Color.White.copy(alpha = 0.85f),
                            selectedContainerColor = Brass.copy(alpha = 0.25f),
                            selectedLabelColor = Brass,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = penStyle.tool == tool,
                            borderColor = Color.White.copy(alpha = 0.3f),
                            selectedBorderColor = Brass,
                            selectedBorderWidth = 1.dp,
                        ),
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onUndo) {
                    Icon(Icons.Rounded.Undo, contentDescription = stringResource(R.string.action_undo), tint = Color.White)
                }
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.action_clear),
                        tint = Color.White,
                    )
                }
                IconButton(onClick = onDone) {
                    Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.action_done), tint = Brass)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                inkColors.forEach { color ->
                    val selected = penStyle.color == color
                    Box(
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(color.toInt()))
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) Color.White else Color.White.copy(alpha = 0.3f),
                                shape = CircleShape,
                            )
                            .pointerInput(color) {
                                detectTapGestures {
                                    onStyleChange(
                                        penStyle.copy(
                                            color = color,
                                            tool = if (penStyle.tool == DrawTool.ERASER) DrawTool.PEN else penStyle.tool,
                                        ),
                                    )
                                }
                            },
                    )
                }
                Spacer(Modifier.weight(1f))
                listOf(1f, 2f, 4f).forEach { width ->
                    val selected = penStyle.width == width
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (selected) Color.White.copy(alpha = 0.2f) else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size((width * 3 + 6).dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .pointerInput(width) {
                                    detectTapGestures { onStyleChange(penStyle.copy(width = width)) }
                                },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarksSheet(
    song: Song,
    currentPage: Int,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onDelete: (com.vlcsheets.app.data.Bookmark) -> Unit,
    onJump: (Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 28.dp)) {
            Text(stringResource(R.string.bookmarks), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.bookmarks_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            val addLabel = stringResource(R.string.add_bookmark, currentPage + 1)
            val currentPageLabel = stringResource(R.string.page_label, currentPage + 1)
            Button(
                onClick = { onAdd(currentPageLabel) },
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Rounded.BookmarkAdd, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(addLabel)
            }
            Spacer(Modifier.height(8.dp))
            if (song.bookmarks.isEmpty()) {
                Text(
                    stringResource(R.string.no_bookmarks),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 18.dp),
                )
            } else {
                song.bookmarks.sortedBy { it.page }.forEach { bookmark ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .pointerInput(bookmark.id) {
                                detectTapGestures { onJump(bookmark.page) }
                            }
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Bookmark, contentDescription = null, tint = Brass)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            val fallbackLabel = stringResource(R.string.page_label, bookmark.page + 1)
                            Text(
                                bookmark.label.ifBlank { fallbackLabel },
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                fallbackLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onDelete(bookmark) }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetronomeSheet(
    vm: MainViewModel,
    song: Song,
    halfPage: Boolean,
    onDismiss: () -> Unit,
) {
    val state by vm.metronomeState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.metronome),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(
                    onClick = { vm.configureMetronome(state.bpm - 1, state.beatsPerBar, state.accentFirst) },
                    modifier = Modifier.size(44.dp),
                ) { Icon(Icons.Rounded.Remove, contentDescription = null) }

                Text(
                    text = state.bpm.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )

                FilledIconButton(
                    onClick = { vm.configureMetronome(state.bpm + 1, state.beatsPerBar, state.accentFirst) },
                    modifier = Modifier.size(44.dp),
                ) { Icon(Icons.Rounded.Add, contentDescription = null) }
            }
            Text(
                stringResource(R.string.bpm),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.speed < 1f) {
                Text(
                    stringResource(R.string.practice_tempo, (state.speed * 100).roundToInt(), effectiveBpm(state.bpm, state.speed)),
                    style = MaterialTheme.typography.labelMedium,
                    color = Brass,
                )
            }
            if (state.bpm != song.tempo || state.beatsPerBar != song.beatsPerBar) {
                TextButton(onClick = { vm.editSong(song.id) { it.copy(tempo = state.bpm, beatsPerBar = state.beatsPerBar) } }) {
                    Text(stringResource(R.string.save_tempo))
                }
            }

            Spacer(Modifier.height(10.dp))
            Slider(
                value = state.bpm.toFloat(),
                onValueChange = { vm.configureMetronome(it.roundToInt(), state.beatsPerBar, state.accentFirst) },
                valueRange = 30f..260f,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.beats_per_bar),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(2, 3, 4, 6).forEach { beats ->
                    FilterChip(
                        selected = state.beatsPerBar == beats,
                        onClick = { vm.configureMetronome(state.bpm, beats, state.accentFirst) },
                        label = { Text(beats.toString()) },
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.accent_first_beat),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                )
                Switch(
                    checked = state.accentFirst,
                    onCheckedChange = { vm.configureMetronome(state.bpm, state.beatsPerBar, it) },
                )
            }

            // Page turning for scores without audio; saved with the score.
            Spacer(Modifier.height(10.dp))
            Text(
                // In half-page mode each metronome turn moves half a page, like a side tap.
                stringResource(if (halfPage) R.string.metronome_turn_half_pages else R.string.metronome_turn_pages),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                FilledIconButton(
                    onClick = { vm.editSong(song.id) { it.copy(barsPerPage = (it.barsPerPage - 1).coerceAtLeast(0)) } },
                    enabled = song.barsPerPage > 0,
                    modifier = Modifier.size(40.dp),
                ) { Icon(Icons.Rounded.Remove, contentDescription = null) }
                Text(
                    if (song.barsPerPage > 0) pluralStringResource(R.plurals.bars, song.barsPerPage, song.barsPerPage)
                    else stringResource(R.string.metronome_turn_off),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                )
                FilledIconButton(
                    onClick = { vm.editSong(song.id) { it.copy(barsPerPage = (it.barsPerPage + 1).coerceAtMost(64)) } },
                    modifier = Modifier.size(40.dp),
                ) { Icon(Icons.Rounded.Add, contentDescription = null) }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.count_in),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(0, 1, 2).forEach { bars ->
                    FilterChip(
                        selected = song.countInBars == bars,
                        onClick = { vm.editSong(song.id) { it.copy(countInBars = bars) } },
                        label = { Text(bars.toString()) },
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(state.beatsPerBar) { index ->
                    val active = state.running && state.currentBeat == index
                    val dot by androidx.compose.animation.core.animateDpAsState(if (active) 18.dp else 12.dp, label = "beat")
                    Box(
                        modifier = Modifier
                            .size(dot)
                            .clip(CircleShape)
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                            ),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { vm.toggleMetronome() },
                modifier = Modifier.pressScale().fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(
                    if (state.running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(if (state.running) R.string.stop else R.string.start),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioSheet(
    player: AudioPlayer,
    files: List<java.io.File>,
    currentPage: Int,
    marks: List<AudioMark>,
    onAddMark: (Int) -> Unit,
    onRemoveMark: (AudioMark) -> Unit,
    loopStart: Int?,
    loopEnd: Int?,
    onLoopChange: (Int?, Int?) -> Unit,
    onAdd: (android.net.Uri) -> Unit,
    onRemove: (java.io.File) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val errorText = stringResource(R.string.audio_error)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onAdd)
    }
    var confirmRemove by remember { mutableStateOf<java.io.File?>(null) }
    var position by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(player.file) {
        while (true) {
            if (!dragging) position = player.positionMs.toFloat()
            delay(250)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
        ) {
            Text(stringResource(R.string.audio), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            if (files.isEmpty()) {
                Text(
                    stringResource(R.string.audio_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            files.forEach { file ->
                val selected = player.file == file
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) Brass.copy(alpha = 0.15f) else Color.Transparent)
                        .pointerInput(file) {
                            detectTapGestures {
                                if (player.file != file && !player.load(file)) {
                                    Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .padding(start = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = if (selected) Brass else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        file.nameWithoutExtension,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { confirmRemove = file }) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (player.file != null) {
                val duration = player.durationMs.toFloat().coerceAtLeast(1f)
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = position.coerceIn(0f, duration),
                    onValueChange = {
                        dragging = true
                        position = it
                    },
                    onValueChangeFinished = {
                        player.seekTo(position.toInt())
                        dragging = false
                    },
                    valueRange = 0f..duration,
                )
                Row {
                    Text(formatTime(position), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.weight(1f))
                    Text(formatTime(duration), style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { player.toggle() },
                    modifier = Modifier.pressScale().fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(if (player.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(if (player.playing) R.string.stop else R.string.start))
                }

                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.practice_speed), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    practiceSpeeds.forEach { s ->
                        FilterChip(
                            selected = player.speed == s,
                            onClick = { player.changeSpeed(s) },
                            label = { Text("${(s * 100).roundToInt()}") },
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.loop), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { onLoopChange(player.positionMs, loopEnd?.takeIf { it > player.positionMs }) }) {
                        Text(stringResource(R.string.loop_start) + (loopStart?.let { "  " + formatTime(it.toFloat()) } ?: ""))
                    }
                    OutlinedButton(onClick = {
                        val pos = player.positionMs
                        onLoopChange(loopStart?.takeIf { it < pos } ?: 0, pos)
                    }) {
                        Text(stringResource(R.string.loop_end) + (loopEnd?.let { "  " + formatTime(it.toFloat()) } ?: ""))
                    }
                    if (loopStart != null || loopEnd != null) {
                        IconButton(onClick = { onLoopChange(null, null) }) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.loop_clear))
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.audio_marks), style = MaterialTheme.typography.labelLarge)
                marks.forEach { mark ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .pointerInput(mark) { detectTapGestures { player.seekTo(mark.ms) } }
                            .padding(start = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.audio_mark_item, formatTime(mark.ms.toFloat()), mark.page + 1),
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onRemoveMark(mark) }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                TextButton(onClick = { onAddMark(player.positionMs) }) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.audio_mark_add, currentPage + 1))
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { picker.launch(arrayOf("audio/*")) }) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.audio_add))
            }
        }
    }

    confirmRemove?.let { file ->
        ConfirmDialog(
            title = stringResource(R.string.audio_remove_title),
            text = file.name,
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = { onRemove(file) },
            onDismiss = { confirmRemove = null },
        )
    }
}

private fun formatTime(ms: Float): String {
    val seconds = (ms / 1000).toInt()
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

internal fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}



