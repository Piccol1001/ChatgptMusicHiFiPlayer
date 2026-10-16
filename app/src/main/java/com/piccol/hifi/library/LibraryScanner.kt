package com.piccol.hifi.library

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.piccol.hifi.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.ArrayDeque

data class ScanState(val running: Boolean = false, val count: Int = 0, val message: String = "")

class LibraryScanner(private val context: Context, private val dao: MusicDao, private val preferences: Preferences) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow(ScanState())
    val state = _state.asStateFlow()
    fun hasMediaPermission(): Boolean = ContextCompat.checkSelfPermission(context,
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    suspend fun scan() = withContext(Dispatchers.IO) {
        mutex.withLock {
            _state.value = ScanState(running = true)
            var count = 0
            val warnings = mutableListOf<String>()
            try {
                if (hasMediaPermission()) {
                    try {
                        val media = scanMedia()
                        dao.replaceSource("media", media)
                        count += media.size
                    } catch (_: SecurityException) { warnings += "Music permission was revoked." }
                    catch (_: RuntimeException) { warnings += "Device music could not be refreshed." }
                } else { dao.markUnavailable("media") }
                preferences.folders.first().forEach { folder ->
                    try {
                        val tracks = scanFolder(Uri.parse(folder))
                        dao.replaceSource(folder, tracks)
                        count += tracks.size
                    } catch (_: SecurityException) {
                        dao.markUnavailable(folder)
                        warnings += "A folder needs access again. Select it in Settings."
                    } catch (_: java.io.IOException) { warnings += "A folder is unavailable; reconnect its storage." }
                    catch (_: IllegalArgumentException) { warnings += "A folder provider could not be read." }
                }
                if (!hasMediaPermission()) warnings += "Device scan needs music access. Selected folders remain available."
            } finally {
                _state.value = ScanState(false, count, warnings.joinToString(" ").ifEmpty { "$count audio files indexed" })
            }
        }
    }

    private fun scanMedia(): List<Track> {
        val roots = if (Build.VERSION.SDK_INT >= 29) MediaStore.getExternalVolumeNames(context).map(MediaStore.Audio.Media::getContentUri)
            else listOf(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        val columns = mutableListOf("_id", "title", "artist", "album", "duration", "_size", "date_added", "mime_type", "album_id", "track", "_display_name")
        if (Build.VERSION.SDK_INT >= 29) columns += "relative_path"
        if (Build.VERSION.SDK_INT >= 30) { columns += "genre"; columns += "album_artist" }
        val tracks = mutableListOf<Track>()
        for (root in roots) {
        val result = context.contentResolver.query(root, columns.toTypedArray(), null, null, null)
            ?: throw java.io.IOException("Media provider returned no cursor")
        result.use { c ->
            while (c.moveToNext()) {
                val id = c.long("_id")
                val number = c.long("track").toInt()
                val albumId = c.long("album_id")
                tracks += Track(
                    uri = ContentUris.withAppendedId(root, id).toString(),
                    title = c.string("title").ifBlank { c.string("_display_name").substringBeforeLast('.') },
                    artist = cleanTag(c.string("artist"), "Unknown artist"),
                    album = cleanTag(c.string("album"), "Unknown album"),
                    albumArtist = c.string("album_artist"), genre = cleanTag(c.string("genre"), "Unknown genre"),
                    trackNumber = number % 1000, discNumber = number / 1000,
                    duration = c.long("duration"), size = c.long("_size"), added = c.long("date_added") * 1000,
                    mime = c.string("mime_type"), folder = c.string("relative_path").ifBlank { "Device music" },
                    artwork = if (albumId > 0) "${root.toString().substringBeforeLast('/')}/albumart/$albumId" else null,
                )
            }
        }
        }
        return tracks
    }

    private suspend fun scanFolder(tree: Uri): List<Track> {
        val resolver = context.contentResolver
        val pending = ArrayDeque<Pair<String, String>>()
        pending.add(DocumentsContract.getTreeDocumentId(tree) to "")
        val visited = mutableSetOf<String>()
        val tracks = mutableListOf<Track>()
        while (pending.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val (id, path) = pending.removeFirst()
            if (!visited.add(id)) continue
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, id)
            val cursor = resolver.query(children, arrayOf("document_id", "_display_name", "mime_type", "_size", "last_modified"), null, null, null)
                ?: throw java.io.IOException("Folder unavailable")
            cursor.use { c ->
                while (c.moveToNext()) {
                    currentCoroutineContext().ensureActive()
                    val child = c.string("document_id")
                    val name = c.string("_display_name")
                    val mime = c.string("mime_type")
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) { pending.add(child to "$path$name/"); continue }
                    if (!isAudioFile(name, mime)) continue
                    val document = DocumentsContract.buildDocumentUriUsingTree(tree, child)
                    val media = if (Build.VERSION.SDK_INT >= 29) runCatching { MediaStore.getMediaUri(context, document) }.getOrNull() else null
                    // Prefer the MediaStore identity when the provider can map it. Never deduplicate by filename.
                    val existing = media?.let { dao.track(it.toString()) }
                    if (existing?.available == true && existing.source == "media") continue
                    val uri = media ?: document
                    val tags = metadata(document)
                    tracks += Track(uri.toString(), tags["title"].orEmpty().ifBlank { name.substringBeforeLast('.') },
                        cleanTag(tags["artist"].orEmpty(), "Unknown artist"), cleanTag(tags["album"].orEmpty(), "Unknown album"),
                        tags["albumArtist"].orEmpty(), cleanTag(tags["genre"].orEmpty(), "Unknown genre"),
                        tags["track"]?.substringBefore('/')?.toIntOrNull() ?: 0, tags["disc"]?.substringBefore('/')?.toIntOrNull() ?: 0,
                        tags["duration"]?.toLongOrNull() ?: 0, c.long("_size"), c.long("last_modified"), mime,
                        "${tree.lastPathSegment?.substringAfter(':') ?: "Selected folder"}/$path", tags["artwork"], tree.toString())
                    if (tracks.size % 50 == 0) _state.value = ScanState(true, tracks.size, "Reading selected folders…")
                }
            }
        }
        return tracks.distinctBy { it.uri }
    }

    private fun metadata(uri: Uri): Map<String, String> {
        val reader = MediaMetadataRetriever()
        return try {
            reader.setDataSource(context, uri)
            val tags = mapOf("title" to 7, "artist" to 2, "album" to 1, "albumArtist" to 13, "genre" to 6, "track" to 0, "disc" to 14, "duration" to 9)
                .mapValues { reader.extractMetadata(it.value).orEmpty() }.toMutableMap()
            reader.embeddedPicture?.let { bytes ->
                val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                val options = android.graphics.BitmapFactory.Options().apply {
                    inSampleSize = Integer.highestOneBit((maxOf(bounds.outWidth, bounds.outHeight) / 512).coerceAtLeast(1))
                }
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.let { bitmap ->
                    val dir = java.io.File(context.cacheDir, "artwork").apply { mkdirs() }
                    val key = java.security.MessageDigest.getInstance("SHA-256").digest(uri.toString().toByteArray()).joinToString("") { "%02x".format(it) }
                    val file = java.io.File(dir, "$key.jpg")
                    try { file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, it) }; tags["artwork"] = Uri.fromFile(file).toString() }
                    finally { bitmap.recycle() }
                }
            }
            tags
        } catch (_: RuntimeException) { emptyMap() } finally { reader.release() }
    }
}

fun cleanTag(value: String, fallback: String) = value.takeUnless { it.isBlank() || it == "<unknown>" } ?: fallback
fun isAudioFile(name: String, mime: String): Boolean = mime.startsWith("audio/") || name.substringAfterLast('.').lowercase() in setOf("mp3", "flac", "wav", "m4a", "aac", "ogg", "opus", "alac", "aif", "aiff")
private fun Cursor.string(column: String): String = getColumnIndex(column).takeIf { it >= 0 }?.let { getString(it) }.orEmpty()
private fun Cursor.long(column: String): Long = getColumnIndex(column).takeIf { it >= 0 }?.let { getLong(it) } ?: 0
