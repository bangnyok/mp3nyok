package com.bangnyok.mp3nyok.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bangnyok.mp3nyok.Mp3NyokApp
import com.bangnyok.mp3nyok.data.Store
import com.bangnyok.mp3nyok.download.DLEngine
import com.bangnyok.mp3nyok.update.Updater

private sealed interface DialogKind {
    data object AddUrl : DialogKind
    data object Manage : DialogKind
    data object Settings : DialogKind
    data class Rename(val trackId: String) : DialogKind
}

@Composable
fun MainScreen() {
    var dialog by remember { mutableStateOf<DialogKind?>(null) }
    var showUpdate by remember { mutableStateOf(false) }

    // Semak update GitHub sekali bila app dibuka
    LaunchedEffect(Unit) { Updater.checkAsync() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
    ) {
        PlayerWindow(
            onUpdate = { showUpdate = true },
            onSettings = { dialog = DialogKind.Settings }
        )
        PlaylistWindow(
            onAddUrl = { dialog = DialogKind.AddUrl },
            onManage = { dialog = DialogKind.Manage },
            onSettings = { dialog = DialogKind.Settings },
            onRenameTrack = { dialog = DialogKind.Rename(it) },
            modifier = Modifier.weight(1f)
        )
        StatusBar(onClick = { dialog = DialogKind.Settings })
    }

    when (val d = dialog) {
        is DialogKind.AddUrl -> AddUrlDialog { dialog = null }
        is DialogKind.Manage -> PlaylistManagerDialog { dialog = null }
        is DialogKind.Settings -> SettingsDialog { dialog = null }
        is DialogKind.Rename -> RenameDialog(d.trackId) { dialog = null }
        null -> {}
    }

    if (showUpdate) {
        UpdateDialog(onDismiss = { showUpdate = false })
    }
}

@Composable
private fun StatusBar(onClick: () -> Unit) {
    val folder by Store.musicFolder.collectAsState()
    val jobs by DLEngine.active.collectAsState()
    val ready by Mp3NyokApp.ytdlReady.collectAsState()
    // Refresh status permission bila user balik dari skrin grant
    var allAccess by remember { mutableStateOf(checkAllFilesAccess()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1500)
            allAccess = checkAllFilesAccess()
        }
    }

    val running = jobs.values.filter {
        it.state == DLEngine.State.QUEUED || it.state == DLEngine.State.RUNNING
    }
    val failed = jobs.values.filter { it.state == DLEngine.State.FAILED }
    Row(
        Modifier
            .fillMaxWidth()
            .background(Wa.BgDark)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(Modifier.weight(1f)) {
            if (!allAccess && android.os.Build.VERSION.SDK_INT >= 30) {
                Text(
                    "⚠ BERI PERMISSION FAIL DULU — tap sini → GRANT ALL FILES ACCESS",
                    color = Wa.LedRed,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                if (!ready) "yt-dlp: initialising..." else folder,
                color = Wa.TextDim,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            running.firstOrNull()?.let {
                Text(
                    "DL ${it.progress.toInt()}%  ${it.title}",
                    color = Wa.Orange,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            failed.firstOrNull()?.let {
                Text(
                    "ERR: ${it.error ?: "-"}",
                    color = Wa.LedRed,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (running.isNotEmpty()) {
            LinearProgressIndicator(
                progress = { (running.firstOrNull()?.progress ?: 0f) / 100f },
                color = Wa.Green,
                trackColor = Color(0xFF333333),
                modifier = Modifier.width(60.dp)
            )
        }
        Text(
            "${running.size} DL",
            color = Wa.Green,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
