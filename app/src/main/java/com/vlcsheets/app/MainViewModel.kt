package com.vlcsheets.app

import android.app.Application
import android.net.Uri
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vlcsheets.app.data.AppSettings
import com.vlcsheets.app.data.Bookmark
import com.vlcsheets.app.data.LibraryData
import com.vlcsheets.app.data.LibraryRepository
import com.vlcsheets.app.data.Setlist
import com.vlcsheets.app.data.SettingsRepository
import com.vlcsheets.app.data.Song
import com.vlcsheets.app.data.Stroke
import com.vlcsheets.app.data.ThemeMode
import com.vlcsheets.app.media.MetronomeEngine
import com.vlcsheets.app.media.MetronomeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Normalizer

enum class SortOrder { TITLE, COMPOSER, YEAR, RECENT, ADDED }

/** A snackbar text; with [onUndo] it gets an "Undo" button. */
data class UiMessage(
    val resId: Int,
    val args: List<Any> = emptyList(),
    val quantity: Int? = null,
    val onUndo: (() -> Unit)? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VlcSheetsApp
    val repository: LibraryRepository = app.repository
    private val settingsRepo: SettingsRepository = app.settings

    val library: StateFlow<LibraryData> = repository.data
    val importProgress = repository.importProgress
    val settings: StateFlow<AppSettings> = settingsRepo.settings

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedCollection = MutableStateFlow<String?>(null)
    val selectedCollection: StateFlow<String?> = _selectedCollection.asStateFlow()

    private val _favoritesOnly = MutableStateFlow(false)
    val favoritesOnly: StateFlow<Boolean> = _favoritesOnly.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.TITLE)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _openSongId = MutableStateFlow<String?>(null)
    val openSongId: StateFlow<String?> = _openSongId.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    val metronome = MetronomeEngine()
    val metronomeState: StateFlow<MetronomeState> = metronome.state

    val filteredSongs: StateFlow<List<Song>> = combine(
        repository.data,
        _query,
        _selectedCollection,
        _favoritesOnly,
        _sortOrder,
    ) { data, query, collection, favOnly, sort ->
        var list = data.songs
        if (favOnly) list = list.filter { it.favorite }
        collection?.let { c -> list = list.filter { c in it.collections } }
        if (query.isNotBlank()) {
            val needle = normalize(query)
            list = list.filter { s ->
                normalize(s.title).contains(needle) ||
                    normalize(s.composer).contains(needle) ||
                    normalize(s.genre).contains(needle) ||
                    normalize(s.key).contains(needle) ||
                    normalize(s.notes).contains(needle) ||
                    s.tags.any { normalize(it).contains(needle) } ||
                    s.collections.any { normalize(it).contains(needle) }
            }
        }
        when (sort) {
            SortOrder.TITLE -> list.sortedWith(compareBy({ normalize(it.title) }, { normalize(it.composer) }))
            SortOrder.COMPOSER -> list.sortedWith(
                compareBy({ normalize(it.composer).ifBlank { "\uFFFF" } }, { normalize(it.title) }),
            )
            // Scores without a year go last.
            SortOrder.YEAR -> list.sortedWith(
                compareBy<Song>({ if (it.year > 0) it.year else Int.MAX_VALUE }, { normalize(it.title) }),
            )
            SortOrder.RECENT -> list.sortedWith(
                compareByDescending<Song> { it.lastOpenedAt }.thenBy { normalize(it.title) },
            )
            SortOrder.ADDED -> list.sortedByDescending { it.createdAt }
        }
    }.flowOn(Dispatchers.Default) // sorting a big library with accent folding is too slow for the UI thread
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { repository.load() }
    }

    // -----------------------------------------------------------------------
    // Library filters
    // -----------------------------------------------------------------------

    fun setQuery(value: String) {
        _query.value = value
    }

    fun selectCollection(name: String?) {
        _selectedCollection.value = name
    }

    fun setFavoritesOnly(value: Boolean) {
        _favoritesOnly.value = value
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    // -----------------------------------------------------------------------
    // Import / export
    // -----------------------------------------------------------------------

    fun importUris(uris: List<Uri>, collection: String? = null) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val result = repository.importUris(uris, collection)
            when {
                result.imported == 0 -> message(R.string.msg_import_failed)
                result.failed > 0 -> pluralMessage(R.plurals.msg_import_partial, result.imported, result.failed)
                else -> pluralMessage(R.plurals.msg_import_ok, result.imported)
            }
        }
    }

    fun importAndOpen(uri: Uri) {
        viewModelScope.launch {
            val result = repository.importUris(listOf(uri))
            result.songs.firstOrNull()?.let { _openSongId.value = it.id }
                ?: message(R.string.msg_import_failed)
        }
    }

    /** One song, or a whole setlist with its songs, as a VLC Sheets file. */
    fun exportShare(uri: Uri, songIds: List<String>, setlist: Setlist? = null) {
        viewModelScope.launch {
            _busy.value = true
            runCatching { repository.exportShare(uri, songIds, setlist) }
                .onSuccess { message(R.string.msg_share_ok) }
                .onFailure { message(R.string.msg_share_error) }
            _busy.value = false
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _busy.value = true
            runCatching { repository.exportTo(uri) }
                .onSuccess { message(R.string.msg_backup_ok) }
                .onFailure { message(R.string.msg_backup_error) }
            _busy.value = false
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            _busy.value = true
            repository.importFrom(uri)
                .onSuccess { message(R.string.msg_restore_ok) }
                .onFailure { message(R.string.msg_restore_error) }
            _busy.value = false
        }
    }

    fun clearLibrary() {
        viewModelScope.launch {
            _busy.value = true
            runCatching { repository.clearLibrary() }
                .onSuccess { message(R.string.msg_library_cleared) }
                .onFailure { message(R.string.msg_error_generic) }
            _busy.value = false
        }
    }

    // -----------------------------------------------------------------------
    // Songs
    // -----------------------------------------------------------------------

    fun saveSong(song: Song) = viewModelScope.launch { repository.updateSong(song) }

    fun toggleFavorite(song: Song) = viewModelScope.launch {
        repository.setFavorite(song.id, !song.favorite)
    }

    fun deleteSongs(ids: Set<String>) = viewModelScope.launch {
        repository.deleteSongs(ids)
        pluralMessage(R.plurals.msg_songs_deleted, ids.size)
    }

    fun markOpened(songId: String) = viewModelScope.launch { repository.markOpened(songId) }

    fun saveLastPage(songId: String, page: Int) = viewModelScope.launch {
        repository.saveLastPage(songId, page)
    }

    fun addStroke(songId: String, stroke: Stroke) = viewModelScope.launch {
        record(AnnotationChange(songId, setOf(stroke.id), emptyList()))
        repository.addStroke(songId, stroke)
    }

    fun clearPage(songId: String, page: Int) = viewModelScope.launch {
        val song = library.value.songs.firstOrNull { it.id == songId } ?: return@launch
        record(AnnotationChange(songId, emptySet(), song.annotations.filter { it.page == page }))
        repository.clearAnnotations(songId, page)
    }

    fun exportPdf(song: Song, uri: Uri) = viewModelScope.launch {
        runCatching {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val out = app.contentResolver.openOutputStream(uri) ?: error("Cannot open destination")
                out.use { com.vlcsheets.app.media.PdfExporter.export(app, song, it) }
            }
        }.onSuccess { message(R.string.msg_export_ok) }
            .onFailure { message(R.string.msg_export_error) }
    }

    fun audioFiles(songId: String): List<java.io.File> = repository.audioFiles(songId)

    fun addAudio(songId: String, uri: Uri, onDone: () -> Unit) = viewModelScope.launch {
        repository.addAudio(songId, uri).onFailure { message(R.string.msg_error_generic) }
        onDone()
    }

    fun removeAudio(songId: String, file: java.io.File, onDone: () -> Unit) = viewModelScope.launch {
        repository.removeAudio(file)
        repository.editSong(songId) { s ->
            s.copy(audioMarks = s.audioMarks.filterNot { it.file == file.name }, jumps = s.jumps.filterNot { it.audioFile == file.name })
        }
        onDone()
    }

    fun editSong(songId: String, block: (Song) -> Song) = viewModelScope.launch { repository.editSong(songId, block) }

    /**
     * Turns the handwriting picked by [pick] into text; what is not recognized stays as it is.
     * [auto] (after a pause while writing) only speaks up when something was converted.
     */
    fun convertHandwriting(songId: String, auto: Boolean = false, pick: (Stroke) -> Boolean) = viewModelScope.launch {
        val samples = library.value.ink
        val song = library.value.songs.firstOrNull { it.id == songId } ?: return@launch
        if (samples.isEmpty()) {
            if (!auto) message(R.string.handwriting_no_samples)
            return@launch
        }
        val (removeIds, added) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            com.vlcsheets.app.data.Handwriting.convert(song.annotations.filter(pick), samples, song.pageAspect)
        }
        if (added.isEmpty()) {
            if (!auto) message(R.string.handwriting_none)
            return@launch
        }
        // Every text on a page gets one size, the new ones and those converted before.
        val height = com.vlcsheets.app.data.Handwriting.TEXT_SIZES.getOrElse(settings.value.handwritingSize) { 0f }
        val oldTexts = song.annotations.filter { it.text.isNotEmpty() && it.page in added.map { a -> a.page } }
        val sized = (oldTexts + added).groupBy { it.page }.values
            .flatMap { com.vlcsheets.app.data.Handwriting.sameSize(it, height) }
        val handwriting = song.annotations.filter { it.id in removeIds }
        repository.replaceStrokes(song.id, removeIds + oldTexts.map { it.id }, sized)
        val undo = AnnotationChange(song.id, sized.map { it.id }.toSet(), handwriting + oldTexts)
        record(undo)
        _messages.trySend(
            UiMessage(R.plurals.handwriting_converted, listOf(added.size), quantity = added.size, onUndo = { undoChange(undo) }),
        )
    }

    /** One annotation edit: the strokes it added and the ones it removed, so undo can swap them back. */
    private data class AnnotationChange(val songId: String, val added: Set<String>, val removed: List<Stroke>)

    private val annotationHistory = ArrayDeque<AnnotationChange>()

    private fun record(change: AnnotationChange) {
        annotationHistory.addLast(change)
        if (annotationHistory.size > 200) annotationHistory.removeFirst()
    }

    private fun undoChange(c: AnnotationChange) {
        annotationHistory.remove(c)
        viewModelScope.launch { repository.replaceStrokes(c.songId, c.added, c.removed) }
    }

    /** The annotation toolbar's undo: the last drawing, erasing, clearing or conversion on this song. */
    fun undoAnnotation(songId: String) {
        annotationHistory.lastOrNull { it.songId == songId }?.let(::undoChange)
    }

    fun addInkSample(sample: com.vlcsheets.app.data.InkSample) = viewModelScope.launch {
        repository.mutateInk { it + sample }
    }

    fun removeInkSample(sample: com.vlcsheets.app.data.InkSample) = viewModelScope.launch {
        repository.mutateInk { it - sample }
    }

    fun forgetInkSamples(symbol: String) = viewModelScope.launch {
        repository.mutateInk { ink -> ink.filterNot { it.symbol == symbol } }
    }

    fun removeStroke(songId: String, strokeId: String) = viewModelScope.launch {
        val stroke = library.value.songs.firstOrNull { it.id == songId }?.annotations?.firstOrNull { it.id == strokeId }
        if (stroke != null) record(AnnotationChange(songId, emptySet(), listOf(stroke)))
        repository.removeStroke(songId, strokeId)
    }

    fun addBookmark(songId: String, page: Int, label: String) = viewModelScope.launch {
        repository.addBookmark(songId, page, label)
    }

    fun removeBookmark(songId: String, bookmark: Bookmark) = viewModelScope.launch {
        repository.removeBookmark(songId, bookmark.id)
    }

    // -----------------------------------------------------------------------
    // Setlists
    // -----------------------------------------------------------------------

    fun createSetlist(name: String, onCreated: (Setlist) -> Unit = {}) {
        viewModelScope.launch {
            val setlist = repository.createSetlist(name.trim())
            message(R.string.msg_setlist_created)
            onCreated(setlist)
        }
    }

    fun renameSetlist(setlist: Setlist, name: String) = viewModelScope.launch {
        repository.updateSetlist(setlist.copy(name = name.trim()))
    }

    fun setSetlistNote(setlist: Setlist, songId: String, note: String) = viewModelScope.launch {
        val notes = if (note.isBlank()) setlist.songNotes - songId else setlist.songNotes + (songId to note.trim())
        repository.updateSetlist(setlist.copy(songNotes = notes))
    }

    fun deleteSetlist(setlist: Setlist) = viewModelScope.launch {
        repository.deleteSetlist(setlist.id)
        message(R.string.msg_setlist_deleted)
    }

    fun addSongsToSetlist(setlist: Setlist, songIds: List<String>) = viewModelScope.launch {
        val merged = (setlist.songIds + songIds).distinct()
        repository.updateSetlist(setlist.copy(songIds = merged))
        pluralMessage(R.plurals.msg_added_to_setlist, songIds.size)
    }

    fun removeSongFromSetlist(setlist: Setlist, songId: String) = viewModelScope.launch {
        repository.updateSetlist(setlist.copy(songIds = setlist.songIds - songId))
    }

    fun moveSetlistSong(setlist: Setlist, from: Int, to: Int) = viewModelScope.launch {
        if (from !in setlist.songIds.indices || to !in setlist.songIds.indices) return@launch
        val ids = setlist.songIds.toMutableList()
        val item = ids.removeAt(from)
        ids.add(to, item)
        repository.updateSetlist(setlist.copy(songIds = ids))
    }

    // -----------------------------------------------------------------------
    // Collections
    // -----------------------------------------------------------------------

    fun createCollection(name: String) = viewModelScope.launch {
        val created = repository.createCollection(name)
        if (created == null) message(R.string.msg_collection_exists) else message(R.string.msg_collection_created)
    }

    fun renameCollection(oldName: String, newName: String) = viewModelScope.launch {
        if (newName.isBlank() || oldName == newName) return@launch
        repository.renameCollection(oldName, newName)
        if (_selectedCollection.value == oldName) _selectedCollection.value = newName
    }

    fun deleteCollection(name: String) = viewModelScope.launch {
        repository.deleteCollection(name)
        if (_selectedCollection.value == name) _selectedCollection.value = null
        message(R.string.msg_collection_deleted)
    }

    // -----------------------------------------------------------------------
    // Settings & metronome
    // -----------------------------------------------------------------------

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settingsRepo.update(transform)
    }

    fun setThemeMode(mode: ThemeMode) = updateSettings { it.copy(themeMode = mode) }

    fun configureMetronome(bpm: Int, beats: Int, accent: Boolean) =
        metronome.configure(bpm, beats, accent)

    fun setPracticeSpeed(speed: Float) = metronome.setSpeed(speed)

    fun toggleMetronome() {
        if (metronome.state.value.running) metronome.stop() else metronome.start()
    }

    fun consumeOpenSong() {
        _openSongId.value = null
    }

    // The count picks the plural form and is also the first value in the text.
    private fun pluralMessage(@PluralsRes resId: Int, count: Int, vararg more: Any) {
        _messages.trySend(UiMessage(resId, listOf(count, *more), quantity = count))
    }

    fun message(@StringRes resId: Int, vararg args: Any) {
        _messages.trySend(UiMessage(resId, args.toList()))
    }

    override fun onCleared() {
        metronome.release()
        super.onCleared()
    }

    companion object {
        fun factory(app: VlcSheetsApp) = viewModelFactory {
            initializer { MainViewModel(app) }
        }

        internal fun normalize(text: String): String {
            val decomposed = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            return decomposed.replace(Regex("\\p{Mn}+"), "")
        }
    }
}
