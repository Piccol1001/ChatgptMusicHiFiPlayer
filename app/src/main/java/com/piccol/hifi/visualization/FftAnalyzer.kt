package com.piccol.hifi.visualization

import kotlin.math.*

/** Radix-2 FFT with a periodic Hann window and coherent single-sided amplitude normalization. */
class FftAnalyzer(val size: Int) {
    init {
        require(size in 512..8192 && size and (size - 1) == 0)
    }

    private val real = DoubleArray(size)
    private val imaginary = DoubleArray(size)
    private val window = DoubleArray(size) { .5 - .5 * cos(2 * PI * it / size) }
    private val normalization = window.sum()
    private val reverse =
        IntArray(size) { Integer.reverse(it) ushr (32 - Integer.numberOfTrailingZeros(size)) }
    private val cosine = DoubleArray(size / 2) { cos(-2 * PI * it / size) }
    private val sine = DoubleArray(size / 2) { sin(-2 * PI * it / size) }
    val magnitudes = FloatArray(size / 2 + 1)

    fun analyze(input: FloatArray, offset: Int = 0): FloatArray {
        require(offset >= 0 && input.size >= offset + size)
        for (i in 0 until size) {
            real[reverse[i]] = input[offset + i] * window[i]
            imaginary[reverse[i]] = 0.0
        }
        var length = 2
        while (length <= size) {
            val half = length / 2
            val step = size / length
            for (start in 0 until size step length) for (k in 0 until half) {
                val a = start + k
                val b = a + half
                val w = k * step
                val tr = cosine[w] * real[b] - sine[w] * imaginary[b]
                val ti = sine[w] * real[b] + cosine[w] * imaginary[b]
                val ar = real[a]
                val ai = imaginary[a]
                real[a] = ar + tr
                imaginary[a] = ai + ti
                real[b] = ar - tr
                imaginary[b] = ai - ti
            }
            length *= 2
        }
        for (i in magnitudes.indices) magnitudes[i] =
            (hypot(real[i], imaginary[i]) * (if (i == 0 || i == size / 2) 1 else 2) / normalization)
                .toFloat()
        return magnitudes
    }
}

fun amplitudeDb(amplitude: Double): Float =
    (20 * log10(amplitude.coerceAtLeast(1e-8))).coerceIn(-96.0, 12.0).toFloat()

fun spectrumBands(
    magnitudes: FloatArray,
    rate: Int,
    fftSize: Int,
    bars: Int,
    logarithmic: Boolean,
): FloatArray {
    val top = min(20000.0, rate * .5)
    fun edge(i: Int) =
        if (logarithmic) 20.0 * (top / 20).pow(i.toDouble() / bars) else top * i / bars
    return FloatArray(bars) { i ->
        val lo = floor(edge(i) * fftSize / rate).toInt().coerceIn(0, magnitudes.lastIndex)
        val hi = ceil(edge(i + 1) * fftSize / rate).toInt().coerceIn(lo + 1, magnitudes.size)
        amplitudeDb((lo until hi).maxOf { magnitudes[it].toDouble() })
    }
}
