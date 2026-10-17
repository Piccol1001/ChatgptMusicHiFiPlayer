package com.piccol.hifi.dsp

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.*
import org.json.*

private val Context.audioStore by preferencesDataStore("audio_dsp")

data class EqPreset(
    val id: String,
    val name: String,
    val category: String,
    val settings: DspSettings,
    val description: String = "Custom settings",
)

class DspRepository(context: Context) {
    private val store = context.audioStore
    private val data =
        store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
    val settings =
        data.map { decodeSettings(it[stringPreferencesKey("settings")]) }.distinctUntilChanged()
    val custom = data.map { decodePresets(it[stringPreferencesKey("presets")]) }
    val favorites = data.map { it[stringSetPreferencesKey("favorites")] ?: emptySet() }
    val recent = data.map { it[stringPreferencesKey("recent")]?.split('|') ?: emptyList() }

    suspend fun update(transform: (DspSettings) -> DspSettings) {
        store.edit {
            val next = transform(decodeSettings(it[stringPreferencesKey("settings")]))
            require(next.valid())
            it[stringPreferencesKey("settings")] = encodeSettings(next)
        }
    }

    suspend fun apply(preset: EqPreset) {
        update { old ->
            preset.settings.copy(
                enabled = true,
                preset = preset.name,
                replayMode = old.replayMode,
                replayPreamp = old.replayPreamp,
                preventClipping = old.preventClipping,
            )
        }
        rememberPreset(preset.id)
    }

    suspend fun rememberPreset(id: String) {
        store.edit {
            it[stringPreferencesKey("recent")] =
                ((listOf(id) + (it[stringPreferencesKey("recent")]?.split('|') ?: emptyList()))
                        .distinct()
                        .take(10))
                    .joinToString("|")
        }
    }

    suspend fun save(name: String, settings: DspSettings) {
        require(name.isNotBlank() && settings.valid())
        store.edit {
            val list = decodePresets(it[stringPreferencesKey("presets")])
            require(list.size < 200) { "At most 200 custom presets" }
            it[stringPreferencesKey("presets")] =
                encodePresets(
                    list +
                        EqPreset(
                            UUID.randomUUID().toString(),
                            name.trim().take(80),
                            "Custom",
                            settings,
                        )
                )
        }
    }

    suspend fun rename(id: String, name: String) {
        require(name.isNotBlank())
        store.edit {
            it[stringPreferencesKey("presets")] =
                encodePresets(
                    decodePresets(it[stringPreferencesKey("presets")]).map { p ->
                        if (p.id == id) p.copy(name = name.trim().take(80)) else p
                    }
                )
        }
    }

    suspend fun delete(id: String) {
        store.edit {
            it[stringPreferencesKey("presets")] =
                encodePresets(
                    decodePresets(it[stringPreferencesKey("presets")]).filterNot { p -> p.id == id }
                )
        }
    }

    suspend fun favorite(id: String) {
        store.edit {
            val key = stringSetPreferencesKey("favorites")
            val old = it[key] ?: emptySet()
            it[key] = if (id in old) old - id else old + id
        }
    }

    suspend fun importPresets(text: String) {
        val imported = decodePresets(text, strict = true)
        require(imported.isNotEmpty()) { "No presets in this file" }
        store.edit {
            val old = decodePresets(it[stringPreferencesKey("presets")])
            require(old.size + imported.size <= 200)
            it[stringPreferencesKey("presets")] =
                encodePresets(
                    old +
                        imported.map { p ->
                            p.copy(id = UUID.randomUUID().toString(), category = "Custom")
                        }
                )
        }
    }
}

fun encodeSettings(s: DspSettings): String =
    JSONObject()
        .apply {
            put("enabled", s.enabled)
            put("mode", s.mode.name)
            put("ten", JSONArray(s.ten))
            put("thirtyOne", JSONArray(s.thirtyOne))
            put(
                "filters",
                JSONArray().apply {
                    s.filters.forEach { f ->
                        put(
                            JSONObject().apply {
                                put("frequency", f.frequency)
                                put("gain", f.gain)
                                put("q", f.q)
                                put("type", f.type.name)
                                put("enabled", f.enabled)
                            }
                        )
                    }
                },
            )
            put("preamp", s.preamp)
            put("autoHeadroom", s.autoHeadroom)
            put("preset", s.preset)
            put("replayMode", s.replayMode.name)
            put("replayPreamp", s.replayPreamp)
            put("preventClipping", s.preventClipping)
        }
        .toString()

fun decodeSettings(text: String?, strict: Boolean = false): DspSettings {
    if (text == null) return DspSettings()
    return try {
        val o = JSONObject(text)
        fun gains(key: String) =
            o.getJSONArray(key).let { a -> (0 until a.length()).map(a::getDouble) }
        val a = o.getJSONArray("filters")
        val s =
            DspSettings(
                o.getBoolean("enabled"),
                EqMode.valueOf(o.getString("mode")),
                gains("ten"),
                gains("thirtyOne"),
                (0 until a.length()).map {
                    a.getJSONObject(it).let { f ->
                        EqFilter(
                            f.getDouble("frequency"),
                            f.getDouble("gain"),
                            f.getDouble("q"),
                            FilterType.valueOf(f.getString("type")),
                            f.getBoolean("enabled"),
                        )
                    }
                },
                o.getDouble("preamp"),
                o.getBoolean("autoHeadroom"),
                o.optString("preset", "Custom"),
                ReplayMode.valueOf(o.getString("replayMode")),
                o.getDouble("replayPreamp"),
                o.getBoolean("preventClipping"),
            )
        require(s.valid()) { "Invalid EQ values" }
        s
    } catch (e: Exception) {
        if (strict) throw IllegalArgumentException("Invalid DSP configuration", e)
        else DspSettings()
    }
}

fun encodePresets(presets: List<EqPreset>): String =
    JSONObject()
        .apply {
            put("version", 1)
            put(
                "presets",
                JSONArray().apply {
                    presets.forEach { p ->
                        put(
                            JSONObject().apply {
                                put("name", p.name)
                                put("id", p.id)
                                put("settings", JSONObject(encodeSettings(p.settings)))
                            }
                        )
                    }
                },
            )
        }
        .toString(2)

fun decodePresets(text: String?, strict: Boolean = false): List<EqPreset> {
    if (text == null) return emptyList()
    return try {
        require(text.toByteArray().size <= 2097152) { "Preset file exceeds 2 MiB" }
        val root = JSONObject(text)
        require(root.getInt("version") == 1) { "Unsupported preset version" }
        val a = root.getJSONArray("presets")
        require(a.length() <= 200)
        (0 until a.length()).map {
            a.getJSONObject(it).let { p ->
                val name = p.getString("name")
                require(name.isNotBlank() && name.length <= 80)
                EqPreset(
                    p.optString("id", UUID.randomUUID().toString()),
                    name,
                    "Custom",
                    decodeSettings(p.getJSONObject("settings").toString(), true),
                )
            }
        }
    } catch (e: Exception) {
        if (strict) throw IllegalArgumentException("Invalid preset file: ${e.message}", e)
        else emptyList()
    }
}
