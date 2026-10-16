package com.piccol.hifi

import android.app.Application
import androidx.room.Room
import com.piccol.hifi.data.*
import com.piccol.hifi.library.LibraryScanner

class HiFiApplication : Application() {
    val database by lazy { Room.databaseBuilder(this, MusicDatabase::class.java, "music.db").build() }
    val preferences by lazy { Preferences(this) }
    val scanner by lazy { LibraryScanner(this, database.music(), preferences) }
}
