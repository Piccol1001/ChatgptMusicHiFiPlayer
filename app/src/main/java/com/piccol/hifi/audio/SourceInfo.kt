package com.piccol.hifi.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SourceInfo(
    val codec: String = "Not reported",
    val container: String = "Not reported",
    val rate: Int? = null,
    val channels: Int? = null,
    val bits: Int? = null,
    val bitrate: Int? = null,
    val encoderDelay: Int = 0,
    val encoderPadding: Int = 0,
)

@UnstableApi
suspend fun sourceInfo(context: Context, uri: String, format: Format) =
    withContext(Dispatchers.IO) {
        var bits =
            when (format.pcmEncoding) {
                C.ENCODING_PCM_16BIT -> 16
                C.ENCODING_PCM_24BIT -> 24
                C.ENCODING_PCM_32BIT,
                C.ENCODING_PCM_FLOAT -> 32
                else -> null
            }
        if (format.sampleMimeType == "audio/flac") {
            bits =
                runCatching {
                        context.contentResolver.openInputStream(Uri.parse(uri))?.use { input ->
                            val header = ByteArray(42)
                            var n = 0
                            while (n < header.size) {
                                val count = input.read(header, n, header.size - n)
                                if (count < 0) break
                                n += count
                            }
                            if (
                                n == 42 &&
                                    String(header, 0, 4, Charsets.US_ASCII) == "fLaC" &&
                                    (header[4].toInt() and 127) == 0
                            ) {
                                var packed = 0L
                                for (i in 18..25) packed =
                                    (packed shl 8) + (header[i].toInt() and 255)
                                ((packed ushr 36) and 31).toInt() + 1
                            } else null
                        }
                    }
                    .getOrNull()
        }
        SourceInfo(
            format.sampleMimeType ?: "Not reported",
            format.containerMimeType
                ?: context.contentResolver.getType(Uri.parse(uri))
                ?: "Not reported",
            format.sampleRate.takeIf { it > 0 },
            format.channelCount.takeIf { it > 0 },
            bits,
            format.bitrate.takeIf { it > 0 },
            format.encoderDelay,
            format.encoderPadding,
        )
    }
