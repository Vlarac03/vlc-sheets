package com.vlcsheets.app

import android.app.Application
import com.vlcsheets.app.data.LibraryRepository
import com.vlcsheets.app.data.SettingsRepository

class VlcSheetsApp : Application() {

    lateinit var repository: LibraryRepository
        private set

    lateinit var settings: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = LibraryRepository(this)
        settings = SettingsRepository(this)
    }
}
