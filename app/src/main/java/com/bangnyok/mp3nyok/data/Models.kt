package com.bangnyok.mp3nyok.data

import kotlinx.serialization.Serializable

/** Satu lagu (listing) dalam playlist. */
@Serializable
data class Track(
    val id: String,
    val playlistId: String,
    val title: String,          // tajuk asal dari YouTube
    val sourceUrl: String,      // URL video (untuk download / test stream)
    val ytid: String = "",      // id video (untuk padankan fail output)
    val fileName: String = "",  // nama fail MP3 dikehendaki user (tanpa .mp3)
    val filePath: String? = null, // set bila dah download
    val durationSec: Double? = null
)

/** Input baru dari dialog Add URL. */
@Serializable
data class NewTrack(
    val title: String,
    val sourceUrl: String,
    val ytid: String = "",
    val durationSec: Double? = null
)

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val trackIds: List<String> = emptyList()
)

@Serializable
internal data class LibraryFile(
    val playlists: List<Playlist> = emptyList(),
    val tracks: Map<String, Track> = emptyMap(),
    val currentPlaylistId: String? = null,
    val musicFolder: String = ""
)
