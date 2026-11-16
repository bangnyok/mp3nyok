package com.bangnyok.mp3nyok.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bangnyok.mp3nyok.BuildConfig
import com.bangnyok.mp3nyok.Mp3NyokApp
import com.bangnyok.mp3nyok.data.NewTrack
import com.bangnyok.mp3nyok.data.Store
import com.bangnyok.mp3nyok.download.DLEngine
import com.bangnyok.mp3nyok.update.Updater
import kotlinx.coroutines.launch

// ------------------------------------------------------------------
// ADD URL — fetch link (video / playlist), rename listing dulu,
// tick yang nak, kemudian masuk playlist.
// ------------------------------------------------------------------

@Composable
fun AddUrlDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val ready by Mp3NyokApp.ytdlReady.collectAsState()
    val playlists by Store.playlists.collectAsState()
    var url by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var plName by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<DLEngine.UrlEntry>>(emptyList()) }
    val checked = remember { mutableStateMapOf<String, Boolean>() }
    val names = remember { mutableStateMapOf<String, String>() }
    // APPEND (tambah ke hujung) atau REPLACE (kosongkan list itu dulu, tanpa padam list lain)
    var mode by remember { mutableStateOf("APPEND") }
    var replaceAsk by remember { mutableStateOf(false) }
    var saveAsName by remember { mutableStateOf("") }
    var keepOld by remember { mutableStateOf(true) }

    // Playlist sedia ada yang nama sama (untuk pilihan APPEND/REPLACE)
    val existing = playlists.firstOrNull {
        it.name.equals(plName.trim(), ignoreCase = true) && plName.isNotBlank()
    }

    WaDialog(title = "ADD URL — YOUTUBE / YT MUSIC", onDismiss = onDismiss) {
        Text(
            "Paste link (video or playlist):",
            color = Wa.TextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TextField(
                value = url,
                onValueChange = { url = it },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Wa.Green
                ),
                colors = darkFieldColors(),
                modifier = Modifier.weight(1f)
            )
            WaButton("PASTE") {
                val t = clipboard.getText()?.toString()?.trim()
                if (!t.isNullOrBlank()) url = t
            }
        }
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
                        error = e.message ?: "fetch failed"
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
                "PLAYLIST NAME:",
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
            // Playlist nama sama sudah wujud? Tanya APPEND atau REPLACE
            if (existing != null) {
                Text(
                    "Playlist \"${existing.name}\" already exists (${existing.trackIds.size} tracks):",
                    color = Wa.Orange, fontSize = 10.sp, fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    WaButton(
                        "APPEND",
                        Modifier.weight(1f),
                        active = mode == "APPEND"
                    ) { mode = "APPEND" }
                    WaButton(
                        "REPLACE",
                        Modifier.weight(1f),
                        active = mode == "REPLACE"
                    ) { mode = "REPLACE" }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    if (mode == "APPEND") "New tracks are added to the end of that list."
                    else "That list will be cleared first (other saved lists are untouched).",
                    color = Wa.TextDim, fontSize = 9.sp, fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                "${checked.count { it.value }} / ${entries.size} selected — edit file names:",
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
                    val mapped = sel.map { NewTrack(it.title, it.watchUrl, it.id, it.durationSec) }
                    val targetName = plName.ifBlank { "Playlist" }
                    if (mode == "REPLACE" && existing != null && existing.trackIds.isNotEmpty()) {
                        // List itu ada isi — tanya dulu: simpan salinan senarai lama?
                        saveAsName = "${existing.name} old"
                        keepOld = true
                        replaceAsk = true
                    } else if (mode == "REPLACE" && existing != null) {
                        Store.replacePlaylistTracks(existing.id, mapped)
                        Toast.makeText(
                            ctx,
                            "\"${existing.name}\" replaced — ${sel.size} tracks",
                            Toast.LENGTH_SHORT
                        ).show()
                        onDismiss()
                    } else {
                        Store.addTracks(targetName, mapped)
                        Toast.makeText(
                            ctx,
                            "${sel.size} tracks added to \"$targetName\"",
                            Toast.LENGTH_SHORT
                        ).show()
                        onDismiss()
                    }
                }
                WaButton("CANCEL", Modifier.weight(1f), onClick = onDismiss)
            }
        }
    }

    // Sub-dialog: REPLACE list yang ada isi — simpan salinan dulu?
    if (replaceAsk) {
        val ex = existing
        AlertDialog(
            onDismissRequest = { replaceAsk = false },
            title = {
                Text(
                    "REPLACE LIST \"${ex?.name ?: ""}\"",
                    fontSize = 14.sp, fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Column {
                    Text(
                        "That list has ${ex?.trackIds?.size ?: 0} tracks. " +
                            "Save a copy of the current list first?",
                        fontSize = 12.sp, fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(6.dp))
                    if (keepOld) {
                        TextField(
                            value = saveAsName,
                            onValueChange = { saveAsName = it },
                            singleLine = true,
                            colors = darkFieldColors(),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp, color = Wa.Green
                            )
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = keepOld,
                            onCheckedChange = { keepOld = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Wa.Green,
                                uncheckedColor = Wa.TextDim
                            )
                        )
                        Text(
                            "Save a copy as a new playlist",
                            fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Wa.TextDim
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val sel = entries.filter { checked[it.id] ?: true }
                    val mapped = sel.map { NewTrack(it.title, it.watchUrl, it.id, it.durationSec) }
                    val exx = existing ?: return@TextButton
                    if (keepOld && saveAsName.isNotBlank()) {
                        Store.copyPlaylist(exx.id, saveAsName)
                    }
                    Store.replacePlaylistTracks(exx.id, mapped)
                    Toast.makeText(
                        ctx,
                        "\"${exx.name}\" replaced — ${sel.size} tracks" +
                            if (keepOld && saveAsName.isNotBlank()) " (copy: \"$saveAsName\")" else "",
                        Toast.LENGTH_SHORT
                    ).show()
                    replaceAsk = false
                    onDismiss()
                }) {
                    Text("REPLACE", color = Wa.Green)
                }
            },
            dismissButton = {
                TextButton(onClick = { replaceAsk = false }) {
                    Text("CANCEL", color = Wa.TextDim)
                }
            }
        )
    }
}

// ------------------------------------------------------------------
// PLAYLIST MANAGER — save as / open / rename / delete
// ------------------------------------------------------------------

private sealed interface MgrSub {
    data class Rename(val pid: String) : MgrSub
    data class Delete(val pid: String) : MgrSub
    data object SaveAs : MgrSub
}

@Composable
fun PlaylistManagerDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
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
            WaButton("SAVE AS", Modifier.weight(1.2f)) {
                sub = MgrSub.SaveAs
            }
            WaButton("OPEN", Modifier.weight(1f), enabled = selId != null) {
                selId?.let { pid ->
                    Store.setCurrent(pid)
                    Toast.makeText(
                        ctx,
                        "Opened: ${playlists.firstOrNull { it.id == pid }?.name ?: ""}",
                        Toast.LENGTH_SHORT
                    ).show()
                    onDismiss()
                }
            }
            WaButton("RENAME", Modifier.weight(1.2f), enabled = selId != null) {
                selId?.let { pid ->
                    subText = playlists.firstOrNull { it.id == pid }?.name ?: ""
                    sub = MgrSub.Rename(pid)
                }
            }
            WaButton("DELETE", Modifier.weight(1.1f), enabled = selId != null) {
                selId?.let { sub = MgrSub.Delete(it) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "SAVE AS: save the current playlist under a new name, OR merge its tracks into another playlist.",
            color = Wa.TextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace
        )

        // Sub dialog (rename / save as / delete)
        sub?.let { s ->
            when (s) {
                is MgrSub.Rename -> {
                    AlertDialog(
                        onDismissRequest = { sub = null },
                        title = {
                            Text(
                                "RENAME PLAYLIST",
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
                                Store.renamePlaylist(s.pid, subText)
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
                is MgrSub.SaveAs -> {
                    val cur = Store.currentPlaylist()
                    var saveName by remember { mutableStateOf("") }
                    var saveTarget by remember { mutableStateOf<String?>(null) }
                    val others = playlists.filter { it.id != cur?.id }
                    WaDialog(title = "SAVE AS — ${cur?.name ?: ""}", onDismiss = { sub = null }) {
                        Text(
                            "1) Save as a new playlist:",
                            color = Wa.TextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace
                        )
                        Spacer(Modifier.height(2.dp))
                        TextField(
                            value = saveName,
                            onValueChange = { saveName = it },
                            singleLine = true,
                            colors = darkFieldColors(),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp, color = Wa.Green
                            )
                        )
                        WaButton("SAVE AS NEW", Modifier.fillMaxWidth(), enabled = saveName.isNotBlank()) {
                            cur?.let { Store.copyPlaylist(it.id, saveName) }
                            sub = null
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "2) OR merge into an existing playlist:",
                            color = Wa.TextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace
                        )
                        LazyColumn(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                                .padding(top = 4.dp)
                                .background(Color(0xFF0B0B0B))
                        ) {
                            itemsIndexed(others) { _, p ->
                                val sel = p.id == saveTarget
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(if (sel) Wa.SelectBlue else Color.Transparent)
                                        .clickable { saveTarget = p.id }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        p.name,
                                        color = if (sel) Color.White else Wa.Green,
                                        fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            WaButton(
                                "MERGE TO SELECTED",
                                Modifier.weight(2f),
                                enabled = saveTarget != null
                            ) {
                                val t = saveTarget ?: return@WaButton
                                cur?.let { Store.mergePlaylist(fromId = it.id, intoId = t) }
                                sub = null
                            }
                            WaButton("CLOSE", Modifier.weight(1f)) { sub = null }
                        }
                    }
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
    val ready by Mp3NyokApp.ytdlReady.collectAsState()
    val ytdlErr by Mp3NyokApp.ytdlError.collectAsState()
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
                ready -> "yt-dlp ready ${Mp3NyokApp.ytdlVersion.value}"
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
            "UPDATE CHECK: " + if (BuildConfig.UPDATE_TOKEN.isBlank()) "OFF (token tiada)" else "ON",
            color = if (BuildConfig.UPDATE_TOKEN.isBlank()) Wa.TextDim else Wa.Green,
            fontSize = 10.sp, fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.height(4.dp))
        val hasCookies = remember { Store.findCookies() != null }
        Text(
            "COOKIES: " + if (hasCookies) "ADA (bot-check bypass aktif)" else "TIADA",
            color = if (hasCookies) Wa.Green else Wa.TextDim,
            fontSize = 10.sp, fontFamily = FontFamily.Monospace
        )
        Text(
            "Bypass 'confirm not a bot' / age-restricted: export cookies.txt dari browser PC " +
                "(extension 'Get cookies.txt LOCALLY' di youtube.com), kemudian letak fail di " +
                "Android/data/com.bangnyok.mp3nyok/files/ atau di folder muzik anda.",
            color = Wa.TextDim, fontSize = 9.sp, fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Nota: MP3 disimpan dengan best audio (diconvert oleh ffmpeg).",
            color = Wa.TextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace
        )
    }
}

// ------------------------------------------------------------------
// UPDATE DIALOG — download & install APK dari GitHub Releases
// ------------------------------------------------------------------

@Composable
fun UpdateDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val rel by Updater.updateInfo.collectAsState()
    val prog by Updater.downloadProgress.collectAsState()
    var installing by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }

    WaDialog(title = "UPDATE", onDismiss = onDismiss) {
        val r = rel
        if (r == null) {
            Text(
                "Tiada update dikesan.",
                color = Wa.TextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(6.dp))
            WaButton("CLOSE", Modifier.fillMaxWidth(), onClick = onDismiss)
        } else {
            Text(
                "VERSI BARU: ${r.version}   (semasa: ${BuildConfig.VERSION_NAME})",
                color = Wa.Green, fontSize = 12.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            if (r.notes.isNotBlank()) {
                Text(
                    r.notes.take(900),
                    color = Wa.Text,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .heightIn(max = 180.dp)
                        .verticalScroll(rememberScrollState())
                )
                Spacer(Modifier.height(6.dp))
            }
            prog?.let {
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { it },
                    color = Wa.Green,
                    trackColor = Color(0xFF333333),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Download ${(it * 100).toInt()}%",
                    color = Wa.Orange, fontSize = 10.sp, fontFamily = FontFamily.Monospace
                )
            }
            err?.let {
                Text(
                    "ERROR: $it",
                    color = Color(0xFFFF5050), fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace, maxLines = 3
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                WaButton("DOWNLOAD & INSTALL", Modifier.weight(1f), enabled = !installing) {
                    installing = true
                    err = null
                    scope.launch {
                        try {
                            val f = Updater.downloadApk(ctx)
                            Updater.install(ctx, f)
                        } catch (e: Exception) {
                            err = e.message?.take(200) ?: "gagal download"
                        }
                        installing = false
                    }
                }
                WaButton("CLOSE", Modifier.weight(1f), onClick = onDismiss)
            }
        }
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
