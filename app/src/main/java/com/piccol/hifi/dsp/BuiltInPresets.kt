package com.piccol.hifi.dsp

/**
 * Voicing recipes, not measured headphone correction. Anchors and weights are deterministic and
 * documented.
 */
object BuiltInPresets {
    private val sub = listOf(1.0, .7, .2, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    private val bass = listOf(.4, .8, 1.0, .4, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    private val body = listOf(0.0, 0.0, .3, .8, 1.0, .3, 0.0, 0.0, 0.0, 0.0)
    private val voice = listOf(0.0, 0.0, 0.0, -.2, .2, 1.0, .8, .3, 0.0, 0.0)
    private val air = listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, .1, .4, 1.0, .8)

    private fun recipe(
        name: String,
        category: String,
        s: Double = 0.0,
        b: Double = 0.0,
        m: Double = 0.0,
        v: Double = 0.0,
        a: Double = 0.0,
    ): EqPreset {
        val gains =
            (0..9).map {
                (sub[it] * s + bass[it] * b + body[it] * m + voice[it] * v + air[it] * a).coerceIn(
                    -9.0,
                    9.0,
                )
            }
        val settings =
            DspSettings(
                ten = gains,
                thirtyOne = interpolateCurve(TEN_BANDS, gains, THIRD_OCTAVE),
                filters = TEN_BANDS.zip(gains).map { (f, g) -> EqFilter(f, g, 1.4) },
            )
        return EqPreset(
            "builtin:$name",
            name,
            category,
            settings,
            "Voicing weights (dB): sub $s, bass $b, body $m, presence $v, air $a. Automatic headroom enabled; genre labels are listening suggestions, not mastering corrections.",
        )
    }

    val all: List<EqPreset> =
        listOf(
            recipe("Flat", "Core"),
            recipe("Reference", "Core"),
            recipe("Warm", "Core", b = 1.5, m = 1.0, a = -1.0),
            recipe("Bright", "Core", a = 2.0),
            recipe("Vocal Clarity", "Core", b = -1.0, v = 2.0),
            recipe("Bass Boost", "Core", s = 1.0, b = 4.0),
            recipe("Deep Bass", "Core", s = 4.0, b = 2.0),
            recipe("Sub-Bass", "Core", s = 5.0),
            recipe("Treble Boost", "Core", a = 3.0),
            recipe("Loudness", "Core", s = 2.0, b = 2.0, m = -1.0, a = 2.0),
            recipe("Smooth", "Core", v = -1.0, a = -2.0),
            recipe("Punchy", "Core", b = 3.0, m = -1.0, v = 1.0),
            recipe("Airy", "Core", a = 1.5),
            recipe("Balanced", "Core", b = .5, v = .5, a = .5),
            recipe("V-Shaped", "Core", b = 3.0, m = -2.0, v = -1.0, a = 3.0),
            recipe("U-Shaped", "Core", s = 2.5, m = -1.0, a = 2.0),
            recipe("Pop", "Genres", b = 1.5, v = 1.0, a = .5),
            recipe("Rock", "Genres", b = 1.0, m = .5, v = 1.5),
            recipe("Hard Rock", "Genres", b = 2.0, m = -1.0, v = 2.0),
            recipe("Metal", "Genres", b = 1.5, m = -1.5, v = 1.5, a = .5),
            recipe("Alternative", "Genres", b = .5, m = .5, v = 1.0, a = -.5),
            recipe("Indie", "Genres", m = 1.0, v = .5, a = .5),
            recipe("Classical", "Genres", s = .5, a = .5),
            recipe("Orchestra", "Genres", s = 1.0, m = .5, a = .5),
            recipe("Jazz", "Genres", b = .5, m = 1.0, a = .5),
            recipe("Blues", "Genres", b = 1.0, m = 1.0, v = .5),
            recipe("Acoustic", "Genres", b = -.5, v = 1.0, a = 1.0),
            recipe("Folk", "Genres", m = .5, v = 1.5),
            recipe("Country", "Genres", b = .5, v = 1.0, a = 1.0),
            recipe("EDM", "Genres", s = 3.0, b = 2.0, m = -1.0, a = 1.5),
            recipe("House", "Genres", s = 1.5, b = 2.5, a = 1.0),
            recipe("Deep House", "Genres", s = 3.0, b = 1.5, a = -.5),
            recipe("Techno", "Genres", s = 2.0, b = 3.0, m = -1.0, a = 1.0),
            recipe("Trance", "Genres", s = 1.5, b = 2.0, v = .5, a = 2.0),
            recipe("Dubstep", "Genres", s = 4.0, b = 2.5, m = -1.5, a = 1.0),
            recipe("Drum & Bass", "Genres", s = 3.0, b = 2.0, v = .5, a = 1.5),
            recipe("Hip-Hop", "Genres", s = 2.5, b = 2.0, v = 1.0),
            recipe("Trap", "Genres", s = 4.0, b = 1.0, v = .5, a = 1.0),
            recipe("R&B", "Genres", s = 1.0, b = 1.5, m = .5, v = 1.0, a = -.5),
            recipe("Funk", "Genres", b = 2.0, v = 1.5, a = .5),
            recipe("Reggae", "Genres", s = 2.0, b = 2.0, m = .5, a = -1.0),
            recipe("Latin", "Genres", b = 1.0, v = 1.0, a = 1.5),
            recipe("K-Pop", "Genres", b = 1.5, v = 1.5, a = 1.5),
            recipe("J-Pop", "Genres", b = 1.0, v = 1.5, a = 1.0),
            recipe("Lo-Fi", "Genres", b = 1.0, m = 1.5, a = -3.0),
            recipe("Chillout", "Genres", s = 1.0, b = 1.0, a = -1.5),
            recipe("Ambient", "Genres", s = 1.0, v = -.5, a = 1.0),
            recipe("Synthwave", "Genres", s = 1.5, b = 1.5, m = -.5, a = 1.0),
            recipe("Headphones", "Listening", s = .5, b = .5, a = .5),
            recipe("IEM", "Listening", s = .5, a = -1.0),
            recipe("Small Speaker", "Listening", s = -3.0, b = 1.5, v = 1.0),
            recipe("Phone Speaker", "Listening", s = -6.0, b = -2.0, v = 1.5),
            recipe("Car Audio", "Listening", s = -1.0, b = -.5, v = 1.5, a = .5),
            recipe("Night Listening", "Listening", s = -2.0, b = -1.0, v = 1.0, a = -1.0),
            recipe("Podcast", "Listening", s = -4.0, b = -1.0, v = 2.0),
            recipe("Gaming", "Listening", b = -1.0, v = 2.0, a = 1.0),
            recipe("Live Concert", "Listening", s = 1.0, b = 1.0, v = 1.0, a = 1.5),
            recipe("Vintage", "Listening", b = 1.0, m = 1.5, a = -2.5),
        )
}
