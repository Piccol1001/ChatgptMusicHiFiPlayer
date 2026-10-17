package com.piccol.hifi.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Runs after Media3's PCM-to-16-bit conversion. Offload/float bypass are disabled in the service.
 */
@UnstableApi
class EqualizerProcessor(private val engine: AudioDspEngine, private val tap: PcmTap) :
    BaseAudioProcessor() {
    override fun onConfigure(
        inputAudioFormat: AudioProcessor.AudioFormat
    ): AudioProcessor.AudioFormat {
        if (
            inputAudioFormat.encoding != C.ENCODING_PCM_16BIT ||
                inputAudioFormat.channelCount !in 1..8 ||
                inputAudioFormat.sampleRate < 8000
        )
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val output = replaceOutputBuffer(inputBuffer.remaining()).order(ByteOrder.LITTLE_ENDIAN)
        val channels = inputAudioFormat.channelCount
        if (engine.isBypassed && !tap.enabled) {
            engine.skipDryFrames(inputBuffer.remaining() / (channels * 2))
            output.put(inputBuffer)
            output.flip()
            return
        }
        while (inputBuffer.remaining() >= channels * 2) {
            engine.beginFrame()
            var left = 0f
            var right = 0f
            for (channel in 0 until channels) {
                val sample = engine.sample(inputBuffer.short / 32768.0, channel)
                val pcm = (sample.coerceIn(-1.0, 32767.0 / 32768) * 32768).roundToInt().toShort()
                output.putShort(pcm)
                if (channel == 0) left = pcm / 32768f
                if (channel == 1) right = pcm / 32768f
            }
            engine.endFrame()
            tap.offer(left, if (channels == 1) left else right)
        }
        output.flip()
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        // configure() can announce a pending format while the previous stream is still draining.
        // BaseAudioProcessor activates inputAudioFormat only at flush; switch channel state here.
        val f = inputAudioFormat
        if (f != AudioProcessor.AudioFormat.NOT_SET) {
            if (engine.sampleRate != f.sampleRate || engine.channels != f.channelCount)
                engine.configure(f.sampleRate, f.channelCount)
            else engine.reset()
            tap.sampleRate = f.sampleRate
        } else engine.reset()
        tap.clear()
    }
}
