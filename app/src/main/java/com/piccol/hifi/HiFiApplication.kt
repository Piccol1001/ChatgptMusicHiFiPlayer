package com.piccol.hifi

import android.app.Application
import androidx.room.Room
import com.piccol.hifi.audio.*
import com.piccol.hifi.data.*
import com.piccol.hifi.dsp.*
import com.piccol.hifi.library.LibraryScanner
import kotlinx.coroutines.flow.*

class HiFiApplication : Application() {
    val database by lazy {
        Room.databaseBuilder(this, MusicDatabase::class.java, "music.db").build()
    }
    val preferences by lazy { Preferences(this) }
    val scanner by lazy { LibraryScanner(this, database.music(), preferences) }
    val dsp by lazy { DspRepository(this) }
    val audioEngine by lazy { AudioDspEngine() }
    val output by lazy { OutputMonitor(this) }
    val profiles by lazy { ProfileRepository(this) }
    val lyrics by lazy { LyricsRepository(this) }
    val smartPlaylists by lazy { SmartPlaylists(this) }
    val sourceInfo = MutableStateFlow(SourceInfo())
    val playbackProblem = MutableStateFlow<String?>(null)
    val embeddedLyrics = MutableStateFlow<String?>(null)
    val effectiveDsp by lazy {
        combine(dsp.settings, profiles.state, output.route) { base, profiles, route ->
                effectiveDsp(base, profiles, route)
            }
            .distinctUntilChanged()
    }
    val pcmTap by lazy { PcmTap() }
    val replayTags = MutableStateFlow(ReplayTags())
    val widgets by lazy { com.piccol.hifi.widgets.WidgetRepository(this) }
    val serviceRunning = MutableStateFlow(false)
    val playbackIsPlaying = MutableStateFlow(false)
    val visualSettings by lazy { com.piccol.hifi.visualization.VisualRepository(this) }
    val visualization by lazy {
        com.piccol.hifi.visualization.VisualizationEngine(pcmTap, playbackIsPlaying)
    }
}
