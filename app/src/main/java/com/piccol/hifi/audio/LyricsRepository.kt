package com.piccol.hifi.audio

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.piccol.hifi.library.readLimited
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.ArrayDeque
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

private val Context.lyricStore by preferencesDataStore("lyrics")

data class LyricOptions(val font: Float = 22f, val autoScroll: Boolean = true)

data class LoadedLyrics(val lyrics: Lyrics, val source: String)

class LyricsRepository(private val context: Context) {
    private val store = context.lyricStore
    private val data =
        store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
    val options =
        data
            .map {
                LyricOptions(
                    it[floatPreferencesKey("font")] ?: 22f,
                    it[booleanPreferencesKey("scroll")] ?: true,
                )
            }
            .distinctUntilChanged()
    val changes = MutableStateFlow(0)

    private fun key(song: String) =
        MessageDigest.getInstance("SHA-256").digest(song.toByteArray()).joinToString("") {
            "%02x".format(it)
        }

    private fun file(song: String) = File(File(context.filesDir, "lyrics"), "${key(song)}.txt")

    fun offset(song: String) =
        data.map { it[longPreferencesKey("offset_${key(song)}")] ?: 0L }.distinctUntilChanged()

    suspend fun offset(song: String, value: Long) {
        store.edit { it[longPreferencesKey("offset_${key(song)}")] = value.coerceIn(-60000, 60000) }
    }

    suspend fun options(transform: (LyricOptions) -> LyricOptions) {
        store.edit {
            val next =
                transform(
                    LyricOptions(
                        it[floatPreferencesKey("font")] ?: 22f,
                        it[booleanPreferencesKey("scroll")] ?: true,
                    )
                )
            it[floatPreferencesKey("font")] = next.font.coerceIn(14f, 40f)
            it[booleanPreferencesKey("scroll")] = next.autoScroll
        }
    }

    suspend fun import(song: String, uri: Uri) =
        withContext(Dispatchers.IO) {
            val text =
                context.contentResolver.openInputStream(uri)?.use { readLimited(it) }
                    ?: throw IOException("Cannot read lyrics")
            save(song, text)
        }

    private fun save(song: String, text: String) {
        require(parseLyrics(text).lines.any { it.text.isNotBlank() }) { "The lyric file is empty" }
        val f = file(song)
        f.parentFile!!.mkdirs()
        require(f.exists() || (f.parentFile!!.list()?.size ?: 0) < 1000) {
            "At most 1,000 local lyric copies"
        }
        val temp = File(f.parentFile, "${f.name}.tmp")
        temp.writeText(text)
        check(temp.renameTo(f)) { "Could not save lyrics" }
        changes.value++
    }

    suspend fun remove(song: String) =
        withContext(Dispatchers.IO) {
            file(song).delete()
            changes.value++
        }

    suspend fun load(song: String, embedded: String?, folders: Set<String>): LoadedLyrics? =
        withContext(Dispatchers.IO) {
            val f = file(song)
            if (f.exists())
                return@withContext LoadedLyrics(parseLyrics(f.readText()), "Imported local lyrics")
            val matched =
                try {
                    match(song, folders)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
            if (matched != null) {
                val text =
                    runCatching {
                            context.contentResolver.openInputStream(matched)?.use {
                                readLimited(it)
                            }
                        }
                        .getOrNull()
                if (!text.isNullOrBlank())
                    return@withContext LoadedLyrics(
                        parseLyrics(text),
                        "Matched in selected music folder",
                    )
            }
            embedded
                ?.takeIf { it.isNotBlank() }
                ?.let { LoadedLyrics(parseLyrics(it), "Embedded lyrics") }
        }

    /**
     * Matches only a sibling filename in a user-authorized SAF tree; never searches arbitrary
     * storage.
     */
    private suspend fun match(song: String, folders: Set<String>): Uri? {
        var examined = 0
        for (folder in folders) {
            val tree = Uri.parse(folder)
            val pending = ArrayDeque<String>()
            pending.add(DocumentsContract.getTreeDocumentId(tree))
            val visited = mutableSetOf<String>()
            while (pending.isNotEmpty() && examined < 10000) {
                currentCoroutineContext().ensureActive()
                val parent = pending.removeFirst()
                if (!visited.add(parent)) continue
                data class Child(val id: String, val name: String, val mime: String)
                val children = mutableListOf<Child>()
                context.contentResolver
                    .query(
                        DocumentsContract.buildChildDocumentsUriUsingTree(tree, parent),
                        arrayOf("document_id", "_display_name", "mime_type"),
                        null,
                        null,
                        null,
                    )
                    ?.use { c ->
                        while (c.moveToNext() && examined++ < 10000) {
                            val child =
                                Child(
                                    c.getString(0),
                                    c.getString(1).orEmpty(),
                                    c.getString(2).orEmpty(),
                                )
                            if (child.mime == DocumentsContract.Document.MIME_TYPE_DIR)
                                pending.add(child.id)
                            else children += child
                        }
                    }
                val audio =
                    children.find { child ->
                        val uri = DocumentsContract.buildDocumentUriUsingTree(tree, child.id)
                        uri.toString() == song ||
                            (Build.VERSION.SDK_INT >= 29 &&
                                com.piccol.hifi.library.isAudioFile(child.name, child.mime) &&
                                runCatching {
                                        MediaStore.getMediaUri(context, uri)?.toString() == song
                                    }
                                    .getOrDefault(false))
                    }
                if (audio != null) {
                    val base = audio.name.substringBeforeLast('.')
                    for (extension in listOf("lrc", "txt")) {
                        val candidates =
                            children.filter { it.name.equals("$base.$extension", true) }
                        if (candidates.size == 1)
                            return DocumentsContract.buildDocumentUriUsingTree(
                                tree,
                                candidates.single().id,
                            )
                    }
                    return null
                }
            }
        }
        return null
    }
}
