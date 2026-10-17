package com.piccol.hifi.dsp

import kotlin.math.*

val TEN_BANDS = listOf(31.0, 62.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0)
val THIRD_OCTAVE =
    listOf(
        20.0,
        25.0,
        31.5,
        40.0,
        50.0,
        63.0,
        80.0,
        100.0,
        125.0,
        160.0,
        200.0,
        250.0,
        315.0,
        400.0,
        500.0,
        630.0,
        800.0,
        1000.0,
        1250.0,
        1600.0,
        2000.0,
        2500.0,
        3150.0,
        4000.0,
        5000.0,
        6300.0,
        8000.0,
        10000.0,
        12500.0,
        16000.0,
        20000.0,
    )

enum class EqMode {
    TEN,
    PARAMETRIC,
    THIRTY_ONE,
}

enum class FilterType {
    BELL,
    LOW_SHELF,
    HIGH_SHELF,
    LOW_PASS,
    HIGH_PASS,
}

enum class ReplayMode {
    OFF,
    TRACK,
    ALBUM,
}

data class EqFilter(
    val frequency: Double = 1000.0,
    val gain: Double = 0.0,
    val q: Double = 1.0,
    val type: FilterType = FilterType.BELL,
    val enabled: Boolean = true,
) {
    fun valid(): Boolean =
        frequency.isFinite() &&
            frequency in 10.0..24000.0 &&
            gain.isFinite() &&
            gain in -12.0..12.0 &&
            q.isFinite() &&
            q in .1..18.0
}

data class DspSettings(
    val enabled: Boolean = false,
    val mode: EqMode = EqMode.TEN,
    val ten: List<Double> = List(10) { 0.0 },
    val thirtyOne: List<Double> = List(31) { 0.0 },
    val filters: List<EqFilter> = TEN_BANDS.map { EqFilter(frequency = it) },
    val preamp: Double = 0.0,
    val autoHeadroom: Boolean = true,
    val preset: String = "Flat",
    val replayMode: ReplayMode = ReplayMode.OFF,
    val replayPreamp: Double = 0.0,
    val preventClipping: Boolean = true,
) {
    fun valid() =
        ten.size == 10 &&
            thirtyOne.size == 31 &&
            filters.size in 10..32 &&
            (ten + thirtyOne).all { it.isFinite() && it in -12.0..12.0 } &&
            filters.all(EqFilter::valid) &&
            preamp.isFinite() &&
            preamp in -24.0..12.0 &&
            replayPreamp.isFinite() &&
            replayPreamp in -12.0..12.0

    fun activeFilters(): List<EqFilter> =
        when (mode) {
            EqMode.TEN -> TEN_BANDS.zip(ten).map { (f, g) -> EqFilter(f, g, 1.4) }
            EqMode.THIRTY_ONE -> THIRD_OCTAVE.zip(thirtyOne).map { (f, g) -> EqFilter(f, g, 4.318) }
            EqMode.PARAMETRIC -> filters
        }
}

data class ReplayTags(
    val trackGain: Double? = null,
    val albumGain: Double? = null,
    val trackPeak: Double? = null,
    val albumPeak: Double? = null,
    val decoderGainActive: Boolean = false,
)

fun replayGain(settings: DspSettings, tags: ReplayTags): Double {
    if (settings.replayMode == ReplayMode.OFF || tags.decoderGainActive) return 0.0
    val album = settings.replayMode == ReplayMode.ALBUM
    // Album mode falls back to track gain, with the matching peak, when album gain is absent.
    val useAlbum = album && tags.albumGain != null
    val gain = (if (useAlbum) tags.albumGain else tags.trackGain) ?: return 0.0
    val peak = if (useAlbum) tags.albumPeak else tags.trackPeak
    val requested = (gain + settings.replayPreamp).coerceIn(-60.0, 24.0)
    return if (settings.preventClipping && peak != null && peak > 0 && peak.isFinite())
        min(requested, -20 * log10(peak))
    else requested
}

fun parseReplayTags(values: Map<String, String>): ReplayTags {
    val tags = values.mapKeys { it.key.uppercase() }
    fun value(key: String): Double? =
        tags[key]
            ?.trim()
            ?.removeSuffix("dB")
            ?.removeSuffix("DB")
            ?.trim()
            ?.toDoubleOrNull()
            ?.takeIf(Double::isFinite)
    return ReplayTags(
        value("REPLAYGAIN_TRACK_GAIN"),
        value("REPLAYGAIN_ALBUM_GAIN"),
        value("REPLAYGAIN_TRACK_PEAK")?.takeIf { it > 0 },
        value("REPLAYGAIN_ALBUM_PEAK")?.takeIf { it > 0 },
    )
}

/** Normalized RBJ Audio EQ Cookbook coefficients, evaluated by the same math used for audio. */
data class Biquad(
    val b0: Double = 1.0,
    val b1: Double = 0.0,
    val b2: Double = 0.0,
    val a1: Double = 0.0,
    val a2: Double = 0.0,
) {
    fun magnitude(frequency: Double, sampleRate: Int): Double {
        val w = 2 * PI * frequency / sampleRate
        val nr = b0 + b1 * cos(w) + b2 * cos(2 * w)
        val ni = -b1 * sin(w) - b2 * sin(2 * w)
        val dr = 1 + a1 * cos(w) + a2 * cos(2 * w)
        val di = -a1 * sin(w) - a2 * sin(2 * w)
        return sqrt((nr * nr + ni * ni) / (dr * dr + di * di).coerceAtLeast(1e-30))
    }

    companion object {
        fun design(filter: EqFilter, rate: Int): Biquad {
            require(filter.valid() && rate >= 8000)
            if (!filter.enabled || filter.frequency >= rate * .5) return Biquad()
            val w = 2 * PI * filter.frequency / rate
            val c = cos(w)
            val s = sin(w)
            val a = 10.0.pow(filter.gain / 40)
            val alpha = s / (2 * filter.q)
            val beta = 2 * sqrt(a) * alpha
            val v =
                when (filter.type) {
                    FilterType.BELL ->
                        doubleArrayOf(
                            1 + alpha * a,
                            -2 * c,
                            1 - alpha * a,
                            1 + alpha / a,
                            -2 * c,
                            1 - alpha / a,
                        )
                    FilterType.LOW_PASS ->
                        doubleArrayOf((1 - c) / 2, 1 - c, (1 - c) / 2, 1 + alpha, -2 * c, 1 - alpha)
                    FilterType.HIGH_PASS ->
                        doubleArrayOf(
                            (1 + c) / 2,
                            -(1 + c),
                            (1 + c) / 2,
                            1 + alpha,
                            -2 * c,
                            1 - alpha,
                        )
                    FilterType.LOW_SHELF ->
                        doubleArrayOf(
                            a * ((a + 1) - (a - 1) * c + beta),
                            2 * a * ((a - 1) - (a + 1) * c),
                            a * ((a + 1) - (a - 1) * c - beta),
                            (a + 1) + (a - 1) * c + beta,
                            -2 * ((a - 1) + (a + 1) * c),
                            (a + 1) + (a - 1) * c - beta,
                        )
                    FilterType.HIGH_SHELF ->
                        doubleArrayOf(
                            a * ((a + 1) + (a - 1) * c + beta),
                            -2 * a * ((a - 1) + (a + 1) * c),
                            a * ((a + 1) + (a - 1) * c - beta),
                            (a + 1) - (a - 1) * c + beta,
                            2 * ((a - 1) - (a + 1) * c),
                            (a + 1) - (a - 1) * c - beta,
                        )
                }
            return Biquad(v[0] / v[3], v[1] / v[3], v[2] / v[3], v[4] / v[3], v[5] / v[3])
        }
    }
}

fun responseDb(filters: List<EqFilter>, rate: Int, frequency: Double): Double =
    filters.sumOf {
        20 * log10(Biquad.design(it, rate).magnitude(frequency, rate).coerceAtLeast(1e-12))
    }

fun maxBoost(filters: List<EqFilter>, rate: Int): Double {
    val coefficients = filters.map { Biquad.design(it, rate) }
    val frequencies =
        (0..511).map { i -> 10.0 * (rate * .49999 / 10).pow(i / 511.0) } +
            filters.filter { it.enabled && it.frequency < rate * .5 }.map { it.frequency }
    return frequencies
        .maxOf { frequency ->
            coefficients.sumOf { 20 * log10(it.magnitude(frequency, rate).coerceAtLeast(1e-12)) }
        }
        .coerceAtLeast(0.0)
}

fun effectivePreamp(settings: DspSettings, rate: Int): Double {
    if (!settings.autoHeadroom) return settings.preamp
    val boost = maxBoost(settings.activeFilters(), rate)
    return min(settings.preamp, -boost - if (boost > 0) .5 else 0.0)
}

/** Log-frequency interpolation of *control gains*, not an assertion of equal acoustic response. */
fun interpolateCurve(
    frequencies: List<Double>,
    gains: List<Double>,
    targets: List<Double>,
): List<Double> {
    require(frequencies.size == gains.size && frequencies.isNotEmpty())
    return targets.map { f ->
        val i = frequencies.indexOfFirst { it >= f }
        when {
            i < 0 -> gains.last()
            i == 0 -> gains.first()
            else -> {
                val t = ln(f / frequencies[i - 1]) / ln(frequencies[i] / frequencies[i - 1])
                gains[i - 1] + t * (gains[i] - gains[i - 1])
            }
        }
    }
}
