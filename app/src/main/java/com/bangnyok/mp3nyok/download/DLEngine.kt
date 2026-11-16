package com.bangnyok.mp3nyok.download

import android.content.Context
import com.bangnyok.mp3nyok.Mp3NyokApp
import com.bangnyok.mp3nyok.data.Store
import com.bangnyok.mp3nyok.data.Track
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.lang.IllegalStateException

/**
 * Enjin download yt-dlp. Queue berjalan satu-satu (stabil),
 * convert ke MP3 melalui ffmpeg, dan rename fail ikut nama user.
 */
object DLEngine {

    enum class State { QUEUED, RUNNING, DONE, FAILED, CANCELLED }

    data class Job(
        val trackId: String,
        val title: String,
        val state: State = State.QUEUED,
        val progress: Float = 0f,
        val line: String = "",
        val error: String? = null
    )

    data class UrlEntry(
        val id: String,
        val title: String,
        val durationSec: Double?,
        val watchUrl: String
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    val active = MutableStateFlow<Map<String, Job>>(emptyMap())

    fun enqueue(track: Track) {
        val existing = active.value[track.id]
        if (existing != null && (existing.state == State.QUEUED || existing.state == State.RUNNING)) return
        active.value = active.value + (
            track.id to Job(
                track.id,
                track.title.ifBlank { track.fileName.ifBlank { track.sourceUrl } }
            )
            )
        scope.launch { mutex.withLock { process(track) } }
    }

    fun cancel(trackId: String) {
        runCatching { YoutubeDL.getInstance().destroyProcessById("mp3nyok-$trackId") }
    }

    fun clearFinished() {
        active.value = active.value.filterValues {
            it.state == State.QUEUED || it.state == State.RUNNING
        }
    }

    private fun setJob(id: String, f: (Job) -> Job) {
        active.update { m -> m[id]?.let { m + (id to f(it)) } ?: m }
    }

    private suspend fun process(track: Track) {
        setJob(track.id) { it.copy(state = State.RUNNING, progress = 0f) }
        try {
            if (!Mp3NyokApp.ytdlReady.value) {
                throw IllegalStateException("yt-dlp masih initialising, cuba lagi sebentar")
            }
            val dir = File(Store.musicFolder.value)
            if (!dir.exists()) dir.mkdirs()

            val req = YoutubeDLRequest(track.sourceUrl)
            req.addOption("-o", File(dir, "%(id)s.%(ext)s").absolutePath)
            req.addOption("-f", "bestaudio/best")
            req.addOption("--extract-audio")
            req.addOption("--audio-format", "mp3")
            req.addOption("--audio-quality", "0")
            req.addOption("--no-playlist")
            req.addOption("--no-mtime")
            req.addOption("--no-warnings")
            Store.findCookies()?.let { req.addOption("--cookies", it.absolutePath) }

            YoutubeDL.getInstance().execute(req, "mp3nyok-${track.id}") { p, _, line ->
                setJob(track.id) { it.copy(progress = p, line = line) }
            }

            // Cari fail output (berdasarkan yt id) kemudian rename ke nama user
            val produced = dir.listFiles()
                ?.filter { it.isFile && it.name.startsWith("${track.ytid}.") }
                ?.maxByOrNull { it.lastModified() }
                ?: throw IllegalStateException("fail output tidak dijumpai")

            val finalBase = Store.sanitizeFileName(track.fileName.ifBlank { track.title })
            val target = Store.uniqueTarget(dir, "$finalBase.mp3")
            if (!produced.renameTo(target)) throw IllegalStateException("gagal rename fail")
            Store.markDownloaded(track.id, target.absolutePath)
            setJob(track.id) { it.copy(state = State.DONE, progress = 100f, line = target.name) }
        } catch (e: YoutubeDL.CanceledException) {
            setJob(track.id) { it.copy(state = State.CANCELLED, error = "dibatalkan") }
        } catch (e: Exception) {
            setJob(track.id) {
                it.copy(state = State.FAILED, error = (e.message ?: "ralat tidak diketahui").take(300))
            }
        }
    }

    /** Dapatkan senarai listing dari URL (video tunggal atau playlist). */
    suspend fun listEntries(url: String): Pair<String, List<UrlEntry>> = withContext(Dispatchers.IO) {
        val req = YoutubeDLRequest(url)
        req.addOption("--flat-playlist")
        req.addOption("--dump-single-json")
        req.addOption("--no-warnings")
        Store.findCookies()?.let { req.addOption("--cookies", it.absolutePath) }
        val resp = YoutubeDL.getInstance().execute(req)
        val obj = JSONObject(resp.out)
        val title = obj.optString("title", "").ifBlank { "Playlist" }
        val out = ArrayList<UrlEntry>()

        val arr = obj.optJSONArray("entries")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val e = arr.optJSONObject(i) ?: continue
                val vid = e.optString("id", "")
                if (vid.isBlank()) continue
                val rawUrl = e.optString("url", "")
                val watch = if (rawUrl.startsWith("http")) rawUrl
                else "https://www.youtube.com/watch?v=$vid"
                val d = e.optDouble("duration", Double.NaN)
                out.add(UrlEntry(vid, e.optString("title", vid), if (d.isNaN()) null else d, watch))
            }
        } else {
            val vid = obj.optString("id", "")
            if (vid.isNotBlank()) {
                val d = obj.optDouble("duration", Double.NaN)
                out.add(UrlEntry(vid, obj.optString("title", vid), if (d.isNaN()) null else d, url))
            }
        }
        if (out.isEmpty()) throw IllegalStateException("Tiada video ditemui dari URL tersebut")
        title to out
    }

    suspend fun updateYtDlp(context: Context): String = withContext(Dispatchers.IO) {
        val status = YoutubeDL.getInstance().updateYoutubeDL(context)
        if (status == YoutubeDL.UpdateStatus.DONE) "yt-dlp dikemaskini" else "yt-dlp masih terkini"
    }
}
