package com.mrras.wamp3

import android.app.Application
import com.mrras.wamp3.data.Store
import com.mrras.wamp3.player.PlayerHolder
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow

class Wamp3App : Application() {

    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        PlayerHolder.init(this)

        // yt-dlp init lambat sikit (kali pertama unzip python), jalan di background
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        appScope.launch {
            try {
                YoutubeDL.getInstance().init(this@Wamp3App)
                FFmpeg.getInstance().init(this@Wamp3App)
                ytdlVersion.value = YoutubeDL.version(this@Wamp3App) ?: ""
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
