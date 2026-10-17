package com.piccol.hifi.audio

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.piccol.hifi.dsp.*
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.*
import org.json.*

private val Context.profileStore by preferencesDataStore("output_profiles")

data class CorrectionProfile(
    val id: String,
    val name: String,
    val settings: DspSettings,
    val notes: String = "",
)

data class ProfileState(
    val profiles: List<CorrectionProfile> = emptyList(),
    val manual: String? = null,
    val assignments: Map<OutputCategory, String> = emptyMap(),
) {
    fun active(route: OutputRoute): CorrectionProfile? {
        val id = route.device?.category?.let { assignments[it] } ?: manual
        return profiles.find { it.id == id }
    }
}

fun effectiveDsp(base: DspSettings, state: ProfileState, route: OutputRoute): DspSettings =
    state.active(route)?.let { p ->
        p.settings.copy(
            preset = p.name,
            replayMode = base.replayMode,
            replayPreamp = base.replayPreamp,
            preventClipping = base.preventClipping,
        )
    } ?: base

class ProfileRepository(context: Context) {
    private val store = context.profileStore
    val state =
        store.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { decodeProfiles(it[stringPreferencesKey("profiles")]) }
            .distinctUntilChanged()

    private suspend fun change(transform: (ProfileState) -> ProfileState) {
        store.edit {
            it[stringPreferencesKey("profiles")] =
                encodeProfiles(transform(decodeProfiles(it[stringPreferencesKey("profiles")])))
        }
    }

    suspend fun save(name: String, settings: DspSettings, notes: String = "") {
        require(name.isNotBlank() && settings.valid())
        change {
            require(it.profiles.size < 100)
            it.copy(
                profiles =
                    it.profiles +
                        CorrectionProfile(
                            UUID.randomUUID().toString(),
                            name.trim().take(80),
                            settings,
                            notes.take(1000),
                        )
            )
        }
    }

    suspend fun rename(id: String, name: String) {
        require(name.isNotBlank())
        change {
            it.copy(
                profiles =
                    it.profiles.map { p ->
                        if (p.id == id) p.copy(name = name.trim().take(80)) else p
                    }
            )
        }
    }

    suspend fun edit(id: String, transform: (DspSettings) -> DspSettings) {
        change {
            it.copy(
                profiles =
                    it.profiles.map { p ->
                        if (p.id == id) {
                            val s = transform(p.settings)
                            require(s.valid())
                            p.copy(settings = s)
                        } else p
                    }
            )
        }
    }

    suspend fun delete(id: String) {
        change {
            it.copy(
                profiles = it.profiles.filterNot { p -> p.id == id },
                manual = it.manual?.takeUnless { m -> m == id },
                assignments = it.assignments.filterValues { v -> v != id },
            )
        }
    }

    suspend fun select(id: String?) {
        change {
            require(id == null || it.profiles.any { p -> p.id == id })
            it.copy(manual = id)
        }
    }

    suspend fun assign(category: OutputCategory, id: String?) {
        change {
            require(id == null || it.profiles.any { p -> p.id == id })
            it.copy(
                assignments =
                    if (id == null) it.assignments - category else it.assignments + (category to id)
            )
        }
    }
}

fun encodeProfiles(s: ProfileState) =
    JSONObject()
        .apply {
            put("version", 1)
            put("manual", s.manual)
            put(
                "assignments",
                JSONObject().apply { s.assignments.forEach { (k, v) -> put(k.name, v) } },
            )
            put(
                "profiles",
                JSONArray().apply {
                    s.profiles.forEach { p ->
                        put(
                            JSONObject().apply {
                                put("id", p.id)
                                put("name", p.name)
                                put("settings", JSONObject(encodeSettings(p.settings)))
                                put("notes", p.notes)
                            }
                        )
                    }
                },
            )
        }
        .toString()

fun decodeProfiles(text: String?): ProfileState =
    try {
        if (text == null) ProfileState()
        else {
            val o = JSONObject(text)
            require(o.getInt("version") == 1)
            val a = o.getJSONArray("profiles")
            require(a.length() <= 100)
            val profiles =
                (0 until a.length()).map {
                    a.getJSONObject(it).let { p ->
                        CorrectionProfile(
                            p.getString("id"),
                            p.getString("name"),
                            decodeSettings(p.getJSONObject("settings").toString(), true),
                            p.optString("notes"),
                        )
                    }
                }
            val assignments = o.optJSONObject("assignments") ?: JSONObject()
            ProfileState(
                profiles,
                o.optString("manual").takeIf { m -> profiles.any { it.id == m } },
                OutputCategory.entries
                    .mapNotNull { k ->
                        assignments
                            .optString(k.name)
                            .takeIf { v -> profiles.any { it.id == v } }
                            ?.let { k to it }
                    }
                    .toMap(),
            )
        }
    } catch (_: Exception) {
        ProfileState()
    }
