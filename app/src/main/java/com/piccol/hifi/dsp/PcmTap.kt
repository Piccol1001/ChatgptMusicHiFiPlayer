package com.piccol.hifi.dsp

/**
 * Single audio writer, one analysis reader. The writer never waits; old visual samples may be
 * dropped.
 */
class PcmTap(private val capacity: Int = 16384) {
    init {
        require(capacity > 0 && capacity and (capacity - 1) == 0)
    }

    private val left = FloatArray(capacity)
    private val right = FloatArray(capacity)
    @Volatile private var written = 0L
    @Volatile var enabled = false
    val sequence: Long
        get() = written

    @Volatile var sampleRate = 48000
    @Volatile
    var generation = 0
        private set

    fun clear() {
        written = 0
        generation++
    }

    fun offer(l: Float, r: Float) {
        if (!enabled) return
        val i = (written and (capacity - 1).toLong()).toInt()
        left[i] = l
        right[i] = r
        written++
    }

    fun copyLatest(l: FloatArray, r: FloatArray): Int {
        require(l.size == r.size && l.size <= capacity)
        val epoch = generation
        val end = written
        val count = minOf(l.size.toLong(), end).toInt()
        val start = end - count
        for (i in 0 until count) {
            val at = ((start + i) and (capacity - 1).toLong()).toInt()
            l[i] = left[at]
            r[i] = right[at]
        }
        // Reject a snapshot if an entire ring was overwritten during copying. Dropping a visual
        // frame is safe.
        return if (epoch != generation || written < end || written - end > capacity - count) 0
        else count
    }
}
