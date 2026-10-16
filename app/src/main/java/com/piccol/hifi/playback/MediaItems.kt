package com.piccol.hifi.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.piccol.hifi.data.SavedItem
import com.piccol.hifi.data.Track

fun Track.mediaItem(): MediaItem = SavedItem(uri, title, artist, album, artwork).mediaItem()
fun SavedItem.mediaItem(): MediaItem = MediaItem.Builder().setMediaId(uri).setUri(uri)
    .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).setAlbumTitle(album)
        .setArtworkUri(artwork?.let(Uri::parse)).build()).build()
fun MediaItem.saved(): SavedItem = SavedItem(mediaId, mediaMetadata.title?.toString().orEmpty(),
    mediaMetadata.artist?.toString().orEmpty(), mediaMetadata.albumTitle?.toString().orEmpty(), mediaMetadata.artworkUri?.toString())
