package com.piccol.hifi

import com.piccol.hifi.audio.*
import com.piccol.hifi.data.*
import com.piccol.hifi.dsp.*
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class AdvancedAudioTest {
    @Test
    fun autoEqParsesRealFiltersAndEngineMatchesPreview() {
        val imported =
            parseAutoEq(
                "# AutoEq\nPreamp: -4 dB\nFilter 1: ON PK Fc 1000 Hz Gain 6 dB Q 1.41\nFilter 2: OFF HS Fc 9000 Hz Gain -2 dB Q 0.7"
            )
        assertEquals(10, imported.settings.filters.size)
        assertFalse(imported.settings.filters[1].enabled)
        val settings = imported.settings.copy(autoHeadroom = false)
        val engine = AudioDspEngine()
        engine.update(settings)
        engine.configure(48000, 1)
        var inputEnergy = 0.0
        var outputEnergy = 0.0
        repeat(48000) { i ->
            val x = .02 * sin(2 * PI * 1000 * i / 48000)
            engine.beginFrame()
            val y = engine.sample(x, 0)
            engine.endFrame()
            if (i > 24000) {
                inputEnergy += x * x
                outputEnergy += y * y
            }
        }
        assertEquals(2.0, 10 * log10(outputEnergy / inputEnergy), .01)
        val graphic = parseAutoEq("GraphicEQ: 20 -3; 1000 2; 20000 -1")
        assertEquals(EqMode.THIRTY_ONE, graphic.settings.mode)
        assertTrue(graphic.notes.contains("approximation"))
    }

    @Test
    fun malformedAutoEqIsRejectedWithoutClampingSilently() {
        val invalid =
            listOf(
                "",
                "GraphicEQ: 100 1; 50 0",
                "GraphicEQ: 20 13; 20000 0",
                "Filter 1: ON PK Fc 1000 Hz Gain 2 dB",
                "Filter 1: ON PK Fc 1000 Hz Gain 2 dB Q 0",
                "Preamp: -99 dB",
                "Include: other.txt",
                "GraphicEQ: NaN 0; 1000 0",
            )
        invalid.forEach { text ->
            assertTrue("Must reject: $text", runCatching { parseAutoEq(text) }.isFailure)
        }
    }

    @Test
    fun lyricsHandleMultipleTimestampsHoursFractionsOffsetsAndPlainText() {
        val lrc =
            parseLyrics(
                "[ar:Artist]\n[offset:100]\n[00:01.2][00:02.34]Repeated\n[00:02.340]Also\n[1:02:03.456]Later"
            )
        assertEquals(listOf(1200L, 2340L, 2340L, 3723456L), lrc.lines.map { it.timeMs })
        assertEquals(-1, lrc.current(1299))
        assertEquals(0, lrc.current(1300))
        assertEquals(2, lrc.current(2440))
        assertEquals(-1, lrc.current(1549, 250))
        assertEquals(0, lrc.current(1550, 250))
        assertEquals(2, lrc.current(2690, 250))
        val plain = parseLyrics("A line\nAnother line")
        assertFalse(plain.synchronized)
        assertEquals(-1, plain.current(5000))
        val uslt =
            byteArrayOf(3) + "eng".toByteArray() + byteArrayOf(0) + "Offline lyrics".toByteArray()
        assertEquals("Offline lyrics", decodeUslt(uslt))
        assertNull(decodeUslt(byteArrayOf(9, 0)))
    }

    @Test
    fun aiffValidates80BitRateWidthAndCompression() {
        val common =
            byteArrayOf(
                0,
                2,
                0,
                0,
                0,
                100,
                0,
                24,
                0x40,
                0x0e,
                0xbb.toByte(),
                0x80.toByte(),
                0,
                0,
                0,
                0,
                0,
                0,
            )
        val f = aiffCommon(common, false)
        assertEquals(48000, f.rate)
        assertEquals(24, f.bits)
        assertEquals(6, f.block)
        assertTrue(
            runCatching { aiffCommon(common.copyOf().apply { this[7] = 20 }, false) }.isFailure
        )
        assertTrue(runCatching { aiffCommon(common + "fl32".toByteArray(), true) }.isFailure)
        assertTrue(aiffCommon(common + "sowt".toByteArray(), true).littleEndian)
    }

    @Test
    fun m3uUsesExactUrisAndReportsUnsupportedPaths() {
        val tracks =
            listOf(
                Track("content://music/1", "Song", "Artist", duration = 3500),
                Track("content://music/2", "Other"),
            )
        val result = parseM3u(exportM3u(tracks))
        assertEquals(tracks.map { it.uri }, result.uris)
        assertEquals(0, result.unresolved)
        assertEquals(
            2,
            parseM3u("#EXTM3U\nrelative.flac\nhttps://example.com/song.mp3\ncontent://music/1")
                .unresolved,
        )
    }
}
