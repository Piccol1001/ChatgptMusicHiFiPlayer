package com.piccol.hifi.audio

import com.piccol.hifi.dsp.*

data class AutoEqImport(val settings: DspSettings, val notes: String, val original: String)

/** A deliberately bounded subset of Equalizer APO / AutoEq text, not a general APO interpreter. */
fun parseAutoEq(text: String): AutoEqImport {
    require(text.toByteArray().size <= 262144) { "Profile exceeds 256 KiB" }
    val lines =
        text
            .removePrefix("\uFEFF")
            .lineSequence()
            .map { it.substringBefore('#').trim() }
            .filter { it.isNotEmpty() }
            .toList()
    var preamp = 0.0
    val filters = mutableListOf<EqFilter>()
    var graphic: List<Pair<Double, Double>>? = null
    val number = "([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+))"
    val pre = Regex("Preamp:\\s*$number\\s*dB", RegexOption.IGNORE_CASE)
    val peq =
        Regex(
            "Filter\\s+\\d+:\\s*(ON|OFF)\\s+(PK|LS|HS|LP|HP)\\s+Fc\\s+$number\\s+Hz(?:\\s+Gain\\s+$number\\s+dB)?(?:\\s+Q\\s+$number)?",
            RegexOption.IGNORE_CASE,
        )
    for (line in lines) when {
        pre.matches(line) -> {
            preamp = pre.matchEntire(line)!!.groupValues[1].toDouble()
            require(preamp.isFinite() && preamp in -24.0..12.0) { "Preamp must be -24 to +12 dB" }
        }
        line.startsWith("GraphicEQ:", true) -> {
            require(graphic == null && filters.isEmpty()) {
                "Use one GraphicEQ or parametric filters"
            }
            val points =
                line.substringAfter(':').split(';').map { pair ->
                    val parts = pair.trim().split(Regex("\\s+"))
                    require(parts.size == 2) { "Expected frequency gain pairs" }
                    val f = parts[0].toDouble()
                    val g = parts[1].toDouble()
                    require(
                        f.isFinite() && g.isFinite() && f in 10.0..24000.0 && g in -12.0..12.0
                    ) {
                        "GraphicEQ values exceed supported bounds"
                    }
                    f to g
                }
            require(
                points.size in 2..1024 &&
                    points.zipWithNext().all { it.first.first < it.second.first }
            ) {
                "GraphicEQ frequencies must increase"
            }
            graphic = points
        }
        peq.matches(line) -> {
            require(graphic == null) { "Mixed GraphicEQ/parametric profiles are unsupported" }
            val v = peq.matchEntire(line)!!.groupValues
            val type =
                when (v[2].uppercase()) {
                    "PK" -> FilterType.BELL
                    "LS" -> FilterType.LOW_SHELF
                    "HS" -> FilterType.HIGH_SHELF
                    "LP" -> FilterType.LOW_PASS
                    else -> FilterType.HIGH_PASS
                }
            require(
                v[4].isNotEmpty() || type in listOf(FilterType.LOW_PASS, FilterType.HIGH_PASS)
            ) {
                "Gain is required"
            }
            require(v[5].isNotEmpty()) { "An explicit Q is required for this filter" }
            val f =
                EqFilter(
                    v[3].toDouble(),
                    v[4].toDoubleOrNull() ?: 0.0,
                    v[5].toDouble(),
                    type,
                    v[1].equals("ON", true),
                )
            require(f.valid()) { "Filter exceeds supported frequency/gain/Q bounds" }
            filters += f
            require(filters.size <= 32) { "At most 32 filters" }
        }
        else -> throw IllegalArgumentException("Unsupported profile line: ${line.take(80)}")
    }
    val s =
        if (graphic != null) {
            val p = graphic!!
            DspSettings(
                enabled = true,
                mode = EqMode.THIRTY_ONE,
                thirtyOne = interpolateCurve(p.map { it.first }, p.map { it.second }, THIRD_OCTAVE),
                preamp = preamp,
                preset = "Imported GraphicEQ",
            )
        } else {
            require(filters.isNotEmpty()) { "No supported filters found" }
            while (filters.size < 10) filters += EqFilter(1000.0, enabled = false)
            DspSettings(
                enabled = true,
                mode = EqMode.PARAMETRIC,
                filters = filters,
                preamp = preamp,
                preset = "Imported AutoEq",
            )
        }
    require(s.valid())
    return AutoEqImport(
        s,
        if (graphic != null)
            "GraphicEQ control points are interpolated onto 31 bands. The resulting acoustic response is an approximation; inspect and adjust it."
        else "Parametric RBJ filters with explicit Q. Automatic headroom remains enabled.",
        text,
    )
}
