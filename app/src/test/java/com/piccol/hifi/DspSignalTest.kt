package com.piccol.hifi

import com.piccol.hifi.dsp.*
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class DspSignalTest {
    private fun measuredGain(settings: DspSettings, frequency: Double = 1000.0): Double {
        val engine = AudioDspEngine()
        engine.configure(48000, 2)
        engine.update(settings)
        var inputPower = 0.0
        var outputPower = 0.0
        repeat(48000) { i ->
            val x = .05 * sin(2 * PI * frequency * i / 48000)
            engine.beginFrame()
            val y = engine.sample(x, 0)
            val right = engine.sample(0.0, 1)
            engine.endFrame()
            assertEquals("No crosstalk", 0.0, right, 1e-12)
            if (i > 24000) {
                inputPower += x * x
                outputPower += y * y
            }
        }
        return 10 * log10(outputPower / inputPower)
    }

    @Test
    fun allThreeModesChangeTheMeasuredFrequencyRegion() {
        for (mode in EqMode.entries) {
            val s =
                DspSettings(
                    enabled = true,
                    mode = mode,
                    autoHeadroom = false,
                    ten = List(10) { if (it == 5) 6.0 else 0.0 },
                    thirtyOne = List(31) { if (it == 17) 6.0 else 0.0 },
                    filters = TEN_BANDS.map { EqFilter(it, if (it == 1000.0) 6.0 else 0.0) },
                )
            assertEquals("$mode", 6.0, measuredGain(s), .08)
        }
    }

    @Test
    fun preampAndBypassAreAudibleAndNeutralRespectively() {
        assertEquals(0.0, measuredGain(DspSettings(enabled = true)), 1e-10)
        assertEquals(-6.0, measuredGain(DspSettings(enabled = true, preamp = -6.0)), .01)
        assertEquals(
            0.0,
            measuredGain(DspSettings(enabled = false, preamp = 12.0, ten = List(10) { 12.0 })),
            1e-10,
        )
    }

    @Test
    fun everyFilterHasExpectedShapeAndStablePoles() {
        for (rate in listOf(44100, 48000, 96000)) for (type in FilterType.entries) for (q in
            listOf(.1, 1.0, 18.0)) {
            val c = Biquad.design(EqFilter(1000.0, 6.0, q, type), rate)
            assertTrue(1 + c.a1 + c.a2 > 0)
            assertTrue(1 - c.a1 + c.a2 > 0)
            assertTrue(1 - c.a2 > 0)
        }
        val low = Biquad.design(EqFilter(1000.0, type = FilterType.LOW_PASS), 48000)
        val high = Biquad.design(EqFilter(1000.0, type = FilterType.HIGH_PASS), 48000)
        assertTrue(low.magnitude(100.0, 48000) > low.magnitude(10000.0, 48000) * 50)
        assertTrue(high.magnitude(10000.0, 48000) > high.magnitude(100.0, 48000) * 50)
        assertEquals(
            6.0,
            20 * log10(Biquad.design(EqFilter(1000.0, 6.0), 48000).magnitude(1000.0, 48000)),
            1e-8,
        )
        assertEquals(Biquad(), Biquad.design(EqFilter(20000.0, 12.0), 32000))
    }

    @Test
    fun transitionsRemainFiniteAndBypassSettlesToExactDrySignal() {
        val engine = AudioDspEngine()
        engine.configure(48000, 1)
        repeat(12000) { i ->
            if (i % 2000 == 0)
                engine.update(
                    DspSettings(
                        enabled = true,
                        mode = EqMode.PARAMETRIC,
                        filters =
                            TEN_BANDS.map { EqFilter(it, if (i % 4000 == 0) 12.0 else -12.0, 18.0) },
                    )
                )
            engine.beginFrame()
            assertTrue(engine.sample(.05 * sin(i * .1), 0).isFinite())
            engine.endFrame()
        }
        engine.update(DspSettings())
        repeat(2000) {
            engine.beginFrame()
            engine.sample(.123, 0)
            engine.endFrame()
        }
        engine.beginFrame()
        assertEquals(.123, engine.sample(.123, 0), 0.0)
    }

    @Test
    fun automaticHeadroomIncludesCumulativeResponse() {
        val s = DspSettings(enabled = true, ten = List(10) { 6.0 })
        assertTrue(effectivePreamp(s, 48000) < -6.0)
        assertTrue(effectivePreamp(s, 48000) + maxBoost(s.activeFilters(), 48000) < 1e-8)
    }

    @Test
    fun replayGainUsesMatchingPeakAndDoesNotInventMissingGain() {
        val tags =
            parseReplayTags(
                mapOf(
                    "replaygain_track_gain" to "-6.0 dB",
                    "REPLAYGAIN_ALBUM_GAIN" to "3.0 dB",
                    "REPLAYGAIN_ALBUM_PEAK" to "1.5",
                )
            )
        assertEquals(-6.0, replayGain(DspSettings(replayMode = ReplayMode.TRACK), tags), 0.0)
        assertEquals(
            -20 * log10(1.5),
            replayGain(DspSettings(replayMode = ReplayMode.ALBUM), tags),
            1e-10,
        )
        assertEquals(
            0.0,
            replayGain(
                DspSettings(replayMode = ReplayMode.TRACK, replayPreamp = 5.0),
                ReplayTags(),
            ),
            0.0,
        )
    }

    @Test
    fun presetsAreValidNeutralAndInterpolationRespectsAnchors() {
        assertTrue(BuiltInPresets.all.size >= 50)
        assertEquals(BuiltInPresets.all.size, BuiltInPresets.all.map { it.id }.distinct().size)
        assertTrue(BuiltInPresets.all.all { it.settings.valid() })
        for (name in listOf("Flat", "Reference")) assertTrue(
            BuiltInPresets.all.first { it.name == name }.settings.ten.all { it == 0.0 }
        )
        assertEquals(
            listOf(0.0, 3.0, 6.0),
            interpolateCurve(
                listOf(100.0, 10000.0),
                listOf(0.0, 6.0),
                listOf(100.0, 1000.0, 10000.0),
            ),
        )
    }
}
