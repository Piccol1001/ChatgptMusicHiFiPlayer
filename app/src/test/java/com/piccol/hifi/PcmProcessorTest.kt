package com.piccol.hifi

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import com.piccol.hifi.dsp.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@UnstableApi
class PcmProcessorTest {
    @Test
    fun emptyInputAfterDecoderResetDoesNotCopyTheSharedSentinel() {
        val p = EqualizerProcessor(AudioDspEngine(), PcmTap())
        p.configure(AudioProcessor.AudioFormat(48000, 2, C.ENCODING_PCM_16BIT))
        p.flush(AudioProcessor.StreamMetadata.DEFAULT)
        p.queueInput(AudioProcessor.EMPTY_BUFFER)
        assertFalse(p.output.hasRemaining())
    }

    @Test
    fun bypassIsByteIdenticalAcrossSignedPcmRange() {
        val engine = AudioDspEngine()
        val processor = EqualizerProcessor(engine, PcmTap())
        processor.configure(AudioProcessor.AudioFormat(48000, 2, C.ENCODING_PCM_16BIT))
        processor.flush(AudioProcessor.StreamMetadata.DEFAULT)
        val input = ByteBuffer.allocateDirect(131072).order(ByteOrder.LITTLE_ENDIAN)
        for (i in Short.MIN_VALUE..Short.MAX_VALUE) input.putShort(i.toShort())
        input.flip()
        val expected = ByteArray(input.remaining())
        input.duplicate().get(expected)
        processor.queueInput(input)
        val output = processor.output
        val actual = ByteArray(output.remaining())
        output.get(actual)
        assertArrayEquals(expected, actual)
        assertEquals(32768L, engine.framesProcessed)
    }

    @Test
    fun sampleRateChangesReconfigureBothDspAndTap() {
        val engine = AudioDspEngine()
        val tap = PcmTap()
        val p = EqualizerProcessor(engine, tap)
        for (rate in listOf(44100, 96000, 32000)) {
            p.configure(AudioProcessor.AudioFormat(rate, 1, C.ENCODING_PCM_16BIT))
            p.flush(AudioProcessor.StreamMetadata.DEFAULT)
            assertEquals(rate, engine.sampleRate)
            assertEquals(rate, tap.sampleRate)
            assertEquals(1, engine.channels)
        }
    }

    @Test(expected = AudioProcessor.UnhandledAudioFormatException::class)
    fun unsupportedPcmIsRejectedInsteadOfSilentlyBypassing() {
        EqualizerProcessor(AudioDspEngine(), PcmTap())
            .configure(AudioProcessor.AudioFormat(48000, 2, C.ENCODING_PCM_FLOAT))
    }

    @Test
    fun pendingMonoFormatDoesNotChangeAnUndrainedStereoProcessor() {
        val engine = AudioDspEngine()
        engine.update(
            DspSettings(
                enabled = true,
                mode = EqMode.PARAMETRIC,
                filters = List(32) { EqFilter(1000.0, 1.0) },
            )
        )
        val p = EqualizerProcessor(engine, PcmTap())
        p.configure(AudioProcessor.AudioFormat(48000, 2, C.ENCODING_PCM_16BIT))
        p.flush(AudioProcessor.StreamMetadata.DEFAULT)
        p.configure(AudioProcessor.AudioFormat(44100, 1, C.ENCODING_PCM_16BIT))
        assertEquals(2, engine.channels)
        assertEquals(48000, engine.sampleRate)
        val input = ByteBuffer.allocateDirect(400).order(ByteOrder.LITTLE_ENDIAN)
        repeat(200) { input.putShort(100) }
        input.flip()
        p.queueInput(input)
        assertEquals(400, p.output.remaining())
        p.flush(AudioProcessor.StreamMetadata.DEFAULT)
        assertEquals(1, engine.channels)
        assertEquals(44100, engine.sampleRate)
    }
}
