# PCM visualizers

All six modes consume the same post-EQ, post-ReplayGain PCM tap in the service's only audio processor. No microphone, simulated waveform, or Android Visualizer capture API is used. The front stereo pair is monitored; mono is duplicated. Android volume and downstream mixer processing occur after this tap.

The audio thread only writes two float samples into a bounded 16,384-frame ring. Analysis runs on Dispatchers.Default with preallocated FFT workspaces. A reader discards a frame if the writer overwrote its snapshot. UI frame arrays and spectrogram pixels are bounded. There is no unbounded audio queue and the writer never waits for analysis.

The periodic Hann FFT uses coherent single-sided amplitude normalization. Its size is selectable from 512 to 8,192. Spectrum bars show the largest bin amplitude in each frequency range in dBFS; they are not integrated band-power or acoustic SPL measurements. Stereo spectra combine channel energy, avoiding phase cancellation. Spectrum/circular/reactive modes share these measured bands. The scope uses a rising-edge trigger, selectable time window and amplitude, and averaged display samples. Stereo meters separately measure RMS with a 300 ms exponential energy average and digital peak hold/decay. They are not calibrated analog VU meters or true-peak meters. The spectrogram stores a fixed-width rolling bitmap.

The analysis flow is collected only by the visible Visualizer screen at lifecycle STARTED and above. Pausing playback disables the tap and suspends analysis. Navigation/background collection cancellation disables the tap in a finally block. Seek/decoder flush clears the ring and increments an epoch so old spectrogram/meter history is discarded. OpenGL rendering uses RENDERMODE_WHEN_DIRTY; the surface pauses and its EGL context is released with its lifecycle.

There are 30 and 60 FPS targets, three rendering quality levels, configurable bars, peak behavior, colors, stereo/time controls, and fullscreen with a visible exit and system Back support. FPS values are targets, not a claim about actual device performance.

## Original shaders and projectM evaluation

The reactive mode contains three original OpenGL ES 2.0 effects: Flow, Orbit, and Prism. They take measured bass/mid/high and RMS uniforms. Digital silence produces black. No third-party shaders or presets are bundled.

projectM was evaluated against its upstream LGPL-2.1 license and integration architecture. It requires a maintained Android native build, GL context coordination, asset packaging and license/distribution work. It is not included in this build. The screen explicitly identifies the original renderer and does not claim projectM support.

## Verification scope

JUnit checks FFT tone location and calibrated amplitude across multiple FFT sizes and 44.1/48/96 kHz rates, silent-channel isolation, ring wrap/reset, pause suspension, and cancellation cleanup. Android instrumentation compiles the original shaders in an EGL pbuffer and checks silent and nonzero signal pixels for every effect. The existing service tests verify real decoded PCM and the supported baseline formats. Physical GPU frame rates, extended battery consumption, audible stutter and thermal behavior remain device checks.
