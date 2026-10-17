package com.piccol.hifi.visualization

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.*
import org.json.JSONObject

private val Context.visualStore by preferencesDataStore("visualization")

class VisualRepository(context: Context) {
    private val store = context.visualStore
    val settings =
        store.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { decode(it[stringPreferencesKey("settings")]) }
            .distinctUntilChanged()

    suspend fun update(transform: (VisualSettings) -> VisualSettings) {
        store.edit {
            val next = transform(decode(it[stringPreferencesKey("settings")]))
            require(next.valid())
            it[stringPreferencesKey("settings")] = encode(next)
        }
    }

    private fun encode(s: VisualSettings) =
        JSONObject()
            .apply {
                put("reducedMotion", s.reducedMotion)
                put("mode", s.mode.name)
                put("fft", s.fftSize)
                put("bars", s.bars)
                put("fps", s.fps)
                put("log", s.logarithmic)
                put("smoothing", s.smoothing)
                put("hold", s.holdMs)
                put("decay", s.decay)
                put("theme", s.theme)
                put("stereo", s.stereo)
                put("time", s.timeMs)
                put("amplitude", s.amplitude)
                put("vertical", s.verticalMeters)
                put("history", s.history)
                put("rotation", s.rotation)
                put("artwork", s.artwork)
                put("effect", s.effect)
                put("quality", s.quality.name)
            }
            .toString()

    private fun decode(text: String?): VisualSettings =
        try {
            val o = JSONObject(text ?: "{}")
            val parsed =
                VisualSettings(
                    VisualMode.valueOf(o.optString("mode", "SPECTRUM")),
                    o.optInt("fft", 2048).takeIf { it in listOf(512, 1024, 2048, 4096, 8192) }
                        ?: 2048,
                    o.optInt("bars", 64).coerceIn(16, 128),
                    o.optInt("fps", 30).takeIf { it == 30 || it == 60 } ?: 30,
                    o.optBoolean("log", true),
                    o.optDouble("smoothing", .6).toFloat().coerceIn(0f, .95f),
                    o.optInt("hold", 500).coerceIn(0, 2000),
                    o.optDouble("decay", 24.0).toFloat().coerceIn(3f, 96f),
                    o.optInt("theme", 0).coerceIn(0, 2),
                    o.optBoolean("stereo", true),
                    o.optInt("time", 40).coerceIn(5, 150),
                    o.optDouble("amplitude", 2.0).toFloat().coerceIn(.5f, 8f),
                    o.optBoolean("vertical"),
                    o.optInt("history", 160).coerceIn(60, 240),
                    o.optBoolean("rotation"),
                    o.optBoolean("artwork", true),
                    o.optInt("effect", 0).coerceIn(0, 2),
                    VisualQuality.valueOf(o.optString("quality", "MEDIUM")),
                    o.optBoolean("reducedMotion"),
                )
            require(parsed.valid())
            parsed
        } catch (_: Exception) {
            VisualSettings()
        }
}
