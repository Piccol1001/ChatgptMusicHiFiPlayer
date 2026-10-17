package com.piccol.hifi.audio

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.*

/** Preserve Media3 extraction and add bounded AIFF PCM support. */
@UnstableApi
class LocalExtractors : ExtractorsFactory {
    private val defaults = DefaultExtractorsFactory().setDisableArtworkMetadata(true)

    override fun createExtractors(): Array<Extractor> =
        arrayOf(AiffExtractor(), *defaults.createExtractors())

    override fun createExtractors(uri: Uri, headers: Map<String, List<String>>): Array<Extractor> =
        arrayOf(AiffExtractor(), *defaults.createExtractors(uri, headers))
}
