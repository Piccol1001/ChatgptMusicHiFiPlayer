package com.piccol.hifi.visualization

import com.piccol.hifi.dsp.PcmTap
import kotlin.math.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class VisualMode {
    SPECTRUM,
    SCOPE,
    METERS,
    SPECTROGRAM,
    CIRCULAR,
    REACTIVE,
}

enum class VisualQuality {
    LOW,
    MEDIUM,
    HIGH,
}

data class VisualSettings(
    val mode: VisualMode = VisualMode.SPECTRUM,
    val fftSize: Int = 2048,
    val bars: Int = 64,
    val fps: Int = 30,
    val logarithmic: Boolean = true,
    val smoothing: Float = .6f,
    val holdMs: Int = 500,
    val decay: Float = 24f,
    val theme: Int = 0,
    val stereo: Boolean = true,
    val timeMs: Int = 40,
    val amplitude: Float = 2f,
    val verticalMeters: Boolean = false,
    val history: Int = 160,
    val rotation: Boolean = false,
    val artwork: Boolean = true,
    val effect: Int = 0,
    val quality: VisualQuality = VisualQuality.MEDIUM,
    val reducedMotion: Boolean = false,
) {
    fun valid() =
        fftSize in listOf(512, 1024, 2048, 4096, 8192) &&
            bars in 16..128 &&
            fps in listOf(30, 60) &&
            smoothing.isFinite() &&
            smoothing in 0f..0.95f &&
            holdMs in 0..2000 &&
            decay.isFinite() &&
            decay in 3f..96f &&
            theme in 0..2 &&
            timeMs in 5..150 &&
            amplitude.isFinite() &&
            amplitude in .5f..8f &&
            history in 60..240 &&
            effect in 0..2
}

data class VisualFrame(
    val sequence: Long = 0,
    val generation: Int = 0,
    val rate: Int = 48000,
    val spectrum: FloatArray = FloatArray(0),
    val peaks: FloatArray = FloatArray(0),
    val waveL: FloatArray = FloatArray(0),
    val waveR: FloatArray = FloatArray(0),
    val rmsL: Float = -96f,
    val rmsR: Float = -96f,
    val peakL: Float = -96f,
    val peakR: Float = -96f,
    val playing: Boolean = false,
    val seconds: Float = 0f,
)

class VisualizationEngine(private val tap: PcmTap, private val playing: StateFlow<Boolean>) {
    fun frames(settings: VisualSettings): Flow<VisualFrame> = flow {
        require(settings.valid())
        val fftL = FftAnalyzer(settings.fftSize)
        val fftR = FftAnalyzer(settings.fftSize)
        val left = FloatArray(16383)
        val right = FloatArray(16383)
        val power = FloatArray(settings.fftSize / 2 + 1)
        val smoothed = FloatArray(settings.bars) { -96f }
        val peaks = FloatArray(settings.bars) { -96f }
        val holds = LongArray(settings.bars)
        val origin = System.nanoTime()
        var previous = origin
        var sequence = Long.MIN_VALUE
        var generation = tap.generation
        var averageL = 0.0
        var averageR = 0.0
        var meterPeakL = -96f
        var meterPeakR = -96f
        var meterHoldL = 0L
        var meterHoldR = 0L
        try {
            while (currentCoroutineContext().isActive) {
                if (!playing.value) {
                    tap.enabled = false
                    emit(VisualFrame(rate = tap.sampleRate))
                    playing.first { it }
                    smoothed.fill(-96f)
                    peaks.fill(-96f)
                    averageL = 0.0
                    averageR = 0.0
                    meterPeakL = -96f
                    meterPeakR = -96f
                }
                tap.enabled = true
                val started = System.nanoTime()
                val epoch = tap.generation
                val end = tap.sequence
                val n = tap.copyLatest(left, right)
                if (epoch != generation) {
                    smoothed.fill(-96f)
                    peaks.fill(-96f)
                    generation = epoch
                    sequence = Long.MIN_VALUE
                    holds.fill(0)
                    averageL = 0.0
                    averageR = 0.0
                    meterPeakL = -96f
                    meterPeakR = -96f
                    meterHoldL = 0
                    meterHoldR = 0
                }
                if (n >= settings.fftSize && end != sequence) {
                    sequence = end
                    val rate = tap.sampleRate
                    val l = fftL.analyze(left, n - settings.fftSize)
                    val r = fftR.analyze(right, n - settings.fftSize)
                    for (i in power.indices) power[i] = sqrt((l[i] * l[i] + r[i] * r[i]) * .5f)
                    val bands =
                        spectrumBands(
                            power,
                            rate,
                            settings.fftSize,
                            settings.bars,
                            settings.logarithmic,
                        )
                    val dt = ((started - previous) / 1e9).coerceIn(.001, .25)
                    previous = started
                    val now = started / 1_000_000
                    val alpha =
                        if (settings.smoothing <= 0) 0.0 else exp(-dt / (settings.smoothing * .25))
                    for (i in bands.indices) {
                        smoothed[i] = (smoothed[i] * alpha + bands[i] * (1 - alpha)).toFloat()
                        if (bands[i] >= peaks[i]) {
                            peaks[i] = bands[i]
                            holds[i] = now + settings.holdMs
                        } else if (now > holds[i])
                            peaks[i] = max(bands[i], peaks[i] - settings.decay * dt.toFloat())
                    }
                    var sumL = 0.0
                    var sumR = 0.0
                    var peakL = 0f
                    var peakR = 0f
                    for (i in n - settings.fftSize until n) {
                        sumL += left[i] * left[i]
                        sumR += right[i] * right[i]
                        peakL = max(peakL, abs(left[i]))
                        peakR = max(peakR, abs(right[i]))
                    }
                    val vu = exp(-dt / .3)
                    averageL = averageL * vu + sumL / settings.fftSize * (1 - vu)
                    averageR = averageR * vu + sumR / settings.fftSize * (1 - vu)
                    val pl = amplitudeDb(peakL.toDouble())
                    val pr = amplitudeDb(peakR.toDouble())
                    if (pl >= meterPeakL) {
                        meterPeakL = pl
                        meterHoldL = now + settings.holdMs
                    } else if (now > meterHoldL)
                        meterPeakL = max(pl, meterPeakL - settings.decay * dt.toFloat())
                    if (pr >= meterPeakR) {
                        meterPeakR = pr
                        meterHoldR = now + settings.holdMs
                    } else if (now > meterHoldR)
                        meterPeakR = max(pr, meterPeakR - settings.decay * dt.toFloat())
                    val points =
                        when (settings.quality) {
                            VisualQuality.LOW -> 128
                            VisualQuality.MEDIUM -> 256
                            VisualQuality.HIGH -> 512
                        }
                    val span = min(n, (rate * settings.timeMs / 1000).coerceAtLeast(points))
                    var from = n - span
                    for (i in max(0, from - span / 4) until from) if (
                        left[i] <= 0 && left[i + 1] > 0
                    ) {
                        from = i
                        break
                    }
                    val available = span
                    fun wave(samples: FloatArray) =
                        FloatArray(points) { i ->
                            val lo = from + i * available / points
                            val hi = from + (i + 1) * available / points
                            var total = 0f
                            for (j in lo until hi) total += samples[j]
                            total / (hi - lo).coerceAtLeast(1)
                        }
                    emit(
                        VisualFrame(
                            end,
                            epoch,
                            rate,
                            smoothed.copyOf(),
                            peaks.copyOf(),
                            wave(left),
                            wave(right),
                            amplitudeDb(sqrt(averageL)),
                            amplitudeDb(sqrt(averageR)),
                            meterPeakL,
                            meterPeakR,
                            true,
                            ((started - origin) / 1e9).toFloat(),
                        )
                    )
                }
                val elapsed = (System.nanoTime() - started) / 1_000_000
                delay((1000L / settings.fps - elapsed).coerceAtLeast(1))
            }
        } finally {
            tap.enabled = false
        }
    }
}
