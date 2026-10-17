package com.piccol.hifi.dsp

import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioOffloadSupport
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink

/** Force decoder selection even when HDMI/USB advertises compressed passthrough. */
@UnstableApi
class PcmOnlyAudioSink(sink: AudioSink) : ForwardingAudioSink(sink) {
    override fun supportsFormat(format: Format) =
        format.sampleMimeType == MimeTypes.AUDIO_RAW && super.supportsFormat(format)

    override fun getFormatSupport(format: Format) =
        if (format.sampleMimeType == MimeTypes.AUDIO_RAW) super.getFormatSupport(format)
        else AudioSink.SINK_FORMAT_UNSUPPORTED

    override fun getFormatOffloadSupport(format: Format) = AudioOffloadSupport.DEFAULT_UNSUPPORTED
}
