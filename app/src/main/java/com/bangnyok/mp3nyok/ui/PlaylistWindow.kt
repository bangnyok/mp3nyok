package com.bangnyok.mp3nyok.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bangnyok.mp3nyok.data.Store
import com.bangnyok.mp3nyok.data.Track
import com.bangnyok.mp3nyok.download.DLEngine
import com.bangnyok.mp3nyok.player.PlayerHolder

@Composable
fun PlaylistWindow(
    onAddUrl: () -> Unit,
    onManage: () -> Unit,
    onSettings: () -> Unit,
    onRenameTrack: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val playlists by Store.playlists.collectAsState()
    val tracks by Store.tracks.collectAsState()
    val currentId by Store.currentPlaylistId.collectAsState()
    val downloads by DLEngine.active.collectAsState()
    val playingTrack by PlayerHolder.current.collectAsState()

    val playlist = playlists.firstOrNull { it.id == currentId } ?: playlists.firstOrNull()
    val plTracks = playlist?.trackIds?.mapNotNull { tracks[it] } ?: emptyList()

    var selected by remember(playlist?.id) { mutableStateOf<Int?>(null) }
    var errJob by remember { mutableStateOf<DLEngine.Job?>(null) }

    errJob?.let { j ->
        AlertDialog(
            onDismissRequest = { errJob = null },
            title = {
                Text("DOWNLOAD ERROR", fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            },
            text = {
                Column {
                    Text(
                        j.title,
                        color = Wa.Green, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        j.error ?: "ralat tidak diketahui",
                        color = Color(0xFFFF5050),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Tips: kalau error 'Sign in to confirm you're not a bot' — letak " +
                            "cookies.txt di folder muzik. Kalau permission denied — buka FOLDER " +
                            "dan beri All files access.",
                        color = Wa.TextDim, fontSize = 9.sp, fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val t = tracks[j.trackId]
                    if (t != null) DLEngine.enqueue(t)
                    errJob = null
                }) { Text("RETRY", color = Wa.Green) }
            },
            dismissButton = {
                TextButton(onClick = { errJob = null }) { Text("CLOSE", color = Wa.TextDim) }
            }
        )
    }

    Column(modifier = modifier) {
        TitleBar("MP3NYOK PLAYLIST  —  ${playlist?.name ?: "-"}")

        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF0B0B0B))
        ) {
            itemsIndexed(plTracks) { i, t ->
                PlaylistRow(
                    index = i + 1,
                    track = t,
                    job = downloads[t.id],
                    selected = selected == i,
                    playing = playingTrack?.id == t.id,
                    onErrClick = { errJob = it },
                    onClick = {
                        selected = i
                        PlayerHolder.playTrack(t, plTracks, i)
                    }
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .background(Wa.Bg)
                .padding(start = 6.dp, end = 6.dp, top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            WaButton("ADD", Modifier.weight(1f), onClick = onAddUrl)
            WaButton("REM", Modifier.weight(1f), enabled = selected != null) {
                val pl = playlist ?: return@WaButton
                selected?.let { sel ->
                    plTracks.getOrNull(sel)?.let { t ->
                        Store.removeTrack(pl.id, t.id)
                        selected = null
                    }
                }
            }
            WaButton("RENAME", Modifier.weight(1.4f), enabled = selected != null) {
                selected?.let { sel ->
                    plTracks.getOrNull(sel)?.let { t -> onRenameTrack(t.id) }
                }
            }
            WaButton("DL", Modifier.weight(1f), enabled = selected != null) {
                selected?.let { sel ->
                    plTracks.getOrNull(sel)?.let { t ->
                        if (t.filePath == null) DLEngine.enqueue(t)
                    }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(Wa.Bg)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            WaButton("DL ALL", Modifier.weight(1f)) {
                plTracks.filter { it.filePath == null }.forEach { DLEngine.enqueue(it) }
            }
            WaButton("MANAGE", Modifier.weight(1f), onClick = onManage)
            WaButton("FOLDER", Modifier.weight(1f), onClick = onSettings)
        }
    }
}

@Composable
private fun PlaylistRow(
    index: Int,
    track: Track,
    job: DLEngine.Job?,
    selected: Boolean,
    playing: Boolean,
    onErrClick: (DLEngine.Job) -> Unit,
    onClick: () -> Unit
) {
    val bg = if (selected) Wa.SelectBlue else Color.Transparent
    val fg = if (selected) Color.White else Wa.Green
    Row(
        Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (playing) " >  " else "%3d. ".format(index),
            color = if (selected) Color.White else Wa.Yellow,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = track.fileName.ifBlank { track.title },
            color = fg,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 2.dp)
        )
        when {
            job != null && (job.state == DLEngine.State.RUNNING || job.state == DLEngine.State.QUEUED) ->
                Text(
                    "${job.progress.toInt()}%",
                    color = Wa.Orange,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            job != null && job.state == DLEngine.State.FAILED ->
                Text(
                    "ERR",
                    color = Color(0xFFFF5050),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { onErrClick(job) }
                )
            track.filePath != null ->
                Text(
                    "MP3",
                    color = if (selected) Color.White else Wa.Green,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            else ->
                Text(
                    "WEB",
                    color = Wa.TextDim,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
        }
        Text(
            text = fmtDur(track.durationSec),
            color = if (selected) Color.White else Wa.TextDim,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
