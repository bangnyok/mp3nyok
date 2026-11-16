package com.mrras.wamp3.player

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultMediaSourceFactory
import androidx.media3.exoplayer.ExoPlayer
import com.mrras.wamp3.Wamp3App
import com.mrras.wamp3.data.Track
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ExoPlayer singleton. Mainkan MP3 dari fail, atau "test stream"
 * terus dari YouTube (tanpa download) bila lagu belum di-download.
 */
object PlayerHolder {

    lateinit var player: ExoPlayer
        private set

    val isPlaying = MutableStateFlow(false)
    val current = MutableStateFlow<Track?>(null)
    val isStream = MutableStateFlow(false)   // true = main test stream dari web
    val positionMs = MutableStateFlow(0L)
    val durationMs = MutableStateFlow(0L)
    val volume = MutableStateFlow(1f)
    val shuffle = MutableStateFlow(false)
    val repeatAll = MutableStateFlow(true)

    private lateinit var appContext: Context
    private var scope: CoroutineScope? = null
    private var queue: List<Track> = emptyList()
    private var queueIndex = -1

    fun init(context: Context) {
        appContext = context.applicationContext
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            )
            .setAllowCrossProtocolRedirects(true)

        player = ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .build()

        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying.value = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && current.value != null) {
                    playNext()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (player.duration > 0) durationMs.value = player.duration
            }
        })

        scope!!.launch {
            while (isActive) {
                delay(250)
                if (player.isPlaying) positionMs.value = player.currentPosition
            }
        }
    }

    fun setVolume(v: Float) {
        volume.value = v
        player.volume = v
    }

    fun toggle() {
        if (player.isPlaying) player.pause() else if (current.value != null) player.play()
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
        current.value = null
        positionMs.value = 0L
        durationMs.value = 0L
        isStream.value = false
    }

    fun seekTo(ms: Long) {
        player.seekTo(ms)
        positionMs.value = ms
    }

    fun playTrack(track: Track, queueTracks: List<Track>, index: Int) {
        queue = queueTracks
        queueIndex = index
        current.value = track
        isStream.value = false
        positionMs.value = 0L
        durationMs.value = ((track.durationSec ?: 0.0) * 1000).toLong()

        // 1) Kalau MP3 dah ada, main dari fail
        val file = track.filePath?.let { File(it) }
        if (file != null && file.exists()) {
            player.setMediaItem(
                MediaItem.Builder()
                    .setUri(Uri.fromFile(file))
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(track.fileName.ifBlank { track.title }).build())
                    .build()
            )
            player.prepare()
            player.play()
            return
        }

        // 2) Belum download — test stream terus dari YouTube
        isStream.value = true
        if (!Wamp3App.ytdlReady.value) {
            Toast.makeText(appContext, "yt-dlp masih initialising...", Toast.LENGTH_SHORT).show()
            return
        }
        scope!!.launch(Dispatchers.IO) {
            try {
                val req = YoutubeDLRequest(track.sourceUrl)
                req.addOption("-f", "bestaudio/best")
                req.addOption("--no-warnings")
                val info = YoutubeDL.getInstance().getInfo(req)
                val streamUrl = info.url ?: throw YoutubeDLException("tiada stream url")
                val mime = when (info.ext) {
                    "webm" -> MimeTypes.AUDIO_WEBM
                    "m4a", "mp4", "m4b" -> MimeTypes.AUDIO_MP4
                    "mp3" -> MimeTypes.AUDIO_MPEG
                    "ogg", "opus" -> MimeTypes.AUDIO_OGG
                    else -> null
                }
                withContext(Dispatchers.Main) {
                    if (current.value?.id == track.id) { // masih track yang diminta
                        player.setMediaItem(
                            MediaItem.Builder()
                                .setUri(streamUrl)
                                .setMimeType(mime)
                                .setMediaMetadata(MediaMetadata.Builder().setTitle(track.title).build())
                                .build()
                        )
                        player.prepare()
                        player.play()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        appContext,
                        "Test stream gagal: ${e.message?.take(200) ?: "-"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun playNext() {
        if (queue.isEmpty()) return
        val nextIdx = when {
            shuffle.value && queue.size > 1 ->
                (0 until queue.size).filter { it != queueIndex }.random()
            queueIndex + 1 < queue.size -> queueIndex + 1
            repeatAll.value -> 0
            else -> { stop(); return }
        }
        playTrack(queue[nextIdx], queue, nextIdx)
    }

    fun playPrev() {
        if (queue.isEmpty()) return
        if (queueIndex - 1 >= 0) {
            playTrack(queue[queueIndex - 1], queue, queueIndex - 1)
        } else if (repeatAll.value) {
            playTrack(queue.last(), queue, queue.size - 1)
        }
    }
}
