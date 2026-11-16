package com.mrras.wamp3.data

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Simpanan pusat: playlists + tracks + folder MP3.
 * Disimpan sebagai JSON dalam filesDir/library.json.
 */
object Store {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }
    private lateinit var file: File

    val playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val tracks = MutableStateFlow<Map<String, Track>>(emptyMap())
    val currentPlaylistId = MutableStateFlow<String?>(null)
    val musicFolder = MutableStateFlow("")

    fun init(context: Context) {
        file = File(context.filesDir, "library.json")
        val lib = if (file.exists()) {
            try {
                json.decodeFromString<LibraryFile>(file.readText())
            } catch (_: Exception) {
                LibraryFile()
            }
        } else LibraryFile()

        musicFolder.value = lib.musicFolder.ifBlank { defaultMusicFolder() }
        var pls = lib.playlists
        if (pls.isEmpty()) pls = listOf(Playlist(UUID.randomUUID().toString(), "Playlist 1"))
        playlists.value = pls
        tracks.value = lib.tracks
        currentPlaylistId.value = lib.currentPlaylistId ?: pls[0].id
        save()
    }

    fun defaultMusicFolder(): String =
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Wamp3").absolutePath

    private fun save() {
        try {
            file.parentFile?.mkdirs()
            file.writeText(
                json.encodeToString(
                    LibraryFile(
                        playlists.value,
                        tracks.value,
                        currentPlaylistId.value,
                        musicFolder.value
                    )
                )
            )
        } catch (_: Exception) {
        }
    }

    fun currentPlaylist(): Playlist? {
        val id = currentPlaylistId.value
        return playlists.value.firstOrNull { it.id == id } ?: playlists.value.firstOrNull()
    }

    fun playlistTracks(pid: String?): List<Track> {
        val p = playlists.value.firstOrNull { it.id == pid } ?: return emptyList()
        val t = tracks.value
        return p.trackIds.mapNotNull { t[it] }
    }

    fun createPlaylist(name: String): Playlist {
        val p = Playlist(UUID.randomUUID().toString(), name.ifBlank { "Playlist" })
        playlists.value = playlists.value + p
        currentPlaylistId.value = p.id
        save()
        return p
    }

    fun setCurrent(pid: String) {
        if (playlists.value.any { it.id == pid }) {
            currentPlaylistId.value = pid
            save()
        }
    }

    fun renamePlaylist(pid: String, name: String) {
        playlists.value = playlists.value.map {
            if (it.id == pid) it.copy(name = name.ifBlank { it.name }) else it
        }
        save()
    }

    fun deletePlaylist(pid: String) {
        playlists.value = playlists.value.filter { it.id != pid }
        tracks.value = tracks.value.filterValues { it.playlistId != pid }
        if (currentPlaylistId.value == pid) {
            currentPlaylistId.value = playlists.value.firstOrNull()?.id
        }
        if (playlists.value.isEmpty()) {
            createPlaylist("Playlist 1")
        }
        save()
    }

    fun addTracks(nameOrId: String, newTracks: List<NewTrack>) {
        if (newTracks.isEmpty()) return
        // Resolve: id dulu, kemudian nama (dialog Add hantar nama playlist)
        val target = playlists.value.firstOrNull { it.id == nameOrId }
            ?: playlists.value.firstOrNull { it.name.equals(nameOrId, ignoreCase = true) }
            ?: createPlaylist(nameOrId)
        val pid = target.id
        val map = tracks.value.toMutableMap()
        val ids = target.trackIds.toMutableList()
        for (nt in newTracks) {
            val t = Track(
                id = UUID.randomUUID().toString(),
                playlistId = pid,
                title = nt.title,
                sourceUrl = nt.sourceUrl,
                ytid = nt.ytid,
                fileName = sanitizeFileName(nt.title),
                durationSec = nt.durationSec
            )
            map[t.id] = t
            ids.add(t.id)
        }
        tracks.value = map
        playlists.value = playlists.value.map {
            if (it.id == pid) it.copy(trackIds = ids) else it
        }
        // Tunjukkan playlist itu terus selepas tambah listing
        currentPlaylistId.value = pid
        save()
    }

    /** Buang listing dari playlist (fail MP3 yang sudah ada kekal di folder). */
    fun removeTrack(pid: String, trackId: String) {
        playlists.value = playlists.value.map {
            if (it.id == pid) it.copy(trackIds = it.trackIds - trackId) else it
        }
        tracks.value = tracks.value - trackId
        save()
    }

    /** Merge: salin semua track playlist <fromId> ke hujung playlist <intoId>. */
    fun mergePlaylist(fromId: String, intoId: String) {
        if (fromId == intoId) return
        val from = playlists.value.firstOrNull { it.id == fromId } ?: return
        val into = playlists.value.firstOrNull { it.id == intoId } ?: return
        val map = tracks.value.toMutableMap()
        val ids = into.trackIds.toMutableList()
        for (tid in from.trackIds) {
            val src = tracks.value[tid] ?: continue
            val copy = src.copy(id = UUID.randomUUID().toString(), playlistId = intoId)
            map[copy.id] = copy
            ids.add(copy.id)
        }
        tracks.value = map
        playlists.value = playlists.value.map {
            if (it.id == intoId) it.copy(trackIds = ids) else it
        }
        save()
    }

    /**
     * Rename listing -> juga rename fail MP3 di disk supaya nama fail
     * ikut kehendak user.
     */
    fun renameTrack(trackId: String, newName: String) {
        val tr = tracks.value[trackId] ?: return
        val clean = sanitizeFileName(newName)
        var newPath = tr.filePath
        val old = tr.filePath
        if (old != null) {
            val f = File(old)
            if (f.exists()) {
                val target = uniqueTarget(f.parentFile ?: File(musicFolder.value), "$clean.mp3")
                if (f.renameTo(target)) newPath = target.absolutePath
            }
        }
        tracks.value = tracks.value + (trackId to tr.copy(fileName = clean, filePath = newPath))
        save()
    }

    fun markDownloaded(trackId: String, path: String) {
        val tr = tracks.value[trackId] ?: return
        tracks.value = tracks.value + (trackId to tr.copy(
            filePath = path,
            fileName = tr.fileName.ifBlank { File(path).nameWithoutExtension }
        ))
        save()
    }

    fun setFolder(path: String) {
        val p = path.trim().trimEnd('/', '\\')
        if (p.isNotBlank()) {
            musicFolder.value = p
            save()
        }
    }

    fun sanitizeFileName(name: String): String {
        val cleaned = name
            .replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(120)
            .trimEnd('.', ' ')
        return cleaned.ifBlank { "untitled" }
    }

    fun uniqueTarget(dir: File, fileName: String): File {
        var f = File(dir, fileName)
        if (!f.exists()) return f
        val base = f.nameWithoutExtension
        val ext = f.extension
        var i = 2
        while (f.exists()) {
            f = File(dir, "$base ($i).$ext")
            i++
        }
        return f
    }
}
