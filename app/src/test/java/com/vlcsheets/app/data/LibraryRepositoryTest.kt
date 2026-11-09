package com.vlcsheets.app.data

import android.content.Context
import android.net.Uri
import com.vlcsheets.app.MainViewModel
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
class LibraryRepositoryTest {

    private lateinit var context: Context
    private lateinit var libraryDir: File

    private val stroke = Stroke("s1", page = 1, points = listOf(Pt(0.1f, 0.2f), Pt(0.3f, 0.4f)), color = 0xFF112233, width = 2f)
    private val song = Song(
        id = "song1", title = "Cigne", composer = "Saint-Saëns", genre = "Clàssica", key = "Sol", tempo = 72,
        notes = "lent", tags = listOf("violoncel"), collections = listOf("Concert"), favorite = true,
        pageCount = 3, thumbFile = "thumb.jpg", pageAspect = 0.7f,
        bookmarks = listOf(Bookmark("b1", 2, "Coda", createdAt = 5)),
        createdAt = 1, updatedAt = 2, lastOpenedAt = 3, openCount = 4, lastPage = 2,
        year = 1886, durationSec = 185, beatsPerBar = 3,
        jumps = listOf(Jump("j1", 2, 0.5f, 0.25f, 0), Jump("j2", 0, 0.1f, 0.1f, 0, audioFile = "a.mp3", audioMs = 61_000)),
        masks = listOf(Mask("m1", 1, 0.1f, 0.2f, 0.9f, 0.3f)),
    )

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        libraryDir = File(context.filesDir, "library")
        libraryDir.deleteRecursively()
    }

    private fun writeIndex(songs: JSONArray) {
        File(libraryDir, "songs/song1").mkdirs()
        File(libraryDir, "songs/song1/score.pdf").writeText("%PDF")
        File(libraryDir, "index.json").writeText(
            JSONObject().put("version", 1).put("songs", songs).put("setlists", JSONArray()).put("collections", JSONArray()).toString(),
        )
    }

    @Test
    fun songSavesAndLoadsTheSame() {
        assertEquals(song, songFromJson(song.toJson()))
        val setlist = Setlist("l1", "Concert", listOf("song1"), "notes", mapOf("song1" to "En Sol"), 1, 2)
        assertEquals(setlist, setlistFromJson(setlist.toJson()))
        assertEquals(listOf(stroke), strokesFromJson(strokesToJson(listOf(stroke))))
    }

    @Test
    fun durationsReadAndWrite() {
        assertEquals(205, parseDuration("3:25"))
        assertEquals(180, parseDuration(" 3 "))
        assertEquals(3725, parseDuration("1:02:05"))
        assertEquals(0, parseDuration(""))
        assertEquals(null, parseDuration("3:75"))
        assertEquals(null, parseDuration("a:10"))
        assertEquals("3:25", formatDuration(205))
        assertEquals("1:02:05", formatDuration(3725))
    }

    @Test
    fun oldAnnotationsInsideIndexMoveToTheirOwnFile() = runBlocking {
        writeIndex(JSONArray().put(song.toJson().put("annotations", strokesToJson(listOf(stroke)))))
        val repo = LibraryRepository(context)
        repo.load()
        assertEquals(listOf(stroke), repo.data.value.songs.single().annotations)
        assertTrue(File(libraryDir, "songs/song1/annotations.json").exists())

        repo.setFavorite("song1", false) // rewrites the index
        assertFalse(File(libraryDir, "index.json").readText().contains("annotations"))
        val reloaded = LibraryRepository(context).apply { load() }
        assertEquals(listOf(stroke), reloaded.data.value.songs.single().annotations)
    }

    @Test
    fun backupThenClearThenRestoreGivesTheSameLibrary() = runBlocking {
        writeIndex(JSONArray().put(song.toJson()))
        val repo = LibraryRepository(context).apply { load() }
        repo.addStroke("song1", stroke)
        repo.mutateInk { listOf(InkSample("4", listOf(listOf(Pt(0f, 0f), Pt(1f, 1f))))) }
        val before = repo.data.value

        val zip = File(context.cacheDir, "backup.zip")
        repo.exportTo(Uri.fromFile(zip))
        repo.clearLibrary()
        repo.mutateInk { emptyList() }
        assertTrue(repo.data.value.songs.isEmpty())

        assertTrue(repo.importFrom(Uri.fromFile(zip)).isSuccess)
        assertEquals(before.songs, repo.data.value.songs)
        assertEquals(before.ink, repo.data.value.ink)
        assertEquals("%PDF", File(libraryDir, "songs/song1/score.pdf").readText())
    }

    @Test
    fun backupWithPathOutsideTheLibraryIsRejected() = runBlocking {
        writeIndex(JSONArray().put(song.toJson()))
        val repo = LibraryRepository(context).apply { load() }

        val zip = File(context.cacheDir, "evil.zip")
        ZipOutputStream(zip.outputStream()).use { out ->
            out.putNextEntry(ZipEntry("library/index.json")); out.write("{}".toByteArray()); out.closeEntry()
            out.putNextEntry(ZipEntry("library/songs/../../evil.txt")); out.write("x".toByteArray()); out.closeEntry()
        }
        assertTrue(repo.importFrom(Uri.fromFile(zip)).isFailure)
        assertFalse(File(context.filesDir, "evil.txt").exists())
        assertEquals(listOf("song1"), repo.data.value.songs.map { it.id })
        assertTrue(File(libraryDir, "songs/song1/score.pdf").exists())
    }

    @Test
    fun shareFileOfASetlistImportsAsNewCopies() = runBlocking {
        writeIndex(JSONArray().put(song.toJson()))
        File(libraryDir, "songs/song1/audio").mkdirs()
        File(libraryDir, "songs/song1/audio/a.mp3").writeText("mp3")
        val repo = LibraryRepository(context).apply { load() }
        repo.addStroke("song1", stroke)
        val setlist = repo.createSetlist("Concert", listOf("song1"))
        repo.updateSetlist(setlist.copy(songNotes = mapOf("song1" to "En Sol")))

        val file = File(context.cacheDir, "concert.vlcs")
        repo.exportShare(Uri.fromFile(file), listOf("song1"), repo.data.value.setlists.single())
        val result = repo.importUris(listOf(Uri.fromFile(file)))

        assertEquals(1, result.imported)
        val copy = result.songs.single()
        assertTrue(copy.id != "song1")
        assertEquals(song.title, copy.title)
        assertEquals(song.jumps, copy.jumps)
        assertEquals(listOf(stroke), copy.annotations)
        assertEquals("mp3", File(libraryDir, "songs/${copy.id}/audio/a.mp3").readText())
        assertEquals(2, repo.data.value.songs.size)
        val importedList = repo.data.value.setlists.last()
        assertEquals(listOf(copy.id), importedList.songIds)
        assertEquals(mapOf(copy.id to "En Sol"), importedList.songNotes)
    }

    @Test
    fun searchIgnoresAccentsAndCase() {
        assertEquals("saint-saens cancons", MainViewModel.normalize("Saint-Saëns CANÇONS"))
    }
}
