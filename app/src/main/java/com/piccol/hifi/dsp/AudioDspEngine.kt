package com.piccol.hifi.dsp

import java.util.concurrent.atomic.AtomicReference
import kotlin.math.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DspFormat(val rate: Int = 48000, val channels: Int = 0)

private data class Plan(
    val filters: List<Biquad>,
    val gain: Double,
    val bypass: Boolean,
    val rate: Int,
    val settings: DspSettings = DspSettings(),
    val tags: ReplayTags = ReplayTags(),
)

private class FilterBank {
    var plan = Plan(emptyList(), 1.0, true, 48000)
    private var z1 = DoubleArray(0)
    private var z2 = DoubleArray(0)
    private var channels = 0

    fun format(count: Int) {
        channels = count
        z1 = DoubleArray(count * 32)
        z2 = DoubleArray(count * 32)
    }

    fun load(next: Plan) {
        plan = next
        z1.fill(0.0)
        z2.fill(0.0)
    }

    fun sample(input: Double, channel: Int): Double {
        if (plan.bypass) return input
        var x = input * plan.gain
        for (i in plan.filters.indices) {
            val c = plan.filters[i]
            val k = i * channels + channel
            val y = c.b0 * x + z1[k]
            z1[k] = c.b1 * x - c.a1 * y + z2[k]
            z2[k] = c.b2 * x - c.a2 * y
            x = y
        }
        return x
    }
}

/**
 * Two preallocated stable filter banks crossfade over 20 ms; no locks or coefficient allocation per
 * frame.
 */
class AudioDspEngine {
    private val _format = MutableStateFlow(DspFormat())
    val format = _format.asStateFlow()
    @Volatile
    var sampleRate = 48000
        private set

    @Volatile
    var channels = 0
        private set

    @Volatile
    var framesProcessed = 0L
        private set

    @Volatile
    var clippedSamples = 0L
        private set

    @Volatile
    var outputPeak = 0.0
        private set

    @Volatile
    var settings = DspSettings()
        private set

    @Volatile
    var replayTags = ReplayTags()
        private set

    /** The plan whose fade has completed on the PCM thread, rather than a requested UI value. */
    @Volatile
    var audibleSettings = DspSettings()
        private set

    @Volatile
    var audibleReplayTags = ReplayTags()
        private set

    private val desired = AtomicReference(compile(settings, replayTags, sampleRate))
    private var active = FilterBank()
    private var incoming = FilterBank()
    private var applied = desired.get()
    private var transitioning = false
    private var frame = 0
    private var ramp = 960
    val isBypassed: Boolean
        get() = !transitioning && desired.get() === applied && active.plan.bypass

    fun skipDryFrames(count: Int) {
        framesProcessed += count
    }

    // Call settings/metadata updates from a background dispatcher, never the audio callback.
    fun update(settings: DspSettings = this.settings, tags: ReplayTags = replayTags) {
        require(settings.valid())
        this.settings = settings
        replayTags = tags
        var rate: Int
        do {
            rate = sampleRate
            val plan = compile(settings, tags, rate)
            desired.set(plan)
        } while (rate != sampleRate)
    }

    fun configure(rate: Int, count: Int) {
        require(rate >= 8000 && count in 1..8)
        sampleRate = rate
        channels = count
        ramp = (rate * .02).toInt().coerceAtLeast(1)
        _format.value = DspFormat(rate, count)
        active.format(count)
        incoming.format(count)
        applied = compile(settings, replayTags, rate)
        desired.set(applied)
        reset()
    }

    fun reset() {
        active.load(applied)
        incoming.load(applied)
        transitioning = false
        frame = 0
        outputPeak = 0.0
        audibleSettings = active.plan.settings
        audibleReplayTags = active.plan.tags
    }

    fun beginFrame() {
        if (!transitioning) {
            val next = desired.get()
            if (next !== applied && next.rate == sampleRate) {
                incoming.load(next)
                applied = next
                transitioning = true
                frame = 0
            }
        }
    }

    fun sample(input: Double, channel: Int): Double {
        val old = active.sample(input, channel)
        val output =
            if (transitioning) {
                val t = frame.toDouble() / ramp
                old * (1 - t) + incoming.sample(input, channel) * t
            } else old
        if (abs(output) > 1.0) clippedSamples++
        outputPeak = max(outputPeak, abs(output))
        return if (output.isFinite()) output else 0.0
    }

    fun endFrame() {
        framesProcessed++
        if (transitioning && ++frame >= ramp) {
            val swap = active
            active = incoming
            incoming = swap
            transitioning = false
            audibleSettings = active.plan.settings
            audibleReplayTags = active.plan.tags
        }
    }

    private fun compile(s: DspSettings, tags: ReplayTags, rate: Int): Plan {
        val filters =
            if (s.enabled)
                s.activeFilters()
                    .map { Biquad.design(it, rate) }
                    .filterNot { it.b0 == 1.0 && it.b1 == it.a1 && it.b2 == it.a2 }
            else emptyList()
        val gainDb = (if (s.enabled) effectivePreamp(s, rate) else 0.0) + replayGain(s, tags)
        return Plan(
            filters,
            10.0.pow(gainDb / 20),
            filters.isEmpty() && gainDb == 0.0,
            rate,
            s,
            tags,
        )
    }
}
