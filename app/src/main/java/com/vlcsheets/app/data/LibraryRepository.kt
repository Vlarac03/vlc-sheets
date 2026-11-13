package com.vlcsheets.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.vlcsheets.app.media.ChordProPdf
import com.vlcsheets.app.media.ImageTools
import com.vlcsheets.app.media.PdfTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.util.UUID

/**
 * Single source of truth for the score library. The whole index is a small JSON
 * document stored inside the app's private folder, next to the imported files,
 * so backup is just a zip of that folder.
 */
class LibraryRepository(private val context: Context) {

    private val mutex = Mutex()

    private val root: File = File(context.filesDir, "library")
    private val songsDir: File = File(root, "songs")
    private val indexFile: File = File(root, "index.json")
    // Inside the library folder, so backups carry it too.
    private val inkFile: File = File(root, "handwriting.json")

    private val _data = MutableStateFlow(LibraryData())
    val data: StateFlow<LibraryData> = _data.asStateFlow()

    private val _importProgress = MutableStateFlow<ImportProgress?>(null)
    val importProgress: StateFlow<ImportProgress?> = _importProgress.asStateFlow()

    init {
        // A restore that died halfway leaves the old library in library.old: put it back.
        val trash = File(context.filesDir, "library.old")
        if (!root.exists() && trash.exists()) trash.renameTo(root)
        root.mkdirs()
        songsDir.mkdirs()
    }

    // -----------------------------------------------------------------------
    // Loading & persistence
    // -----------------------------------------------------------------------

    suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock { loadLocked() }
    }

    private fun loadLocked() {
        com.vlcsheets.app.media.ThumbnailCache.clear()
        val parsed = runCatching {
            if (!indexFile.exists()) return@runCatching LibraryData(loaded = true)
            val json = JSONObject(indexFile.readText(Charsets.UTF_8))
            val songs = json.optJSONArray("songs")?.let { arr ->
                buildList {
                    for (i in 0 until arr.length()) {
                        // A hand-made index must not point outside songs/.
                        arr.optJSONObject(i)?.let(::songFromJson)?.takeIf(Song::hasSafeNames)?.let { add(it) }
                    }
                }
            } ?: emptyList()
            val setlists = json.optJSONArray("setlists")?.let { arr ->
                buildList {
                    for (i in 0 until arr.length()) {
                        arr.optJSONObject(i)?.let { add(setlistFromJson(it)) }
                    }
                }
            } ?: emptyList()
            val collections = json.optJSONArray("collections")?.let { arr ->
                buildList {
                    for (i in 0 until arr.length()) {
                        arr.optJSONObject(i)?.let { add(collectionFromJson(it)) }
                    }
                }
            } ?: emptyList()
            val songsWithStrokes = songs.map { song ->
                val file = annotationsFile(song.id)
                when {
                    file.exists() -> song.copy(annotations = strokesFromJson(JSONArray(file.readText(Charsets.UTF_8))))
                    // Old format: strokes were inside index.json. Move them to their own file.
                    song.annotations.isNotEmpty() -> song.also { writeAnnotationsLocked(it) }
                    else -> song
                }
            }
            val known = collections.map { it.name }.toMutableSet()
            val migrated = songs.flatMap { it.collections }
                .distinct()
                .filterNot { it in known }
                .map { Collection(id = UUID.randomUUID().toString(), name = it) }
            LibraryData(
                songs = songsWithStrokes,
                setlists = setlists,
                collections = collections + migrated,
                loaded = true,
            )
        }.getOrElse { LibraryData(loaded = true) }
        val ink = runCatching { inkSamplesFromJson(JSONArray(inkFile.readText(Charsets.UTF_8))) }.getOrDefault(emptyList())
        _data.value = parsed.copy(ink = ink)
    }

    private fun persistLocked(data: LibraryData) {
        val json = JSONObject().apply {
            put("version", 1)
            put("songs", JSONArray().apply { data.songs.forEach { put(it.toJson()) } })
            put("setlists", JSONArray().apply { data.setlists.forEach { put(it.toJson()) } })
            put("collections", JSONArray().apply { data.collections.forEach { put(it.toJson()) } })
        }
        root.mkdirs()
        if (indexFile.exists()) {
            runCatching { indexFile.copyTo(File(root, "index.json.bak"), overwrite = true) }
        }
        writeAtomic(indexFile, json.toString())
    }

    private fun writeAtomic(file: File, text: String) {
        val tmp = File(file.parentFile, "${file.name}.tmp")
        FileOutputStream(tmp).use { out ->
            out.write(text.toByteArray(Charsets.UTF_8))
            out.fd.sync()
        }
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
    }

    private fun annotationsFile(songId: String) = File(File(songsDir, songId), "annotations.json")

    private fun writeAnnotationsLocked(song: Song) {
        val file = annotationsFile(song.id)
        if (song.annotations.isEmpty()) {
            file.delete()
        } else {
            file.parentFile?.mkdirs()
            writeAtomic(file, strokesToJson(song.annotations).toString())
        }
    }

    /** Changes one song's strokes and writes only its annotations file, not the whole index. */
    private suspend fun mutateStrokes(songId: String, block: (List<Stroke>) -> List<Stroke>) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val song = _data.value.songs.firstOrNull { it.id == songId } ?: return@withLock
                val updated = song.copy(annotations = block(song.annotations))
                _data.value = _data.value.copy(songs = _data.value.songs.map { if (it.id == songId) updated else it })
                writeAnnotationsLocked(updated)
            }
        }

    private suspend fun mutate(block: (LibraryData) -> LibraryData) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val next = block(_data.value)
            _data.value = next
            persistLocked(next)
        }
    }

    // -----------------------------------------------------------------------
    // Files
    // -----------------------------------------------------------------------

    fun songFolder(songId: String): File {
        require(isSafeName(songId)) { "song id" }
        return File(songsDir, songId)
    }

    fun songFile(song: Song): File {
        require(isSafeName(song.fileName)) { "file name" }
        return File(songFolder(song.id), song.fileName)
    }

    /** Audio tracks linked to a song (songs/<id>/audio), sorted by name. */
    fun audioFiles(songId: String): List<File> =
        File(songFolder(songId), "audio").listFiles()?.filter { it.isFile }?.sortedBy { it.name.lowercase() }
            ?: emptyList()

    suspend fun addAudio(songId: String, uri: Uri): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val name = (queryDisplayName(uri) ?: "audio.mp3").replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val dir = File(songFolder(songId), "audio").apply { mkdirs() }
            var dest = File(dir, name)
            var n = 2
            while (dest.exists()) dest = File(dir, "${name.substringBeforeLast('.')} (${n++}).${name.substringAfterLast('.', "mp3")}")
            copyUri(uri, dest)
            dest
        }
    }

    suspend fun removeAudio(file: File) = withContext(Dispatchers.IO) {
        if (file.canonicalPath.startsWith(songsDir.canonicalPath + File.separator)) file.delete()
    }

    // -----------------------------------------------------------------------
    // Import
    // -----------------------------------------------------------------------

    suspend fun importUris(
        uris: List<Uri>,
        defaultCollection: String? = null,
    ): ImportResult = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext ImportResult(0, 0)
        val imported = mutableListOf<Song>()
        val importedSetlists = mutableListOf<Setlist>()
        var failed = 0
        val resolver = context.contentResolver

        uris.forEachIndexed { index, uri ->
            val displayName = queryDisplayName(uri) ?: "Partitura ${index + 1}"
            _importProgress.value = ImportProgress(index + 1, uris.size, displayName)
            val songs = runCatching {
                when {
                    displayName.endsWith(".msf", ignoreCase = true) -> importMsf(uri, defaultCollection)
                    displayName.endsWith(".$SHARE_EXTENSION", ignoreCase = true) ->
                        importShare(uri, defaultCollection, importedSetlists)
                    else -> listOfNotNull(importSingle(uri, displayName, defaultCollection))
                }
            }.onFailure { android.util.Log.w("VlcSheets", "Import failed: $displayName", it) }.getOrDefault(emptyList())
            if (songs.isNotEmpty()) imported += songs else failed += 1
        }

        if (imported.isNotEmpty()) {
            mutex.withLock {
                val current = _data.value
                val known = current.collections.map { it.name }.toMutableSet()
                val added = imported.flatMap { it.collections }
                    .filterNot { it in known }
                    .distinct()
                    .map { Collection(id = UUID.randomUUID().toString(), name = it) }
                val next = current.copy(
                    songs = current.songs + imported,
                    setlists = current.setlists + importedSetlists,
                    collections = current.collections + added,
                    loaded = true,
                )
                _data.value = next
                persistLocked(next)
            }
        }
        _importProgress.value = null
        ImportResult(imported = imported.size, failed = failed, songs = imported)
    }

    private suspend fun importSingle(
        uri: Uri,
        displayName: String,
        defaultCollection: String?,
    ): Song? {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri)
        val ext = displayName.substringAfterLast('.', "").lowercase()
        val isPdf = mime == "application/pdf" || ext == "pdf"
        val isImage = mime?.startsWith("image/") == true || ext in IMAGE_EXTENSIONS
        val isText = !isPdf && !isImage && (ext in TEXT_EXTENSIONS || mime?.startsWith("text/") == true)
        if (!isPdf && !isImage && !isText) return null

        val id = UUID.randomUUID().toString()
        val folder = File(songsDir, id)
        folder.mkdirs()
        val cleanTitle = displayName
            .substringBeforeLast('.')
            .replace('_', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { "Partitura" }

        return try {
            if (isText) {
                // Text and ChordPro become a PDF, so they are read and annotated like any score.
                val source = File(folder, "source.${ext.ifBlank { "txt" }}")
                copyUri(uri, source)
                val parsed = parseChordPro(source.readText(Charsets.UTF_8))
                val pdf = File(folder, "score.pdf")
                ChordProPdf.write(parsed, cleanTitle, pdf)
                return buildSong(id, folder, pdf, true, parsed.title.ifBlank { cleanTitle }, parsed.artist, defaultCollection)
            }
            val dest = File(folder, if (isPdf) "score.pdf" else "page.${ext.ifBlank { "jpg" }}")
            copyUri(uri, dest)
            buildSong(id, folder, dest, isPdf, cleanTitle, "", defaultCollection)
        } catch (t: Throwable) {
            android.util.Log.w("VlcSheets", "Import failed: $displayName", t)
            folder.deleteRecursively()
            null
        }
    }

    /** Builds the Song for a document already copied to [dest] (page count, aspect and thumbnail). */
    private fun buildSong(
        id: String,
        folder: File,
        dest: File,
        isPdf: Boolean,
        title: String,
        composer: String,
        defaultCollection: String?,
    ): Song {
        val now = System.currentTimeMillis()
        return if (isPdf) {
            val info = PdfTools.inspect(dest) ?: throw IllegalStateException("PDF")
            val thumb = PdfTools.renderFirstPage(dest, THUMB_WIDTH)
            val thumbName = thumb?.let { saveJpeg(it, File(folder, "thumb.jpg")) }
            Song(
                id = id,
                title = title,
                composer = composer,
                kind = SongKind.PDF,
                fileName = dest.name,
                pageCount = info.pageCount.coerceAtLeast(1),
                pageAspect = info.firstPageAspect,
                thumbFile = thumbName,
                collections = listOfNotNull(defaultCollection?.takeIf { it.isNotBlank() }),
                createdAt = now,
                updatedAt = now,
            )
        } else {
            val aspect = ImageTools.aspect(dest)
            val thumb = ImageTools.decode(dest, THUMB_WIDTH)
            val thumbName = thumb?.let { saveJpeg(it, File(folder, "thumb.jpg")) }
            Song(
                id = id,
                title = title,
                composer = composer,
                kind = SongKind.IMAGE,
                fileName = dest.name,
                pageCount = 1,
                pageAspect = aspect,
                thumbFile = thumbName,
                collections = listOfNotNull(defaultCollection?.takeIf { it.isNotBlank() }),
                createdAt = now,
                updatedAt = now,
            )
        }
    }

    /** Imports every song of a .msf file: document, annotations and audio files. */
    private fun importMsf(uri: Uri, defaultCollection: String?): List<Song> {
        val tmp = File(context.cacheDir, "import.msf")
        try {
            copyUri(uri, tmp)
            return FileInputStream(tmp).channel.use { ch ->
                val buf = ch.map(FileChannel.MapMode.READ_ONLY, 0, ch.size())
                MsfReader.read(buf).mapNotNull { runCatching { songFromMsf(ch, it, defaultCollection) }.getOrNull() }
            }
        } finally {
            tmp.delete()
        }
    }

    /**
     * Imports a VLC Sheets file (see [exportShare]). Every song gets a new id, so importing the
     * same file twice gives two copies instead of overwriting; a setlist inside goes to [setlists].
     */
    private fun importShare(uri: Uri, defaultCollection: String?, setlists: MutableList<Setlist>): List<Song> {
        val tmp = File(context.cacheDir, "import-share")
        val created = mutableListOf<File>()
        try {
            tmp.deleteRecursively()
            unzip(uri, tmp) { name -> name.takeIf { it == SHARE_INDEX || it.startsWith("songs/") } }
            val json = JSONObject(File(tmp, SHARE_INDEX).readText(Charsets.UTF_8))
            val now = System.currentTimeMillis()
            val newIds = mutableMapOf<String, String>()
            val arr = json.optJSONArray("songs") ?: JSONArray()
            val songs = List(arr.length()) { i ->
                val old = songFromJson(arr.getJSONObject(i))
                val src = File(File(tmp, "songs"), old.id)
                // Ids and file names come from outside: they must stay inside songs/ and the song's folder.
                require(old.hasSafeNames() && src.isDirectory) { "song folder" }
                val id = UUID.randomUUID().toString()
                val folder = File(songsDir, id)
                created += folder
                src.copyRecursively(folder)
                newIds[old.id] = id
                val strokes = File(folder, "annotations.json").takeIf { it.exists() }
                    ?.let { strokesFromJson(JSONArray(it.readText(Charsets.UTF_8))) } ?: old.annotations
                old.copy(
                    id = id,
                    annotations = strokes,
                    collections = (old.collections + listOfNotNull(defaultCollection?.takeIf { it.isNotBlank() })).distinct(),
                    createdAt = now,
                    updatedAt = now,
                    lastOpenedAt = 0L,
                    openCount = 0,
                )
            }
            json.optJSONObject("setlist")?.let(::setlistFromJson)?.let { s ->
                setlists += s.copy(
                    id = UUID.randomUUID().toString(),
                    songIds = s.songIds.mapNotNull(newIds::get),
                    songNotes = s.songNotes.mapNotNull { (k, v) -> newIds[k]?.let { it to v } }.toMap(),
                    createdAt = now,
                    updatedAt = now,
                )
            }
            return songs
        } catch (t: Throwable) {
            created.forEach { it.deleteRecursively() }
            throw t
        } finally {
            tmp.deleteRecursively()
        }
    }

    private fun songFromMsf(ch: FileChannel, msf: MsfReader.MsfSong, defaultCollection: String?): Song? {
        val ext = { f: MsfReader.MsfFile -> f.name.substringAfterLast('.').lowercase() }
        val doc = msf.files.firstOrNull { ext(it) == "pdf" }
            ?: msf.files.firstOrNull { ext(it) in IMAGE_EXTENSIONS }
            ?: return null
        val isPdf = ext(doc) == "pdf"
        val id = UUID.randomUUID().toString()
        val folder = File(songsDir, id)
        folder.mkdirs()
        try {
            val dest = File(folder, if (isPdf) "score.pdf" else "page.${ext(doc)}")
            copyRegion(ch, doc, dest)
            msf.files.filter { ext(it) in AUDIO_EXTENSIONS }.forEach { copyRegion(ch, it, File(folder, "audio/${it.name}")) }
            val title = msf.title.substringBeforeLast(" - ", msf.title).trim().ifBlank { "Partitura" }
            val composer = msf.title.substringAfterLast(" - ", "").trim()
            val base = buildSong(id, folder, dest, isPdf, title, composer, defaultCollection)
            val song = base.copy(
                annotations = msf.strokes.filter { it.page in 0 until base.pageCount }.map {
                    Stroke(UUID.randomUUID().toString(), it.page, it.points, it.color, it.width, it.highlighter)
                },
            )
            writeAnnotationsLocked(song) // the folder is new and not in the library yet, so no lock is needed
            return song
        } catch (t: Throwable) {
            folder.deleteRecursively()
            throw t
        }
    }

    private fun copyRegion(ch: FileChannel, file: MsfReader.MsfFile, dest: File) {
        dest.parentFile?.mkdirs()
        FileOutputStream(dest).channel.use { out ->
            var done = 0L
            while (done < file.size) {
                val n = ch.transferTo(file.offset + done, file.size - done, out)
                if (n <= 0) throw IllegalStateException("Short copy")
                done += n
            }
        }
    }

    private fun copyUri(uri: Uri, dest: File) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open $uri")
        input.use { source ->
            FileOutputStream(dest).use { out -> source.copyTo(out) }
        }
    }

    private fun saveJpeg(bitmap: android.graphics.Bitmap, file: File): String? = runCatching {
        FileOutputStream(file).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, out)
        }
        file.name
    }.getOrNull()

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
    }.getOrNull() ?: uri.takeIf { it.scheme == "file" }?.lastPathSegment

    // -----------------------------------------------------------------------
    // Songs
    // -----------------------------------------------------------------------

    suspend fun updateSong(song: Song) = mutate { data ->
        val known = data.collections.map { it.name }.toMutableSet()
        val newCollections = song.collections.filterNot { it in known }
            .map { Collection(id = UUID.randomUUID().toString(), name = it) }
        data.copy(
            songs = data.songs.map { if (it.id == song.id) song.copy(updatedAt = System.currentTimeMillis()) else it },
            collections = data.collections + newCollections,
        )
    }

    /** Changes one song starting from its latest saved version (so other edits are not lost). */
    suspend fun editSong(songId: String, block: (Song) -> Song) = mutate { data ->
        data.copy(songs = data.songs.map { if (it.id == songId) block(it) else it })
    }

    /** Changes several songs in one write. */
    suspend fun editSongs(ids: Set<String>, block: (Song) -> Song) = mutate { data ->
        data.copy(songs = data.songs.map { if (it.id in ids) block(it) else it })
    }

    suspend fun setFavorite(songId: String, favorite: Boolean) = mutate { data ->
        data.copy(songs = data.songs.map { if (it.id == songId) it.copy(favorite = favorite) else it })
    }

    suspend fun deleteSongs(ids: Set<String>) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val data = _data.value
                val next = data.copy(
                    songs = data.songs.filterNot { it.id in ids },
                    setlists = data.setlists.map { sl -> sl.copy(songIds = sl.songIds.filterNot { it in ids }, songNotes = sl.songNotes - ids) },
                )
                _data.value = next
                persistLocked(next)
                ids.filter(::isSafeName).forEach { runCatching { File(songsDir, it).deleteRecursively() } }
            }
        }
    }

    suspend fun markOpened(songId: String) = mutate { data ->
        data.copy(
            songs = data.songs.map {
                if (it.id == songId) it.copy(
                    lastOpenedAt = System.currentTimeMillis(),
                    openCount = it.openCount + 1,
                ) else it
            },
        )
    }

    suspend fun saveLastPage(songId: String, page: Int) {
        if (_data.value.songs.firstOrNull { it.id == songId }?.lastPage == page) return
        mutate { data ->
            data.copy(
                songs = data.songs.map {
                    if (it.id == songId) it.copy(lastPage = page.coerceAtLeast(0)) else it
                },
            )
        }
    }

    suspend fun renameCollection(oldName: String, newName: String) = mutate { data ->
        data.copy(
            songs = data.songs.map { song ->
                if (oldName in song.collections) {
                    song.copy(collections = song.collections.map { if (it == oldName) newName else it }.distinct())
                } else song
            },
            collections = data.collections.map { if (it.name == oldName) it.copy(name = newName) else it },
        )
    }

    suspend fun deleteCollection(name: String) = mutate { data ->
        data.copy(
            songs = data.songs.map { song ->
                if (name in song.collections) song.copy(collections = song.collections - name) else song
            },
            collections = data.collections.filterNot { it.name == name },
        )
    }

    suspend fun createCollection(name: String): Collection? {
        val clean = name.trim()
        if (clean.isBlank()) return null
        if (_data.value.collections.any { it.name.equals(clean, ignoreCase = true) }) return null
        val collection = Collection(id = UUID.randomUUID().toString(), name = clean)
        mutate { data -> data.copy(collections = data.collections + collection) }
        return collection
    }

    // -----------------------------------------------------------------------
    // Annotations & bookmarks
    // -----------------------------------------------------------------------

    suspend fun addStroke(songId: String, stroke: Stroke) = mutateStrokes(songId) { it + stroke }

    /** Swaps handwriting for its text in one write. */
    suspend fun replaceStrokes(songId: String, removeIds: Set<String>, add: List<Stroke>) =
        mutateStrokes(songId) { strokes -> strokes.filterNot { it.id in removeIds } + add }

    /** Changes the handwriting samples and writes only their file. */
    suspend fun mutateInk(block: (List<InkSample>) -> List<InkSample>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val ink = block(_data.value.ink)
            _data.value = _data.value.copy(ink = ink)
            writeAtomic(inkFile, inkSamplesToJson(ink).toString())
        }
    }

    suspend fun removeStroke(songId: String, strokeId: String) =
        mutateStrokes(songId) { strokes -> strokes.filterNot { it.id == strokeId } }

    suspend fun clearAnnotations(songId: String, page: Int) =
        mutateStrokes(songId) { strokes -> strokes.filterNot { it.page == page } }

    suspend fun addBookmark(songId: String, page: Int, label: String) = mutate { data ->
        data.copy(songs = data.songs.map { song ->
            if (song.id == songId) {
                song.copy(bookmarks = song.bookmarks + Bookmark(UUID.randomUUID().toString(), page, label))
            } else song
        })
    }

    suspend fun removeBookmark(songId: String, bookmarkId: String) = mutate { data ->
        data.copy(songs = data.songs.map { song ->
            if (song.id == songId) song.copy(bookmarks = song.bookmarks.filterNot { it.id == bookmarkId }) else song
        })
    }

    // -----------------------------------------------------------------------
    // Setlists
    // -----------------------------------------------------------------------

    suspend fun createSetlist(name: String, songIds: List<String> = emptyList()): Setlist {
        val setlist = Setlist(id = UUID.randomUUID().toString(), name = name, songIds = songIds)
        mutate { data -> data.copy(setlists = data.setlists + setlist) }
        return setlist
    }

    suspend fun updateSetlist(setlist: Setlist) = mutate { data ->
        data.copy(
            setlists = data.setlists.map {
                if (it.id == setlist.id) setlist.copy(updatedAt = System.currentTimeMillis()) else it
            },
        )
    }

    suspend fun deleteSetlist(id: String) = mutate { data ->
        data.copy(setlists = data.setlists.filterNot { it.id == id })
    }

    // -----------------------------------------------------------------------
    // Backup / restore / clear
    // -----------------------------------------------------------------------

    suspend fun exportTo(uri: Uri) = withContext(Dispatchers.IO) {
        mutex.withLock {
            persistLocked(_data.value)
            val output = context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Cannot open destination")
            output.use { out ->
                java.util.zip.ZipOutputStream(out.buffered()).use { zip ->
                    val base = root.toPath()
                    root.walkTopDown().filter { it.isFile }.forEach { file ->
                        val entryName = base.relativize(file.toPath()).toString().replace('\\', '/')
                        if (entryName.endsWith(".tmp")) return@forEach
                        zip.putNextEntry(java.util.zip.ZipEntry("library/$entryName"))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
        }
    }

    /**
     * Writes some songs (and a setlist, if given) as one VLC Sheets file: a zip with
     * [SHARE_INDEX] and each song's folder (document, annotations, audio files).
     */
    suspend fun exportShare(uri: Uri, songIds: List<String>, setlist: Setlist?) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val songs = songIds.distinct().mapNotNull { id -> _data.value.songs.firstOrNull { it.id == id } }
            require(songs.isNotEmpty()) { "Nothing to export" }
            val json = JSONObject().apply {
                put("format", "vlcsheets")
                put("version", 1)
                put("songs", JSONArray().apply { songs.forEach { put(it.toJson()) } })
                setlist?.let { put("setlist", it.toJson()) }
            }
            val output = context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Cannot open destination")
            output.use { out ->
                java.util.zip.ZipOutputStream(out.buffered()).use { zip ->
                    zip.putNextEntry(java.util.zip.ZipEntry(SHARE_INDEX))
                    zip.write(json.toString().toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                    songs.forEach { song ->
                        val folder = songFolder(song.id)
                        folder.walkTopDown().filter { it.isFile && !it.name.endsWith(".tmp") }.forEach { file ->
                            val rel = folder.toPath().relativize(file.toPath()).toString().replace('\\', '/')
                            zip.putNextEntry(java.util.zip.ZipEntry("songs/${song.id}/$rel"))
                            file.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
            }
        }
    }

    /**
     * Extracts the zip at [uri] into [dest]; [map] gives each entry's path inside [dest], or null to skip it.
     * Stops with an error once more than [maxBytes] have been written (a zip bomb would fill the disk).
     */
    internal fun unzip(uri: Uri, dest: File, maxBytes: Long = MAX_UNZIPPED_BYTES, map: (String) -> String?) {
        var total = 0L
        val buffer = ByteArray(64 * 1024)
        dest.mkdirs()
        val input = context.contentResolver.openInputStream(uri) ?: throw IllegalStateException("Cannot open file")
        input.use { raw ->
            java.util.zip.ZipInputStream(raw.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val rel = if (entry.isDirectory) null else map(entry.name.replace('\\', '/'))
                    if (rel != null) {
                        val outFile = File(dest, rel)
                        if (!outFile.canonicalPath.startsWith(dest.canonicalPath + File.separator)) {
                            throw SecurityException("Invalid archive")
                        }
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { out ->
                            while (true) {
                                val n = zip.read(buffer)
                                if (n < 0) break
                                total += n
                                if (total > maxBytes) throw SecurityException("Archive too large")
                                out.write(buffer, 0, n)
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
    }

    suspend fun importFrom(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val tmp = File(context.filesDir, "library.restore")
            val trash = File(context.filesDir, "library.old")
            try {
                tmp.deleteRecursively()
                unzip(uri, tmp) { name ->
                    name.takeIf { it == "library/index.json" || it == "library/handwriting.json" || it.startsWith("library/songs/") }
                        ?.removePrefix("library/")
                }
                val restoredIndex = File(tmp, "index.json")
                require(restoredIndex.exists()) { "index.json missing" }
                // Validate that the index can be parsed before replacing anything.
                val restoredSongs = JSONObject(restoredIndex.readText(Charsets.UTF_8)).optJSONArray("songs")
                for (i in 0 until (restoredSongs?.length() ?: 0)) {
                    require(songFromJson(restoredSongs!!.getJSONObject(i)).hasSafeNames()) { "unsafe name in index" }
                }

                trash.deleteRecursively()
                if (root.exists() && !root.renameTo(trash)) {
                    throw IllegalStateException("Cannot move current library")
                }
                if (!tmp.renameTo(root)) {
                    throw IllegalStateException("Cannot install backup")
                }
                songsDir.mkdirs()
                // An older backup has no handwriting samples: keep the ones on the device.
                File(trash, inkFile.name).takeIf { it.exists() && !inkFile.exists() }?.copyTo(inkFile)
                trash.deleteRecursively()
                loadLocked()
                Result.success(Unit)
            } catch (t: Throwable) {
                tmp.deleteRecursively()
                if (!root.exists() && trash.exists()) trash.renameTo(root)
                runCatching { loadLocked() }
                Result.failure(t)
            }
        }
    }

    suspend fun clearLibrary() = withContext(Dispatchers.IO) {
        mutex.withLock {
            songsDir.deleteRecursively()
            songsDir.mkdirs()
            // The handwriting samples are about the user, not the library: they stay.
            val next = LibraryData(loaded = true, ink = _data.value.ink)
            _data.value = next
            persistLocked(next)
        }
    }

    companion object {
        private const val THUMB_WIDTH = 480
        const val SHARE_EXTENSION = "vlcs"
        private const val MAX_UNZIPPED_BYTES = 2L * 1024 * 1024 * 1024
        private const val SHARE_INDEX = "vlcsheets.json"
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "bmp", "gif", "heic", "heif")
        private val TEXT_EXTENSIONS = setOf("txt", "cho", "crd", "chopro", "chordpro", "pro")
        private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "wav", "ogg", "flac", "opus")
    }
}
