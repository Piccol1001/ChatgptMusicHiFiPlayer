package com.piccol.hifi.library

import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Bounded local imports, including Android 8 where InputStream.readNBytes is unavailable. */
fun readLimited(input: InputStream, limit: Int = 262144): String {
    val output = ByteArrayOutputStream()
    val chunk = ByteArray(4096)
    while (true) {
        val count = input.read(chunk)
        if (count < 0) break
        require(output.size() + count <= limit) { "File is too large" }
        output.write(chunk, 0, count)
    }
    return output.toString("UTF-8")
}
