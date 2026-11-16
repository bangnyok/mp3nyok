package com.bangnyok.mp3nyok

import android.app.Application
import com.bangnyok.mp3nyok.data.Store
import com.bangnyok.mp3nyok.player.PlayerHolder
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow

class Mp3NyokApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        PlayerHolder.init(this)

        // yt-dlp init lambat sikit (kali pertama unzip python), jalan di background
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        appScope.launch {
            try {
                YoutubeDL.getInstance().init(this@Mp3NyokApp)
                FFmpeg.getInstance().init(this@Mp3NyokApp)
                ytdlVersion.value = YoutubeDL.version(this@Mp3NyokApp) ?: ""
                ytdlReady.value = true
            } catch (e: Exception) {
                ytdlError.value = e.message ?: e.toString()
            }
        }
    }

    companion object {
        val ytdlReady = MutableStateFlow(false)
        val ytdlVersion = MutableStateFlow("")
        val ytdlError = MutableStateFlow<String?>(null)
    }
}
