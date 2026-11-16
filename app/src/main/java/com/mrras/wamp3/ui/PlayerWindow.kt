package com.mrras.wamp3.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrras.wamp3.data.Store
import com.mrras.wamp3.player.PlayerHolder
import com.mrras.wamp3.update.Updater

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerWindow(modifier: Modifier = Modifier, onUpdate: () -> Unit = {}) {
    val isPlaying by PlayerHolder.isPlaying.collectAsState()
    val current by PlayerHolder.current.collectAsState()
    val isStream by PlayerHolder.isStream.collectAsState()
    val posMs by PlayerHolder.positionMs.collectAsState()
    val durMs by PlayerHolder.durationMs.collectAsState()
    val volume by PlayerHolder.volume.collectAsState()
    val shuffle by PlayerHolder.shuffle.collectAsState()
    val repeatAll by PlayerHolder.repeatAll.collectAsState()
    val update by Updater.updateInfo.collectAsState()
    val loading by PlayerHolder.loading.collectAsState()

    val playlists by Store.playlists.collectAsState()
    val tracks by Store.tracks.collectAsState()
    val currentId by Store.currentPlaylistId.collectAsState()
    val plTracks = (playlists.firstOrNull { it.id == currentId } ?: playlists.firstOrNull())
        ?.trackIds?.mapNotNull { tracks[it] } ?: emptyList()

    var dragging by remember { mutableStateOf(false) }
    var sliderPos by remember { mutableStateOf(0f) }

    Column(
        modifier
            .fillMaxWidth()
            .background(Wa.Bg)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        TitleBar(
            "WAMP3 — YouTube Music Player",
            trailing = {
                update?.let { u ->
                    Text(
                        "UPD ${u.version.removePrefix("v")} >",
                        color = Wa.Orange,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onUpdate() }
                            .padding(start = 8.dp)
                    )
                }
            }
        )
        Spacer(Modifier.height(8.dp))

        // Marquee tajuk
        Box(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF000000))
                .border(1.dp, Wa.Border)
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            Text(
                text = current?.let {
                    (if (isStream) "[TEST STREAM] " else "") + (it.fileName.ifBlank { it.title })
                } ?: "-- stopped --",
                color = Wa.Green,
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
        }

        Spacer(Modifier.height(6.dp))

        // LED time + info
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .background(Color(0xFF000000))
                    .border(1.dp, Wa.Border)
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    text = fmtTime(posMs),
                    color = Wa.LedRed,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    when {
                        loading -> "MENGAMBIL TEST STREAM..."
                        isStream -> "TEST STREAM (belum download)"
                        else -> "MP3 FILE"
                    },
                    color = when {
                        loading -> Wa.Yellow
                        isStream -> Wa.Orange
                        else -> Wa.Green
                    },
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "TOTAL " + fmtTime(durMs) + "   VOL " + (volume * 100).toInt() + "%",
                    color = Wa.TextDim,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Seek bar
        val pos = when {
            dragging -> sliderPos
            durMs > 0 -> (posMs.toFloat() / durMs).coerceIn(0f, 1f)
            else -> 0f
        }
        Slider(
            value = pos,
            onValueChange = {
                dragging = true
                sliderPos = it
            },
            onValueChangeFinished = {
                dragging = false
                PlayerHolder.seekTo((sliderPos * durMs).toLong())
            },
            enabled = durMs > 0,
            colors = waSliderColors(),
            modifier = Modifier.fillMaxWidth()
        )

        // Volume
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "VOL",
                color = Wa.TextDim,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Slider(
                value = volume,
                onValueChange = { PlayerHolder.setVolume(it) },
                colors = waSliderColors(),
                modifier = Modifier
                    .weight(1f)
                    .height(26.dp)
            )
            Text(
                "${(volume * 100).toInt()}%",
                color = Wa.TextDim,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(Modifier.height(2.dp))

        // Transport buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WaButton("|<<", Modifier.weight(1f)) { PlayerHolder.playPrev() }
            WaButton(
                if (isPlaying) "II" else ">>",
                Modifier.weight(1f)
            ) {
                if (PlayerHolder.current.value == null) {
                    if (plTracks.isNotEmpty()) PlayerHolder.playTrack(plTracks[0], plTracks, 0)
                } else {
                    PlayerHolder.toggle()
                }
            }
            WaButton("[]", Modifier.weight(1f)) { PlayerHolder.stop() }
            WaButton(">|", Modifier.weight(1f)) { PlayerHolder.playNext() }
            WaButton(if (shuffle) "SHF*" else "SHF", Modifier.weight(1f)) {
                PlayerHolder.shuffle.value = !shuffle
            }
            WaButton(if (repeatAll) "RPT*" else "RPT", Modifier.weight(1f)) {
                PlayerHolder.repeatAll.value = !repeatAll
            }
        }
    }
}
