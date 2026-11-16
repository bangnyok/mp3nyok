package com.mrras.wamp3.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrras.wamp3.Wamp3App
import com.mrras.wamp3.data.NewTrack
import com.mrras.wamp3.data.Store
import com.mrras.wamp3.download.DLEngine
import kotlinx.coroutines.launch

// ------------------------------------------------------------------
// ADD URL — fetch link (video / playlist), rename listing dulu,
// tick yang nak, kemudian masuk playlist.
// ------------------------------------------------------------------

@Composable
fun AddUrlDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ready by Wamp3App.ytdlReady.collectAsState()
    var url by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var plName by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<DLEngine.UrlEntry>>(emptyList()) }
    val checked = remember { mutableStateMapOf<String, Boolean>() }
    val names = remember { mutableStateMapOf<String, String>() }

    WaDialog(title = "ADD URL — YOUTUBE / YT MUSIC", onDismiss = onDismiss) {
        Text(
            "Paste link (video atau playlist):",
            color = Wa.TextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace
        )
        TextField(
            value = url,
            onValueChange = { url = it },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Wa.Green
            ),
            colors = darkFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            WaButton("FETCH", Modifier.weight(1f), enabled = url.isNotBlank() && !busy && ready) {
                scope.launch {
                    busy = true
                    error = null
                    try {
                        val (t, list) = DLEngine.listEntries(url.trim())
                        plName = t
                        entries = list
                        list.forEach { e ->
                            checked[e.id] = true
                            names[e.id] = Store.sanitizeFileName(e.title)
                        }
                    } catch (e: Exception) {
                        error = e.message ?: "gagal fetch"
                    }
                    busy = false
                }
            }
            WaButton("CLOSE", Modifier.weight(1f), onClick = onDismiss)
        }
        if (busy) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    Modifier.size(14.dp), color = Wa.Green, strokeWidth = 2.dp
                )
                Text(
                    "  fetching listings...",
                    color = Wa.TextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace
                )
            }
        }
        error?.let {
            Spacer(Modifier.height(6.dp))
            Text(
                "ERROR: $it",
                color = Color(0xFFFF5050), fontSize = 11.sp,
                fontFamily = FontFamily.Monospace, maxLines = 3, overflow = TextOverflow.Ellipsis
            )
        }

        if (entries.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "NAMA PLAYLIST:",
                color = Wa.TextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace
            )
            TextField(
                value = plName,
                onValueChange = { plName = it },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Wa.Green
                ),
                colors = darkFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${checked.count { it.value }} / ${entries.size} dipilih — edit nama fail:",
                color = Wa.TextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace
            )
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .padding(top = 4.dp)
            ) {
                items(entries) { e ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = checked[e.id] ?: true,
                            onCheckedChange = { checked[e.id] = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Wa.Green,
                                uncheckedColor = Wa.TextDim
                            )
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                e.title,
                                color = Wa.Green, fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            TextField(
                                value = names[e.id] ?: e.title,
                                onValueChange = { names[e.id] = Store.sanitizeFileName(it) },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp, color = Wa.Text
                                ),
                                colors = darkFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Text(
                            fmtDur(e.durationSec),
                            color = Wa.TextDim, fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                WaButton(
                    "ADD SELECTED",
                    Modifier.weight(1f),
                    enabled = checked.any { it.value }
                ) {
                    val sel = entries.filter { checked[it.id] ?: true }
                    Store.addTracks(
                        plName.ifBlank { "Playlist" },
                        sel.map { NewTrack(it.title, it.watchUrl, it.id, it.durationSec) }
                    )
                    onDismiss()
                }
                WaButton("CANCEL", Modifier.weight(1f), onClick = onDismiss)
            }
        }
    }
}

// ------------------------------------------------------------------
// PLAYLIST MANAGER — new / open / rename / merge / delete
// ------------------------------------------------------------------

private sealed interface MgrSub {
    data object New : MgrSub
    data class Rename(val pid: String) : MgrSub
    data class Delete(val pid: String) : MgrSub
}

@Composable
fun PlaylistManagerDialog(onDismiss: () -> Unit) {
    val playlists by Store.playlists.collectAsState()
    val currentId by Store.currentPlaylistId.collectAsState()
    var selId by remember { mutableStateOf(playlists.firstOrNull()?.id) }
    var sub by remember { mutableStateOf<MgrSub?>(null) }
    var subText by remember { mutableStateOf("") }

    WaDialog(title = "PLAYLIST MANAGER", onDismiss = onDismiss) {
        Text(
            "Tap untuk select ( ● = playlist semasa ):",
            color = Wa.TextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace
        )
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
                .padding(top = 4.dp)
                .background(Color(0xFF0B0B0B))
        ) {
            itemsIndexed(playlists) { _, p ->
                val sel = p.id == selId
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(if (sel) Wa.SelectBlue else Color.Transparent)
                        .clickable { selId = p.id }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            p.name + if (p.id == currentId) "  ●" else "",
                            color = if (sel) Color.White else Wa.Green,
                            fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${p.trackIds.size} tracks",
                            color = Wa.TextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            WaButton("NEW", Modifier.weight(1f)) {
                subText = ""
                sub = MgrSub.New
            }
            WaButton("OPEN", Modifier.weight(1f), enabled = selId != null) {
                selId?.let { Store.setCurrent(it) }
            }
            WaButton("RENAME", Modifier.weight(1.2f), enabled = selId != null) {
                selId?.let { pid ->
                    subText = playlists.firstOrNull { it.id == pid }?.name ?: ""
                    sub = MgrSub.Rename(pid)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        val cur = Store.currentPlaylist()
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            WaButton(
                "MERGE -> CURRENT",
                Modifier.weight(1.6f),
                enabled = selId != null && cur != null && selId != cur.id
            ) {
                val c = Store.currentPlaylist() ?: return@WaButton
                selId?.let { Store.mergePlaylist(fromId = it, intoId = c.id) }
            }
            WaButton("DELETE", Modifier.weight(1f), enabled = selId != null) {
                selId?.let { sub = MgrSub.Delete(it) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "MERGE -> CURRENT: salin semua listing playlist yang dipilih ke hujung playlist semasa.",
            color = Wa.TextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace
        )

        // Sub dialog (rename / new / delete)
        sub?.let { s ->
            when (s) {
                is MgrSub.New, is MgrSub.Rename -> {
                    AlertDialog(
                        onDismissRequest = { sub = null },
                        title = {
                            Text(
                                if (s is MgrSub.New) "NEW PLAYLIST" else "RENAME PLAYLIST",
                                fontSize = 14.sp, fontFamily = FontFamily.Monospace
                            )
                        },
                        text = {
                            TextField(
                                value = subText,
                                onValueChange = { subText = it },
                                singleLine = true,
                                colors = darkFieldColors(),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp, color = Wa.Green
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                when (s) {
                                    is MgrSub.New -> Store.createPlaylist(subText)
                                    is MgrSub.Rename -> Store.renamePlaylist(s.pid, subText)
                                    else -> {}
                                }
                                sub = null
                            }) { Text("OK", color = Wa.Green) }
                        },
                        dismissButton = {
                            TextButton(onClick = { sub = null }) {
                                Text("CANCEL", color = Wa.TextDim)
                            }
                        }
                    )
                }
                is MgrSub.Delete -> {
                    val name = playlists.firstOrNull { it.id == s.pid }?.name ?: ""
                    AlertDialog(
                        onDismissRequest = { sub = null },
                        title = {
                            Text("DELETE PLAYLIST", fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                        },
                        text = {
                            Text(
                                "Buang playlist \"$name\"?\nFail MP3 yang sudah download tidak dipadam.",
                                fontSize = 12.sp, fontFamily = FontFamily.Monospace
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                Store.deletePlaylist(s.pid)
                                selId = Store.playlists.value.firstOrNull()?.id
                                sub = null
                            }) { Text("DELETE", color = Color(0xFFFF5050)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { sub = null }) {
                                Text("CANCEL", color = Wa.TextDim)
                            }
                        }
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// SETTINGS — folder MP3, permission, update yt-dlp
// ------------------------------------------------------------------

@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val ready by Wamp3App.ytdlReady.collectAsState()
    val ytdlErr by Wamp3App.ytdlError.collectAsState()
    var folder by remember { mutableStateOf(Store.musicFolder.value) }
    var msg by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var allAccess by remember { mutableStateOf(checkAllFilesAccess()) }

    WaDialog(title = "SETTINGS", onDismiss = onDismiss) {
        Text(
            "Folder MP3 (tempat simpan / senang cari):",
            color = Wa.TextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace
        )
        TextField(
            value = folder,
            onValueChange = { folder = it },
            singleLine = true,
            colors = darkFieldColors(),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Wa.Green
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            WaButton("APPLY", Modifier.weight(1f)) {
                Store.setFolder(folder)
                msg = "folder disimpan: ${Store.musicFolder.value}"
            }
            WaButton("RESET DEFAULT", Modifier.weight(1f)) {
                folder = Store.defaultMusicFolder()
                Store.setFolder(folder)
                msg = "folder disimpan: ${Store.musicFolder.value}"
            }
        }

        if (Build.VERSION.SDK_INT >= 30 && !allAccess) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Untuk simpan ke folder awam (Music/...), beri \"All files access\":",
                color = Wa.Orange, fontSize = 10.sp, fontFamily = FontFamily.Monospace
            )
            WaButton("GRANT ALL FILES ACCESS", Modifier.fillMaxWidth()) {
                try {
                    ctx.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:${ctx.packageName}")
                        )
                    )
                } catch (_: Exception) {
                    try {
                        ctx.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                    } catch (_: Exception) {
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = when {
                ytdlErr != null -> "yt-dlp ERROR: $ytdlErr"
                ready -> "yt-dlp ready ${Wamp3App.ytdlVersion.value}"
                else -> "yt-dlp: initialising..."
            },
            color = if (ready) Wa.Green else Wa.TextDim,
            fontSize = 10.sp, fontFamily = FontFamily.Monospace
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
            WaButton("UPDATE yt-dlp", Modifier.weight(1f), enabled = ready && !busy) {
                scope.launch {
                    busy = true
                    try {
                        msg = DLEngine.updateYtDlp(ctx)
                    } catch (e: Exception) {
                        msg = "update gagal: ${e.message?.take(150)}"
                    }
                    busy = false
                }
            }
            WaButton("CLOSE", Modifier.weight(1f), onClick = onDismiss)
        }
        msg?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                it, color = Wa.Green, fontSize = 10.sp,
                fontFamily = FontFamily.Monospace, maxLines = 3, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Nota: MP3 disimpan dengan best audio (diconvert oleh ffmpeg).",
            color = Wa.TextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace
        )
    }
}

private fun checkAllFilesAccess(): Boolean =
    Build.VERSION.SDK_INT < 30 || Environment.isExternalStorageManager()

// ------------------------------------------------------------------
// RENAME TRACK — rename listing + fail MP3 di disk
// ------------------------------------------------------------------

@Composable
fun RenameDialog(trackId: String, onDismiss: () -> Unit) {
    val track = Store.tracks.value[trackId]
    var name by remember(trackId) {
        mutableStateOf(track?.fileName?.ifBlank { track?.title ?: "" } ?: "")
    }

    WaDialog(title = "RENAME TRACK", onDismiss = onDismiss) {
        if (track == null) {
            Text("Track tidak dijumpai.", color = Wa.TextDim, fontSize = 11.sp)
        } else {
            Text(
                "Nama fail MP3 (tanpa .mp3). Fail di disk juga akan direname:",
                color = Wa.TextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(4.dp))
            TextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                colors = darkFieldColors(),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Wa.Green
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                WaButton("RENAME", Modifier.weight(1f), enabled = name.isNotBlank()) {
                    Store.renameTrack(trackId, name)
                    onDismiss()
                }
                WaButton("CANCEL", Modifier.weight(1f), onClick = onDismiss)
            }
        }
    }
}
