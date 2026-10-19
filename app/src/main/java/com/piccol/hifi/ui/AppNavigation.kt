package com.piccol.hifi.ui

import android.content.Intent
import java.util.Locale

/** Retain feature routes while exposing only three primary destinations. */
enum class Screen(val label: String) {
    LIBRARY("Library"),
    PLAYLISTS("Playlists"),
    PLAYER("Playing"),
    EQ("Equalizer"),
    VISUALIZER("Visualizer");

    val primary: PrimarySection
        get() =
            when (this) {
                LIBRARY,
                PLAYLISTS -> PrimarySection.LIBRARY
                PLAYER -> PrimarySection.PLAYER
                EQ,
                VISUALIZER -> PrimarySection.AUDIO
            }
}

enum class PrimarySection(val label: String) {
    LIBRARY("Library"),
    PLAYER("Playing"),
    AUDIO("Audio"),
}

fun screenForRoute(route: String?): Screen? =
    when (route?.trim('/')?.lowercase(Locale.ROOT)) {
        "library",
        "songs" -> Screen.LIBRARY
        "playlists",
        "library/playlists" -> Screen.PLAYLISTS
        "playing",
        "player",
        "now-playing" -> Screen.PLAYER
        "audio",
        "eq",
        "equalizer",
        "audio/equalizer" -> Screen.EQ
        "visualizer",
        "audio/visualizer" -> Screen.VISUALIZER
        else -> null
    }

fun screenForIntent(intent: Intent): Screen? {
    if (intent.getBooleanExtra("open_player", false)) return Screen.PLAYER
    if (intent.getBooleanExtra("open_equalizer", false)) return Screen.EQ
    if (intent.getBooleanExtra("open_visualizer", false)) return Screen.VISUALIZER
    listOf("route", "screen", "destination").forEach { key ->
        screenForRoute(intent.getStringExtra(key))?.let {
            return it
        }
    }
    val uri = intent.data
    if (intent.action == Intent.ACTION_VIEW && uri?.scheme == "hifi") {
        return screenForRoute(
            listOfNotNull(uri.host, uri.path?.trim('/')?.takeIf { it.isNotBlank() })
                .joinToString("/")
        )
    }
    return null
}
