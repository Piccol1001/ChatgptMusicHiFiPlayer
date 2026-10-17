package com.piccol.hifi.audio

data class LyricLine(val timeMs: Long?, val text: String)

data class Lyrics(val lines: List<LyricLine>, val offsetMs: Long = 0) {
    val synchronized: Boolean
        get() = lines.any { it.timeMs != null }

    fun current(positionMs: Long, userOffsetMs: Long = 0): Int {
        if (!synchronized) return -1
        val time = positionMs - offsetMs - userOffsetMs
        var low = 0
        var high = lines.size
        while (low < high) {
            val mid = (low + high) / 2
            if ((lines[mid].timeMs ?: Long.MAX_VALUE) <= time) low = mid + 1 else high = mid
        }
        return low - 1
    }
}

fun parseLyrics(text: String): Lyrics {
    require(text.toByteArray().size <= 262144) { "Lyrics exceed 256 KiB" }
    val stamp = Regex("\\[(?:(\\d{1,2}):)?(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")
    val offset =
        Regex("\\[offset:([+-]?\\d+)]", RegexOption.IGNORE_CASE)
            .find(text)
            ?.groupValues
            ?.get(1)
            ?.toLongOrNull()
            ?.coerceIn(-600000, 600000) ?: 0
    val timed = mutableListOf<LyricLine>()
    val plain = mutableListOf<LyricLine>()
    text.removePrefix("\uFEFF").lineSequence().take(10000).forEach { raw ->
        val stamps = stamp.findAll(raw).toList()
        if (stamps.isNotEmpty()) {
            val words = raw.replace(stamp, "").trim()
            for (match in stamps) {
                val v = match.groupValues
                val seconds = v[3].toInt()
                if (seconds < 60) {
                    val time =
                        ((v[1].toLongOrNull() ?: 0) * 3600 + v[2].toLong() * 60 + seconds) * 1000 +
                            (v[4].padEnd(3, '0').toLongOrNull() ?: 0)
                    timed += LyricLine(time, words)
                }
            }
        } else if (!raw.trim().matches(Regex("\\[[a-z]+:.*]", RegexOption.IGNORE_CASE)))
            plain += LyricLine(null, raw)
    }
    return if (timed.isNotEmpty()) Lyrics(timed.sortedBy { it.timeMs }, offset) else Lyrics(plain)
}

/** ID3 USLT data excludes the frame header. Handles Latin-1, UTF-16 with BOM, BE and UTF-8. */
fun decodeUslt(data: ByteArray): String? =
    runCatching {
            if (data.size < 5) return null
            val encoding = data[0].toInt() and 255
            val charset =
                when (encoding) {
                    0 -> Charsets.ISO_8859_1
                    1 -> Charsets.UTF_16
                    2 -> Charsets.UTF_16BE
                    3 -> Charsets.UTF_8
                    else -> return null
                }
            var start = 4
            if (encoding == 1 || encoding == 2) {
                while (
                    start + 1 < data.size &&
                        !(data[start] == 0.toByte() && data[start + 1] == 0.toByte())
                ) start += 2
                start += 2
            } else {
                while (start < data.size && data[start] != 0.toByte()) start++
                start++
            }
            if (start >= data.size) null
            else String(data, start, data.size - start, charset).trimEnd('\u0000')
        }
        .getOrNull()
