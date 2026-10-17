package com.piccol.hifi.audio

import androidx.media3.common.*
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.*
import java.io.IOException
import kotlin.math.*

data class AiffPcm(
    val channels: Int,
    val frames: Long,
    val bits: Int,
    val rate: Int,
    val littleEndian: Boolean = false,
) {
    val block: Int
        get() = channels * (bits / 8)

    val durationUs: Long
        get() = frames * 1000000 / rate
}

fun aiffCommon(bytes: ByteArray, compressed: Boolean): AiffPcm {
    require(bytes.size >= if (compressed) 22 else 18) { "Short AIFF COMM chunk" }
    fun u(i: Int) = bytes[i].toInt() and 255
    val channels = (u(0) shl 8) or u(1)
    val frames = (2..5).fold(0L) { n, i -> (n shl 8) + u(i) }
    val bits = (u(6) shl 8) or u(7)
    val exponent = ((u(8) and 127) shl 8) or u(9)
    var mantissa = 0.0
    for (i in 10..17) mantissa = mantissa * 256 + u(i)
    val rate = mantissa * 2.0.pow(exponent - 16383 - 63)
    require(
        u(8) and 128 == 0 &&
            rate.isFinite() &&
            rate in 8000.0..384000.0 &&
            abs(rate - rate.roundToInt()) < .001
    ) {
        "Unsupported AIFF sample rate"
    }
    require(channels in 1..8 && bits in listOf(16, 24, 32) && frames > 0) {
        "Only 16/24/32-bit signed PCM with 1–8 channels is supported"
    }
    val compression = if (compressed) String(bytes, 18, 4, Charsets.US_ASCII) else "NONE"
    require(compression in listOf("NONE", "sowt", "twos")) { "Compressed AIFC is unsupported" }
    return AiffPcm(channels, frames, bits, rate.roundToInt(), compression == "sowt")
}

/** Bounded streaming extraction of AIFF/AIFC signed PCM. No full-file buffering or native codec. */
@UnstableApi
class AiffExtractor : Extractor {
    private lateinit var output: ExtractorOutput
    private lateinit var track: TrackOutput
    private var format: AiffPcm? = null
    private var start = 0L
    private var length = 0L
    private var formEnd = Long.MAX_VALUE
    private val buffer = ByteArray(32768)
    private val packet = ParsableByteArray(buffer)

    override fun sniff(input: ExtractorInput): Boolean {
        val header = ByteArray(12)
        return input.peekFully(header, 0, 12, true) &&
            String(header, 0, 4, Charsets.US_ASCII) == "FORM" &&
            String(header, 8, 4, Charsets.US_ASCII) in listOf("AIFF", "AIFC")
    }

    override fun init(output: ExtractorOutput) {
        this.output = output
    }

    override fun read(input: ExtractorInput, seekPosition: PositionHolder): Int {
        try {
            if (format == null) {
                val h = ByteArray(12)
                input.readFully(h, 0, 12)
                require(String(h, 0, 4, Charsets.US_ASCII) == "FORM")
                val kind = String(h, 8, 4, Charsets.US_ASCII)
                require(kind in listOf("AIFF", "AIFC"))
                fun uint(b: ByteArray, at: Int) =
                    (at until at + 4).fold(0L) { n, i -> (n shl 8) + (b[i].toInt() and 255) }
                formEnd = 8 + uint(h, 4)
                var common: AiffPcm? = null
                var chunks = 0
                while (input.position + 8 <= formEnd) {
                    require(++chunks <= 10000) { "Too many AIFF chunks" }
                    val c = ByteArray(8)
                    input.readFully(c, 0, 8)
                    val size = uint(c, 4)
                    val id = String(c, 0, 4, Charsets.US_ASCII)
                    require(
                        size <= Int.MAX_VALUE &&
                            input.position + size <= formEnd &&
                            (input.length < 0 || input.position + size <= input.length)
                    ) {
                        "Truncated AIFF chunk"
                    }
                    when (id) {
                        "COMM" -> {
                            require(size in 18..65536)
                            val b = ByteArray(size.toInt())
                            input.readFully(b, 0, b.size)
                            common = aiffCommon(b, kind == "AIFC")
                            if (size % 2 == 1L) input.skipFully(1)
                        }
                        "SSND" -> {
                            val f =
                                common ?: throw IllegalArgumentException("COMM must precede SSND")
                            require(size >= 8)
                            val b = ByteArray(8)
                            input.readFully(b, 0, 8)
                            val offset = uint(b, 0)
                            require(offset <= size - 8)
                            input.skipFully(offset.toInt())
                            start = input.position
                            length = minOf(size - 8 - offset, f.frames * f.block)
                            require(length == f.frames * f.block) { "Truncated AIFF PCM" }
                            format = f
                            track = output.track(0, C.TRACK_TYPE_AUDIO)
                            track.format(
                                Format.Builder()
                                    .setSampleMimeType(MimeTypes.AUDIO_RAW)
                                    .setContainerMimeType("audio/aiff")
                                    .setChannelCount(f.channels)
                                    .setSampleRate(f.rate)
                                    .setPcmEncoding(
                                        when (f.bits) {
                                            24 -> C.ENCODING_PCM_24BIT
                                            32 -> C.ENCODING_PCM_32BIT
                                            else -> C.ENCODING_PCM_16BIT
                                        }
                                    )
                                    .setAverageBitrate(f.rate * f.block * 8)
                                    .build()
                            )
                            output.endTracks()
                            output.seekMap(
                                object : SeekMap {
                                    override fun isSeekable() = true

                                    override fun getDurationUs() = f.durationUs

                                    override fun getSeekPoints(timeUs: Long): SeekMap.SeekPoints {
                                        val frame =
                                            (timeUs.coerceAtLeast(0) * f.rate / 1000000)
                                                .coerceAtMost(f.frames - 1)
                                        return SeekMap.SeekPoints(
                                            SeekPoint(
                                                frame * 1000000 / f.rate,
                                                start + frame * f.block,
                                            )
                                        )
                                    }
                                }
                            )
                            break
                        }
                        else -> {
                            input.skipFully(size.toInt())
                            if (size % 2 == 1L) input.skipFully(1)
                        }
                    }
                }
                require(format != null) { "Missing AIFF PCM chunks" }
            }
            val f = format!!
            val remaining = start + length - input.position
            if (remaining <= 0) return Extractor.RESULT_END_OF_INPUT
            val count = minOf(remaining, buffer.size.toLong()).toInt() / f.block * f.block
            if (count == 0) return Extractor.RESULT_END_OF_INPUT
            val position = input.position
            input.readFully(buffer, 0, count)
            if (!f.littleEndian) {
                val width = f.bits / 8
                for (at in 0 until count step width) for (i in 0 until width / 2) {
                    val b = buffer[at + i]
                    buffer[at + i] = buffer[at + width - 1 - i]
                    buffer[at + width - 1 - i] = b
                }
            }
            packet.reset(buffer, count)
            track.sampleData(packet, count)
            track.sampleMetadata(
                (position - start) / f.block * 1000000 / f.rate,
                C.BUFFER_FLAG_KEY_FRAME,
                count,
                0,
                null,
            )
            return Extractor.RESULT_CONTINUE
        } catch (e: IllegalArgumentException) {
            throw IOException("Unsupported or malformed AIFF: ${e.message}", e)
        }
    }

    override fun seek(position: Long, timeUs: Long) {
        if (position == 0L) format = null
    }

    override fun release() = Unit
}
