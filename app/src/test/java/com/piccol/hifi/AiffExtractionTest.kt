package com.piccol.hifi

import androidx.media3.common.*
import androidx.media3.common.util.*
import androidx.media3.extractor.*
import com.piccol.hifi.audio.LocalExtractors
import java.io.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@UnstableApi
class AiffExtractionTest {
    private fun fixture(): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeBytes("FORM")
            out.writeInt(54)
            out.writeBytes("AIFFCOMM")
            out.writeInt(18)
            out.write(
                byteArrayOf(
                    0,
                    2,
                    0,
                    0,
                    0,
                    2,
                    0,
                    16,
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
            )
            out.writeBytes("SSND")
            out.writeInt(16)
            out.writeLong(0)
            out.write(byteArrayOf(0x12, 0x34, 0x80.toByte(), 0, 0x7f, 0xff.toByte(), 0, 0))
        }
        return bytes.toByteArray()
    }

    private data class Capture(
        var format: Format? = null,
        var seek: SeekMap? = null,
        val pcm: ByteArrayOutputStream = ByteArrayOutputStream(),
    )

    private fun extract(bytes: ByteArray): Capture {
        val result = Capture()
        val input = ByteArrayInputStream(bytes)
        val reader =
            DefaultExtractorInput(
                { buffer, offset, length -> input.read(buffer, offset, length) },
                0,
                bytes.size.toLong(),
            )
        val extractor = LocalExtractors().createExtractors()[0]
        assertTrue(extractor.sniff(reader))
        reader.resetPeekPosition()
        extractor.init(
            object : ExtractorOutput {
                override fun endTracks() = Unit

                override fun seekMap(map: SeekMap) {
                    result.seek = map
                }

                override fun track(id: Int, type: Int): TrackOutput =
                    object : TrackOutput {
                        override fun format(format: Format) {
                            result.format = format
                        }

                        override fun sampleData(
                            input: DataReader,
                            length: Int,
                            allowEndOfInput: Boolean,
                            sampleDataPart: Int,
                        ): Int = throw AssertionError("Unexpected data reader")

                        override fun sampleData(
                            data: ParsableByteArray,
                            length: Int,
                            sampleDataPart: Int,
                        ) {
                            result.pcm.write(data.data, data.position, length)
                            data.skipBytes(length)
                        }

                        override fun sampleMetadata(
                            timeUs: Long,
                            flags: Int,
                            size: Int,
                            offset: Int,
                            cryptoData: TrackOutput.CryptoData?,
                        ) = Unit
                    }
            }
        )
        while (extractor.read(reader, PositionHolder()) != Extractor.RESULT_END_OF_INPUT) {}
        return result
    }

    @Test
    fun streamingExtractorSwapsPcmAndSeeksToAlignedFrames() {
        val first = extract(fixture())
        assertArrayEquals(
            byteArrayOf(0x34, 0x12, 0, 0x80.toByte(), 0xff.toByte(), 0x7f, 0, 0),
            first.pcm.toByteArray(),
        )
        assertEquals(48000, first.format!!.sampleRate)
        assertEquals(41L, first.seek!!.durationUs)
        assertEquals(58L, first.seek!!.getSeekPoints(30).first.position)
    }

    @Test
    fun truncatedChunkIsRejected() {
        assertTrue(runCatching { extract(fixture().copyOf(60)) }.exceptionOrNull() is IOException)
    }
}
