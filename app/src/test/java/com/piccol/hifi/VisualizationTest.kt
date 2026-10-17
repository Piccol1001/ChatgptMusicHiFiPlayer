package com.piccol.hifi

import com.piccol.hifi.dsp.PcmTap
import com.piccol.hifi.visualization.*
import kotlin.math.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test

class VisualizationTest {
    @Test
    fun fftLocatesAndMeasuresKnownTonesAcrossRatesAndSizes() {
        for (rate in listOf(44100, 48000, 96000)) for (size in listOf(512, 2048, 8192)) {
            val bin = size / 16
            val amplitude = .25
            val samples = FloatArray(size) { (amplitude * sin(2 * PI * bin * it / size)).toFloat() }
            val magnitudes = FftAnalyzer(size).analyze(samples)
            assertEquals(bin, magnitudes.indices.maxBy { magnitudes[it] })
            assertEquals(-12.0412, amplitudeDb(magnitudes[bin].toDouble()).toDouble(), .002)
            val frequency = bin.toDouble() * rate / size
            val bands = spectrumBands(magnitudes, rate, size, 64, true)
            val peak = bands.indices.maxBy { bands[it] }
            val low = 20.0 * (min(20000.0, rate * .5) / 20).pow(peak / 64.0)
            val high = 20.0 * (min(20000.0, rate * .5) / 20).pow((peak + 1) / 64.0)
            assertTrue(
                frequency >= low - rate.toDouble() / size &&
                    frequency <= high + rate.toDouble() / size
            )
        }
        assertTrue(FftAnalyzer(1024).analyze(FloatArray(1024)).all { it == 0f })
    }

    @Test
    fun stereoMetersKeepChannelsSeparateAndCancelReleasesTap() = runBlocking {
        val tap = PcmTap()
        val playing = MutableStateFlow(true)
        val received = mutableListOf<VisualFrame>()
        val job = launch {
            VisualizationEngine(tap, playing)
                .frames(VisualSettings(smoothing = 0f, fftSize = 512, fps = 60))
                .collect { received += it }
        }
        yield()
        assertTrue(tap.enabled)
        repeat(8) {
            repeat(1024) { i -> tap.offer((.5 * sin(2 * PI * 32 * i / 512)).toFloat(), 0f) }
            delay(20)
        }
        val frame = received.last { it.playing }
        assertEquals(-6.0206, frame.peakL.toDouble(), .01)
        assertEquals(-96f, frame.peakR, 0f)
        assertEquals(-96f, frame.rmsR, 0f)
        assertTrue(frame.rmsL in -20f..-9f)
        assertTrue(frame.spectrum.max() > -10f) // a silent right channel must not cancel left
        playing.value = false
        delay(40)
        assertFalse(tap.enabled)
        assertFalse(received.last().playing)
        job.cancelAndJoin()
        assertFalse(tap.enabled)
    }

    @Test
    fun tapWrapAndSeekDoNotExposeStaleSamples() {
        val tap = PcmTap(16)
        tap.enabled = true
        repeat(40) { tap.offer(it.toFloat(), -it.toFloat()) }
        val l = FloatArray(8)
        val r = FloatArray(8)
        assertEquals(8, tap.copyLatest(l, r))
        assertEquals(32f, l[0], 0f)
        assertEquals(-39f, r[7], 0f)
        val generation = tap.generation
        tap.clear()
        assertEquals(generation + 1, tap.generation)
        assertEquals(0, tap.copyLatest(l, r))
        tap.offer(1f, 2f)
        assertEquals(1, tap.copyLatest(l, r))
        assertEquals(2f, r[0], 0f)
    }
}
