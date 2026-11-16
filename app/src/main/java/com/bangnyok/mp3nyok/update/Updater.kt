package com.bangnyok.mp3nyok.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.bangnyok.mp3nyok.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Semak update dari GitHub Releases repo private ni.
 * Perlukan fine-grained PAT (Contents: read-only) dibakep dalam
 * BuildConfig.UPDATE_TOKEN (disuntik oleh CI dari secret UPDATE_TOKEN).
 */
object Updater {

    const val REPO_API = "https://api.github.com/repos/bangnyok/mp3nyok/releases/latest"

    data class Release(
        val version: String,   // contoh "v1.0.2"
        val assetId: Long,     // id asset APK (API endpoint)
        val notes: String
    )

    val updateInfo = MutableStateFlow<Release?>(null)
    val downloadProgress = MutableStateFlow<Float?>(null)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun checkAsync() {
        scope.launch {
            runCatching { check() }
        }
    }

    suspend fun check() {
        val token = BuildConfig.UPDATE_TOKEN
        if (token.isBlank()) return // update check off
        val conn = URL(REPO_API).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            if (conn.responseCode != 200) return
            val text = conn.inputStream.bufferedReader().readText()
            val obj = JSONObject(text)
            val tag = obj.optString("tag_name", "")
            if (tag.isBlank() || !isNewer(tag, BuildConfig.VERSION_NAME)) return

            val assets = obj.optJSONArray("assets") ?: return
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.optString("name", "").endsWith(".apk", true)) {
                    updateInfo.value = Release(tag, a.optLong("id"), obj.optString("body", ""))
                    break
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    /** Banding "v1.2.10" vs semasa; true kalau remote lebih baru. */
    fun isNewer(remote: String, current: String): Boolean {
        fun parse(v: String) = v.removePrefix("v").removePrefix("V")
            .split(".").map { s -> s.filter { it.isDigit() }.ifBlank { "0" }.toInt() }
        val r = parse(remote)
        val c = parse(current)
        for (i in 0 until maxOf(r.size, c.size)) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv != cv) return rv > cv
        }
        return false
    }

    /** Download APK (asset API endpoint, octet-stream). Pulangkan fail. */
    suspend fun downloadApk(context: Context): File = withContext(Dispatchers.IO) {
        val rel = updateInfo.value ?: throw IllegalStateException("tiada info update")
        val token = BuildConfig.UPDATE_TOKEN
        val dir = context.getExternalFilesDir(null) ?: context.cacheDir
        val out = File(dir, "mp3nyok-update.apk")
        downloadProgress.value = 0f

        val url = "https://api.github.com/repos/bangnyok/mp3nyok/releases/assets/${rel.assetId}"

        fun open(u: String, withAuth: Boolean): HttpURLConnection {
            val c = URL(u).openConnection() as HttpURLConnection
            c.connectTimeout = 15_000
            c.readTimeout = 60_000
            c.instanceFollowRedirects = false
            if (withAuth) {
                c.setRequestProperty("Authorization", "Bearer $token")
                c.setRequestProperty("Accept", "application/octet-stream")
            }
            return c
        }

        var conn = open(url, true)
        try {
            // GitHub memulangkan 302 ke signed URL — ikut redirect manual
            var redirects = 0
            while (true) {
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location")
                        ?: throw IOException("redirect tanpa Location")
                    conn.disconnect()
                    if (++redirects > 5) throw IOException("terlalu banyak redirect")
                    conn = open(loc, false)
                } else break
            }
            if (conn.responseCode !in 200..299) {
                throw IOException("HTTP ${conn.responseCode} semasa download APK")
            }
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                FileOutputStream(out).use { fos ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n == -1) break
                        fos.write(buf, 0, n)
                        done += n
                        if (total > 0) downloadProgress.value = done.toFloat() / total
                    }
                }
            }
            downloadProgress.value = 1f
            out
        } finally {
            conn.disconnect()
        }
    }

    /** Buka installer Android untuk fail APK. */
    fun install(context: Context, file: File) {
        val ctx = context.applicationContext
        if (Build.VERSION.SDK_INT >= 26 && !ctx.packageManager.canRequestPackageInstalls()) {
            try {
                ctx.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${ctx.packageName}")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                Toast.makeText(ctx, "Benarkan 'Install unknown apps' untuk Mp3Nyok, kemudian cuba lagi.", Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
            }
            return
        }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            ctx.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(ctx, "Tiada installer APK: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
