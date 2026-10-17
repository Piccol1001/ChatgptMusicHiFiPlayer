package com.piccol.hifi

import com.piccol.hifi.dsp.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DspPersistenceTest {
    @Test
    fun settingsAndCustomPresetsSurviveRepositoryRecreation() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val a = DspRepository(context)
        val s =
            DspSettings(
                enabled = true,
                mode = EqMode.THIRTY_ONE,
                preamp = -4.0,
                replayMode = ReplayMode.ALBUM,
            )
        a.update { s }
        a.save("Persistent", s)
        val b = DspRepository(context)
        assertEquals(s, b.settings.first())
        assertTrue(b.custom.first().any { it.name == "Persistent" && it.settings == s })
        val p = b.custom.first().last()
        b.rename(p.id, "Renamed")
        assertEquals("Renamed", b.custom.first().last().name)
        b.delete(p.id)
        assertFalse(b.custom.first().any { it.id == p.id })
    }

    @Test
    fun presetSerializationIsValidatedAndMalformedImportsDoNotOverwrite() = runBlocking {
        val s =
            DspSettings(
                enabled = true,
                filters = TEN_BANDS.map { EqFilter(it, 2.0, .7, FilterType.LOW_SHELF) },
            )
        assertEquals(s, decodeSettings(encodeSettings(s), true))
        val text = encodePresets(listOf(EqPreset("a", "Round trip", "Custom", s)))
        assertEquals(s, decodePresets(text, true).single().settings)
        val r = DspRepository(RuntimeEnvironment.getApplication())
        val before = r.custom.first()
        try {
            r.importPresets("{bad")
            fail("Malformed import accepted")
        } catch (_: IllegalArgumentException) {}
        assertEquals(before, r.custom.first())
        try {
            decodeSettings(
                encodeSettings(
                    s.copy(
                        filters =
                            s.filters.mapIndexed { i, f ->
                                if (i == 0) f.copy(frequency = 999999.0) else f
                            }
                    )
                ),
                true,
            )
            fail("Invalid frequency accepted")
        } catch (_: IllegalArgumentException) {}
    }
}
