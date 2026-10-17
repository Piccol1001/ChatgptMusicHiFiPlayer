package com.piccol.hifi.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject

private val Context.store by preferencesDataStore("player")

data class Settings(
    val speed: Float = 1f,
    val technicalInfo: Boolean = true,
    val accent: Int = 0,
    val detailedList: Boolean = false,
)

data class SavedItem(
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val artwork: String?,
)

data class QueueSnapshot(
    val items: List<SavedItem> = emptyList(),
    val index: Int = 0,
    val position: Long = 0,
    val shuffle: Boolean = false,
    val repeat: Int = 0,
)

class Preferences(context: Context) {
    private val store = context.store
    private val values =
        store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
    val settings =
        values.map {
            Settings(
                it[floatPreferencesKey("speed")] ?: 1f,
                it[booleanPreferencesKey("technical")] ?: true,
                it[intPreferencesKey("accent")] ?: 0,
                it[booleanPreferencesKey("detailed_list")] ?: false,
            )
        }
    val folders = values.map { it[stringSetPreferencesKey("folders")] ?: emptySet() }

    suspend fun addFolder(uri: String) {
        store.edit {
            it[stringSetPreferencesKey("folders")] =
                (it[stringSetPreferencesKey("folders")] ?: emptySet()) + uri
        }
    }

    suspend fun removeFolder(uri: String) {
        store.edit {
            it[stringSetPreferencesKey("folders")] =
                (it[stringSetPreferencesKey("folders")] ?: emptySet()) - uri
        }
    }

    suspend fun speed(speed: Float) {
        store.edit { it[floatPreferencesKey("speed")] = speed.coerceIn(.5f, 2f) }
    }

    suspend fun technical(value: Boolean) {
        store.edit { it[booleanPreferencesKey("technical")] = value }
    }

    suspend fun detailedList(value: Boolean) {
        store.edit { it[booleanPreferencesKey("detailed_list")] = value }
    }

    suspend fun accent(value: Int) {
        store.edit { it[intPreferencesKey("accent")] = value }
    }

    suspend fun snapshot(): QueueSnapshot =
        decodeQueue(values.first()[stringPreferencesKey("queue")])

    suspend fun saveQueue(snapshot: QueueSnapshot) {
        store.edit { it[stringPreferencesKey("queue")] = encodeQueue(snapshot) }
    }
}

fun encodeQueue(s: QueueSnapshot): String =
    JSONObject()
        .apply {
            put("index", s.index)
            put("position", s.position)
            put("shuffle", s.shuffle)
            put("repeat", s.repeat)
            put(
                "items",
                JSONArray().apply {
                    s.items.forEach { item ->
                        put(
                            JSONObject().apply {
                                put("uri", item.uri)
                                put("title", item.title)
                                put("artist", item.artist)
                                put("album", item.album)
                                put("artwork", item.artwork)
                            }
                        )
                    }
                },
            )
        }
        .toString()

fun decodeQueue(value: String?): QueueSnapshot =
    try {
        val o = JSONObject(value ?: "{}")
        val a = o.optJSONArray("items") ?: JSONArray()
        val items =
            (0 until a.length()).mapNotNull { i ->
                a.optJSONObject(i)?.let {
                    val uri = it.optString("uri")
                    if (!uri.startsWith("content://")) null
                    else
                        SavedItem(
                            uri,
                            it.optString("title"),
                            it.optString("artist"),
                            it.optString("album"),
                            it.optString("artwork").takeIf(String::isNotBlank),
                        )
                }
            }
        QueueSnapshot(
            items,
            o.optInt("index").coerceIn(0, (items.size - 1).coerceAtLeast(0)),
            o.optLong("position").coerceAtLeast(0),
            o.optBoolean("shuffle"),
            o.optInt("repeat").coerceIn(0, 2),
        )
    } catch (_: org.json.JSONException) {
        QueueSnapshot()
    }
